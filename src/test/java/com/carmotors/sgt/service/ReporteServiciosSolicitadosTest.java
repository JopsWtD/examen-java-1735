package com.carmotors.sgt.service;

import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.RegistroConsumoRepuesto;
import com.carmotors.sgt.model.RegistroMantenimientoPreventivo;
import com.carmotors.sgt.model.RegistroProductividadTecnico;
import com.carmotors.sgt.model.RegistroServicioSolicitado;
import com.carmotors.sgt.model.ResumenServicioSolicitado;
import com.carmotors.sgt.dao.ReporteDAO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReporteServiciosSolicitadosTest {
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 12);

    @Test
    void agregaRepuestosHistoricosYCuentaCadaLineaUnaSolaVez() {
        ReporteService servicio = servicioCon(List.of(
                registro(1, 2, 3, FECHA.atStartOfDay(), "10.00", 2, "3.35"),
                registro(1, 2, 3, FECHA.atStartOfDay(), "10.00", 1, "1.30"),
                registro(2, 2, 4, FECHA.atTime(23, 59, 59), "5.55", 0, null),
                registro(3, 1, 5, FECHA.atTime(9, 0), "100.00", 0, null)));

        List<ResumenServicioSolicitado> resumen = servicio.generarReporteServiciosSolicitados(FECHA, FECHA);
        assertEquals(2, resumen.size());
        assertEquals(2, resumen.get(0).getServicioTipoId());
        assertEquals(2, resumen.get(0).getCantidadLineas());
        assertEquals(new BigDecimal("23.55"), resumen.get(0).getIngresos());
        assertEquals(1, resumen.get(1).getServicioTipoId());
        assertEquals(1, resumen.get(1).getCantidadLineas());
        assertEquals(new BigDecimal("100.00"), resumen.get(1).getIngresos());
    }

    @Test
    void excluyeEstadosYFechasQueNoCumplenElRangoDeFinalizacion() {
        ReporteService servicio = servicioCon(List.of(
                registro(1, 1, 1, FECHA.atStartOfDay(), "9.00", 0, null),
                registro(2, 1, 2, FECHA.atStartOfDay(), "9.00", 0, null),
                registro(3, 1, 6, FECHA.atStartOfDay(), "9.00", 0, null),
                registro(4, 1, 3, FECHA.minusDays(1).atTime(23, 59, 59), "9.00", 0, null),
                registro(5, 1, 4, FECHA.plusDays(1).atStartOfDay(), "9.00", 0, null),
                registro(6, 1, 5, null, "9.00", 0, null)));

        assertTrue(servicio.generarReporteServiciosSolicitados(FECHA, FECHA).isEmpty());
    }

    @Test
    void desempataPorIdDeTipoAunqueTenganElMismoNombre() {
        ReporteService servicio = servicioCon(List.of(
                registro(1, 9, 3, FECHA.atStartOfDay(), "9.00", 0, null),
                registro(2, 1, 3, FECHA.atStartOfDay(), "8.00", 0, null)));

        List<ResumenServicioSolicitado> resumen = servicio.generarReporteServiciosSolicitados(FECHA, FECHA);
        assertEquals(2, resumen.size());
        assertEquals(1, resumen.get(0).getServicioTipoId());
        assertEquals(9, resumen.get(1).getServicioTipoId());
    }

    @Test
    void requiereAmbasFechasYRechazaRangosInvertidos() {
        ReporteService servicio = servicioCon(List.of());
        assertThrows(ValidacionException.class, () -> servicio.generarReporteServiciosSolicitados(null, FECHA));
        assertThrows(ValidacionException.class, () -> servicio.generarReporteServiciosSolicitados(FECHA, null));
        assertThrows(ValidacionException.class,
                () -> servicio.generarReporteServiciosSolicitados(FECHA.plusDays(1), FECHA));
    }

    private RegistroServicioSolicitado registro(int linea, int tipo, int estado, LocalDateTime fecha,
                                                 String manoDeObra, int cantidad, String precio) {
        return new RegistroServicioSolicitado(linea, tipo, "Servicio", estado, fecha, new BigDecimal(manoDeObra),
                cantidad, precio == null ? null : new BigDecimal(precio));
    }

    private ReporteService servicioCon(List<RegistroServicioSolicitado> registros) {
        return new ReporteService(new ReporteDAO() {
            @Override
            public List<RegistroConsumoRepuesto> listarConsumoRepuestos(LocalDate desde, LocalDate hasta) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<RegistroServicioSolicitado> listarServiciosSolicitados(LocalDate desde, LocalDate hasta) {
                assertEquals(FECHA, desde);
                assertEquals(FECHA, hasta);
                return registros;
            }

            @Override
            public List<RegistroProductividadTecnico> listarProductividadTecnicos(LocalDate desde, LocalDate hasta) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<RegistroMantenimientoPreventivo> listarMantenimientosPreventivos() {
                throw new UnsupportedOperationException();
            }
        });
    }
}
