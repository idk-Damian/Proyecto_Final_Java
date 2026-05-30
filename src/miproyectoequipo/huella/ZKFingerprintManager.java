/*
 * Huella: Wrapper del SDK ZKTeco ZKFingerReader
 * Encapsula toda la interacción con el lector ZK9500
 */
package miproyectoequipo.huella;

import com.zkteco.biometric.FingerprintSensorErrorCode;
import com.zkteco.biometric.FingerprintSensorEx;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.swing.SwingUtilities;

/**
 * Gestiona toda la interacción con el lector de huellas ZKTeco ZK9500.
 * Usa el SDK ZKFinger Standard 5.3.0.33 (ZKFingerReader.jar).
 *
 * Funcionalidades:
 * - Inicialización/cierre del dispositivo
 * - Captura continua de huellas en hilo separado
 * - Registro (Enroll) con 3 capturas + merge
 * - Verificación 1:1 (DBMatch)
 * - Identificación 1:N (DBIdentify)
 *
 * @author Vladimir
 */
public class ZKFingerprintManager {

    // Constantes del SDK
    private static final int TEMPLATE_SIZE = 2048;
    private static final int ENROLL_COUNT = 3; // Capturas necesarias para registrar

    // Estado del dispositivo
    private long mhDevice = 0;   // Handle del dispositivo
    private long mhDB = 0;       // Handle de la base de datos interna del SDK
    private boolean mbStop = true;
    private int fpWidth = 0;
    private int fpHeight = 0;

    // Buffers
    private byte[] imgbuf = null;
    private byte[] template = new byte[TEMPLATE_SIZE];
    private int[] templateLen = new int[1];

    // Para Enroll (registro)
    private boolean bEnrollando = false;
    private int enrollIdx = 0;
    private byte[][] enrollTemplates = new byte[ENROLL_COUNT][TEMPLATE_SIZE];

    // Para Identificación - mapeo de FID a cédula
    private Map<Integer, String> fidToCedula = new HashMap<>();
    private int nextFid = 1;

    // Listener de eventos
    private HuellaListener listener;

    // Hilo de captura
    private Thread captureThread;

    /**
     * Establece el listener para recibir eventos del lector.
     * @param listener implementación de HuellaListener
     */
    public void setListener(HuellaListener listener) {
        this.listener = listener;
    }

    /**
     * Inicializa el SDK y abre el dispositivo.
     * @return true si se inicializó correctamente
     */
    public boolean iniciar() {
        if (mhDevice != 0) {
            notificarError("El dispositivo ya está abierto.");
            return false;
        }

        // Inicializar SDK
        if (FingerprintSensorErrorCode.ZKFP_ERR_OK != FingerprintSensorEx.Init()) {
            notificarError("Error al inicializar el SDK ZKTeco. Verifique que el driver esté instalado.");
            return false;
        }

        // Verificar dispositivos conectados
        int deviceCount = FingerprintSensorEx.GetDeviceCount();
        if (deviceCount < 1) {
            notificarError("No se detectó ningún lector de huellas. Conecte el ZK9500.");
            FingerprintSensorEx.Terminate();
            return false;
        }

        // Abrir primer dispositivo
        mhDevice = FingerprintSensorEx.OpenDevice(0);
        if (mhDevice == 0) {
            notificarError("No se pudo abrir el lector de huellas.");
            FingerprintSensorEx.Terminate();
            return false;
        }

        // Inicializar base de datos interna del SDK (para match/identify)
        mhDB = FingerprintSensorEx.DBInit();
        if (mhDB == 0) {
            notificarError("Error al inicializar la DB interna del SDK.");
            FingerprintSensorEx.CloseDevice(mhDevice);
            mhDevice = 0;
            FingerprintSensorEx.Terminate();
            return false;
        }

        // Obtener dimensiones de la imagen
        byte[] paramValue = new byte[4];
        int[] size = new int[]{4};
        FingerprintSensorEx.GetParameters(mhDevice, 1, paramValue, size);
        fpWidth = byteArrayToInt(paramValue);
        size[0] = 4;
        FingerprintSensorEx.GetParameters(mhDevice, 2, paramValue, size);
        fpHeight = byteArrayToInt(paramValue);

        imgbuf = new byte[fpWidth * fpHeight];

        // Iniciar hilo de captura
        mbStop = false;
        captureThread = new Thread(this::loopCaptura, "ZKFingerCapture");
        captureThread.setDaemon(true);
        captureThread.start();

        System.out.println("[ZKFinger] Dispositivo abierto. Imagen: " + fpWidth + "x" + fpHeight);

        if (listener != null) {
            SwingUtilities.invokeLater(() -> listener.onDispositivoListo(fpWidth, fpHeight));
        }

        return true;
    }

