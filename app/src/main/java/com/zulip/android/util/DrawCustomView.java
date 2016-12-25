package com.zulip.android.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.zulip.android.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This class defines a custom view which is used to draw on a canvas.
 */

public class DrawCustomView extends View {
    //drawing path
    private Path drawPath;

    //defines what to draw
    private Paint canvasPaint;

    //defines how to draw
    private Paint drawPaint;

    //brush color
    private int paintColor;

    //canvas - holding pen, holds your drawings
    //and transfers them to the view
    private Canvas drawCanvas;

    //canvas bitmap
    private Bitmap canvasBitmap;

    //brush size
    private float currentBrushSize;

    // paths followed on canvas
    private ArrayList<Path> paths = new ArrayList<>();

    private Map<Path, Integer> colorsMap = new HashMap<>();

    // from https://android.googlesource.com/platform/development/+/master/samples/ApiDemos/src/com/example/android/apis/graphics/FingerPaint.java
    private float mX, mY;
    private static final float TOUCH_TOLERANCE = 4;

    public DrawCustomView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init(){
        currentBrushSize = getResources().getInteger(R.integer.brush_medium_size);
        paintColor = ContextCompat.getColor(getContext(), R.color.red_marker_tool);

        drawPath = new Path();
        drawPaint = new Paint();
        drawPaint.setColor(paintColor);
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(currentBrushSize);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);

        canvasPaint = new Paint(Paint.DITHER_FLAG);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        for (Path p : paths)
        {
            drawPaint.setColor(colorsMap.get(p));
            canvas.drawPath(p, drawPaint);
        }
        drawPaint.setColor(paintColor);
        canvas.drawPath(drawPath, drawPaint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        //create canvas of certain device size.
        super.onSizeChanged(w, h, oldw, oldh);

        //create Bitmap of certain w,h
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

        //apply bitmap to graphic to start drawing.
        drawCanvas = new Canvas(canvasBitmap);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                touch_start(touchX, touchY);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touch_move(touchX, touchY);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touch_up();
                invalidate();
                break;
            default:
                return false;
        }
        return true;
    }

    private void touch_start(float x, float y) {
        drawPath.reset();
        drawPath.moveTo(x, y);
        mX = x;
        mY = y;
    }

    private void touch_move(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            drawPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
            mX = x;
            mY = y;
        }
    }

    private void touch_up() {
        drawPath.lineTo(mX, mY);
        drawCanvas.drawPath(drawPath, drawPaint);
        paths.add(drawPath);
        // store the color of mPath
        colorsMap.put(drawPath, paintColor);
        drawPath = new Path();
    }

    public void onClickUndo () {
        if (paths.size()>0)
        {
            Path path = paths.remove(paths.size()-1);
            invalidate();
        }
    }

    public void setBrushColor(int color) {
        paintColor = color;
    }
}
