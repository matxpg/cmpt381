package com.example.wizard.cmpt381;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.example.wizard.cmpt381.DrawingTools.ACreator;
import com.example.wizard.cmpt381.DrawingTools.CircleOperation;
import com.example.wizard.cmpt381.DrawingTools.IDrawOperation;
import com.example.wizard.cmpt381.DrawingTools.SquareOperation;
import com.example.wizard.cmpt381.DrawingTools.TriangleOperation;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Class to handle All things Draw-related.
 * paints, paths, drawing operations on a canvas, canvases, bitmaps, listeners, onTouch, etc.
 */


public class DrawManager implements OnTouchListener {

    private static final int MAX_UNDO = 10;
    private static final String TAG = "DrawManager";
    private final Paint fDitherPaint = new Paint(Paint.DITHER_FLAG);
    private LinkedList<IDrawOperation> fOperations;
    private Map<Integer, ACreator> fCreators;
    private IDrawOperation fCurrentOperation = null;
    private ACreator fCurrentCreator = null;
    //Initialized onSizeChanged in view
    private Bitmap fBackgroundImage;
    private Canvas fBackgroundCanvas;
    private Bitmap fBackgroundImageBackUP;
    private Canvas fBackgroundCanvasBackUP;
    private boolean initialized = false; //Gets fully initialized on screen size changed, with the bitmaps
    private PaintState fPaintState = new PaintState();

    public DrawManager() {
        fOperations = new LinkedList<IDrawOperation>();
        fCreators = new HashMap<Integer, ACreator>();
        fCurrentCreator = null;
    }

    public PaintState getPaintState() {
        return fPaintState;
    }

    public void setup(int aWidth, int aHeight) {
        if (!initialized) {
            Log.d(TAG, "initializing DrawManager");
            fBackgroundImage = Bitmap.createBitmap(aWidth, aHeight, Bitmap.Config.ARGB_8888);
            fBackgroundCanvas = new Canvas(fBackgroundImage);
            fBackgroundImageBackUP = Bitmap.createBitmap(aWidth, aHeight, Bitmap.Config.ARGB_8888);
            fBackgroundCanvasBackUP = new Canvas(fBackgroundImageBackUP);
            Log.d(TAG, "Created Background and Backup, and Canvases with dimensions:\n" +
                    " width: " + aWidth + " height: " + aHeight);
//            fBackgroundCanvas.drawColor(Color.WHITE);
//            fBackgroundCanvasBackUP.drawColor(Color.WHITE);
            this.initialized = true;
        }
    }

    public void undo() {
        synchronized (fBackgroundCanvas) {
            if (fOperations.size() != 0) {
                IDrawOperation op = fOperations.removeLast();
                op.undo();
                backup();
            }
        }
    }


    private void backup() {
        fBackgroundCanvas.drawBitmap(fBackgroundImageBackUP, 0, 0, fDitherPaint);
        for (IDrawOperation op : fOperations) {
            op.draw(fBackgroundCanvas);
        }
    }


    public void addTool(int aKey, ACreator aDrawingTool) {
        fCreators.put(aKey, aDrawingTool);
    }

    public void setCurrentTool(int aKey) {
        if (fCreators.containsKey(aKey)) {
            fCurrentCreator = fCreators.get(aKey);
        }
    }

    public ACreator getCurrentCreator() {
        return fCurrentCreator;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        Log.d(TAG, "onTouch");
        return onTouch(view, new MyMotionEvent(motionEvent.getAction(), motionEvent.getX(), motionEvent.getY()));
    }

