package com.carmotors.sgt.service;

import com.carmotors.sgt.controller.OrdenServicioController;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.TransicionEstadoInvalidaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.CargaLineaTecnico;
import com.carmotors.sgt.model.Cliente;
import com.carmotors.sgt.model.DetalleRepuesto;
import com.carmotors.sgt.model.DetalleTecnico;
import com.carmotors.sgt.model.Lote;
import com.carmotors.sgt.model.OrdenServicio;
import com.carmotors.sgt.model.OrdenServicioDetalle;
import com.carmotors.sgt.model.Proveedor;
import com.carmotors.sgt.model.ProveedorRepuesto;
import com.carmotors.sgt.model.Repuesto;
import com.carmotors.sgt.model.ReservaLiberada;
import com.carmotors.sgt.model.ResultadoCancelarOrden;
import com.carmotors.sgt.model.ServicioTipo;
import com.carmotors.sgt.model.Tecnico;
import com.carmotors.sgt.model.Vehiculo;
import com.carmotors.sgt.dao.ClienteDAO;
import com.carmotors.sgt.dao.DetalleRepuestoDAO;
import com.carmotors.sgt.dao.LoteDAO;
import com.carmotors.sgt.dao.OrdenServicioDAO;
import com.carmotors.sgt.dao.OrdenServicioDetalleDAO;
import com.carmotors.sgt.dao.ProveedorDAO;
import com.carmotors.sgt.dao.ProveedorRepuestoDAO;
import com.carmotors.sgt.dao.RepuestoDAO;
import com.carmotors.sgt.dao.ServicioTipoDAO;
import com.carmotors.sgt.dao.TecnicoDAO;
import com.carmotors.sgt.dao.VehiculoDAO;
import com.carmotors.sgt.service.state.EstadoCancelada;
import com.carmotors.sgt.service.state.EstadoCompletado;
import com.carmotors.sgt.service.state.EstadoEnProceso;
import com.carmotors.sgt.service.state.EstadoEntregado;
import com.carmotors.sgt.service.state.EstadoFacturado;
import com.carmotors.sgt.service.state.EstadoPendiente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias de la cancelación de órdenes de servicio (T5.3 — RF-14, RN-04, RN-05, RN-06, HU-08).
 */
class OrdenServicioCancelarTest {

    private OrdenServicioDAOMock ordenDAO;
    private OrdenServicioDetalleDAOMock detalleDAO;
    private DetalleRepuestoDAOMock detalleRepuestoDAO;
    private RepuestoDAOMock repuestoDAO;
    private LoteDAOMock loteDAO;
    private OrdenServicioService service;
    private OrdenServicioController controller;

    @BeforeEach
    void setUp() {
        ordenDAO = new OrdenServicioDAOMock();
        detalleDAO = new OrdenServicioDetalleDAOMock();
        detalleRepuestoDAO = new DetalleRepuestoDAOMock();
        repuestoDAO = new RepuestoDAOMock();
        loteDAO = new LoteDAOMock();

        InventarioService inventarioService = new InventarioService(
                repuestoDAO, loteDAO, new ProveedorRepuestoDAOVacio(), new ProveedorDAOVacio());
        service = new OrdenServicioService(ordenDAO, detalleDAO, new VehiculoDAOVacio(),
                new ClienteDAOVacio(), new ServicioTipoDAOVacio(), new TecnicoDAOVacio(),
                repuestoDAO, detalleRepuestoDAO, loteDAO, inventarioService, false);
        controller = new OrdenServicioController(service);
    }

