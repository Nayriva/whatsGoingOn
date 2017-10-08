package ee.ut.madp.whatsgoingon.helpers;

import android.support.design.widget.TextInputLayout;
import android.widget.EditText;

public class ModelHelper {

    /**
     * Gets specified property of specified object
     * @param inputTextLayout
     * @return
     */
    public static String getProperty(Object inputTextLayout) {
        String property = "";
        if (inputTextLayout instanceof TextInputLayout) {
            EditText editText = ((TextInputLayout) inputTextLayout).getEditText();
            property = editText.getText().toString();
        } else if (inputTextLayout instanceof EditText) {
            EditText editText = (EditText) inputTextLayout;
            property = editText.getText().toString();
        }

        return property;
    }

    /**
     * Sets property to edittext
     * @param textInputLayout
     * @param input
     */
    public static void setProperty(TextInputLayout textInputLayout, String input) {
        EditText editText = textInputLayout.getEditText();
        editText.setText(input);
    }
}
