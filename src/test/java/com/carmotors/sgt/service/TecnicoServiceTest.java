package com.carmotors.sgt.service;

import com.carmotors.sgt.exception.DatoDuplicadoException;
import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.CargaLineaTecnico;
import com.carmotors.sgt.model.Especialidad;
import com.carmotors.sgt.model.Tecnico;
import com.carmotors.sgt.dao.EspecialidadDAO;
import com.carmotors.sgt.dao.TecnicoDAO;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias en memoria para {@link TecnicoService} (RF-33, RN-17).
 */
class TecnicoServiceTest {

    private TecnicoDAOEnMemoria tecnicoRepository;
    private EspecialidadDAOEnMemoria especialidadRepository;
    private TecnicoService tecnicoService;

    @BeforeEach
    void configurar() {
        tecnicoRepository = new TecnicoDAOEnMemoria();
        especialidadRepository = new EspecialidadDAOEnMemoria();
        especialidadRepository.agregar(new Especialidad(1, "Mecánica general"));
        especialidadRepository.agregar(new Especialidad(3, "Frenos"));
        tecnicoService = new TecnicoService(tecnicoRepository, especialidadRepository);
    }

    @Test
    @DisplayName("Debe registrar un técnico válido con especialidad existente y activo en true")
    void registrarTecnicoValido() {
        Tecnico nuevo = new Tecnico(0, "  Pedro Ramírez  ", " 1090456123 ", 3, " 3205550001 ", true);
        Tecnico guardado = tecnicoService.registrarTecnico(nuevo);

        assertEquals(1, guardado.getId());
        assertEquals("Pedro Ramírez", guardado.getNombre());
        assertEquals("1090456123", guardado.getDocumentoIdentidad());
        assertEquals(3, guardado.getEspecialidadId());
        assertEquals("3205550001", guardado.getTelefono());
        assertTrue(guardado.isActivo());
    }

    @Test
    @DisplayName("Debe rechazar documento duplicado con DatoDuplicadoException")
    void rechazarDocumentoDuplicado() {
        tecnicoService.registrarTecnico(new Tecnico(0, "Pedro Ramírez", "1090456123", 3, null, true));

        Tecnico repetido = new Tecnico(0, "Otro Técnico", "1090456123", 1, "3001112233", true);
        assertThrows(DatoDuplicadoException.class, () -> tecnicoService.registrarTecnico(repetido));
    }

    @Test
    @DisplayName("Debe rechazar especialidad inexistente con EntidadNoEncontradaException")
    void rechazarEspecialidadInexistente() {
        Tecnico invalido = new Tecnico(0, "Juan Pérez", "1090456124", 99, null, true);
        assertThrows(EntidadNoEncontradaException.class, () -> tecnicoService.registrarTecnico(invalido));
    }

    @Test
    @DisplayName("Debe rechazar nombre o documento vacío con ValidacionException")
    void rechazarCamposObligatoriosVacios() {
        Tecnico sinNombre = new Tecnico(0, "   ", "1090456124", 1, null, true);
        assertThrows(ValidacionException.class, () -> tecnicoService.registrarTecnico(sinNombre));

        Tecnico sinDocumento = new Tecnico(0, "Juan Pérez", " ", 1, null, true);
        assertThrows(ValidacionException.class, () -> tecnicoService.registrarTecnico(sinDocumento));
    }

    @Test
    @DisplayName("Debe actualizar datos de un técnico existente")
    void actualizarTecnicoExistente() {
        Tecnico creado = tecnicoService.registrarTecnico(new Tecnico(0, "Laura Gómez", "1090456125", 1, null, true));
        creado.setNombre("Laura Gómez Updated");
        creado.setEspecialidadId(3);
        creado.setTelefono("3109998877");

        Tecnico actualizado = tecnicoService.actualizarTecnico(creado);
        assertEquals("Laura Gómez Updated", actualizado.getNombre());
        assertEquals(3, actualizado.getEspecialidadId());
        assertEquals("3109998877", actualizado.getTelefono());
    }