    @Test
    @DisplayName("Cancelar orden Pendiente consolida y libera todas las reservas sin tocar lotes ni borrar historial")
    void cancelarOrdenPendienteLiberaReservasMultiLineaYConservaHistorial() {
        ordenDAO.agregar(crearOrden(10, new EstadoPendiente().getEstadoId()));
        detalleDAO.agregar(crearLinea(101, 10, "Cambio de aceite", new BigDecimal("60000.00")));
        detalleDAO.agregar(crearLinea(102, 10, "Mantenimiento preventivo", new BigDecimal("90000.00")));

        // Repuesto 1 tiene 7 reservadas en total (5 de esta orden en 2 líneas + 2 de otra orden).
        repuestoDAO.agregar(crearRepuesto(1, "Aceite 10W-40", 7, 5));
        repuestoDAO.agregar(crearRepuesto(2, "Filtro de aceite", 1, 3));

        loteDAO.agregar(crearLote(1, 1, LocalDate.now().plusDays(60), 20));
        loteDAO.agregar(crearLote(2, 2, null, 10));

        detalleRepuestoDAO.agregar(crearReserva(101, 1, 2, "Aceite 10W-40", new BigDecimal("38000.00")));
        detalleRepuestoDAO.agregar(crearReserva(101, 2, 1, "Filtro de aceite", new BigDecimal("25000.00")));
        detalleRepuestoDAO.agregar(crearReserva(102, 1, 3, "Aceite 10W-40", new BigDecimal("38000.00")));

        ResultadoCancelarOrden resultado = service.cancelarOrden(10);

        assertEquals(new EstadoCancelada().getEstadoId(), resultado.getOrden().getEstadoId());
        assertNotNull(resultado.getOrden().getFechaCancelacion());
        assertEquals(2, resultado.getReservasLiberadas().size());
        assertEquals(6, resultado.getTotalUnidadesLiberadas());

        ReservaLiberada liberadoAceite = resultado.getReservasLiberadas().get(0);
        assertEquals(1, liberadoAceite.getRepuestoId());
        assertEquals("Aceite 10W-40", liberadoAceite.getNombreRepuesto());
        assertEquals(5, liberadoAceite.getUnidadesLiberadas());

        ReservaLiberada liberadoFiltro = resultado.getReservasLiberadas().get(1);
        assertEquals(2, liberadoFiltro.getRepuestoId());
        assertEquals("Filtro de aceite", liberadoFiltro.getNombreRepuesto());
        assertEquals(1, liberadoFiltro.getUnidadesLiberadas());

        // Se liberaron exactamente las unidades de la orden 10; la reserva de otra orden (2) permanece.
        assertEquals(2, repuestoDAO.buscarPorId(1).getCantidadReservada());
        assertEquals(0, repuestoDAO.buscarPorId(2).getCantidadReservada());

        // El stock físico de los lotes no se tocó (RF-14 / RN-05).
        assertEquals(20, loteDAO.cantidadActual(1));
        assertEquals(10, loteDAO.cantidadActual(2));
        assertEquals(0, loteDAO.descuentosRealizados);

        // Líneas y detalle_repuestos se conservan como historial (RF-14).
        assertEquals(2, detalleDAO.listarPorOrden(10).size());
        assertEquals(2, detalleRepuestoDAO.listarPorDetalle(101).size());
        assertEquals(1, detalleRepuestoDAO.listarPorDetalle(102).size());
    }

    @Test
    @DisplayName("Cancelar orden En Proceso libera reservas, conserva técnicos y horas y delega desde controlador")
    void cancelarOrdenEnProcesoLiberaReservasYConservaTecnicosYHoras() {
        ordenDAO.agregar(crearOrden(20, new EstadoEnProceso().getEstadoId()));
        detalleDAO.agregar(crearLinea(201, 20, "Cambio de frenos", new BigDecimal("90000.00")));
        detalleDAO.asignarTecnico(201, 7, new BigDecimal("2.50"));

        repuestoDAO.agregar(crearRepuesto(4, "Pastillas de freno", 3, 2));
        loteDAO.agregar(crearLote(40, 4, null, 12));
        detalleRepuestoDAO.agregar(crearReserva(201, 4, 3, "Pastillas de freno", new BigDecimal("120000.00")));

        ResultadoCancelarOrden resultado = controller.cancelarOrden(20);

        assertEquals(new EstadoCancelada().getEstadoId(), resultado.getOrden().getEstadoId());
        assertNotNull(resultado.getOrden().getFechaCancelacion());
        assertEquals(1, resultado.getReservasLiberadas().size());
        assertEquals(3, resultado.getTotalUnidadesLiberadas());
        assertEquals(0, repuestoDAO.buscarPorId(4).getCantidadReservada());
        assertEquals(12, loteDAO.cantidadActual(40));

        // Se conservan técnicos y horas como historial (RF-14).
        List<DetalleTecnico> tecnicos = detalleDAO.listarTecnicosAsignados(201);
        assertEquals(1, tecnicos.size());
        assertEquals(7, tecnicos.get(0).getTecnicoId());
        assertEquals(0, new BigDecimal("2.50").compareTo(tecnicos.get(0).getHorasTrabajadas()));
    }

