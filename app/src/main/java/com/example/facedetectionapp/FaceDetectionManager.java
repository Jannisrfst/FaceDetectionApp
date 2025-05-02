// Defines the package for organizing the code within the application.
package com.example.facedetectionapp;

// Import necessary Android graphics classes.
import android.graphics.Bitmap; // Represents an image bitmap, which will be the input for face detection.

// Import AndroidX annotation for nullability checks.
import androidx.annotation.NonNull; // Used to indicate that a parameter, field, or method return value can never be null.

// Import Google Mobile Services (GMS) Task API classes for handling asynchronous operations.
import com.google.android.gms.tasks.OnFailureListener; // Listener called when a Task fails.
import com.google.android.gms.tasks.OnSuccessListener; // Listener called when a Task successfully completes.

// Import ML Kit Vision API classes specifically for face detection.
import com.google.mlkit.vision.common.InputImage;      // Wrapper class for the image input provided to ML Kit detectors.
import com.google.mlkit.vision.face.Face;              // Represents a detected face and its associated attributes.
import com.google.mlkit.vision.face.FaceDetection;     // Entry point for acquiring a FaceDetector instance.
import com.google.mlkit.vision.face.FaceDetector;      // The main class that performs face detection.
import com.google.mlkit.vision.face.FaceDetectorOptions; // Configuration options for the FaceDetector.

// Import Java utility classes.
import java.util.List; // Interface for representing an ordered collection (used for the list of detected faces).

/**
 * Manages ML Kit face detection operations.
 * This class follows the Singleton pattern to ensure only one instance manages the
 * FaceDetector and its configuration throughout the application lifecycle.
 * It configures the detector, provides a method to process images, and handles callbacks
 * for success or failure.
 */
public class FaceDetectionManager {

    // --- Singleton Implementation ---

    // Static variable to hold the single instance of FaceDetectionManager.
    // Initialized to null. 'volatile' could be added for stronger thread safety if needed in complex scenarios.
    private static FaceDetectionManager INSTANCE;

    // --- Configuration Constants ---

    // Defines the minimum size of a face to be detected, relative to the image width.
    // e.g., 0.15f means faces smaller than 15% of the image width will be ignored.
    // Helps filter out very small or distant faces, potentially improving performance.
    private static final float MIN_FACE_SIZE = 0.15f;
    // Sets the desired performance mode for the detector.
    // FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE prioritizes accuracy over speed.
    // FaceDetectorOptions.PERFORMANCE_MODE_FAST prioritizes speed over accuracy.
    private static final int PERFORMANCE_MODE = FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE;

    // --- Member Variables ---

    // Holds the configuration options for the face detector.
    private final FaceDetectorOptions faceDetectorOptions;
    // The actual ML Kit FaceDetector instance used to process images.
    private final FaceDetector faceDetector;

    /**
     * Private constructor to prevent direct instantiation from outside the class (Singleton pattern).
     * Initializes the FaceDetectorOptions and the FaceDetector itself.
     */
    private FaceDetectionManager() {
        // Build the configuration options for the face detector.
        faceDetectorOptions = new FaceDetectorOptions.Builder()
                // Set the performance mode (trade-off between speed and accuracy).
                .setPerformanceMode(PERFORMANCE_MODE)
                // Set the minimum face size to detect.
                .setMinFaceSize(MIN_FACE_SIZE)
                // Enable face tracking. This assigns an ID to each detected face, allowing
                // tracking across consecutive frames. Useful for video processing.
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL) // Wichtig!

                .enableTracking()
                // Build the options object.
                .build();

        // Get an instance of the FaceDetector using the specified options.
        faceDetector = FaceDetection.getClient(faceDetectorOptions);
    }

    /**
     * Provides the global access point to the Singleton instance of FaceDetectionManager.
     * Creates the instance lazily (only when first requested).
     * The 'synchronized' keyword ensures thread safety during the first instantiation.
     *
     * @return The single instance of FaceDetectionManager.
     */
    public static synchronized FaceDetectionManager getInstance() {
        // If the instance hasn't been created yet...
        if (INSTANCE == null) {
            // ...create a new instance.
            INSTANCE = new FaceDetectionManager();
        }
        // Return the existing or newly created instance.
        return INSTANCE;
    }

    // --- Callback Interface ---

    /**
     * Interface definition for a callback mechanism.
     * Allows the calling code (e.g., an Activity or ViewModel) to receive the results
     * of the asynchronous face detection process.
     */
    public interface FaceDetectionCallback {
        /**
         * Called when face detection is successful.
         * @param faces A List of Face objects detected in the image. The list might be empty
         * if no faces were found meeting the criteria.
         */
        void onSuccess(List<Face> faces);

        /**
         * Called when face detection encounters an error.
         * @param e The Exception that occurred during the detection process.
         */
        void onError(Exception e);
    }

    // --- Public Methods ---

    /**
     * Performs face detection on the provided Bitmap image.
     * The operation is asynchronous; results are returned via the callback.
     *
     * @param bitmap The input image as an Android Bitmap.
     * @param callback The callback implementation to handle success or failure outcomes.
     */
    public void detectFaces(Bitmap bitmap, @NonNull final FaceDetectionCallback callback) {
        // Ensure the callback provided is not null. (Annotation helps, but a runtime check could be added).

        // Convert the Android Bitmap into an InputImage format required by ML Kit.
        // The second parameter 'rotationDegrees' is 0 assuming the bitmap orientation is correct.
        // If processing camera frames directly, you might need to calculate the correct rotation.
        InputImage inputImage = InputImage.fromBitmap(bitmap, 0);

        // Start the face detection process on the prepared input image.
        // This returns a Task<List<Face>>, representing an asynchronous operation.
        faceDetector.process(inputImage)
                // Attach a listener to handle the successful completion of the task.
                .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                    @Override
                    public void onSuccess(List<Face> faces) {
                        // When successful, invoke the onSuccess method of the provided callback,
                        // passing the list of detected faces.
                        callback.onSuccess(faces);
                    }
                })
                // Attach a listener to handle the failure of the task.
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // When failure occurs, invoke the onError method of the provided callback,
                        // passing the exception that caused the failure.
                        callback.onError(e);
                    }
                });
    }

    /**
     * Releases the resources used by the ML Kit FaceDetector.
     * It's important to call this when the detector is no longer needed (e.g., in onDestroy of
     * an Activity or Fragment) to prevent memory leaks.
     */
    public void shutdown() {
        // Close the face detector and release its underlying resources.
        if (faceDetector != null) {
            faceDetector.close();
        }
        // Note: Depending on lifecycle, you might also want to set INSTANCE = null here,
        // but typically Singleton instances live for the app's duration unless specifically reset.
    }
}