package com.carmotors.sgt.service.facade;

import com.carmotors.sgt.controller.OrdenServicioController;
import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.CargaLineaTecnico;
import com.carmotors.sgt.model.Cliente;
import com.carmotors.sgt.model.DetalleTecnico;
import com.carmotors.sgt.model.Especialidad;
import com.carmotors.sgt.model.LineaOrdenSolicitada;
import com.carmotors.sgt.model.Lote;
import com.carmotors.sgt.model.OrdenServicio;
import com.carmotors.sgt.model.OrdenServicioDetalle;
import com.carmotors.sgt.model.Proveedor;
import com.carmotors.sgt.model.ProveedorRepuesto;
import com.carmotors.sgt.model.Repuesto;
import com.carmotors.sgt.model.ServicioTipo;
import com.carmotors.sgt.model.Tecnico;
import com.carmotors.sgt.model.Vehiculo;
import com.carmotors.sgt.dao.ClienteDAO;
import com.carmotors.sgt.dao.EspecialidadDAO;
import com.carmotors.sgt.dao.LoteDAO;
import com.carmotors.sgt.dao.OrdenServicioDAO;
import com.carmotors.sgt.dao.OrdenServicioDetalleDAO;
import com.carmotors.sgt.dao.ProveedorDAO;
import com.carmotors.sgt.dao.ProveedorRepuestoDAO;
import com.carmotors.sgt.dao.RepuestoDAO;
import com.carmotors.sgt.dao.ServicioTipoDAO;
import com.carmotors.sgt.dao.TecnicoDAO;
import com.carmotors.sgt.dao.VehiculoDAO;
import com.carmotors.sgt.dao.impl.RepuestoDAOImpl;
import com.carmotors.sgt.service.InventarioService;
import com.carmotors.sgt.service.OrdenServicioService;
import com.carmotors.sgt.service.TecnicoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias de {@link TallerFacade} y guardas transaccionales de {@link RepuestoDAOImpl} (T5.4).
 */
class TallerFacadeTest {

    private OrdenServicioService ordenServicioService;
    private InventarioService inventarioService;
    private TecnicoService tecnicoService;
    private TallerFacade tallerFacade;
    private OrdenServicioController controller;

    @BeforeEach
    void setUp() {
        OrdenServicioDAOMock ordenDAO = new OrdenServicioDAOMock();
        OrdenServicioDetalleDAOMock detalleDAO = new OrdenServicioDetalleDAOMock();
        VehiculoDAOMock vehiculoDAO = new VehiculoDAOMock();
        vehiculoDAO.agregar(new Vehiculo(1, 1, "ABC123", "Chevrolet", "Spark"));

        ClienteDAOMock clienteDAO = new ClienteDAOMock();
        clienteDAO.agregar(new Cliente(1, "Ana Pérez", "1090001", "3101112233", "ana@correo.com", true));

        ServicioTipoDAOMock servicioTipoDAO = new ServicioTipoDAOMock();
        servicioTipoDAO.agregar(new ServicioTipo(1, "Cambio de aceite", 1, 1, new BigDecimal("60000.00"), 6));

        TecnicoDAOMock tecnicoDAO = new TecnicoDAOMock();
        tecnicoDAO.agregar(new Tecnico(1, "Pedro", "2001", 3, "3201112233", true));
        tecnicoDAO.agregar(new Tecnico(2, "Luis", "2002", 1, "3201112234", false));

        EspecialidadDAOMock especialidadDAO = new EspecialidadDAOMock();
        especialidadDAO.agregar(new Especialidad(1, "Mecánica general"));
        especialidadDAO.agregar(new Especialidad(3, "Frenos"));

        RepuestoDAOMock repuestoDAO = new RepuestoDAOMock();
        LoteDAOMock loteDAO = new LoteDAOMock();

        ordenServicioService = new OrdenServicioService(
                ordenDAO, detalleDAO, vehiculoDAO, clienteDAO, servicioTipoDAO,
                tecnicoDAO, repuestoDAO, false);
        inventarioService = new InventarioService(
                repuestoDAO, loteDAO, new ProveedorRepuestoDAOVacio(), new ProveedorDAOVacio());
        tecnicoService = new TecnicoService(tecnicoDAO, especialidadDAO);
        tallerFacade = new TallerFacade(ordenServicioService, inventarioService, tecnicoService);
        controller = new OrdenServicioController(ordenServicioService, tallerFacade);
    }

    @Test
    @DisplayName("TallerFacade valida que sus tres servicios colaboradores no sean nulos")
    void rechazaDependenciasNulasEnConstructor() {
        assertThrows(ValidacionException.class, () -> new TallerFacade(null, inventarioService, tecnicoService));
        assertThrows(ValidacionException.class, () -> new TallerFacade(ordenServicioService, null, tecnicoService));
        assertThrows(ValidacionException.class, () -> new TallerFacade(ordenServicioService, inventarioService, null));
    }

    @Test
    @DisplayName("crearOrdenIntegral crea la orden sin reservas y valida entradas inválidas")
    void crearOrdenIntegralYValidaciones() {
        LineaOrdenSolicitada linea = new LineaOrdenSolicitada(
                new OrdenServicioDetalle(1, "Revisión rápida"), Collections.emptyList());

        OrdenServicio creada = controller.crearOrdenIntegral(1, "Mantenimiento preventivo",
                Collections.singletonList(linea));

        assertNotNull(creada);
        assertEquals(1, creada.getId());
        assertEquals(1, controller.listarLineas(creada.getId()).size());

        assertThrows(ValidacionException.class, () -> tallerFacade.crearOrdenIntegral(1, "Problema", null));
        assertThrows(ValidacionException.class, () -> tallerFacade.crearOrdenIntegral(
                1, "Problema", Collections.emptyList()));
        assertThrows(ValidacionException.class, () -> tallerFacade.crearOrdenIntegral(
                1, "Problema", Collections.singletonList(
                        new LineaOrdenSolicitada(new OrdenServicioDetalle(1, null), null))));
    }

