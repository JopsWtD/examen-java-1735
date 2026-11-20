package com.carmotors.sgt.service;

import com.carmotors.sgt.controller.OrdenServicioController;
import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.StockInsuficienteException;
import com.carmotors.sgt.exception.TransicionEstadoInvalidaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.CargaLineaTecnico;
import com.carmotors.sgt.model.Cliente;
import com.carmotors.sgt.model.ConsumoRepuesto;
import com.carmotors.sgt.model.DetalleRepuesto;
import com.carmotors.sgt.model.DetalleTecnico;
import com.carmotors.sgt.model.Lote;
import com.carmotors.sgt.model.OrdenServicio;
import com.carmotors.sgt.model.OrdenServicioDetalle;
import com.carmotors.sgt.model.Proveedor;
import com.carmotors.sgt.model.ProveedorRepuesto;
import com.carmotors.sgt.model.Repuesto;
import com.carmotors.sgt.model.ResultadoCompletarOrden;
import com.carmotors.sgt.model.ServicioTipo;
import com.carmotors.sgt.model.StockRepuesto;
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
import com.carmotors.sgt.dao.impl.EspecialidadDAOImpl;
import com.carmotors.sgt.service.facade.TallerFacade;
import com.carmotors.sgt.service.observer.StockObserver;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de la finalización de órdenes (T5.2, RF-13, RN-03, RN-04, RN-08, RN-22) con DAOs
 * en memoria y sin transacción JDBC. Las fechas son relativas a {@code LocalDate.now()}.
 */
class OrdenServicioCompletarTest {

    private static final int PENDIENTE = 1;
    private static final int EN_PROCESO = 2;
    private static final int COMPLETADO = 3;

    private static final int PASTILLAS = 4;
    private static final int LIQUIDO = 6;
    private static final int BATERIA = 7;
    private static final int FILTRO = 12;

    private final LocalDate hoy = LocalDate.now();

    private OrdenServicioService service;
    private InventarioService inventarioService;
    private OrdenServicioDAOMock ordenDAO;
    private RepuestoDAOMock repuestoDAO;
    private LoteDAOMock loteDAO;
    private ObserverEspia observer;

