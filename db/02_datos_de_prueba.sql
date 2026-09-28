-- SIGTOA - Datos de prueba.
-- Ejecutar una sola vez sobre las tablas vacías creadas por 01_schema.sql.
USE agro;
SET NAMES utf8mb4;

START TRANSACTION;

-- ---------------------------------------------------------------------
-- Usuarios para la auditoría de los ejemplos.
-- ---------------------------------------------------------------------
INSERT INTO usuarios (nombre, usuario_login, password_hash, activo)
VALUES ('Encargado de prueba', 'demo_encargado', 'SIN_LOGIN_DATOS_DE_PRUEBA', 0);
SET @encargado = LAST_INSERT_ID();

INSERT INTO usuarios (nombre, usuario_login, password_hash, activo)
VALUES ('Ingeniero de prueba', 'demo_ingeniero', 'SIN_LOGIN_DATOS_DE_PRUEBA', 0);
SET @ingeniero = LAST_INSERT_ID();

-- ---------------------------------------------------------------------
-- Campañas
-- ---------------------------------------------------------------------
INSERT INTO campanas (nombre) VALUES ('24-25');
SET @camp2425 = LAST_INSERT_ID();

INSERT INTO campanas (nombre) VALUES ('25-26');
SET @camp2526 = LAST_INSERT_ID();

-- ---------------------------------------------------------------------
-- Establecimientos y lotes
-- ---------------------------------------------------------------------
INSERT INTO establecimientos (nombre) VALUES ('ESTANCIA CHARRA');
SET @norte = LAST_INSERT_ID();

INSERT INTO establecimientos (nombre) VALUES ('EL CARMEN');
SET @sur = LAST_INSERT_ID();

INSERT INTO establecimientos (nombre) VALUES ('TORRE');
SET @este = LAST_INSERT_ID();

INSERT INTO lotes (establecimiento_id, nombre, hectareas) VALUES (@norte, '6 NORTE', 20.00);
SET @lote6 = LAST_INSERT_ID();
INSERT INTO lotes (establecimiento_id, nombre, hectareas) VALUES (@norte, '7 NORTE', 15.00);
SET @lote7 = LAST_INSERT_ID();
INSERT INTO lotes (establecimiento_id, nombre, hectareas) VALUES (@norte, '8 NORTE', 18.00);
SET @lote8 = LAST_INSERT_ID();
INSERT INTO lotes (establecimiento_id, nombre, hectareas) VALUES (@norte, '9 NORTE', 22.00);
SET @lote9 = LAST_INSERT_ID();

INSERT INTO lotes (establecimiento_id, nombre, hectareas) VALUES (@sur, 'EL BAJO', 10.00);
SET @bajo = LAST_INSERT_ID();
-- Este lote queda sin operaciones para comprobar las consultas.
INSERT INTO lotes (establecimiento_id, nombre, hectareas) VALUES (@sur, 'RESERVA', 5.00);
INSERT INTO lotes (establecimiento_id, nombre, hectareas) VALUES (@sur, 'LA LOMA', 12.00);
SET @loma = LAST_INSERT_ID();
INSERT INTO lotes (establecimiento_id, nombre, hectareas) VALUES (@sur, 'EL MEDIO', 14.00);
SET @medio = LAST_INSERT_ID();

INSERT INTO lotes (establecimiento_id, nombre, hectareas) VALUES (@este, 'LOS ALGARROBOS', 25.00);
SET @algarrobos = LAST_INSERT_ID();
INSERT INTO lotes (establecimiento_id, nombre, hectareas) VALUES (@este, 'EL POTRERO', 16.00);
SET @potrero = LAST_INSERT_ID();
INSERT INTO lotes (establecimiento_id, nombre, hectareas) VALUES (@este, 'SANTA ROSA', 30.00);
SET @santarosa = LAST_INSERT_ID();

-- ---------------------------------------------------------------------
-- Contratistas, depósitos y acopios
-- ---------------------------------------------------------------------
INSERT INTO contratistas (nombre) VALUES ('Contratista de prueba');
SET @contratista1 = LAST_INSERT_ID();
INSERT INTO contratistas (nombre) VALUES ('AgroServicios del Sur');
SET @contratista2 = LAST_INSERT_ID();
INSERT INTO contratistas (nombre) VALUES ('Fumigaciones San Martín');
SET @contratista3 = LAST_INSERT_ID();

INSERT INTO depositos (nombre) VALUES ('Depósito central');
SET @deposito1 = LAST_INSERT_ID();
INSERT INTO depositos (nombre) VALUES ('Depósito Sur');
SET @deposito2 = LAST_INSERT_ID();

