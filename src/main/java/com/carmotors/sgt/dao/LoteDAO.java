package com.carmotors.sgt.dao;

import java.time.LocalDate;
import java.util.List;

import com.carmotors.sgt.model.Lote;

/**
 * Acceso a datos del lote de inventario.
 */
public interface LoteDAO {

    Lote buscarPorCodigo(String codigoLote);

    List<Lote> listarPorRepuesto(int repuestoId);

    int guardar(Lote lote);

    /**
     * Stock físico: suma de {@code cantidad_actual} de todos los lotes del repuesto (0 si no tiene lotes).
     */
    int sumarStockFisico(int repuestoId);

    /**
     * Stock operativo: suma de {@code cantidad_actual} de los lotes del repuesto sin caducidad
     * o con caducidad igual o posterior a la fecha indicada (0 si no hay lotes vigentes).
     */
    int sumarStockOperativo(int repuestoId, LocalDate fechaReferencia);

    /**
     * Lista los lotes candidatos para el reporte de vencimiento (RF-06):
     * con saldo restante mayor a cero, fecha de caducidad no nula y caducidad menor o igual
     * a la fecha límite indicada (incluye lotes ya vencidos y lotes por vencer hasta la fecha límite).
     */
    List<Lote> listarPorVencer(LocalDate fechaLimite);

    /**
     * Descuenta unidades del saldo de un lote al completar una orden (RN-03).
     * Solo actualiza si el lote tiene saldo suficiente.
     *
     * @param loteId   identificador del lote
     * @param cantidad unidades a descontar
     * @return {@code true} si se descontó; {@code false} si el lote no existe o no tiene saldo suficiente
     */
    boolean descontarUnidades(int loteId, int cantidad);
}
