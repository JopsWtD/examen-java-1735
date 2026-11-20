package com.carmotors.sgt.service;

import com.carmotors.sgt.exception.DatoDuplicadoException;
import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Especialidad;
import com.carmotors.sgt.model.MantenimientoTipo;
import com.carmotors.sgt.model.ServicioTipo;
import com.carmotors.sgt.dao.EspecialidadDAO;
import com.carmotors.sgt.dao.MantenimientoTipoDAO;
import com.carmotors.sgt.dao.ServicioTipoDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pruebas unitarias en memoria para {@link ServicioTipoService} (T2.5).
 */
class ServicioTipoServiceTest {

    private ServicioTipoDAOEnMemoria servicioTipoRepository;
    private MantenimientoTipoDAOEnMemoria mantenimientoTipoRepository;
    private EspecialidadDAOEnMemoria especialidadRepository;
    private ServicioTipoService servicioTipoService;

    @BeforeEach
    void configurar() {
        servicioTipoRepository = new ServicioTipoDAOEnMemoria();
        mantenimientoTipoRepository = new MantenimientoTipoDAOEnMemoria();
        especialidadRepository = new EspecialidadDAOEnMemoria();

        mantenimientoTipoRepository.agregar(new MantenimientoTipo(1, "Preventivo"));
        mantenimientoTipoRepository.agregar(new MantenimientoTipo(2, "Correctivo"));

        especialidadRepository.agregar(new Especialidad(1, "Mecánica general"));
        especialidadRepository.agregar(new Especialidad(3, "Frenos"));

        servicioTipoService = new ServicioTipoService(
                servicioTipoRepository, mantenimientoTipoRepository, especialidadRepository);
    }

    @Test
    @DisplayName("Debe registrar un servicio Preventivo válido normalizando BigDecimal a escala 2 HALF_UP")
    void registrarServicioPreventivoValido() {
        ServicioTipo nuevo = new ServicioTipo(
                0, "  Cambio de aceite  ", 1, 1, new BigDecimal("60000.556"), 6);

        ServicioTipo guardado = servicioTipoService.registrarServicioTipo(nuevo);

        assertEquals(1, guardado.getId());
        assertEquals("Cambio de aceite", guardado.getNombre());
        assertEquals(new BigDecimal("60000.56"), guardado.getPrecioManoObraBase());
        assertEquals(2, guardado.getPrecioManoObraBase().scale());
        assertEquals(6, guardado.getIntervaloMeses());
    }

    @Test
    @DisplayName("Debe registrar un servicio Correctivo válido con intervalo nulo")
    void registrarServicioCorrectivoValido() {
        ServicioTipo nuevo = new ServicioTipo(
                0, "Cambio de pastillas de freno", 2, 3, new BigDecimal("90000.00"), null);

        ServicioTipo guardado = servicioTipoService.registrarServicioTipo(nuevo);

        assertEquals(1, guardado.getId());
        assertNull(guardado.getIntervaloMeses());
    }

    @Test
    @DisplayName("Debe rechazar Preventivo sin intervalo o con intervalo no positivo")
    void rechazarPreventivoSinIntervaloValido() {
        ServicioTipo sinIntervalo = new ServicioTipo(
                0, "Afinación de motor", 1, 1, new BigDecimal("150000.00"), null);
        assertThrows(ValidacionException.class, () -> servicioTipoService.registrarServicioTipo(sinIntervalo));

        ServicioTipo intervaloCero = new ServicioTipo(
                0, "Afinación de motor", 1, 1, new BigDecimal("150000.00"), 0);
        assertThrows(ValidacionException.class, () -> servicioTipoService.registrarServicioTipo(intervaloCero));
    }

    @Test
    @DisplayName("Debe rechazar Correctivo si se envía un intervalo en meses")
    void rechazarCorrectivoConIntervalo() {
        ServicioTipo correctivoConMeses = new ServicioTipo(
                0, "Reparación de frenos", 2, 3, new BigDecimal("120000.00"), 12);
        assertThrows(ValidacionException.class, () -> servicioTipoService.registrarServicioTipo(correctivoConMeses));
    }

    @Test
    @DisplayName("Debe rechazar precio negativo y nombre duplicado ignorando mayúsculas")
    void rechazarPrecioNegativoYNombreDuplicado() {
        ServicioTipo precioNegativo = new ServicioTipo(
                0, "Servicio inválido", 2, 1, new BigDecimal("-100.00"), null);
        assertThrows(ValidacionException.class, () -> servicioTipoService.registrarServicioTipo(precioNegativo));

        servicioTipoService.registrarServicioTipo(
                new ServicioTipo(0, "Cambio de aceite", 1, 1, new BigDecimal("60000.00"), 6));

        ServicioTipo duplicado = new ServicioTipo(
                0, "cambio de aceite", 1, 1, new BigDecimal("65000.00"), 6);
        assertThrows(DatoDuplicadoException.class, () -> servicioTipoService.registrarServicioTipo(duplicado));
    }