    @BeforeEach
    void setUp() {
        ordenDAO = new OrdenServicioDAOMock();
        OrdenServicioDetalleDAOMock detalleDAO = new OrdenServicioDetalleDAOMock();
        DetalleRepuestoDAOMock detalleRepuestoDAO = new DetalleRepuestoDAOMock();
        repuestoDAO = new RepuestoDAOMock();
        loteDAO = new LoteDAOMock();

        repuestoDAO.agregar(new Repuesto(PASTILLAS, 1, "Pastillas de freno delanteras", "Bosch",
                new BigDecimal("120000.00"), 1, 4, true));
        repuestoDAO.agregar(new Repuesto(LIQUIDO, 4, "Líquido de frenos DOT4 (500 ml)", "Bosch",
                new BigDecimal("28000.00"), 3, 4, true));
        repuestoDAO.agregar(new Repuesto(BATERIA, 2, "Batería 12V 45Ah", "Varta",
                new BigDecimal("380000.00"), 3, 2, true));
        repuestoDAO.agregar(new Repuesto(FILTRO, 4, "Filtro de cabina", "Mann",
                new BigDecimal("35000.00"), 2, 3, true));

        loteDAO.agregar(new Lote(5, "LOT-PAS", 2, PASTILLAS, hoy.minusDays(150), null, 10, 10));
        loteDAO.agregar(new Lote(9, "LOT-LIQ-B", 2, LIQUIDO, hoy.minusDays(20), hoy.plusDays(40), 10, 10));
        loteDAO.agregar(new Lote(7, "LOT-LIQ-A", 2, LIQUIDO, hoy.minusDays(170), hoy.plusDays(13), 12, 2));
        loteDAO.agregar(new Lote(8, "LOT-LIQ-VENC", 2, LIQUIDO, hoy.minusDays(800), hoy.minusDays(48), 6, 2));
        loteDAO.agregar(new Lote(10, "LOT-BAT-VENC", 4, BATERIA, hoy.minusDays(400), hoy.minusDays(1), 10, 10));
        loteDAO.agregar(new Lote(11, "LOT-BAT", 4, BATERIA, hoy.minusDays(30), null, 2, 2));
        loteDAO.agregar(new Lote(14, "LOT-FCA", 1, FILTRO, hoy.minusDays(60), null, 10, 4));

        // Orden 2 En Proceso: dos líneas que reservan el mismo líquido (se agrupan: 1 + 2 = 3).
        ordenDAO.agregar(new OrdenServicio(2, 2, EN_PROCESO, LocalDateTime.now(), "Ruido al frenar"));
        detalleDAO.agregar(linea(2, 2, "90000.00"));
        detalleDAO.agregar(linea(10, 2, "90000.00"));
        detalleRepuestoDAO.agregar(new DetalleRepuesto(2, PASTILLAS, 1, "Pastillas de freno delanteras",
                new BigDecimal("120000.00")));
        detalleRepuestoDAO.agregar(new DetalleRepuesto(2, LIQUIDO, 1, "Líquido de frenos DOT4 (500 ml)",
                new BigDecimal("28000.00")));
        detalleRepuestoDAO.agregar(new DetalleRepuesto(10, LIQUIDO, 2, "Líquido de frenos DOT4 (500 ml)",
                new BigDecimal("28000.00")));

        ordenDAO.agregar(new OrdenServicio(1, 1, PENDIENTE, LocalDateTime.now(), "Cambio de aceite"));
        detalleDAO.agregar(linea(1, 1, "60000.00"));

        ordenDAO.agregar(new OrdenServicio(3, 3, COMPLETADO, LocalDateTime.now(), "No enciende"));
        detalleDAO.agregar(linea(4, 3, "40000.00"));

        // Orden 4: línea con mano de obra negativa (RN-08).
        ordenDAO.agregar(new OrdenServicio(4, 4, EN_PROCESO, LocalDateTime.now(), "Mantenimiento"));
        detalleDAO.agregar(linea(40, 4, "-1.00"));

        // Orden 5: la batería solo tiene 2 unidades vigentes (el lote de 10 venció ayer).
        ordenDAO.agregar(new OrdenServicio(5, 5, EN_PROCESO, LocalDateTime.now(), "Batería"));
        detalleDAO.agregar(linea(50, 5, "40000.00"));
        detalleRepuestoDAO.agregar(new DetalleRepuesto(50, BATERIA, 3, "Batería 12V 45Ah",
                new BigDecimal("380000.00")));

        // Orden 6: sin repuestos.
        ordenDAO.agregar(new OrdenServicio(6, 6, EN_PROCESO, LocalDateTime.now(), "Alineación"));
        detalleDAO.agregar(linea(60, 6, "70000.00"));

        // Orden 7: el filtro queda con disponible 2 <= mínimo 3 tras completar.
        ordenDAO.agregar(new OrdenServicio(7, 7, EN_PROCESO, LocalDateTime.now(), "Filtro de cabina"));
        detalleDAO.agregar(linea(70, 7, "30000.00"));
        detalleRepuestoDAO.agregar(new DetalleRepuesto(70, FILTRO, 2, "Filtro de cabina",
                new BigDecimal("35000.00")));

        inventarioService = new InventarioService(repuestoDAO, loteDAO,
                new ProveedorRepuestoDAOVacio(), new ProveedorDAOVacio());
        observer = new ObserverEspia();
        inventarioService.agregarObserver(observer);

        service = new OrdenServicioService(ordenDAO, detalleDAO, new VehiculoDAOVacio(), new ClienteDAOVacio(),
                new ServicioTipoDAOVacio(), new TecnicoDAOVacio(), repuestoDAO, detalleRepuestoDAO, loteDAO,
                inventarioService, false);
    }

    @Test
    void fachadaNotificaUnaSolaVezYUnicamenteSiLaOrdenSeCompleta() {
        TecnicoService tecnicos = new TecnicoService(new TecnicoDAOVacio(), new EspecialidadDAOImpl());
        TallerFacade fachada = new TallerFacade(service, inventarioService, tecnicos);
        OrdenServicioController controlador = new OrdenServicioController(service, fachada);

        assertThrows(StockInsuficienteException.class, () -> controlador.completarOrden(5));
        assertTrue(observer.notificados.isEmpty());

        ResultadoCompletarOrden resultado = controlador.completarOrden(7);
        assertEquals(COMPLETADO, resultado.getOrden().getEstadoId());
        assertEquals(1, observer.notificados.size());
        assertEquals(FILTRO, observer.notificados.get(0).getRepuesto().getId());
    }

