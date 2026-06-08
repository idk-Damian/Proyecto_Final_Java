package miproyectoequipo.vista;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Size;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

import miproyectoequipo.modelo.Usuario;
import miproyectoequipo.modelo.Empleado;
import miproyectoequipo.dao.EmpleadoDAO;
import miproyectoequipo.rostro.RostroManager;

public class ValidacionRostroDialog extends JDialog {

    private JLabel lblVideo;
    private JLabel lblMensaje;
    private OpenCVFrameGrabber grabber;
    private RostroManager rostroManager;
    private Usuario usuarioObjetivo;
    private Empleado empleadoObjetivo;

    private boolean validado = false;
    private boolean isRunning = true;

    public ValidacionRostroDialog(JFrame parent, Usuario usuario) {
        super(parent, "Validación Facial", true);
        this.usuarioObjetivo = usuario;

        EmpleadoDAO dao = new EmpleadoDAO();
        this.empleadoObjetivo = dao.buscarPorCedula(usuario.getCedula());

        this.rostroManager = new RostroManager();

        setSize(500, 500);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        lblVideo = new JLabel("Iniciando cámara...");
        lblVideo.setHorizontalAlignment(SwingConstants.CENTER);
        add(lblVideo, BorderLayout.CENTER);

        lblMensaje = new JLabel("Por favor, mire a la cámara para registrar su asistencia.");
        lblMensaje.setHorizontalAlignment(SwingConstants.CENTER);
        lblMensaje.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblMensaje.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(lblMensaje, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopCamera();
            }
        });

        startCamera();
    }

    public boolean isValidado() {
        return validado;
    }

    private void startCamera() {
        new Thread(() -> {
            try {
                grabber = new OpenCVFrameGrabber(0);
                grabber.start();

                OpenCVFrameConverter.ToMat converterMat = new OpenCVFrameConverter.ToMat();
                Java2DFrameConverter converterBimg = new Java2DFrameConverter();

                int matchCount = 0;

                while (isRunning && grabber != null && isVisible()) {
                    Frame frame = grabber.grab();
                    if (frame == null) continue;

                    Mat matImage = converterMat.convert(frame);
                    if (matImage == null) continue;

                    RectVector faces = rostroManager.detectarRostros(matImage);

                    if (faces.size() > 0) {
                        Rect rect = faces.get(0);
                        Mat faceMat = new Mat(matImage, rect);

                        int idReconocido = rostroManager.reconocerRostro(faceMat);

                        if (empleadoObjetivo != null && idReconocido == empleadoObjetivo.getId()) {
                            matchCount++;
                            rectangle(matImage, rect, new org.bytedeco.opencv.opencv_core.Scalar(0, 255, 0, 0), 2, 8, 0);

                            if (matchCount >= 5) {
                                validado = true;
                                isRunning = false;
                                SwingUtilities.invokeLater(() -> {
                                    lblMensaje.setText("✅ Identidad confirmada.");
                                    lblMensaje.setForeground(Color.GREEN.darker());
                                });
                                Thread.sleep(1000);
                                SwingUtilities.invokeLater(this::dispose);
                                break;
                            }
                        } else {
                            matchCount = 0;
                            rectangle(matImage, rect, new org.bytedeco.opencv.opencv_core.Scalar(0, 0, 255, 0), 2, 8, 0);
                        }
                    } else {
                        matchCount = 0;
                    }

                    BufferedImage bimg = converterBimg.convert(converterMat.convert(matImage));
                    if (bimg != null && isRunning) {
                        SwingUtilities.invokeLater(() -> lblVideo.setIcon(new ImageIcon(bimg)));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                stopCamera();
            }
        }).start();
    }

    private void stopCamera() {
        isRunning = false;
        if (grabber != null) {
            try {
                grabber.stop();
                grabber.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            grabber = null;
        }
    }
}
