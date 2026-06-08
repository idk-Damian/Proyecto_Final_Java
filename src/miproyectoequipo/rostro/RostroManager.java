package miproyectoequipo.rostro;

import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_face.*;
import org.bytedeco.opencv.opencv_objdetect.*;
import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.IntBuffer;

public class RostroManager {

    private CascadeClassifier faceDetector;
    private FaceRecognizer faceRecognizer;
    private static final String HAARCASCADE_PATH = "haarcascade_frontalface_default.xml";
    private static final String DATASET_DIR = "dataset_rostros";
    private static final String MODEL_FILE = "dataset_rostros/modelo_rostros.yml";

    public RostroManager() {
        File datasetDir = new File(DATASET_DIR);
        if (!datasetDir.exists()) {
            datasetDir.mkdir();
        }

        faceDetector = new CascadeClassifier(HAARCASCADE_PATH);
        if (faceDetector.empty()) {
            System.err.println("Error cargando " + HAARCASCADE_PATH);
        }

        faceRecognizer = LBPHFaceRecognizer.create();
        File modelFile = new File(MODEL_FILE);
        if (modelFile.exists()) {
            faceRecognizer.read(MODEL_FILE);
            System.out.println("Modelo de rostros cargado exitosamente.");
        }
    }

    public RectVector detectarRostros(Mat image) {
        Mat grayImage = new Mat();
        cvtColor(image, grayImage, COLOR_BGR2GRAY);
        equalizeHist(grayImage, grayImage);

        RectVector faces = new RectVector();
        faceDetector.detectMultiScale(grayImage, faces, 1.1, 3, 0, new Size(150, 150), new Size(500, 500));
        return faces;
    }

    public void guardarRostro(Mat image, int employeeId, int sampleNumber) {
        String filename = DATASET_DIR + "/" + employeeId + "_" + sampleNumber + ".jpg";
        imwrite(filename, image);
    }

    public boolean entrenarModelo() {
        File dir = new File(DATASET_DIR);
        FilenameFilter filter = (d, name) -> name.endsWith(".jpg");
        File[] files = dir.listFiles(filter);

        if (files == null || files.length == 0) {
            System.out.println("No hay imagenes para entrenar.");
            return false;
        }

        MatVector images = new MatVector(files.length);
        Mat labels = new Mat(files.length, 1, CV_32SC1);
        IntBuffer labelsBuf = labels.createBuffer();

        int counter = 0;
        for (File imageFile : files) {
            Mat img = imread(imageFile.getAbsolutePath(), IMREAD_GRAYSCALE);

            int label = Integer.parseInt(imageFile.getName().split("_")[0]);

            images.put(counter, img);
            labelsBuf.put(counter, label);
            counter++;
        }

        faceRecognizer.train(images, labels);
        faceRecognizer.save(MODEL_FILE);
        System.out.println("Modelo entrenado y guardado en " + MODEL_FILE);
        return true;
    }

    public int reconocerRostro(Mat faceImage) {
        IntPointer label = new IntPointer(1);
        DoublePointer confidence = new DoublePointer(1);

        Mat grayFace = new Mat();
        if (faceImage.channels() > 1) {
            cvtColor(faceImage, grayFace, COLOR_BGR2GRAY);
        } else {
            grayFace = faceImage;
        }

        Mat resized = new Mat();
        resize(grayFace, resized, new Size(160, 160));

        faceRecognizer.predict(resized, label, confidence);

        int predictedLabel = label.get(0);
        double conf = confidence.get(0);

        System.out.println("Predicción: " + predictedLabel + " Confianza: " + conf);

        if (conf < 75.0) {
            return predictedLabel;
        }
        return -1;
    }
}