INSERT INTO acopios (nombre) VALUES ('Silo propio');
SET @acopio1 = LAST_INSERT_ID();
INSERT INTO acopios (nombre) VALUES ('Acopio Cooperativa');
SET @acopio2 = LAST_INSERT_ID();

-- ---------------------------------------------------------------------
-- Labores y cultivos
-- ---------------------------------------------------------------------
INSERT INTO labores (nombre) VALUES ('Pulverización terrestre');
SET @pulv_terrestre = LAST_INSERT_ID();
INSERT INTO labores (nombre) VALUES ('Pulverización aérea');
SET @pulv_aerea = LAST_INSERT_ID();
INSERT INTO labores (nombre) VALUES ('Labranza');
SET @labranza = LAST_INSERT_ID();
INSERT INTO labores (nombre) VALUES ('Fertilización');
SET @fertilizacion = LAST_INSERT_ID();

INSERT INTO cultivos (nombre) VALUES ('Maíz');
SET @maiz = LAST_INSERT_ID();
INSERT INTO cultivos (nombre) VALUES ('Soja');
SET @soja = LAST_INSERT_ID();
INSERT INTO cultivos (nombre) VALUES ('Trigo');
SET @trigo = LAST_INSERT_ID();
INSERT INTO cultivos (nombre) VALUES ('Girasol');
SET @girasol = LAST_INSERT_ID();

-- ---------------------------------------------------------------------
-- Grupos de insumo, principios activos e insumos
-- ---------------------------------------------------------------------
INSERT INTO grupos_insumo (nombre, cultivo_id) VALUES ('Semillas de maíz', @maiz);
SET @grupo_maiz = LAST_INSERT_ID();
INSERT INTO grupos_insumo (nombre, cultivo_id) VALUES ('Semillas de soja', @soja);
SET @grupo_soja = LAST_INSERT_ID();
INSERT INTO grupos_insumo (nombre, cultivo_id) VALUES ('Semillas de trigo', @trigo);
SET @grupo_trigo = LAST_INSERT_ID();
INSERT INTO grupos_insumo (nombre, cultivo_id) VALUES ('Semillas de girasol', @girasol);
SET @grupo_girasol = LAST_INSERT_ID();
INSERT INTO grupos_insumo (nombre, cultivo_id) VALUES ('Herbicidas', NULL);
SET @grupo_herbicida = LAST_INSERT_ID();
INSERT INTO grupos_insumo (nombre, cultivo_id) VALUES ('Fungicidas', NULL);
SET @grupo_fungicida = LAST_INSERT_ID();
INSERT INTO grupos_insumo (nombre, cultivo_id) VALUES ('Insecticidas', NULL);
SET @grupo_insecticida = LAST_INSERT_ID();
INSERT INTO grupos_insumo (nombre, cultivo_id) VALUES ('Fertilizantes', NULL);
SET @grupo_fertilizante = LAST_INSERT_ID();

INSERT INTO principios_activos (grupo_id, nombre) VALUES (@grupo_herbicida, 'Glifosato');
SET @pa_glifosato = LAST_INSERT_ID();
INSERT INTO principios_activos (grupo_id, nombre) VALUES (@grupo_herbicida, '2,4-D');
SET @pa_24d = LAST_INSERT_ID();
INSERT INTO principios_activos (grupo_id, nombre) VALUES (@grupo_fungicida, 'Azoxistrobina');
SET @pa_azoxi = LAST_INSERT_ID();
INSERT INTO principios_activos (grupo_id, nombre) VALUES (@grupo_insecticida, 'Cipermetrina');
SET @pa_ciper = LAST_INSERT_ID();

-- Semillas
INSERT INTO insumos (grupo_id, principio_activo_id, nombre) VALUES (@grupo_maiz, NULL, 'Maíz A');
SET @maizA = LAST_INSERT_ID();
INSERT INTO insumos (grupo_id, principio_activo_id, nombre) VALUES (@grupo_maiz, NULL, 'Maíz B');
SET @maizB = LAST_INSERT_ID();
INSERT INTO insumos (grupo_id, principio_activo_id, nombre) VALUES (@grupo_soja, NULL, 'Soja A');
SET @sojaA = LAST_INSERT_ID();
INSERT INTO insumos (grupo_id, principio_activo_id, nombre) VALUES (@grupo_soja, NULL, 'Soja B');
SET @sojaB = LAST_INSERT_ID();
INSERT INTO insumos (grupo_id, principio_activo_id, nombre) VALUES (@grupo_trigo, NULL, 'Trigo A');
SET @trigoA = LAST_INSERT_ID();
INSERT INTO insumos (grupo_id, principio_activo_id, nombre) VALUES (@grupo_girasol, NULL, 'Girasol A');
SET @girasolA = LAST_INSERT_ID();

