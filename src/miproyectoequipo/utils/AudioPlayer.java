package miproyectoequipo.utils;

import java.io.FileInputStream;
import java.io.InputStream;
import javazoom.jl.player.Player;

public class AudioPlayer {

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
