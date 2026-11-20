package com.carmotors.sgt.service;

import com.carmotors.sgt.exception.StockInsuficienteException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Lote;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Algoritmo FEFO determinista para consumir unidades de los lotes de un repuesto
 * (RN-03, sección 2.6). No accede a base de datos: solo decide cuántas unidades
 * se descuentan de cada lote.
 *
 * <p>Orden de consumo: solo lotes no vencidos (caducidad nula o igual/posterior a la
 * fecha de referencia) y con unidades; primero los lotes con caducidad, por fecha más
 * próxima; en empate, fecha de ingreso más antigua; luego menor ID. Los lotes sin
 * caducidad se consumen al final, por fecha de ingreso más antigua y menor ID.</p>
 */
public class ConsumoLotesFefo {

    /**
     * Suma las unidades de los lotes no vencidos (stock operativo, sección 2.5).
     *
     * @param lotes           lotes del repuesto
     * @param fechaReferencia fecha con la que se decide si un lote está vencido
     * @return unidades disponibles en lotes vigentes
     */
    public int calcularStockOperativo(List<Lote> lotes, LocalDate fechaReferencia) {
        int total = 0;
        for (Lote lote : lotesConsumibles(lotes, fechaReferencia)) {
            total += lote.getCantidadActual();
        }
        return total;
    }

    /**
     * Calcula cuántas unidades se descuentan de cada lote siguiendo FEFO.
     *
     * @param lotes           lotes del repuesto (en cualquier orden)
     * @param fechaReferencia fecha con la que se decide si un lote está vencido
     * @param cantidad        unidades a consumir (mayor que cero)
     * @return mapa {@code loteId -> unidades a descontar}, en el orden de consumo
     * @throws StockInsuficienteException si los lotes vigentes no cubren la cantidad
     */
    public Map<Integer, Integer> calcularConsumo(List<Lote> lotes, LocalDate fechaReferencia, int cantidad) {
        if (cantidad <= 0) {
            throw new ValidacionException("La cantidad a consumir debe ser mayor que cero.");
        }

        Map<Integer, Integer> consumo = new LinkedHashMap<>();
        int pendiente = cantidad;
        for (Lote lote : lotesConsumibles(lotes, fechaReferencia)) {
            if (pendiente == 0) {
                break;
            }
            int unidades = Math.min(pendiente, lote.getCantidadActual());
            consumo.put(lote.getId(), unidades);
            pendiente -= unidades;
        }

        if (pendiente > 0) {
            throw new StockInsuficienteException("Stock operativo insuficiente: se requieren " + cantidad
                    + " unidad(es) y los lotes vigentes solo tienen " + (cantidad - pendiente) + ".");
        }
        return consumo;
    }

    private List<Lote> lotesConsumibles(List<Lote> lotes, LocalDate fechaReferencia) {
        if (lotes == null) {
            throw new ValidacionException("La lista de lotes es obligatoria.");
        }
        if (fechaReferencia == null) {
            throw new ValidacionException("La fecha de referencia es obligatoria.");
        }
        return lotes.stream()
                .filter(lote -> lote.getCantidadActual() > 0)
                .filter(lote -> lote.getFechaCaducidad() == null || !lote.getFechaCaducidad().isBefore(fechaReferencia))
                .sorted((a, b) -> compararFefo(a, b))
                .collect(Collectors.toList());
    }

    private int compararFefo(Lote a, Lote b) {
        boolean aSinCaducidad = a.getFechaCaducidad() == null;
        boolean bSinCaducidad = b.getFechaCaducidad() == null;
        if (aSinCaducidad != bSinCaducidad) {
            return aSinCaducidad ? 1 : -1;
        }
        if (!aSinCaducidad) {
            int porCaducidad = a.getFechaCaducidad().compareTo(b.getFechaCaducidad());
            if (porCaducidad != 0) {
                return porCaducidad;
            }
        }
        int porIngreso = a.getFechaIngreso().compareTo(b.getFechaIngreso());
        if (porIngreso != 0) {
            return porIngreso;
        }
        return Integer.compare(a.getId(), b.getId());
    }
}
