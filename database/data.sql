-- =====================================================================
-- CarMotors - Sistema de Gestión de Taller (SGT)
-- data.sql : datos de prueba (ejecutar DESPUÉS de schema.sql)
-- =====================================================================

USE carmotors;

-- ---------------------------------------------------------------------
-- 1. TABLAS DE CONSULTA
-- ---------------------------------------------------------------------

INSERT INTO servicio_estados (id, nombre) VALUES
    (1, 'Pendiente'),
    (2, 'En Proceso'),
    (3, 'Completado'),
    (4, 'Facturado'),
    (5, 'Entregado'),
    (6, 'Cancelada');

INSERT INTO mantenimiento_tipos (id, nombre) VALUES
    (1, 'Preventivo'),
    (2, 'Correctivo');

INSERT INTO repuesto_tipos (id, nombre) VALUES
    (1, 'Mecánico'),
    (2, 'Eléctrico'),
    (3, 'Carrocería'),
    (4, 'Consumo');

INSERT INTO especialidades (id, nombre) VALUES
    (1, 'Mecánica general'),
    (2, 'Motor'),
    (3, 'Frenos'),
    (4, 'Eléctrica'),
    (5, 'Suspensión y dirección');

-- ---------------------------------------------------------------------
-- 2. CLIENTES Y VEHÍCULOS
-- ---------------------------------------------------------------------

INSERT INTO clientes (id, nombre, documento_identidad, telefono, email, activo) VALUES
    (1, 'María Fernanda Rojas',      '1094567801',    '3105551101', 'maria.rojas@correo.com',          TRUE),
    (2, 'Carlos Andrés Peña',        '1090123402',    '3115551102', 'carlos.pena@correo.com',          TRUE),
    (3, 'Luisa Marcela Díaz',        '1093456703',    '3125551103', 'luisa.diaz@correo.com',           TRUE),
    (4, 'Jorge Eduardo Castillo',    '88245604',      '3135551104', 'jorge.castillo@correo.com',       TRUE),
    (5, 'Sandra Milena Vega',        '60345205',      '3145551105', 'sandra.vega@correo.com',          TRUE),
    (6, 'Transportes Rápidos S.A.S.','900123456-7',   '3155551106', 'contacto@transportesrapidos.com', TRUE),
    (7, 'Pablo Ortiz',               '1091234507',    '3165551107', 'pablo.ortiz@correo.com',          FALSE),
    (8, 'Valentina Mora', '1092345608', '3175551108', 'valentina.mora@example.com', TRUE),
    (9, 'Mateo Ruiz',     '1092345609', '3185551109', 'mateo.ruiz@example.com', TRUE),
    (10, 'Ana Torres',   '1092345610', '3195551110', 'ana.torres@example.com', TRUE),
    (11, 'Iván Ríos',    '1092345611', NULL,         'ivan.rios@example.com', TRUE),
    (12, 'Elena Núñez',  '1092345612', '3205551112', 'elena.nunez@example.com', TRUE);

INSERT INTO vehiculos (id, cliente_id, placa, marca, modelo) VALUES
    (1, 1, 'ABC123', 'Chevrolet', 'Spark GT'),
    (2, 2, 'DEF456', 'Renault',   'Logan'),
    (3, 3, 'GHI789', 'Mazda',     '3'),
    (4, 4, 'JKL012', 'Kia',       'Rio'),
    (5, 5, 'MNO345', 'Toyota',    'Hilux'),
    (6, 6, 'PQR678', 'Chevrolet', 'NPR'),
    (7, 6, 'STU901', 'Hyundai',   'H100'),
    (8, 1, 'VWX234', 'Renault',   'Duster'),
    (9,  8, 'YZA567', 'Nissan',     'March'),
    (10, 8, 'BCD890', 'Suzuki',     'Vitara'),
    (11, 9, 'EFG123', 'Ford',       'Fiesta'),
    (12, 9, 'HIJ456', 'Mazda',      'CX-5'),
    (13, 10, 'KLM789', 'Kia',       'Picanto'),
    (14, 10, 'NOP012', 'Toyota',    'Corolla'),
    (15, 11, 'QRS345', 'Honda',     'Civic'),
    (16, 11, 'TUV678', 'Hyundai',   'i20'),
    (17, 12, 'WXY901', 'Volkswagen', 'Gol'),
    (18, 7,  'ZAB234', 'Renault',   'Sandero');