    private OrdenServicioDetalle linea(int id, int ordenId, String costoManoObra) {
        return new OrdenServicioDetalle(id, ordenId, 1, "Servicio " + id, new BigDecimal(costoManoObra), null);
    }

    @Test
    @DisplayName("Completa la orden: descuenta lotes por FEFO, reduce el reservado y registra estado y fecha")
    void completarConExito() {
        LocalDateTime antes = LocalDateTime.now().withNano(0);

        ResultadoCompletarOrden resultado = service.completarOrden(2);

        OrdenServicio orden = resultado.getOrden();
        assertEquals(COMPLETADO, orden.getEstadoId());
        assertNotNull(orden.getFechaCompletado());
        assertFalse(orden.getFechaCompletado().isBefore(antes));
        assertEquals(COMPLETADO, ordenDAO.buscarPorId(2).getEstadoId());
        assertEquals(orden.getFechaCompletado(), ordenDAO.buscarPorId(2).getFechaCompletado());

        assertEquals(9, loteDAO.cantidadActual(5));
        assertEquals(0, loteDAO.cantidadActual(7));
        assertEquals(9, loteDAO.cantidadActual(9));
        assertEquals(2, loteDAO.cantidadActual(8));

        assertEquals(0, repuestoDAO.buscarPorId(PASTILLAS).getCantidadReservada());
        assertEquals(0, repuestoDAO.buscarPorId(LIQUIDO).getCantidadReservada());

        List<ConsumoRepuesto> consumos = resultado.getConsumos();
        assertEquals(2, consumos.size());
        assertEquals(PASTILLAS, consumos.get(0).getRepuestoId());
        assertEquals(1, consumos.get(0).getUnidadesDescontadas());
        assertEquals(LIQUIDO, consumos.get(1).getRepuestoId());
        assertEquals(3, consumos.get(1).getUnidadesDescontadas());
        assertTrue(observer.notificados.isEmpty());
    }

    @Test
    @DisplayName("Rechaza una orden inexistente")
    void rechazarOrdenInexistente() {
        assertThrows(EntidadNoEncontradaException.class, () -> service.completarOrden(99));
        assertThrows(ValidacionException.class, () -> service.completarOrden(0));
    }

    @Test
    @DisplayName("Rechaza completar una orden Pendiente o ya Completada sin cambiar nada")
    void rechazarEstadoInvalido() {
        assertThrows(TransicionEstadoInvalidaException.class, () -> service.completarOrden(1));
        assertThrows(TransicionEstadoInvalidaException.class, () -> service.completarOrden(3));

        assertEquals(PENDIENTE, ordenDAO.buscarPorId(1).getEstadoId());
        assertNull(ordenDAO.buscarPorId(1).getFechaCompletado());
        assertTrue(observer.notificados.isEmpty());
    }

    @Test
    @DisplayName("Rechaza una línea con mano de obra negativa (RN-08)")
    void rechazarManoDeObraNegativa() {
        ValidacionException error = assertThrows(ValidacionException.class, () -> service.completarOrden(4));

        assertTrue(error.getMessage().contains("RN-08"), error.getMessage());
        assertEquals(EN_PROCESO, ordenDAO.buscarPorId(4).getEstadoId());
    }

    @Test
    @DisplayName("Rechaza si un lote vencido deja sin cobertura la reserva y no cambia nada")
    void rechazarCoberturaInsuficientePorLoteVencido() {
        StockInsuficienteException error = assertThrows(StockInsuficienteException.class,
                () -> service.completarOrden(5));

        assertTrue(error.getMessage().contains("Batería 12V 45Ah"), error.getMessage());
        assertTrue(error.getMessage().contains("requiere 3"), error.getMessage());
        assertTrue(error.getMessage().contains("stock operativo es 2"), error.getMessage());
        assertEquals(10, loteDAO.cantidadActual(10));
        assertEquals(2, loteDAO.cantidadActual(11));
        assertEquals(3, repuestoDAO.buscarPorId(BATERIA).getCantidadReservada());
        assertEquals(EN_PROCESO, ordenDAO.buscarPorId(5).getEstadoId());
        assertNull(ordenDAO.buscarPorId(5).getFechaCompletado());
        assertTrue(observer.notificados.isEmpty());
    }