    @Test
    @DisplayName("Cancelar orden Pendiente sin repuestos reservados cambia a Cancelada sin liberar unidades")
    void cancelarOrdenSinRepuestosTransicionaACancelada() {
        ordenDAO.agregar(crearOrden(30, new EstadoPendiente().getEstadoId()));
        detalleDAO.agregar(crearLinea(301, 30, "Diagnóstico general", new BigDecimal("50000.00")));

        ResultadoCancelarOrden resultado = service.cancelarOrden(30);

        assertEquals(new EstadoCancelada().getEstadoId(), resultado.getOrden().getEstadoId());
        assertNotNull(resultado.getOrden().getFechaCancelacion());
        assertTrue(resultado.getReservasLiberadas().isEmpty());
        assertEquals(0, resultado.getTotalUnidadesLiberadas());
    }

    @Test
    @DisplayName("Rechaza cancelar órdenes en estados Completado, Facturado, Entregado o Cancelada (RN-06)")
    void rechazaCancelarEnEstadosNoPermitidos() {
        int[] estadosNoPermitidos = {
                new EstadoCompletado().getEstadoId(),
                new EstadoFacturado().getEstadoId(),
                new EstadoEntregado().getEstadoId(),
                new EstadoCancelada().getEstadoId()
        };

        repuestoDAO.agregar(crearRepuesto(5, "Bujía", 2, 2));

        int idOrden = 40;
        for (int estadoId : estadosNoPermitidos) {
            ordenDAO.agregar(crearOrden(idOrden, estadoId));
            detalleDAO.agregar(crearLinea(idOrden * 10, idOrden, "Servicio", new BigDecimal("40000.00")));
            detalleRepuestoDAO.agregar(crearReserva(idOrden * 10, 5, 1, "Bujía", new BigDecimal("45000.00")));

            int ordenActual = idOrden;
            assertThrows(TransicionEstadoInvalidaException.class, () -> service.cancelarOrden(ordenActual));
            assertEquals(estadoId, ordenDAO.buscarPorId(ordenActual).getEstadoId());
            assertNull(ordenDAO.buscarPorId(ordenActual).getFechaCancelacion());
            idOrden++;
        }

        // Ninguna reserva fue tocada.
        assertEquals(2, repuestoDAO.buscarPorId(5).getCantidadReservada());
    }

    @Test
    @DisplayName("Rechaza ID no positivo u orden inexistente al cancelar")
    void rechazaIdInvalidoYOrdenInexistente() {
        assertThrows(ValidacionException.class, () -> service.cancelarOrden(0));
        assertThrows(ValidacionException.class, () -> service.cancelarOrden(-5));
        assertThrows(EntidadNoEncontradaException.class, () -> service.cancelarOrden(999));
    }

