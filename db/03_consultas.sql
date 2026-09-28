USE agro;

-- RF06 / CU-06: Datos técnicos de las siembras de un lote.
-- Devuelve todas las siembras del lote indicado en @lote_id, con campaña,
-- establecimiento, semilla y los datos de la máquina, la densidad y la
-- fertilización. Excluye las siembras eliminadas y las
-- ordena de la más reciente a la más antigua.

SET @lote_id = 1;
SELECT
s.siembra_id,
cp.nombre AS campana,
e.nombre AS establecimiento,
l.nombre AS lote,
s.fecha_inicio,
s.fecha_fin,
i.nombre AS semilla,
s.hectareas_monitor,
s.tractor,
s.marcha_trabajo,
s.velocidad_siembra,
s.sembradora,
s.engranaje_conductor,
s.engranaje_conducido,
s.placa,
s.grano_por_metro,
s.semilla_total_kg,
s.calibre,
s.desvio,
s.tipo_fertilizante,
s.fertilizante_kg_ha,
s.marcha_caja_fertilizante,
s.comentarios
FROM siembras s
JOIN campanas cp ON cp.campana_id = s.campana_id
JOIN lotes l ON l.lote_id = s.lote_id
JOIN establecimientos e ON e.establecimiento_id = l.establecimiento_id
JOIN insumos i ON i.insumo_id = s.semilla_insumo_id
WHERE l.lote_id = @lote_id
AND s.eliminado_en IS NULL
ORDER BY s.fecha_inicio DESC, s.siembra_id DESC;


-- RF07 / CU-07: Trazabilidad por lote y campaña.
-- Consulta la vista v_trazabilidad_campana, que reúne las operaciones
-- (siembras, cosechas, órdenes de trabajo y aplicaciones) de un lote en
-- una campaña. Se filtra con @lote_id y @campana_id y se ordena por
-- fecha y etapa para reconstruir la cronología de la actividad.
SET @lote_id = 1;
SET @campana_id = 1;
SELECT *
FROM v_trazabilidad_campana
WHERE lote_id = @lote_id
AND campana_id = @campana_id
ORDER BY fecha, orden_etapa;


-- RF05 / CU-05: Aplicaciones y dosis de una orden de trabajo.
-- Lista, para la orden indicada en @orden_id, cada aplicación con su lote,
-- insumo, superficie trabajada, total aplicado y unidad. La dosis por
-- hectárea se calcula como total aplicado / superficie
-- NULLIF previene una división por cero. Excluye órdenes eliminadas.

SET @orden_id = 1;
SELECT
ot.numero_orden,
l.nombre AS lote,
i.nombre AS insumo,
ol.superficie_ha,
oli.total AS total_aplicado,
oi.unidad,
ROUND(
    oli.total / NULLIF(ol.superficie_ha, 0),
    3
) AS dosis_por_ha
FROM orden_lote_insumos oli
JOIN ordenes_trabajo ot ON ot.orden_id = oli.orden_id
JOIN orden_lotes ol
ON ol.orden_lote_id = oli.orden_lote_id
AND ol.orden_id = oli.orden_id
JOIN lotes l ON l.lote_id = ol.lote_id
JOIN orden_insumos oi
ON oi.orden_insumo_id = oli.orden_insumo_id
AND oi.orden_id = oli.orden_id
JOIN insumos i ON i.insumo_id = oi.insumo_id
WHERE ot.orden_id = @orden_id
AND ot.eliminado_en IS NULL
ORDER BY l.nombre, i.nombre;



-- RF08 / CU-08: Rindes de las cosechas de una campaña.
-- Consulta la vista v_rendimiento_lote, que muestra por cada lote la
-- siembra y la cosecha asociadas, las hectáreas, el peso total cosechado
-- y el rinde en kg/ha. Se ordena por establecimiento, lote y fechas.
SET @campana_id = 1;
SELECT
campana,
establecimiento,
lote,
cultivo,
fecha_siembra,
fecha_cosecha,
hectareas,
peso_total_kg,
kg_por_ha
FROM v_rendimiento_lote
WHERE campana_id = @campana_id
ORDER BY
establecimiento,
lote,
fecha_siembra,
fecha_cosecha;