    @Test
    @DisplayName("Debe desactivar lógicamente sin eliminar el registro ni borrarlo de listarTodos")
    void desactivarYReactivarTecnicoLogicamente() {
        Tecnico creado = tecnicoService.registrarTecnico(new Tecnico(0, "Luis Ortega", "1090456128", 1, null, true));

        Tecnico desactivado = tecnicoService.desactivarTecnico(creado.getId());
        assertFalse(desactivado.isActivo());
        assertEquals(0, tecnicoService.listarActivos().size());
        assertEquals(1, tecnicoService.listarTodos().size());

        Tecnico reactivado = tecnicoService.reactivarTecnico(creado.getId());
        assertTrue(reactivado.isActivo());
        assertEquals(1, tecnicoService.listarActivos().size());
    }

    @Test
    @DisplayName("Debe lanzar EntidadNoEncontradaException al buscar un ID inexistente")
    void buscarIdInexistenteLanzaExcepcion() {
        assertThrows(EntidadNoEncontradaException.class, () -> tecnicoService.buscarPorId(999));
    }

    @Test
    @DisplayName("Debe contar y listar en orden las líneas en Pendiente y En Proceso de un técnico")
    void consultarYContarCargaPendienteYEnProceso() {
        Tecnico pedro = tecnicoService.registrarTecnico(new Tecnico(0, "Pedro Ramírez", "1090456123", 3, null, true));

        tecnicoRepository.agregarAsignacion(pedro.getId(), new CargaLineaTecnico(
                5, 12, "DEF456", "Cambio de líquido de frenos",
                new EstadoEnProceso().getEstadoId(), "En Proceso", new BigDecimal("1.50")
        ));
        tecnicoRepository.agregarAsignacion(pedro.getId(), new CargaLineaTecnico(
                2, 4, "ABC123", "Revisión y cambio de pastillas de freno",
                new EstadoPendiente().getEstadoId(), "Pendiente", new BigDecimal("2.00")
        ));
        tecnicoRepository.agregarAsignacion(pedro.getId(), new CargaLineaTecnico(
                2, 3, "ABC123", "Alineación y balanceo",
                new EstadoPendiente().getEstadoId(), "Pendiente", new BigDecimal("1.00")
        ));

        assertEquals(3, tecnicoService.obtenerCargaTrabajo(pedro.getId()));
        List<CargaLineaTecnico> lineas = tecnicoService.consultarCargaTrabajo(pedro.getId());
        assertEquals(3, lineas.size());
        assertEquals(2, lineas.get(0).getOrdenId());
        assertEquals(3, lineas.get(0).getDetalleId());
        assertEquals(2, lineas.get(1).getOrdenId());
        assertEquals(4, lineas.get(1).getDetalleId());
        assertEquals(5, lineas.get(2).getOrdenId());
        assertEquals(12, lineas.get(2).getDetalleId());
    }

    @Test
    @DisplayName("Debe excluir órdenes Completado, Facturado, Entregado y Cancelada del cálculo de carga")
    void excluirOrdenesNoActivasDeCarga() {
        Tecnico pedro = tecnicoService.registrarTecnico(new Tecnico(0, "Pedro Ramírez", "1090456123", 3, null, true));

        tecnicoRepository.agregarAsignacion(pedro.getId(), new CargaLineaTecnico(
                1, 1, "ABC123", "Frenos", new EstadoPendiente().getEstadoId(), "Pendiente", new BigDecimal("1.00")
        ));
        tecnicoRepository.agregarAsignacion(pedro.getId(), new CargaLineaTecnico(
                2, 2, "DEF456", "Motor", new EstadoCompletado().getEstadoId(), "Completado", new BigDecimal("2.00")
        ));
        tecnicoRepository.agregarAsignacion(pedro.getId(), new CargaLineaTecnico(
                3, 3, "GHI789", "Eléctrica", new EstadoFacturado().getEstadoId(), "Facturado", new BigDecimal("1.50")
        ));
        tecnicoRepository.agregarAsignacion(pedro.getId(), new CargaLineaTecnico(
                4, 4, "JKL012", "Aceite", new EstadoEntregado().getEstadoId(), "Entregado", new BigDecimal("1.00")
        ));
        tecnicoRepository.agregarAsignacion(pedro.getId(), new CargaLineaTecnico(
                5, 5, "MNO345", "Alineación", new EstadoCancelada().getEstadoId(), "Cancelada", new BigDecimal("0.00")
        ));

        assertEquals(1, tecnicoService.obtenerCargaTrabajo(pedro.getId()));
        List<CargaLineaTecnico> lineas = tecnicoService.consultarCargaTrabajo(pedro.getId());
        assertEquals(1, lineas.size());
        assertEquals(1, lineas.get(0).getOrdenId());
    }