    @Test
    @DisplayName("Una orden sin repuestos se completa sin tocar el inventario")
    void completarOrdenSinRepuestos() {
        ResultadoCompletarOrden resultado = service.completarOrden(6);

        assertEquals(COMPLETADO, resultado.getOrden().getEstadoId());
        assertNotNull(resultado.getOrden().getFechaCompletado());
        assertTrue(resultado.getConsumos().isEmpty());
        assertEquals(0, loteDAO.descuentosRealizados);
        assertTrue(observer.notificados.isEmpty());
    }

    @Test
    @DisplayName("Notifica al Observer cuando el repuesto queda en o bajo el mínimo (RN-22)")
    void notificarStockBajoTrasCompletar() {
        service.completarOrden(7);

        assertEquals(2, loteDAO.cantidadActual(14));
        assertEquals(0, repuestoDAO.buscarPorId(FILTRO).getCantidadReservada());
        assertEquals(1, observer.notificados.size());
        StockRepuesto stock = observer.notificados.get(0);
        assertEquals(FILTRO, stock.getRepuesto().getId());
        assertEquals(2, stock.getDisponible());
        assertEquals(3, stock.getStockMinimo());
    }

    @Test
    @DisplayName("Bloquea completar si existe déficit global por lotes vencidos aunque el operativo cubra esta orden (§2.6)")
    void bloquearCompletarSiHayDeficitGlobalDeReservas() {
        // El repuesto FILTRO tiene stock operativo = 4 (lote 14), y orden 7 requiere 2 unidades.
        // Simulamos otra orden activa que eleva cantidad_reservada global a 6 (déficit = 2).
        repuestoDAO.buscarPorId(FILTRO).setCantidadReservada(6);

        assertThrows(StockInsuficienteException.class, () -> service.completarOrden(7));
        assertEquals(EN_PROCESO, ordenDAO.buscarPorId(7).getEstadoId());
        assertEquals(4, loteDAO.cantidadActual(14));
        assertEquals(6, repuestoDAO.buscarPorId(FILTRO).getCantidadReservada());
    }

    // ------------------------------------------------------------------
    // Observer espía y DAOs en memoria
    // ------------------------------------------------------------------

    private static class ObserverEspia implements StockObserver {
        private final List<StockRepuesto> notificados = new ArrayList<>();

        @Override
        public void onStockBajo(StockRepuesto stock) {
            notificados.add(stock);
        }
    }

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
            OrdenServicio guardada = ordenes.get(id);
            if (guardada == null) {
                return null;
            }
            OrdenServicio copia = new OrdenServicio(guardada.getId(), guardada.getVehiculoId(),
                    guardada.getEstadoId(), guardada.getFechaRecepcion(), guardada.getDescripcionProblema());
            copia.setFechaCompletado(guardada.getFechaCompletado());
            return copia;
        }

        @Override
        public boolean actualizarEstado(int ordenId, int nuevoEstadoId) {
            throw new UnsupportedOperationException("No se usa en estas pruebas.");
        }

        @Override
        public boolean marcarCompletada(int ordenId, int estadoId, LocalDateTime fechaCompletado) {
            OrdenServicio orden = ordenes.get(ordenId);
            if (orden == null) {
                return false;
            }
            orden.setEstadoId(estadoId);
            orden.setFechaCompletado(fechaCompletado);
            return true;
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
        public boolean descontarReserva(int repuestoId, int cantidad) {
            Repuesto repuesto = buscarPorId(repuestoId);
            if (repuesto == null || repuesto.getCantidadReservada() < cantidad) {
                return false;
            }
            repuesto.setCantidadReservada(repuesto.getCantidadReservada() - cantidad);
            return true;
        }

        @Override
        public boolean aumentarReservado(int repuestoId, int cantidad) {
            throw new UnsupportedOperationException("No se usa en estas pruebas.");
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

        /** Devuelve los lotes en el orden de inserción: la clase FEFO es la que los ordena. */
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
            throw new UnsupportedOperationException("No se usa en estas pruebas.");
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
            for (Lote lote : lotes) {
                if (lote.getId() == loteId && lote.getCantidadActual() >= cantidad) {
                    lote.setCantidadActual(lote.getCantidadActual() - cantidad);
                    descuentosRealizados++;
                    return true;
                }
            }
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
