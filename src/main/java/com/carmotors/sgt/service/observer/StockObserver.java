package com.carmotors.sgt.service.observer;

import com.carmotors.sgt.model.StockRepuesto;

/**
 * Interfaz para suscriptores interesados en alertas y cambios de stock (RN-22, Patrón Observer, RNF-10 ISP).
 */
public interface StockObserver {

    /**
     * Invocado cuando un repuesto activo queda en o por debajo de su stock mínimo tras un consumo o evaluación.
     *
     * @param stock métricas consolidadas del repuesto afectado.
     */
    void onStockBajo(StockRepuesto stock);
}
