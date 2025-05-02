# Face Detection Android App

## Overview

This Android application performs real-time face detection and analysis using on-device machine learning. It utilizes Google's ML Kit and the CameraX API to identify, track, and analyze facial features from the device's camera feed or static images. The core principle is to demonstrate on-device ML capabilities without relying on cloud services, ensuring privacy and offline functionality.

## Features

* **Real-time Face Detection:** Detects and visually marks faces in live camera previews and static images.
* **Facial Feature Analysis:** Analyzes features like eye-opening probability, smiling probability, and head rotation (pitch, yaw, roll).
* **On-Device Processing:** All analysis happens locally on the device, ensuring user privacy.
* **Visual Feedback:** Displays bounding boxes around detected faces and shows analysis results in an intuitive UI.

## Technology Stack

* 🤖 **Backend/Core Logic:** Android (Java/Kotlin - inferred from Android context)
* <0xF0><0x9F><0xA7><0xA0> **Machine Learning:** Google ML Kit Face Detection
* 📸 **Camera:** Android CameraX API
* 📱 **UI:** Android Views, ConstraintLayout

## Architecture

The application follows the Separation of Concerns principle, dividing functionality into:

1.  **UI Component (`FaceDetectionActivity`):** Manages user interface, interactions, and coordinates other components.
2.  **Face Detection Manager (`FaceDetectionManager`):** Encapsulates ML Kit initialization, configuration, and processing calls (using Singleton pattern).
3.  **Analysis Component (`FaceAnalyzer`):** Interprets raw ML Kit results into meaningful information (e.g., probabilities to boolean states).
4.  **Visualization Component (`FaceOverlayView`):** Custom view to draw overlays (like bounding boxes) on the image feed.

Design patterns like Observer (Callbacks), Builder, and Singleton are utilized.

## System Requirements

* Android API Level 24 (Android 7.0 Nougat) or higher.
* Device Camera.
* Minimum 2 GB RAM recommended.
* Approx. 50 MB free storage.

## Installation

1.  Clone the repository: `git clone <repository-url>`
2.  Open the project in Android Studio.
3.  Allow Gradle to sync and download dependencies. Ensure necessary Android SDK components are installed.
4.  Connect an Android device (API >= 24, USB Debugging enabled) or start an emulator.
5.  Select the target device in Android Studio.
6.  Run the app (`Shift+F10` or Run > Run 'app').

## Usage

1.  Launch the app.
2.  Grant camera permission when prompted.
3.  Point the camera at a face.
4.  Tap "Capture Image" to analyze a single frame from the camera. Results (bounding box, details) will be displayed.
5.  Tap "Load Sample Image" to analyze a predefined test image.

## Key Components

* `FaceDetectionActivity.java`: Main activity, handles UI, permissions, camera setup, and coordination.
* `FaceDetectionManager.java`: Singleton class managing the ML Kit `FaceDetector` instance and processing requests.
* `FaceAnalyzer.java`: Interprets raw ML Kit `Face` object data (probabilities, angles) into usable formats.
* `FaceOverlayView.java`: Custom `View` for drawing bounding boxes and potentially other visuals over the camera preview.

## ML Kit Integration

* **Configuration:** Uses `FaceDetectorOptions.Builder` to set parameters like performance mode (`ACCURATE` vs `FAST`), classification mode (detecting eyes open/smile), landmark mode, minimum face size, and tracking.
* **Processing:** Takes a `Bitmap` or `InputImage` and passes it to the `FaceDetector`'s `process()` method.
* **Results:** Asynchronously returns a list of `Face` objects containing bounding boxes, tracking IDs (if enabled), Euler angles (head rotation), classification probabilities (eyes open, smiling), and landmarks (if enabled).

## Privacy Considerations

* **On-Device Processing:** All face detection and analysis occurs *only* on the user's device.
* **No Data Transmission:** No images or analysis data are sent to external servers.
* **No Data Storage:** The app does not permanently store images or analysis results (in its current version). Data is held temporarily in memory for analysis.
* **No Identification:** The app detects face features but does not perform biometric identification or recognition. Tracking IDs are for short-term tracking within a session only.

## Known Limitations & Potential Improvements

* **Limitations:** Performance depends on device hardware; detection can struggle with extreme angles, poor lighting, or occlusions; classification probabilities can fluctuate; tracking IDs are not persistent across static image analyses.
* **Future Enhancements:**
    * Implement continuous analysis using CameraX `ImageAnalysis` use case.
    * Apply temporal smoothing to results.
    * Use adaptive classification thresholds.
    * Visualize face landmarks.
    * Improve UI for handling multiple detected faces.
    * Optimize image conversion (YUV to Bitmap).
    * Add more robust error handling and user feedback.
    * Planned SQLite integration (considering vector storage or encrypted biometrics).

## Authors

* Jannis Reufsteck


## License

Licensed under MIT.
