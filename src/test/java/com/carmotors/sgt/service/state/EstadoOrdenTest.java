package com.carmotors.sgt.service.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.carmotors.sgt.exception.TransicionEstadoInvalidaException;

class EstadoOrdenTest {

    @Test
    @DisplayName("Debe permitir el ciclo normal Pendiente a Entregado")
    void debePermitirCicloNormal() {
        EstadoOrden estado = new EstadoPendiente();

        estado = estado.iniciarProceso();
        assertEquals("En Proceso", estado.getNombre());

        estado = estado.completar();
        assertEquals("Completado", estado.getNombre());

        estado = estado.facturar();
        assertEquals("Facturado", estado.getNombre());

        estado = estado.entregar();
        assertEquals("Entregado", estado.getNombre());
        assertFalse(estado.permiteEdicion());
    }

    @Test
    @DisplayName("Debe permitir cancelar desde Pendiente")
    void debePermitirCancelarDesdePendiente() {
        EstadoOrden estado = new EstadoPendiente().cancelar();

        assertEquals("Cancelada", estado.getNombre());
        assertFalse(estado.permiteEdicion());
    }

    @Test
    @DisplayName("Debe permitir cancelar desde En Proceso")
    void debePermitirCancelarDesdeEnProceso() {
        EstadoOrden estado = new EstadoPendiente().iniciarProceso();
        estado = estado.cancelar();

        assertEquals("Cancelada", estado.getNombre());
    }

    @Test
    @DisplayName("Debe rechazar saltos desde Pendiente")
    void debeRechazarSaltoDesdePendiente() {
        EstadoOrden estado = new EstadoPendiente();

        TransicionEstadoInvalidaException error = assertThrows(
                TransicionEstadoInvalidaException.class,
                () -> estado.completar()
        );

        assertTrue(error.getMessage().contains("de Pendiente a Completado"));
    }

    @Test
    @DisplayName("Debe rechazar retroceso desde Completado")
    void debeRechazarRetrocesoDesdeCompletado() {
        EstadoOrden estado = new EstadoPendiente().iniciarProceso().completar();

        assertThrows(TransicionEstadoInvalidaException.class, () -> estado.iniciarProceso());
    }

    @Test
    @DisplayName("Debe rechazar facturar desde En Proceso")
    void debeRechazarFacturarDesdeEnProceso() {
        EstadoOrden estado = new EstadoPendiente().iniciarProceso();

        assertThrows(TransicionEstadoInvalidaException.class, () -> estado.facturar());
    }

    @Test
    @DisplayName("Debe rechazar cancelar desde Completado")
    void debeRechazarCancelarDesdeCompletado() {
        EstadoOrden estado = new EstadoPendiente().iniciarProceso().completar();

        assertThrows(TransicionEstadoInvalidaException.class, () -> estado.cancelar());
    }

    @Test
    @DisplayName("Debe rechazar cualquier transición desde Entregado")
    void debeRechazarTransicionesDesdeEntregado() {
        EstadoOrden estado = new EstadoPendiente().iniciarProceso().completar().facturar().entregar();

        assertThrows(TransicionEstadoInvalidaException.class,
                () -> estado.transicionarA(new EstadoPendiente()));
        assertThrows(TransicionEstadoInvalidaException.class, () -> estado.cancelar());
    }

    @Test
    @DisplayName("Debe rechazar cualquier transición desde Cancelada")
    void debeRechazarTransicionesDesdeCancelada() {
        EstadoOrden estado = new EstadoPendiente().cancelar();

        assertThrows(TransicionEstadoInvalidaException.class,
                () -> estado.transicionarA(new EstadoFacturado()));
        assertThrows(TransicionEstadoInvalidaException.class, () -> estado.entregar());
    }

    @Test
    @DisplayName("Debe rechazar estados destino nulos")
    void debeRechazarEstadoDestinoNulo() {
        EstadoOrden estado = new EstadoPendiente();

        TransicionEstadoInvalidaException error = assertThrows(
                TransicionEstadoInvalidaException.class,
                () -> estado.transicionarA(null)
        );

        assertTrue(error.getMessage().contains("estado nulo"));
    }

    @Test
    @DisplayName("Debe exponer los IDs oficiales de servicio_estados")
    void debeExponerIdsOficiales() {
        assertEquals(1, new EstadoPendiente().getEstadoId());
        assertEquals(2, new EstadoEnProceso().getEstadoId());
        assertEquals(3, new EstadoCompletado().getEstadoId());
        assertEquals(4, new EstadoFacturado().getEstadoId());
        assertEquals(5, new EstadoEntregado().getEstadoId());
        assertEquals(6, new EstadoCancelada().getEstadoId());
    }

    @Test
    @DisplayName("Debe mapear desde cada ID válido de servicio_estados")
    void debeMapearIdsValidos() {
        assertTrue(EstadoOrdenFactory.desdeId(1) instanceof EstadoPendiente);
        assertTrue(EstadoOrdenFactory.desdeId(2) instanceof EstadoEnProceso);
        assertTrue(EstadoOrdenFactory.desdeId(3) instanceof EstadoCompletado);
        assertTrue(EstadoOrdenFactory.desdeId(4) instanceof EstadoFacturado);
        assertTrue(EstadoOrdenFactory.desdeId(5) instanceof EstadoEntregado);
        assertTrue(EstadoOrdenFactory.desdeId(6) instanceof EstadoCancelada);
    }

    @Test
    @DisplayName("Debe rechazar IDs desconocidos de servicio_estados")
    void debeRechazarIdDesconocido() {
        TransicionEstadoInvalidaException error = assertThrows(
                TransicionEstadoInvalidaException.class,
                () -> EstadoOrdenFactory.desdeId(99)
        );

        assertTrue(error.getMessage().contains("ID: 99"));
    }
}