    @Test
    @DisplayName("Debe rechazar tipo de mantenimiento o especialidad inexistente")
    void rechazarCatalogosInexistentes() {
        ServicioTipo mantInvalido = new ServicioTipo(
                0, "Servicio X", 99, 1, new BigDecimal("50000.00"), null);
        assertThrows(EntidadNoEncontradaException.class, () -> servicioTipoService.registrarServicioTipo(mantInvalido));

        ServicioTipo espInvalida = new ServicioTipo(
                0, "Servicio Y", 2, 99, new BigDecimal("50000.00"), null);
        assertThrows(EntidadNoEncontradaException.class, () -> servicioTipoService.registrarServicioTipo(espInvalida));
    }

    @Test
    @DisplayName("Debe actualizar y eliminar un tipo de servicio existente")
    void actualizarYEliminarServicioTipo() {
        ServicioTipo creado = servicioTipoService.registrarServicioTipo(
                new ServicioTipo(0, "Alineación", 1, 1, new BigDecimal("70000.00"), 12));

        creado.setNombre("Alineación y balanceo");
        creado.setMantenimientoTipoId(2);
        creado.setIntervaloMeses(null);
        creado.setPrecioManoObraBase(new BigDecimal("85000"));

        ServicioTipo actualizado = servicioTipoService.actualizarServicioTipo(creado);
        assertEquals("Alineación y balanceo", actualizado.getNombre());
        assertEquals(new BigDecimal("85000.00"), actualizado.getPrecioManoObraBase());
        assertNull(actualizado.getIntervaloMeses());

        servicioTipoService.eliminarServicioTipo(creado.getId());
        assertEquals(0, servicioTipoService.listarTodos().size());
    }

    @Test
    @DisplayName("Debe rechazar nombre numérico o mayor a 100 caracteres, precio desbordado e intervalo > 120 meses")
    void rechazarNombreInvalidoPrecioExcesivoEIntervaloDesbordado() {
        assertThrows(ValidacionException.class, () -> servicioTipoService.registrarServicioTipo(
                new ServicioTipo(0, "0", 2, 1, new BigDecimal("50000.00"), null)));
        assertThrows(ValidacionException.class, () -> servicioTipoService.registrarServicioTipo(
                new ServicioTipo(0, "a".repeat(101), 2, 1, new BigDecimal("50000.00"), null)));
        assertThrows(ValidacionException.class, () -> servicioTipoService.registrarServicioTipo(
                new ServicioTipo(0, "Servicio válido", 2, 1, new BigDecimal("10000000000.00"), null)));
        assertThrows(ValidacionException.class, () -> servicioTipoService.registrarServicioTipo(
                new ServicioTipo(0, "Preventivo extremo", 1, 1, new BigDecimal("50000.00"), 121)));
    }

    private static class MantenimientoTipoDAOEnMemoria implements MantenimientoTipoDAO {

        private final List<MantenimientoTipo> datos = new ArrayList<>();

        void agregar(MantenimientoTipo tipo) {
            datos.add(tipo);
        }

        @Override
        public List<MantenimientoTipo> listarTodos() {
            return new ArrayList<>(datos);
        }

        @Override
        public MantenimientoTipo buscarPorId(int id) {
            for (MantenimientoTipo tipo : datos) {
                if (tipo.getId() == id) {
                    return tipo;
                }
            }
            return null;
        }
    }

    private static class EspecialidadDAOEnMemoria implements EspecialidadDAO {

        private final List<Especialidad> datos = new ArrayList<>();

        void agregar(Especialidad esp) {
            datos.add(esp);
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

    private static class ServicioTipoDAOEnMemoria implements ServicioTipoDAO {

        private final List<ServicioTipo> datos = new ArrayList<>();
        private int secuenciaId = 1;

        @Override
        public List<ServicioTipo> listarTodos() {
            return new ArrayList<>(datos);
        }

        @Override
        public ServicioTipo buscarPorId(int id) {
            for (ServicioTipo item : datos) {
                if (item.getId() == id) {
                    return item;
                }
            }
            return null;
        }

        @Override
        public ServicioTipo buscarPorNombre(String nombre) {
            for (ServicioTipo item : datos) {
                if (item.getNombre().equalsIgnoreCase(nombre)) {
                    return item;
                }
            }
            return null;
        }

        @Override
        public int guardar(ServicioTipo servicioTipo) {
            int id = secuenciaId++;
            ServicioTipo copia = new ServicioTipo(
                    id,
                    servicioTipo.getNombre(),
                    servicioTipo.getMantenimientoTipoId(),
                    servicioTipo.getEspecialidadId(),
                    servicioTipo.getPrecioManoObraBase(),
                    servicioTipo.getIntervaloMeses()
            );
            datos.add(copia);
            return id;
        }

        @Override
        public boolean actualizar(ServicioTipo servicioTipo) {
            for (int i = 0; i < datos.size(); i++) {
                if (datos.get(i).getId() == servicioTipo.getId()) {
                    datos.set(i, new ServicioTipo(
                            servicioTipo.getId(),
                            servicioTipo.getNombre(),
                            servicioTipo.getMantenimientoTipoId(),
                            servicioTipo.getEspecialidadId(),
                            servicioTipo.getPrecioManoObraBase(),
                            servicioTipo.getIntervaloMeses()
                    ));
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean eliminar(int id) {
            for (int i = 0; i < datos.size(); i++) {
                if (datos.get(i).getId() == id) {
                    datos.remove(i);
                    return true;
                }
            }
            return false;
        }
    }
}