    @Test
    @DisplayName("Orden cancelada bloquea definitivamente ediciones y transiciones posteriores (HU-08, RN-05, RN-06)")
    void ordenCanceladaBloqueaModificacionesYTransicionesPosteriores() {
        ordenDAO.agregar(crearOrden(50, new EstadoPendiente().getEstadoId()));
        detalleDAO.agregar(crearLinea(501, 50, "Alineación", new BigDecimal("70000.00")));
        repuestoDAO.agregar(crearRepuesto(6, "Líquido de frenos", 1, 2));
        detalleRepuestoDAO.agregar(crearReserva(501, 6, 1, "Líquido de frenos", new BigDecimal("28000.00")));

        service.cancelarOrden(50);

        assertThrows(ValidacionException.class, () -> service.validarOrdenEditable(50));
        assertThrows(ValidacionException.class, () -> service.agregarLinea(
                50, new OrdenServicioDetalle(1, null), null, false));
        assertThrows(ValidacionException.class, () -> service.quitarLinea(50, 501));
        assertThrows(ValidacionException.class, () -> service.asignarTecnicoALinea(
                50, 501, 1, BigDecimal.ZERO, false));
        assertThrows(ValidacionException.class, () -> service.reservarRepuesto(501, 6, 1));
        assertThrows(ValidacionException.class, () -> service.retirarRepuesto(501, 6));
        assertThrows(TransicionEstadoInvalidaException.class, () -> service.pasarAEnProceso(50));
        assertThrows(TransicionEstadoInvalidaException.class, () -> service.completarOrden(50));
        assertThrows(TransicionEstadoInvalidaException.class, () -> service.cancelarOrden(50));
    }

    @Test
    @DisplayName("Falla con CarMotorsException si la reserva es inconsistente o falla el marcado de cancelación")
    void falloEnReservaInconsistenteOMarcarCanceladaLanzaExcepcion() {
        // Caso 1: reserva inconsistente (asignado 4, reservado actual 1 -> descontarReserva devuelve false).
        ordenDAO.agregar(crearOrden(60, new EstadoPendiente().getEstadoId()));
        detalleDAO.agregar(crearLinea(601, 60, "Cambio de aceite", new BigDecimal("60000.00")));
        repuestoDAO.agregar(crearRepuesto(10, "Aceite", 1, 5));
        detalleRepuestoDAO.agregar(crearReserva(601, 10, 4, "Aceite", new BigDecimal("38000.00")));

        CarMotorsException errorReserva = assertThrows(CarMotorsException.class, () -> service.cancelarOrden(60));
        assertTrue(errorReserva.getMessage().contains("inconsistente"));
        assertEquals(new EstadoPendiente().getEstadoId(), ordenDAO.buscarPorId(60).getEstadoId());
        assertNull(ordenDAO.buscarPorId(60).getFechaCancelacion());
        assertEquals(1, repuestoDAO.buscarPorId(10).getCantidadReservada());

        // Caso 2: marcarCancelada devuelve false.
        ordenDAO.agregar(crearOrden(61, new EstadoPendiente().getEstadoId()));
        detalleDAO.agregar(crearLinea(611, 61, "Diagnóstico", new BigDecimal("40000.00")));
        ordenDAO.fallarMarcarCancelada = true;

        assertThrows(CarMotorsException.class, () -> service.cancelarOrden(61));
        assertEquals(new EstadoPendiente().getEstadoId(), ordenDAO.buscarPorId(61).getEstadoId());
        assertNull(ordenDAO.buscarPorId(61).getFechaCancelacion());
    }

    @Test
    @DisplayName("Falla si el servicio no fue configurado con DetalleRepuestoDAO")
    void rechazaCancelarSinDetalleRepuestoDao() {
        OrdenServicioService servicioSinReservas = new OrdenServicioService(
                ordenDAO, detalleDAO, new VehiculoDAOVacio(), new ClienteDAOVacio(),
                new ServicioTipoDAOVacio(), new TecnicoDAOVacio(), repuestoDAO, false);

        assertThrows(CarMotorsException.class, () -> servicioSinReservas.cancelarOrden(1));
    }

    private OrdenServicio crearOrden(int id, int estadoId) {
        OrdenServicio orden = new OrdenServicio();
        orden.setId(id);
        orden.setVehiculoId(1);
        orden.setEstadoId(estadoId);
        orden.setFechaRecepcion(LocalDateTime.now().minusHours(2).withNano(0));
        orden.setDescripcionProblema("Problema de prueba");
        return orden;
    }