-- Fitosanitarios
INSERT INTO insumos (grupo_id, principio_activo_id, nombre)
VALUES (@grupo_herbicida, @pa_glifosato, 'Herbicida de prueba');
SET @herb1 = LAST_INSERT_ID();
INSERT INTO insumos (grupo_id, principio_activo_id, nombre)
VALUES (@grupo_herbicida, @pa_24d, 'Herbicida hormonal');
SET @herb2 = LAST_INSERT_ID();
INSERT INTO insumos (grupo_id, principio_activo_id, nombre)
VALUES (@grupo_fungicida, @pa_azoxi, 'Fungicida foliar');
SET @fungicida1 = LAST_INSERT_ID();
INSERT INTO insumos (grupo_id, principio_activo_id, nombre)
VALUES (@grupo_insecticida, @pa_ciper, 'Insecticida piretroide');
SET @insecticida1 = LAST_INSERT_ID();

-- Fertilizantes (sin principio activo)
INSERT INTO insumos (grupo_id, principio_activo_id, nombre)
VALUES (@grupo_fertilizante, NULL, 'Fosfato monoamónico');
SET @fertil_map = LAST_INSERT_ID();
INSERT INTO insumos (grupo_id, principio_activo_id, nombre)
VALUES (@grupo_fertilizante, NULL, 'Urea granulada');
SET @fertil_urea = LAST_INSERT_ID();

-- =======================================================================
-- CAMPAÑA 24-25 (histórica, todas con cosecha ya cargada)
-- =======================================================================

-- Siembra 1: 6 NORTE, maíz, con datos técnicos completos.
INSERT INTO siembras (
    campana_id, lote_id, semilla_insumo_id, fecha_inicio, fecha_fin,
    hectareas_monitor, tractor, marcha_trabajo, velocidad_siembra,
    sembradora, engranaje_conductor, engranaje_conducido, placa,
    grano_por_metro, semilla_total_kg, calibre, desvio,
    tipo_fertilizante, fertilizante_kg_ha, marcha_caja_fertilizante,
    comentarios, creado_por
) VALUES (
    @camp2425, @lote6, @maizA, '2024-10-10', '2024-10-11',
    20.00, 'Tractor de prueba', 4, 6.20,
    'Sembradora de prueba', 17, 24, 'P-10',
    6.20, 580.00, 0.85, 2.50,
    'Fosfato monoamónico', 75.00, 4,
    'Campaña 24-25, siembra de referencia histórica.', @encargado
);
SET @s1 = LAST_INSERT_ID();

-- Siembra 2: 7 NORTE, soja, datos mínimos.
INSERT INTO siembras (
    campana_id, lote_id, semilla_insumo_id,
    fecha_inicio, fecha_fin, hectareas_monitor, semilla_total_kg, creado_por
) VALUES (
    @camp2425, @lote7, @sojaA,
    '2024-11-05', '2024-11-06', 15.00, 720.00, @encargado
);
SET @s2 = LAST_INSERT_ID();

-- Siembra 3: EL BAJO, trigo (cultivo de invierno, se siembra en junio).
INSERT INTO siembras (
    campana_id, lote_id, semilla_insumo_id,
    fecha_inicio, fecha_fin, hectareas_monitor, semilla_total_kg, creado_por
) VALUES (
    @camp2425, @bajo, @trigoA,
    '2024-06-15', '2024-06-16', 10.00, 1000.00, @encargado
);
SET @s3 = LAST_INSERT_ID();

-- Siembra 4: LA LOMA, girasol, datos mínimos.
INSERT INTO siembras (
    campana_id, lote_id, semilla_insumo_id,
    fecha_inicio, fecha_fin, hectareas_monitor, semilla_total_kg, creado_por
) VALUES (
    @camp2425, @loma, @girasolA,
    '2024-10-20', '2024-10-21', 12.00, 60.00, @encargado
);
SET @s4 = LAST_INSERT_ID();

