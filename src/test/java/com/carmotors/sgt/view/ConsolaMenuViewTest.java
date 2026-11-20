package com.carmotors.sgt.view;

import com.carmotors.sgt.util.ConsoleReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsolaMenuViewTest {

    @AfterEach
    void restaurarScanner() {
        ConsoleReader.reiniciarScannerSistema();
    }

    @Test
    void menuPrincipalPermiteSalirConOpcionCero() {
        ConsoleReader.setScanner(new Scanner("0\n"));
        ConsolaMenuView menu = new ConsolaMenuView();
        menu.mostrarMenuPrincipal();
    }

    @Test
    void menuPrincipalInvocaSubmenuConectadoYVuelveAlMenu() {
        ConsoleReader.setScanner(new Scanner("1\n0\n"));
        ConsolaMenuView menu = new ConsolaMenuView();

        final int[] llamadas = {0};
        menu.setSubmenuClientes(new SubmenuVista() {
            @Override
            public String obtenerTitulo() {
                return "Clientes y Vehículos";
            }

            @Override
            public void mostrarMenu() {
                llamadas[0] = llamadas[0] + 1;
            }
        });

        menu.mostrarMenuPrincipal();
        assertEquals(1, llamadas[0]);
    }

    @Test
    void menuPrincipalManejaModuloNoConectadoYExcepcionSinCerrarAplicacion() {
        ConsoleReader.setScanner(new Scanner("3\n\n2\n\n0\n"));
        ConsolaMenuView menu = new ConsolaMenuView();

        final int[] intentos = {0};
        menu.setSubmenuProveedores(new SubmenuVista() {
            @Override
            public String obtenerTitulo() {
                return "Proveedores";
            }

            @Override
            public void mostrarMenu() {
                intentos[0] = intentos[0] + 1;
                throw new RuntimeException("Error simulado");
            }
        });

        menu.mostrarMenuPrincipal();
        assertEquals(1, intentos[0]);
    }

    @Test
    void menuPrincipalRecorreLosSieteModulosConectadosYSaleLimpio() {
        ConsoleReader.setScanner(new Scanner("1\n2\n3\n4\n5\n6\n7\n0\n"));
        ConsolaMenuView menu = new ConsolaMenuView();

        final int[] visitas = new int[7];
        menu.setSubmenuClientes(submenuContado("Clientes y Vehículos", visitas, 0));
        menu.setSubmenuProveedores(submenuContado("Proveedores", visitas, 1));
        menu.setSubmenuInventario(submenuContado("Inventario y Lotes", visitas, 2));
        menu.setSubmenuTecnicos(submenuContado("Técnicos y Especialidades", visitas, 3));
        menu.setSubmenuServicios(submenuContado("Servicios y Órdenes de Trabajo", visitas, 4));
        menu.setSubmenuFacturacion(submenuContado("Facturación", visitas, 5));
        menu.setSubmenuReportes(submenuContado("Reportes Gerenciales", visitas, 6));

        menu.mostrarMenuPrincipal();

        for (int i = 0; i < visitas.length; i++) {
            assertEquals(1, visitas[i], "El módulo #" + (i + 1) + " debe invocarse exactamente una vez.");
        }
    }

    private SubmenuVista submenuContado(String titulo, int[] visitas, int indice) {
        return new SubmenuVista() {
            @Override
            public String obtenerTitulo() {
                return titulo;
            }

            @Override
            public void mostrarMenu() {
                visitas[indice] = visitas[indice] + 1;
            }
        };
    }
}
