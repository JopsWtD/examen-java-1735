package com.carmotors.sgt.service.chain;

import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.CargaLineaTecnico;
import com.carmotors.sgt.model.Especialidad;
import com.carmotors.sgt.model.Tecnico;
import com.carmotors.sgt.dao.EspecialidadDAO;
import com.carmotors.sgt.dao.TecnicoDAO;
import com.carmotors.sgt.service.TecnicoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SugerenciaTecnicoChainTest {

    private static final int ESP_MOTOR = 2;
    private static final int ESP_FRENOS = 3;
    private static final int ESP_ELECTRICA = 4;

    private SugerenciaTecnicoChain chain;

    @BeforeEach
    void setUp() {
        chain = new SugerenciaTecnicoChain();
    }

    @Test
    @DisplayName("Eslabón 1: debe elegir al especialista libre con menor ID")
    void debeElegirEspecialistaLibreConMenorId() {
        List<Tecnico> tecnicos = Arrays.asList(
                tecnico(10, "Carlos", ESP_MOTOR, true),
                tecnico(5, "Ana", ESP_MOTOR, true),
                tecnico(2, "Luis", ESP_FRENOS, true)
        );
        Map<Integer, Integer> cargas = new HashMap<>();
        cargas.put(10, 0);
        cargas.put(5, 0);
        cargas.put(2, 0);

        SugerenciaTecnico sugerencia = chain.sugerir(ESP_MOTOR, tecnicos, cargas);

        assertNotNull(sugerencia);
        assertEquals(5, sugerencia.getTecnico().getId());
        assertEquals(0, sugerencia.getCargaActual());
        assertTrue(sugerencia.isEspecialidadCoincide());
        assertEquals("Especialista libre", sugerencia.getCriterioAplicado());
    }

    @Test
    @DisplayName("Eslabón 2: debe elegir especialista con menor carga (>0) sin bajar al eslabón 3")
    void debeElegirEspecialistaMenorCargaYDetenerCadena() {
        List<Tecnico> tecnicos = Arrays.asList(
                tecnico(12, "Pedro", ESP_MOTOR, true),
                tecnico(7, "Marta", ESP_MOTOR, true),
                tecnico(3, "Jorge", ESP_MOTOR, true),
                tecnico(1, "General Libre", ESP_FRENOS, true)
        );
        Map<Integer, Integer> cargas = new HashMap<>();
        cargas.put(12, 2);
        cargas.put(7, 2);
        cargas.put(3, 4);
        cargas.put(1, 0);

        SugerenciaTecnico sugerencia = chain.sugerir(ESP_MOTOR, tecnicos, cargas);

        assertNotNull(sugerencia);
        assertEquals(7, sugerencia.getTecnico().getId());
        assertEquals(2, sugerencia.getCargaActual());
        assertTrue(sugerencia.isEspecialidadCoincide());
        assertEquals("Especialista con menor carga", sugerencia.getCriterioAplicado());
    }

    @Test
    @DisplayName("Eslabón 3: debe elegir técnico de otra especialidad con menor carga y marcar no coincidencia")
    void debeElegirGeneralConMenorCargaCuandoNoHayEspecialistas() {
        List<Tecnico> tecnicos = Arrays.asList(
                tecnico(15, "Sofía", ESP_FRENOS, true),
                tecnico(8, "Mateo", ESP_ELECTRICA, true),
                tecnico(4, "Elena", ESP_FRENOS, true)
        );
        Map<Integer, Integer> cargas = new HashMap<>();
        cargas.put(15, 1);
        cargas.put(8, 1);
        cargas.put(4, 3);

        SugerenciaTecnico sugerencia = chain.sugerir(ESP_MOTOR, tecnicos, cargas);

        assertNotNull(sugerencia);
        assertEquals(8, sugerencia.getTecnico().getId());
        assertEquals(1, sugerencia.getCargaActual());
        assertFalse(sugerencia.isEspecialidadCoincide());
        assertEquals("Técnico de otra especialidad con menor carga", sugerencia.getCriterioAplicado());
    }

    @Test
    @DisplayName("Debe excluir estrictamente técnicos inactivos aunque tengan carga 0 y coincida la especialidad")
    void debeExcluirTecnicosInactivosEnTodosLosEslabones() {
        List<Tecnico> tecnicos = Arrays.asList(
                null,
                tecnico(1, "Inactivo Especialista Libre", ESP_MOTOR, false),
                tecnico(2, "Inactivo General Libre", ESP_FRENOS, false),
                tecnico(9, "Activo General Ocupado", ESP_ELECTRICA, true)
        );
        Map<Integer, Integer> cargas = new HashMap<>();
        cargas.put(1, 0);
        cargas.put(2, 0);
        cargas.put(9, 2);

        SugerenciaTecnico sugerencia = chain.sugerir(ESP_MOTOR, tecnicos, cargas);

        assertNotNull(sugerencia);
        assertEquals(9, sugerencia.getTecnico().getId());
        assertEquals(2, sugerencia.getCargaActual());
        assertFalse(sugerencia.isEspecialidadCoincide());
    }

    @Test
    @DisplayName("Debe retornar null controlado cuando la lista es nula, vacía o solo tiene inactivos")
    void debeRetornarNullSiNoHayTecnicosActivos() {
        assertNull(chain.sugerir(ESP_MOTOR, null, null));
        assertNull(chain.sugerir(ESP_MOTOR, Collections.emptyList(), new HashMap<>()));

        List<Tecnico> soloInactivos = Arrays.asList(
                tecnico(1, "Inactivo 1", ESP_MOTOR, false),
                tecnico(2, "Inactivo 2", ESP_FRENOS, false)
        );
        assertNull(chain.sugerir(ESP_MOTOR, soloInactivos, Collections.emptyMap()));
    }

    @Test
    @DisplayName("Debe asumir carga 0 cuando el mapa de cargas es nulo o no contiene al técnico")
    void debeAsumirCargaCeroSiMapaEsNuloOSinEntrada() {
        List<Tecnico> tecnicos = Arrays.asList(
                tecnico(6, "Especialista Sin Registro", ESP_MOTOR, true)
        );

        SugerenciaTecnico sugerencia = chain.sugerir(ESP_MOTOR, tecnicos, null);

        assertNotNull(sugerencia);
        assertEquals(6, sugerencia.getTecnico().getId());
        assertEquals(0, sugerencia.getCargaActual());
        assertTrue(sugerencia.isEspecialidadCoincide());
        assertEquals("Especialista libre", sugerencia.getCriterioAplicado());
    }

    @Test
    @DisplayName("Debe lanzar ValidacionException si especialidadRequeridaId es menor o igual a cero")
    void debeValidarEspecialidadRequeridaPositiva() {
        List<Tecnico> tecnicos = Collections.singletonList(tecnico(1, "Ana", ESP_MOTOR, true));

        assertThrows(ValidacionException.class,
                () -> chain.sugerir(0, tecnicos, Collections.emptyMap()));
        assertThrows(ValidacionException.class,
                () -> chain.sugerir(-3, tecnicos, Collections.emptyMap()));
    }

    @Test
    @DisplayName("Debe integrar TecnicoService consultando técnicos activos y cargas reales")
    void debeSugerirIntegrandoTecnicoService() {
        List<Tecnico> tecnicosActivos = Arrays.asList(
                tecnico(10, "Bruno", ESP_FRENOS, true),
                tecnico(2, "Carlos", ESP_MOTOR, true),
                tecnico(5, "Diana", ESP_MOTOR, true)
        );
        Map<Integer, Integer> cargas = new HashMap<>();
        cargas.put(2, 2);
        cargas.put(5, 1);

        TecnicoService tecnicoService = crearTecnicoServiceEnMemoria(tecnicosActivos, cargas);
        SugerenciaTecnico resultado = chain.sugerir(ESP_MOTOR, tecnicoService);

        assertNotNull(resultado);
        assertEquals(5, resultado.getTecnico().getId());
        assertEquals(1, resultado.getCargaActual());
        assertTrue(resultado.isEspecialidadCoincide());
        assertEquals("Especialista con menor carga", resultado.getCriterioAplicado());
    }

    @Test
    @DisplayName("Debe lanzar ValidacionException si TecnicoService es nulo")
    void debeRechazarTecnicoServiceNulo() {
        assertThrows(ValidacionException.class,
                () -> chain.sugerir(ESP_MOTOR, (TecnicoService) null));
    }

    private TecnicoService crearTecnicoServiceEnMemoria(
            List<Tecnico> activos,
            Map<Integer, Integer> cargas) {
        TecnicoDAO tecnicoDAO = new TecnicoDAO() {
            @Override
            public List<Tecnico> listarTodos() {
                return activos;
            }

            @Override
            public List<Tecnico> listarActivos() {
                return activos;
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
                return Collections.emptyList();
            }

            @Override
            public Map<Integer, Integer> contarCargaActivaPorTecnico() {
                return cargas;
            }
        };

        EspecialidadDAO especialidadDAO = new EspecialidadDAO() {
            @Override
            public List<Especialidad> listarTodos() {
                return Collections.emptyList();
            }

            @Override
            public Especialidad buscarPorId(int id) {
                return null;
            }
        };

        return new TecnicoService(tecnicoDAO, especialidadDAO);
    }

    private Tecnico tecnico(int id, String nombre, int especialidadId, boolean activo) {
        return new Tecnico(id, nombre, "DOC-" + id, especialidadId, "300000000" + id, activo);
    }
}
