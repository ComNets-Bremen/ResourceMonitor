package de.uni_bremen.comnets.resourcemonitor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

/**
 * Create a simple Pie Chart
 */

public class SimplePieChart extends View {
    public static final String TAG = SimplePieChart.class.getSimpleName();

    private RectF mRect = new RectF();
    private float mPercentage = 0.0f;
    private Context mContext;

    public SimplePieChart(Context context) {
        super(context);
        mContext = context;
    }

    public SimplePieChart(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public SimplePieChart(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
    }

    /**
     * Get the percentage of the current pie chart
     *
     * @return the percentage
     */
    public float getPercentage(){
        return mPercentage;
    }


    /**
     * Set the percentage of the current pie chart
     *
     * @param percentage The percentage
     */
    public void setPercentage(float percentage){
        mPercentage = percentage;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh){
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > h) {
            mRect.set(w / 2 - h / 2, 0, w / 2 + h / 2, h);
        } else {
            mRect.set(0, h / 2 - w / 2, w, h / 2 + w / 2);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Helper.getCanvasCircle(mContext, canvas, mPercentage, getWidth(), getHeight());

    }
}