    @Test
    @DisplayName("Debe retornar cero y lista vacía cuando el técnico no tiene carga asignada")
    void tecnicoSinCargaRetornaCeroYListaVacia() {
        Tecnico juan = tecnicoService.registrarTecnico(new Tecnico(0, "Juan Pérez", "1090456124", 1, null, true));

        assertEquals(0, tecnicoService.obtenerCargaTrabajo(juan.getId()));
        assertTrue(tecnicoService.consultarCargaTrabajo(juan.getId()).isEmpty());
    }

    @Test
    @DisplayName("Debe lanzar EntidadNoEncontradaException al consultar carga de un técnico inexistente")
    void consultarCargaTecnicoInexistenteLanzaExcepcion() {
        assertThrows(EntidadNoEncontradaException.class, () -> tecnicoService.obtenerCargaTrabajo(999));
        assertThrows(EntidadNoEncontradaException.class, () -> tecnicoService.consultarCargaTrabajo(999));
    }

    @Test
    @DisplayName("Debe lanzar ValidacionException cuando el ID de técnico es cero o negativo")
    void consultarCargaIdInvalidoLanzaExcepcion() {
        assertThrows(ValidacionException.class, () -> tecnicoService.obtenerCargaTrabajo(0));
        assertThrows(ValidacionException.class, () -> tecnicoService.obtenerCargaTrabajo(-3));
        assertThrows(ValidacionException.class, () -> tecnicoService.consultarCargaTrabajo(0));
        assertThrows(ValidacionException.class, () -> tecnicoService.consultarCargaTrabajo(-1));
    }

    @Test
    @DisplayName("Debe permitir consultar la carga activa de un técnico inactivo con historial vigente")
    void consultarCargaTecnicoInactivoConHistorialVigente() {
        Tecnico luis = tecnicoService.registrarTecnico(new Tecnico(0, "Luis Ortega", "1090456128", 1, null, true));
        tecnicoRepository.agregarAsignacion(luis.getId(), new CargaLineaTecnico(
                10, 20, "PQR678", "Cambio de aceite",
                new EstadoEnProceso().getEstadoId(), "En Proceso", new BigDecimal("1.00")
        ));
        tecnicoService.desactivarTecnico(luis.getId());

        assertEquals(1, tecnicoService.obtenerCargaTrabajo(luis.getId()));
        List<CargaLineaTecnico> lineas = tecnicoService.consultarCargaTrabajo(luis.getId());
        assertEquals(1, lineas.size());
        assertEquals("PQR678", lineas.get(0).getPlaca());
    }

