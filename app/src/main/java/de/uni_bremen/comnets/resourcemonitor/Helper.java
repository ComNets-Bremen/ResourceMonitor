package de.uni_bremen.comnets.resourcemonitor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.PeriodicSync;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.PowerManager;
import android.support.annotation.ColorInt;
import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.webkit.WebView;
import android.widget.TextView;

/**
 * Some helper classes required for several different modules
 */

public class Helper {
    public static final String TAG = MonitorService.class.getSimpleName();

    /**
     * Show a Message dialog to the user
     *
     * @param context   The application context
     * @param s         The message as a String object
     * @param title     The title of the message box
     */
    static void showUserMessage(Context context, String s, String title) {
        Spanned span = new SpannedString(s);
        showUserMessage(context, span, title);
    }

    /**
     * Show a Message dialog to the user
     *
     * @param context The app context
     * @param msg     The message as a Spanned object
     * @param title   The title of the message box
     * @param listener A onclick listener
     */
    static void showUserMessage(Context context, Spanned msg, String title, DialogInterface.OnClickListener listener) {
        AlertDialog dlgAlert = new AlertDialog.Builder(context)
                .setTitle(title)
                .setPositiveButton(context.getText(R.string.button_ok), listener)
                .setMessage(msg)
                .setIcon(android.R.drawable.ic_dialog_info)
                .create();
        dlgAlert.show();
        TextView tv = (TextView) dlgAlert.findViewById(android.R.id.message);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        tv.setClickable(true);
    }

    /**
     * Show a Message dialog to the user
     *
     * @param context The app context
     * @param msg     The message as a Spanned object
     * @param title   The title of the message box
     */
    static void showUserMessage(Context context, Spanned msg, String title) {
        showUserMessage(context, msg, title, null);
    }



