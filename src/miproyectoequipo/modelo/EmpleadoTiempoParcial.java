/*
 * Modelo: Empleado a Tiempo Parcial
 * Pago por hora: $5.00
 * Máximo 8 horas diarias
 */
package miproyectoequipo.modelo;

/**
 * Empleado de tiempo parcial.
 * Gana $5 por hora trabajada, máximo 8 horas diarias.
 * 
 * @author Vladimir
 */
public class EmpleadoTiempoParcial extends Empleado {

    public static final double PAGO_POR_HORA = 5.00;
    public static final int MAX_HORAS_DIARIAS = 8;

    public EmpleadoTiempoParcial() {
        super();
        setTipoContrato(TipoContrato.TIEMPO_PARCIAL);
    }

    public EmpleadoTiempoParcial(String cedula, String nombre, String apellido, String cargo) {
        super(cedula, nombre, apellido, cargo, TipoContrato.TIEMPO_PARCIAL);
    }

    /**
     * Calcula el sueldo mensual.
     * horasTrabajadas * $5.00 (ajustando minutos a fracciones de hora)
     * 
     * @param horasTrabajadas total de horas trabajadas en el mes
     * @param minutosAtraso no se aplica descuento, solo se pagan las horas efectivas
     * @return sueldo calculado
     */
    @Override
    public double calcularSueldo(double horasTrabajadas, int minutosAtraso) {
        return horasTrabajadas * PAGO_POR_HORA;
    }

    public double getPagoPorHora() {
        return PAGO_POR_HORA;
    }
}