-- Siembra 5: LOS ALGARROBOS, maíz, con datos técnicos completos.
INSERT INTO siembras (
    campana_id, lote_id, semilla_insumo_id, fecha_inicio, fecha_fin,
    hectareas_monitor, tractor, marcha_trabajo, velocidad_siembra,
    sembradora, engranaje_conductor, engranaje_conducido, placa,
    grano_por_metro, semilla_total_kg, calibre, desvio,
    tipo_fertilizante, fertilizante_kg_ha, marcha_caja_fertilizante,
    comentarios, creado_por
) VALUES (
    @camp2425, @algarrobos, @maizA, '2024-10-25', '2024-10-26',
    25.00, 'Tractor de prueba', 5, 6.80,
    'Sembradora de prueba', 17, 24, 'P-11',
    6.00, 700.00, 0.85, 2.00,
    'Urea granulada', 90.00, 5,
    'Lote grande, requiere dos tolvas.', @ingeniero
);
SET @s5 = LAST_INSERT_ID();

-- Cosechas de la campaña 24-25.
-- s1 (6 NORTE, maíz): netos 19000 y 18500 kg -> total 37500 kg / 20 ha = 1875.0 kg/ha.
INSERT INTO cosechas (siembra_id, fecha_inicio, fecha_fin, hectareas, humedad, acopio_id, comentarios, creado_por)
VALUES (@s1, '2025-03-18', '2025-03-19', 20.00, 15.50, @acopio1, 'Cosecha con dos camiones.', @encargado);
SET @c1 = LAST_INSERT_ID();
INSERT INTO pesajes (cosecha_id, orden_llegada, peso_bruto, peso_tara)
VALUES (@c1, 1, 28000.00, 9000.00), (@c1, 2, 27500.00, 9000.00);

-- s2 (7 NORTE, soja): netos 14000 y 13500 kg -> total 27500 kg / 15 ha = 1833.3 kg/ha.
INSERT INTO cosechas (siembra_id, fecha_inicio, fecha_fin, hectareas, humedad, acopio_id, comentarios, creado_por)
VALUES (@s2, '2025-04-02', '2025-04-03', 15.00, 13.00, @acopio2, NULL, @encargado);
SET @c2 = LAST_INSERT_ID();
INSERT INTO pesajes (cosecha_id, orden_llegada, peso_bruto, peso_tara)
VALUES (@c2, 1, 21000.00, 7000.00), (@c2, 2, 20500.00, 7000.00);

-- s3 (EL BAJO, trigo): netos 16000 y 15000 kg -> total 31000 kg / 10 ha = 3100.0 kg/ha.
INSERT INTO cosechas (siembra_id, fecha_inicio, fecha_fin, hectareas, humedad, acopio_id, comentarios, creado_por)
VALUES (@s3, '2024-12-05', '2024-12-06', 10.00, 12.00, @acopio1, NULL, @encargado);
SET @c3 = LAST_INSERT_ID();
INSERT INTO pesajes (cosecha_id, orden_llegada, peso_bruto, peso_tara)
VALUES (@c3, 1, 20000.00, 4000.00), (@c3, 2, 19000.00, 4000.00);

-- s4 (LA LOMA, girasol): netos 10000 y 9500 kg -> total 19500 kg / 12 ha = 1625.0 kg/ha.
INSERT INTO cosechas (siembra_id, fecha_inicio, fecha_fin, hectareas, humedad, acopio_id, comentarios, creado_por)
VALUES (@s4, '2025-03-10', '2025-03-11', 12.00, 9.00, @acopio2, NULL, @encargado);
SET @c4 = LAST_INSERT_ID();
INSERT INTO pesajes (cosecha_id, orden_llegada, peso_bruto, peso_tara)
VALUES (@c4, 1, 14000.00, 4000.00), (@c4, 2, 13500.00, 4000.00);

-- s5 (LOS ALGARROBOS, maíz): netos 20000, 21000 y 19500 kg -> total 60500 kg / 25 ha = 2420.0 kg/ha.
INSERT INTO cosechas (siembra_id, fecha_inicio, fecha_fin, hectareas, humedad, acopio_id, comentarios, creado_por)
VALUES (@s5, '2025-04-15', '2025-04-16', 25.00, 16.00, @acopio1, 'Cosecha con tres camiones.', @ingeniero);
SET @c5 = LAST_INSERT_ID();
INSERT INTO pesajes (cosecha_id, orden_llegada, peso_bruto, peso_tara)
VALUES (@c5, 1, 30000.00, 10000.00), (@c5, 2, 31000.00, 10000.00), (@c5, 3, 29500.00, 10000.00);

-- =======================================================================
-- CAMPAÑA 25-26 (actual: algunas ya cosechadas, otras todavía en pie)
-- =======================================================================