    /**
     * Carga huellas desde la BD al cache del SDK para identificación 1:N.
     * @param huellas Map con cedula → templateBase64
     */
    public void cargarHuellasEnCache(Map<String, String> huellas) {
        fidToCedula.clear();
        // Limpiar DB anterior
        if (mhDB != 0) {
            FingerprintSensorEx.DBFree(mhDB);
            mhDB = FingerprintSensorEx.DBInit();
        }
        nextFid = 1;

        for (Map.Entry<String, String> entry : huellas.entrySet()) {
            String cedula = entry.getKey();
            String base64 = entry.getValue();

            byte[] templateBytes = Base64.getDecoder().decode(base64);
            int ret = FingerprintSensorEx.DBAdd(mhDB, nextFid, templateBytes);
            if (ret == 0) {
                fidToCedula.put(nextFid, cedula);
                nextFid++;
            } else {
                System.err.println("[ZKFinger] Error al agregar huella de " + cedula + " al cache: " + ret);
            }
        }

        System.out.println("[ZKFinger] Cache cargado con " + fidToCedula.size() + " huellas.");
    }

    /**
     * Agrega una huella al cache del SDK (sin recargar todas).
     * @param cedula del empleado
     * @param templateBase64 template en Base64
     */
    public void agregarHuellaAlCache(String cedula, String templateBase64) {
        byte[] templateBytes = Base64.getDecoder().decode(templateBase64);
        int ret = FingerprintSensorEx.DBAdd(mhDB, nextFid, templateBytes);
        if (ret == 0) {
            fidToCedula.put(nextFid, cedula);
            nextFid++;
            System.out.println("[ZKFinger] Huella de " + cedula + " agregada al cache.");
        }
    }

    /**
     * Inicia el proceso de registro de huella (Enroll).
     * Se necesitan 3 capturas del mismo dedo.
     */
    public void iniciarEnroll() {
        enrollIdx = 0;
        bEnrollando = true;
        System.out.println("[ZKFinger] Modo Enroll activado. Coloque el dedo 3 veces.");
    }

    /**
     * Cancela el proceso de Enroll actual.
     */
    public void cancelarEnroll() {
        bEnrollando = false;
        enrollIdx = 0;
    }

    /**
     * Verifica si el modo Enroll está activo.
     * @return true si está enrollando
     */
    public boolean isEnrollando() {
        return bEnrollando;
    }

    /**
     * Verifica si el dispositivo está conectado y activo.
     * @return true si está activo
     */
    public boolean isActivo() {
        return mhDevice != 0 && !mbStop;
    }

