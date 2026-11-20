package com.carmotors.sgt.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias para la jerarquía de excepciones de CarMotors SGT.
 * Valida la herencia, constructores y la convención de manejo de errores (RNF-12, RNF-23).
 */
class CarMotorsExceptionTest {

    @Test
    @DisplayName("Todas las excepciones de dominio deben heredar de CarMotorsException y RuntimeException")
    void testJerarquiaHerencia() {
        CarMotorsException base = new CarMotorsException("Error base");
        EntidadNoEncontradaException noEncontrada = new EntidadNoEncontradaException("No encontrado");
        DatoDuplicadoException duplicado = new DatoDuplicadoException("Duplicado");
        ValidacionException validacion = new ValidacionException("Validación");
        ConfiguracionException config = new ConfiguracionException("Configuración");

        assertTrue(base instanceof RuntimeException, "CarMotorsException debe ser una RuntimeException");
        assertTrue(noEncontrada instanceof CarMotorsException, "EntidadNoEncontradaException debe heredar de CarMotorsException");
        assertTrue(duplicado instanceof CarMotorsException, "DatoDuplicadoException debe heredar de CarMotorsException");
        assertTrue(validacion instanceof CarMotorsException, "ValidacionException debe heredar de CarMotorsException");
        assertTrue(config instanceof CarMotorsException, "ConfiguracionException debe heredar de CarMotorsException");
    }

    @Test
    @DisplayName("Los constructores deben preservar correctamente el mensaje y la causa raíz")
    void testConstructoresPreservanMensajeYCausa() {
        Throwable causa = new IllegalArgumentException("Causa original");

        CarMotorsException ex1 = new CarMotorsException("Mensaje base", causa);
        assertEquals("Mensaje base", ex1.getMessage());
        assertSame(causa, ex1.getCause());

        EntidadNoEncontradaException ex2 = new EntidadNoEncontradaException("Cliente 10 no existe", causa);
        assertEquals("Cliente 10 no existe", ex2.getMessage());
        assertSame(causa, ex2.getCause());

        DatoDuplicadoException ex3 = new DatoDuplicadoException("Placa ABC123 ya registrada", causa);
        assertEquals("Placa ABC123 ya registrada", ex3.getMessage());
        assertSame(causa, ex3.getCause());

        ValidacionException ex4 = new ValidacionException("El precio debe ser positivo", causa);
        assertEquals("El precio debe ser positivo", ex4.getMessage());
        assertSame(causa, ex4.getCause());

        ConfiguracionException ex5 = new ConfiguracionException("Error en config", causa);
        assertEquals("Error en config", ex5.getMessage());
        assertSame(causa, ex5.getCause());
    }

    @Test
    @DisplayName("Simulación de convención Service -> Controller sin abortar ejecución (RNF-12)")
    void testFlujoServiceControllerSimulado() {
        // Simulación de Servicio que valida negocio y lanza excepción de dominio
        ServicioSimulado servicio = new ServicioSimulado();
        ControladorSimulado controlador = new ControladorSimulado(servicio);

        // Caso 1: Entidad no encontrada capturada limpiamente
        String respuestaNoExiste = controlador.buscarPorId(999);
        assertNotNull(respuestaNoExiste);
        assertTrue(respuestaNoExiste.contains("No se encontró el registro con ID: 999"));

        // Caso 2: Dato duplicado capturado limpiamente
        String respuestaDuplicado = controlador.registrar("12345678");
        assertNotNull(respuestaDuplicado);
        assertTrue(respuestaDuplicado.contains("Ya existe un cliente con documento: 12345678"));

        // Caso 3: Validacion fallida capturada limpiamente
        String respuestaValidacion = controlador.registrar("");
        assertNotNull(respuestaValidacion);
        assertTrue(respuestaValidacion.contains("El documento no puede estar vacío"));
    }

    // --- Clases auxiliares para la simulación del patrón RNF-12 ---

    private static class ServicioSimulado {
        String buscar(int id) {
            if (id == 999) {
                throw new EntidadNoEncontradaException("No se encontró el registro con ID: " + id);
            }
            return "Registro " + id;
        }

        void guardar(String documento) {
            if (documento == null || documento.trim().isEmpty()) {
                throw new ValidacionException("El documento no puede estar vacío");
            }
            if ("12345678".equals(documento)) {
                throw new DatoDuplicadoException("Ya existe un cliente con documento: " + documento);
            }
        }
    }

    private static class ControladorSimulado {
        private final ServicioSimulado servicio;

        ControladorSimulado(ServicioSimulado servicio) {
            this.servicio = servicio;
        }

        String buscarPorId(int id) {
            try {
                return servicio.buscar(id);
            } catch (CarMotorsException e) {
                // Controller captura la excepción de dominio y retorna mensaje amigable sin System.exit()
                return "  [!] Error de negocio: " + e.getMessage();
            }
        }

        String registrar(String documento) {
            try {
                servicio.guardar(documento);
                return "  [OK] Registrado exitosamente";
            } catch (CarMotorsException e) {
                return "  [!] Error al registrar: " + e.getMessage();
            }
        }
    }
}
