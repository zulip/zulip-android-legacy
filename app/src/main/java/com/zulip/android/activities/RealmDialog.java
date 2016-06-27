package com.zulip.android.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.zulip.android.R;
import com.zulip.android.ZulipApp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class RealmDialog extends DialogFragment {
    private ZulipApp app;
    private ArrayAdapter<String> realmsAdapter;
    private Context context;

    static RealmDialog newInstance() {
        return new RealmDialog();
    }

    public RealmDialog() {
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        builder.setTitle(R.string.realm_title);
        View rootView = inflater.inflate(R.layout.realm_dialog_list, null);
        List<String> realmsList = new ArrayList<>();
        context = getActivity();
        app = ZulipApp.get();
        ListView listView = (ListView) rootView.findViewById(R.id.realmListView);
        realmsList = new ArrayList<String>(app.serverStringSet);
        realmsAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, realmsList);
        listView.setAdapter(realmsAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                new AlertDialog.Builder(context)
                        .setTitle(R.string.realm_switch)
                        .setMessage(context.getString(R.string.realm_switch_confirm, realmsAdapter.getItem(position)))
                        .setPositiveButton(getResources().getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, int whichButton) {
                                if (position == app.currentRealm) {
                                    Toast.makeText(context, R.string.realm_this_is_current, Toast.LENGTH_LONG).show();
                                    return;
                                }
                                final ProgressDialog progressDialog = new ProgressDialog(getActivity());
                                progressDialog.setCancelable(false);
                                progressDialog.setTitle(app.getString(R.string.realm_switching));
                                progressDialog.setMessage(app.getString(R.string.please_wait));
                                progressDialog.show();
                                ((ZulipActivity) getActivity()).switchRealm(progressDialog, position);
                                if (RealmDialog.this.getDialog() != null)
                                    RealmDialog.this.getDialog().cancel();
                            }
                        })
                        .setNegativeButton(getResources().getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        });
        builder.setView(rootView)
                .setPositiveButton(R.string.realm_add, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        RealmDialog.this.getDialog().cancel();
                    }
                });
        return builder.create();
    }
}