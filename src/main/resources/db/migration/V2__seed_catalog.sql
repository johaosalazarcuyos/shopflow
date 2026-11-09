INSERT INTO categories (name, slug) VALUES
    ('Laptops', 'laptops'),
    ('Periféricos', 'perifericos'),
    ('Audio', 'audio'),
    ('Monitores', 'monitores'),
    ('Almacenamiento', 'almacenamiento');

INSERT INTO products (sku, name, description, price, stock, category_id, created_at, updated_at)
SELECT v.sku, v.name, v.description, v.price, v.stock, c.id, now(), now()
FROM (VALUES
    ('LAP-14-R5',   'Laptop 14" Ryzen 5 16GB/512GB',       'Ultraliviana, ideal para estudiantes y programación.', 2899.00, 12, 'laptops'),
    ('LAP-16-I7',   'Laptop 16" Core i7 32GB/1TB',          'Pantalla 2.5K y RTX para desarrollo y diseño.',        5499.00,  5, 'laptops'),
    ('PER-MOUSE-W', 'Mouse inalámbrico ergonómico',         'Sensor 4000 DPI, batería de 70 días.',                   89.90, 80, 'perifericos'),
    ('PER-KB-MECH', 'Teclado mecánico TKL switches red',    'Layout español latino, retroiluminado.',                249.00, 35, 'perifericos'),
    ('PER-WEBCAM',  'Webcam Full HD 1080p',                 'Micrófono dual con reducción de ruido.',                159.00, 40, 'perifericos'),
    ('AUD-HP-ANC',  'Audífonos over-ear con ANC',           'Cancelación activa de ruido, 30 h de batería.',         399.00, 25, 'audio'),
    ('AUD-EARBUDS', 'Audífonos in-ear Bluetooth',           'Resistentes al agua IPX5.',                             129.00, 60, 'audio'),
    ('MON-27-QHD',  'Monitor 27" QHD IPS 165Hz',            'Ideal para programar con dos ventanas lado a lado.',   1199.00, 15, 'monitores'),
    ('MON-24-FHD',  'Monitor 24" Full HD IPS',              'Bordes delgados y soporte VESA.',                       549.00, 30, 'monitores'),
    ('STO-SSD-1TB', 'SSD NVMe 1TB',                         'Lectura de hasta 5000 MB/s.',                           329.00, 50, 'almacenamiento'),
    ('STO-USB-128', 'Memoria USB 3.2 de 128GB',             'Carcasa metálica.',                                      49.90, 120, 'almacenamiento')
) AS v(sku, name, description, price, stock, category)
JOIN categories c ON c.slug = v.category;