-- ---------------------------------------------------------------------
-- 3. TÉCNICOS Y CATÁLOGO DE SERVICIOS
-- ---------------------------------------------------------------------

INSERT INTO tecnicos (id, nombre, documento_identidad, especialidad_id, telefono, activo) VALUES
    (1, 'Pedro Ramírez',   '1090456123', 3, '3205550001', TRUE),   -- Frenos
    (2, 'Juan Pérez',      '1090456124', 2, '3205550002', TRUE),   -- Motor
    (3, 'Laura Gómez',     '1090456125', 4, '3205550003', TRUE),   -- Eléctrica
    (4, 'Carlos Mendoza',  '1090456126', 5, '3205550004', TRUE),   -- Suspensión y dirección
    (5, 'Andrea Torres',   '1090456127', 1, '3205550005', TRUE),   -- Mecánica general
    (7, 'Diego Suárez',    '1090456129', 3, '3205550007', TRUE);   -- Frenos

INSERT INTO servicio_tipos (id, nombre, mantenimiento_tipo_id, especialidad_id, precio_mano_obra_base, intervalo_meses) VALUES
    (1,  'Cambio de aceite y filtro',               1, 1,  60000.00, 6),
    (2,  'Revisión y cambio de pastillas de freno', 2, 3,  90000.00, NULL),
    (3,  'Alineación y balanceo',                   1, 5,  70000.00, 12),
    (4,  'Cambio de batería',                       2, 4,  40000.00, NULL),
    (5,  'Afinación de motor',                      1, 2, 150000.00, 12),
    (6,  'Reparación de suspensión',                2, 5, 180000.00, NULL),
    (8,  'Revisión del sistema eléctrico',          1, 4,  50000.00, 12),
    (10, 'Cambio de líquido de frenos',             1, 3,  55000.00, 24);

-- ---------------------------------------------------------------------
-- 4. PROVEEDORES
-- ---------------------------------------------------------------------

INSERT INTO proveedores (id, nombre, telefono, email, direccion, activo) VALUES
    (1, 'AutoPartes del Norte',      '5775550101', 'ventas@autopartesnorte.com',  'Av. 5 # 12-30, Cúcuta',      TRUE),
    (2, 'Frenos y Más',              '5775550102', 'pedidos@frenosymas.com',      'Calle 10 # 4-25, Cúcuta',    TRUE),
    (3, 'Lubricantes Andinos',       '5775550103', 'comercial@lubriandinos.com',  'Zona Industrial, Cúcuta',    TRUE),
    (4, 'ElectroCar Colombia',       '6015550104', 'info@electrocar.co',          'Cra. 30 # 15-10, Bogotá',    TRUE),
    (5, 'Importadora Sur',           '6025550105', NULL,                          'Calle 9 # 20-15, Cali',      TRUE),
    (6, 'Suministros del Parque', '6075550106', 'ventas@parque.example.com', 'Av. 7 # 10-15, Cúcuta', TRUE),
    (7, 'Repuestos del Valle', '6025550107', 'ventas@valle.example.com', 'Calle 18 # 3-14, Cali', TRUE),
    (8, 'Distribuidora Central', '6015550108', NULL, 'Cra. 20 # 8-23, Bogotá', FALSE);

-- ---------------------------------------------------------------------
-- 5. REPUESTOS
--    El stock de cada repuesto = suma de cantidad_actual de sus lotes (ver sección 6)
--    cantidad_reservada = suma de lo asignado en órdenes Pendiente / En Proceso
-- ---------------------------------------------------------------------

