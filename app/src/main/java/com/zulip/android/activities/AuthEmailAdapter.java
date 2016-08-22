package com.zulip.android.activities;

import android.content.Context;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.zulip.android.R;
import com.zulip.android.util.AuthClickListener;

import java.util.List;

/**
 * Adapter used for the RecyclerView here{@link DevAuthActivity} for showing Emails in the DevAuthBackend
 */
class AuthEmailAdapter extends RecyclerView.Adapter<AuthEmailAdapter.AuthEmailViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ADMIN = 1;
    private static final int VIEW_TYPE_USER = 2;
    private List<String> emails;
    private int userStringPosition; //Position of the User String header
    private static AuthClickListener authClickListener;

    private
    @ColorInt
    int devAuthUserColor;
    private
    @ColorInt
    int devAuthAdminColor;

    AuthEmailAdapter(List<String> emails, int directAdminSize, Context context) {
        this.userStringPosition = directAdminSize + 1; //+1 due to the Administrator String
        this.emails = emails;
        devAuthUserColor = ContextCompat.getColor(context, R.color.dev_auth_user);
        devAuthAdminColor = ContextCompat.getColor(context, R.color.dev_auth_admin);
    }

    @Override
    public AuthEmailViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = null;
        if (viewType == VIEW_TYPE_HEADER)
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.dev_auth_header, parent, false);
        else
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.dev_auth_row, parent, false);
        return new AuthEmailViewHolder(v);
    }

    @Override
    public void onBindViewHolder(AuthEmailViewHolder holder, int position) {
        if (position == 0) {
            if (userStringPosition == 1)
                ((TextView) holder.view).setText(R.string.admin_none); //Show none if no admins
            else ((TextView) holder.view).setText(R.string.admin);
        } else if (position == userStringPosition)
            ((TextView) holder.view).setText(R.string.normal_user);
        else if (position > userStringPosition) {
            ((Button) holder.view).setText(emails.get(position - 2));
            holder.view.setBackgroundColor(devAuthUserColor);
        } else {
            ((Button) holder.view).setText(emails.get(position - 1));
            holder.view.setBackgroundColor(devAuthAdminColor);
        }
    }

    @Override
    public int getItemCount() {
        return emails.size() + 2;
        //+2 due to the two headers {"Admin and user"}
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 || position == userStringPosition) return VIEW_TYPE_HEADER;
        if (position > userStringPosition) return VIEW_TYPE_USER;
        else return VIEW_TYPE_ADMIN;
    }

    void setOnItemClickListener(AuthClickListener authClickListener) {
        AuthEmailAdapter.authClickListener = authClickListener;
    }

    class AuthEmailViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        View view;

        AuthEmailViewHolder(View itemView) {
            super(itemView);
            view = itemView.findViewById(android.R.id.text1);
            if (view instanceof Button) {
                view.setOnClickListener(this);
            }
        }

        @Override
        public void onClick(View v) {
            authClickListener.onItemClick(((Button) v).getText().toString());
        }
    }
}
