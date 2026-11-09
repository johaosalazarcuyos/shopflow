package dev.harold.shopflow.order;

import dev.harold.shopflow.cart.Cart;
import dev.harold.shopflow.cart.CartItem;
import dev.harold.shopflow.cart.CartRepository;
import dev.harold.shopflow.catalog.Product;
import dev.harold.shopflow.catalog.ProductRepository;
import dev.harold.shopflow.catalog.ProductService;
import dev.harold.shopflow.common.ApiException;
import dev.harold.shopflow.common.PageResponse;
import dev.harold.shopflow.config.AppProperties;
import dev.harold.shopflow.order.OrderDtos.*;
import dev.harold.shopflow.order.OrderEvents.OrderPlaced;
import dev.harold.shopflow.order.OrderEvents.OrderStatusChanged;
import dev.harold.shopflow.payment.PaymentService;
import dev.harold.shopflow.user.UserRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.*;

@Service
@Transactional
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orders;
    private final CartRepository carts;
    private final ProductRepository products;
    private final ProductService productService;
    private final UserRepository users;
    private final PaymentService payments;
    private final ShippingPolicy shipping;
    private final OrderNumberGenerator numbers;
    private final ApplicationEventPublisher events;
    private final AppProperties props;
    private final Clock clock;
    private final Counter ordersPlaced;
    private final Counter ordersCancelled;

    public OrderService(OrderRepository orders, CartRepository carts, ProductRepository products,
                        ProductService productService, UserRepository users, PaymentService payments,
                        ShippingPolicy shipping, OrderNumberGenerator numbers, ApplicationEventPublisher events,
                        AppProperties props, Clock clock, MeterRegistry meters) {
        this.orders = orders;
        this.carts = carts;
        this.products = products;
        this.productService = productService;
        this.users = users;
        this.payments = payments;
        this.shipping = shipping;
        this.numbers = numbers;
        this.events = events;
        this.props = props;
        this.clock = clock;
        this.ordersPlaced = Counter.builder("shopflow.orders.placed").description("Pedidos creados").register(meters);
        this.ordersCancelled = Counter.builder("shopflow.orders.cancelled").description("Pedidos cancelados").register(meters);
    }

    /**
     * Convierte el carrito en un pedido de forma atómica: bloquea las filas de producto,
     * valida stock de todos los ítems, descuenta y vacía el carrito. Si algo falla, no cambia nada.
     */
    public OrderResponse checkout(Long userId, String actor, CheckoutRequest request) {
        Cart cart = carts.findForCheckoutByUserId(userId).filter(c -> !c.isEmpty())
                .orElseThrow(() -> ApiException.unprocessable("EMPTY_CART", "El carrito está vacío"));

        Map<Long, Integer> requested = new TreeMap<>();
        for (CartItem item : cart.getItems()) {
            requested.put(item.getProduct().getId(), item.getQuantity());
        }

        List<Product> locked = products.findAllByIdForUpdate(requested.keySet());
        List<InsufficientStockException.Item> problems = new ArrayList<>();
        for (Product p : locked) {
            int qty = requested.get(p.getId());
            if (!p.isActive() || !p.hasStock(qty)) {
                problems.add(new InsufficientStockException.Item(p.getId(), p.getName(), qty, p.isActive() ? p.getStock() : 0));
            }
        }
        if (!problems.isEmpty()) {
            throw new InsufficientStockException(problems);
        }

        AddressRequest a = request.shippingAddress();
        Order order = new Order(uniqueNumber(), users.getReferenceById(userId),
                new ShippingAddress(a.fullName(), a.line1(), a.district(), a.city(), a.phone()), actor);
        for (Product p : locked) {
            int qty = requested.get(p.getId());
            p.decreaseStock(qty);
            order.addItem(new OrderItem(order, p, qty));
        }
        order.applyShipping(shipping.costFor(order.getSubtotal()));
        orders.save(order);
        cart.clear();

        productService.evict(requested.keySet());
        events.publishEvent(new OrderPlaced(order.getId()));
        ordersPlaced.increment();
        log.info("Pedido {} creado por usuario {} (total {})", order.getOrderNumber(), userId, order.getTotal());
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummary> listForUser(Long userId, Pageable pageable) {
        return PageResponse.of(orders.findByUserId(userId, pageable).map(OrderSummary::from));
    }

    @Transactional(readOnly = true)
    public OrderResponse getForUser(Long userId, Long orderId) {
        return OrderResponse.from(orders.findByIdAndUserId(orderId, userId).orElseThrow(OrderService::notFound));
    }

    public OrderResponse cancelByCustomer(Long userId, Long orderId, String actor, String reason) {
        Order order = orders.findForUpdate(orderId, userId).orElseThrow(OrderService::notFound);
        cancel(order, actor, reason == null ? "Cancelado por el cliente" : reason);
        return OrderResponse.from(order);
    }

    // ---------------- administración ----------------

    @Transactional(readOnly = true)
    public PageResponse<OrderSummary> listAll(OrderStatus status, Pageable pageable) {
        return PageResponse.of((status == null ? orders.findAll(pageable) : orders.findByStatus(status, pageable))
                .map(OrderSummary::from));
    }

    @Transactional(readOnly = true)
    public OrderResponse getAny(Long orderId) {
        return OrderResponse.from(orders.findById(orderId).orElseThrow(OrderService::notFound));
    }

    public OrderResponse updateStatus(Long orderId, StatusUpdateRequest r, String actor) {
        Order order = orders.findByIdForUpdate(orderId).orElseThrow(OrderService::notFound);
        switch (r.status()) {
            case PAID, PENDING_PAYMENT -> throw ApiException.unprocessable("STATUS_NOT_ALLOWED",
                    "El estado " + r.status() + " solo lo asigna el sistema de pagos");
            case CANCELLED -> cancel(order, actor, r.note());
            default -> {
                order.transitionTo(r.status(), actor, r.note());
                events.publishEvent(new OrderStatusChanged(order.getId(), r.status()));
            }
        }
        return OrderResponse.from(order);
    }

    /** Cancela pedidos que no se pagaron a tiempo. Lo invoca {@link ExpiredOrderJob}. */
    public int cancelExpiredOrders() {
        var cutoff = clock.instant().minus(props.orders().paymentTimeout());
        List<Long> ids = orders.findIdsByStatusCreatedBefore(OrderStatus.PENDING_PAYMENT, cutoff);
        int cancelled = 0;
        for (Long id : ids) {
            Order order = orders.findByIdForUpdate(id).orElseThrow();
            if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
                cancel(order, "system", "Pago no recibido dentro del plazo");
                cancelled++;
            }
        }
        return cancelled;
    }

    private void cancel(Order order, String actor, String reason) {
        if (!order.getStatus().canTransitionTo(OrderStatus.CANCELLED)) {
            throw new InvalidOrderStateException(order.getStatus(), OrderStatus.CANCELLED);
        }
        if (order.getStatus() == OrderStatus.PAID) {
            payments.refund(order);
        }
        order.transitionTo(OrderStatus.CANCELLED, actor, reason);
        restock(order);
        ordersCancelled.increment();
        events.publishEvent(new OrderStatusChanged(order.getId(), OrderStatus.CANCELLED));
    }

    private void restock(Order order) {
        Map<Long, Integer> qty = new HashMap<>();
        order.getItems().forEach(i -> qty.merge(i.getProductId(), i.getQuantity(), Integer::sum));
        products.findAllByIdForUpdate(qty.keySet()).forEach(p -> p.increaseStock(qty.get(p.getId())));
        productService.evict(qty.keySet());
    }

    private String uniqueNumber() {
        String n;
        do {
            n = numbers.next();
        } while (orders.existsByOrderNumber(n));
        return n;
    }

    private static ApiException notFound() {
        return ApiException.notFound("Pedido no encontrado");
    }
}
