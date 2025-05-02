package com.example.facedetectionapp;


import android.content.Context;        // Provides access to application-specific resources and classes.
import android.graphics.Canvas;        // The class used for drawing shapes, text, and bitmaps.
import android.graphics.Color;         // Contains static methods and constants for colors.
import android.graphics.Paint;         // Holds the style and color information about how to draw geometries, text, and bitmaps.
import android.graphics.Rect;          // Represents a rectangle with integer coordinates. Used here for face boundaries.
import android.util.AttributeSet;     // Used when inflating a view from XML layout, contains attributes defined in the XML.
import android.util.Pair;           // A generic container for holding two related objects. Used here for landmark coordinates (x, y).
import android.view.View;            // The base class for widgets, which are used to create interactive UI components.

// Imports necessary Java utility classes.
import java.util.ArrayList;        // Resizable-array implementation of the List interface.
import java.util.List;             // An ordered collection (also known as a sequence).

/**
 * Custom view class designed to draw overlays on top of a camera preview or image.
 * Specifically, it draws rectangles around detected faces and dots for facial landmarks.
 * It extends the base Android View class, allowing it to be placed in layouts and draw custom content.
 */
public class FaceOverlayView extends View {

    // --- Member Variables ---

    // Paint object used for drawing the bounding boxes around detected faces.
    private Paint faceBoundsPaint;
    // Paint object used for drawing the facial landmarks (e.g., eyes, nose, mouth corners).
    private Paint landmarkPaint;

    // A list to store the rectangles (bounding boxes) for all detected faces in the current frame.
    // Each Rect defines the top, left, right, and bottom coordinates of a detected face.
    private List<Rect> faceBounds;
    // A list to store the coordinates of detected facial landmarks.
    // Each Pair contains the x (first) and y (second) coordinates of a landmark point.
    private List<Pair<Float, Float>> landmarks;

    // --- Constructors ---
    // These are standard constructors for an Android View. They allow the view to be created
    // either programmatically (using Context) or inflated from an XML layout file (using Context and AttributeSet).

    /**
     * Constructor used when creating the view programmatically.
     * @param context The Context the view is running in, through which it can access the current theme, resources, etc.
     */
    public FaceOverlayView(Context context) {
        super(context); // Calls the constructor of the parent class (View).
        init();         // Calls the initialization method to set up default values and objects.
    }

    /**
     * Constructor used when inflating the view from XML layout.
     * @param context The Context the view is running in.
     * @param attrs   The attributes of the XML tag that is inflating the view.
     */
    public FaceOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs); // Calls the constructor of the parent class (View).
        init();              // Calls the initialization method.
    }

    /**
     * Constructor used when inflating the view from XML layout with a default style attribute.
     * @param context The Context the view is running in.
     * @param attrs   The attributes of the XML tag that is inflating the view.
     * @param defStyleAttr An attribute in the current theme that contains a reference to a style resource
     * that supplies default values for the view. Can be 0 to not look for defaults.
     */
    public FaceOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr); // Calls the constructor of the parent class (View).
        init();                            // Calls the initialization method.
    }

    // --- Initialization ---

    /**
     * Initializes the Paint objects and the data lists.
     * This method is called by all constructors to ensure the view is properly set up.
     */
    private void init() {
        // Initialize the Paint for drawing face bounding boxes.
        faceBoundsPaint = new Paint();
        faceBoundsPaint.setColor(Color.GREEN);       // Set the color of the rectangle lines to green.
        faceBoundsPaint.setStyle(Paint.Style.STROKE); // Set the style to draw only the outline (stroke) of the rectangle.
        faceBoundsPaint.setStrokeWidth(4f);          // Set the thickness of the outline.

        // Initialize the Paint for drawing facial landmarks.
        landmarkPaint = new Paint();
        landmarkPaint.setColor(Color.YELLOW);      // Set the color of the landmark points to yellow.
        landmarkPaint.setStyle(Paint.Style.FILL);  // Set the style to fill the points (draw solid dots).
        // Note: setStrokeWidth affects the size of points drawn with drawPoint.
        landmarkPaint.setStrokeWidth(8f);          // Set the size of the landmark points.

        // Initialize the lists to store face bounds and landmarks.
        // Use ArrayList for dynamic resizing.
        faceBounds = new ArrayList<>();
        landmarks = new ArrayList<>();
    }

    // --- Public Methods ---

    /**
     * Updates the view with new face detection results.
     * This method should be called whenever new face detection data is available
     * (e.g., from a camera frame processor or image analysis).
     *
     * @param bounds A List of Rect objects, where each Rect represents the bounding box of a detected face.
     * @param points A List of Pair objects, where each Pair represents the (x, y) coordinates of a detected facial landmark.
     */
    public void updateFaceResults(List<Rect> bounds, List<Pair<Float, Float>> points) {
        // Clear the previous face bounds data.
        faceBounds.clear();
        // Add all the new face bounds rectangles to the list.
        faceBounds.addAll(bounds);

        // Clear the previous landmark data.
        landmarks.clear();
        // Add all the new landmark points to the list.
        landmarks.addAll(points);

        // Invalidate the view. This is crucial!
        // It tells the Android system that the view's content needs to be redrawn.
        // This will trigger a call to the onDraw() method.
        invalidate();
    }

    // --- Drawing Logic ---

    /**
     * This method is called by the Android system whenever the view needs to redraw itself.
     * This happens after invalidate() is called or when the view layout changes.
     * All custom drawing logic should be placed here.
     *
     * @param canvas The Canvas object on which the background will be drawn. You use this canvas
     * to draw your own content (the face overlays).
     */
    @Override
    protected void onDraw(Canvas canvas) {
        // Always call the superclass method first.
        // This takes care of drawing the view's background and other standard view drawing.
        super.onDraw(canvas);

        // --- Draw Face Bounding Boxes ---
        // Iterate through the list of face bounding boxes (Rect objects).
        for (Rect rect : faceBounds) {
            // Draw each rectangle onto the canvas using the coordinates defined in the Rect
            // and the style defined in faceBoundsPaint.
            canvas.drawRect(rect, faceBoundsPaint);
        }

        // --- Draw Facial Landmarks ---
        // Iterate through the list of landmark points (Pair<Float, Float> objects).
        for (Pair<Float, Float> point : landmarks) {
            // Draw each landmark as a point on the canvas.
            // point.first provides the x-coordinate.
            // point.second provides the y-coordinate.
            // The appearance of the point is defined by landmarkPaint.
            canvas.drawPoint(point.first, point.second, landmarkPaint);
        }
    }
}