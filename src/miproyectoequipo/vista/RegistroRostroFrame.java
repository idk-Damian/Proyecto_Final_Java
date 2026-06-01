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

import miproyectoequipo.modelo.Empleado;
import miproyectoequipo.rostro.RostroManager;

public class RegistroRostroFrame extends JFrame {

    private JLabel lblVideo;
    private JButton btnCapturar;
    private JLabel lblInfo;
    
    private OpenCVFrameGrabber grabber;
    private RostroManager rostroManager;
    private Empleado empleado;
    
    private boolean isCapturing = false;
    private int numMuestras = 0;
    private final int MUESTRAS_OBJETIVO = 20;

    public RegistroRostroFrame(Empleado empleado) {
        this.empleado = empleado;
        this.rostroManager = new RostroManager();
        
        setTitle("Registro Facial - " + empleado.getNombre());
        setSize(640, 560);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        lblVideo = new JLabel();
        lblVideo.setHorizontalAlignment(SwingConstants.CENTER);
        add(lblVideo, BorderLayout.CENTER);

        JPanel panelSur = new JPanel(new FlowLayout());
        btnCapturar = new JButton("Iniciar Captura de Muestras");
        lblInfo = new JLabel("Muestras: 0 / " + MUESTRAS_OBJETIVO);
        
        panelSur.add(btnCapturar);
        panelSur.add(lblInfo);
        add(panelSur, BorderLayout.SOUTH);

        btnCapturar.addActionListener(e -> {
            isCapturing = true;
            btnCapturar.setEnabled(false);
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopCamera();
            }
        });

        startCamera();
    }

    private void startCamera() {
        new Thread(() -> {
            try {
                grabber = new OpenCVFrameGrabber(0); // Camara por defecto
                grabber.start();

                OpenCVFrameConverter.ToMat converterMat = new OpenCVFrameConverter.ToMat();
                Java2DFrameConverter converterBimg = new Java2DFrameConverter();

                while (grabber != null && isVisible()) {
                    Frame frame = grabber.grab();
                    if (frame == null) continue;

                    Mat matImage = converterMat.convert(frame);
                    if (matImage == null) continue;

                    // Detectar rostro
                    RectVector faces = rostroManager.detectarRostros(matImage);

                    // Si estamos capturando y hay un rostro visible
                    if (isCapturing && faces.size() == 1) {
                        Rect rect = faces.get(0);
                        Mat faceMat = new Mat(matImage, rect);
                        
                        // Resize to 160x160
                        Mat resizedFace = new Mat();
                        org.bytedeco.opencv.global.opencv_imgproc.resize(faceMat, resizedFace, new Size(160, 160));

                        numMuestras++;
                        rostroManager.guardarRostro(resizedFace, empleado.getId(), numMuestras);
                        
                        SwingUtilities.invokeLater(() -> {
                            lblInfo.setText("Muestras: " + numMuestras + " / " + MUESTRAS_OBJETIVO);
                        });

                        if (numMuestras >= MUESTRAS_OBJETIVO) {
                            isCapturing = false;
                            SwingUtilities.invokeLater(() -> {
                                btnCapturar.setText("Completado");
                                JOptionPane.showMessageDialog(this, "Muestras capturadas. Entrenando modelo...");
                            });
                            
                            rostroManager.entrenarModelo();
                            
                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(this, "Modelo facial entrenado exitosamente.");
                                stopCamera();
                                dispose();
                            });
                            break;
                        }
                    }

                    // Dibujar rectángulo rojo sobre el rostro en pantalla
                    for (int i = 0; i < faces.size(); i++) {
                        Rect r = faces.get(i);
                        rectangle(matImage, r, new org.bytedeco.opencv.opencv_core.Scalar(0, 0, 255, 0), 2, 8, 0);
                    }

                    BufferedImage bimg = converterBimg.convert(converterMat.convert(matImage));
                    if (bimg != null) {
                        SwingUtilities.invokeLater(() -> {
                            lblVideo.setIcon(new ImageIcon(bimg));
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void stopCamera() {
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
