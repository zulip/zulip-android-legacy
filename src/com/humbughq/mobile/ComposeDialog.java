package com.humbughq.mobile;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.EditText;

import com.humbughq.mobile.HumbugAsyncPushTask.AsyncTaskCompleteListener;

public class ComposeDialog extends DialogFragment {
    HumbugActivity activity;
    ZulipApp app;
    private MessageType type;

    private View view;
    private EditText recipient;
    private EditText subject;
    private EditText body;

    public static ComposeDialog newInstance(MessageType type, String stream,
            String topic, String pmRecipients) {
        ComposeDialog d = new ComposeDialog();
        Bundle args = d.getArguments();
        if (args == null) {
            args = new Bundle();
        }
        args.putSerializable("type", type);
        args.putString("stream", stream);
        args.putString("topic", topic);
        args.putString("pmRecipients", pmRecipients);
        d.setArguments(args);
        return d;
    }

    public ComposeDialog() {
    }

    /**
     * Switches the compose window's state to compose a personal message.
     */
    protected void switchToPersonal() {
        subject.setVisibility(View.GONE);
        recipient.setGravity(Gravity.FILL_HORIZONTAL);
        recipient.setHint(R.string.pm_prompt);
    }

    /**
     * Switches the compose window's state to compose a stream message.
     */
    protected void switchToStream() {
        subject.setVisibility(View.VISIBLE);
        recipient.setGravity(Gravity.NO_GRAVITY);
        recipient.setHint(R.string.stream);
    }

    /**
     * Check if a field is nonempty and mark fields as invalid if they are.
     * 
     * @param field
     *            The field to check
     * @param fieldName
     *            The human-readable name of the field
     * @return Whether the field correctly validated.
     */
    protected boolean requireFilled(EditText field, String fieldName) {
        if (field.getText().toString().equals("")) {
            field.setError("You must specify a " + fieldName);
            field.requestFocus();
            return false;
        }
        return true;

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        view = inflater.inflate(R.layout.compose, null);

        Bundle bundle = getArguments();
        type = (MessageType) bundle.getSerializable("type");
        final String stream = bundle.getString("stream");
        final String topic = bundle.getString("topic");
        final String pmRecipients = bundle.getString("pmRecipients");

        Log.i("onCreateDialog", "" + type + " " + stream + " " + topic + " "
                + pmRecipients);

        recipient = (EditText) view.findViewById(R.id.composeRecipient);
        subject = (EditText) view.findViewById(R.id.composeSubject);
        body = (EditText) view.findViewById(R.id.composeText);

        activity = ((HumbugActivity) getActivity());
        app = activity.app;

        AlertDialog dialog = builder.setView(view).setTitle("Compose")
                .setPositiveButton(R.string.send, null)
                .setNegativeButton(android.R.string.cancel, null).create();

        if (type == MessageType.STREAM_MESSAGE) {
            this.switchToStream();
            if (stream != null) {
                recipient.setText(stream);
                if (topic != null) {
                    subject.setText(topic);
                    body.requestFocus();
                } else {
                    subject.setText("");
                    subject.requestFocus();
                }
            } else {
                recipient.setText("");
                recipient.requestFocus();
            }
        } else {
            this.switchToPersonal();
            if (pmRecipients != null) {
                recipient.setText(pmRecipients);
                body.requestFocus();
            } else {
                recipient.setText("");
                recipient.requestFocus();
            }
        }

        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        return dialog;
    }

    private void sending(boolean isSending) {
        view.findViewById(R.id.composeStatus).setVisibility(
                isSending ? View.VISIBLE : View.GONE);

        ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE)
                .setEnabled(!isSending);

        recipient.setEnabled(!isSending);
        subject.setEnabled(!isSending);
        body.setEnabled(!isSending);
    }

    @Override
    public void onResume() {
        super.onResume();
        // getButton only works after the view is shown
        ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean valid = true;

                        if (type == MessageType.STREAM_MESSAGE) {
                            valid &= requireFilled(subject, "subject");
                            valid &= requireFilled(recipient, "recipient"); // stream
                        } else if (type == MessageType.PRIVATE_MESSAGE) {
                            valid &= requireFilled(recipient, "recipient");
                        }

                        valid &= requireFilled(body, "message body");

                        if (valid) {
                            Message msg = new Message(app);
                            msg.setSender(app.you);

                            if (type == MessageType.STREAM_MESSAGE) {
                                msg.setType(MessageType.STREAM_MESSAGE);
                                msg.setStream(new Stream(recipient.getText()
                                        .toString()));
                                msg.setSubject(subject.getText().toString());
                            } else if (type == MessageType.PRIVATE_MESSAGE) {
                                msg.setType(MessageType.PRIVATE_MESSAGE);
                                msg.setRecipient(recipient.getText().toString()
                                        .split(","));
                            }

                            msg.setContent(body.getText().toString());

                            sending(true);

                            AsyncSend sender = new AsyncSend(activity, msg);
                            sender.setCallback(new AsyncTaskCompleteListener() {
                                public void onTaskComplete(String result) {
                                    dismiss();
                                }

                                public void onTaskFailure(String result) {
                                    sending(false);
                                    body.setError("Error sending message");
                                }
                            });

                            sender.execute();
                        }
                    }
                });
    }
}
