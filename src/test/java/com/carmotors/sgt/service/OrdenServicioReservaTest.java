package com.carmotors.sgt.service;

import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.StockInsuficienteException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Cliente;
import com.carmotors.sgt.model.DetalleRepuesto;
import com.carmotors.sgt.model.DetalleTecnico;
import com.carmotors.sgt.model.Lote;
import com.carmotors.sgt.model.OrdenServicio;
import com.carmotors.sgt.model.OrdenServicioDetalle;
import com.carmotors.sgt.model.Repuesto;
import com.carmotors.sgt.model.ServicioTipo;
import com.carmotors.sgt.model.Vehiculo;
import com.carmotors.sgt.dao.ClienteDAO;
import com.carmotors.sgt.dao.DetalleRepuestoDAO;
import com.carmotors.sgt.dao.LoteDAO;
import com.carmotors.sgt.dao.OrdenServicioDAO;
import com.carmotors.sgt.dao.OrdenServicioDetalleDAO;
import com.carmotors.sgt.dao.RepuestoDAO;
import com.carmotors.sgt.dao.ServicioTipoDAO;
import com.carmotors.sgt.dao.VehiculoDAO;
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
 * Pruebas de la reserva de repuestos en líneas de servicio (T4.7, RF-10, RN-01, RN-02, RN-11)
 * con DAOs en memoria.
 */
class OrdenServicioReservaTest {

    private static final int ORDEN_PENDIENTE = 1;
    private static final int ORDEN_EN_PROCESO = 2;
    private static final int ORDEN_COMPLETADA = 3;
    private static final int LINEA_PENDIENTE = 1;
    private static final int OTRA_LINEA_PENDIENTE = 2;
    private static final int LINEA_COMPLETADA = 3;
    private static final int LINEA_EN_PROCESO = 4;
    private static final int DISCO_FRENO = 5;
    private static final int CORREA_INACTIVA = 11;

    private OrdenServicioService service;
    private RepuestoDAOMock repuestoDAO;
    private DetalleRepuestoDAOMock detalleRepuestoDAO;
    private LoteDAOMock loteDAO;

    @BeforeEach
    void setUp() {
        OrdenServicioDAOMock ordenDAO = new OrdenServicioDAOMock();
        ordenDAO.agregar(new OrdenServicio(ORDEN_PENDIENTE, 1, 1, LocalDateTime.now(), "Cambio de aceite"));
        ordenDAO.agregar(new OrdenServicio(ORDEN_EN_PROCESO, 2, 2, LocalDateTime.now(), "Frenos ruidosos"));
        ordenDAO.agregar(new OrdenServicio(ORDEN_COMPLETADA, 3, 3, LocalDateTime.now(), "No enciende"));

        OrdenServicioDetalleDAOMock detalleDAO = new OrdenServicioDetalleDAOMock();
        detalleDAO.agregar(new OrdenServicioDetalle(LINEA_PENDIENTE, ORDEN_PENDIENTE, 1,
                "Cambio de aceite y filtro", new BigDecimal("60000.00"), null));
        detalleDAO.agregar(new OrdenServicioDetalle(OTRA_LINEA_PENDIENTE, ORDEN_PENDIENTE, 2,
                "Revisión de frenos", new BigDecimal("90000.00"), null));
        detalleDAO.agregar(new OrdenServicioDetalle(LINEA_COMPLETADA, ORDEN_COMPLETADA, 3,
                "Diagnóstico eléctrico", new BigDecimal("50000.00"), null));
        detalleDAO.agregar(new OrdenServicioDetalle(LINEA_EN_PROCESO, ORDEN_EN_PROCESO, 2,
                "Revisión de frenos", new BigDecimal("90000.00"), null));

        repuestoDAO = new RepuestoDAOMock();
        repuestoDAO.agregar(new Repuesto(DISCO_FRENO, 1, "Disco de freno delantero", "Brembo",
                new BigDecimal("210000"), 0, 4, true));
        repuestoDAO.agregar(new Repuesto(CORREA_INACTIVA, 1, "Correa de distribución", "Gates",
                new BigDecimal("150000.00"), 0, 2, false));

        loteDAO = new LoteDAOMock();
        loteDAO.fijarStock(DISCO_FRENO, 3);
        loteDAO.fijarStock(CORREA_INACTIVA, 5);

        detalleRepuestoDAO = new DetalleRepuestoDAOMock();
        InventarioService inventarioService = new InventarioService(repuestoDAO, loteDAO);

        service = new OrdenServicioService(ordenDAO, detalleDAO, new VehiculoDAOVacio(), new ClienteDAOVacio(),
                new ServicioTipoDAOVacio(), repuestoDAO, detalleRepuestoDAO, inventarioService, false);
    }