    private OrdenServicioDetalle crearLinea(int id, int ordenId, String nombreServicio, BigDecimal manoObra) {
        OrdenServicioDetalle linea = new OrdenServicioDetalle();
        linea.setId(id);
        linea.setOrdenId(ordenId);
        linea.setServicioTipoId(1);
        linea.setNombreServicioSnapshot(nombreServicio);
        linea.setCostoManoObra(manoObra);
        return linea;
    }

    private Repuesto crearRepuesto(int id, String nombre, int reservado, int stockMinimo) {
        Repuesto repuesto = new Repuesto();
        repuesto.setId(id);
        repuesto.setRepuestoTipoId(1);
        repuesto.setNombre(nombre);
        repuesto.setMarca("Marca");
        repuesto.setPrecioVenta(new BigDecimal("25000.00"));
        repuesto.setCantidadReservada(reservado);
        repuesto.setStockMinimo(stockMinimo);
        repuesto.setActivo(true);
        return repuesto;
    }

    private Lote crearLote(int id, int repuestoId, LocalDate caducidad, int cantidadActual) {
        Lote lote = new Lote();
        lote.setId(id);
        lote.setCodigoLote("LOT-" + id);
        lote.setProveedorId(1);
        lote.setRepuestoId(repuestoId);
        lote.setFechaIngreso(LocalDate.now().minusDays(10));
        lote.setFechaCaducidad(caducidad);
        lote.setCantidadInicial(cantidadActual);
        lote.setCantidadActual(cantidadActual);
        return lote;
    }

    private DetalleRepuesto crearReserva(int detalleId, int repuestoId, int cantidad,
                                         String nombreSnapshot, BigDecimal precioUnitario) {
        DetalleRepuesto reserva = new DetalleRepuesto();
        reserva.setDetalleId(detalleId);
        reserva.setRepuestoId(repuestoId);
        reserva.setCantidadAsignada(cantidad);
        reserva.setNombreRepuestoSnapshot(nombreSnapshot);
        reserva.setPrecioUnitarioVenta(precioUnitario);
        return reserva;
    }

    private static class OrdenServicioDAOMock implements OrdenServicioDAO {
        private final Map<Integer, OrdenServicio> ordenes = new HashMap<>();
        private boolean fallarMarcarCancelada = false;

        void agregar(OrdenServicio orden) {
            ordenes.put(orden.getId(), orden);
        }

        @Override
        public int guardar(OrdenServicio orden) {
            throw new UnsupportedOperationException("No se usa en estas pruebas.");
        }

        @Override
        public OrdenServicio buscarPorId(int id) {
            return ordenes.get(id);
        }

        @Override
        public OrdenServicio buscarPorIdParaActualizar(int id) {
            return ordenes.get(id);
        }

        @Override
        public boolean actualizarEstado(int ordenId, int nuevoEstadoId) {
            OrdenServicio orden = ordenes.get(ordenId);
            if (orden == null) {
                return false;
            }
            orden.setEstadoId(nuevoEstadoId);
            return true;
        }

        @Override
        public boolean marcarCompletada(int ordenId, int estadoId, LocalDateTime fechaCompletado) {
            return false;
        }

        @Override
        public boolean marcarCancelada(int ordenId, int estadoId, LocalDateTime fechaCancelacion) {
            if (fallarMarcarCancelada) {
                return false;
            }
            OrdenServicio orden = ordenes.get(ordenId);
            if (orden == null) {
                return false;
            }
            orden.setEstadoId(estadoId);
            orden.setFechaCancelacion(fechaCancelacion);
            return true;
        }

        @Override
        public boolean marcarEntregada(int ordenId, int estadoId, LocalDateTime fechaEntrega) {
            return false;
        }
    }

    private static class OrdenServicioDetalleDAOMock implements OrdenServicioDetalleDAO {
        private final List<OrdenServicioDetalle> detalles = new ArrayList<>();
        private final Map<Integer, List<DetalleTecnico>> tecnicosPorDetalle = new HashMap<>();

        void agregar(OrdenServicioDetalle detalle) {
            detalles.add(detalle);
        }