INSERT INTO repuestos (id, repuesto_tipo_id, nombre, marca, precio_venta, cantidad_reservada, stock_minimo, activo) VALUES
    (1,  4, 'Aceite sintético 10W-40 (litro)',  'Mobil',   38000.00, 6, 10, TRUE),
    (2,  4, 'Filtro de aceite',                 'Fram',    25000.00, 1,  5, TRUE),
    (3,  4, 'Filtro de aire',                   'Mann',    32000.00, 1,  4, TRUE),
    (4,  1, 'Pastillas de freno delanteras',    'Bosch',  120000.00, 3,  4, TRUE),
    (5,  1, 'Disco de freno delantero',         'Brembo', 210000.00, 0,  4, TRUE),
    (6,  4, 'Líquido de frenos DOT4 (500 ml)',  'Bosch',   28000.00, 2,  4, TRUE),
    (7,  2, 'Batería 12V 45Ah',                 'Varta',  380000.00, 0,  2, TRUE),
    (8,  2, 'Bujía de iridio',                  'NGK',     45000.00, 0,  8, TRUE),
    (9,  1, 'Amortiguador delantero',           'Monroe', 260000.00, 0,  2, TRUE),
    (10, 3, 'Kit de pintura de retoque',        'Sikkens', 90000.00, 0,  2, TRUE),
    (11, 1, 'Correa de distribución',           'Gates',  150000.00, 0,  2, FALSE),
    (12, 4, 'Filtro de cabina',                 'Mann',    35000.00, 0,  3, TRUE),
    (13, 1, 'Bieleta estabilizadora',   'TRW',     85000.00, 2, 3, TRUE),
    (14, 1, 'Terminal de dirección',   'Moog',   105000.00, 0, 3, TRUE),
    (15, 2, 'Fusible de 15 A',          'Bosch',    4500.00, 0, 5, TRUE),
    (16, 3, 'Grapa de guardabarros',    'Dorman',   3000.00, 0, 4, TRUE),
    (17, 4, 'Refrigerante (litro)',     'Prestone', 33000.00, 0, 5, TRUE),
    (18, 4, 'Limpiador de contactos',   'CRC',     28000.00, 0, 3, TRUE),
    (19, 1, 'Rodamiento delantero',    'SKF',    115000.00, 0, 2, TRUE),
    (20, 2, 'Bombillo H7',              'Osram',   27000.00, 0, 3, TRUE);

-- Catálogo proveedor - repuesto (un solo proveedor habitual por repuesto)
INSERT INTO proveedor_repuestos (proveedor_id, repuesto_id, costo, tiempo_entrega_dias, es_habitual) VALUES
    (3, 1,   24000.00, 2, TRUE),
    (1, 1,   26000.00, 3, FALSE),
    (2, 1,   25500.00, 4, FALSE),
    (3, 2,   15000.00, 2, TRUE),
    (1, 3,   20000.00, 3, TRUE),
    (2, 4,   78000.00, 3, TRUE),
    (1, 4,   85000.00, 4, FALSE),
    (2, 5,  140000.00, 4, TRUE),
    (2, 6,   16000.00, 2, TRUE),
    (4, 7,  260000.00, 3, TRUE),
    (4, 8,   28000.00, 3, TRUE),
    (1, 9,  170000.00, 5, TRUE),
    (1, 10,  55000.00, 4, TRUE),
    (1, 11,  95000.00, 6, TRUE),
    (1, 12,  22000.00, 3, TRUE),
    (6, 13,  52000.00, 3, TRUE),
    (6, 14,  67000.00, 4, TRUE),
    (6, 15,   2400.00, 2, TRUE),
    (7, 16,   1500.00, 5, FALSE),
    (6, 17,  22000.00, 3, TRUE),
    (7, 18,  17200.00, 4, TRUE),
    (7, 19,  74000.00, 5, TRUE),
    (7, 20,  18000.00, 4, FALSE),
    (8, 20,  17500.00, 2, FALSE);

-- ---------------------------------------------------------------------
-- 6. LOTES (el proveedor de cada lote ofrece ese repuesto - RN-14)
-- ---------------------------------------------------------------------

