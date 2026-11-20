package com.carmotors.sgt.service;

import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.exception.DatoDuplicadoException;
import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.exception.TransicionEstadoInvalidaException;
import com.carmotors.sgt.model.CargaLineaTecnico;
import com.carmotors.sgt.model.Cliente;
import com.carmotors.sgt.model.ConsultaOrdenServicio;
import com.carmotors.sgt.model.DetalleRepuesto;
import com.carmotors.sgt.model.FiltroOrdenServicio;
import com.carmotors.sgt.model.ResumenOrdenServicio;
import com.carmotors.sgt.model.DetalleTecnico;
import com.carmotors.sgt.model.OrdenServicio;
import com.carmotors.sgt.model.OrdenServicioDetalle;
import com.carmotors.sgt.model.Repuesto;
import com.carmotors.sgt.model.ServicioTipo;
import com.carmotors.sgt.model.Tecnico;
import com.carmotors.sgt.model.Vehiculo;
import com.carmotors.sgt.dao.ClienteDAO;
import com.carmotors.sgt.dao.ConsultaOrdenServicioDAO;
import com.carmotors.sgt.dao.DetalleRepuestoDAO;
import com.carmotors.sgt.dao.OrdenServicioDAO;
import com.carmotors.sgt.dao.OrdenServicioDetalleDAO;
import com.carmotors.sgt.dao.RepuestoDAO;
import com.carmotors.sgt.dao.ServicioTipoDAO;
import com.carmotors.sgt.dao.TecnicoDAO;
import com.carmotors.sgt.dao.VehiculoDAO;
import com.carmotors.sgt.service.chain.SugerenciaTecnico;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrdenServicioServiceTest {

    private OrdenServicioService service;
    private OrdenServicioDAOMock ordenDAO;
    private OrdenServicioDetalleDAOMock detalleDAO;
    private TecnicoDAOMock tecnicoDAO;
    private RepuestoDAOMock repuestoDAO;
    private OrdenServicioService servicioConsulta;
    private ConsultaOrdenDAOMock consultaDAO;
    private final Map<Integer, List<DetalleRepuesto>> piezasPorLinea = new HashMap<>();

    @BeforeEach
    void setUp() {
        ordenDAO = new OrdenServicioDAOMock();
        detalleDAO = new OrdenServicioDetalleDAOMock();
        tecnicoDAO = new TecnicoDAOMock();
        repuestoDAO = new RepuestoDAOMock();

        ClienteDAOMock clienteDAO = new ClienteDAOMock();
        clienteDAO.agregar(new Cliente(1, "María Rojas", "1094567801", "3105551101", "maria@correo.com", true));
        clienteDAO.agregar(new Cliente(7, "Pablo Ortiz", "1091234507", "3165551107", "pablo@correo.com", false));

        VehiculoDAOMock vehiculoDAO = new VehiculoDAOMock();
        vehiculoDAO.agregar(new Vehiculo(1, 1, "ABC123", "Chevrolet", "Spark GT"));
        vehiculoDAO.agregar(new Vehiculo(9, 7, "ZZZ999", "Mazda", "2"));

        ServicioTipoDAOMock servicioTipoDAO = new ServicioTipoDAOMock();
        servicioTipoDAO.agregar(new ServicioTipo(1, "Cambio de aceite y filtro", 1, 1, new BigDecimal("60000"), 6));
        servicioTipoDAO.agregar(new ServicioTipo(2, "Revisión y cambio de pastillas de freno", 2, 3,
                new BigDecimal("90000.005"), null));

        tecnicoDAO.agregar(new Tecnico(1, "Pedro Ramírez", "1090111222", 1, "3101112233", true));
        tecnicoDAO.agregar(new Tecnico(2, "Laura Gómez", "1090333444", 3, "3104445566", true));
        tecnicoDAO.agregar(new Tecnico(3, "Luis Ortega", "1090555666", 1, "3107778899", false));

        repuestoDAO.agregar(new Repuesto(1, 1, "Aceite sintético", "Mobil", new BigDecimal("38000.00"), 4, 2, true));
        repuestoDAO.agregar(new Repuesto(2, 1, "Filtro de aceite", "Bosch", new BigDecimal("25000.00"), 1, 2, true));

        service = new OrdenServicioService(
                ordenDAO, detalleDAO, vehiculoDAO, clienteDAO, servicioTipoDAO,
                tecnicoDAO, repuestoDAO, false);

        consultaDAO = new ConsultaOrdenDAOMock();
        servicioConsulta = new OrdenServicioService(ordenDAO, detalleDAO, vehiculoDAO, clienteDAO, servicioTipoDAO,
                tecnicoDAO, repuestoDAO, new DetalleRepuestoDAOMock(), consultaDAO);
    }

    private List<OrdenServicioDetalle> lineas(int... tipoIds) {
        List<OrdenServicioDetalle> lista = new ArrayList<>();
        for (int tipoId : tipoIds) {
            lista.add(new OrdenServicioDetalle(tipoId, null));
        }
        return lista;
    }

    @Test
    @DisplayName("Crea una orden Pendiente con una línea que copia nombre y mano de obra del tipo")
    void crearOrdenConUnaLinea() {
        List<OrdenServicioDetalle> solicitadas = new ArrayList<>();
        solicitadas.add(new OrdenServicioDetalle(1, "  Usar aceite sintético  "));
        LocalDateTime antes = LocalDateTime.now().withNano(0);

        OrdenServicio orden = service.crearOrden(1, "  Cambio de aceite por kilometraje  ", solicitadas);

        assertEquals(1, orden.getId());
        assertEquals(1, orden.getVehiculoId());
        assertEquals(new EstadoPendiente().getEstadoId(), orden.getEstadoId());
        assertEquals("Cambio de aceite por kilometraje", orden.getDescripcionProblema());
        assertNotNull(orden.getFechaRecepcion());
        assertFalse(orden.getFechaRecepcion().isBefore(antes));
        assertFalse(orden.getFechaRecepcion().isAfter(LocalDateTime.now()));
        assertNull(orden.getFechaCompletado());
        assertNull(orden.getFechaEntrega());
        assertNull(orden.getFechaCancelacion());

        List<OrdenServicioDetalle> guardadas = service.listarLineas(orden.getId());
        assertEquals(1, guardadas.size());
        OrdenServicioDetalle linea = guardadas.get(0);
        assertEquals(1, linea.getOrdenId());
        assertEquals(1, linea.getServicioTipoId());
        assertEquals("Cambio de aceite y filtro", linea.getNombreServicioSnapshot());
        assertEquals(new BigDecimal("60000.00"), linea.getCostoManoObra());
        assertEquals("Usar aceite sintético", linea.getObservaciones());
        assertEquals("Pendiente", service.obtenerNombreEstado(orden.getEstadoId()));
    }

    @Test
    @DisplayName("Crea una orden con varias líneas, todas asociadas al ID generado")
    void crearOrdenConVariasLineas() {
        OrdenServicio orden = service.crearOrden(1, "Mantenimiento general", lineas(1, 2, 1));

        List<OrdenServicioDetalle> guardadas = service.listarLineas(orden.getId());
        assertEquals(3, guardadas.size());
        assertEquals("Cambio de aceite y filtro", guardadas.get(0).getNombreServicioSnapshot());
        assertEquals("Revisión y cambio de pastillas de freno", guardadas.get(1).getNombreServicioSnapshot());
        assertEquals(new BigDecimal("90000.01"), guardadas.get(1).getCostoManoObra());
        assertEquals(new BigDecimal("60000.00"), guardadas.get(2).getCostoManoObra());
        for (OrdenServicioDetalle linea : guardadas) {
            assertEquals(orden.getId(), linea.getOrdenId());
            assertTrue(linea.getId() > 0);
            assertNull(linea.getObservaciones());
        }
        assertEquals(1, ordenDAO.ordenes.size());
    }

    @Test
    @DisplayName("Rechaza un vehículo inexistente sin guardar nada")
    void rechazarVehiculoInexistente() {
        assertThrows(EntidadNoEncontradaException.class,
                () -> service.crearOrden(99, "Ruido en el motor", lineas(1)));
        assertNadaGuardado();
    }

    @Test
    @DisplayName("Rechaza un vehículo cuyo cliente dueño está inactivo")
    void rechazarClienteInactivo() {
        assertThrows(ValidacionException.class,
                () -> service.crearOrden(9, "Ruido en el motor", lineas(1)));
        assertNadaGuardado();
    }

    @Test
    @DisplayName("Rechaza una descripción vacía o en blanco")
    void rechazarDescripcionVacia() {
        assertThrows(ValidacionException.class, () -> service.crearOrden(1, "   ", lineas(1)));
        assertThrows(ValidacionException.class, () -> service.crearOrden(1, null, lineas(1)));
        assertNadaGuardado();
    }

    @Test
    @DisplayName("Rechaza una descripción de más de 255 caracteres y acepta exactamente 255")
    void rechazarDescripcionDemasiadoLarga() {
        String larga = "a".repeat(256);
        assertThrows(ValidacionException.class, () -> service.crearOrden(1, larga, lineas(1)));
        assertNadaGuardado();

        OrdenServicio orden = service.crearOrden(1, "b".repeat(255), lineas(1));
        assertEquals(255, orden.getDescripcionProblema().length());
    }

    @Test
    @DisplayName("Rechaza una orden sin líneas de servicio (RN-18)")
    void rechazarOrdenSinLineas() {
        assertThrows(ValidacionException.class, () -> service.crearOrden(1, "Revisión", new ArrayList<>()));
        assertThrows(ValidacionException.class, () -> service.crearOrden(1, "Revisión", null));
        assertNadaGuardado();
    }

    @Test
    @DisplayName("Rechaza una línea con un tipo de servicio inexistente sin guardar la cabecera")
    void rechazarTipoServicioInexistente() {
        assertThrows(EntidadNoEncontradaException.class,
                () -> service.crearOrden(1, "Revisión", lineas(1, 42)));
        assertNadaGuardado();
    }

    @Test
    @DisplayName("T4.3: Agrega línea a orden Pendiente congelando snapshot y costo de mano de obra")
    void agregarLineaEnOrdenPendiente() {
        OrdenServicio orden = service.crearOrden(1, "Revisión general", lineas(1));

        int nuevoDetalleId = service.agregarLinea(
                orden.getId(), new OrdenServicioDetalle(2, "  Revisar discos  "), null, false);

        assertTrue(nuevoDetalleId > 0);
        List<OrdenServicioDetalle> guardadas = service.listarLineas(orden.getId());
        assertEquals(2, guardadas.size());
        OrdenServicioDetalle nueva = guardadas.get(1);
        assertEquals("Revisión y cambio de pastillas de freno", nueva.getNombreServicioSnapshot());
        assertEquals(new BigDecimal("90000.01"), nueva.getCostoManoObra());
        assertEquals("Revisar discos", nueva.getObservaciones());
        assertFalse(service.requiereTecnicoParaAgregar(orden.getId()));
    }

    @Test
    @DisplayName("T4.3: En Proceso exige técnico activo, asigna en la misma operación y respeta RN-19")
    void agregarLineaEnOrdenEnProcesoConValidacionesTecnicoYEspecialidad() {
        OrdenServicio orden = service.crearOrden(1, "Falla frenos", lineas(1));
        orden.setEstadoId(new EstadoEnProceso().getEstadoId());
        assertTrue(service.requiereTecnicoParaAgregar(orden.getId()));

        // Sin técnico -> rechaza
        assertThrows(ValidacionException.class,
                () -> service.agregarLinea(orden.getId(), new OrdenServicioDetalle(1, null), null, false));

        // Técnico inactivo (ID 3) -> rechaza
        assertThrows(ValidacionException.class,
                () -> service.agregarLinea(orden.getId(), new OrdenServicioDetalle(1, null), 3, false));

        // Especialidad distinta sin confirmar (Servicio 2 requiere esp. 3, Técnico 1 es esp. 1) -> rechaza
        assertThrows(ValidacionException.class,
                () -> service.agregarLinea(orden.getId(), new OrdenServicioDetalle(2, null), 1, false));

        // Especialidad distinta confirmada -> permite y asigna
        int detalleConfirmado = service.agregarLinea(
                orden.getId(), new OrdenServicioDetalle(2, null), 1, true);
        assertEquals(Integer.valueOf(1), detalleDAO.tecnicosAsignados.get(detalleConfirmado));

        // Especialidad coincidente (Servicio 2 esp. 3, Técnico 2 esp. 3) -> permite sin confirmación extra
        int detalleEspecialista = service.agregarLinea(
                orden.getId(), new OrdenServicioDetalle(2, null), 2, false);
        assertEquals(Integer.valueOf(2), detalleDAO.tecnicosAsignados.get(detalleEspecialista));
    }

    @Test
    @DisplayName("T4.3: Bloquea agregar o retirar líneas en estados Completado, Facturado, Entregado y Cancelada")
    void rechazarEdicionEnEstadosNoEditables() {
        int[] estadosBloqueados = new int[] {
                new EstadoCompletado().getEstadoId(),
                new EstadoFacturado().getEstadoId(),
                new EstadoEntregado().getEstadoId(),
                new EstadoCancelada().getEstadoId()
        };

        for (int estadoId : estadosBloqueados) {
            OrdenServicio orden = service.crearOrden(1, "Orden bloqueada", lineas(1, 2));
            int detalleId = service.listarLineas(orden.getId()).get(0).getId();
            orden.setEstadoId(estadoId);

            assertThrows(ValidacionException.class,
                    () -> service.requiereTecnicoParaAgregar(orden.getId()));
            assertThrows(ValidacionException.class,
                    () -> service.agregarLinea(orden.getId(), new OrdenServicioDetalle(1, null), 1, true));
            assertThrows(ValidacionException.class,
                    () -> service.quitarLinea(orden.getId(), detalleId));
        }
    }

    @Test
    @DisplayName("T4.3: Retirar línea libera exactamente sus reservas en repuestos y protege última línea (RN-05, RN-18)")
    void quitarLineaLiberaReservasYProtegeUltimaLinea() {
        OrdenServicio orden = service.crearOrden(1, "Cambio aceite y alineación", lineas(1, 2));
        List<OrdenServicioDetalle> guardadas = service.listarLineas(orden.getId());
        int detalleConReservas = guardadas.get(0).getId();
        int detalleSinReservas = guardadas.get(1).getId();

        detalleDAO.agregarReserva(detalleConReservas, 1, 4);
        detalleDAO.agregarReserva(detalleConReservas, 2, 1);

        service.quitarLinea(orden.getId(), detalleConReservas);

        assertEquals(0, repuestoDAO.buscarPorId(1).getCantidadReservada());
        assertEquals(0, repuestoDAO.buscarPorId(2).getCantidadReservada());
        assertEquals(1, service.listarLineas(orden.getId()).size());

        // No permite retirar la última línea restante
        assertThrows(ValidacionException.class,
                () -> service.quitarLinea(orden.getId(), detalleSinReservas));
    }

    @Test
    @DisplayName("T4.3: En Proceso rechaza retirar línea con horas trabajadas > 0 y permite si tiene 0.00h (RF-35)")
    void quitarLineaEnProcesoValidaHorasTrabajadas() {
        OrdenServicio orden = service.crearOrden(1, "Trabajo en progreso", lineas(1, 2, 1));
        List<OrdenServicioDetalle> guardadas = service.listarLineas(orden.getId());
        int detalleConHoras = guardadas.get(0).getId();
        int detalleSinHoras = guardadas.get(1).getId();

        detalleDAO.fijarHoras(detalleConHoras, new BigDecimal("1.50"));
        detalleDAO.fijarHoras(detalleSinHoras, BigDecimal.ZERO);
        orden.setEstadoId(new EstadoEnProceso().getEstadoId());

        assertThrows(ValidacionException.class,
                () -> service.quitarLinea(orden.getId(), detalleConHoras));
        assertEquals(3, service.listarLineas(orden.getId()).size());

        service.quitarLinea(orden.getId(), detalleSinHoras);
        assertEquals(2, service.listarLineas(orden.getId()).size());
    }

    @Test
    @DisplayName("T4.3: especialidadCoincide maneja nulos de forma segura")
    void especialidadCoincideConNulos() {
        ServicioTipo tipo = new ServicioTipo(1, "Aceite", 1, 2, new BigDecimal("50000"), 6);
        Tecnico tecnicoIgual = new Tecnico(1, "Ana", "100", 2, null, true);
        Tecnico tecnicoDistinto = new Tecnico(2, "Luis", "200", 3, null, true);

        assertFalse(service.especialidadCoincide(null, tecnicoIgual));
        assertFalse(service.especialidadCoincide(tipo, null));
        assertFalse(service.especialidadCoincide(tipo, tecnicoDistinto));
        assertTrue(service.especialidadCoincide(tipo, tecnicoIgual));
    }

    @Test
    @DisplayName("T4.6: Sugerencia aplica RN-20/RN-21 y excluye técnicos ya asignados a la línea")
    void sugerirTecnicoParaLineaRespetaCadenaYExcluyeYaAsignados() {
        // Servicio 1 requiere especialidad 1. En setUp hay:
        // Técnico 1 (esp 1, activo), Técnico 2 (esp 3, activo), Técnico 3 (esp 1, inactivo).
        tecnicoDAO.agregar(new Tecnico(4, "Ana Gómez", "1090888999", 1, "3109998877", true));
        OrdenServicio orden = service.crearOrden(1, "Motor", lineas(1));
        int detalleId = service.listarLineas(orden.getId()).get(0).getId();

        // Dos especialistas libres (IDs 1 y 4): desempata por menor ID -> técnico 1
        SugerenciaTecnico primera = service.sugerirTecnicoParaLinea(orden.getId(), detalleId);
        assertNotNull(primera);
        assertEquals(1, primera.getTecnico().getId());
        assertEquals(0, primera.getCargaActual());
        assertTrue(primera.isEspecialidadCoincide());
        assertEquals("Especialista libre", primera.getCriterioAplicado());

        // Ponemos carga 3 al técnico 1 y carga 1 al técnico 4 -> sugiere especialista menor carga (4)
        tecnicoDAO.fijarCarga(1, 3);
        tecnicoDAO.fijarCarga(4, 1);
        SugerenciaTecnico segunda = service.sugerirTecnicoParaLinea(orden.getId(), detalleId);
        assertNotNull(segunda);
        assertEquals(4, segunda.getTecnico().getId());
        assertEquals(1, segunda.getCargaActual());
        assertEquals("Especialista con menor carga", segunda.getCriterioAplicado());

        // Asignamos los dos especialistas activos (1 y 4) a la línea -> sugiere respaldo general (2)
        service.asignarTecnicoALinea(orden.getId(), detalleId, 1, BigDecimal.ZERO, false);
        service.asignarTecnicoALinea(orden.getId(), detalleId, 4, BigDecimal.ZERO, false);
        tecnicoDAO.fijarCarga(2, 2);

        SugerenciaTecnico respaldo = service.sugerirTecnicoParaLinea(orden.getId(), detalleId);
        assertNotNull(respaldo);
        assertEquals(2, respaldo.getTecnico().getId());
        assertEquals(2, respaldo.getCargaActual());
        assertFalse(respaldo.isEspecialidadCoincide());
        assertEquals("Técnico de otra especialidad con menor carga", respaldo.getCriterioAplicado());

        // Asignamos también al técnico 2 -> sin candidatos activos restantes, devuelve null
        service.asignarTecnicoALinea(orden.getId(), detalleId, 2, BigDecimal.ZERO, true);
        assertNull(service.sugerirTecnicoParaLinea(orden.getId(), detalleId));
    }

    @Test
    @DisplayName("T4.6: Asigna múltiples técnicos a una línea y normaliza horas (null -> 0.00, HALF_UP)")
    void asignarVariosTecnicosALineaYNormalizarHoras() {
        OrdenServicio orden = service.crearOrden(1, "Frenos", lineas(2));
        int detalleId = service.listarLineas(orden.getId()).get(0).getId();

        // Técnico 2 coincide con especialidad 3 del servicio 2; horas null -> 0.00
        DetalleTecnico primero = service.asignarTecnicoALinea(orden.getId(), detalleId, 2, null, false);
        assertEquals(detalleId, primero.getDetalleId());
        assertEquals(2, primero.getTecnicoId());
        assertEquals(new BigDecimal("0.00"), primero.getHorasTrabajadas());

        // Orden pasa a En Proceso y se asigna segundo técnico (ID 1, otra especialidad confirmada) con 1.235h
        orden.setEstadoId(new EstadoEnProceso().getEstadoId());
        DetalleTecnico segundo = service.asignarTecnicoALinea(
                orden.getId(), detalleId, 1, new BigDecimal("1.235"), true);
        assertEquals(new BigDecimal("1.24"), segundo.getHorasTrabajadas());

        List<DetalleTecnico> asignados = service.listarTecnicosDeLinea(orden.getId(), detalleId);
        assertEquals(2, asignados.size());
        assertEquals(1, asignados.get(0).getTecnicoId());
        assertEquals(new BigDecimal("1.24"), asignados.get(0).getHorasTrabajadas());
        assertEquals(2, asignados.get(1).getTecnicoId());
        assertEquals(new BigDecimal("0.00"), asignados.get(1).getHorasTrabajadas());
    }

    @Test
    @DisplayName("T4.6: Rechaza técnico inexistente, inactivo, duplicado y horas negativas o fuera de rango")
    void rechazarTecnicoInexistenteInactivoDuplicadoYHorasInvalidas() {
        OrdenServicio orden = service.crearOrden(1, "Diagnóstico", lineas(1));
        int detalleId = service.listarLineas(orden.getId()).get(0).getId();

        assertThrows(EntidadNoEncontradaException.class,
                () -> service.asignarTecnicoALinea(orden.getId(), detalleId, 999, BigDecimal.ZERO, false));
        assertThrows(ValidacionException.class,
                () -> service.asignarTecnicoALinea(orden.getId(), detalleId, 3, BigDecimal.ZERO, true));
        assertThrows(ValidacionException.class,
                () -> service.asignarTecnicoALinea(orden.getId(), detalleId, 1, new BigDecimal("-0.50"), false));
        assertThrows(ValidacionException.class,
                () -> service.asignarTecnicoALinea(orden.getId(), detalleId, 1, new BigDecimal("-0.001"), false));
        assertThrows(ValidacionException.class,
                () -> service.asignarTecnicoALinea(orden.getId(), detalleId, 1, new BigDecimal("1000.00"), false));

        service.asignarTecnicoALinea(orden.getId(), detalleId, 1, new BigDecimal("1.00"), false);
        assertThrows(DatoDuplicadoException.class,
                () -> service.asignarTecnicoALinea(orden.getId(), detalleId, 1, BigDecimal.ZERO, false));
        assertThrows(DatoDuplicadoException.class,
                () -> service.requiereConfirmacionEspecialidad(orden.getId(), detalleId, 1));
    }

    @Test
    @DisplayName("T4.6: Exige confirmación explícita si la especialidad no coincide (RN-19)")
    void asignarTecnicoOtraEspecialidadExigeConfirmacion() {
        OrdenServicio orden = service.crearOrden(1, "Frenos", lineas(2));
        int detalleId = service.listarLineas(orden.getId()).get(0).getId();

        assertFalse(service.requiereConfirmacionEspecialidad(orden.getId(), detalleId, 2));
        assertTrue(service.requiereConfirmacionEspecialidad(orden.getId(), detalleId, 1));

        assertThrows(ValidacionException.class,
                () -> service.asignarTecnicoALinea(orden.getId(), detalleId, 1, new BigDecimal("2.00"), false));

        DetalleTecnico confirmada = service.asignarTecnicoALinea(
                orden.getId(), detalleId, 1, new BigDecimal("2.00"), true);
        assertEquals(new BigDecimal("2.00"), confirmada.getHorasTrabajadas());
    }

    @Test
    @DisplayName("T4.6: Registra y actualiza horas trabajadas validando asignación y rango")
    void registrarHorasTrabajadasActualizaYValida() {
        OrdenServicio orden = service.crearOrden(1, "Aceite", lineas(1));
        int detalleId = service.listarLineas(orden.getId()).get(0).getId();
        service.asignarTecnicoALinea(orden.getId(), detalleId, 1, BigDecimal.ZERO, false);

        DetalleTecnico actualizadas = service.registrarHorasTrabajadas(
                orden.getId(), detalleId, 1, new BigDecimal("3.755"));
        assertEquals(new BigDecimal("3.76"), actualizadas.getHorasTrabajadas());
        assertTrue(detalleDAO.tieneHorasTrabajadas(detalleId));

        assertThrows(ValidacionException.class,
                () -> service.registrarHorasTrabajadas(orden.getId(), detalleId, 1, null));
        assertThrows(ValidacionException.class,
                () -> service.registrarHorasTrabajadas(orden.getId(), detalleId, 1, new BigDecimal("-1.00")));
        assertThrows(ValidacionException.class,
                () -> service.registrarHorasTrabajadas(orden.getId(), detalleId, 1, new BigDecimal("999.995")));
        assertThrows(EntidadNoEncontradaException.class,
                () -> service.registrarHorasTrabajadas(orden.getId(), detalleId, 2, new BigDecimal("1.00")));
    }

    @Test
    @DisplayName("T4.6: Bloquea sugerencia, asignación y horas en Completado, Facturado, Entregado y Cancelada")
    void rechazarAsignacionYHorasEnEstadosBloqueados() {
        int[] estadosBloqueados = new int[] {
                new EstadoCompletado().getEstadoId(),
                new EstadoFacturado().getEstadoId(),
                new EstadoEntregado().getEstadoId(),
                new EstadoCancelada().getEstadoId()
        };

        for (int estadoId : estadosBloqueados) {
            OrdenServicio orden = service.crearOrden(1, "Orden bloqueada", lineas(1));
            int detalleId = service.listarLineas(orden.getId()).get(0).getId();
            service.asignarTecnicoALinea(orden.getId(), detalleId, 1, BigDecimal.ONE, false);
            orden.setEstadoId(estadoId);

            assertThrows(ValidacionException.class,
                    () -> service.validarOrdenEditable(orden.getId()));
            assertThrows(ValidacionException.class,
                    () -> service.sugerirTecnicoParaLinea(orden.getId(), detalleId));
            assertThrows(ValidacionException.class,
                    () -> service.requiereConfirmacionEspecialidad(orden.getId(), detalleId, 2));
            assertThrows(ValidacionException.class,
                    () -> service.asignarTecnicoALinea(orden.getId(), detalleId, 2, BigDecimal.ZERO, true));
            assertThrows(ValidacionException.class,
                    () -> service.registrarHorasTrabajadas(orden.getId(), detalleId, 1, BigDecimal.TEN));
            assertThrows(ValidacionException.class,
                    () -> service.actualizarCostoManoObra(orden.getId(), detalleId, new BigDecimal("50000")));
            assertThrows(ValidacionException.class,
                    () -> service.sugerirTecnicoParaTipoServicio(orden.getId(), 1));
        }
    }

    @Test
    @DisplayName("RN-08: Ajusta el costo de mano de obra en órdenes editables y rechaza valores inválidos")
    void actualizarCostoManoObraPermiteAjusteEnEstadosEditablesYRechazaInvalidos() {
        OrdenServicio orden = service.crearOrden(1, "Ajuste de mano de obra", lineas(1));
        int detalleId = service.listarLineas(orden.getId()).get(0).getId();

        OrdenServicioDetalle enCero = service.actualizarCostoManoObra(orden.getId(), detalleId, BigDecimal.ZERO);
        assertEquals(new BigDecimal("0.00"), enCero.getCostoManoObra());

        orden.setEstadoId(new EstadoEnProceso().getEstadoId());
        OrdenServicioDetalle ajustada = service.actualizarCostoManoObra(
                orden.getId(), detalleId, new BigDecimal("125000.555"));
        assertEquals(new BigDecimal("125000.56"), ajustada.getCostoManoObra());
        assertEquals(new BigDecimal("125000.56"), detalleDAO.buscarPorId(detalleId).getCostoManoObra());

        assertThrows(ValidacionException.class,
                () -> service.actualizarCostoManoObra(orden.getId(), detalleId, null));
        assertThrows(ValidacionException.class,
                () -> service.actualizarCostoManoObra(orden.getId(), detalleId, new BigDecimal("-1.00")));
        assertThrows(ValidacionException.class,
                () -> service.actualizarCostoManoObra(orden.getId(), detalleId, new BigDecimal("10000000000.00")));
    }

    @Test
    @DisplayName("RF-09: Sugiere técnico antes de persistir nueva línea en una orden En Proceso")
    void sugerirTecnicoParaTipoServicioAplicaCadenaAntesDeCrearLinea() {
        OrdenServicio orden = service.crearOrden(1, "Agregar línea en proceso", lineas(1));
        orden.setEstadoId(new EstadoEnProceso().getEstadoId());
        tecnicoDAO.agregar(new Tecnico(4, "Carlos Mendoza", "1090666777", 1, "3109998877", true));

        // Tipo 1 requiere especialidad 1; técnicos activos 1 y 4 tienen especialidad 1 (menor ID: 1)
        SugerenciaTecnico sugerencia = service.sugerirTecnicoParaTipoServicio(orden.getId(), 1);
        assertNotNull(sugerencia);
        assertEquals(1, sugerencia.getTecnico().getId());
        assertEquals("Especialista libre", sugerencia.getCriterioAplicado());

        tecnicoDAO.fijarCarga(1, 3);
        tecnicoDAO.fijarCarga(4, 1);
        SugerenciaTecnico menorCarga = service.sugerirTecnicoParaTipoServicio(orden.getId(), 1);
        assertNotNull(menorCarga);
        assertEquals(4, menorCarga.getTecnico().getId());
        assertEquals("Especialista con menor carga", menorCarga.getCriterioAplicado());
    }

    @Test
    void combinaFiltrosDeCabeceraConTecnicoYTipoDeServicio() {
        LocalDate fecha = LocalDate.of(2026, 11, 11);
        ResumenOrdenServicio coincide = new ResumenOrdenServicio(2, 2, "ABC123", fecha.atTime(23, 59));
        coincide.getTecnicos().put(1, "Pedro");
        coincide.getTiposServicio().add(2);

        ResumenOrdenServicio otroTecnico = new ResumenOrdenServicio(3, 2, "ABC123", fecha.atTime(12, 0));
        otroTecnico.getTecnicos().put(3, "Luis");
        otroTecnico.getTiposServicio().add(2);

        ResumenOrdenServicio otroTipo = new ResumenOrdenServicio(4, 2, "ABC123", fecha.atTime(12, 0));
        otroTipo.getTecnicos().put(1, "Pedro");
        otroTipo.getTiposServicio().add(1);
        consultaDAO.resultados = List.of(coincide, otroTecnico, otroTipo);

        List<ResumenOrdenServicio> resultados = servicioConsulta.consultarOrdenes(
                new FiltroOrdenServicio(2, 1, " abc-123 ", 2, fecha, fecha));
        assertEquals(List.of(coincide), resultados);
        assertEquals(2, consultaDAO.filtroRecibido.getEstadoId());
        assertEquals("ABC123", consultaDAO.filtroRecibido.getPlaca());
        assertEquals(fecha, consultaDAO.filtroRecibido.getDesde());
        assertEquals(fecha, consultaDAO.filtroRecibido.getHasta());
    }

    @Test
    void rechazaRangoInvertidoAntesDeConsultar() {
        LocalDate hoy = LocalDate.of(2026, 11, 11);
        assertThrows(ValidacionException.class, () -> servicioConsulta.consultarOrdenes(
                new FiltroOrdenServicio(null, null, null, null, hoy, hoy.minusDays(1))));
        assertNull(consultaDAO.filtroRecibido);
    }

    @Test
    void rechazaPlacaInvalidaAntesDeConsultar() {
        assertThrows(ValidacionException.class, () -> servicioConsulta.consultarOrdenes(
                new FiltroOrdenServicio(null, null, "A!", null, null, null)));
        assertNull(consultaDAO.filtroRecibido);
    }

    @Test
    void rechazaFiltroNulo() {
        assertThrows(ValidacionException.class, () -> servicioConsulta.consultarOrdenes(null));
        assertNull(consultaDAO.filtroRecibido);
    }

    @Test
    void retornaListaVaciaSiNingunaOrdenCumpleElFiltro() {
        consultaDAO.resultados = List.of(new ResumenOrdenServicio(2, 2, "ABC123", LocalDateTime.now()));
        assertTrue(servicioConsulta.consultarOrdenes(
                new FiltroOrdenServicio(null, 99, null, null, null, null)).isEmpty());
    }

    @Test
    void detalleIncluyeHorasYSubtotalConPreciosHistoricos() {
        OrdenServicio orden = service.crearOrden(1, "Prueba", lineas(1));
        int lineaId = detalleDAO.listarPorOrden(orden.getId()).get(0).getId();
        detalleDAO.asignarTecnico(lineaId, 3, new BigDecimal("1.25"));
        piezasPorLinea.put(lineaId, List.of(
                new DetalleRepuesto(lineaId, 1, 4, "Aceite guardado", new BigDecimal("38000.00")),
                new DetalleRepuesto(lineaId, 2, 1, "Filtro guardado", new BigDecimal("25000.00"))));

        ConsultaOrdenServicio resultado = servicioConsulta.consultarDetalle(orden.getId());
        ConsultaOrdenServicio.Linea linea = resultado.getLineas().get(0);
        assertEquals(new BigDecimal("237000.00"), linea.getSubtotal());
        assertEquals("Aceite guardado", linea.getRepuestos().get(0).getNombreRepuestoSnapshot());
        assertEquals(3, linea.getHorasPorTecnico().get(0).getTecnico().getId());
        assertEquals(new BigDecimal("1.25"), linea.getHorasPorTecnico().get(0).getHorasTrabajadas());
    }

    @Test
    void clasificaRepuestosSegunLosSeisEstados() {
        OrdenServicio orden = service.crearOrden(1, "Prueba de estados", lineas(1));
        String[] etiquetas = {"Reservados", "Reservados", "Consumidos", "Consumidos", "Consumidos", "Liberados"};
        for (int i = 0; i < etiquetas.length; i++) {
            orden.setEstadoId(i + 1);
            assertEquals(etiquetas[i], servicioConsulta.consultarDetalle(orden.getId()).getEstadoRepuestos());
        }
    }

    @Test
    void usaSnapshotFacturadoSinModificarElClienteNiElVehiculoActual() {
        OrdenServicio orden = service.crearOrden(1, "Prueba de factura", lineas(1));
        orden.setEstadoId(new EstadoFacturado().getEstadoId());
        consultaDAO.ordenFacturada = orden.getId();
        ConsultaOrdenServicio historica = servicioConsulta.consultarDetalle(orden.getId());
        assertEquals("Cliente histórico", historica.getCliente().getNombre());
        assertEquals("HIS123", historica.getVehiculo().getPlaca());

        OrdenServicio pendiente = service.crearOrden(1, "Otra orden", lineas(1));
        ConsultaOrdenServicio actual = servicioConsulta.consultarDetalle(pendiente.getId());
        assertEquals("María Rojas", actual.getCliente().getNombre());
        assertEquals("ABC123", actual.getVehiculo().getPlaca());
    }

    @Test
    void informaCuandoLaOrdenNoExiste() {
        assertThrows(EntidadNoEncontradaException.class, () -> servicioConsulta.consultarDetalle(999));
    }

    private static class ConsultaOrdenDAOMock implements ConsultaOrdenServicioDAO {
        private List<ResumenOrdenServicio> resultados = List.of();
        private FiltroOrdenServicio filtroRecibido;
        private int ordenFacturada;

        @Override
        public List<ResumenOrdenServicio> listar(FiltroOrdenServicio filtro) {
            filtroRecibido = filtro;
            return resultados;
        }

        @Override
        public List<ResumenOrdenServicio> listarPorCliente(int clienteId) {
            return resultados;
        }

        @Override
        public void cargarDatosFacturados(int ordenId, Cliente cliente, Vehiculo vehiculo) {
            if (ordenId == ordenFacturada) {
                cliente.setNombre("Cliente histórico");
                vehiculo.setPlaca("HIS123");
            }
        }
    }

    // ------------------------------------------------------------------
    // T4.9: transición de Pendiente a En Proceso (RF-12, RN-06, RN-07, HU-05)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Pasa a En Proceso una orden cuyas líneas tienen técnico activo (RF-12, RN-07)")
    void pasarAEnProcesoConTecnicosActivos() {
        OrdenServicio orden = service.crearOrden(1, "Ruido en el tren delantero", lineas(1, 2));
        for (OrdenServicioDetalle linea : detalleDAO.listarPorOrden(orden.getId())) {
            detalleDAO.asignarTecnico(linea.getId(), 2);
        }
        assertEquals(1, orden.getEstadoId());

        OrdenServicio actualizada = service.pasarAEnProceso(orden.getId());

        assertEquals(2, actualizada.getEstadoId());
        assertEquals(2, ordenDAO.buscarPorId(orden.getId()).getEstadoId());
    }

    @Test
    @DisplayName("Bloquea la transición si una línea no tiene técnico e identifica la línea (HU-05)")
    void bloquearTransicionSinTecnico() {
        OrdenServicio orden = service.crearOrden(1, "Frenos y aceite", lineas(1, 2));
        List<OrdenServicioDetalle> creadas = detalleDAO.listarPorOrden(orden.getId());
        OrdenServicioDetalle primera = creadas.get(0);
        OrdenServicioDetalle segunda = creadas.get(1);
        detalleDAO.asignarTecnico(primera.getId(), 1);

        ValidacionException error = assertThrows(ValidacionException.class,
                () -> service.pasarAEnProceso(orden.getId()));

        assertTrue(error.getMessage().contains(String.valueOf(segunda.getId())), error.getMessage());
        assertTrue(error.getMessage().contains(segunda.getNombreServicioSnapshot()), error.getMessage());
        assertEquals(1, ordenDAO.buscarPorId(orden.getId()).getEstadoId());
    }

    @Test
    @DisplayName("Bloquea la transición si el único técnico de una línea está inactivo (RN-07)")
    void bloquearTransicionConTecnicoInactivo() {
        OrdenServicio orden = service.crearOrden(1, "Cambio de aceite", lineas(1));
        OrdenServicioDetalle linea = detalleDAO.listarPorOrden(orden.getId()).get(0);
        detalleDAO.asignarTecnico(linea.getId(), 3);

        ValidacionException error = assertThrows(ValidacionException.class,
                () -> service.pasarAEnProceso(orden.getId()));

        assertTrue(error.getMessage().contains(String.valueOf(linea.getId())), error.getMessage());
        assertEquals(1, ordenDAO.buscarPorId(orden.getId()).getEstadoId());
    }

    @Test
    @DisplayName("Rechaza la transición cuando la orden no está en Pendiente (RN-06)")
    void rechazarTransicionDesdeEstadoNoPendiente() {
        OrdenServicio orden = service.crearOrden(1, "Diagnóstico eléctrico", lineas(1));
        OrdenServicioDetalle linea = detalleDAO.listarPorOrden(orden.getId()).get(0);
        detalleDAO.asignarTecnico(linea.getId(), 1);
        orden.setEstadoId(3);

        assertThrows(TransicionEstadoInvalidaException.class,
                () -> service.pasarAEnProceso(orden.getId()));
        assertEquals(3, ordenDAO.buscarPorId(orden.getId()).getEstadoId());
    }

    @Test
    @DisplayName("Rechaza una orden inexistente o un ID no positivo al pasar a En Proceso")
    void rechazarOrdenInexistenteOIdInvalido() {
        assertThrows(EntidadNoEncontradaException.class, () -> service.pasarAEnProceso(999));
        assertThrows(ValidacionException.class, () -> service.pasarAEnProceso(0));
        assertThrows(ValidacionException.class, () -> service.pasarAEnProceso(-5));
    }

    @Test
    @DisplayName("Si el DAO no actualiza el estado se lanza CarMotorsException y la orden no cambia")
    void fallarActualizacionDeEstado() {
        OrdenServicio orden = service.crearOrden(1, "Alineación", lineas(1));
        OrdenServicioDetalle linea = detalleDAO.listarPorOrden(orden.getId()).get(0);
        detalleDAO.asignarTecnico(linea.getId(), 1);
        ordenDAO.deshabilitarActualizarEstado();

        assertThrows(CarMotorsException.class, () -> service.pasarAEnProceso(orden.getId()));
        assertEquals(1, ordenDAO.buscarPorId(orden.getId()).getEstadoId());
    }

    private class DetalleRepuestoDAOMock implements DetalleRepuestoDAO {
        @Override
        public boolean guardar(DetalleRepuesto detalleRepuesto) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DetalleRepuesto buscar(int detalleId, int repuestoId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<DetalleRepuesto> listarPorDetalle(int detalleId) {
            return piezasPorLinea.getOrDefault(detalleId, List.of());
        }

        @Override
        public boolean eliminar(int detalleId, int repuestoId) {
            throw new UnsupportedOperationException();
        }
    }

    private void assertNadaGuardado() {
        assertTrue(ordenDAO.ordenes.isEmpty());
        assertTrue(detalleDAO.detalles.isEmpty());
    }

    // ------------------------------------------------------------------
    // DAOs en memoria
    // ------------------------------------------------------------------

    // ------------------------------------------------------------------
    // T6.4: entrega de la orden (RF-12, RN-10)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Entrega una orden Facturada registrando el estado Entregado y la fecha (RN-10)")
    void entregarOrdenFacturada() {
        OrdenServicio orden = service.crearOrden(1, "Servicio listo para entrega", lineas(1));
        orden.setEstadoId(4);

        OrdenServicio entregada = service.entregarOrden(orden.getId());

        assertEquals(5, entregada.getEstadoId());
        assertNotNull(entregada.getFechaEntrega());
        assertEquals(5, ordenDAO.buscarPorId(orden.getId()).getEstadoId());
        assertNotNull(ordenDAO.buscarPorId(orden.getId()).getFechaEntrega());
    }

    @Test
    @DisplayName("Rechaza la entrega desde cualquier estado distinto de Facturado (RN-10)")
    void rechazarEntregaDesdeEstadosInvalidos() {
        int[] estadosInvalidos = {1, 2, 3, 5, 6};
        for (int estado : estadosInvalidos) {
            OrdenServicio orden = service.crearOrden(1, "Orden en estado " + estado, lineas(1));
            orden.setEstadoId(estado);

            assertThrows(TransicionEstadoInvalidaException.class,
                    () -> service.entregarOrden(orden.getId()), "Estado inválido: " + estado);
            assertEquals(estado, ordenDAO.buscarPorId(orden.getId()).getEstadoId());
            assertNull(ordenDAO.buscarPorId(orden.getId()).getFechaEntrega());
        }
    }

    @Test
    @DisplayName("Rechaza entregar una orden inexistente o con ID no positivo")
    void rechazarEntregaDeOrdenInexistenteOIdInvalido() {
        assertThrows(EntidadNoEncontradaException.class, () -> service.entregarOrden(999));
        assertThrows(ValidacionException.class, () -> service.entregarOrden(0));
        assertThrows(ValidacionException.class, () -> service.entregarOrden(-5));
    }

    @Test
    @DisplayName("Si el DAO no registra la entrega se lanza CarMotorsException y la orden sigue Facturada")
    void fallarRegistroDeEntrega() {
        OrdenServicio orden = service.crearOrden(1, "Servicio facturado", lineas(1));
        orden.setEstadoId(4);
        ordenDAO.deshabilitarActualizarEstado();

        assertThrows(CarMotorsException.class, () -> service.entregarOrden(orden.getId()));
        assertEquals(4, ordenDAO.buscarPorId(orden.getId()).getEstadoId());
        assertNull(ordenDAO.buscarPorId(orden.getId()).getFechaEntrega());
    }

    private static class OrdenServicioDAOMock implements OrdenServicioDAO {
        private final Map<Integer, OrdenServicio> ordenes = new HashMap<>();
        private int secuencia = 0;
        private boolean permitirActualizarEstado = true;

        void deshabilitarActualizarEstado() {
            permitirActualizarEstado = false;
        }

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
            if (!permitirActualizarEstado) {
                return false;
            }
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
            return false;
        }

        @Override
        public boolean marcarEntregada(int ordenId, int estadoId, LocalDateTime fechaEntrega) {
            if (!permitirActualizarEstado) {
                return false;
            }
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
        private final Map<Integer, Integer> tecnicosAsignados = new HashMap<>();
        private final Map<Integer, List<DetalleTecnico>> asignacionesPorDetalle = new HashMap<>();
        private final Map<Integer, BigDecimal> horasPorDetalle = new HashMap<>();
        private final Map<Integer, Map<Integer, Integer>> reservasPorDetalle = new HashMap<>();
        private int secuencia = 0;

        void agregarReserva(int detalleId, int repuestoId, int cantidad) {
            Map<Integer, Integer> mapa = reservasPorDetalle.get(detalleId);
            if (mapa == null) {
                mapa = new LinkedHashMap<>();
                reservasPorDetalle.put(detalleId, mapa);
            }
            mapa.put(repuestoId, cantidad);
        }

        void fijarHoras(int detalleId, BigDecimal horas) {
            horasPorDetalle.put(detalleId, horas);
        }

        @Override
        public int guardar(OrdenServicioDetalle detalle) {
            secuencia++;
            detalles.add(detalle);
            return secuencia;
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
            asignarTecnico(detalleId, tecnicoId, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }

        @Override
        public void asignarTecnico(int detalleId, int tecnicoId, BigDecimal horasTrabajadas) {
            tecnicosAsignados.put(detalleId, tecnicoId);
            List<DetalleTecnico> lista = asignacionesPorDetalle.get(detalleId);
            if (lista == null) {
                lista = new ArrayList<>();
                asignacionesPorDetalle.put(detalleId, lista);
            }
            BigDecimal normalizadas = horasTrabajadas == null
                    ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    : horasTrabajadas.setScale(2, RoundingMode.HALF_UP);
            lista.add(new DetalleTecnico(detalleId, tecnicoId, normalizadas));
        }

        @Override
        public boolean existeTecnicoAsignado(int detalleId, int tecnicoId) {
            List<DetalleTecnico> lista = asignacionesPorDetalle.get(detalleId);
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
            List<DetalleTecnico> lista = asignacionesPorDetalle.get(detalleId);
            if (lista == null) {
                return new ArrayList<>();
            }
            List<DetalleTecnico> copia = new ArrayList<>();
            for (DetalleTecnico asignacion : lista) {
                copia.add(new DetalleTecnico(
                        asignacion.getDetalleId(),
                        asignacion.getTecnicoId(),
                        asignacion.getHorasTrabajadas()));
            }
            copia.sort((a, b) -> Integer.compare(a.getTecnicoId(), b.getTecnicoId()));
            return copia;
        }

        @Override
        public boolean actualizarHorasTrabajadas(int detalleId, int tecnicoId, BigDecimal horasTrabajadas) {
            List<DetalleTecnico> lista = asignacionesPorDetalle.get(detalleId);
            if (lista == null) {
                return false;
            }
            BigDecimal normalizadas = horasTrabajadas.setScale(2, RoundingMode.HALF_UP);
            for (DetalleTecnico asignacion : lista) {
                if (asignacion.getTecnicoId() == tecnicoId) {
                    asignacion.setHorasTrabajadas(normalizadas);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean tieneHorasTrabajadas(int detalleId) {
            BigDecimal horas = horasPorDetalle.get(detalleId);
            if (horas != null && horas.signum() > 0) {
                return true;
            }
            List<DetalleTecnico> lista = asignacionesPorDetalle.get(detalleId);
            if (lista != null) {
                for (DetalleTecnico asignacion : lista) {
                    if (asignacion.getHorasTrabajadas() != null && asignacion.getHorasTrabajadas().signum() > 0) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public Map<Integer, Integer> listarReservasParaActualizar(int detalleId) {
            Map<Integer, Integer> mapa = reservasPorDetalle.get(detalleId);
            return mapa != null ? new LinkedHashMap<>(mapa) : new LinkedHashMap<>();
        }

        @Override
        public boolean actualizarCostoManoObra(int ordenId, int detalleId, BigDecimal costoManoObra) {
            for (OrdenServicioDetalle detalle : detalles) {
                if (detalle.getOrdenId() == ordenId && detalle.getId() == detalleId) {
                    detalle.setCostoManoObra(costoManoObra);
                    return true;
                }
            }
            return false;
        }

        @Override
        public void eliminar(int ordenId, int detalleId) {
            for (int i = 0; i < detalles.size(); i++) {
                OrdenServicioDetalle d = detalles.get(i);
                if (d.getOrdenId() == ordenId && d.getId() == detalleId) {
                    detalles.remove(i);
                    return;
                }
            }
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

        void agregar(Vehiculo vehiculo) {
            vehiculos.put(vehiculo.getId(), vehiculo);
        }

        @Override
        public int guardar(Vehiculo vehiculo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Vehiculo buscarPorId(int id) {
            return vehiculos.get(id);
        }

        @Override
        public Vehiculo buscarPorPlaca(String placa) {
            for (Vehiculo vehiculo : vehiculos.values()) {
                if (vehiculo.getPlaca().equals(placa)) {
                    return vehiculo;
                }
            }
            return null;
        }

        @Override
        public List<Vehiculo> listarPorCliente(int clienteId) {
            return new ArrayList<>();
        }

        @Override
        public List<Vehiculo> listarTodos() {
            return new ArrayList<>(vehiculos.values());
        }
    }

    private static class ClienteDAOMock implements ClienteDAO {
        private final Map<Integer, Cliente> clientes = new HashMap<>();

        void agregar(Cliente cliente) {
            clientes.put(cliente.getId(), cliente);
        }

        @Override
        public List<Cliente> listarTodos() {
            return new ArrayList<>(clientes.values());
        }

        @Override
        public List<Cliente> listarActivos() {
            return new ArrayList<>();
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
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean actualizar(Cliente cliente) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean actualizarEstadoActivo(int id, boolean activo) {
            throw new UnsupportedOperationException();
        }
    }

    private static class ServicioTipoDAOMock implements ServicioTipoDAO {
        private final Map<Integer, ServicioTipo> tipos = new HashMap<>();

        void agregar(ServicioTipo tipo) {
            tipos.put(tipo.getId(), tipo);
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
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean actualizar(ServicioTipo servicioTipo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean eliminar(int id) {
            throw new UnsupportedOperationException();
        }
    }

    private static class TecnicoDAOMock implements TecnicoDAO {
        private final Map<Integer, Tecnico> tecnicos = new HashMap<>();
        private final Map<Integer, Integer> cargas = new HashMap<>();

        void agregar(Tecnico tecnico) {
            tecnicos.put(tecnico.getId(), tecnico);
        }

        void fijarCarga(int tecnicoId, int carga) {
            cargas.put(tecnicoId, carga);
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
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean actualizar(Tecnico tecnico) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean actualizarEstadoActivo(int id, boolean activo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<CargaLineaTecnico> listarCargaPorTecnico(int tecnicoId) {
            return new ArrayList<>();
        }

        @Override
        public Map<Integer, Integer> contarCargaActivaPorTecnico() {
            return new HashMap<>(cargas);
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
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean actualizar(Repuesto repuesto) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean desactivar(int id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean reactivar(int id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean descontarReserva(int repuestoId, int cantidad) {
            Repuesto r = repuestos.get(repuestoId);
            if (r == null || r.getCantidadReservada() < cantidad) {
                return false;
            }
            r.setCantidadReservada(r.getCantidadReservada() - cantidad);
            return true;
        }

        @Override
        public boolean aumentarReservado(int repuestoId, int cantidad) {
            Repuesto r = repuestos.get(repuestoId);
            if (r == null) {
                return false;
            }
            r.setCantidadReservada(r.getCantidadReservada() + cantidad);
            return true;
        }
    }
}
