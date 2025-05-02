package com.example.facedetectionapp;

// Android core imports for permissions, UI elements, Bitmaps, and Activity lifecycle.
import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

// AndroidX imports for core functionalities, AppCompatActivity, and CameraX components.
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

// Google Guava import for ListenableFuture, used extensively by CameraX APIs.
import com.google.common.util.concurrent.ListenableFuture;

// ML Kit Vision import for the Face class.
import com.google.mlkit.vision.face.Face;

// Java core imports for handling byte buffers and collections.
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

// Java concurrent imports for managing background threads and asynchronous tasks.
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An AppCompatActivity that demonstrates integrating CameraX for live preview and image capture
 * with ML Kit Face Detection. It displays the camera feed, allows capturing an image,
 * detects faces in the captured image, analyzes them for properties like eye openness and smiling,
 * and displays the results along with drawing bounding boxes around detected faces.
 *
 * This version also includes SQLite database integration to store and view face data.
 */
public class FaceDetectionActivity extends AppCompatActivity {

    // --- Constants ---
    // Request code used when asking for camera permission.
    private static final int REQUEST_CAMERA_PERMISSION = 10;
    // Array of permissions required by this activity. Currently, only CAMERA.
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};
    // Tag for logging
    private static final String TAG = "FaceDetectionActivity";

    // --- UI Views ---
    // Displays the live camera preview feed.
    private PreviewView previewView;
    // Custom view overlaid on top of the preview to draw face bounding boxes.
    private FaceOverlayView faceOverlayView;
    // Button to trigger image capture and face detection.
    private Button captureButton;
    // TextView to display information derived from face analysis.
    private TextView faceInfoTextView;
    // Button to view saved database entries
    private Button viewDataButton;

    // --- CameraX and Threading ---
    // Executor service to run camera-related tasks on a background thread.
    private ExecutorService cameraExecutor;
    // CameraX use case object for capturing still images.
    private ImageCapture imageCapture;

    // --- Face Detection Components ---
    // Manager class (Singleton) handling ML Kit face detector setup and execution.
    private FaceDetectionManager faceDetectionManager;
    // Utility class for analyzing specific features of a detected face.
    private FaceAnalyzer faceAnalyzer;

    // --- Database Components ---
    // Helper for SQLite database operations
    private DatabaseHelper dbHelper;

    /**
     * Called when the activity is first created. Responsible for layout inflation,
     * view initialization, setting up listeners, checking permissions, and starting the camera.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being
     * shut down then this Bundle contains the data it most recently
     * supplied in onSaveInstanceState(Bundle). Otherwise it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // Call the superclass implementation.
        // Set the user interface layout for this Activity.
        setContentView(R.layout.activity_face_detection);

        // --- Initialize UI Views ---
        // Find views by their IDs defined in the XML layout.
        previewView = findViewById(R.id.previewView);
        faceOverlayView = findViewById(R.id.faceOverlayView);
        captureButton = findViewById(R.id.captureButton);
        faceInfoTextView = findViewById(R.id.faceInfoTextView);
        viewDataButton = findViewById(R.id.viewDataButton);

        // --- Initialize Database ---
        dbHelper = new DatabaseHelper(this);

        // --- Initialize Background Executor ---
        // Create a single-threaded executor to handle camera operations off the main UI thread.
        cameraExecutor = Executors.newSingleThreadExecutor();

        // --- Initialize Face Detection Components ---
        // Get the singleton instance of the FaceDetectionManager.
        faceDetectionManager = FaceDetectionManager.getInstance();
        // Create an instance of the FaceAnalyzer utility class.
        faceAnalyzer = new FaceAnalyzer();

        // --- Permissions Check and Camera Startup ---
        // Check if all required permissions (CAMERA) are already granted.
        if (allPermissionsGranted()) {
            // If permissions are granted, start the camera setup process.
            startCamera();
        } else {
            // If permissions are not granted, request them from the user.
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CAMERA_PERMISSION
            );
        }

        // --- Set up UI Listeners ---
        // Set an onClick listener for the capture button. When clicked, it calls the captureImage() method.
        captureButton.setOnClickListener(v -> captureImage());
        Button loadSampleButton = findViewById(R.id.loadSampleButton);
        loadSampleButton.setOnClickListener(v -> loadSampleImage());

        // Set up listener for viewing database data
        viewDataButton.setOnClickListener(v -> showSavedFaceData());
    }

    /**
     * Initializes and starts the CameraX camera session.
     * Configures the Preview and ImageCapture use cases and binds them to the Activity's lifecycle.
     */
    private void startCamera() {
        // Get a future instance of ProcessCameraProvider. This is used to bind the lifecycle of cameras.
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        // Add a listener to the future. It will run when the CameraProvider is available.
        // The second argument specifies the executor to run the listener on (main thread here).
        cameraProviderFuture.addListener(() -> {
            try {
                // Retrieve the ProcessCameraProvider instance from the future.
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // --- Configure Preview Use Case ---
                // Build the Preview use case with default settings.
                Preview preview = new Preview.Builder().build();
                // Connect the Preview use case surface provider to the PreviewView UI element.
                // This directs the camera preview stream to the view.
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // --- Configure Image Capture Use Case ---
                // Build the ImageCapture use case.
                imageCapture = new ImageCapture.Builder()
                        // Set capture mode to prioritize lower latency, potentially sacrificing some quality.
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                // --- Select Camera ---
                // Choose the default back-facing camera. You could use DEFAULT_FRONT_CAMERA as well.
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // --- Bind Use Cases to Lifecycle ---
                // Unbind any existing use cases from the camera provider before rebinding.
                // This prevents errors if the camera was already in use.
                cameraProvider.unbindAll();

                // Bind the configured use cases (Preview, ImageCapture) to the camera provider
                // and the lifecycle of this Activity (`this`). This links camera operation to the Activity's state.
                cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture
                );

                // Log success or update UI (optional)
                // showToast("Camera started successfully");

            } catch (ExecutionException | InterruptedException exc) {
                // Handle exceptions during camera provider retrieval or binding.
                showToast("Camera initialization failed: " + exc.getMessage());
                // Log the error for debugging.
                Log.e(TAG, "Use case binding failed", exc);
            }
            // Ensure the listener runs on the main thread for UI updates or CameraX setup.
        }, ContextCompat.getMainExecutor(this));
    }

    /**
     * Captures a still image using the configured ImageCapture use case.
     * Handles the asynchronous result (success or error).
     */
    private void captureImage() {
        // Ensure the ImageCapture use case has been initialized.
        if (imageCapture == null) {
            showToast("Camera not ready");
            return;
        }

        // Initiate the image capture process.
        imageCapture.takePicture(
                // Specify the executor on which the callback methods (onCaptureSuccess, onError) will run.
                // Using the main executor is common for direct UI updates after capture.
                ContextCompat.getMainExecutor(this),
                // Provide a callback object to handle the image capture result.
                new ImageCapture.OnImageCapturedCallback() {
                    /**
                     * Called when the image capture is successful.
                     * @param imageProxy An object containing the captured image data and metadata.
                     * IMPORTANT: You must call imageProxy.close() when done to release resources.
                     */
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                        // Convert the captured ImageProxy object to an Android Bitmap.
                        Bitmap bitmap = imageToBitmap(imageProxy);
                        // Process the resulting Bitmap using the face detection manager.
                        processImage(bitmap);
                        // CRITICAL: Close the ImageProxy to release its underlying resources (like image buffers).
                        // Failure to close can prevent future captures.
                        imageProxy.close();
                    }

                    /**
                     * Called when image capture fails.
                     * @param exception An object describing the error that occurred.
                     */
                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        // Display an error message to the user.
                        showToast("Image capture failed: " + exception.getMessage());
                        // Log the error for debugging.
                        Log.e(TAG, "Image capture error", exception);
                    }
                }
        );
    }

    /**
     * Converts an ImageProxy object (from CameraX) to a correctly oriented Android Bitmap.
     *
     * @param imageProxy The ImageProxy containing the captured image data (likely in YUV or JPEG format).
     * @return A Bitmap object representing the image, rotated according to the ImageProxy's metadata.
     */
    private Bitmap imageToBitmap(ImageProxy imageProxy) {
        // Get the first plane of the image. For JPEG format, there's only one plane.
        // For YUV_420_888 (common default), Plane 0 contains the Y (luminance) data.
        // Here, we assume the underlying format is accessible as a single buffer (like JPEG).
        // If handling YUV directly, conversion would be more complex.
        ImageProxy.PlaneProxy planeProxy = imageProxy.getPlanes()[0];
        ByteBuffer buffer = planeProxy.getBuffer(); // Get the raw byte buffer.
        byte[] bytes = new byte[buffer.remaining()]; // Create a byte array of the correct size.
        buffer.get(bytes); // Copy buffer data into the byte array.

        // Decode the byte array into an initial Bitmap object.
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        // --- Handle Rotation ---
        // Get the rotation degrees needed to orient the image upright.
        // Camera sensors are often mounted rotated relative to the device's natural orientation.
        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();

        // If rotation is needed (not 0 degrees)...
        if (rotationDegrees != 0) {
            // Create a Matrix object to apply the rotation.
            Matrix matrix = new Matrix();
            matrix.postRotate(rotationDegrees);

            // Create a new Bitmap that is rotated correctly.
            // This creates a new Bitmap object; the original 'bitmap' is not modified directly.
            bitmap = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true // 'true' enables filtering for better quality
            );
        }

        // Return the potentially rotated Bitmap.
        return bitmap;
    }


    /**
     * Processes the captured Bitmap image using the FaceDetectionManager.
     * Handles the results asynchronously via a callback.
     *
     * @param bitmap The Bitmap image to process for face detection.
     */
    private void processImage(Bitmap bitmap) {
        // Call the face detection manager to detect faces in the provided bitmap.
        // Provide a callback implementation to handle success and error cases.
        faceDetectionManager.detectFaces(bitmap, new FaceDetectionManager.FaceDetectionCallback() {
            /**
             * Called when face detection completes successfully.
             * @param faces A List of Face objects detected in the image.
             */
            @Override
            public void onSuccess(List<Face> faces) {
                // Check if any faces were detected.
                if (faces.isEmpty()) {
                    showToast("No faces detected");
                    // Clear previous overlay drawings and info text.
                    runOnUiThread(() -> {
                        faceOverlayView.updateFaceResults(new ArrayList<>(), new ArrayList<>());
                        faceInfoTextView.setText("No faces detected");
                    });
                    return; // Nothing more to do if no faces are found.
                }

                // Log the number of faces detected
                Log.d(TAG, "Faces detected: " + faces.size());

                // Prepare lists to hold bounding boxes and landmarks for the overlay view.
                List<Rect> boundingBoxes = new ArrayList<>();
                List<Pair<Float, Float>> landmarks = new ArrayList<>();

                // Process the first detected face for detailed analysis and display.
                Face firstFace = faces.get(0); // Get the first face from the list.

                // Extract bounding box for the overlay.
                boundingBoxes.add(firstFace.getBoundingBox());

                // --- Analyze Face Features ---
                Integer faceId = firstFace.getTrackingId();
                boolean eyesOpen = faceAnalyzer.areEyesOpen(firstFace);
                boolean smiling = faceAnalyzer.isSmiling(firstFace);
                FaceAnalyzer.HeadRotation rotation = faceAnalyzer.getHeadRotation(firstFace);

                // --- Update UI ---
                updateFaceInfo(faceId != null ? faceId : -1, eyesOpen, smiling, rotation);

                // --- Save Face Data to Database ---
                saveFaceDataToDatabase(firstFace);

                // Update the FaceOverlayView to draw the bounding box(es).
                runOnUiThread(() -> {
                    faceOverlayView.updateFaceResults(boundingBoxes, landmarks);
                });
            }

            /**
             * Called when face detection fails.
             * @param e The exception that occurred.
             */
            @Override
            public void onError(Exception e) {
                // Show an error message to the user.
                showToast("Face detection failed: " + e.getMessage());
                // Log the detailed error.
                Log.e(TAG, "Face detection error", e);
                // Clear previous overlay drawings and info text on error.
                runOnUiThread(() -> {
                    faceOverlayView.updateFaceResults(new ArrayList<>(), new ArrayList<>());
                    faceInfoTextView.setText("Face detection error");
                });
            }
        });
    }

    /**
     * Updates the faceInfoTextView UI element with the results of the face analysis.
     * Ensures the UI update happens on the main thread.
     *
     * @param faceId   The tracking ID of the face (-1 if unavailable).
     * @param eyesOpen Boolean indicating if eyes are detected as open.
     * @param smiling  Boolean indicating if a smile is detected.
     * @param rotation HeadRotation object containing Euler angles (X, Y, Z).
     */
    private void updateFaceInfo(
            int faceId,
            boolean eyesOpen,
            boolean smiling,
            FaceAnalyzer.HeadRotation rotation
    ) {
        // Ensure UI updates happen on the main thread.
        runOnUiThread(() -> {
            // Format the analysis results into a readable string.
            String faceInfo = "Face ID: " + (faceId == -1 ? "N/A" : faceId) + "\n" +
                    "Eyes Open: " + eyesOpen + "\n" +
                    "Smiling: " + smiling + "\n" +
                    String.format("Rotation X: %.1f°", rotation.getX()) + "\n" +
                    String.format("Rotation Y: %.1f°", rotation.getY()) + "\n" +
                    String.format("Rotation Z: %.1f°", rotation.getZ());

            // Set the formatted text to the TextView.
            faceInfoTextView.setText(faceInfo);
        });
    }

    /**
     * Helper method to display a short Toast message on the UI thread.
     *
     * @param message The string message to display.
     */
    private void showToast(String message) {
        // Ensure the Toast is shown on the main UI thread.
        runOnUiThread(() -> {
            Toast.makeText(FaceDetectionActivity.this, message, Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Checks if all permissions defined in REQUIRED_PERMISSIONS have been granted.
     *
     * @return true if all permissions are granted, false otherwise.
     */
    private boolean allPermissionsGranted() {
        // Iterate through the list of required permissions.
        for (String permission : REQUIRED_PERMISSIONS) {
            // Check if the current permission is granted using ContextCompat for compatibility.
            if (ContextCompat.checkSelfPermission(this, permission) !=
                    PackageManager.PERMISSION_GRANTED) {
                // If any permission is not granted, return false immediately.
                return false;
            }
        }
        // If the loop completes without returning false, all permissions are granted.
        return true;
    }

    /**
     * Callback received when the user responds to the permission request dialog.
     *
     * @param requestCode  The request code passed to requestPermissions().
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions. Either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        // Call the superclass implementation first.
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Check if the result corresponds to our camera permission request.
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            // Check again if all required permissions were granted by the user.
            if (allPermissionsGranted()) {
                // If granted, start the camera.
                startCamera();
            } else {
                // If not granted, inform the user and close the activity.
                showToast("Permissions not granted by the user.");
                finish(); // Close the activity as it cannot function without camera permission.
            }
        }
    }

    /**
     * Called when the activity is being destroyed.
     * Releases resources like the camera executor and the face detector.
     */
    @Override
    protected void onDestroy() {
        // Call the superclass implementation first.
        super.onDestroy();
        // Shut down the background executor service to stop its thread and release resources.
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        // Shut down the face detection manager to release ML Kit resources.
        if (faceDetectionManager != null) {
            faceDetectionManager.shutdown();
        }
    }

    /**
     * Loads a sample image from resources for testing face detection.
     */
    private void loadSampleImage() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888; // Highest quality
        options.inScaled = false; // No automatic scaling

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.neutral_closed, options);

        if (bitmap != null) {
            processImage(bitmap);
        }
    }

    /**
     * Saves face data to the SQLite database.
     * Uses rotation angles and smile probability as the data points.
     *
     * @param face The Face object containing the data to save
     */
    private void saveFaceDataToDatabase(Face face) {
        if (face == null) {
            Log.e(TAG, "Attempted to save null face");
            return;
        }

        // Get rotation angles from the face using the analyzer
        FaceAnalyzer.HeadRotation rotation = faceAnalyzer.getHeadRotation(face);

        // Get smile probability (or use 0.0 if not available)
        Float smileProbability = face.getSmilingProbability();
        double smileValue = (smileProbability != null) ? smileProbability : 0.0;

        // Get writable database
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        // Prepare values for insert
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_VECTOR_X, rotation.getX());
        values.put(DatabaseHelper.COLUMN_VECTOR_Y, rotation.getY());
        values.put(DatabaseHelper.COLUMN_VECTOR_Z, rotation.getZ());
        values.put(DatabaseHelper.COLUMN_NUMMERISCHE_WERTE, smileValue);

        // Insert into database
        long id = database.insert(DatabaseHelper.TABLE_DATA, null, values);

        // Close the database
        database.close();

        // Provide feedback
        if (id != -1) {
            showToast("Face data saved to database (ID: " + id + ")");
        } else {
            showToast("Error saving face data");
        }
    }

    /**
     * Retrieves all saved face data from the database and displays it in a dialog.
     */
    private void showSavedFaceData() {
        // Get readable database
        SQLiteDatabase database = dbHelper.getReadableDatabase();

        // Query all saved data
        Cursor cursor = database.query(
                DatabaseHelper.TABLE_DATA,
                null,  // Select all columns
                null,  // No WHERE clause
                null,  // No WHERE parameters
                null,  // No GROUP BY
                null,  // No HAVING
                DatabaseHelper.COLUMN_PERSON_ID + " DESC"  // Order by ID, newest first
        );

        StringBuilder dataBuilder = new StringBuilder();

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PERSON_ID));
                float x = cursor.getFloat(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_VECTOR_X));
                float y = cursor.getFloat(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_VECTOR_Y));
                float z = cursor.getFloat(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_VECTOR_Z));
                double value = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NUMMERISCHE_WERTE));

                dataBuilder.append("ID: ").append(id)
                        .append(", X: ").append(String.format("%.1f", x)).append("°")
                        .append(", Y: ").append(String.format("%.1f", y)).append("°")
                        .append(", Z: ").append(String.format("%.1f", z)).append("°")
                        .append(", Smile: ").append(String.format("%.2f", value))
                        .append("\n\n");
            } while (cursor.moveToNext());
        } else {
            dataBuilder.append("No saved face data found.");
        }

        cursor.close();
        database.close();

        // Show the data in an alert dialog
        new AlertDialog.Builder(this)
                .setTitle("Saved Face Data")
                .setMessage(dataBuilder.toString())
                .setPositiveButton("OK", null)
                .setNegativeButton("Delete All", (dialog, which) -> {
                    // Delete all data if requested
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    db.delete(DatabaseHelper.TABLE_DATA, null, null);
                    db.close();
                    showToast("All saved data deleted");
                })
                .show();
    }
}