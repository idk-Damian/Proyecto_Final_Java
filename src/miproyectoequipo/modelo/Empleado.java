/*
 * Modelo: Empleado (clase abstracta)
 * Base para EmpleadoTiempoCompleto y EmpleadoTiempoParcial
 */
package miproyectoequipo.modelo;

/**
 * Clase abstracta que representa un empleado del sistema.
 * Los empleados pueden ser de tiempo completo o tiempo parcial.
 * 
 * @author Vladimir
 */
public abstract class Empleado {

    public enum TipoContrato {
        TIEMPO_COMPLETO,
        TIEMPO_PARCIAL
    }

    private int id;
    private String cedula;
    private String nombre;
    private String apellido;
    private String cargo;
    private TipoContrato tipoContrato;
    private boolean activo;

    public Empleado() {
        this.activo = true;
    }

    public Empleado(String cedula, String nombre, String apellido, String cargo, TipoContrato tipoContrato) {
        this.cedula = cedula;
        this.nombre = nombre;
        this.apellido = apellido;
        this.cargo = cargo;
        this.tipoContrato = tipoContrato;
        this.activo = true;
    }

    /**
     * Calcula el sueldo mensual del empleado.
     * La implementación varía según el tipo de contrato.
     * 
     * @param horasTrabajadas total de horas trabajadas en el mes
     * @param minutosAtraso total de minutos de atraso en el mes
     * @return sueldo calculado
     */
    public abstract double calcularSueldo(double horasTrabajadas, int minutosAtraso);

    // Getters y Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public TipoContrato getTipoContrato() {
        return tipoContrato;
    }

    public void setTipoContrato(TipoContrato tipoContrato) {
        this.tipoContrato = tipoContrato;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }

    @Override
    public String toString() {
        return "Empleado{" + "cedula=" + cedula + ", nombre=" + getNombreCompleto() + ", tipo=" + tipoContrato + '}';
    }
}
