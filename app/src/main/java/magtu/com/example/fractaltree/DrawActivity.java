package magtu.com.example.fractaltree;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.*;
import android.graphics.*;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.*;
import java.util.Random;

public class DrawActivity extends Activity /*implements View.OnTouchListener*/ {
    private DrawThread gameThread;
    private Paint paint;
    private DisplayMetrics metrics;
    private float angle;
    private float branchCnt;
    private Random rand;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        angle = 0;
        branchCnt = 0.1f;
        rand = new Random();
        GameView view = new GameView(this);
        setContentView(view);
        hideUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameThread.setRunning(false);
        hideUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameThread.setRunning(false);
        hideUI();
    }

    // Function that turn on immersive mode
    public void hideUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    /**
     * On move change angle by X, and length by Y axis.
     * @param event above
     * @return return true if touch
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                // Do some stuff
                angle = Remap(event.getX(),0, metrics.widthPixels,
                        -180,180);
                branchCnt = Remap(event.getY(), metrics.heightPixels/2,
                        metrics.heightPixels,0.7f,0.1f);
        }
        return true;
    }

    private class GameView extends SurfaceView implements SurfaceHolder.Callback {

        public GameView(Context context) {
            super(context);
            gameThread = new DrawThread(getHolder(), context, this);
            getHolder().addCallback(this);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height) {
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            gameThread.setRunning(true);
            gameThread.start();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            boolean retry = true;
            // turn off thread
            gameThread.setRunning(false);
            while (retry) {
                try {
                    gameThread.join();
                    retry = false;
                } catch (InterruptedException e) {
                    // if cant turn off, then continue
                }
            }
        }
    }

    // Main class of all graphics with thread
    private class DrawThread extends Thread {
        private boolean runFlag = false;
        private final SurfaceHolder surfaceHolder;
        private long prevTime;
        long now, elapsedTime;

        /**
         * @param surfaceHolder
         * @param context
         */

        @SuppressLint("ClickableViewAccessibility")
        private DrawThread(SurfaceHolder surfaceHolder, Context context, GameView view) {
            this.surfaceHolder = surfaceHolder;
            metrics = context.getResources().getDisplayMetrics();

            // Paint
            paint = new Paint();
            paint.setAntiAlias(true);
            paint.setColor(Color.WHITE);
            // load components for graphics

            // save current time
            prevTime = System.currentTimeMillis();
        }


        private void setRunning(boolean run) {
            runFlag = run;
        }

        @Override
        public void run() {
            Canvas canvas;
            while (!runFlag) {
                try {
                    surfaceHolder.wait();
                } catch (InterruptedException ignored) {
                }
            }
            while (runFlag) {
                // get current time and calculate difference with older time
                now = System.currentTimeMillis();
                elapsedTime = now - prevTime;
                if (elapsedTime > 5) {
                    //if time > N milliseconds, save current time
                    prevTime = now;
                    updateFrame(); // picture update every N milliseconds
                } canvas = null;
                try {
                    // get Canvas and create drawings
                    canvas = surfaceHolder.lockCanvas(null);
                    if (canvas != null) synchronized (surfaceHolder) {
                        // graphics
                        canvas.drawColor(Color.BLACK);
                        draw(canvas, paint);
                    }
                } finally {
                    if (canvas != null) {
                        // if graphics done, set it on display
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }
            }
        }

        private void updateFrame() {
            // supporting method for graphics update
        }

        private void draw(Canvas canvas, Paint paint) {
            paint.setColor(Color.WHITE);
            // Start drawing from internal zero
            canvas.translate(metrics.widthPixels / 2, metrics.heightPixels);
            branch(canvas, paint, 250);
        }

        private void branch(Canvas canvas, Paint paint, float length) {
            //paint.setColor(Color.WHITE);
            paint.setARGB(rand.nextInt(), rand.nextInt(), rand.nextInt(), rand.nextInt());
            paint.setStrokeWidth(3);
            canvas.drawLine(0, 0, 0, -length, paint);
            canvas.translate(0, -length);
            if (length > 5) {
                    canvas.save();
                canvas.rotate(angle);
                branch(canvas, paint, length * branchCnt);
                    canvas.restore();
                    canvas.save();
                canvas.rotate(-angle);
                branch(canvas, paint, length * branchCnt);
                    canvas.restore();
            }
        }
    }

    /**
     * This function is remapping line value
     * Example:
     * 0 from 10
     * remap(4 from 0  to 10) = 40 from 0 to 100
     *
     * @param s  value to find
     * @param a1 start value 1
     * @param a2 stop value 1
     * @param b1 start value 2
     * @param b2 start value 2
     * @return returns a float value
     */
    private float Remap(float s, float a1, float a2, float b1, float b2) {
        return b1 + (s - a1) * (b2 - b1) / (a2 - a1);
    }
}
