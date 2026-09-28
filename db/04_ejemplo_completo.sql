-- SIGTOA - Insercion, consulta y baja logica de siembra, cosecha y orden de trabajo.
-- Ejecutar despues de 01_schema.sql y 02_datos_de_prueba.sql.

USE agro;
SET NAMES utf8mb4;

-- Datos de ejemplo e inicializacion de variables --

SET @campana_ejemplo = (
SELECT campana_id
FROM campanas
WHERE nombre = '25-26'
);

SET @lote_ejemplo = (
SELECT l.lote_id
FROM lotes l
JOIN establecimientos e ON e.establecimiento_id = l.establecimiento_id
WHERE e.nombre = 'EL CARMEN'
AND l.nombre = 'EL BAJO'
);

SET @semilla_ejemplo = (
SELECT i.insumo_id
FROM insumos i
JOIN grupos_insumo g ON g.grupo_id = i.grupo_id
WHERE g.nombre = 'Semillas de maíz'
AND i.nombre = 'Maíz A'
);

SET @usuario_ejemplo = (
SELECT usuario_id
FROM usuarios
WHERE usuario_login = 'demo_encargado'
);

START TRANSACTION;

-- Se bloquea la campaña para asignar el siguiente numero de orden
SELECT campana_id INTO @campana_ejemplo
FROM campanas
WHERE nombre = '25-26'
FOR UPDATE;

-- Datos relacionados que se reutilizan en la cosecha y la orden
SET @establecimiento_ejemplo = (
SELECT establecimiento_id FROM lotes WHERE lote_id = @lote_ejemplo
);

SET @cultivo_ejemplo = (
SELECT g.cultivo_id
FROM insumos i
JOIN grupos_insumo g ON g.grupo_id = i.grupo_id
WHERE i.insumo_id = @semilla_ejemplo
);

SET @acopio_ejemplo = (
SELECT acopio_id FROM acopios WHERE nombre = 'Silo propio'
);

SET @labor_ejemplo = (
SELECT labor_id FROM labores WHERE nombre = 'Pulverización terrestre'
);

SET @contratista_ejemplo = (
SELECT contratista_id FROM contratistas WHERE nombre = 'Contratista de prueba'
);

SET @deposito_ejemplo = (
SELECT deposito_id FROM depositos WHERE nombre = 'Depósito central'
);

SET @herbicida_ejemplo = (
SELECT i.insumo_id
FROM insumos i
JOIN grupos_insumo g ON g.grupo_id = i.grupo_id
WHERE g.nombre = 'Herbicidas'
AND i.nombre = 'Herbicida de prueba'
);

-- Inserciones --
-- Siembra
INSERT INTO siembras (
campana_id,
lote_id,
semilla_insumo_id,
fecha_inicio,
fecha_fin,
hectareas_monitor,
tractor,
marcha_trabajo,
velocidad_siembra,
sembradora,
engranaje_conductor,
engranaje_conducido,
placa,
grano_por_metro,
semilla_total_kg,
calibre,
desvio,
tipo_fertilizante,
fertilizante_kg_ha,
marcha_caja_fertilizante,
comentarios,
creado_por
)
VALUES (
@campana_ejemplo,
@lote_ejemplo,
@semilla_ejemplo,
'2025-10-20',
'2025-10-21',
10.00,
'Tractor de prueba',
4,
6.50,
'Sembradora de prueba',
17,
24,
'P-12',
6.00,
300.00,
0.85,
3.00,
'Fosfato monoamónico',
80.00,
4,
'Ejemplo de siembra completa para el TP2.',
@usuario_ejemplo
);

SET @siembra_ejemplo = LAST_INSERT_ID();

-- Cosecha de la siembra anterior, con dos pesajes
INSERT INTO cosechas (
siembra_id, fecha_inicio, fecha_fin, hectareas,
humedad, acopio_id, comentarios, creado_por
)
VALUES (
@siembra_ejemplo, '2026-03-20', '2026-03-21', 10.00,
14.00, @acopio_ejemplo,
'Ejemplo de cosecha completa para el TP2.', @usuario_ejemplo
);

SET @cosecha_ejemplo = LAST_INSERT_ID();

-- El peso neto se calcula automaticamente como peso bruto menos tara
INSERT INTO pesajes (cosecha_id, orden_llegada, peso_bruto, peso_tara)
VALUES
(@cosecha_ejemplo, 1, 20000.00, 10000.00),
(@cosecha_ejemplo, 2, 21000.00, 10000.00);

-- Orden de trabajo sobre el mismo lote y campaña
-- Tambien se cuentan las ordenes dadas de baja, porque su numero no se reutiliza
SET @numero_orden_ejemplo = (
SELECT CONCAT('25-26-', -- prefijo del periodo
LPAD(
    -- Se toman los ultimos 4 numeros de la orden,
    -- busca el mayor y le suma 1 para obtener el que sigue
    COALESCE(
        MAX(CAST(RIGHT(numero_orden, 4) AS UNSIGNED)),
        0
    ) + 1,
    4, -- El numero tiene que tener 4 digitos
    '0' -- Llena los digitos que faltan con 0 a la izquierda
))
FROM ordenes_trabajo
WHERE campana_id = @campana_ejemplo
);