INSERT INTO lotes (id, codigo_lote, proveedor_id, repuesto_id, fecha_ingreso, fecha_caducidad, cantidad_inicial, cantidad_actual) VALUES
    (1,  'LOT-ACE-2026-001', 3, 1,  '2026-03-10', '2028-03-10', 40, 32),
    (2,  'LOT-ACE-2026-002', 3, 1,  '2026-09-15', '2028-09-15', 24, 24),
    (3,  'LOT-FAC-2026-001', 3, 2,  '2026-04-02', NULL,         20, 18),
    (4,  'LOT-FAR-2026-001', 1, 3,  '2026-05-12', NULL,         15, 15),
    (5,  'LOT-PAS-2026-001', 2, 4,  '2026-06-18', NULL,         10, 10),
    (6,  'LOT-DIS-2026-001', 2, 5,  '2026-07-22', NULL,          3,  3),
    (7,  'LOT-LIQ-2026-001', 2, 6,  '2026-06-01', '2026-12-01', 12, 11),
    (8,  'LOT-LIQ-2024-001', 2, 6,  '2024-10-01', '2026-10-01',  2,  2),
    (9,  'LOT-BAT-2026-001', 4, 7,  '2026-08-20', NULL,          6,  4),
    (10, 'LOT-BUJ-2026-001', 4, 8,  '2026-05-30', NULL,         24, 19),
    (11, 'LOT-AMO-2026-001', 1, 9,  '2026-07-05', NULL,          8,  8),
    (12, 'LOT-PIN-2026-001', 1, 10, '2026-08-01', '2027-08-01',  6,  6),
    (13, 'LOT-COR-2026-001', 1, 11, '2026-04-20', NULL,          5,  5),
    (14, 'LOT-FCA-2026-001', 1, 12, '2026-09-02', NULL,         10, 10),
    (15, 'LOT-ACE-2026-003', 1, 1,  '2026-10-16', NULL,  8, 8),
    (16, 'LOT-FAC-2026-002', 3, 2,  '2026-10-16', NULL, 10, 10),
    (17, 'LOT-PAS-2026-002', 1, 4,  '2026-10-16', NULL,  8, 8),
    -- Vencido con saldo y próximo a vencer en cualquier fecha de carga.
    (18, 'LOT-DIS-VENCIDO',  2, 5,  CURRENT_DATE - INTERVAL 90 DAY, CURRENT_DATE - INTERVAL 1 DAY, 4, 4),
    (19, 'LOT-LIQ-PROXIMO',  2, 6,  CURRENT_DATE - INTERVAL 20 DAY, CURRENT_DATE + INTERVAL 10 DAY, 8, 8),
    (20, 'LOT-LIQ-FUTURO',   2, 6,  CURRENT_DATE - INTERVAL 20 DAY, CURRENT_DATE + INTERVAL 180 DAY, 9, 9),
    (21, 'LOT-BAT-2026-002', 4, 7,  '2026-10-16', NULL,  7, 7),
    (22, 'LOT-BUJ-2026-002', 4, 8,  '2026-10-16', NULL,  8, 8),
    (23, 'LOT-BIE-2026-001', 6, 13, '2026-10-16', NULL,  8, 8),
    (24, 'LOT-TER-2026-001', 6, 14, '2026-10-16', NULL,  9, 8),
    (25, 'LOT-FUS-2026-001', 6, 15, '2026-10-16', NULL, 20, 20),
    (26, 'LOT-GRA-2026-001', 7, 16, '2026-10-16', NULL, 12, 12),
    (27, 'LOT-REF-FUTURO',   6, 17, CURRENT_DATE - INTERVAL 30 DAY, CURRENT_DATE + INTERVAL 270 DAY, 5, 5),
    (28, 'LOT-LIM-FUTURO',   7, 18, CURRENT_DATE - INTERVAL 30 DAY, CURRENT_DATE + INTERVAL 365 DAY, 6, 6),
    (29, 'LOT-ROD-2026-001', 7, 19, '2026-10-16', NULL,  6, 6),
    (30, 'LOT-BOM-2026-001', 8, 20, '2026-02-25', NULL,  6, 6);

-- ---------------------------------------------------------------------
-- 7. EVALUACIONES DE PROVEEDORES (la Importadora Sur queda sin evaluar)
-- ---------------------------------------------------------------------

INSERT INTO evaluaciones_proveedor (proveedor_id, fecha, calidad, puntualidad, comentario) VALUES
    (1, '2026-06-20', 4, 5, 'Buen servicio y entregas a tiempo'),
    (1, '2026-09-10', 5, 4, NULL),
    (2, '2026-08-05', 5, 5, 'Excelente calidad en frenos'),
    (3, '2026-07-12', 4, 3, 'Entrega con un día de retraso'),
    (4, '2026-09-01', 3, 4, 'Producto correcto, empaque dañado'),
    (2, '2026-09-14', 3, 3, 'Retraso en el pedido'),
    (3, '2026-10-05', 5, 5, 'Pedido completo y a tiempo'),
    (4, '2026-10-07', 4, 4, 'Entrega completa'),
    (6, '2026-10-17', 5, 5, 'Terminales sin observaciones'),
    (6, '2026-10-25', 4, 3, 'Despacho parcial'),
    (7, '2026-10-18', 4, 5, 'Entrega al día siguiente'),
    (8, '2026-02-26', 3, 4, 'Evaluación anterior a su desactivación');

