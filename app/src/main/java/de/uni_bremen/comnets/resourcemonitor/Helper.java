package de.uni_bremen.comnets.resourcemonitor;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

/**
 * Some helper classes required for several different modules
 */

class Helper {

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
     */
    static void showUserMessage(Context context, Spanned msg, String title) {
        AlertDialog dlgAlert = new AlertDialog.Builder(context)
                .setTitle(title)
                .setPositiveButton(context.getText(R.string.button_ok), null)
                .setMessage(msg)
                .setIcon(android.R.drawable.ic_dialog_info)
                .create();
        dlgAlert.show();
        TextView tv = (TextView) dlgAlert.findViewById(android.R.id.message);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        tv.setClickable(true);
    }

    /**
     * Show a HTML message dialog to the user
     *
     * @param context   The application context
     * @param text      The HTML text message
     * @param title     The dialog title
     */
    static void showHTMLUserMessage(Context context, String text, String title) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            showUserMessage(context, Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY), title);
        } else {
            showUserMessage(context, Html.fromHtml(text), title);
        }
    }
}