    /**
     * Cierra el dispositivo y libera recursos.
     */
    public void cerrar() {
        mbStop = true;

        // Esperar a que el hilo termine
        if (captureThread != null) {
            try {
                captureThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (mhDB != 0) {
            FingerprintSensorEx.DBFree(mhDB);
            mhDB = 0;
        }

        if (mhDevice != 0) {
            FingerprintSensorEx.CloseDevice(mhDevice);
            mhDevice = 0;
        }

        FingerprintSensorEx.Terminate();
        System.out.println("[ZKFinger] Dispositivo cerrado.");
    }

    // =============================================
    // Hilo de captura continua
    // =============================================

    /**
     * Loop principal de captura. Corre en un hilo separado.
     * Detecta cuando se coloca un dedo y procesa la huella.
     */
    private void loopCaptura() {
        while (!mbStop) {
            templateLen[0] = TEMPLATE_SIZE;

            int ret = FingerprintSensorEx.AcquireFingerprint(mhDevice, imgbuf, template, templateLen);

            if (ret == FingerprintSensorErrorCode.ZKFP_ERR_OK) {
                // Huella capturada exitosamente
                byte[] capturedTemplate = new byte[templateLen[0]];
                System.arraycopy(template, 0, capturedTemplate, 0, templateLen[0]);

                String base64 = FingerprintSensorEx.BlobToBase64(template, templateLen[0]);

                // Notificar captura al listener
                if (listener != null) {
                    byte[] imgCopy = new byte[imgbuf.length];
                    System.arraycopy(imgbuf, 0, imgCopy, 0, imgbuf.length);
                    SwingUtilities.invokeLater(() -> listener.onCapturaExitosa(imgCopy, capturedTemplate, base64));
                }

                // Procesar según modo actual
                if (bEnrollando) {
                    procesarEnroll(capturedTemplate);
                } else {
                    procesarIdentificacion(capturedTemplate);
                }
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Procesa una captura durante el Enroll.
     * Necesita 3 capturas del mismo dedo, luego hace merge.
     */
    private void procesarEnroll(byte[] capturedTemplate) {
        // Verificar que no esté ya registrada
        int[] fid = new int[1];
        int[] score = new int[1];
        if (FingerprintSensorEx.DBIdentify(mhDB, capturedTemplate, fid, score) == 0) {
            String cedulaExistente = fidToCedula.get(fid[0]);
            if (listener != null) {
                SwingUtilities.invokeLater(() ->
                    listener.onCapturaError("Esta huella ya está registrada" +
                        (cedulaExistente != null ? " (cédula: " + cedulaExistente + ")" : "") + ". Cancelando enroll.")
                );
            }
            bEnrollando = false;
            enrollIdx = 0;
            return;
        }

        // Verificar que sea el mismo dedo (si no es la primera captura)
        if (enrollIdx > 0) {
            int matchResult = FingerprintSensorEx.DBMatch(mhDB, enrollTemplates[enrollIdx - 1], capturedTemplate);
            if (matchResult <= 0) {
                if (listener != null) {
                    SwingUtilities.invokeLater(() ->
                        listener.onCapturaError("Por favor, coloque el mismo dedo las 3 veces.")
                    );
                }
                return;
            }
        }

        // Guardar captura
        System.arraycopy(capturedTemplate, 0, enrollTemplates[enrollIdx], 0, capturedTemplate.length);
        enrollIdx++;

        if (listener != null) {
            int idx = enrollIdx;
            SwingUtilities.invokeLater(() -> listener.onProgresoEnroll(idx, ENROLL_COUNT));
        }

        // Si tenemos las 3 capturas, hacer merge
        if (enrollIdx >= ENROLL_COUNT) {
            int[] mergedLen = new int[]{TEMPLATE_SIZE};
            byte[] mergedTemplate = new byte[TEMPLATE_SIZE];

            int ret = FingerprintSensorEx.DBMerge(mhDB,
                    enrollTemplates[0], enrollTemplates[1], enrollTemplates[2],
                    mergedTemplate, mergedLen);

            if (ret == 0) {
                String base64Final = FingerprintSensorEx.BlobToBase64(mergedTemplate, mergedLen[0]);
                if (listener != null) {
                    SwingUtilities.invokeLater(() -> listener.onEnrollCompleto(base64Final));
                }
                System.out.println("[ZKFinger] Enroll exitoso. Template generado.");
            } else {
                if (listener != null) {
                    SwingUtilities.invokeLater(() ->
                        listener.onCapturaError("Error al fusionar las 3 capturas (código: " + ret + "). Intente de nuevo.")
                    );
                }
            }

            bEnrollando = false;
            enrollIdx = 0;
        }
    }

    /**
     * Procesa una captura para identificación 1:N.
     * Busca la huella en el cache del SDK.
     */
    private void procesarIdentificacion(byte[] capturedTemplate) {
        if (fidToCedula.isEmpty()) {
            return; // No hay huellas en cache, no intentar identificar
        }

        int[] fid = new int[1];
        int[] score = new int[1];
        int ret = FingerprintSensorEx.DBIdentify(mhDB, capturedTemplate, fid, score);

        if (ret == 0) {
            String cedula = fidToCedula.get(fid[0]);
            int sc = score[0];
            if (listener != null) {
                SwingUtilities.invokeLater(() -> listener.onIdentificacionResultado(cedula, sc));
            }
        } else {
            if (listener != null) {
                SwingUtilities.invokeLater(() -> listener.onIdentificacionResultado(null, 0));
            }
        }
    }

    // =============================================
    // Utilidades
    // =============================================

    /**
     * Guarda la imagen de la huella como archivo BMP.
     * @param imgBuf buffer de imagen del sensor
     * @param path ruta donde guardar el BMP
     */
    public void guardarImagenBMP(byte[] imgBuf, String path) {
        try {
            writeBitmap(imgBuf, fpWidth, fpHeight, path);
        } catch (IOException e) {
            System.err.println("[ZKFinger] Error al guardar imagen: " + e.getMessage());
        }
    }

    public int getFpWidth() {
        return fpWidth;
    }

    public int getFpHeight() {
        return fpHeight;
    }

    // =============================================
    // Funciones de utilidad del SDK (bitmap, bytes)
    // =============================================

    private static void writeBitmap(byte[] imageBuf, int nWidth, int nHeight, String path) throws IOException {
        FileOutputStream fos = new FileOutputStream(path);
        DataOutputStream dos = new DataOutputStream(fos);

        int w = (((nWidth + 3) / 4) * 4);
        int bfType = 0x424d;
        int bfSize = 54 + 1024 + w * nHeight;
        int bfOffBits = 54 + 1024;

        dos.writeShort(bfType);
        dos.write(intToByteArray(bfSize), 0, 4);
        dos.write(intToByteArray(0), 0, 2);
        dos.write(intToByteArray(0), 0, 2);
        dos.write(intToByteArray(bfOffBits), 0, 4);

        dos.write(intToByteArray(40), 0, 4);
        dos.write(intToByteArray(nWidth), 0, 4);
        dos.write(intToByteArray(nHeight), 0, 4);
        dos.write(intToByteArray(1), 0, 2);
        dos.write(intToByteArray(8), 0, 2);
        dos.write(intToByteArray(0), 0, 4);
        dos.write(intToByteArray(w * nHeight), 0, 4);
        dos.write(intToByteArray(0), 0, 4);
        dos.write(intToByteArray(0), 0, 4);
        dos.write(intToByteArray(0), 0, 4);
        dos.write(intToByteArray(0), 0, 4);

        for (int i = 0; i < 256; i++) {
            dos.writeByte(i);
            dos.writeByte(i);
            dos.writeByte(i);
            dos.writeByte(0);
        }

        byte[] filter = null;
        if (w > nWidth) {
            filter = new byte[w - nWidth];
        }

        for (int i = 0; i < nHeight; i++) {
            dos.write(imageBuf, (nHeight - 1 - i) * nWidth, nWidth);
            if (w > nWidth) {
                dos.write(filter, 0, w - nWidth);
            }
        }
        dos.flush();
        dos.close();
        fos.close();
    }

    private static byte[] intToByteArray(int number) {
        byte[] abyte = new byte[4];
        abyte[0] = (byte) (0xff & number);
        abyte[1] = (byte) ((0xff00 & number) >> 8);
        abyte[2] = (byte) ((0xff0000 & number) >> 16);
        abyte[3] = (byte) ((0xff000000 & number) >> 24);
        return abyte;
    }

    private static int byteArrayToInt(byte[] bytes) {
        int number = bytes[0] & 0xFF;
        number |= ((bytes[1] << 8) & 0xFF00);
        number |= ((bytes[2] << 16) & 0xFF0000);
        number |= ((bytes[3] << 24) & 0xFF000000);
        return number;
    }

    private void notificarError(String mensaje) {
        System.err.println("[ZKFinger] " + mensaje);
        if (listener != null) {
            SwingUtilities.invokeLater(() -> listener.onDispositivoError(mensaje));
        }
    }
}