    @Test
    @DisplayName("Reserva válida: aumenta el reservado y guarda el snapshot de nombre y precio (RN-02, RN-11)")
    void reservarRepuestoValido() {
        DetalleRepuesto reserva = service.reservarRepuesto(LINEA_PENDIENTE, DISCO_FRENO, 2);

        assertNotNull(reserva);
        assertEquals(LINEA_PENDIENTE, reserva.getDetalleId());
        assertEquals(DISCO_FRENO, reserva.getRepuestoId());
        assertEquals(2, reserva.getCantidadAsignada());
        assertEquals("Disco de freno delantero", reserva.getNombreRepuestoSnapshot());
        assertEquals(new BigDecimal("210000.00"), reserva.getPrecioUnitarioVenta());

        assertEquals(2, repuestoDAO.buscarPorId(DISCO_FRENO).getCantidadReservada());
        assertEquals(3, loteDAO.sumarStockFisico(DISCO_FRENO));
        assertEquals(1, service.consultarDisponible(DISCO_FRENO));

        DetalleRepuesto guardada = detalleRepuestoDAO.buscar(LINEA_PENDIENTE, DISCO_FRENO);
        assertNotNull(guardada);
        assertEquals("Disco de freno delantero", guardada.getNombreRepuestoSnapshot());
        assertEquals(new BigDecimal("210000.00"), guardada.getPrecioUnitarioVenta());
    }

    @Test
    @DisplayName("Rechaza una cantidad igual a cero o negativa")
    void rechazarCantidadNoPositiva() {
        assertThrows(ValidacionException.class, () -> service.reservarRepuesto(LINEA_PENDIENTE, DISCO_FRENO, 0));
        assertThrows(ValidacionException.class, () -> service.reservarRepuesto(LINEA_PENDIENTE, DISCO_FRENO, -3));
        assertNadaReservado();
    }

    @Test
    @DisplayName("Rechaza una cantidad mayor al disponible e informa el disponible (RN-01)")
    void rechazarCantidadMayorAlDisponible() {
        service.reservarRepuesto(LINEA_PENDIENTE, DISCO_FRENO, 2);

        StockInsuficienteException error = assertThrows(StockInsuficienteException.class,
                () -> service.reservarRepuesto(OTRA_LINEA_PENDIENTE, DISCO_FRENO, 5));

        assertTrue(error.getMessage().contains("disponible 1"), error.getMessage());
        assertEquals(2, repuestoDAO.buscarPorId(DISCO_FRENO).getCantidadReservada());
        assertEquals(1, detalleRepuestoDAO.datos.size());
    }

    @Test
    @DisplayName("Rechaza un repuesto inexistente")
    void rechazarRepuestoInexistente() {
        assertThrows(EntidadNoEncontradaException.class,
                () -> service.reservarRepuesto(LINEA_PENDIENTE, 99, 1));
        assertNadaReservado();
    }

    @Test
    @DisplayName("Rechaza un repuesto inactivo aunque tenga stock")
    void rechazarRepuestoInactivo() {
        ValidacionException error = assertThrows(ValidacionException.class,
                () -> service.reservarRepuesto(LINEA_PENDIENTE, CORREA_INACTIVA, 1));
        assertTrue(error.getMessage().contains("inactivo"), error.getMessage());
        assertNadaReservado();
    }

