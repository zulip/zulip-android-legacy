package com.zulip.android.viewholders;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.text.method.LinkMovementMethod;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import com.zulip.android.R;
import com.zulip.android.ZulipApp;
import com.zulip.android.models.Message;
import com.zulip.android.models.MessageType;
import com.zulip.android.util.OnItemClickListener;

public class MessageHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnCreateContextMenuListener {

    public ImageView gravatar;
    public TextView senderName;
    public TextView timestamp,leftTimestamp;
    public TextView edited, leftEdited;
    public TextView contentView;
    public View leftBar;
    public RelativeLayout messageTile;
    public ImageView contentImage;
    public ImageView starImage, leftStarImage;
    public View contentImageContainer;
    public TableLayout reactionsTable;
    public OnItemClickListener onItemClickListener;

    public MessageHolder(final View itemView) {
        super(itemView);
        gravatar = (ImageView) itemView.findViewById(R.id.gravatar);
        senderName = (TextView) itemView.findViewById(R.id.senderName);
        timestamp = (TextView) itemView.findViewById(R.id.timestamp);
        leftTimestamp = (TextView) itemView.findViewById(R.id.left_timestamp);
        edited = (TextView) itemView.findViewById(R.id.message_edit_tag);
        leftEdited = (TextView) itemView.findViewById(R.id.left_message_edit_tag);
        contentView = (TextView) itemView.findViewById(R.id.contentView);
        contentView.setMovementMethod(LinkMovementMethod.getInstance());
        leftBar = itemView.findViewById(R.id.leftBar);
        messageTile = (RelativeLayout) itemView.findViewById(R.id.messageTile);
        contentImage = (ImageView) itemView.findViewById(R.id.load_image);
        starImage = (ImageView) itemView.findViewById(R.id.star_image);
        leftStarImage = (ImageView) itemView.findViewById(R.id.left_star_image);
        contentImageContainer = itemView.findViewById(R.id.load_image_container);
        reactionsTable = (TableLayout) itemView.findViewById(R.id.reactions_table);
        contentView.setOnClickListener(this);
        contentView.setLongClickable(true);
        itemView.setOnCreateContextMenuListener(this);

        // Add click listener to sender view
        View senderView = itemView.findViewById(R.id.senderTile);
        if (senderView != null) senderView.setOnClickListener(this);
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
            if (msg.getSender().getId() != ZulipApp.get().getYou().getId()) {
                menu.findItem(R.id.edit_message).setVisible(false);
            }
            if (msg.getMessageStar()) {
                menu.findItem(R.id.star_message).setVisible(false);
            } else {
                menu.findItem(R.id.un_star_message).setVisible(false);
            }

        } else if (msg.getPersonalReplyTo(ZulipApp.get()).length > 1) {
            MenuInflater inflater = ((Activity) v.getContext()).getMenuInflater();
            inflater.inflate(R.menu.context_private, menu);
            if (msg.getSender().getId() != ZulipApp.get().getYou().getId()) {
                menu.findItem(R.id.edit_message).setVisible(false);
            }
            if (msg.getMessageStar()) {
                menu.findItem(R.id.star_message).setVisible(false);
            } else {
                menu.findItem(R.id.un_star_message).setVisible(false);
            }
        } else {
            MenuInflater inflater = ((Activity) v.getContext()).getMenuInflater();
            inflater.inflate(R.menu.context_single_private, menu);
            if (msg.getSender().getId() != ZulipApp.get().getYou().getId()) {
                menu.findItem(R.id.edit_message).setVisible(false);
            }
            if (msg.getMessageStar()) {
                menu.findItem(R.id.star_message).setVisible(false);
            } else {
                menu.findItem(R.id.un_star_message).setVisible(false);
            }
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