-- ---------------------------------------------------------------------
-- 8. ÓRDENES DE SERVICIO (3 Pendientes, 2 En Proceso, 2 Completadas,
--    1 Facturada, 3 Entregadas y 1 Cancelada)
-- ---------------------------------------------------------------------

-- La orden 12 deja un recordatorio preventivo próximo al cargar los datos.
SET @entrega_recordatorio = CURRENT_DATE - INTERVAL 12 MONTH + INTERVAL 10 DAY;

INSERT INTO ordenes_servicio (id, vehiculo_id, estado_id, fecha_recepcion, fecha_completado, fecha_entrega, fecha_cancelacion, descripcion_problema) VALUES
    (1, 1, 1, '2026-11-12 08:30:00', NULL,                  NULL,                  NULL,                  'Cambio de aceite por kilometraje'),
    (2, 2, 2, '2026-11-11 09:15:00', NULL,                  NULL,                  NULL,                  'Ruido al frenar y vibración en el volante'),
    (3, 3, 3, '2026-11-10 10:00:00', '2026-11-12 16:45:00', NULL,                  NULL,                  'El vehículo no enciende en las mañanas'),
    (4, 4, 4, '2026-11-05 08:00:00', '2026-11-06 15:30:00', NULL,                  NULL,                  'Mantenimiento general de los 20.000 km'),
    (5, 5, 5, '2026-05-27 09:00:00', '2026-05-28 11:00:00', '2026-05-29 10:00:00', NULL,                  'Cambio de aceite programado'),
    (6, 6, 6, '2026-10-20 14:00:00', NULL,                  NULL,                  '2026-10-21 09:00:00', 'Alineación; el cliente canceló la visita'),
    (7, 9, 1, '2026-11-13 08:15:00', NULL, NULL, NULL, 'Mantenimiento preventivo antes de un viaje'),
    (8, 11, 1, '2026-11-14 10:00:00', NULL, NULL, NULL, 'Golpeteo en la suspensión'),
    (9, 13, 2, '2026-11-13 14:00:00', NULL, NULL, NULL, 'Revisión de frenos y cambio de líquido'),
    (10, 15, 3, '2026-11-08 07:30:00', '2026-11-09 16:10:00', NULL, NULL, 'Vibración en dirección y marcha irregular'),
    (11, 7, 5, '2026-11-01 08:00:00', '2026-11-02 10:00:00', '2026-11-02 16:00:00', NULL, 'Mantenimiento de dirección, frenos y batería'),
    (12, 8, 5, TIMESTAMP(@entrega_recordatorio - INTERVAL 2 DAY, '08:00:00'),
                TIMESTAMP(@entrega_recordatorio - INTERVAL 1 DAY, '11:00:00'),
                TIMESTAMP(@entrega_recordatorio, '10:00:00'), NULL, 'Alineación preventiva sin repuestos');

INSERT INTO orden_servicio_detalle (id, orden_id, servicio_tipo_id, nombre_servicio_snapshot, costo_mano_obra, observaciones) VALUES
    (1, 1, 1, 'Cambio de aceite y filtro',               60000.00, NULL),
    (2, 2, 2, 'Revisión y cambio de pastillas de freno', 90000.00, 'Revisar también los discos'),
    (3, 2, 3, 'Alineación y balanceo',                   70000.00, NULL),
    (4, 3, 4, 'Cambio de batería',                       40000.00, 'Batería sulfatada'),
    (5, 4, 1, 'Cambio de aceite y filtro',               60000.00, NULL),
    (6, 4, 5, 'Afinación de motor',                     150000.00, 'Incluye cambio de bujías'),
    (7, 5, 1, 'Cambio de aceite y filtro',               60000.00, NULL),
    (8, 6, 3, 'Alineación y balanceo',                   70000.00, NULL),
    (9, 1, 3, 'Alineación y balanceo',                   70000.00, 'Línea adicional para probar el retiro con reservas'),
    (10, 2, 2, 'Revisión y cambio de pastillas de freno', 90000.00, 'Trabajo aún sin horas registradas'),
    (11, 7, 1, 'Cambio de aceite y filtro',                60000.00, NULL),
    (12, 7, 5, 'Afinación de motor',                      150000.00, 'Incluye filtro de aire'),
    (13, 8, 6, 'Reparación de suspensión',                180000.00, NULL),
    (14, 8, 3, 'Alineación y balanceo',                    70000.00, NULL),
    (15, 9, 2, 'Revisión y cambio de pastillas de freno',  90000.00, NULL),
    (16, 9, 10, 'Cambio de líquido de frenos',             55000.00, NULL),
    (17, 10, 6, 'Reparación de suspensión',               180000.00, NULL),
    (18, 10, 5, 'Afinación de motor',                     150000.00, NULL),
    (19, 11, 3, 'Alineación y balanceo',                    70000.00, NULL),
    (20, 11, 10, 'Cambio de líquido de frenos',             55000.00, NULL),
    (21, 11, 4, 'Cambio de batería',                       40000.00, NULL),
    (22, 12, 3, 'Alineación y balanceo',                    70000.00, NULL);

