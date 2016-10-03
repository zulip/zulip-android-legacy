package com.zulip.android.viewholders;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.text.method.LinkMovementMethod;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.zulip.android.R;
import com.zulip.android.ZulipApp;
import com.zulip.android.models.Message;
import com.zulip.android.models.MessageType;
import com.zulip.android.util.OnItemClickListener;

public class MessageHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnCreateContextMenuListener {

    public ImageView gravatar;
    public TextView senderName;
    public TextView timestamp;
    public TextView contentView;
    public View leftBar;
    public RelativeLayout messageTile;
    public ImageView contentImage;
    public View contentImageContainer;
    public OnItemClickListener onItemClickListener;

    public MessageHolder(final View itemView) {
        super(itemView);
        gravatar = (ImageView) itemView.findViewById(R.id.gravatar);
        senderName = (TextView) itemView.findViewById(R.id.senderName);
        timestamp = (TextView) itemView.findViewById(R.id.timestamp);
        contentView = (TextView) itemView.findViewById(R.id.contentView);
        contentView.setMovementMethod(LinkMovementMethod.getInstance());
        leftBar = itemView.findViewById(R.id.leftBar);
        messageTile = (RelativeLayout) itemView.findViewById(R.id.messageTile);
        contentImage = (ImageView) itemView.findViewById(R.id.load_image);
        contentImageContainer = itemView.findViewById(R.id.load_image_container);
        contentView.setOnClickListener(this);
        contentView.setLongClickable(true);
        itemView.setOnCreateContextMenuListener(this);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        Message msg = onItemClickListener.getMessageAtPosition(getAdapterPosition());
        onItemClickListener.setContextItemSelectedPosition(getAdapterPosition());
        if (msg == null) {
            return;
        }
        if (msg.getType().equals(MessageType.STREAM_MESSAGE)) {
            MenuInflater inflater = ((Activity) v.getContext()).getMenuInflater();
            inflater.inflate(R.menu.context_stream, menu);
        } else if (msg.getPersonalReplyTo(ZulipApp.get()).length > 1) {
            MenuInflater inflater = ((Activity) v.getContext()).getMenuInflater();
            inflater.inflate(R.menu.context_private, menu);
        } else {
            MenuInflater inflater = ((Activity) v.getContext()).getMenuInflater();
            inflater.inflate(R.menu.context_single_private, menu);
        }
    }

    @Override
    public void onClick(View view) {
        onItemClickListener.onItemClick(view.getId(), getAdapterPosition());
    }

    public void setItemClickListener(OnItemClickListener itemClickListener) {
        this.onItemClickListener = itemClickListener;
    }
}
