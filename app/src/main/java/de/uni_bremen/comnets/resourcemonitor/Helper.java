package de.uni_bremen.comnets.resourcemonitor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.PowerManager;
import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.method.LinkMovementMethod;
import android.util.Log;
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
}