-- Técnicos por línea. Pedro y Diego empatan con dos líneas abiertas de Frenos;
-- la línea 17 combina dos técnicos con horas distintas para probar productividad.
INSERT INTO detalle_tecnicos (detalle_id, tecnico_id, horas_trabajadas) VALUES
    (2, 1, 2.00),
    (3, 4, 1.50),
    (4, 3, 1.00),
    (5, 5, 1.00),
    (6, 2, 3.00),
    (7, 5, 1.00),
    (10, 1, 0.00),
    (15, 7, 1.25),
    (16, 7, 0.75),
    (17, 4, 2.00),
    (17, 5, 1.00),   -- Apoyo de mecánica general con otra especialidad confirmada.
    (18, 2, 1.50),
    (19, 4, 1.00),
    (20, 1, 0.75),
    (21, 3, 0.50),
    (22, 4, 0.00);

-- Repuestos por línea (el precio queda copiado al asignar - RN-11)
INSERT INTO detalle_repuestos (detalle_id, repuesto_id, cantidad_asignada, nombre_repuesto_snapshot, precio_unitario_venta) VALUES
    (1, 1, 4, 'Aceite sintético 10W-40 (litro)', 38000.00),
    (1, 2, 1, 'Filtro de aceite',                25000.00),
    (2, 4, 1, 'Pastillas de freno delanteras',  120000.00),
    (2, 6, 1, 'Líquido de frenos DOT4 (500 ml)', 28000.00),
    (4, 7, 1, 'Batería 12V 45Ah',               380000.00),
    (5, 1, 4, 'Aceite sintético 10W-40 (litro)', 38000.00),
    (5, 2, 1, 'Filtro de aceite',                25000.00),
    (6, 8, 4, 'Bujía de iridio',                 45000.00),
    (7, 1, 4, 'Aceite sintético 10W-40 (litro)', 38000.00),
    (7, 2, 1, 'Filtro de aceite',                25000.00),
    (8, 9, 1,  'Amortiguador delantero',          260000.00), -- Reserva liberada al cancelar.
    (11, 1, 2, 'Aceite sintético 10W-40 (litro)',    38000.00),
    (12, 3, 1, 'Filtro de aire',                    32000.00),
    (13, 13, 2, 'Bieleta estabilizadora',            85000.00),
    (15, 4, 2, 'Pastillas de freno delanteras',     120000.00),
    (16, 6, 1, 'Líquido de frenos DOT4 (500 ml)',    28000.00),
    (17, 14, 1, 'Terminal de dirección',           105000.00),
    (18, 8, 1, 'Bujía de iridio',                   45000.00),
    (20, 6, 1, 'Líquido de frenos DOT4 (500 ml)',    26000.00),
    (21, 7, 1, 'Batería 12V 45Ah',                 350000.00);

-- ---------------------------------------------------------------------
-- 9. FACTURAS (IVA 19 %, RN-12)
--    Factura 1: orden 5 -> repuestos 177000 + mano obra 60000 = 237000; IVA 45030; total 282030
--    Factura 2: orden 4 -> repuestos 357000 + mano obra 210000 = 567000; IVA 107730; total 674730
--    Factura 3: orden 11 -> repuestos 376000 + mano obra 165000 = 541000; IVA 102790; total 643790
--    Factura 4: orden 12 -> solo mano de obra 70000; IVA 13300; total 83300
--    Todas quedan pendientes de generar TXT porque esta carga solo escribe en MySQL.
-- ---------------------------------------------------------------------

