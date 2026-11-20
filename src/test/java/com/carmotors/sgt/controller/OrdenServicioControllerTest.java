package com.carmotors.sgt.controller;

import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.CargaLineaTecnico;
import com.carmotors.sgt.model.Cliente;
import com.carmotors.sgt.model.DetalleTecnico;
import com.carmotors.sgt.model.OrdenServicio;
import com.carmotors.sgt.model.OrdenServicioDetalle;
import com.carmotors.sgt.model.Repuesto;
import com.carmotors.sgt.model.ServicioTipo;
import com.carmotors.sgt.model.Tecnico;
import com.carmotors.sgt.model.Vehiculo;
import com.carmotors.sgt.dao.ClienteDAO;
import com.carmotors.sgt.dao.OrdenServicioDAO;
import com.carmotors.sgt.dao.OrdenServicioDetalleDAO;
import com.carmotors.sgt.dao.RepuestoDAO;
import com.carmotors.sgt.dao.ServicioTipoDAO;
import com.carmotors.sgt.dao.TecnicoDAO;
import com.carmotors.sgt.dao.VehiculoDAO;
import com.carmotors.sgt.service.OrdenServicioService;
import com.carmotors.sgt.service.chain.SugerenciaTecnico;
import com.carmotors.sgt.service.state.EstadoCompletado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrdenServicioControllerTest {

    private OrdenServicioController controller;
    private OrdenServicio orden;
    private int detalleId;

    @BeforeEach
    void setUp() {
        OrdenServicioDAOMock ordenDAO = new OrdenServicioDAOMock();
        OrdenServicioDetalleDAOMock detalleDAO = new OrdenServicioDetalleDAOMock();
        VehiculoDAOMock vehiculoDAO = new VehiculoDAOMock();
        vehiculoDAO.agregar(new Vehiculo(1, 1, "ABC123", "Mazda", "3"));

        ClienteDAOMock clienteDAO = new ClienteDAOMock();
        clienteDAO.agregar(new Cliente(1, "Carlos Ruiz", "1098765432", "3001112233", "carlos@correo.com", true));

        ServicioTipoDAOMock servicioTipoDAO = new ServicioTipoDAOMock();
        servicioTipoDAO.agregar(new ServicioTipo(1, "Sincronización", 1, 2, new BigDecimal("120000.00"), 6));

        TecnicoDAOMock tecnicoDAO = new TecnicoDAOMock();
        tecnicoDAO.agregar(new Tecnico(1, "Laura", "1001", 2, "3001110001", true));
        tecnicoDAO.agregar(new Tecnico(2, "Mario", "1002", 3, "3001110002", true));
        tecnicoDAO.agregar(new Tecnico(3, "Inactivo", "1003", 2, "3001110003", false));

        OrdenServicioService service = new OrdenServicioService(
                ordenDAO, detalleDAO, vehiculoDAO, clienteDAO, servicioTipoDAO,
                tecnicoDAO, new RepuestoDAOMock(), false);
        controller = new OrdenServicioController(service);

        orden = controller.crearOrden(1, "Falla encendido",
                Collections.singletonList(new OrdenServicioDetalle(1, null)));
        detalleId = controller.listarLineas(orden.getId()).get(0).getId();
    }

    @Test
    @DisplayName("Delega sugerencia, valida edición y asigna técnico devolviendo mensaje claro")
    void sugerirYAsignarTecnicoConExito() {
        controller.validarOrdenEditable(orden.getId());
        SugerenciaTecnico sugerencia = controller.sugerirTecnicoParaLinea(orden.getId(), detalleId);

        assertNotNull(sugerencia);
        assertEquals(1, sugerencia.getTecnico().getId());

        String mensaje = controller.asignarTecnicoALinea(
                orden.getId(), detalleId, 1, new BigDecimal("1.50"), false);
        assertTrue(mensaje.contains("Técnico asignado a la línea " + detalleId));
        assertTrue(mensaje.contains("1.50 h"));

        List<DetalleTecnico> asignados = controller.listarTecnicosDeLinea(orden.getId(), detalleId);
        assertEquals(1, asignados.size());
        assertEquals(1, asignados.get(0).getTecnicoId());
    }

    @Test
    @DisplayName("Captura CarMotorsException al asignar inactivo, duplicado o especialidad sin confirmar")
    void asignarTecnicoCapturaErroresAmigables() {
        String errorInactivo = controller.asignarTecnicoALinea(
                orden.getId(), detalleId, 3, BigDecimal.ZERO, false);
        assertTrue(errorInactivo.startsWith("No se pudo asignar el técnico:"));

        String errorEspecialidad = controller.asignarTecnicoALinea(
                orden.getId(), detalleId, 2, BigDecimal.ZERO, false);
        assertTrue(errorEspecialidad.startsWith("No se pudo asignar el técnico:"));

        controller.asignarTecnicoALinea(orden.getId(), detalleId, 1, BigDecimal.ZERO, false);
        String errorDuplicado = controller.asignarTecnicoALinea(
                orden.getId(), detalleId, 1, BigDecimal.ZERO, false);
        assertTrue(errorDuplicado.startsWith("No se pudo asignar el técnico:"));
    }

    @Test
    @DisplayName("Registra horas trabajadas y devuelve mensajes amigables en éxito, fallo y orden bloqueada")
    void registrarHorasTrabajadasYEstadoBloqueado() {
        String sinAsignar = controller.registrarHorasTrabajadas(
                orden.getId(), detalleId, 1, new BigDecimal("2.00"));
        assertTrue(sinAsignar.startsWith("No se pudieron registrar las horas:"));

        controller.asignarTecnicoALinea(orden.getId(), detalleId, 1, BigDecimal.ZERO, false);
        String ok = controller.registrarHorasTrabajadas(
                orden.getId(), detalleId, 1, new BigDecimal("2.50"));
        assertEquals("Horas trabajadas registradas: 2.50 h.", ok);

        orden.setEstadoId(new EstadoCompletado().getEstadoId());
        assertThrows(ValidacionException.class, () -> controller.validarOrdenEditable(orden.getId()));
        String bloqueado = controller.asignarTecnicoALinea(
                orden.getId(), detalleId, 2, BigDecimal.ZERO, true);
        assertTrue(bloqueado.startsWith("No se pudo asignar el técnico:"));
    }

    private static class OrdenServicioDAOMock implements OrdenServicioDAO {
        private final Map<Integer, OrdenServicio> ordenes = new HashMap<>();
        private int secuencia = 0;

        @Override
        public int guardar(OrdenServicio orden) {
            secuencia++;
            ordenes.put(secuencia, orden);
            return secuencia;
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
            OrdenServicio orden = ordenes.get(ordenId);
            if (orden == null) {
                return false;
            }
            orden.setEstadoId(estadoId);
            orden.setFechaEntrega(fechaEntrega);
            return true;
        }
    }

    private static class OrdenServicioDetalleDAOMock implements OrdenServicioDetalleDAO {
        private final List<OrdenServicioDetalle> detalles = new ArrayList<>();
        private final Map<Integer, List<DetalleTecnico>> asignaciones = new HashMap<>();
        private int secuencia = 0;

        @Override
        public int guardar(OrdenServicioDetalle detalle) {
            secuencia++;
            detalles.add(detalle);
            return secuencia;
        }

        @Override
        public List<OrdenServicioDetalle> listarPorOrden(int ordenId) {
            List<OrdenServicioDetalle> resultado = new ArrayList<>();
            for (OrdenServicioDetalle d : detalles) {
                if (d.getOrdenId() == ordenId) {
                    resultado.add(d);
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
            asignarTecnico(detalleId, tecnicoId, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }

        @Override
        public void asignarTecnico(int detalleId, int tecnicoId, BigDecimal horasTrabajadas) {
            List<DetalleTecnico> lista = asignaciones.get(detalleId);
            if (lista == null) {
                lista = new ArrayList<>();
                asignaciones.put(detalleId, lista);
            }
            lista.add(new DetalleTecnico(detalleId, tecnicoId, horasTrabajadas.setScale(2, RoundingMode.HALF_UP)));
        }

        @Override
        public boolean existeTecnicoAsignado(int detalleId, int tecnicoId) {
            List<DetalleTecnico> lista = asignaciones.get(detalleId);
            if (lista == null) {
                return false;
            }
            for (DetalleTecnico a : lista) {
                if (a.getTecnicoId() == tecnicoId) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public List<DetalleTecnico> listarTecnicosAsignados(int detalleId) {
            List<DetalleTecnico> lista = asignaciones.get(detalleId);
            return lista == null ? new ArrayList<>() : new ArrayList<>(lista);
        }

        @Override
        public boolean actualizarHorasTrabajadas(int detalleId, int tecnicoId, BigDecimal horasTrabajadas) {
            List<DetalleTecnico> lista = asignaciones.get(detalleId);
            if (lista == null) {
                return false;
            }
            for (DetalleTecnico a : lista) {
                if (a.getTecnicoId() == tecnicoId) {
                    a.setHorasTrabajadas(horasTrabajadas.setScale(2, RoundingMode.HALF_UP));
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean tieneHorasTrabajadas(int detalleId) {
            return false;
        }

        @Override
        public Map<Integer, Integer> listarReservasParaActualizar(int detalleId) {
            return new LinkedHashMap<>();
        }

        @Override
        public void eliminar(int ordenId, int detalleId) {
            detalles.removeIf(d -> d.getOrdenId() == ordenId && d.getId() == detalleId);
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

    private static class VehiculoDAOMock implements VehiculoDAO {
        private final Map<Integer, Vehiculo> vehiculos = new HashMap<>();

        void agregar(Vehiculo v) {
            vehiculos.put(v.getId(), v);
        }

        @Override
        public int guardar(Vehiculo vehiculo) {
            return 0;
        }

        @Override
        public Vehiculo buscarPorId(int id) {
            return vehiculos.get(id);
        }

        @Override
        public Vehiculo buscarPorPlaca(String placa) {
            return null;
        }

        @Override
        public List<Vehiculo> listarPorCliente(int clienteId) {
            return Collections.emptyList();
        }

        @Override
        public List<Vehiculo> listarTodos() {
            return new ArrayList<>(vehiculos.values());
        }
    }

    private static class ClienteDAOMock implements ClienteDAO {
        private final Map<Integer, Cliente> clientes = new HashMap<>();

        void agregar(Cliente c) {
            clientes.put(c.getId(), c);
        }

        @Override
        public List<Cliente> listarTodos() {
            return new ArrayList<>(clientes.values());
        }

        @Override
        public List<Cliente> listarActivos() {
            return new ArrayList<>(clientes.values());
        }

        @Override
        public Cliente buscarPorId(int id) {
            return clientes.get(id);
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

    private static class ServicioTipoDAOMock implements ServicioTipoDAO {
        private final Map<Integer, ServicioTipo> tipos = new HashMap<>();

        void agregar(ServicioTipo t) {
            tipos.put(t.getId(), t);
        }

        @Override
        public List<ServicioTipo> listarTodos() {
            return new ArrayList<>(tipos.values());
        }

        @Override
        public ServicioTipo buscarPorId(int id) {
            return tipos.get(id);
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

    private static class TecnicoDAOMock implements TecnicoDAO {
        private final Map<Integer, Tecnico> tecnicos = new HashMap<>();

        void agregar(Tecnico t) {
            tecnicos.put(t.getId(), t);
        }

        @Override
        public List<Tecnico> listarTodos() {
            return new ArrayList<>(tecnicos.values());
        }

        @Override
        public List<Tecnico> listarActivos() {
            List<Tecnico> activos = new ArrayList<>();
            for (Tecnico t : tecnicos.values()) {
                if (t.isActivo()) {
                    activos.add(t);
                }
            }
            return activos;
        }

        @Override
        public Tecnico buscarPorId(int id) {
            return tecnicos.get(id);
        }

        @Override
        public Tecnico buscarPorIdParaActualizar(int id) {
            return tecnicos.get(id);
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
            return Collections.emptyList();
        }

        @Override
        public Map<Integer, Integer> contarCargaActivaPorTecnico() {
            return Collections.emptyMap();
        }
    }

    private static class RepuestoDAOMock implements RepuestoDAO {
        @Override
        public List<Repuesto> listarTodos() {
            return Collections.emptyList();
        }

        @Override
        public List<Repuesto> listarActivos() {
            return Collections.emptyList();
        }

        @Override
        public Repuesto buscarPorId(int id) {
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
        public boolean descontarReserva(int repuestoId, int cantidad) {
            return false;
        }

        @Override
        public boolean aumentarReservado(int repuestoId, int cantidad) {
            return false;
        }
    }
}
