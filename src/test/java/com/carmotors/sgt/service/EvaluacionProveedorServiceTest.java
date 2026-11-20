package com.carmotors.sgt.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.EvaluacionProveedor;
import com.carmotors.sgt.model.Proveedor;
import com.carmotors.sgt.model.RankingProveedor;
import com.carmotors.sgt.dao.ProveedorDAO;
import com.carmotors.sgt.dao.EvaluacionProveedorDAO;

class EvaluacionProveedorServiceTest {

    private EvaluacionProveedorService service;
    private ProveedorDAOMock proveedorDAO;
    private EvaluacionProveedorDAOMock evaluacionDAO;

    @BeforeEach
    void setUp() {
        proveedorDAO = new ProveedorDAOMock();
        proveedorDAO.agregar(new Proveedor(1, "AutoPartes del Norte", "5775550101", "ventas@autopartesnorte.com", "Calle 1", true));
        proveedorDAO.agregar(new Proveedor(2, "Frenos y Más", "5775550102", "pedidos@frenosymas.com", "Calle 2", false));
        evaluacionDAO = new EvaluacionProveedorDAOMock();
        service = new EvaluacionProveedorService(evaluacionDAO, proveedorDAO);
    }

    @Test
    @DisplayName("Debe registrar una evaluación válida con fecha actual y texto opcional")
    void registrarEvaluacionValida() {
        EvaluacionProveedor evaluacion = new EvaluacionProveedor();
        evaluacion.setProveedorId(1);
        evaluacion.setCalidad(5);
        evaluacion.setPuntualidad(4);
        evaluacion.setComentario("  Buena atención y entrega a tiempo  ");

        EvaluacionProveedor guardada = service.registrarEvaluacion(evaluacion);

        assertEquals(1, guardada.getId());
        assertEquals(1, guardada.getProveedorId());
        assertEquals(LocalDate.now(), guardada.getFecha());
        assertEquals("Buena atención y entrega a tiempo", guardada.getComentario());
    }

    @Test
    @DisplayName("Debe rechazar calidad y puntualidad fuera del rango 1 a 5")
    void rechazarCalificacionesFueraDeRango() {
        EvaluacionProveedor baja = new EvaluacionProveedor();
        baja.setProveedorId(1);
        baja.setCalidad(0);
        baja.setPuntualidad(3);
        assertThrows(ValidacionException.class, () -> service.registrarEvaluacion(baja));

        EvaluacionProveedor alta = new EvaluacionProveedor();
        alta.setProveedorId(1);
        alta.setCalidad(3);
        alta.setPuntualidad(6);
        assertThrows(ValidacionException.class, () -> service.registrarEvaluacion(alta));
    }

    @Test
    @DisplayName("Debe rechazar proveedores inexistentes e inactivos")
    void rechazarProveedorInvalido() {
        EvaluacionProveedor inexistente = new EvaluacionProveedor();
        inexistente.setProveedorId(999);
        inexistente.setCalidad(4);
        inexistente.setPuntualidad(4);
        assertThrows(EntidadNoEncontradaException.class, () -> service.registrarEvaluacion(inexistente));

        EvaluacionProveedor inactivo = new EvaluacionProveedor();
        inactivo.setProveedorId(2);
        inactivo.setCalidad(4);
        inactivo.setPuntualidad(4);
        assertThrows(ValidacionException.class, () -> service.registrarEvaluacion(inactivo));
    }

    @Test
    @DisplayName("Debe listar el historial de una evaluación por proveedor")
    void listarHistorialPorProveedor() {
        EvaluacionProveedor primera = new EvaluacionProveedor();
        primera.setProveedorId(1);
        primera.setCalidad(4);
        primera.setPuntualidad(5);
        primera.setFecha(LocalDate.of(2026, 6, 20));
        service.registrarEvaluacion(primera);

        EvaluacionProveedor segunda = new EvaluacionProveedor();
        segunda.setProveedorId(1);
        segunda.setCalidad(5);
        segunda.setPuntualidad(4);
        service.registrarEvaluacion(segunda);

        List<EvaluacionProveedor> historial = service.listarPorProveedor(1);
        assertEquals(2, historial.size());
        assertTrue(historial.get(0).getFecha().compareTo(historial.get(1).getFecha()) >= 0);
    }

