package no.sforman.myevents;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.util.Log;
import android.widget.DatePicker;

import androidx.fragment.app.DialogFragment;


class DatePickerFragment extends DialogFragment
        implements DatePickerDialog.OnDateSetListener {

    private static final String TAG = "DatePickerFragment";

    private OnDateChosenListener dListener = null;
    public interface OnDateChosenListener {
        void onDateChosen(int year, int month, int day);
    }

    public void setOnDateChosenListener(OnDateChosenListener listener){
        Log.d(TAG, "setOnDateChosenListener: ");
        dListener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.d(TAG, "onCreateDialog: ");
        // Use the current date as the default date in the picker
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        // Create a new instance of DatePickerDialog and return it
        return new DatePickerDialog(getActivity(), this, year, month, day);
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        if(dListener != null){
            dListener.onDateChosen(year, month, day);
        }
    }


}
