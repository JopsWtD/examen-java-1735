package com.carmotors.sgt.service;

import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.CargaLineaTecnico;
import com.carmotors.sgt.model.ProductividadTecnico;
import com.carmotors.sgt.model.RegistroConsumoRepuesto;
import com.carmotors.sgt.model.RegistroMantenimientoPreventivo;
import com.carmotors.sgt.model.RegistroProductividadTecnico;
import com.carmotors.sgt.model.RegistroServicioSolicitado;
import com.carmotors.sgt.model.ReporteProductividadTecnicos;
import com.carmotors.sgt.model.Tecnico;
import com.carmotors.sgt.dao.ReporteDAO;
import com.carmotors.sgt.dao.TecnicoDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del reporte de productividad por técnico (T7.2, RF-26, §4.3) con DAOs en memoria.
 */
class ReporteProductividadTecnicosTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 1, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 12, 31);

    private ReporteDAOMock reporteDAO;
    private TecnicoDAOMock tecnicoDAO;
    private ReporteService service;

    @BeforeEach
    void setUp() {
        reporteDAO = new ReporteDAOMock();
        tecnicoDAO = new TecnicoDAOMock();
        service = new ReporteService(reporteDAO, tecnicoDAO);
    }

    private RegistroProductividadTecnico registro(int linea, String costo, Integer tecnicoId, String nombre,
                                                  boolean activo, String horas) {
        return new RegistroProductividadTecnico(linea, new BigDecimal(costo), tecnicoId, nombre, activo,
                horas == null ? null : new BigDecimal(horas));
    }

    private Tecnico tecnico(int id, String nombre, boolean activo) {
        return new Tecnico(id, nombre, "DOC-" + id, 1, null, activo);
    }

    private ReporteProductividadTecnicos generar() {
        return service.generarReporteProductividadTecnicos(DESDE, HASTA);
    }

    private ProductividadTecnico buscar(ReporteProductividadTecnicos reporte, int tecnicoId) {
        for (ProductividadTecnico tecnico : reporte.getTecnicos()) {
            if (tecnico.getTecnicoId() == tecnicoId) {
                return tecnico;
            }
        }
        return null;
    }

    private BigDecimal monto(String valor) {
        return new BigDecimal(valor);
    }

    private void assertTecnico(ProductividadTecnico tecnico, int lineas, String horas, String manoObra) {
        assertEquals(lineas, tecnico.getLineasCompletadas(), tecnico.getNombre());
        assertEquals(monto(horas), tecnico.getHorasTrabajadas(), tecnico.getNombre());
        assertEquals(monto(manoObra), tecnico.getManoObraAtribuida(), tecnico.getNombre());
    }

    @Test
    @DisplayName("Un técnico por línea recibe todo el costo de mano de obra")
    void unTecnicoPorLinea() {
        reporteDAO.agregar(registro(6, "150000.00", 2, "Juan Pérez", true, "3.00"));

        ReporteProductividadTecnicos reporte = generar();

        assertTecnico(buscar(reporte, 2), 1, "3.00", "150000.00");
        assertEquals(monto("0.00"), reporte.getManoObraSinDistribuir());
        assertEquals(monto("150000.00"), reporte.getManoObraTotal());
    }

    @Test
    @DisplayName("Varios técnicos reciben la mano de obra en proporción exacta a sus horas")
    void horasProporcionales() {
        reporteDAO.agregar(registro(1, "90000.00", 1, "Pedro Ramírez", true, "2.00"));
        reporteDAO.agregar(registro(1, "90000.00", 4, "Carlos Mendoza", true, "1.00"));

        ReporteProductividadTecnicos reporte = generar();

        assertTecnico(buscar(reporte, 1), 1, "2.00", "60000.00");
        assertTecnico(buscar(reporte, 4), 1, "1.00", "30000.00");
    }

    @Test
    @DisplayName("Centavos de redondeo: en empate de horas van al técnico de menor ID")
    void centavosAlMenorIdEnEmpate() {
        reporteDAO.agregar(registro(1, "100000.00", 3, "Laura Gómez", true, "1.00"));
        reporteDAO.agregar(registro(1, "100000.00", 1, "Pedro Ramírez", true, "1.00"));
        reporteDAO.agregar(registro(1, "100000.00", 2, "Juan Pérez", true, "1.00"));

        ReporteProductividadTecnicos reporte = generar();

        assertTecnico(buscar(reporte, 1), 1, "1.00", "33333.34");
        assertTecnico(buscar(reporte, 2), 1, "1.00", "33333.33");
        assertTecnico(buscar(reporte, 3), 1, "1.00", "33333.33");
        assertEquals(1, reporte.getTecnicos().get(0).getTecnicoId());
    }

    @Test
    @DisplayName("Centavos de redondeo: van al técnico con más horas aunque tenga mayor ID")
    void centavosAlDeMasHoras() {
        // 16666.67 + 16666.67 + 66666.67 = 100000.01 -> se descuenta el centavo al de 4 horas.
        reporteDAO.agregar(registro(1, "100000.00", 1, "Pedro Ramírez", true, "1.00"));
        reporteDAO.agregar(registro(1, "100000.00", 2, "Juan Pérez", true, "1.00"));
        reporteDAO.agregar(registro(1, "100000.00", 5, "Andrea Torres", true, "4.00"));

        ReporteProductividadTecnicos reporte = generar();

        assertTecnico(buscar(reporte, 1), 1, "1.00", "16666.67");
        assertTecnico(buscar(reporte, 2), 1, "1.00", "16666.67");
        assertTecnico(buscar(reporte, 5), 1, "4.00", "66666.66");
    }

    @Test
    @DisplayName("Una línea con 0 horas o sin técnicos va a 'Sin distribuir' y nadie la recibe")
    void lineaSinHorasVaASinDistribuir() {
        reporteDAO.agregar(registro(10, "90000.00", 1, "Pedro Ramírez", true, "0.00"));
        reporteDAO.agregar(registro(11, "50000.00", null, null, false, null));

        ReporteProductividadTecnicos reporte = generar();

        assertTecnico(buscar(reporte, 1), 1, "0.00", "0.00");
        assertEquals(monto("140000.00"), reporte.getManoObraSinDistribuir());
        assertEquals(monto("140000.00"), reporte.getManoObraTotal());
        assertEquals(2, reporte.getLineasCompletadas());
    }

    @Test
    @DisplayName("Activos sin trabajo aparecen en cero; inactivos sin trabajo no; inactivos con trabajo sí")
    void tecnicosActivosEInactivos() {
        tecnicoDAO.agregar(tecnico(1, "Pedro Ramírez", true));
        tecnicoDAO.agregar(tecnico(4, "Carlos Mendoza", true));
        tecnicoDAO.agregar(tecnico(6, "Luis Ortega", false));
        tecnicoDAO.agregar(tecnico(8, "Mario Ríos", false));
        reporteDAO.agregar(registro(3, "70000.00", 8, "Mario Ríos", false, "1.50"));

        ReporteProductividadTecnicos reporte = generar();

        assertEquals(3, reporte.getTecnicos().size());
        ProductividadTecnico inactivoConTrabajo = buscar(reporte, 8);
        assertFalse(inactivoConTrabajo.isActivo());
        assertTecnico(inactivoConTrabajo, 1, "1.50", "70000.00");
        assertTecnico(buscar(reporte, 1), 0, "0.00", "0.00");
        assertTecnico(buscar(reporte, 4), 0, "0.00", "0.00");
        assertNull(buscar(reporte, 6));
    }

    @Test
    @DisplayName("Ordena por mano de obra descendente, luego horas descendente y luego nombre ascendente")
    void ordenDelResultado() {
        tecnicoDAO.agregar(tecnico(20, "Bruno", true));
        tecnicoDAO.agregar(tecnico(21, "Andrés", true));
        reporteDAO.agregar(registro(1, "60000.00", 10, "Zoe", true, "3.00"));
        reporteDAO.agregar(registro(2, "60000.00", 11, "Ana", true, "3.00"));
        reporteDAO.agregar(registro(3, "60000.00", 12, "Beto", true, "5.00"));
        reporteDAO.agregar(registro(4, "90000.00", 13, "Carla", true, "1.00"));

        List<String> nombres = generar().getTecnicos().stream()
                .map(tecnico -> tecnico.getNombre())
                .collect(Collectors.toList());

        List<String> esperado = new ArrayList<>();
        esperado.add("Carla");
        esperado.add("Beto");
        esperado.add("Ana");
        esperado.add("Zoe");
        esperado.add("Andrés");
        esperado.add("Bruno");
        assertEquals(esperado, nombres);
    }

    @Test
    @DisplayName("Rechaza fechas vacías o invertidas sin consultar el DAO")
    void validarFechas() {
        assertThrows(ValidacionException.class, () -> service.generarReporteProductividadTecnicos(null, HASTA));
        assertThrows(ValidacionException.class, () -> service.generarReporteProductividadTecnicos(DESDE, null));
        assertThrows(ValidacionException.class,
                () -> service.generarReporteProductividadTecnicos(HASTA, DESDE));
        assertEquals(0, reporteDAO.consultas);
    }

    @Test
    @DisplayName("Datos de data.sql: resultado esperado y atribuido + sin distribuir = mano de obra total")
    void sumaAtribuidaMasSinDistribuirIgualTotal() {
        tecnicoDAO.agregar(tecnico(1, "Pedro Ramírez", true));
        tecnicoDAO.agregar(tecnico(2, "Juan Pérez", true));
        tecnicoDAO.agregar(tecnico(3, "Laura Gómez", true));
        tecnicoDAO.agregar(tecnico(4, "Carlos Mendoza", true));
        tecnicoDAO.agregar(tecnico(5, "Andrea Torres", true));
        tecnicoDAO.agregar(tecnico(6, "Luis Ortega", false));
        tecnicoDAO.agregar(tecnico(7, "Diego Suárez", true));
        // Líneas de las órdenes 3, 4 y 5 (Completada, Facturada y Entregada).
        reporteDAO.agregar(registro(4, "40000.00", 3, "Laura Gómez", true, "1.00"));
        reporteDAO.agregar(registro(5, "60000.00", 5, "Andrea Torres", true, "1.00"));
        reporteDAO.agregar(registro(6, "150000.00", 2, "Juan Pérez", true, "3.00"));
        reporteDAO.agregar(registro(7, "60000.00", 5, "Andrea Torres", true, "1.00"));
        // Línea extra con reparto con centavos y otra sin horas.
        reporteDAO.agregar(registro(20, "100000.00", 1, "Pedro Ramírez", true, "1.00"));
        reporteDAO.agregar(registro(20, "100000.00", 4, "Carlos Mendoza", true, "1.00"));
        reporteDAO.agregar(registro(20, "100000.00", 7, "Diego Suárez", true, "1.00"));
        reporteDAO.agregar(registro(21, "33333.33", 7, "Diego Suárez", true, "0.00"));

        ReporteProductividadTecnicos reporte = generar();

        assertTecnico(buscar(reporte, 2), 1, "3.00", "150000.00");
        assertTecnico(buscar(reporte, 5), 2, "2.00", "120000.00");
        assertTecnico(buscar(reporte, 3), 1, "1.00", "40000.00");
        assertTecnico(buscar(reporte, 1), 1, "1.00", "33333.34");
        assertTecnico(buscar(reporte, 7), 2, "1.00", "33333.33");
        assertNull(buscar(reporte, 6));
        assertEquals(monto("33333.33"), reporte.getManoObraSinDistribuir());
        assertEquals(6, reporte.getLineasCompletadas());

        BigDecimal atribuido = reporte.getTecnicos().stream()
                .map(tecnico -> tecnico.getManoObraAtribuida())
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        assertEquals(monto("443333.33"), reporte.getManoObraTotal());
        assertEquals(reporte.getManoObraTotal(), atribuido.add(reporte.getManoObraSinDistribuir()));
    }

    @Test
    @DisplayName("Sin órdenes completadas en el rango: técnicos activos en cero y totales en cero")
    void sinLineasEnElPeriodo() {
        tecnicoDAO.agregar(tecnico(1, "Pedro Ramírez", true));

        ReporteProductividadTecnicos reporte = generar();

        assertTrue(reporte.sinLineasEnPeriodo());
        assertEquals(1, reporte.getTecnicos().size());
        assertTecnico(reporte.getTecnicos().get(0), 0, "0.00", "0.00");
        assertEquals(monto("0.00"), reporte.getManoObraSinDistribuir());
        assertEquals(monto("0.00"), reporte.getManoObraTotal());
    }

    // ------------------------------------------------------------------
    // DAOs en memoria
    // ------------------------------------------------------------------

    private static class ReporteDAOMock implements ReporteDAO {
        private final List<RegistroProductividadTecnico> registros = new ArrayList<>();
        private int consultas = 0;

        void agregar(RegistroProductividadTecnico registro) {
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
            consultas++;
            assertEquals(DESDE, fechaDesde);
            assertEquals(HASTA, fechaHasta);
            return new ArrayList<>(registros);
        }

        @Override
        public List<RegistroMantenimientoPreventivo> listarMantenimientosPreventivos() {
            throw new UnsupportedOperationException();
        }
    }

    private static class TecnicoDAOMock implements TecnicoDAO {
        private final List<Tecnico> tecnicos = new ArrayList<>();

        void agregar(Tecnico tecnico) {
            tecnicos.add(tecnico);
        }

        @Override
        public List<Tecnico> listarTodos() {
            return new ArrayList<>(tecnicos);
        }

        @Override
        public List<Tecnico> listarActivos() {
            return tecnicos.stream()
                    .filter(tecnico -> tecnico.isActivo())
                    .collect(Collectors.toList());
        }

        @Override
        public Tecnico buscarPorId(int id) {
            for (Tecnico tecnico : tecnicos) {
                if (tecnico.getId() == id) {
                    return tecnico;
                }
            }
            return null;
        }

        @Override
        public Tecnico buscarPorIdParaActualizar(int id) {
            return buscarPorId(id);
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