    @Test
    @DisplayName("Rechaza una línea de servicio inexistente")
    void rechazarLineaInexistente() {
        assertThrows(EntidadNoEncontradaException.class,
                () -> service.reservarRepuesto(42, DISCO_FRENO, 1));
        assertNadaReservado();
    }

    @Test
    @DisplayName("Rechaza reservar en una orden cuyo estado no permite cambios (Completado)")
    void rechazarOrdenQueNoPermiteCambios() {
        ValidacionException error = assertThrows(ValidacionException.class,
                () -> service.reservarRepuesto(LINEA_COMPLETADA, DISCO_FRENO, 1));
        assertTrue(error.getMessage().contains("no permite cambios"), error.getMessage());
        assertNadaReservado();
    }

    @Test
    @DisplayName("Rechaza un repuesto ya asignado a la misma línea")
    void rechazarRepuestoYaAsignado() {
        service.reservarRepuesto(LINEA_PENDIENTE, DISCO_FRENO, 1);

        ValidacionException error = assertThrows(ValidacionException.class,
                () -> service.reservarRepuesto(LINEA_PENDIENTE, DISCO_FRENO, 1));

        assertTrue(error.getMessage().contains("ya está asignado"), error.getMessage());
        assertEquals(1, repuestoDAO.buscarPorId(DISCO_FRENO).getCantidadReservada());
        assertEquals(1, detalleRepuestoDAO.datos.size());
    }

    @Test
    @DisplayName("Retiro válido en orden Pendiente: el reservado vuelve a su valor previo y no toca el stock físico (RN-05, HU-06)")
    void retirarRepuestoEnOrdenPendiente() {
        service.reservarRepuesto(LINEA_PENDIENTE, DISCO_FRENO, 2);
        assertEquals(2, repuestoDAO.buscarPorId(DISCO_FRENO).getCantidadReservada());

        DetalleRepuesto retirada = service.retirarRepuesto(LINEA_PENDIENTE, DISCO_FRENO);

        assertNotNull(retirada);
        assertEquals(LINEA_PENDIENTE, retirada.getDetalleId());
        assertEquals(DISCO_FRENO, retirada.getRepuestoId());
        assertEquals(2, retirada.getCantidadAsignada());
        assertEquals("Disco de freno delantero", retirada.getNombreRepuestoSnapshot());
        assertEquals(0, repuestoDAO.buscarPorId(DISCO_FRENO).getCantidadReservada());
        assertNull(detalleRepuestoDAO.buscar(LINEA_PENDIENTE, DISCO_FRENO));
        assertEquals(3, loteDAO.sumarStockFisico(DISCO_FRENO));
        assertEquals(3, service.consultarDisponible(DISCO_FRENO));
    }

    @Test
    @DisplayName("Retiro válido en orden En Proceso")
    void retirarRepuestoEnOrdenEnProceso() {
        service.reservarRepuesto(LINEA_EN_PROCESO, DISCO_FRENO, 1);

        service.retirarRepuesto(LINEA_EN_PROCESO, DISCO_FRENO);

        assertEquals(0, repuestoDAO.buscarPorId(DISCO_FRENO).getCantidadReservada());
        assertTrue(detalleRepuestoDAO.listarPorDetalle(LINEA_EN_PROCESO).isEmpty());
    }

    @Test
    @DisplayName("Rechaza retirar repuestos en una orden Completada (RN-05)")
    void rechazarRetiroEnOrdenCompletada() {
        detalleRepuestoDAO.guardar(new DetalleRepuesto(LINEA_COMPLETADA, DISCO_FRENO, 1,
                "Disco de freno delantero", new BigDecimal("210000.00")));
        repuestoDAO.aumentarReservado(DISCO_FRENO, 1);

        ValidacionException error = assertThrows(ValidacionException.class,
                () -> service.retirarRepuesto(LINEA_COMPLETADA, DISCO_FRENO));

        assertTrue(error.getMessage().contains("no permite cambios"), error.getMessage());
        assertNotNull(detalleRepuestoDAO.buscar(LINEA_COMPLETADA, DISCO_FRENO));
        assertEquals(1, repuestoDAO.buscarPorId(DISCO_FRENO).getCantidadReservada());
    }

