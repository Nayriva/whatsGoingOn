package ee.ut.madp.whatsgoingon.helpers;

import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextWatcher;

import com.mobsandgeeks.saripaar.Validator;

import java.util.List;

public class MyTextWatcherHelper {

    public static class MyTextWatcher implements TextWatcher {
        private Validator validator;


        public MyTextWatcher(Validator validator) {
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
        for (TextInputLayout textInputLayout : inputLayoutList) {
            if (textInputLayout.getEditText() != null) {
                textInputLayout.getEditText().addTextChangedListener(new MyTextWatcher(validator));
            }
        }
    }

    public static void clearAllInputs(List<TextInputLayout> inputLayoutList) {
        for (TextInputLayout textInputLayout : inputLayoutList) {
            textInputLayout.setError(null);
            textInputLayout.setErrorEnabled(false);
        }
    }
}