        @Override
        public int guardar(OrdenServicioDetalle detalle) {
            throw new UnsupportedOperationException("No se usa en estas pruebas.");
        }

        @Override
        public List<OrdenServicioDetalle> listarPorOrden(int ordenId) {
            List<OrdenServicioDetalle> resultado = new ArrayList<>();
            for (OrdenServicioDetalle detalle : detalles) {
                if (detalle.getOrdenId() == ordenId) {
                    resultado.add(detalle);
                }
            }
            return resultado;
        }

        @Override
        public List<OrdenServicioDetalle> listarPorOrdenParaActualizar(int ordenId) {
            return listarPorOrden(ordenId);
        }

        @Override
        public void asignarTecnico(int detalleId, int tecnicoId) {
            asignarTecnico(detalleId, tecnicoId, BigDecimal.ZERO);
        }

        @Override
        public void asignarTecnico(int detalleId, int tecnicoId, BigDecimal horasTrabajadas) {
            List<DetalleTecnico> lista = tecnicosPorDetalle.get(detalleId);
            if (lista == null) {
                lista = new ArrayList<>();
                tecnicosPorDetalle.put(detalleId, lista);
            }
            lista.add(new DetalleTecnico(detalleId, tecnicoId, horasTrabajadas));
        }

        @Override
        public boolean existeTecnicoAsignado(int detalleId, int tecnicoId) {
            List<DetalleTecnico> lista = tecnicosPorDetalle.get(detalleId);
            if (lista == null) {
                return false;
            }
            for (DetalleTecnico asignacion : lista) {
                if (asignacion.getTecnicoId() == tecnicoId) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public List<DetalleTecnico> listarTecnicosAsignados(int detalleId) {
            List<DetalleTecnico> lista = tecnicosPorDetalle.get(detalleId);
            return lista == null ? new ArrayList<>() : new ArrayList<>(lista);
        }

        @Override
        public boolean actualizarHorasTrabajadas(int detalleId, int tecnicoId, BigDecimal horasTrabajadas) {
            return false;
        }

        @Override
        public boolean tieneHorasTrabajadas(int detalleId) {
            return false;
        }

        @Override
        public Map<Integer, Integer> listarReservasParaActualizar(int detalleId) {
            return new HashMap<>();
        }

        @Override
        public void eliminar(int ordenId, int detalleId) {
            throw new UnsupportedOperationException("No se usa en estas pruebas.");
        }

        @Override
        public OrdenServicioDetalle buscarPorId(int id) {
            for (OrdenServicioDetalle detalle : detalles) {
                if (detalle.getId() == id) {
                    return detalle;
                }
            }
            return null;
        }
    }

    private static class DetalleRepuestoDAOMock implements DetalleRepuestoDAO {
        private final List<DetalleRepuesto> datos = new ArrayList<>();

        void agregar(DetalleRepuesto detalleRepuesto) {
            datos.add(detalleRepuesto);
        }

        @Override
        public boolean guardar(DetalleRepuesto detalleRepuesto) {
            throw new UnsupportedOperationException("No se usa en estas pruebas.");
        }

        @Override
        public DetalleRepuesto buscar(int detalleId, int repuestoId) {
            for (DetalleRepuesto detalleRepuesto : datos) {
                if (detalleRepuesto.getDetalleId() == detalleId && detalleRepuesto.getRepuestoId() == repuestoId) {
                    return detalleRepuesto;
                }
            }
            return null;
        }

        @Override
        public List<DetalleRepuesto> listarPorDetalle(int detalleId) {
            List<DetalleRepuesto> resultado = new ArrayList<>();
            for (DetalleRepuesto detalleRepuesto : datos) {
                if (detalleRepuesto.getDetalleId() == detalleId) {
                    resultado.add(detalleRepuesto);
                }
            }
            return resultado;
        }

        @Override
        public boolean eliminar(int detalleId, int repuestoId) {
            throw new UnsupportedOperationException("No se usa en estas pruebas.");
        }
    }

    private static class RepuestoDAOMock implements RepuestoDAO {
        private final Map<Integer, Repuesto> repuestos = new HashMap<>();

        void agregar(Repuesto repuesto) {
            repuestos.put(repuesto.getId(), repuesto);
        }

        @Override
        public List<Repuesto> listarTodos() {
            return new ArrayList<>(repuestos.values());
        }

        @Override
        public List<Repuesto> listarActivos() {
            return new ArrayList<>(repuestos.values());
        }

        @Override
        public Repuesto buscarPorId(int id) {
            return repuestos.get(id);
        }

        @Override
        public Repuesto buscarPorNombre(String nombre) {
            return null;
        }

        @Override
        public int guardar(Repuesto repuesto) {
            return 0;
        }

        @Override
        public boolean actualizar(Repuesto repuesto) {
            return false;
        }

        @Override
        public boolean desactivar(int id) {
            return false;
        }

        @Override
        public boolean reactivar(int id) {
            return false;
        }

        @Override
        public boolean descontarReserva(int repuestoId, int cantidad) {
            Repuesto repuesto = repuestos.get(repuestoId);
            if (repuesto == null || repuesto.getCantidadReservada() < cantidad) {
                return false;
            }
            repuesto.setCantidadReservada(repuesto.getCantidadReservada() - cantidad);
            return true;
        }

        @Override
        public boolean aumentarReservado(int repuestoId, int cantidad) {
            return false;
        }
    }

    private static class LoteDAOMock implements LoteDAO {
        private final List<Lote> lotes = new ArrayList<>();
        private int descuentosRealizados = 0;

        void agregar(Lote lote) {
            lotes.add(lote);
        }

        int cantidadActual(int loteId) {
            for (Lote lote : lotes) {
                if (lote.getId() == loteId) {
                    return lote.getCantidadActual();
                }
            }
            throw new IllegalArgumentException("Lote inexistente: " + loteId);
        }

        @Override
        public Lote buscarPorCodigo(String codigoLote) {
            return null;
        }

        @Override
        public List<Lote> listarPorRepuesto(int repuestoId) {
            List<Lote> resultado = new ArrayList<>();
            for (Lote lote : lotes) {
                if (lote.getRepuestoId() == repuestoId) {
                    resultado.add(lote);
                }
            }
            return resultado;
        }

        @Override
        public int guardar(Lote lote) {
            return 0;
        }

        @Override
        public int sumarStockFisico(int repuestoId) {
            int total = 0;
            for (Lote lote : listarPorRepuesto(repuestoId)) {
                total += lote.getCantidadActual();
            }
            return total;
        }

        @Override
        public int sumarStockOperativo(int repuestoId, LocalDate fechaReferencia) {
            int total = 0;
            for (Lote lote : listarPorRepuesto(repuestoId)) {
                if (lote.getFechaCaducidad() == null || !lote.getFechaCaducidad().isBefore(fechaReferencia)) {
                    total += lote.getCantidadActual();
                }
            }
            return total;
        }

        @Override
        public List<Lote> listarPorVencer(LocalDate fechaLimite) {
            return new ArrayList<>();
        }

        @Override
        public boolean descontarUnidades(int loteId, int cantidad) {
            descuentosRealizados++;
            return false;
        }
    }

    private static class ProveedorRepuestoDAOVacio implements ProveedorRepuestoDAO {
        @Override
        public List<ProveedorRepuesto> listarPorProveedor(int proveedorId) {
            return new ArrayList<>();
        }

        @Override
        public List<ProveedorRepuesto> listarPorRepuesto(int repuestoId) {
            return new ArrayList<>();
        }

        @Override
        public ProveedorRepuesto buscarPorProveedorYRepuesto(int proveedorId, int repuestoId) {
            return null;
        }

        @Override
        public boolean guardar(ProveedorRepuesto proveedorRepuesto) {
            return false;
        }

        @Override
        public boolean actualizarCondiciones(ProveedorRepuesto proveedorRepuesto) {
            return false;
        }

        @Override
        public boolean eliminar(int proveedorId, int repuestoId) {
            return false;
        }

        @Override
        public int desmarcarHabitual(int repuestoId) {
            return 0;
        }

        @Override
        public boolean marcarHabitual(int proveedorId, int repuestoId) {
            return false;
        }
    }

    private static class ProveedorDAOVacio implements ProveedorDAO {
        @Override
        public List<Proveedor> listarTodos() {
            return new ArrayList<>();
        }

        @Override
        public List<Proveedor> listarActivos() {
            return new ArrayList<>();
        }

        @Override
        public Proveedor buscarPorId(int id) {
            return null;
        }

        @Override
        public Proveedor buscarPorEmail(String email) {
            return null;
        }

        @Override
        public int guardar(Proveedor proveedor) {
            return 0;
        }

        @Override
        public boolean actualizar(Proveedor proveedor) {
            return false;
        }

        @Override
        public boolean desactivar(int id) {
            return false;
        }

        @Override
        public boolean reactivar(int id) {
            return false;
        }
    }

    private static class VehiculoDAOVacio implements VehiculoDAO {
        @Override
        public int guardar(Vehiculo vehiculo) {
            return 0;
        }

        @Override
        public Vehiculo buscarPorId(int id) {
            return null;
        }

        @Override
        public Vehiculo buscarPorPlaca(String placa) {
            return null;
        }

        @Override
        public List<Vehiculo> listarPorCliente(int clienteId) {
            return new ArrayList<>();
        }

        @Override
        public List<Vehiculo> listarTodos() {
            return new ArrayList<>();
        }
    }

    private static class ClienteDAOVacio implements ClienteDAO {
        @Override
        public List<Cliente> listarTodos() {
            return new ArrayList<>();
        }

        @Override
        public List<Cliente> listarActivos() {
            return new ArrayList<>();
        }

        @Override
        public Cliente buscarPorId(int id) {
            return null;
        }

        @Override
        public Cliente buscarPorDocumento(String documento) {
            return null;
        }

        @Override
        public Cliente buscarPorEmail(String email) {
            return null;
        }

        @Override
        public int guardar(Cliente cliente) {
            return 0;
        }

        @Override
        public boolean actualizar(Cliente cliente) {
            return false;
        }

        @Override
        public boolean actualizarEstadoActivo(int id, boolean activo) {
            return false;
        }
    }

    private static class ServicioTipoDAOVacio implements ServicioTipoDAO {
        @Override
        public List<ServicioTipo> listarTodos() {
            return new ArrayList<>();
        }

        @Override
        public ServicioTipo buscarPorId(int id) {
            return null;
        }

        @Override
        public ServicioTipo buscarPorNombre(String nombre) {
            return null;
        }

        @Override
        public int guardar(ServicioTipo servicioTipo) {
            return 0;
        }

        @Override
        public boolean actualizar(ServicioTipo servicioTipo) {
            return false;
        }

        @Override
        public boolean eliminar(int id) {
            return false;
        }
    }

    private static class TecnicoDAOVacio implements TecnicoDAO {
        @Override
        public List<Tecnico> listarTodos() {
            return new ArrayList<>();
        }

        @Override
        public List<Tecnico> listarActivos() {
            return new ArrayList<>();
        }

        @Override
        public Tecnico buscarPorId(int id) {
            return null;
        }

        @Override
        public Tecnico buscarPorIdParaActualizar(int id) {
            return null;
        }

        @Override
        public Tecnico buscarPorDocumento(String documento) {
            return null;
        }

        @Override
        public int guardar(Tecnico tecnico) {
            return 0;
        }

        @Override
        public boolean actualizar(Tecnico tecnico) {
            return false;
        }

        @Override
        public boolean actualizarEstadoActivo(int id, boolean activo) {
            return false;
        }

        @Override
        public List<CargaLineaTecnico> listarCargaPorTecnico(int tecnicoId) {
            return new ArrayList<>();
        }

        @Override
        public Map<Integer, Integer> contarCargaActivaPorTecnico() {
            return new HashMap<>();
        }
    }
}