-- Siembra 6: 6 NORTE, maíz, con datos técnicos completos (caso de referencia).
INSERT INTO siembras (
    campana_id, lote_id, semilla_insumo_id, fecha_inicio, fecha_fin,
    hectareas_monitor, tractor, marcha_trabajo, velocidad_siembra,
    sembradora, engranaje_conductor, engranaje_conducido, placa,
    grano_por_metro, semilla_total_kg, calibre, desvio,
    tipo_fertilizante, fertilizante_kg_ha, marcha_caja_fertilizante,
    comentarios, creado_por
) VALUES (
    @camp2526, @lote6, @maizA, '2025-10-15', '2025-10-16',
    20.00, 'Tractor de prueba', 4, 6.50,
    'Sembradora de prueba', 17, 24, 'P-12',
    6.00, 600.00, 0.85, 3.00,
    'Fosfato monoamónico', 80.00, 4,
    'Datos ficticios para el TP2.', @encargado
);
SET @s6 = LAST_INSERT_ID();

-- Siembra 7: 7 NORTE, soja, todavía sin cosechar.
INSERT INTO siembras (
    campana_id, lote_id, semilla_insumo_id,
    fecha_inicio, fecha_fin, hectareas_monitor, semilla_total_kg, creado_por
) VALUES (
    @camp2526, @lote7, @sojaA,
    '2025-11-01', '2025-11-02', 15.00, 750.00, @encargado
);
SET @s7 = LAST_INSERT_ID();

-- Siembra 8: EL MEDIO, trigo.
INSERT INTO siembras (
    campana_id, lote_id, semilla_insumo_id,
    fecha_inicio, fecha_fin, hectareas_monitor, semilla_total_kg, creado_por
) VALUES (
    @camp2526, @medio, @trigoA,
    '2025-06-10', '2025-06-11', 14.00, 1400.00, @encargado
);
SET @s8 = LAST_INSERT_ID();

-- Siembra 9: EL POTRERO, girasol, todavía sin cosechar.
INSERT INTO siembras (
    campana_id, lote_id, semilla_insumo_id,
    fecha_inicio, fecha_fin, hectareas_monitor, semilla_total_kg, creado_por
) VALUES (
    @camp2526, @potrero, @girasolA,
    '2025-10-18', '2025-10-19', 16.00, 80.00, @encargado
);
SET @s9 = LAST_INSERT_ID();

-- Siembra 10: SANTA ROSA, maíz (variedad B), con datos técnicos.
INSERT INTO siembras (
    campana_id, lote_id, semilla_insumo_id, fecha_inicio, fecha_fin,
    hectareas_monitor, tractor, marcha_trabajo, velocidad_siembra,
    sembradora, engranaje_conductor, engranaje_conducido, placa,
    grano_por_metro, semilla_total_kg, calibre, desvio,
    tipo_fertilizante, fertilizante_kg_ha, marcha_caja_fertilizante,
    comentarios, creado_por
) VALUES (
    @camp2526, @santarosa, @maizB, '2025-10-05', '2025-10-06',
    30.00, 'Tractor 2', 5, 7.00,
    'Sembradora 2', 18, 22, 'P-20',
    6.50, 840.00, 0.82, 2.80,
    'Urea granulada', 95.00, 5,
    'Lote más nuevo, monitor recién calibrado.', @ingeniero
);
SET @s10 = LAST_INSERT_ID();

-- Siembra 11: LOS ALGARROBOS, soja (variedad B), todavía sin cosechar.
INSERT INTO siembras (
    campana_id, lote_id, semilla_insumo_id,
    fecha_inicio, fecha_fin, hectareas_monitor, semilla_total_kg, creado_por
) VALUES (
    @camp2526, @algarrobos, @sojaB,
    '2025-11-15', '2025-11-16', 25.00, 1200.00, @ingeniero
);
SET @s11 = LAST_INSERT_ID();

-- Siembra 12: 9 NORTE, maíz (variedad B), todavía sin cosechar.
INSERT INTO siembras (
    campana_id, lote_id, semilla_insumo_id,
    fecha_inicio, fecha_fin, hectareas_monitor, semilla_total_kg, creado_por
) VALUES (
    @camp2526, @lote9, @maizB,
    '2025-10-08', '2025-10-09', 22.00, 616.00, @encargado
);
SET @s12 = LAST_INSERT_ID();

