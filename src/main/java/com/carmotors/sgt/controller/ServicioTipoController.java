package com.carmotors.sgt.controller;

import com.carmotors.sgt.model.Especialidad;
import com.carmotors.sgt.model.MantenimientoTipo;
import com.carmotors.sgt.model.ServicioTipo;
import com.carmotors.sgt.service.ServicioTipoService;

import java.util.List;

/**
 * Controlador para el catálogo de tipos de servicio (RNF-04).
 */
public class ServicioTipoController {

    private final ServicioTipoService servicioTipoService;

    public ServicioTipoController(ServicioTipoService servicioTipoService) {
        if (servicioTipoService == null) {
            throw new IllegalArgumentException("El servicio de tipos de servicio es obligatorio.");
        }
        this.servicioTipoService = servicioTipoService;
    }

    public List<ServicioTipo> listarTodos() {
        return servicioTipoService.listarTodos();
    }

    public List<MantenimientoTipo> listarTiposMantenimiento() {
        return servicioTipoService.listarTiposMantenimiento();
    }

    public List<Especialidad> listarEspecialidades() {
        return servicioTipoService.listarEspecialidades();
    }

    public String obtenerNombreMantenimientoTipo(int mantenimientoTipoId) {
        MantenimientoTipo tipo = servicioTipoService.buscarTipoMantenimientoPorId(mantenimientoTipoId);
        return tipo != null ? tipo.getNombre() : "Desconocido";
    }

    public String obtenerNombreEspecialidad(int especialidadId) {
        Especialidad esp = servicioTipoService.buscarEspecialidadPorId(especialidadId);
        return esp != null ? esp.getNombre() : "Desconocida";
    }

    public ServicioTipo buscarPorId(int id) {
        return servicioTipoService.buscarPorId(id);
    }

    public ServicioTipo registrar(ServicioTipo servicioTipo) {
        return servicioTipoService.registrarServicioTipo(servicioTipo);
    }

    public ServicioTipo actualizar(ServicioTipo servicioTipo) {
        return servicioTipoService.actualizarServicioTipo(servicioTipo);
    }

    public void eliminar(int id) {
        servicioTipoService.eliminarServicioTipo(id);
    }
}
