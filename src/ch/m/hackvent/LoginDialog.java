package ch.m.hackvent;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.EditText;

public class LoginDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
    	final EditText input = new EditText(getActivity());
    	input.setSingleLine();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Username, please ...")
               .setView(input)
               .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       ((MainActivity)getActivity()).setUser(input.getText().toString());
                   }
               });
        return builder.create();
    }
}