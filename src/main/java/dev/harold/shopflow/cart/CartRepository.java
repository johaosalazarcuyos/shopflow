package dev.harold.shopflow.cart;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Cart> findByUserId(Long userId);

    /**
     * Para el checkout: NO carga los productos. Si los cargara aquí, el posterior SELECT ... FOR UPDATE
     * podría devolver una versión más nueva que la ya presente en el contexto de persistencia.
     */
    @EntityGraph(attributePaths = "items")
    Optional<Cart> findForCheckoutByUserId(Long userId);
}