    @Test
    @DisplayName("Debe retornar el mapa de cargas activas por técnico para la cadena de sugerencia")
    void obtenerCargasActivasPorTecnico() {
        Tecnico t1 = tecnicoService.registrarTecnico(new Tecnico(0, "Pedro Ramírez", "1090456123", 3, null, true));
        Tecnico t2 = tecnicoService.registrarTecnico(new Tecnico(0, "Juan Pérez", "1090456124", 1, null, true));
        Tecnico t3 = tecnicoService.registrarTecnico(new Tecnico(0, "Laura Gómez", "1090456125", 3, null, true));

        tecnicoRepository.agregarAsignacion(t1.getId(), new CargaLineaTecnico(
                1, 1, "ABC123", "Frenos", new EstadoPendiente().getEstadoId(), "Pendiente", new BigDecimal("1.00")
        ));
        tecnicoRepository.agregarAsignacion(t1.getId(), new CargaLineaTecnico(
                2, 2, "DEF456", "Frenos", new EstadoEnProceso().getEstadoId(), "En Proceso", new BigDecimal("2.00")
        ));
        tecnicoRepository.agregarAsignacion(t2.getId(), new CargaLineaTecnico(
                3, 3, "GHI789", "Aceite", new EstadoCompletado().getEstadoId(), "Completado", new BigDecimal("1.00")
        ));
        tecnicoRepository.agregarAsignacion(t3.getId(), new CargaLineaTecnico(
                4, 4, "JKL012", "Frenos", new EstadoPendiente().getEstadoId(), "Pendiente", new BigDecimal("0.50")
        ));

        Map<Integer, Integer> cargas = tecnicoService.obtenerCargasActivasPorTecnico();
        assertEquals(2, cargas.get(t1.getId()));
        assertFalse(cargas.containsKey(t2.getId()));
        assertEquals(1, cargas.get(t3.getId()));
    }

    @Test
    @DisplayName("Debe rechazar nombre numérico o sin apellido, documento corto y teléfono inválido")
    void rechazarNombreDocumentoYTelefonoIlogicos() {
        assertThrows(ValidacionException.class, () -> tecnicoService.registrarTecnico(
                new Tecnico(0, "0", "1090456123", 1, "3205550001", true)));
        assertThrows(ValidacionException.class, () -> tecnicoService.registrarTecnico(
                new Tecnico(0, "Pedro", "1090456123", 1, "3205550001", true)));
        assertThrows(ValidacionException.class, () -> tecnicoService.registrarTecnico(
                new Tecnico(0, "Pedro 123", "1090456123", 1, "3205550001", true)));
        assertThrows(ValidacionException.class, () -> tecnicoService.registrarTecnico(
                new Tecnico(0, "Pedro Ramírez", "2", 1, "3205550001", true)));
        assertThrows(ValidacionException.class, () -> tecnicoService.registrarTecnico(
                new Tecnico(0, "Pedro Ramírez", "1090456123", 1, "2", true)));
        assertThrows(ValidacionException.class, () -> tecnicoService.registrarTecnico(
                new Tecnico(0, "Pedro Ramírez", "1090456123", 1, "abc12345", true)));
    }

    @Test
    @DisplayName("Actualizar un técnico inactivo conserva activo=false sin reactivarlo (RN-17)")
    void actualizarTecnicoInactivoConservaEstadoInactivo() {
        Tecnico creado = tecnicoService.registrarTecnico(
                new Tecnico(0, "Pedro Ramírez", "1090456123", 1, "3205550001", true));
        tecnicoService.desactivarTecnico(creado.getId());

        Tecnico cambios = new Tecnico();
        cambios.setId(creado.getId());
        cambios.setNombre("Pedro Antonio Ramírez");
        cambios.setDocumentoIdentidad("1090456123");
        cambios.setEspecialidadId(3);
        cambios.setTelefono("3205559999");
        cambios.setActivo(true);

        Tecnico actualizado = tecnicoService.actualizarTecnico(cambios);
        assertFalse(actualizado.isActivo());
        assertFalse(tecnicoService.buscarPorId(creado.getId()).isActivo());
    }

    private static class EspecialidadDAOEnMemoria implements EspecialidadDAO {

        private final List<Especialidad> datos = new ArrayList<>();

        void agregar(Especialidad especialidad) {
            datos.add(especialidad);
        }

        @Override
        public List<Especialidad> listarTodos() {
            return new ArrayList<>(datos);
        }

        @Override
        public Especialidad buscarPorId(int id) {
            for (Especialidad esp : datos) {
                if (esp.getId() == id) {
                    return esp;
                }
            }
            return null;
        }
    }

    private static class TecnicoDAOEnMemoria implements TecnicoDAO {

