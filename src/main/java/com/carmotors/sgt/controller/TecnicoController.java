package com.carmotors.sgt.controller;

import com.carmotors.sgt.model.CargaLineaTecnico;
import com.carmotors.sgt.model.Especialidad;
import com.carmotors.sgt.model.Tecnico;
import com.carmotors.sgt.service.TecnicoService;

import java.util.List;

/**
 * Controlador de técnicos: recibe peticiones de la vista y coordina con {@link TecnicoService}.
 * No accede directamente a repositorios ni abre conexiones JDBC (RNF-04).
 */
public class TecnicoController {

    private final TecnicoService tecnicoService;

    public TecnicoController(TecnicoService tecnicoService) {
        if (tecnicoService == null) {
            throw new IllegalArgumentException("El servicio de técnicos es obligatorio.");
        }
        this.tecnicoService = tecnicoService;
    }

    public List<Tecnico> listarTodos() {
        return tecnicoService.listarTodos();
    }

    public List<Tecnico> listarActivos() {
        return tecnicoService.listarActivos();
    }

    public List<Especialidad> listarEspecialidades() {
        return tecnicoService.listarEspecialidades();
    }

    public String obtenerNombreEspecialidad(int especialidadId) {
        Especialidad especialidad = tecnicoService.buscarEspecialidadPorId(especialidadId);
        return especialidad != null ? especialidad.getNombre() : "Desconocida";
    }

    public Tecnico buscarPorId(int id) {
        return tecnicoService.buscarPorId(id);
    }

    public Tecnico buscarPorDocumento(String documento) {
        return tecnicoService.buscarPorDocumento(documento);
    }

    public Tecnico registrar(Tecnico tecnico) {
        return tecnicoService.registrarTecnico(tecnico);
    }

    public Tecnico actualizar(Tecnico tecnico) {
        return tecnicoService.actualizarTecnico(tecnico);
    }

    public Tecnico desactivar(int id) {
        return tecnicoService.desactivarTecnico(id);
    }

    public Tecnico reactivar(int id) {
        return tecnicoService.reactivarTecnico(id);
    }

    public int obtenerCargaTrabajo(int tecnicoId) {
        return tecnicoService.obtenerCargaTrabajo(tecnicoId);
    }

    public List<CargaLineaTecnico> consultarCargaTrabajo(int tecnicoId) {
        return tecnicoService.consultarCargaTrabajo(tecnicoId);
    }
}