-- Cosechas de la campaña 25-26.
-- s6 (6 NORTE, maíz): netos 20000 y 22000 kg -> total 42000 kg / 20 ha = 2100.0 kg/ha.
INSERT INTO cosechas (siembra_id, fecha_inicio, fecha_fin, hectareas, humedad, acopio_id, comentarios, creado_por)
VALUES (@s6, '2026-03-20', '2026-03-21', 20.00, 14.00, @acopio1, 'Cosecha de prueba con dos camiones.', @encargado);
SET @c6 = LAST_INSERT_ID();
INSERT INTO pesajes (cosecha_id, orden_llegada, peso_bruto, peso_tara)
VALUES (@c6, 1, 30000.00, 10000.00), (@c6, 2, 32000.00, 10000.00);

-- s8 (EL MEDIO, trigo): netos 18000 y 17500 kg -> total 35500 kg / 14 ha = 2535.7 kg/ha.
INSERT INTO cosechas (siembra_id, fecha_inicio, fecha_fin, hectareas, humedad, acopio_id, comentarios, creado_por)
VALUES (@s8, '2025-12-01', '2025-12-02', 14.00, 11.50, @acopio1, NULL, @encargado);
SET @c7 = LAST_INSERT_ID();
INSERT INTO pesajes (cosecha_id, orden_llegada, peso_bruto, peso_tara)
VALUES (@c7, 1, 22000.00, 4000.00), (@c7, 2, 21500.00, 4000.00);

-- s10 (SANTA ROSA, maíz): netos 23000, 22500 y 21800 kg -> total 67300 kg / 30 ha = 2243.3 kg/ha.
INSERT INTO cosechas (siembra_id, fecha_inicio, fecha_fin, hectareas, humedad, acopio_id, comentarios, creado_por)
VALUES (@s10, '2026-03-25', '2026-03-26', 30.00, 15.00, @acopio2, 'Cosecha con tres camiones.', @ingeniero);
SET @c8 = LAST_INSERT_ID();
INSERT INTO pesajes (cosecha_id, orden_llegada, peso_bruto, peso_tara)
VALUES (@c8, 1, 34000.00, 11000.00), (@c8, 2, 33500.00, 11000.00), (@c8, 3, 32800.00, 11000.00);

-- s7, s9, s11 y s12 quedan sin cosecha para probar los cultivos "en pie".

-- =======================================================================
-- ÓRDENES DE TRABAJO
-- =======================================================================

-- Orden 24-25-0001: pulverización terrestre en ESTANCIA CHARRA, dos lotes, un insumo
-- que se remite en total (70 L) y se reparte 40 L / 30 L entre los lotes.
INSERT INTO ordenes_trabajo (
    numero_orden, fecha, labor_id, establecimiento_id,
    contratista_id, deposito_id, remito_numero, campana_id, creado_por
) VALUES (
    '24-25-0001', '2024-11-05', @pulv_terrestre, @norte,
    @contratista1, @deposito1, 'REMITO-2024-001', @camp2425, @ingeniero
);
SET @orden1 = LAST_INSERT_ID();

INSERT INTO orden_lotes (orden_id, lote_id, cultivo_id, superficie_ha)
VALUES (@orden1, @lote6, @maiz, 20.00);
SET @ol1_6n = LAST_INSERT_ID();
INSERT INTO orden_lotes (orden_id, lote_id, cultivo_id, superficie_ha)
VALUES (@orden1, @lote7, @soja, 15.00);
SET @ol1_7n = LAST_INSERT_ID();

INSERT INTO orden_insumos (orden_id, insumo_id, cantidad_a_remitir, unidad)
VALUES (@orden1, @herb1, 70.000, 'L');
SET @oi1_herb = LAST_INSERT_ID();

INSERT INTO orden_lote_insumos (orden_id, orden_lote_id, orden_insumo_id, total)
VALUES (@orden1, @ol1_6n, @oi1_herb, 40.000),
       (@orden1, @ol1_7n, @oi1_herb, 30.000);

-- Orden 24-25-0002: labranza en EL CARMEN, un solo lote, sin insumos.
INSERT INTO ordenes_trabajo (
    numero_orden, fecha, labor_id, establecimiento_id,
    contratista_id, campana_id, creado_por
) VALUES (
    '24-25-0002', '2024-05-20', @labranza, @sur,
    @contratista2, @camp2425, @ingeniero
);
SET @orden2 = LAST_INSERT_ID();

INSERT INTO orden_lotes (orden_id, lote_id, cultivo_id, superficie_ha)
VALUES (@orden2, @bajo, @trigo, 10.00);

