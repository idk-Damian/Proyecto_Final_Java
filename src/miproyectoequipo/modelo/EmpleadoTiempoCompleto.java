/*
 * Modelo: Empleado a Tiempo Completo
 * Sueldo fijo: $1500 mensuales
 * Descuento por atraso: $0.20 por cada minuto
 */
package miproyectoequipo.modelo;

/**
 * Empleado de tiempo completo.
 * Trabaja 8 horas diarias, sueldo fijo $1500 con descuento por atrasos.
 * 
 * @author Vladimir
 */
public class EmpleadoTiempoCompleto extends Empleado {

    public static final double SUELDO_FIJO = 1500.00;
    public static final double DESCUENTO_POR_MINUTO_ATRASO = 0.20;

    public EmpleadoTiempoCompleto() {
        super();
        setTipoContrato(TipoContrato.TIEMPO_COMPLETO);
    }

    public EmpleadoTiempoCompleto(String cedula, String nombre, String apellido, String cargo) {
        super(cedula, nombre, apellido, cargo, TipoContrato.TIEMPO_COMPLETO);
    }

    /**
     * Calcula el sueldo mensual.
     * Sueldo fijo $1500 - (minutosAtraso * $0.20)
     * 
     * @param horasTrabajadas no se usa para tiempo completo
     * @param minutosAtraso minutos totales de atraso en el mes
     * @return sueldo con descuentos aplicados
     */
    @Override
    public double calcularSueldo(double horasTrabajadas, int minutosAtraso) {
        double descuento = minutosAtraso * DESCUENTO_POR_MINUTO_ATRASO;
        return SUELDO_FIJO - descuento;
    }

    public double getSueldoFijo() {
        return SUELDO_FIJO;
    }
}