    @Test
    @DisplayName("Debe rechazar comentarios que superen los 255 caracteres")
    void rechazarComentarioMayorA255Caracteres() {
        StringBuilder textoLargo = new StringBuilder();
        for (int i = 0; i < 256; i++) {
            textoLargo.append('a');
        }

        EvaluacionProveedor evaluacion = new EvaluacionProveedor();
        evaluacion.setProveedorId(1);
        evaluacion.setCalidad(5);
        evaluacion.setPuntualidad(5);
        evaluacion.setComentario(textoLargo.toString());

        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> service.registrarEvaluacion(evaluacion));

        assertTrue(ex.getMessage().contains("255"));
    }

    @Test
    @DisplayName("Ranking: promedio del proveedor con un decimal y HALF_UP")
    void rankingRedondeaPromedioAUnDecimal() {
        evaluacionDAO.guardar(nuevaEvaluacion(1, 5, 4)); // 4.5
        evaluacionDAO.guardar(nuevaEvaluacion(1, 4, 4)); // 4.0 -> (4.5 + 4.0) / 2 = 4.25 -> 4.3

        List<RankingProveedor> ranking = service.generarRanking();

        RankingProveedor fila = ranking.get(0);
        assertEquals("AutoPartes del Norte", fila.getProveedor().getNombre());
        assertEquals(new BigDecimal("4.3"), fila.getPromedio());
        assertEquals(2, fila.getCantidadEvaluaciones());
    }

    @Test
    @DisplayName("Ranking: ordena por promedio de mayor a menor")
    void rankingOrdenaPorPromedioDescendente() {
        proveedorDAO.agregar(new Proveedor(3, "Lubricantes Andinos", "5775550103", "c@l.com", "Calle 3", true));
        evaluacionDAO.guardar(nuevaEvaluacion(1, 3, 3)); // 3.0
        evaluacionDAO.guardar(nuevaEvaluacion(2, 5, 5)); // 5.0
        evaluacionDAO.guardar(nuevaEvaluacion(3, 4, 5)); // 4.5

        List<RankingProveedor> ranking = service.generarRanking();

        assertEquals(3, ranking.size());
        assertEquals("Frenos y Más", ranking.get(0).getProveedor().getNombre());
        assertEquals(new BigDecimal("5.0"), ranking.get(0).getPromedio());
        assertEquals("Lubricantes Andinos", ranking.get(1).getProveedor().getNombre());
        assertEquals(new BigDecimal("4.5"), ranking.get(1).getPromedio());
        assertEquals("AutoPartes del Norte", ranking.get(2).getProveedor().getNombre());
        assertEquals(new BigDecimal("3.0"), ranking.get(2).getPromedio());
    }

    @Test
    @DisplayName("Ranking: en empate de promedio ordena por nombre ascendente")
    void rankingDesempataPorNombre() {
        proveedorDAO.agregar(new Proveedor(3, "Lubricantes Andinos", "5775550103", "c@l.com", "Calle 3", true));
        proveedorDAO.agregar(new Proveedor(4, "ElectroCar Colombia", "6015550104", "i@e.co", "Calle 4", true));
        evaluacionDAO.guardar(nuevaEvaluacion(3, 4, 3)); // 3.5
        evaluacionDAO.guardar(nuevaEvaluacion(4, 3, 4)); // 3.5

        List<RankingProveedor> ranking = service.generarRanking();

        assertEquals("ElectroCar Colombia", ranking.get(0).getProveedor().getNombre());
        assertEquals("Lubricantes Andinos", ranking.get(1).getProveedor().getNombre());
        assertEquals(ranking.get(0).getPromedio(), ranking.get(1).getPromedio());
    }

    @Test
    @DisplayName("Ranking: proveedores sin evaluar al final, ordenados por nombre")
    void rankingProveedoresSinEvaluarAlFinal() {
        proveedorDAO.agregar(new Proveedor(3, "Importadora Sur", "6025550105", null, "Calle 9", true));
        proveedorDAO.agregar(new Proveedor(4, "Baterías Express", "6015550199", "b@e.co", "Calle 7", true));
        evaluacionDAO.guardar(nuevaEvaluacion(1, 1, 2)); // 1.5, el evaluado más bajo

        List<RankingProveedor> ranking = service.generarRanking();

        assertEquals(4, ranking.size());
        assertEquals("AutoPartes del Norte", ranking.get(0).getProveedor().getNombre());
        assertEquals(new BigDecimal("1.5"), ranking.get(0).getPromedio());
        assertEquals("Baterías Express", ranking.get(1).getProveedor().getNombre());
        assertEquals("Frenos y Más", ranking.get(2).getProveedor().getNombre());
        assertEquals("Importadora Sur", ranking.get(3).getProveedor().getNombre());
        for (int i = 1; i < ranking.size(); i++) {
            assertNull(ranking.get(i).getPromedio());
            assertEquals(0, ranking.get(i).getCantidadEvaluaciones());
        }
    }

    @Test
    @DisplayName("Ranking: si nadie tiene evaluaciones, todos aparecen sin evaluar por nombre")
    void rankingTodosSinEvaluar() {
        List<RankingProveedor> ranking = service.generarRanking();

        assertEquals(2, ranking.size());
        assertEquals("AutoPartes del Norte", ranking.get(0).getProveedor().getNombre());
        assertEquals("Frenos y Más", ranking.get(1).getProveedor().getNombre());
        assertNull(ranking.get(0).getPromedio());
        assertNull(ranking.get(1).getPromedio());
    }

    @Test
    @DisplayName("Ranking: sin proveedores registrados devuelve una lista vacía")
    void rankingSinProveedores() {
        EvaluacionProveedorService servicioVacio =
                new EvaluacionProveedorService(new EvaluacionProveedorDAOMock(), new ProveedorDAOMock());

        List<RankingProveedor> ranking = servicioVacio.generarRanking();

        assertNotNull(ranking);
        assertTrue(ranking.isEmpty());
    }

    private EvaluacionProveedor nuevaEvaluacion(int proveedorId, int calidad, int puntualidad) {
        EvaluacionProveedor evaluacion = new EvaluacionProveedor();
        evaluacion.setProveedorId(proveedorId);
        evaluacion.setCalidad(calidad);
        evaluacion.setPuntualidad(puntualidad);
        return evaluacion;
    }

    private static class ProveedorDAOMock implements ProveedorDAO {
        private final List<Proveedor> datos = new ArrayList<>();

        void agregar(Proveedor proveedor) {
            datos.add(proveedor);
        }

        @Override
        public List<Proveedor> listarTodos() { return new ArrayList<>(datos); }

        @Override
        public List<Proveedor> listarActivos() {
            List<Proveedor> activos = new ArrayList<>();
            for (Proveedor proveedor : datos) {
                if (proveedor.isActivo()) {
                    activos.add(proveedor);
                }
            }
            return activos;
        }

        @Override
        public Proveedor buscarPorId(int id) {
            for (Proveedor proveedor : datos) {
                if (proveedor.getId() == id) {
                    return proveedor;
                }
            }
            return null;
        }

        @Override
        public Proveedor buscarPorEmail(String email) { return null; }

        @Override
        public int guardar(Proveedor proveedor) { return 0; }

        @Override
        public boolean actualizar(Proveedor proveedor) { return false; }

        @Override
        public boolean desactivar(int id) { return false; }

        @Override
        public boolean reactivar(int id) { return false; }
    }

    private static class EvaluacionProveedorDAOMock implements EvaluacionProveedorDAO {
        private final List<EvaluacionProveedor> datos = new ArrayList<>();
        private int secuencia = 1;

        @Override
        public List<EvaluacionProveedor> listarTodos() {
            return new ArrayList<>(datos);
        }

        @Override
        public List<EvaluacionProveedor> listarPorProveedor(int proveedorId) {
            List<EvaluacionProveedor> resultado = new ArrayList<>();
            for (EvaluacionProveedor item : datos) {
                if (item.getProveedorId() == proveedorId) {
                    resultado.add(item);
                }
            }
            resultado.sort((a, b) -> b.getFecha().compareTo(a.getFecha()));
            return resultado;
        }

        @Override
        public int guardar(EvaluacionProveedor evaluacion) {
            int id = secuencia++;
            EvaluacionProveedor copia = new EvaluacionProveedor();
            copia.setId(id);
            copia.setProveedorId(evaluacion.getProveedorId());
            copia.setFecha(evaluacion.getFecha());
            copia.setCalidad(evaluacion.getCalidad());
            copia.setPuntualidad(evaluacion.getPuntualidad());
            copia.setComentario(evaluacion.getComentario());
            datos.add(copia);
            return id;
        }

        @Override
        public EvaluacionProveedor buscarPorId(int id) { return null; }
    }
}
