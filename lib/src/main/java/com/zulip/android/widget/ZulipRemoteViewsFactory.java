package com.zulip.android.widget;

import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.j256.ormlite.stmt.QueryBuilder;
import com.zulip.android.R;
import com.zulip.android.ZulipApp;
import com.zulip.android.models.Message;
import com.zulip.android.models.MessageType;
import com.zulip.android.util.ZLog;

import java.sql.SQLException;
import java.util.List;

import static com.zulip.android.widget.WidgetPreferenceFragment.FROM_PREFERENCE;

public class ZulipRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private Context context;
    private List<Message> messageList;
    private String from;

    public ZulipRemoteViewsFactory(Context applicationContext, Intent intent) {
        context = applicationContext;
        from = intent.getStringExtra(FROM_PREFERENCE);
    }

    @Override
    public void onCreate() {

    }

    private String setupWhere() {
        switch (from) {
            //These values present in R.arrays.from_values
            case "today":
                return "timestamp BETWEEN DATE('now') AND DATE('now', '+1 day')";
            case "yesterday":
                return "DATE(timestamp) >= DATE('now',  '-1 days')";
            case "week":
                return "DATE(timestamp) >= DATE('now', 'weekday 0', '-7 days')";
            case "all":
            default:
                return "";
        }
    }

    @Override
    public void onDataSetChanged() {
        Log.i("ZULIP_WIDGET", "onDataSetChanged() = Data reloaded");
        QueryBuilder<Message, Object> queryBuilder = ZulipApp.get().getDao(Message.class).queryBuilder();
        String filter;
        filter = setupWhere();
        if (!filter.equals("")) {
            queryBuilder.where().raw(filter);
        }

        try {
            messageList = queryBuilder.query();
        } catch (SQLException e) {
            ZLog.logException(e);
        }
    }

    @Override
    public void onDestroy() {

    }

    public int getCount() {
        return messageList.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews remoteView = new RemoteViews(context.getPackageName(), R.layout.widget_row);
        Message message = messageList.get(position);
        if (message.getType() == MessageType.STREAM_MESSAGE) {
            remoteView.setTextViewText(R.id.widget_header, message.getStream().getName() + " > " + message.getSubject());
        } else {
            remoteView.setTextViewText(R.id.widget_header, message.getDisplayRecipient(ZulipApp.get()));
        }
        remoteView.setTextViewText(R.id.widget_sendername, message.getSender().getName());
        remoteView.setTextViewText(R.id.widget_message, message.getFormattedContent(ZulipApp.get()));

        if (from.equals("today")) {
            remoteView.setTextViewText(R.id.widget_timestamp, DateUtils.formatDateTime(context, message.getTimestamp().getTime(), DateUtils.FORMAT_SHOW_TIME));
        } else {
            remoteView.setTextViewText(R.id.widget_timestamp, DateUtils.formatDateTime(context, message
                    .getTimestamp().getTime(), DateUtils.FORMAT_SHOW_DATE
                    | DateUtils.FORMAT_ABBREV_MONTH
                    | DateUtils.FORMAT_SHOW_TIME));
        }
        return remoteView;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