    @Test
    @DisplayName("Rechaza un repuesto que no está reservado en la línea")
    void rechazarRepuestoNoReservadoEnLinea() {
        assertThrows(EntidadNoEncontradaException.class,
                () -> service.retirarRepuesto(LINEA_PENDIENTE, DISCO_FRENO));
    }

    @Test
    @DisplayName("Rechaza retirar un repuesto de una línea inexistente")
    void rechazarRetiroDeLineaInexistente() {
        assertThrows(EntidadNoEncontradaException.class, () -> service.retirarRepuesto(42, DISCO_FRENO));
    }

    @Test
    @DisplayName("Rechaza IDs no positivos al retirar un repuesto")
    void rechazarIdsNoPositivosAlRetirar() {
        assertThrows(ValidacionException.class, () -> service.retirarRepuesto(0, DISCO_FRENO));
        assertThrows(ValidacionException.class, () -> service.retirarRepuesto(LINEA_PENDIENTE, 0));
    }

    @Test
    @DisplayName("Si la reserva es inconsistente no se elimina la fila y se informa el error")
    void reservaInconsistenteNoElimina() {
        detalleRepuestoDAO.guardar(new DetalleRepuesto(LINEA_PENDIENTE, DISCO_FRENO, 2,
                "Disco de freno delantero", new BigDecimal("210000.00")));

        CarMotorsException error = assertThrows(CarMotorsException.class,
                () -> service.retirarRepuesto(LINEA_PENDIENTE, DISCO_FRENO));

        assertTrue(error.getMessage().contains("inconsistente"), error.getMessage());
        assertNotNull(detalleRepuestoDAO.buscar(LINEA_PENDIENTE, DISCO_FRENO));
    }

    @Test
    @DisplayName("listarRepuestosDeLinea devuelve las reservas de la línea y valida el ID")
    void listarRepuestosDeLinea() {
        service.reservarRepuesto(LINEA_PENDIENTE, DISCO_FRENO, 2);

        List<DetalleRepuesto> reservas = service.listarRepuestosDeLinea(LINEA_PENDIENTE);

        assertEquals(1, reservas.size());
        assertEquals(DISCO_FRENO, reservas.get(0).getRepuestoId());
        assertTrue(service.listarRepuestosDeLinea(OTRA_LINEA_PENDIENTE).isEmpty());
        assertThrows(ValidacionException.class, () -> service.listarRepuestosDeLinea(0));
        assertThrows(EntidadNoEncontradaException.class, () -> service.listarRepuestosDeLinea(42));
    }

    private void assertNadaReservado() {
        assertTrue(detalleRepuestoDAO.datos.isEmpty());
        assertEquals(0, repuestoDAO.buscarPorId(DISCO_FRENO).getCantidadReservada());
    }

    // ------------------------------------------------------------------
    // DAOs en memoria
    // ------------------------------------------------------------------

    private static class OrdenServicioDAOMock implements OrdenServicioDAO {
        private final Map<Integer, OrdenServicio> ordenes = new HashMap<>();

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
            throw new UnsupportedOperationException("No se usa en estas pruebas.");
        }

        @Override
        public boolean marcarCompletada(int ordenId, int estadoId, LocalDateTime fechaCompletado) {
            return false;
        }

        @Override
        public boolean marcarCancelada(int ordenId, int estadoId, LocalDateTime fechaCancelacion) {
            return false;
        }

