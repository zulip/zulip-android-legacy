package com.zulip.android.util;

import android.app.AlertDialog;
import android.app.Dialog;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.zulip.android.R;

/**
 * List dialog for picking option of upload (camera or file).
 * <p>
 * The activity that creates an instance of this dialog fragment must
 * implement ListDialogListener interface {@link ListDialogListener} in order to receive event callbacks.
 * Each method passes the DialogFragment in case the host needs to query it.
 */

public class ListDialog extends DialogFragment {

    // Use this instance of the interface to deliver action events
    ListDialogListener mListener;

    // Override the Fragment.onAttach() method to instantiate the ListDialogListener
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the ListDialogListener so we can send events to the host
            mListener = (ListDialogListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
                    + " must implement ListDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        final ViewGroup nullParent = null;
        View rootView = inflater.inflate(R.layout.list_dialog, nullParent);
        View cameraListItem = rootView.findViewById(R.id.picture_dialog);
        View fileListItem = rootView.findViewById(R.id.pick_file_dialog);
        ImageView cameraImage = (ImageView) rootView.findViewById(R.id.camera_share_icon);
        ImageView fileImage = (ImageView) rootView.findViewById(R.id.file_share_icon);

        //Add proper colorState tinting to the camera and fileList Icons
        cameraImage.setColorFilter(getResources().getColorStateList(R.color.colorTextSecondary).getColorForState(cameraImage.getDrawableState(), 0));
        fileImage.setColorFilter(getResources().getColorStateList(R.color.colorTextSecondary).getColorForState(fileImage.getDrawableState(), 0));

        // if device doesn't have camera, disable camera option
        if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            cameraListItem.setEnabled(false);
        } else {
            cameraListItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onDialogPhotoClick(ListDialog.this);
                }
            });
        }

        fileListItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onDialogFileClick(ListDialog.this);
            }
        });
        builder.setView(rootView);
        return builder.create();
    }

    public interface ListDialogListener {
        void onDialogPhotoClick(DialogFragment dialog);

        void onDialogFileClick(DialogFragment dialog);
    }
}
