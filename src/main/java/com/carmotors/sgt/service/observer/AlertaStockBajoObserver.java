package com.carmotors.sgt.service.observer;

import com.carmotors.sgt.model.Repuesto;
import com.carmotors.sgt.model.StockRepuesto;

/**
 * Observador concreto que emite una alerta estructurada por consola cuando
 * el disponible de un repuesto cae a su mínimo o menos (RN-22, RNF-11, T5.1).
 */
public class AlertaStockBajoObserver implements StockObserver {

    @Override
    public void onStockBajo(StockRepuesto stock) {
        if (stock == null || stock.getRepuesto() == null) {
            return;
        }

        Repuesto repuesto = stock.getRepuesto();
        System.out.println("\n  [ALERTA DE STOCK BAJO — RN-22]");
        System.out.println("  ==================================================");
        System.out.println("  Repuesto:           " + repuesto.getId() + " - " + repuesto.getNombre());
        System.out.println("  Stock Físico:       " + stock.getStockFisico());
        System.out.println("  Stock Operativo:    " + stock.getStockOperativo());
        System.out.println("  Reservado:          " + stock.getReservado());
        System.out.println("  Disponible:         " + stock.getDisponible() + " (Mínimo requerido: " + stock.getStockMinimo() + ")");
        if (stock.getDeficit() > 0) {
            System.out.println("  Déficit de reserva: " + stock.getDeficit());
        }
        System.out.println("  Proveedor habitual: " + stock.getProveedorHabitual());
        System.out.println("  ==================================================");
    }
}
