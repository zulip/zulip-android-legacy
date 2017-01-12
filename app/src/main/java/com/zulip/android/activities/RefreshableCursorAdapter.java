package com.zulip.android.activities;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.zulip.android.R;
import com.zulip.android.ZulipApp;
import com.zulip.android.models.PeopleDrawerList;
import com.zulip.android.models.Presence;
import com.zulip.android.models.PresenceType;
import com.zulip.android.util.Constants;
import com.zulip.android.viewholders.floatingRecyclerViewLables.FloatingHeaderAdapter;
import com.zulip.android.viewholders.floatingRecyclerViewLables.FloatingHeaderDecoration;

import java.util.List;

/**
 * An adapter that can be refreshed by any object that has a reference to it.
 * <p/>
 * This is useful when you don't want to have to encode knowledge of how to
 * refresh an adapter into any function that might want to do so.
 */
public abstract class RefreshableCursorAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements FloatingHeaderAdapter<RefreshableCursorAdapter.HeaderHolder> {

    private List<PeopleDrawerList> mList;
    private Context mContext;
    private ZulipApp mApp;

    protected abstract void onPeopleSelect(int id);

    RefreshableCursorAdapter(Context context, List<PeopleDrawerList> list, ZulipApp app) {
        this.mList = list;
        this.mContext = context;
        this.mApp = app;
    }

    void filterList(List<PeopleDrawerList> list) {
        this.mList = list;
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.stream_tile, parent, false);
        return new TileHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof TileHolder) {
            TileHolder tileHolder = (TileHolder) holder;
            if (position == 0) {
                //All Private message
                tileHolder.tvName.setText(R.string.all_private_messages);
                tileHolder.ivDot.setVisibility(View.VISIBLE);
                tileHolder.ivDot.setBackgroundResource(R.drawable.ic_email_black_24dp);
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onPeopleSelect(Constants.ALL_PEOPLE_ID);
                    }
                });
                return;
            }
            position = tileHolder.getLayoutPosition() - 1;
            tileHolder.tvName.setText(mList.get(position).getPerson().getName());
            //app is passed as parameter from ZulipActivity
            //presence of all persons are in ZulipApp
            //if ZulipApp==null then presence can't be accessed
            if (mApp == null || mList.get(position).getPerson().getEmail() == null) {
                tileHolder.ivDot.setVisibility(View.VISIBLE);
                tileHolder.ivDot.setBackgroundResource(R.drawable.presence_inactive);
            } else {
                Presence presence = mApp.presences.get(mList.get(position).getPerson().getEmail());
                if (presence == null) {
                    tileHolder.ivDot.setVisibility(View.VISIBLE);
                    tileHolder.ivDot.setBackgroundResource(R.drawable.presence_inactive);
                } else {
                    PresenceType status = presence.getStatus();
                    long age = presence.getAge();
                    if (age > Constants.INACTIVE_TIME_OUT) {
                        //person is inactive
                        //show empty circle with stroke
                        tileHolder.ivDot.setVisibility(View.VISIBLE);
                        tileHolder.ivDot.setBackgroundResource(R.drawable.presence_inactive);
                    } else if (PresenceType.ACTIVE == status) {
                        //person is active
                        //show green circle
                        tileHolder.ivDot.setVisibility(View.VISIBLE);
                        tileHolder.ivDot.setBackgroundResource(R.drawable.presence_active);
                    } else if (PresenceType.IDLE == status) {
                        //person is away
                        //show orange circle
                        tileHolder.ivDot.setVisibility(View.VISIBLE);
                        tileHolder.ivDot.setBackgroundResource(R.drawable.presence_away);
                    } else {
                        //can't determine state of person
                        //show as inactive
                        tileHolder.ivDot.setVisibility(View.VISIBLE);
                        tileHolder.ivDot.setBackgroundResource(R.drawable.presence_inactive);
                    }
                }
            }
            final int finalPosition = position;
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onPeopleSelect(mList.get(finalPosition).getPerson().getId());
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return mList.size() + 1;
    }

    @Override
    public long getHeaderId(int position) {
        if (position == 0)
            return FloatingHeaderDecoration.NO_HEADER_ID;
        return (long) mList.get(position - 1).getGroupId();
    }

    @Override
    public HeaderHolder onCreateHeaderViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.people_drawer_heading, parent, false);
        return new HeaderHolder(view);
    }

    @Override
    public void onBindHeaderViewHolder(HeaderHolder headerHolder, int position) {
        headerHolder.tvHeading.setText(mList.get(position - 1).getGroupName());
    }

    private static class TileHolder extends RecyclerView.ViewHolder {

        private TextView tvName;
        private View ivDot;

        TileHolder(View itemView) {
            super(itemView);
            tvName = (TextView) itemView.findViewById(R.id.name);
            ivDot = itemView.findViewById(R.id.stream_dot);
        }
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {

        private TextView tvHeading;

        HeaderHolder(View itemView) {
            super(itemView);
            tvHeading = (TextView) itemView.findViewById(R.id.tvHeading);
        }
    }
}
