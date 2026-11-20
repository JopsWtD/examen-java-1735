package com.carmotors.sgt.service;

import com.carmotors.sgt.controller.ReportesController;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.RegistroConsumoRepuesto;
import com.carmotors.sgt.model.RegistroMantenimientoPreventivo;
import com.carmotors.sgt.model.RegistroProductividadTecnico;
import com.carmotors.sgt.model.RegistroServicioSolicitado;
import com.carmotors.sgt.model.ReporteConsumoRepuestos;
import com.carmotors.sgt.model.ReporteConsumoRepuestos.DetalleConsumo;
import com.carmotors.sgt.model.ReporteConsumoRepuestos.ResumenConsumo;
import com.carmotors.sgt.dao.ReporteDAO;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias del reporte de consumo de repuestos (T7.3 — RF-27, §4.2, §4.3, RNF-11, HU-13).
 */
class ReporteConsumoRepuestosTest {

    private ReporteDAOMock reporteDAO;
    private ReporteService reporteService;
    private ReportesController reportesController;

    @BeforeEach
    void setUp() {
        reporteDAO = new ReporteDAOMock();
        reporteService = new ReporteService(reporteDAO);
        reportesController = new ReportesController(reporteService);
    }

    @Test
    @DisplayName("Calcula consumo por repuesto, tipo de servicio y desglose usando precios históricos y orden descendente")
    void calculaConsumoConSnapshotsHistoricosYOrdenDescendente() {
        int completado = new EstadoCompletado().getEstadoId();
        int facturado = new EstadoFacturado().getEstadoId();
        int entregado = new EstadoEntregado().getEstadoId();

        // Orden 3 (Completado): Batería 1 x 380000.00 = 380000.00 en Cambio de batería
        reporteDAO.agregar(new RegistroConsumoRepuesto(
                3, completado, LocalDateTime.of(2026, 11, 12, 16, 45),
                7, "Batería 12V 45Ah", 4, "Cambio de batería",
                1, new BigDecimal("380000.00")));

        // Orden 4 (Facturado): Aceite 4 x 38000.00 = 152000.00 y Filtro 1 x 25000.00 = 25000.00 (Cambio de aceite)
        //                      Bujía 4 x 45000.00 = 180000.00 (Afinación de motor)
        reporteDAO.agregar(new RegistroConsumoRepuesto(
                4, facturado, LocalDateTime.of(2026, 11, 6, 15, 30),
                1, "Aceite sintético 10W-40", 1, "Cambio de aceite y filtro",
                4, new BigDecimal("38000.00")));
        reporteDAO.agregar(new RegistroConsumoRepuesto(
                4, facturado, LocalDateTime.of(2026, 11, 6, 15, 30),
                2, "Filtro de aceite", 1, "Cambio de aceite y filtro",
                1, new BigDecimal("25000.00")));
        reporteDAO.agregar(new RegistroConsumoRepuesto(
                4, facturado, LocalDateTime.of(2026, 11, 6, 15, 30),
                8, "Bujía de iridio", 5, "Afinación de motor",
                4, new BigDecimal("45000.00")));

        // Orden 5 (Entregado, precio histórico anterior diferente para Aceite: 35000.00):
        // Aceite 4 x 35000.00 = 140000.00 -> total histórico Aceite = 152000 + 140000 = 292000.00 (8 u.)
        reporteDAO.agregar(new RegistroConsumoRepuesto(
                5, entregado, LocalDateTime.of(2026, 5, 28, 11, 0),
                1, "Aceite 10W-40 (nombre anterior)", 1, "Cambio de aceite y filtro",
                4, new BigDecimal("35000.00")));

        ReporteConsumoRepuestos reporte = reporteService.generarReporteConsumoRepuestos(
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 11, 30));

        assertFalse(reporte.estaVacio());
        assertEquals(14, reporte.getTotalUnidades());
        // 380000 + 152000 + 25000 + 180000 + 140000 = 877000.00
        assertEquals(new BigDecimal("877000.00"), reporte.getValorComercialTotal());

