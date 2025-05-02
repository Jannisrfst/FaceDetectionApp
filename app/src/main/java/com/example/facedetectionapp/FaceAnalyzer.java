package com.example.facedetectionapp;

// Imports the Face class from Google's ML Kit Vision library.
// This class represents a detected face and contains information about its properties (landmarks, probabilities, etc.).
import com.google.mlkit.vision.face.Face;

/**
 * A utility class designed to analyze properties of a detected face using the ML Kit Face object.
 * It provides methods to determine common facial expressions and orientations like
 * eye openness, smiling probability, and head rotation.
 */
public class FaceAnalyzer {

    /**
     * Determines if both eyes of the detected face are likely open.
     * ML Kit provides probabilities for each eye being open.
     *
     * @param face The Face object detected by ML Kit Vision Face Detection.
     * @return true if both left and right eye open probabilities are above a threshold (0.5),
     * false otherwise, or if probabilities are not available (null).
     */
    public boolean areEyesOpen(Face face) {
        // Retrieve the probability that the left eye is open. This is a value between 0.0 and 1.0.
        // It might be null if the face detector wasn't configured to classify eyes or if it couldn't determine the state.
        Float leftEyeOpenProbability = face.getLeftEyeOpenProbability();
        // Retrieve the probability that the right eye is open. Similar conditions as the left eye.
        Float rightEyeOpenProbability = face.getRightEyeOpenProbability();

        // Check if either probability value is null. If classification was disabled or failed,
        // we cannot reliably determine eye state, so we default to returning false.
        if (leftEyeOpenProbability == null || rightEyeOpenProbability == null) {
            return false; // Return false if probabilities are unavailable.
        }

        // Check if both probabilities exceed a certain threshold (e.g., 0.5 or 50%).
        // This threshold can be adjusted based on desired sensitivity.
        // If both eyes have a high probability of being open, return true.
        return leftEyeOpenProbability > 0.5f && rightEyeOpenProbability > 0.5f;
    }

    /**
     * Determines if the detected face is likely smiling.
     * ML Kit provides a probability for the face smiling.
     *
     * @param face The Face object detected by ML Kit Vision Face Detection.
     * @return true if the smiling probability is above a threshold (0.7),
     * false otherwise, or if the probability is not available (null).
     */
    public boolean isSmiling(Face face) {
        // Retrieve the probability that the face is smiling. This is a value between 0.0 and 1.0.
        // It might be null if the face detector wasn't configured for classification or couldn't determine the state.
        Float smileProbability = face.getSmilingProbability();

        // Check if the probability value is null. If classification was disabled or failed,
        // we cannot reliably determine if the person is smiling, so we default to returning false.
        if (smileProbability == null) {
            return false; // Return false if the probability is unavailable.
        }

        // Check if the probability exceeds a certain threshold (e.g., 0.7 or 70%).
        // This threshold indicates a higher confidence that the person is smiling. It can be adjusted.
        // If the probability is high enough, return true.
        return smileProbability > 0.7f;
    }

    /**
     * A simple static nested class to hold the head rotation angles (Euler angles).
     * This acts as a data structure to conveniently return the three rotation values together.
     * Being static means it doesn't need an instance of FaceAnalyzer to be created.
     */
    public static class HeadRotation {
        // Rotation around the X-axis (nodding "yes"). In degrees.
        private final float x;
        // Rotation around the Y-axis (shaking "no"). In degrees. Positive Y is to the left.
        private final float y;
        // Rotation around the Z-axis (tilting head side-to-side). In degrees. Positive Z is counterclockwise.
        private final float z;

        /**
         * Constructor for the HeadRotation data class.
         * @param x Euler angle X (rotation around X-axis).
         * @param y Euler angle Y (rotation around Y-axis).
         * @param z Euler angle Z (rotation around Z-axis).
         */
        public HeadRotation(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        /** Getter for the X-axis rotation angle. */
        public float getX() { return x; }
        /** Getter for the Y-axis rotation angle. */
        public float getY() { return y; }
        /** Getter for the Z-axis rotation angle. */
        public float getZ() { return z; }
    }

    /**
     * Retrieves the head rotation angles (Euler angles) from the detected face.
     * These angles describe the orientation of the head in 3D space.
     *
     * @param face The Face object detected by ML Kit Vision Face Detection.
     * @return A HeadRotation object containing the Euler angles (X, Y, Z) in degrees.
     * Note: The ML Kit `getHeadEulerAngleX/Y/Z` methods return valid floats (not null),
     * typically 0 if contour detection or detailed tracking isn't enabled or possible.
     */
    public HeadRotation getHeadRotation(Face face) {
        // Get the rotation angle around the X-axis (up/down tilt).
        float eulerX = face.getHeadEulerAngleX();
        // Get the rotation angle around the Y-axis (left/right turn).
        float eulerY = face.getHeadEulerAngleY(); // Positive value means the face is turned towards the left of the image.
        // Get the rotation angle around the Z-axis (sideways tilt).
        float eulerZ = face.getHeadEulerAngleZ(); // Positive value means the face is tilted counterclockwise.

        // Create a new HeadRotation object to store and return these angles together.
        return new HeadRotation(eulerX, eulerY, eulerZ);
    }
}