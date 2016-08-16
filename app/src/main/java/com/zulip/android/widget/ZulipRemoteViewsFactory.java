package com.zulip.android.widget;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
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
    @Override
    public void onDataSetChanged() {
        Log.i("ZULIP_WIDGET", "onDataSetChanged() = Data reloaded");
        QueryBuilder<Message, Object> queryBuilder = ZulipApp.get().getDao(Message.class).queryBuilder();

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
