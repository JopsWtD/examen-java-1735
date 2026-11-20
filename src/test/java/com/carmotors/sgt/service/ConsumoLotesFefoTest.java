package com.carmotors.sgt.service;

import com.carmotors.sgt.exception.StockInsuficienteException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Lote;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del algoritmo FEFO determinista (RN-03, sección 2.6) sin base de datos.
 * Todas las fechas son relativas a {@code LocalDate.now()}.
 */
class ConsumoLotesFefoTest {

    private final ConsumoLotesFefo fefo = new ConsumoLotesFefo();
    private final LocalDate hoy = LocalDate.now();

    private Lote lote(int id, LocalDate ingreso, LocalDate caducidad, int cantidadActual) {
        return new Lote(id, "LOT-" + id, 1, 1, ingreso, caducidad, Math.max(cantidadActual, 1), cantidadActual);
    }

    private List<Integer> ordenConsumo(Map<Integer, Integer> consumo) {
        return new ArrayList<>(consumo.keySet());
    }

    @Test
    @DisplayName("Un solo lote: descuenta del único lote disponible")
    void unSoloLote() {
        List<Lote> lotes = Arrays.asList(lote(1, hoy.minusDays(10), hoy.plusDays(30), 10));

        Map<Integer, Integer> consumo = fefo.calcularConsumo(lotes, hoy, 4);

        assertEquals(1, consumo.size());
        assertEquals(4, consumo.get(1));
    }

    @Test
    @DisplayName("Varios lotes con caducidad: consume primero el que vence antes")
    void consumePrimeroElQueVenceAntes() {
        List<Lote> lotes = Arrays.asList(
                lote(1, hoy.minusDays(30), hoy.plusDays(60), 10),
                lote(2, hoy.minusDays(5), hoy.plusDays(10), 10));

        Map<Integer, Integer> consumo = fefo.calcularConsumo(lotes, hoy, 3);

        assertEquals(Arrays.asList(2), ordenConsumo(consumo));
        assertEquals(3, consumo.get(2));
    }

    @Test
    @DisplayName("Empate de caducidad: desempata por ingreso más antiguo y luego por menor ID")
    void empateDeCaducidad() {
        LocalDate caducidad = hoy.plusDays(20);
        List<Lote> lotes = Arrays.asList(
                lote(5, hoy.minusDays(5), caducidad, 1),
                lote(4, hoy.minusDays(10), caducidad, 1),
                lote(3, hoy.minusDays(10), caducidad, 1));

        Map<Integer, Integer> consumo = fefo.calcularConsumo(lotes, hoy, 3);

        assertEquals(Arrays.asList(3, 4, 5), ordenConsumo(consumo));
    }

    @Test
    @DisplayName("Los lotes sin caducidad se consumen al final, por ingreso más antiguo y menor ID")
    void lotesSinCaducidadAlFinal() {
        List<Lote> lotes = Arrays.asList(
                lote(7, hoy.minusDays(5), null, 1),
                lote(6, hoy.minusDays(5), null, 1),
                lote(8, hoy.minusDays(20), null, 1),
                lote(9, hoy.minusDays(1), hoy.plusDays(300), 1));

        Map<Integer, Integer> consumo = fefo.calcularConsumo(lotes, hoy, 4);

        assertEquals(Arrays.asList(9, 8, 6, 7), ordenConsumo(consumo));
    }

    @Test
    @DisplayName("Un lote vencido se ignora para el consumo y para el stock operativo")
    void loteVencidoIgnorado() {
        List<Lote> lotes = Arrays.asList(
                lote(1, hoy.minusDays(60), hoy.minusDays(1), 10),
                lote(2, hoy.minusDays(10), null, 5));

        Map<Integer, Integer> consumo = fefo.calcularConsumo(lotes, hoy, 3);

        assertEquals(Arrays.asList(2), ordenConsumo(consumo));
        assertEquals(5, fefo.calcularStockOperativo(lotes, hoy));
    }

    @Test
    @DisplayName("Un lote que vence exactamente hoy sí se usa")
    void loteQueVenceHoySeUsa() {
        List<Lote> lotes = Arrays.asList(
                lote(1, hoy.minusDays(30), hoy, 2),
                lote(2, hoy.minusDays(30), null, 5));

        Map<Integer, Integer> consumo = fefo.calcularConsumo(lotes, hoy, 2);

        assertEquals(Arrays.asList(1), ordenConsumo(consumo));
        assertEquals(2, consumo.get(1));
        assertEquals(7, fefo.calcularStockOperativo(lotes, hoy));
    }

    @Test
    @DisplayName("Reparte el consumo entre varios lotes cuando el primero no alcanza")
    void repartirEntreVariosLotes() {
        List<Lote> lotes = Arrays.asList(
                lote(2, hoy.minusDays(10), hoy.plusDays(10), 5),
                lote(1, hoy.minusDays(10), hoy.plusDays(5), 2),
                lote(3, hoy.minusDays(10), hoy.plusDays(15), 0));

        Map<Integer, Integer> consumo = fefo.calcularConsumo(lotes, hoy, 4);

        assertEquals(Arrays.asList(1, 2), ordenConsumo(consumo));
        assertEquals(2, consumo.get(1));
        assertEquals(2, consumo.get(2));
    }

    @Test
    @DisplayName("Rechaza el consumo si los lotes vigentes no alcanzan")
    void stockInsuficiente() {
        List<Lote> lotes = Arrays.asList(
                lote(1, hoy.minusDays(60), hoy.minusDays(1), 10),
                lote(2, hoy.minusDays(10), null, 3));

        StockInsuficienteException error = assertThrows(StockInsuficienteException.class,
                () -> fefo.calcularConsumo(lotes, hoy, 4));

        assertTrue(error.getMessage().contains("solo tienen 3"), error.getMessage());
    }

    @Test
    @DisplayName("Rechaza una cantidad igual a cero o negativa")
    void rechazarCantidadNoPositiva() {
        List<Lote> lotes = Arrays.asList(lote(1, hoy.minusDays(1), null, 5));

        assertThrows(ValidacionException.class, () -> fefo.calcularConsumo(lotes, hoy, 0));
        assertThrows(ValidacionException.class, () -> fefo.calcularConsumo(lotes, hoy, -2));
    }
}