        // 1) Por repuesto ordenado por valor comercial descendente:
        // Batería (380000) > Aceite (292000) > Bujía (180000) > Filtro (25000)
        List<ResumenConsumo> porRepuesto = reporte.getPorRepuesto();
        assertEquals(4, porRepuesto.size());
        assertEquals(7, porRepuesto.get(0).getId());
        assertEquals(new BigDecimal("380000.00"), porRepuesto.get(0).getValorComercial());
        assertEquals(1, porRepuesto.get(1).getId());
        assertEquals("Aceite sintético 10W-40", porRepuesto.get(1).getNombre());
        assertEquals(8, porRepuesto.get(1).getUnidades());
        assertEquals(new BigDecimal("292000.00"), porRepuesto.get(1).getValorComercial());
        assertEquals(8, porRepuesto.get(2).getId());
        assertEquals(new BigDecimal("180000.00"), porRepuesto.get(2).getValorComercial());
        assertEquals(2, porRepuesto.get(3).getId());
        assertEquals(new BigDecimal("25000.00"), porRepuesto.get(3).getValorComercial());

        // 2) Por tipo de servicio ordenado por valor comercial descendente:
        // Cambio de batería (380000) > Cambio de aceite y filtro (317000) > Afinación de motor (180000)
        List<ResumenConsumo> porServicio = reporte.getPorTipoServicio();
        assertEquals(3, porServicio.size());
        assertEquals(4, porServicio.get(0).getId());
        assertEquals(new BigDecimal("380000.00"), porServicio.get(0).getValorComercial());
        assertEquals(1, porServicio.get(1).getId());
        assertEquals(9, porServicio.get(1).getUnidades());
        assertEquals(new BigDecimal("317000.00"), porServicio.get(1).getValorComercial());
        assertEquals(5, porServicio.get(2).getId());
        assertEquals(new BigDecimal("180000.00"), porServicio.get(2).getValorComercial());