INSERT INTO ordenes_trabajo (
numero_orden, fecha, labor_id, establecimiento_id,
contratista_id, deposito_id, remito_numero, campana_id,
convenio, comentarios, creado_por
)
VALUES (
@numero_orden_ejemplo, '2025-11-10', @labor_ejemplo,
@establecimiento_ejemplo, @contratista_ejemplo, @deposito_ejemplo,
'REM-EJEMPLO', @campana_ejemplo, 'Servicio por hectárea',
'Ejemplo de orden completa para el TP2.', @usuario_ejemplo
);

SET @orden_ejemplo = LAST_INSERT_ID();

INSERT INTO orden_lotes (orden_id, lote_id, cultivo_id, superficie_ha)
VALUES (@orden_ejemplo, @lote_ejemplo, @cultivo_ejemplo, 10.00);

SET @orden_lote_ejemplo = LAST_INSERT_ID();

INSERT INTO orden_insumos (orden_id, insumo_id, cantidad_a_remitir, unidad)
VALUES (@orden_ejemplo, @herbicida_ejemplo, 20.000, 'L');

SET @orden_insumo_ejemplo = LAST_INSERT_ID();

-- Se aplican los 20 litros sobre las 10 hectareas del lote
INSERT INTO orden_lote_insumos (orden_id, orden_lote_id, orden_insumo_id, total)
VALUES (@orden_ejemplo, @orden_lote_ejemplo, @orden_insumo_ejemplo, 20.000);




-- Consultas --
-- Se pueden repetir despues de las bajas para comprobar que los datos se conservan

-- Todos los datos de la siembra
SELECT *
FROM siembras
WHERE siembra_id = @siembra_ejemplo;

-- Todos los datos de la cosecha y sus pesajes
SELECT *
FROM cosechas
WHERE cosecha_id = @cosecha_ejemplo;

SELECT *
FROM pesajes
WHERE cosecha_id = @cosecha_ejemplo
ORDER BY orden_llegada;

-- Resultado esperado: 21.000 kg en 10 ha, con un rinde de 2.100 kg/ha
SELECT
c.cosecha_id,
c.hectareas,
SUM(p.peso_neto) AS peso_total_kg,
ROUND(SUM(p.peso_neto) / NULLIF(c.hectareas, 0), 2) AS rinde_kg_ha
FROM cosechas c
JOIN pesajes p ON p.cosecha_id = c.cosecha_id
WHERE c.cosecha_id = @cosecha_ejemplo
GROUP BY c.cosecha_id, c.hectareas;

-- Todos los datos de la orden, su lote y la lista de remision
SELECT *
FROM ordenes_trabajo
WHERE orden_id = @orden_ejemplo;

SELECT ol.*, l.nombre AS lote, cu.nombre AS cultivo
FROM orden_lotes ol
JOIN lotes l ON l.lote_id = ol.lote_id
LEFT JOIN cultivos cu ON cu.cultivo_id = ol.cultivo_id
WHERE ol.orden_id = @orden_ejemplo;

SELECT oi.*, i.nombre AS insumo
FROM orden_insumos oi
JOIN insumos i ON i.insumo_id = oi.insumo_id
WHERE oi.orden_id = @orden_ejemplo;

-- Aplicacion y dosis. Resultado esperado: 20 L en 10 ha, con una dosis de 2 L/ha
SELECT
l.nombre AS lote,
i.nombre AS insumo,
ol.superficie_ha,
oli.total AS cantidad_aplicada,
oi.unidad,
ROUND(oli.total / NULLIF(ol.superficie_ha, 0), 3) AS dosis_por_ha
FROM orden_lote_insumos oli
JOIN orden_lotes ol ON ol.orden_lote_id = oli.orden_lote_id
JOIN orden_insumos oi ON oi.orden_insumo_id = oli.orden_insumo_id
JOIN lotes l ON l.lote_id = ol.lote_id
JOIN insumos i ON i.insumo_id = oi.insumo_id
WHERE oli.orden_id = @orden_ejemplo;


-- Borrados logicos --
UPDATE cosechas
SET eliminado_por = @usuario_ejemplo,
eliminado_en = NOW()
WHERE cosecha_id = @cosecha_ejemplo
AND eliminado_en IS NULL;

UPDATE ordenes_trabajo
SET eliminado_por = @usuario_ejemplo,
eliminado_en = NOW()
WHERE orden_id = @orden_ejemplo
AND eliminado_en IS NULL;

UPDATE siembras
SET eliminado_por = @usuario_ejemplo,
eliminado_en = NOW()
WHERE siembra_id = @siembra_ejemplo
AND eliminado_en IS NULL;

COMMIT;
