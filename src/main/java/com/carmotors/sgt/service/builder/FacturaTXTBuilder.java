package com.carmotors.sgt.service.builder;

import com.carmotors.sgt.model.Factura;
import com.carmotors.sgt.model.FacturaDetalle;
import com.carmotors.sgt.model.FacturaSnapshot;
import com.carmotors.sgt.model.TipoConcepto;

import java.time.format.DateTimeFormatter;
import java.util.List;

/** Maqueta un comprobante interno con los valores persistidos al facturar. */
public class FacturaTXTBuilder {
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String SEPARADOR = "--------------------------------------------------\n";
    private final StringBuilder texto = new StringBuilder();

    public FacturaTXTBuilder conEncabezado(Factura factura) {
        texto.append(SEPARADOR)
                .append("CAR MOTORS | COMPROBANTE INTERNO\n")
                .append("Factura: ").append(factura.getNumeroFactura()).append('\n')
                .append("Orden de servicio: ").append(factura.getOrdenId()).append('\n')
                .append("Fecha de emisión: ").append(factura.getFechaEmision().format(FORMATO_FECHA)).append('\n');
        return this;
    }

    public FacturaTXTBuilder conClienteYVehiculo(FacturaSnapshot snapshot) {
        texto.append(SEPARADOR)
                .append("Cliente: ").append(snapshot.getClienteNombre()).append('\n')
                .append("Documento: ").append(snapshot.getClienteDocumento()).append('\n')
                .append("Teléfono: ").append(textoOpcional(snapshot.getClienteTelefono())).append('\n')
                .append("Correo: ").append(textoOpcional(snapshot.getClienteEmail())).append('\n')
                .append("Vehículo: ").append(snapshot.getVehiculoPlaca()).append(" | ")
                .append(snapshot.getVehiculoMarca()).append(' ').append(snapshot.getVehiculoModelo()).append('\n');
        return this;
    }

    public FacturaTXTBuilder conConceptos(List<FacturaDetalle> conceptos) {
        texto.append(SEPARADOR).append("Conceptos (COP)\n");
        for (FacturaDetalle detalle : conceptos) {
            String tipo = detalle.getTipoConcepto() == TipoConcepto.MANO_OBRA ? "Mano de obra" : "Repuesto";
            texto.append("Línea ").append(detalle.getOrdenDetalleId()).append(" | ").append(tipo)
                    .append(" | ").append(detalle.getDescripcionSnapshot()).append('\n')
                    .append("  ").append(detalle.getCantidad()).append(" x $")
                    .append(detalle.getPrecioUnitario().toPlainString()).append(" = $")
                    .append(detalle.getSubtotal().toPlainString()).append('\n');
        }
        return this;
    }

    public FacturaTXTBuilder conTotales(Factura factura) {
        texto.append(SEPARADOR)
                .append("Total mano de obra: $").append(factura.getTotalManoObra().toPlainString()).append('\n')
                .append("Total repuestos: $").append(factura.getTotalRepuestos().toPlainString()).append('\n')
                .append("Subtotal: $").append(factura.getSubtotal().toPlainString()).append('\n')
                .append("IVA (").append(factura.getTasaIva().movePointRight(2).stripTrailingZeros().toPlainString())
                .append("%): $").append(factura.getImpuestos().toPlainString()).append('\n')
                .append("Total (COP): $").append(factura.getTotalFinal().toPlainString()).append('\n');
        return this;
    }

    public String construir() {
        return texto.toString();
    }

    private String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? "N/A" : valor;
    }
}