-- Orden 24-25-0003: fertilización en TORRE, dos lotes, un insumo repartido.
INSERT INTO ordenes_trabajo (
    numero_orden, fecha, labor_id, establecimiento_id,
    contratista_id, deposito_id, remito_numero, campana_id, creado_por
) VALUES (
    '24-25-0003', '2024-10-01', @fertilizacion, @este,
    @contratista1, @deposito2, 'REMITO-2024-003', @camp2425, @ingeniero
);
SET @orden3 = LAST_INSERT_ID();

INSERT INTO orden_lotes (orden_id, lote_id, cultivo_id, superficie_ha)
VALUES (@orden3, @algarrobos, @maiz, 25.00);
SET @ol3_alg = LAST_INSERT_ID();
INSERT INTO orden_lotes (orden_id, lote_id, cultivo_id, superficie_ha)
VALUES (@orden3, @santarosa, NULL, 30.00);
SET @ol3_sr = LAST_INSERT_ID();

INSERT INTO orden_insumos (orden_id, insumo_id, cantidad_a_remitir, unidad)
VALUES (@orden3, @fertil_urea, 1100.000, 'kg');
SET @oi3_urea = LAST_INSERT_ID();

INSERT INTO orden_lote_insumos (orden_id, orden_lote_id, orden_insumo_id, total)
VALUES (@orden3, @ol3_alg, @oi3_urea, 500.000),
       (@orden3, @ol3_sr, @oi3_urea, 600.000);

INSERT INTO ordenes_trabajo (
    numero_orden, fecha, labor_id, establecimiento_id,
    contratista_id, deposito_id, remito_numero, campana_id, creado_por
) VALUES (
    '25-26-0001', '2025-11-10', @pulv_terrestre, @norte,
    @contratista1, @deposito1, 'REMITO-DEMO-001', @camp2526, @ingeniero
);
SET @orden4 = LAST_INSERT_ID();

INSERT INTO orden_lotes (orden_id, lote_id, cultivo_id, superficie_ha)
VALUES (@orden4, @lote6, @maiz, 20.00);
SET @ol4_6n = LAST_INSERT_ID();
INSERT INTO orden_lotes (orden_id, lote_id, cultivo_id, superficie_ha)
VALUES (@orden4, @lote7, @soja, 15.00);
SET @ol4_7n = LAST_INSERT_ID();

INSERT INTO orden_insumos (orden_id, insumo_id, cantidad_a_remitir, unidad)
VALUES (@orden4, @herb1, 70.000, 'L');
SET @oi4_herb = LAST_INSERT_ID();

INSERT INTO orden_lote_insumos (orden_id, orden_lote_id, orden_insumo_id, total)
VALUES (@orden4, @ol4_6n, @oi4_herb, 40.000),
       (@orden4, @ol4_7n, @oi4_herb, 30.000);

-- Orden 25-26-0002: labranza en EL CARMEN sin insumos.
INSERT INTO ordenes_trabajo (
    numero_orden, fecha, labor_id, establecimiento_id,
    contratista_id, campana_id, creado_por
) VALUES (
    '25-26-0002', '2025-09-20', @labranza, @sur,
    @contratista1, @camp2526, @ingeniero
);
SET @orden5 = LAST_INSERT_ID();

INSERT INTO orden_lotes (orden_id, lote_id, superficie_ha)
VALUES (@orden5, @bajo, 10.00);

-- Orden 25-26-0003: pulverización aérea en TORRE, dos lotes y DOS insumos
-- (herbicida hormonal + fungicida), cada uno repartido entre los lotes.
INSERT INTO ordenes_trabajo (
    numero_orden, fecha, labor_id, establecimiento_id,
    contratista_id, deposito_id, remito_numero, campana_id, creado_por
) VALUES (
    '25-26-0003', '2025-12-05', @pulv_aerea, @este,
    @contratista3, @deposito2, 'REMITO-2025-010', @camp2526, @ingeniero
);
SET @orden6 = LAST_INSERT_ID();

INSERT INTO orden_lotes (orden_id, lote_id, cultivo_id, superficie_ha)
VALUES (@orden6, @algarrobos, @soja, 25.00);
SET @ol6_alg = LAST_INSERT_ID();
INSERT INTO orden_lotes (orden_id, lote_id, cultivo_id, superficie_ha)
VALUES (@orden6, @santarosa, @maiz, 30.00);
SET @ol6_sr = LAST_INSERT_ID();