    /**
     * Show a HTML message dialog to the user
     *
     * @param context   The application context
     * @param text      The HTML text message
     * @param title     The dialog title
     * @param listener  The onClick listener
     */
    static void showHTMLUserMessage(Context context, String text, String title, DialogInterface.OnClickListener listener) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            showUserMessage(context, Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY), title, listener);
        } else {
            showUserMessage(context, Html.fromHtml(text), title, listener);
        }
    }

    /**
     * Show a HTML message dialog to the user
     *
     * @param context   The application context
     * @param text      The HTML text message
     * @param title     The dialog title
     */
    static void showHTMLUserMessage(Context context, String text, String title) {
        showHTMLUserMessage(context, text, title, null);
    }

    /**
     * Show HTML text using a WebView
     * @param context   The context
     * @param text      The HTML text
     * @param title     The title of the dialog
     * @param listener  A DialogInterface.OnClickListener
     */
    static void showWebviewAlert(Context context, String text, String title, DialogInterface.OnClickListener listener){
        WebView webView = new WebView(context);
        webView.loadData(text, "text/html", "utf-8");
        webView.setClickable(true);

        AlertDialog dlgAlert = new AlertDialog.Builder(context)
                .setTitle(title)
                .setPositiveButton(context.getText(R.string.button_ok), listener)
                .setView(webView)
                .setIcon(android.R.drawable.ic_dialog_info)
                .create();
        dlgAlert.show();
    }

    /**
     * Checks whether doze is active for this app or not
     *
     * @param context   The app context
     * @return true if doze is active
     */
    public static boolean isPowerSaving(Context context){
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            Log.d(TAG, "isIgnoringBatteryOpt: " + pm.isIgnoringBatteryOptimizations(context.getPackageName()));
            return !pm.isIgnoringBatteryOptimizations(context.getPackageName());
        } else {
            return false;
        }
    }

    /**
     * Create a human readable string from a period of time, i.e. 12 days, 15 hours, 12 seconds
     *
     * @param timespan      The timespan in milliseconds
     * @param shortValues   Shold short values be used ("s" vs "second")
     * @param ctx           The application context
     * @return              A string with the requested information
     */
    public static String timePeriodFormat(long timespan, boolean shortValues, Context ctx){
        long SECOND = 1000;
        long MINUTE = SECOND * 60;
        long HOUR = MINUTE * 60;
        long DAY = HOUR * 24;
        long YEAR = DAY * 365;

        StringBuilder sb = new StringBuilder();

        int years =  (int) Math.floor((double) timespan / (double) YEAR);
        Log.d(TAG, "Year: " + years);
        timespan -= years * YEAR;

        if (years > 0) {
            sb.append(years);
            if (shortValues) {
                sb.append("y");
            } else {
                sb.append(" ");
                if (years > 1) {
                    sb.append(ctx.getString(R.string.date_year_plural));
                } else {
                    sb.append(ctx.getString(R.string.date_year_singular));
                }
            }
            sb.append(" ");
        }

        int days = (int) Math.floor((double) timespan / (double) DAY);
        Log.d(TAG, "Day: " + days);
        timespan -= days * DAY;

        if (days > 0) {
            sb.append(days);

            if (shortValues) {
                sb.append("d");
            } else {
                sb.append(" ");
                if (days > 1) {
                    sb.append(ctx.getString(R.string.date_day_plural));
                } else {
                    sb.append(ctx.getString(R.string.date_day_singular));
                }
            }
            sb.append(" ");
        }

        int hours = (int) Math.floor((double) timespan / (double) HOUR);
        Log.d(TAG, "Hour: " + hours);
        timespan -= hours * HOUR;

        if (hours > 0) {
            sb.append(hours);

            if (shortValues) {
                sb.append("h");
            } else {
                sb.append(" ");
                if (hours > 1) {
                    sb.append(ctx.getString(R.string.date_hour_plural));
                } else {
                    sb.append(ctx.getString(R.string.date_hour_singular));
                }

            }
            sb.append(" ");
        }

        int minutes = (int) Math.floor((double) timespan / (double) MINUTE);
        Log.d(TAG, "Minute: " + minutes);
        timespan -= minutes * MINUTE;

        if (minutes > 0) {
            sb.append(minutes);
            if (shortValues){
                sb.append("m");
            } else {
                sb.append(" ");
                if (minutes > 1) {
                    sb.append(ctx.getString(R.string.date_minute_plural));
                } else {
                    sb.append(ctx.getString(R.string.date_minute_singular));
                }
            }
            sb.append(" ");
        }

        int seconds = (int) Math.round((double) timespan / (double) SECOND);
        Log.d(TAG, "Second: " + seconds);

        if (seconds > 0) {
            sb.append(seconds);

            if (shortValues){
                sb.append("s");
            } else {
                sb.append(" ");
                if (seconds > 1) {
                    sb.append(ctx.getString(R.string.date_second_plural));
                } else {
                    sb.append(ctx.getString(R.string.date_second_singular));
                }
            }
        }

        return sb.toString().trim();
    }

    /**
     * Round a number to a given number of decimal places
     *
     * @param number    Number to be rounded
     * @param digits    Digits to be rounded
     * @return          The resulting number
     */
    static double decimalRound(double number, int digits){
        return Math.round(number*Math.pow(10, digits)) / Math.pow(10, digits);

    }

    /**
     * Draw a pie chart for a given percentage
     *
     * @param c             The canvas
     * @param percentage    The percentage
     * @param width         Width
     * @param height        Height
     * @return              Th canvas object
     */
    public static Canvas getCanvasCircle(Context context, Canvas c, double percentage, int width, int height) {
        Paint pRed = new Paint();
        Paint pGreen = new Paint();
        pRed.setColor(Color.LTGRAY);
        pRed.setAntiAlias(true);
        pGreen.setColor(Color.argb(255, 72, 143,0));
        pGreen.setAntiAlias(true);
        long radius = Math.round(Math.min(height, width)*0.4);
        final RectF arc = new RectF(width/2 - radius, height/2 - radius, width/2 + radius, height/2 +radius);


        @ColorInt int color = Color.WHITE;

        TypedValue a = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.windowBackground, a, true);
        if (a.type >= TypedValue.TYPE_FIRST_COLOR_INT && a.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            // windowBackground is a color
            color = a.data;
        }
        if (c != null) {
            c.drawColor(color);


            //Log.d(TAG, "Width: " + width + " Height: " + height);
            c.drawCircle(width / 2, height / 2, radius, pRed);

            c.drawArc(arc, -90, 360 * ((float) percentage), true, pGreen);
        }

        return c;
    }
}