    public boolean onTouch(View view, MyMotionEvent motionEvent) {
        int action = motionEvent.getAction();

        float point[] = new float[]{motionEvent.getX(), motionEvent.getY()};

        if (fCurrentCreator != null) {
            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    addOperation(fCurrentCreator.startDrawingOperation(point[0], point[1]));
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    fCurrentCreator.updateDrawingOperation(point[0], point[1]);
                    break;
                }
                case MotionEvent.ACTION_UP: {
                    fCurrentCreator.endDrawingOperation();
                    finishOp();
                    break;
                }
                default:
                    return false;
            }
        }
        return true;
    }

    public void addOperation(IDrawOperation op) {
        synchronized (fBackgroundCanvas) {
            fCurrentOperation = op;
            fOperations.addLast(fCurrentOperation);
            while (fOperations.size() > MAX_UNDO) {
                op = fOperations.removeFirst();
                op.complete();
                op.draw(fBackgroundCanvasBackUP);
            }
        }
    }

    private void finishOp() {
        synchronized (fBackgroundCanvas) {
            fCurrentOperation.draw(fBackgroundCanvas);
            fCurrentOperation = null;
        }
    }

    public void draw(Canvas aCanvas) {
        synchronized (fBackgroundCanvas) {
            aCanvas.drawBitmap(fBackgroundImage, 0.f, 0.f, fDitherPaint);
            if (fCurrentOperation != null) {
                fCurrentOperation.draw(aCanvas);
            }
        }
    }

    public void setBackgroundCircle() {

        synchronized (fBackgroundCanvas) {
            fBackgroundCanvas.drawColor(Color.GRAY);   //     fBackgroundCanvas.drawColor(fPaintState.getColor() | 0xff000000);
        }
        float rectPadding = 30f;
        Paint p = new Paint();
        p.setColor(Color.WHITE);
        //MAYBE WHITE?
        p.setStyle(Paint.Style.FILL);
        RectF rect = new RectF();
        rect.set(rectPadding, rectPadding, getWidth() - rectPadding, getHeight() - rectPadding);
        CircleOperation op = new CircleOperation(p, rect);
        addOperation(op);
    }

    public void setBackgroundTriangle() {

        synchronized (fBackgroundCanvas) {
            fBackgroundCanvas.drawColor(Color.GRAY);   //     fBackgroundCanvas.drawColor(fPaintState.getColor() | 0xff000000);
        }
        float rectPadding = 30f;

        RectF rect = new RectF();
        rect.set(rectPadding, rectPadding, getWidth() - rectPadding, getHeight() - rectPadding);
        TriangleOperation op = new TriangleOperation(new Paint(), rect);
        addOperation(op);
    }

    public void setBackgroundRectangle() {

        synchronized (fBackgroundCanvas) {
            fBackgroundCanvas.drawColor(Color.GRAY);   //     fBackgroundCanvas.drawColor(fPaintState.getColor() | 0xff000000);
        }
        float rectPadding = 30f;
        Paint p = new Paint();
        p.setColor(Color.WHITE);
        p.setStyle(Paint.Style.FILL);
        RectF rect = new RectF();
        rect.set(rectPadding, rectPadding, getWidth() - rectPadding, getHeight() - rectPadding);

        SquareOperation op = new SquareOperation(p, rect);
        addOperation(op);
    }


    public int getWidth() {
        return fBackgroundImage.getWidth();
    }

    public int getHeight() {
        return fBackgroundImage.getHeight();
    }

    public Bitmap copyBitmap() {
        Bitmap result = Bitmap.createBitmap(fBackgroundImage.getWidth(), fBackgroundImage.getHeight(), Bitmap.Config.ARGB_8888);
        draw(new Canvas(result));
        return result;
    }

    public Bitmap getBitmap() {
        return fBackgroundImage;
    }

    public void putBitmapAsBackground(Bitmap aBitmap) {

        float width = getWidth();
        float height = getHeight();

        float bitmapWidth = aBitmap.getWidth();
        float bitmapHeight = aBitmap.getHeight();

        Matrix matrix = new Matrix();
        float dx, dy, scale;

        if (width > height) {
            // canvas is in on paysage mode
            if (bitmapHeight > bitmapWidth) {
                // bitmap is in on portrait mode
                matrix.setRotate(-90);
                matrix.postTranslate(0, bitmapWidth);

                float temp = bitmapWidth;
                bitmapWidth = bitmapHeight;
                bitmapHeight = temp;
            }

            scale = width / bitmapWidth;
            if (scale * bitmapHeight > height) {
                scale = height / bitmapHeight;
                dx = (width - scale * bitmapWidth) / 2.f;
                dy = 0.f;
            } else {
                dx = 0;
                dy = (height - scale * bitmapHeight) / 2.f;
            }
        } else {
            // canvas is in on portrait mode
            if (bitmapWidth > bitmapHeight) {
                // bitmap is in on paysage mode
                matrix.setRotate(90);
                matrix.postTranslate(bitmapHeight, 0);

                float temp = bitmapWidth;
                bitmapWidth = bitmapHeight;
                bitmapHeight = temp;
            }

            scale = height / bitmapHeight;
            if (scale * bitmapWidth > bitmapWidth) {
                scale = width / bitmapWidth;
                dx = 0;
                dy = (height - scale * bitmapHeight) / 2.f;
            } else {
                dx = (width - scale * bitmapWidth) / 2.f;
                dy = 0.f;
            }
        }
        matrix.postScale(scale, scale);
        matrix.postTranslate(dx, dy);

        synchronized (fBackgroundCanvas) {
            //     fBackgroundCanvas.drawColor(fPaintState.getColor() | 0xff000000);
            fOperations.clear();
            fBackgroundCanvas.drawBitmap(aBitmap, matrix, null);
            fBackgroundCanvasBackUP.drawBitmap(aBitmap, matrix, null);
            for (Map.Entry<Integer, ACreator> creator : fCreators.entrySet()) {
                creator.getValue().clear();
            }
        }
    }

    static class MyMotionEvent {

        int action;
        float x;
        float y;

        public MyMotionEvent(int aAction, float aX, float aY) {
            action = aAction;
            x = aX;
            y = aY;
        }

        public int getAction() {
            return action;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }
    }
}