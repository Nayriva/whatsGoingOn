package ee.ut.madp.whatsgoingon.helpers;

import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;

import com.mobsandgeeks.saripaar.Validator;

import java.util.List;


/**
 * Helper for working with text watcher.
 */
public class TextWatcherHelper {

    private static final String TAG = TextWatcherHelper.class.getSimpleName();

    public static class MyTextWatcher implements TextWatcher {
        private Validator validator;


        public MyTextWatcher(Validator validator) {
            Log.i(TAG, "constructor");
            this.validator = validator;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            //intentionally left blank
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            //intentionally left blank
        }

        @Override
        public void afterTextChanged(Editable editable) {
            validator.validate();
        }
    }

    public static void setTextWatcherListeners(List<TextInputLayout> inputLayoutList, Validator validator) {
        Log.i(TAG, "setTextWatcherListeners");
        for (TextInputLayout textInputLayout : inputLayoutList) {
            if (textInputLayout.getEditText() != null) {
                textInputLayout.getEditText().addTextChangedListener(new MyTextWatcher(validator));
            }
        }
    }

    public static void clearAllInputs(List<TextInputLayout> inputLayoutList) {
        Log.i(TAG, "clearAllInputs");
        for (TextInputLayout textInputLayout : inputLayoutList) {
            textInputLayout.setError(null);
            textInputLayout.setErrorEnabled(false);
        }
    }
}
