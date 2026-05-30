package miproyectoequipo.utils;

import java.io.FileInputStream;
import java.io.InputStream;
import javazoom.jl.player.Player;

/**
 * Utilidad para reproducir archivos de audio MP3 usando la librería JLayer.
 */
public class AudioPlayer {

    /**
     * Reproduce un archivo MP3 en un hilo separado para no bloquear la interfaz.
     * @param rutaArchivo Ruta relativa o absoluta al archivo MP3
     */
    public static void reproducirMP3(String rutaArchivo) {
        new Thread(() -> {
            try {
                InputStream is = new FileInputStream(rutaArchivo);
                Player player = new Player(is);
                player.play();
                player.close();
            } catch (Exception e) {
                System.err.println("[AudioPlayer] Error al reproducir audio: " + e.getMessage());
            }
        }, "AudioPlayerThread").start();
    }
}