        // 3) Desglose combinado (repuesto, servicio):
        List<DetalleConsumo> detalle = reporte.getDetallePorRepuestoYServicio();
        assertEquals(4, detalle.size());
        assertEquals(7, detalle.get(0).getRepuestoId());
        assertEquals(4, detalle.get(0).getServicioTipoId());
        assertEquals(new BigDecimal("380000.00"), detalle.get(0).getValorComercial());
        assertEquals(1, detalle.get(1).getRepuestoId());
        assertEquals(1, detalle.get(1).getServicioTipoId());
        assertEquals(8, detalle.get(1).getUnidades());
        assertEquals(new BigDecimal("292000.00"), detalle.get(1).getValorComercial());
    }

    @Test
    @DisplayName("Excluye estrictamente reservas de Pendiente, En Proceso y órdenes Canceladas (RF-27)")
    void excluyePendienteEnProcesoYCancelada() {
        LocalDateTime fecha = LocalDateTime.of(2026, 11, 10, 12, 0);

        reporteDAO.agregar(new RegistroConsumoRepuesto(
                1, new EstadoPendiente().getEstadoId(), fecha,
                1, "Aceite", 1, "Cambio de aceite", 4, new BigDecimal("38000.00")));
        reporteDAO.agregar(new RegistroConsumoRepuesto(
                2, new EstadoEnProceso().getEstadoId(), fecha,
                4, "Pastillas", 2, "Frenos", 2, new BigDecimal("120000.00")));
        reporteDAO.agregar(new RegistroConsumoRepuesto(
                6, new EstadoCancelada().getEstadoId(), fecha,
                6, "Líquido frenos", 2, "Frenos", 1, new BigDecimal("28000.00")));

        // Únicos válidos: Completado (3), Facturado (4), Entregado (5)
        reporteDAO.agregar(new RegistroConsumoRepuesto(
                3, new EstadoCompletado().getEstadoId(), fecha,
                2, "Filtro", 1, "Cambio de aceite", 1, new BigDecimal("25000.00")));
        reporteDAO.agregar(new RegistroConsumoRepuesto(
                4, new EstadoFacturado().getEstadoId(), fecha,
                2, "Filtro", 1, "Cambio de aceite", 2, new BigDecimal("25000.00")));
        reporteDAO.agregar(new RegistroConsumoRepuesto(
                5, new EstadoEntregado().getEstadoId(), fecha,
                2, "Filtro", 1, "Cambio de aceite", 1, new BigDecimal("25000.00")));

        ReporteConsumoRepuestos reporte = reporteService.generarReporteConsumoRepuestos(null, null);

        assertEquals(1, reporte.getPorRepuesto().size());
        assertEquals(2, reporte.getPorRepuesto().get(0).getId());
        assertEquals(4, reporte.getTotalUnidades());
        assertEquals(new BigDecimal("100000.00"), reporte.getValorComercialTotal());
    }

    @Test
    @DisplayName("Filtra estrictamente por rango inclusivo de fecha_completado (§4.2)")
    void filtraPorRangoInclusivoDeFechaCompletado() {
        int completado = new EstadoCompletado().getEstadoId();

        // Antes del límite inferior -> excluido
        reporteDAO.agregar(new RegistroConsumoRepuesto(
                10, completado, LocalDateTime.of(2026, 10, 31, 23, 59, 59),
                1, "Aceite", 1, "Cambio de aceite", 2, new BigDecimal("30000.00")));
        // En el límite inferior exacto -> incluido
        reporteDAO.agregar(new RegistroConsumoRepuesto(
                11, completado, LocalDateTime.of(2026, 11, 1, 0, 0, 0),
                2, "Filtro", 1, "Cambio de aceite", 1, new BigDecimal("25000.00")));
        // En el límite superior exacto -> incluido
        reporteDAO.agregar(new RegistroConsumoRepuesto(
                12, completado, LocalDateTime.of(2026, 11, 15, 23, 59, 59),
                3, "Bujía", 5, "Afinación", 2, new BigDecimal("40000.00")));
        // Después del límite superior -> excluido
        reporteDAO.agregar(new RegistroConsumoRepuesto(
                13, completado, LocalDateTime.of(2026, 11, 16, 0, 0, 0),
                4, "Batería", 4, "Cambio de batería", 1, new BigDecimal("350000.00")));
        // Sin fecha_completado -> excluido
        reporteDAO.agregar(new RegistroConsumoRepuesto(
                14, completado, null,
                5, "Disco", 2, "Frenos", 1, new BigDecimal("210000.00")));

        ReporteConsumoRepuestos reporte = reporteService.generarReporteConsumoRepuestos(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 15));

        assertEquals(2, reporte.getPorRepuesto().size());
        assertEquals(3, reporte.getTotalUnidades());
        assertEquals(new BigDecimal("105000.00"), reporte.getValorComercialTotal());
        assertEquals(3, reporte.getPorRepuesto().get(0).getId());
        assertEquals(2, reporte.getPorRepuesto().get(1).getId());
    }

    @Test
    @DisplayName("Desempata de forma determinista por unidades desc y nombre asc cuando el valor coincide")
    void desempataDeFormaDeterministaEnIgualdadDeValor() {
        int completado = new EstadoCompletado().getEstadoId();
        LocalDateTime fecha = LocalDateTime.of(2026, 11, 5, 10, 0);

        // Todos suman 100000.00:
        // Repuesto 20 ("Zapatas"): 5 u. -> primero por mayor cantidad de unidades
        // Repuesto 10 ("Aceite"):  2 u. -> segundo por orden alfabético ("Aceite" < "Filtro")
        // Repuesto 30 ("Filtro"):  2 u. -> tercero
        reporteDAO.agregar(new RegistroConsumoRepuesto(
                1, completado, fecha, 30, "Filtro", 1, "Servicio A", 2, new BigDecimal("50000.00")));
        reporteDAO.agregar(new RegistroConsumoRepuesto(
                2, completado, fecha, 10, "Aceite", 1, "Servicio A", 2, new BigDecimal("50000.00")));
        reporteDAO.agregar(new RegistroConsumoRepuesto(
                3, completado, fecha, 20, "Zapatas", 1, "Servicio A", 5, new BigDecimal("20000.00")));

        ReporteConsumoRepuestos reporte = reporteService.generarReporteConsumoRepuestos(null, null);

        List<ResumenConsumo> porRepuesto = reporte.getPorRepuesto();
        assertEquals(3, porRepuesto.size());
        assertEquals(20, porRepuesto.get(0).getId());
        assertEquals(10, porRepuesto.get(1).getId());
        assertEquals(30, porRepuesto.get(2).getId());
    }

    @Test
    @DisplayName("Retorna reporte vacío con totales en cero cuando no hay datos en el rango (HU-13)")
    void retornaReporteVacioCuandoNoHayDatos() {
        ReporteConsumoRepuestos reporte = reportesController.generarReporteConsumoRepuestos(
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));

        assertTrue(reporte.estaVacio());
        assertTrue(reporte.getPorRepuesto().isEmpty());
        assertTrue(reporte.getPorTipoServicio().isEmpty());
        assertTrue(reporte.getDetallePorRepuestoYServicio().isEmpty());
        assertEquals(0, reporte.getTotalUnidades());
        assertEquals(new BigDecimal("0.00"), reporte.getValorComercialTotal());
    }

    @Test
    @DisplayName("Rechaza rango de fechas invertido y dependencias nulas en servicio y controlador")
    void rechazaRangoFechasInvalidoYDependenciasNulas() {
        assertThrows(ValidacionException.class, () -> reporteService.generarReporteConsumoRepuestos(
                LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 10)));
        assertThrows(ValidacionException.class, () -> new ReporteService(null));
        assertThrows(IllegalArgumentException.class, () -> new ReportesController(null));
    }

    @Test
    @DisplayName("Controlador delega correctamente la generación del reporte con rango abierto o acotado")
    void controladorDelegaCorrectamente() {
        reporteDAO.agregar(new RegistroConsumoRepuesto(
                7, new EstadoEntregado().getEstadoId(), LocalDateTime.of(2026, 8, 15, 9, 0),
                12, "Filtro de cabina", 9, "Cambio de filtro de aire y cabina",
                2, new BigDecimal("35000.00")));

        ReporteConsumoRepuestos reporte = reportesController.generarReporteConsumoRepuestos(
                LocalDate.of(2026, 8, 1), null);

        assertFalse(reporte.estaVacio());
        assertEquals(2, reporte.getTotalUnidades());
        assertEquals(new BigDecimal("70000.00"), reporte.getValorComercialTotal());
        assertEquals(1, reporte.getDetallePorRepuestoYServicio().size());
    }

    private static class ReporteDAOMock implements ReporteDAO {
        private final List<RegistroConsumoRepuesto> registros = new ArrayList<>();

        void agregar(RegistroConsumoRepuesto registro) {
            registros.add(registro);
        }

        @Override
        public List<RegistroServicioSolicitado> listarServiciosSolicitados(LocalDate fechaDesde, LocalDate fechaHasta) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<RegistroConsumoRepuesto> listarConsumoRepuestos(LocalDate fechaDesde, LocalDate fechaHasta) {
            return new ArrayList<>(registros);
        }

        @Override
        public List<RegistroProductividadTecnico> listarProductividadTecnicos(LocalDate fechaDesde,
                                                                              LocalDate fechaHasta) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<RegistroMantenimientoPreventivo> listarMantenimientosPreventivos() {
            throw new UnsupportedOperationException();
        }
    }
}
