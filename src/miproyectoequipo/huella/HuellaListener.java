
package miproyectoequipo.huella;

public interface HuellaListener {

    void onCapturaExitosa(byte[] imagen, byte[] template, String base64);

    void onCapturaError(String mensaje);

    void onDispositivoListo(int ancho, int alto);

    void onDispositivoError(String mensaje);

    void onProgresoEnroll(int capturaActual, int totalCapturas);

    void onEnrollCompleto(String templateBase64);

    void onIdentificacionResultado(String cedula, int score);
}