        @Override
        public boolean marcarEntregada(int ordenId, int estadoId, LocalDateTime fechaEntrega) {
            return false;
        }
    }

    private static class OrdenServicioDetalleDAOMock implements OrdenServicioDetalleDAO {
        private final List<OrdenServicioDetalle> detalles = new ArrayList<>();

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
        public OrdenServicioDetalle buscarPorId(int id) {
            for (OrdenServicioDetalle detalle : detalles) {
                if (detalle.getId() == id) {
                    return detalle;
                }
            }
            return null;
        }

        @Override
        public List<OrdenServicioDetalle> listarPorOrdenParaActualizar(int ordenId) {
            return listarPorOrden(ordenId);
        }

        @Override
        public void asignarTecnico(int detalleId, int tecnicoId) {
            throw new UnsupportedOperationException("No se usa en estas pruebas.");
        }

        @Override
        public void asignarTecnico(int detalleId, int tecnicoId, BigDecimal horasTrabajadas) {
            throw new UnsupportedOperationException("No se usa en estas pruebas.");
        }

        @Override
        public boolean existeTecnicoAsignado(int detalleId, int tecnicoId) {
            return false;
        }

        @Override
        public List<DetalleTecnico> listarTecnicosAsignados(int detalleId) {
            return new ArrayList<>();
        }

        @Override
        public boolean actualizarHorasTrabajadas(int detalleId, int tecnicoId, BigDecimal horasTrabajadas) {
            throw new UnsupportedOperationException("No se usa en estas pruebas.");
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
    }

    private static class RepuestoDAOMock implements RepuestoDAO {
        private final List<Repuesto> datos = new ArrayList<>();

        void agregar(Repuesto repuesto) {
            datos.add(repuesto);
        }

        @Override
        public List<Repuesto> listarTodos() {
            return new ArrayList<>(datos);
        }

        @Override
        public List<Repuesto> listarActivos() {
            List<Repuesto> activos = new ArrayList<>();
            for (Repuesto repuesto : datos) {
                if (repuesto.isActivo()) {
                    activos.add(repuesto);
                }
            }
            return activos;
        }

        @Override
        public Repuesto buscarPorId(int id) {
            for (Repuesto repuesto : datos) {
                if (repuesto.getId() == id) {
                    return repuesto;
                }
            }
            return null;
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
        public boolean aumentarReservado(int repuestoId, int cantidad) {
            Repuesto repuesto = buscarPorId(repuestoId);
            if (repuesto == null) {
                return false;
            }
            repuesto.setCantidadReservada(repuesto.getCantidadReservada() + cantidad);
            return true;
        }

        @Override
        public boolean descontarReserva(int repuestoId, int cantidad) {
            Repuesto repuesto = buscarPorId(repuestoId);
            if (repuesto == null || repuesto.getCantidadReservada() < cantidad) {
                return false;
            }
            repuesto.setCantidadReservada(repuesto.getCantidadReservada() - cantidad);
            return true;
        }
    }

    private static class DetalleRepuestoDAOMock implements DetalleRepuestoDAO {
        private final List<DetalleRepuesto> datos = new ArrayList<>();

        @Override
        public boolean guardar(DetalleRepuesto detalleRepuesto) {
            datos.add(detalleRepuesto);
            return true;
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
            DetalleRepuesto encontrada = buscar(detalleId, repuestoId);
            if (encontrada == null) {
                return false;
            }
            datos.remove(encontrada);
            return true;
        }
    }

    /** Lotes simplificados: stock físico igual al operativo (sin lotes vencidos). */
    private static class LoteDAOMock implements LoteDAO {
        private final Map<Integer, Integer> stockPorRepuesto = new HashMap<>();

        void fijarStock(int repuestoId, int cantidad) {
            stockPorRepuesto.put(repuestoId, cantidad);
        }

        @Override
        public Lote buscarPorCodigo(String codigoLote) {
            return null;
        }

        @Override
        public List<Lote> listarPorRepuesto(int repuestoId) {
            return new ArrayList<>();
        }

        @Override
        public int guardar(Lote lote) {
            return 0;
        }

        @Override
        public int sumarStockFisico(int repuestoId) {
            return stockPorRepuesto.getOrDefault(repuestoId, 0);
        }

        @Override
        public int sumarStockOperativo(int repuestoId, LocalDate fechaReferencia) {
            return stockPorRepuesto.getOrDefault(repuestoId, 0);
        }

        @Override
        public List<Lote> listarPorVencer(LocalDate fechaLimite) {
            return new ArrayList<>();
        }

        @Override
        public boolean descontarUnidades(int loteId, int cantidad) {
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
}
