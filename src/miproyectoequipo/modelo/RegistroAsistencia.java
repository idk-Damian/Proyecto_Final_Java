
package miproyectoequipo.modelo;

import java.time.LocalDate;
import java.time.LocalTime;

public class RegistroAsistencia {

    public static final LocalTime HORA_ENTRADA_MANANA = LocalTime.of(8, 0);
    public static final LocalTime HORA_SALIDA_MANANA = LocalTime.of(13, 0);
    public static final LocalTime HORA_ENTRADA_TARDE = LocalTime.of(14, 0);
    public static final LocalTime HORA_SALIDA_TARDE = LocalTime.of(17, 0);

    private int id;
    private String cedulaEmpleado;
    private LocalDate fecha;
    private LocalTime horaEntradaManana;
    private LocalTime horaSalidaManana;
    private LocalTime horaEntradaTarde;
    private LocalTime horaSalidaTarde;
    private int minutosAtraso;

    public RegistroAsistencia() {
        this.fecha = LocalDate.now();
        this.minutosAtraso = 0;
    }

    public RegistroAsistencia(String cedulaEmpleado) {
        this.cedulaEmpleado = cedulaEmpleado;
        this.fecha = LocalDate.now();
        this.minutosAtraso = 0;
    }

    public double calcularHorasTrabajadas() {
        double horas = 0;

        if (horaEntradaManana != null && horaSalidaManana != null) {
            long minutosManana = java.time.Duration.between(horaEntradaManana, horaSalidaManana).toMinutes();
            horas += minutosManana / 60.0;
        }

        if (horaEntradaTarde != null && horaSalidaTarde != null) {
            long minutosTarde = java.time.Duration.between(horaEntradaTarde, horaSalidaTarde).toMinutes();
            horas += minutosTarde / 60.0;
        }

        return Math.min(horas, 8.0);
    }

    public int calcularMinutosAtraso() {
        int atraso = 0;

        if (horaEntradaManana != null && horaEntradaManana.isAfter(HORA_ENTRADA_MANANA)) {
            atraso += java.time.Duration.between(HORA_ENTRADA_MANANA, horaEntradaManana).toMinutes();
        }

        if (horaEntradaTarde != null && horaEntradaTarde.isAfter(HORA_ENTRADA_TARDE)) {
            atraso += java.time.Duration.between(HORA_ENTRADA_TARDE, horaEntradaTarde).toMinutes();
        }

        this.minutosAtraso = atraso;
        return atraso;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCedulaEmpleado() {
        return cedulaEmpleado;
    }

    public void setCedulaEmpleado(String cedulaEmpleado) {
        this.cedulaEmpleado = cedulaEmpleado;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalTime getHoraEntradaManana() {
        return horaEntradaManana;
    }

    public void setHoraEntradaManana(LocalTime horaEntradaManana) {
        this.horaEntradaManana = horaEntradaManana;
    }

    public LocalTime getHoraSalidaManana() {
        return horaSalidaManana;
    }

    public void setHoraSalidaManana(LocalTime horaSalidaManana) {
        this.horaSalidaManana = horaSalidaManana;
    }

    public LocalTime getHoraEntradaTarde() {
        return horaEntradaTarde;
    }

    public void setHoraEntradaTarde(LocalTime horaEntradaTarde) {
        this.horaEntradaTarde = horaEntradaTarde;
    }

    public LocalTime getHoraSalidaTarde() {
        return horaSalidaTarde;
    }

    public void setHoraSalidaTarde(LocalTime horaSalidaTarde) {
        this.horaSalidaTarde = horaSalidaTarde;
    }

    public int getMinutosAtraso() {
        return minutosAtraso;
    }

    public void setMinutosAtraso(int minutosAtraso) {
        this.minutosAtraso = minutosAtraso;
    }

    @Override
    public String toString() {
        return "RegistroAsistencia{" + "cedula=" + cedulaEmpleado + ", fecha=" + fecha + ", atraso=" + minutosAtraso + "min}";
    }
}
