package com.carmotors.sgt.service;

import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.RecordatorioMantenimiento;
import com.carmotors.sgt.model.RegistroConsumoRepuesto;
import com.carmotors.sgt.model.RegistroMantenimientoPreventivo;
import com.carmotors.sgt.model.RegistroProductividadTecnico;
import com.carmotors.sgt.model.RegistroServicioSolicitado;
import com.carmotors.sgt.dao.ReporteDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del reporte de recordatorios de mantenimiento preventivo (T7.5, RF-21, RN-15) con un DAO
 * en memoria y una fecha de referencia fija.
 */
class ReporteRecordatoriosTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 19);
    private static final int CAMBIO_ACEITE = 1;
    private static final int ALINEACION = 3;

    private ReporteDAOMock reporteDAO;
    private ReporteService service;

    @BeforeEach
    void setUp() {
        reporteDAO = new ReporteDAOMock();
        service = new ReporteService(reporteDAO);
    }

    /** Línea preventiva entregada: el intervalo es de 6 meses para aceite y 12 para alineación. */
    private void entrega(int vehiculoId, String placa, int clienteId, String cliente, boolean activo,
                         int servicioTipoId, int ordenId, LocalDate fechaEntrega) {
        String servicio = servicioTipoId == CAMBIO_ACEITE ? "Cambio de aceite y filtro" : "Alineación y balanceo";
        int intervalo = servicioTipoId == CAMBIO_ACEITE ? 6 : 12;
        reporteDAO.agregar(new RegistroMantenimientoPreventivo(vehiculoId, placa, "Marca", "Modelo", clienteId,
                cliente, "31000000" + clienteId, "cliente" + clienteId + "@correo.com", activo, servicioTipoId,
                servicio, intervalo, ordenId, fechaEntrega.atTime(10, 0)));
    }

    private List<String> placas(List<RecordatorioMantenimiento> recordatorios) {
        return recordatorios.stream()
                .map(recordatorio -> recordatorio.getPlaca())
                .collect(Collectors.toList());
    }

    @Test
    @DisplayName("Recordatorio próximo: la fecha sugerida cae dentro de los 30 días (caso de data.sql)")
    void recordatorioProximo() {
        entrega(5, "MNO345", 5, "Sandra Milena Vega", true, CAMBIO_ACEITE, 5, LocalDate.of(2026, 5, 29));

        List<RecordatorioMantenimiento> recordatorios = service.generarReporteRecordatorios(HOY);

        assertEquals(1, recordatorios.size());
        RecordatorioMantenimiento recordatorio = recordatorios.get(0);
        assertEquals("Sandra Milena Vega", recordatorio.getClienteNombre());
        assertEquals("MNO345", recordatorio.getPlaca());
        assertEquals("Cambio de aceite y filtro", recordatorio.getNombreServicio());
        assertEquals(LocalDate.of(2026, 5, 29), recordatorio.getFechaUltimaEntrega());
        assertEquals(LocalDate.of(2026, 11, 29), recordatorio.getFechaSugerida());
        assertEquals(10, recordatorio.getDiasRestantes());
        assertFalse(recordatorio.isVencido());
    }

    @Test
    @DisplayName("Recordatorio vencido: la fecha sugerida ya pasó")
    void recordatorioVencido() {
        entrega(1, "ABC123", 1, "María Fernanda Rojas", true, ALINEACION, 20, LocalDate.of(2025, 11, 1));

        List<RecordatorioMantenimiento> recordatorios = service.generarReporteRecordatorios(HOY);

        assertEquals(1, recordatorios.size());
        RecordatorioMantenimiento recordatorio = recordatorios.get(0);
        assertEquals(LocalDate.of(2026, 11, 1), recordatorio.getFechaSugerida());
        assertTrue(recordatorio.isVencido());
        assertEquals(18, recordatorio.getDiasAtraso());
        assertEquals(-18, recordatorio.getDiasRestantes());
    }

    @Test
    @DisplayName("Límites: se incluyen la fecha sugerida de hoy y la de dentro de 30 días; la de 31 no")
    void limitesDeLaVentana() {
        entrega(1, "HOY000", 1, "Cliente Uno", true, CAMBIO_ACEITE, 1, LocalDate.of(2026, 5, 19));
        entrega(2, "DIA030", 2, "Cliente Dos", true, CAMBIO_ACEITE, 2, LocalDate.of(2026, 6, 19));
        entrega(3, "DIA031", 3, "Cliente Tres", true, CAMBIO_ACEITE, 3, LocalDate.of(2026, 6, 20));

        List<RecordatorioMantenimiento> recordatorios = service.generarReporteRecordatorios(HOY);

        assertEquals(Arrays.asList("HOY000", "DIA030"), placas(recordatorios));
        assertEquals(0, recordatorios.get(0).getDiasRestantes());
        assertFalse(recordatorios.get(0).isVencido());
        assertEquals(30, recordatorios.get(1).getDiasRestantes());
    }

    @Test
    @DisplayName("Un servicio más reciente del mismo tipo y vehículo reemplaza al anterior")
    void entregaMasRecienteReemplazaLaAnterior() {
        entrega(1, "ABC123", 1, "María Fernanda Rojas", true, CAMBIO_ACEITE, 10, LocalDate.of(2025, 9, 1));
        entrega(1, "ABC123", 1, "María Fernanda Rojas", true, CAMBIO_ACEITE, 30, LocalDate.of(2026, 5, 29));
        entrega(1, "ABC123", 1, "María Fernanda Rojas", true, CAMBIO_ACEITE, 20, LocalDate.of(2026, 1, 15));
        // Vehículo 2: la entrega reciente saca al vehículo de la ventana aunque la anterior estuviera vencida.
        entrega(2, "DEF456", 2, "Carlos Andrés Peña", true, CAMBIO_ACEITE, 11, LocalDate.of(2025, 1, 10));
        entrega(2, "DEF456", 2, "Carlos Andrés Peña", true, CAMBIO_ACEITE, 40, LocalDate.of(2026, 11, 10));

        List<RecordatorioMantenimiento> recordatorios = service.generarReporteRecordatorios(HOY);

        assertEquals(1, recordatorios.size());
        RecordatorioMantenimiento recordatorio = recordatorios.get(0);
        assertEquals("ABC123", recordatorio.getPlaca());
        assertEquals(30, recordatorio.getOrdenId());
        assertEquals(LocalDate.of(2026, 5, 29), recordatorio.getFechaUltimaEntrega());
        assertEquals(LocalDate.of(2026, 11, 29), recordatorio.getFechaSugerida());
    }

    @Test
    @DisplayName("El mismo tipo de servicio en dos vehículos genera dos recordatorios independientes")
    void mismoTipoEnDosVehiculos() {
        entrega(7, "STU901", 6, "Transportes Rápidos S.A.S.", true, CAMBIO_ACEITE, 50, LocalDate.of(2026, 5, 25));
        entrega(6, "PQR678", 6, "Transportes Rápidos S.A.S.", true, CAMBIO_ACEITE, 51, LocalDate.of(2026, 5, 25));

        List<RecordatorioMantenimiento> recordatorios = service.generarReporteRecordatorios(HOY);

        assertEquals(Arrays.asList("PQR678", "STU901"), placas(recordatorios));
    }

    @Test
    @DisplayName("Un cliente inactivo no aparece en los recordatorios")
    void clienteInactivoNoAparece() {
        entrega(9, "INA999", 7, "Pablo Ortiz", false, CAMBIO_ACEITE, 60, LocalDate.of(2026, 5, 20));
        entrega(5, "MNO345", 5, "Sandra Milena Vega", true, CAMBIO_ACEITE, 5, LocalDate.of(2026, 5, 29));

        List<RecordatorioMantenimiento> recordatorios = service.generarReporteRecordatorios(HOY);

        assertEquals(Arrays.asList("MNO345"), placas(recordatorios));
    }

    @Test
    @DisplayName("Orden: vencidos del más atrasado al más reciente, luego próximos del más cercano; empate por placa")
    void ordenDelResultado() {
        entrega(1, "PRX020", 1, "Cliente", true, CAMBIO_ACEITE, 1, LocalDate.of(2026, 6, 9));    // en 20 días
        entrega(2, "VEN005", 2, "Cliente", true, CAMBIO_ACEITE, 2, LocalDate.of(2026, 5, 14));   // hace 5 días
        entrega(3, "PRX003", 3, "Cliente", true, CAMBIO_ACEITE, 3, LocalDate.of(2026, 5, 22));   // en 3 días
        entrega(4, "VEN030", 4, "Cliente", true, ALINEACION, 4, LocalDate.of(2025, 10, 20));    // hace 30 días
        entrega(5, "BBB111", 5, "Cliente", true, CAMBIO_ACEITE, 5, LocalDate.of(2026, 5, 29));   // en 10 días
        entrega(6, "AAA111", 6, "Cliente", true, CAMBIO_ACEITE, 6, LocalDate.of(2026, 5, 29));   // en 10 días

        List<RecordatorioMantenimiento> recordatorios = service.generarReporteRecordatorios(HOY);

        assertEquals(Arrays.asList("VEN030", "VEN005", "PRX003", "AAA111", "BBB111", "PRX020"),
                placas(recordatorios));
    }

    @Test
    @DisplayName("Sin recordatorios: lista vacía, tanto sin entregas como con entregas fuera de la ventana")
    void sinRecordatorios() {
        assertTrue(service.generarReporteRecordatorios(HOY).isEmpty());

        entrega(4, "JKL012", 4, "Jorge Eduardo Castillo", true, CAMBIO_ACEITE, 4, HOY);
        entrega(4, "JKL012", 4, "Jorge Eduardo Castillo", true, ALINEACION, 4, HOY);

        assertTrue(service.generarReporteRecordatorios(HOY).isEmpty());
    }

    @Test
    @DisplayName("Ignora registros sin intervalo y exige la fecha de referencia")
    void registrosSinIntervaloYFechaObligatoria() {
        reporteDAO.agregar(new RegistroMantenimientoPreventivo(1, "SIN000", "Marca", "Modelo", 1, "Cliente",
                null, "c@correo.com", true, 2, "Servicio sin intervalo", null, 1, HOY.minusYears(1).atStartOfDay()));

        assertTrue(service.generarReporteRecordatorios(HOY).isEmpty());
        assertThrows(ValidacionException.class, () -> service.generarReporteRecordatorios(null));
    }

    // ------------------------------------------------------------------
    // DAO en memoria
    // ------------------------------------------------------------------

    private static class ReporteDAOMock implements ReporteDAO {
        private final List<RegistroMantenimientoPreventivo> registros = new ArrayList<>();

        void agregar(RegistroMantenimientoPreventivo registro) {
            registros.add(registro);
        }

        @Override
        public List<RegistroConsumoRepuesto> listarConsumoRepuestos(LocalDate fechaDesde, LocalDate fechaHasta) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<RegistroServicioSolicitado> listarServiciosSolicitados(LocalDate fechaDesde, LocalDate fechaHasta) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<RegistroProductividadTecnico> listarProductividadTecnicos(LocalDate fechaDesde,
                                                                              LocalDate fechaHasta) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<RegistroMantenimientoPreventivo> listarMantenimientosPreventivos() {
            return new ArrayList<>(registros);
        }
    }
}
