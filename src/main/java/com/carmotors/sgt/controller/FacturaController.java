package com.carmotors.sgt.controller;

import com.carmotors.sgt.model.ConsultaFactura;
import com.carmotors.sgt.model.Factura;
import com.carmotors.sgt.model.ResultadoReimpresionFactura;
import com.carmotors.sgt.service.FacturaService;

import java.nio.file.Path;
import java.util.List;

/**
 * Controlador de facturación (RNF-04): solo delega en {@link FacturaService}.
 */
public class FacturaController {

    private final FacturaService facturaService;

    public FacturaController(FacturaService facturaService) {
        if (facturaService == null) {
            throw new IllegalArgumentException("El servicio de facturación es obligatorio.");
        }
        this.facturaService = facturaService;
    }

    public ConsultaFactura generarFactura(int ordenId) {
        return facturaService.generarFactura(ordenId);
    }

    public ConsultaFactura consultarFactura(int facturaId) {
        return facturaService.consultarFactura(facturaId);
    }

    public ConsultaFactura consultarFacturaPorOrden(int ordenId) {
        return facturaService.consultarFacturaPorOrden(ordenId);
    }

    public ConsultaFactura buscarFacturaPorOrden(int ordenId) {
        return facturaService.buscarFacturaPorOrden(ordenId);
    }

    public ConsultaFactura consultarFacturaPorNumero(String numeroFactura) {
        return facturaService.consultarFacturaPorNumero(numeroFactura);
    }

    public List<Factura> listarFacturas() {
        return facturaService.listarFacturas();
    }

    public Path reintentarArchivo(int facturaId) {
        return facturaService.reintentarArchivo(facturaId);
    }

    public ResultadoReimpresionFactura reimprimirFactura(int facturaId) {
        return facturaService.reimprimirFactura(facturaId);
    }

    public ResultadoReimpresionFactura reimprimirFacturaPorOrden(int ordenId) {
        return facturaService.reimprimirFacturaPorOrden(ordenId);
    }

    public ResultadoReimpresionFactura reimprimirFacturaPorNumero(String numeroFactura) {
        return facturaService.reimprimirFacturaPorNumero(numeroFactura);
    }
}
