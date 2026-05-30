/*
 * Huella: Interface de callbacks para eventos del lector
 */
package miproyectoequipo.huella;

/**
 * Interface para recibir eventos del lector de huellas ZKTeco.
 * Implementar en las vistas que necesiten capturar huellas.
 * 
 * @author Vladimir
 */
public interface HuellaListener {

    /**
     * Se invoca cuando se captura una huella exitosamente.
     * 
     * @param imagen bytes de la imagen de la huella (para visualización BMP)
     * @param template bytes del template biométrico
     * @param base64 template en formato Base64 (para almacenamiento)
     */
    void onCapturaExitosa(byte[] imagen, byte[] template, String base64);

    /**
     * Se invoca cuando hay un error en la captura.
     * 
     * @param mensaje descripción del error
     */
    void onCapturaError(String mensaje);

    /**
     * Se invoca cuando el dispositivo se conecta y está listo.
     * 
     * @param ancho ancho de la imagen del sensor
     * @param alto alto de la imagen del sensor
     */
    void onDispositivoListo(int ancho, int alto);

    /**
     * Se invoca cuando el dispositivo se desconecta o hay error.
     * 
     * @param mensaje razón de la desconexión
     */
    void onDispositivoError(String mensaje);

    /**
     * Se invoca durante el proceso de enroll (registro de huella).
     * 
     * @param capturaActual número de captura actual (1, 2 o 3)
     * @param totalCapturas total de capturas necesarias (3)
     */
    void onProgresoEnroll(int capturaActual, int totalCapturas);

    /**
     * Se invoca cuando el enroll se completa exitosamente.
     * 
     * @param templateBase64 template final fusionado en Base64
     */
    void onEnrollCompleto(String templateBase64);

    /**
     * Se invoca con el resultado de una identificación 1:N.
     * 
     * @param cedula cédula del empleado identificado, null si no se encontró
     * @param score puntuación de la coincidencia
     */
    void onIdentificacionResultado(String cedula, int score);
}
