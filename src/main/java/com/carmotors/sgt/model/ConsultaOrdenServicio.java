package com.carmotors.sgt.model;

import java.math.BigDecimal;
import java.util.List;

public class ConsultaOrdenServicio {
    private final OrdenServicio orden;
    private final Cliente cliente;
    private final Vehiculo vehiculo;
    private final String estadoRepuestos;
    private final List<Linea> lineas;

    public ConsultaOrdenServicio(OrdenServicio orden, Cliente cliente, Vehiculo vehiculo,
                                 String estadoRepuestos, List<Linea> lineas) {
        this.orden = orden;
        this.cliente = cliente;
        this.vehiculo = vehiculo;
        this.estadoRepuestos = estadoRepuestos;
        this.lineas = lineas;
    }

    public OrdenServicio getOrden() { return orden; }
    public Cliente getCliente() { return cliente; }
    public Vehiculo getVehiculo() { return vehiculo; }
    public String getEstadoRepuestos() { return estadoRepuestos; }
    public List<Linea> getLineas() { return lineas; }

    public static class Linea {
        private final OrdenServicioDetalle detalle;
        private final List<HorasTecnico> horasPorTecnico;
        private final List<DetalleRepuesto> repuestos;
        private final BigDecimal subtotal;

        public Linea(OrdenServicioDetalle detalle, List<HorasTecnico> horasPorTecnico,
                     List<DetalleRepuesto> repuestos, BigDecimal subtotal) {
            this.detalle = detalle;
            this.horasPorTecnico = horasPorTecnico;
            this.repuestos = repuestos;
            this.subtotal = subtotal;
        }

        public OrdenServicioDetalle getDetalle() { return detalle; }
        public List<HorasTecnico> getHorasPorTecnico() { return horasPorTecnico; }
        public List<DetalleRepuesto> getRepuestos() { return repuestos; }
        public BigDecimal getSubtotal() { return subtotal; }
    }

    public static class HorasTecnico {
        private final Tecnico tecnico;
        private final BigDecimal horasTrabajadas;

        public HorasTecnico(Tecnico tecnico, BigDecimal horasTrabajadas) {
            this.tecnico = tecnico;
            this.horasTrabajadas = horasTrabajadas;
        }

        public Tecnico getTecnico() { return tecnico; }
        public BigDecimal getHorasTrabajadas() { return horasTrabajadas; }
    }
}