        private final List<Tecnico> datos = new ArrayList<>();
        private final Map<Integer, List<CargaLineaTecnico>> asignaciones = new HashMap<>();
        private int secuenciaId = 1;

        void agregarAsignacion(int tecnicoId, CargaLineaTecnico linea) {
            List<CargaLineaTecnico> lista = asignaciones.get(tecnicoId);
            if (lista == null) {
                lista = new ArrayList<>();
                asignaciones.put(tecnicoId, lista);
            }
            lista.add(linea);
        }

        @Override
        public List<Tecnico> listarTodos() {
            return new ArrayList<>(datos);
        }

        @Override
        public List<Tecnico> listarActivos() {
            List<Tecnico> activos = new ArrayList<>();
            for (Tecnico tecnico : datos) {
                if (tecnico.isActivo()) {
                    activos.add(tecnico);
                }
            }
            return activos;
        }

        @Override
        public Tecnico buscarPorId(int id) {
            for (Tecnico tecnico : datos) {
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
            for (Tecnico tecnico : datos) {
                if (tecnico.getDocumentoIdentidad().equals(documento)) {
                    return tecnico;
                }
            }
            return null;
        }

        @Override
        public int guardar(Tecnico tecnico) {
            int nuevoId = secuenciaId++;
            Tecnico copia = new Tecnico(
                    nuevoId,
                    tecnico.getNombre(),
                    tecnico.getDocumentoIdentidad(),
                    tecnico.getEspecialidadId(),
                    tecnico.getTelefono(),
                    tecnico.isActivo()
            );
            datos.add(copia);
            return nuevoId;
        }

        @Override
        public boolean actualizar(Tecnico tecnico) {
            for (int i = 0; i < datos.size(); i++) {
                if (datos.get(i).getId() == tecnico.getId()) {
                    datos.set(i, new Tecnico(
                            tecnico.getId(),
                            tecnico.getNombre(),
                            tecnico.getDocumentoIdentidad(),
                            tecnico.getEspecialidadId(),
                            tecnico.getTelefono(),
                            tecnico.isActivo()
                    ));
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean actualizarEstadoActivo(int id, boolean activo) {
            for (Tecnico tecnico : datos) {
                if (tecnico.getId() == id) {
                    tecnico.setActivo(activo);
                    return true;
                }
            }
            return false;
        }

        @Override
        public List<CargaLineaTecnico> listarCargaPorTecnico(int tecnicoId) {
            int pendienteId = new EstadoPendiente().getEstadoId();
            int enProcesoId = new EstadoEnProceso().getEstadoId();
            List<CargaLineaTecnico> todas = asignaciones.get(tecnicoId);
            List<CargaLineaTecnico> activas = new ArrayList<>();
            if (todas != null) {
                for (CargaLineaTecnico linea : todas) {
                    if (linea.getEstadoId() == pendienteId || linea.getEstadoId() == enProcesoId) {
                        activas.add(linea);
                    }
                }
            }
            activas.sort(Comparator.comparingInt(CargaLineaTecnico::getOrdenId)
                    .thenComparingInt(CargaLineaTecnico::getDetalleId));
            return activas;
        }

        @Override
        public Map<Integer, Integer> contarCargaActivaPorTecnico() {
            int pendienteId = new EstadoPendiente().getEstadoId();
            int enProcesoId = new EstadoEnProceso().getEstadoId();
            Map<Integer, Integer> resultado = new HashMap<>();
            for (Map.Entry<Integer, List<CargaLineaTecnico>> entry : asignaciones.entrySet()) {
                Set<Integer> detallesUnicos = new HashSet<>();
                for (CargaLineaTecnico linea : entry.getValue()) {
                    if (linea.getEstadoId() == pendienteId || linea.getEstadoId() == enProcesoId) {
                        detallesUnicos.add(linea.getDetalleId());
                    }
                }
                if (!detallesUnicos.isEmpty()) {
                    resultado.put(entry.getKey(), detallesUnicos.size());
                }
            }
            return resultado;
        }
    }
}