INSERT INTO orden_insumos (orden_id, insumo_id, cantidad_a_remitir, unidad)
VALUES (@orden6, @herb2, 110.000, 'L');
SET @oi6_herb = LAST_INSERT_ID();
INSERT INTO orden_insumos (orden_id, insumo_id, cantidad_a_remitir, unidad)
VALUES (@orden6, @fungicida1, 55.000, 'L');
SET @oi6_fung = LAST_INSERT_ID();

INSERT INTO orden_lote_insumos (orden_id, orden_lote_id, orden_insumo_id, total)
VALUES (@orden6, @ol6_alg, @oi6_herb, 50.000),
       (@orden6, @ol6_sr, @oi6_herb, 60.000),
       (@orden6, @ol6_alg, @oi6_fung, 25.000),
       (@orden6, @ol6_sr, @oi6_fung, 30.000);

-- Orden 25-26-0004: fertilización en ESTANCIA CHARRA sobre un lote sin siembra cargada.
INSERT INTO ordenes_trabajo (
    numero_orden, fecha, labor_id, establecimiento_id,
    contratista_id, deposito_id, remito_numero, campana_id, creado_por
) VALUES (
    '25-26-0004', '2025-10-01', @fertilizacion, @norte,
    @contratista1, @deposito1, 'REMITO-2025-004', @camp2526, @ingeniero
);
SET @orden7 = LAST_INSERT_ID();

INSERT INTO orden_lotes (orden_id, lote_id, superficie_ha)
VALUES (@orden7, @lote8, 18.00);
SET @ol7_8n = LAST_INSERT_ID();

INSERT INTO orden_insumos (orden_id, insumo_id, cantidad_a_remitir, unidad)
VALUES (@orden7, @fertil_map, 360.000, 'kg');
SET @oi7_map = LAST_INSERT_ID();

INSERT INTO orden_lote_insumos (orden_id, orden_lote_id, orden_insumo_id, total)
VALUES (@orden7, @ol7_8n, @oi7_map, 360.000);

-- Orden 25-26-0005: fertilización en EL CARMEN, dos lotes, un insumo repartido.
INSERT INTO ordenes_trabajo (
    numero_orden, fecha, labor_id, establecimiento_id,
    contratista_id, deposito_id, remito_numero, campana_id, creado_por
) VALUES (
    '25-26-0005', '2025-08-15', @fertilizacion, @sur,
    @contratista2, @deposito2, 'REMITO-2025-005', @camp2526, @ingeniero
);
SET @orden8 = LAST_INSERT_ID();

INSERT INTO orden_lotes (orden_id, lote_id, superficie_ha)
VALUES (@orden8, @loma, 12.00);
SET @ol8_loma = LAST_INSERT_ID();
INSERT INTO orden_lotes (orden_id, lote_id, cultivo_id, superficie_ha)
VALUES (@orden8, @medio, @trigo, 14.00);
SET @ol8_medio = LAST_INSERT_ID();

INSERT INTO orden_insumos (orden_id, insumo_id, cantidad_a_remitir, unidad)
VALUES (@orden8, @fertil_urea, 520.000, 'kg');
SET @oi8_urea = LAST_INSERT_ID();

INSERT INTO orden_lote_insumos (orden_id, orden_lote_id, orden_insumo_id, total)
VALUES (@orden8, @ol8_loma, @oi8_urea, 240.000),
       (@orden8, @ol8_medio, @oi8_urea, 280.000);

-- Orden 25-26-0006: pulverización terrestre en TORRE, un lote, un insumo.
INSERT INTO ordenes_trabajo (
    numero_orden, fecha, labor_id, establecimiento_id,
    contratista_id, deposito_id, remito_numero, campana_id, creado_por
) VALUES (
    '25-26-0006', '2025-11-20', @pulv_terrestre, @este,
    @contratista3, @deposito2, 'REMITO-2025-006', @camp2526, @ingeniero
);
SET @orden9 = LAST_INSERT_ID();

INSERT INTO orden_lotes (orden_id, lote_id, cultivo_id, superficie_ha)
VALUES (@orden9, @potrero, @girasol, 16.00);
SET @ol9_potrero = LAST_INSERT_ID();

INSERT INTO orden_insumos (orden_id, insumo_id, cantidad_a_remitir, unidad)
VALUES (@orden9, @insecticida1, 32.000, 'L');
SET @oi9_insect = LAST_INSERT_ID();

INSERT INTO orden_lote_insumos (orden_id, orden_lote_id, orden_insumo_id, total)
VALUES (@orden9, @ol9_potrero, @oi9_insect, 32.000);

COMMIT;