    @Test
    @DisplayName("Delega consultas de técnicos activos, técnico por ID y especialidad a través de la fachada")
    void delegaConsultasDeTecnicosYEspecialidades() {
        List<Tecnico> activos = controller.listarTecnicosActivos();
        assertEquals(1, activos.size());
        assertEquals("Pedro", activos.get(0).getNombre());

        Tecnico encontrado = controller.buscarTecnico(1);
        assertNotNull(encontrado);
        assertEquals("Pedro", encontrado.getNombre());

        assertEquals("Frenos", controller.obtenerNombreEspecialidad(3));
        assertEquals("Desconocida", controller.obtenerNombreEspecialidad(99));
    }

    @Test
    @DisplayName("RepuestoDAOImpl.buscarPorIdParaActualizar exige ID positivo y conexión transaccional compartida")
    void buscarPorIdParaActualizarExigeConexionCompartidaYIdPositivo() {
        RepuestoDAOImpl daoSinConexion = new RepuestoDAOImpl();
        assertThrows(ValidacionException.class, () -> daoSinConexion.buscarPorIdParaActualizar(0));
        assertThrows(ValidacionException.class, () -> daoSinConexion.buscarPorIdParaActualizar(-3));
        CarMotorsException ex = assertThrows(CarMotorsException.class,
                () -> daoSinConexion.buscarPorIdParaActualizar(1));
        assertTrue(ex.getMessage().contains("conexión transaccional"));
    }

    private static class OrdenServicioDAOMock implements OrdenServicioDAO {
        private final Map<Integer, OrdenServicio> ordenes = new HashMap<>();
        private int secuencia = 0;

        @Override
        public int guardar(OrdenServicio orden) {
            secuencia++;
            orden.setId(secuencia);
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
            return false;
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
        private int secuencia = 0;

        @Override
        public int guardar(OrdenServicioDetalle detalle) {
            secuencia++;
            detalle.setId(secuencia);
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
        }

        @Override
        public void asignarTecnico(int detalleId, int tecnicoId, BigDecimal horasTrabajadas) {
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
        }

        @Override
        public OrdenServicioDetalle buscarPorId(int id) {
            for (OrdenServicioDetalle d : detalles) {
                if (d.getId() == id) {
                    return d;
                }
            }
            return null;
        }
    }

    private static class VehiculoDAOMock implements VehiculoDAO {
        private final Map<Integer, Vehiculo> datos = new HashMap<>();

        void agregar(Vehiculo v) {
            datos.put(v.getId(), v);
        }

        @Override
        public int guardar(Vehiculo vehiculo) {
            return 0;
        }

        @Override
        public Vehiculo buscarPorId(int id) {
            return datos.get(id);
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
            return new ArrayList<>(datos.values());
        }
    }

    private static class ClienteDAOMock implements ClienteDAO {
        private final Map<Integer, Cliente> datos = new HashMap<>();

        void agregar(Cliente c) {
            datos.put(c.getId(), c);
        }

        @Override
        public List<Cliente> listarTodos() {
            return new ArrayList<>(datos.values());
        }

        @Override
        public List<Cliente> listarActivos() {
            return new ArrayList<>(datos.values());
        }

        @Override
        public Cliente buscarPorId(int id) {
            return datos.get(id);
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
        private final Map<Integer, ServicioTipo> datos = new HashMap<>();

        void agregar(ServicioTipo st) {
            datos.put(st.getId(), st);
        }

        @Override
        public List<ServicioTipo> listarTodos() {
            return new ArrayList<>(datos.values());
        }

        @Override
        public ServicioTipo buscarPorId(int id) {
            return datos.get(id);
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
        private final Map<Integer, Tecnico> datos = new HashMap<>();

        void agregar(Tecnico t) {
            datos.put(t.getId(), t);
        }

        @Override
        public List<Tecnico> listarTodos() {
            return new ArrayList<>(datos.values());
        }

        @Override
        public List<Tecnico> listarActivos() {
            List<Tecnico> activos = new ArrayList<>();
            for (Tecnico t : datos.values()) {
                if (t.isActivo()) {
                    activos.add(t);
                }
            }
            return activos;
        }

        @Override
        public Tecnico buscarPorId(int id) {
            return datos.get(id);
        }

        @Override
        public Tecnico buscarPorIdParaActualizar(int id) {
            return datos.get(id);
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

    private static class EspecialidadDAOMock implements EspecialidadDAO {
        private final Map<Integer, Especialidad> datos = new HashMap<>();

        void agregar(Especialidad e) {
            datos.put(e.getId(), e);
        }

        @Override
        public List<Especialidad> listarTodos() {
            return new ArrayList<>(datos.values());
        }

        @Override
        public Especialidad buscarPorId(int id) {
            return datos.get(id);
        }
    }

    private static class RepuestoDAOMock implements RepuestoDAO {
        @Override
        public List<Repuesto> listarTodos() {
            return new ArrayList<>();
        }

        @Override
        public List<Repuesto> listarActivos() {
            return new ArrayList<>();
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

    private static class LoteDAOMock implements LoteDAO {
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
            return 0;
        }

        @Override
        public int sumarStockOperativo(int repuestoId, LocalDate fechaReferencia) {
            return 0;
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
}