INSERT INTO facturas (id, numero_factura, orden_id, fecha_emision, total_repuestos, total_mano_obra, subtotal, tasa_iva, impuestos, total_final, archivo_generado) VALUES
    (1, 'F-2026-0001', 5, '2026-05-28 12:00:00', 177000.00,  60000.00, 237000.00, 0.1900,  45030.00, 282030.00, FALSE),
    (2, 'F-2026-0002', 4, '2026-11-06 16:00:00', 357000.00, 210000.00, 567000.00, 0.1900, 107730.00, 674730.00, FALSE),
    (3, 'F-2026-0003', 11, '2026-11-02 11:00:00', 376000.00, 165000.00, 541000.00, 0.1900, 102790.00, 643790.00, FALSE),
    (4, CONCAT('F-', YEAR(@entrega_recordatorio), '-0004'), 12, TIMESTAMP(@entrega_recordatorio, '09:00:00'),
        0.00, 70000.00, 70000.00, 0.1900, 13300.00, 83300.00, FALSE);

INSERT INTO factura_snapshot (factura_id, cliente_nombre_snapshot, cliente_documento_snapshot, cliente_telefono_snapshot, cliente_email_snapshot, vehiculo_placa_snapshot, vehiculo_marca_snapshot, vehiculo_modelo_snapshot) VALUES
    (1, 'Sandra Milena Vega',     '60345205', '3145551105', 'sandra.vega@correo.com',    'MNO345', 'Toyota', 'Hilux'),
    (2, 'Jorge Eduardo Castillo', '88245604', '3135551104', 'jorge.castillo@correo.com', 'JKL012', 'Kia',    'Rio'),
    (3, 'Transportes Rápidos S.A.S.', '900123456-7', '3155551106', 'contacto@transportesrapidos.com', 'STU901', 'Hyundai', 'H100'),
    (4, 'María Fernanda Rojas', '1094567801', '3105551101', 'maria.rojas@correo.com', 'VWX234', 'Renault', 'Duster');

INSERT INTO factura_detalles (id, factura_id, orden_detalle_id, repuesto_id, tipo_concepto, descripcion_snapshot, cantidad, precio_unitario, subtotal) VALUES
    -- Factura 1 (orden 5)
    (1, 1, 7, NULL, 'MANO_OBRA', 'Cambio de aceite y filtro',         1,  60000.00,  60000.00),
    (2, 1, 7, 1,    'REPUESTO',  'Aceite sintético 10W-40 (litro)',   4,  38000.00, 152000.00),
    (3, 1, 7, 2,    'REPUESTO',  'Filtro de aceite',                  1,  25000.00,  25000.00),
    -- Factura 2 (orden 4)
    (4, 2, 5, NULL, 'MANO_OBRA', 'Cambio de aceite y filtro',         1,  60000.00,  60000.00),
    (5, 2, 5, 1,    'REPUESTO',  'Aceite sintético 10W-40 (litro)',   4,  38000.00, 152000.00),
    (6, 2, 5, 2,    'REPUESTO',  'Filtro de aceite',                  1,  25000.00,  25000.00),
    (7, 2, 6, NULL, 'MANO_OBRA', 'Afinación de motor',                1, 150000.00, 150000.00),
    (8, 2, 6, 8,    'REPUESTO',  'Bujía de iridio',                   4,  45000.00, 180000.00),
    -- Factura 3 (orden 11)
    (9,  3, 19, NULL, 'MANO_OBRA', 'Alineación y balanceo',                 1,  70000.00,  70000.00),
    (10, 3, 20, NULL, 'MANO_OBRA', 'Cambio de líquido de frenos',           1,  55000.00,  55000.00),
    (11, 3, 20, 6,    'REPUESTO',  'Líquido de frenos DOT4 (500 ml)',       1,  26000.00,  26000.00),
    (12, 3, 21, NULL, 'MANO_OBRA', 'Cambio de batería',                     1,  40000.00,  40000.00),
    (13, 3, 21, 7,    'REPUESTO',  'Batería 12V 45Ah',                      1, 350000.00, 350000.00),
    -- Factura 4 (orden 12, solo mano de obra)
    (14, 4, 22, NULL, 'MANO_OBRA', 'Alineación y balanceo',                  1,  70000.00,  70000.00);
