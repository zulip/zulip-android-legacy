package com.zulip.android.networking.response.events;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;
import com.zulip.android.util.TypeSwapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by patrykpoborca on 8/26/16.
 */

public class GetEventResponse {

    @SerializedName("msg")
    private String msg;

    @SerializedName("result")
    private String result;

    @SerializedName("handler_id")
    private int handlerId;

    @SerializedName("events")
    private List<EventsBranch> events;

    @Nullable
    public <T>List<T> getEventsOf(EventsBranch.BranchType branchType) {
        return getEventsOf(branchType, null);
    }

    @Nullable
    public <T extends EventsBranch, R>List<R> getEventsOf(EventsBranch.BranchType branchType, TypeSwapper<T, R> converter) {
        if(events == null) {
            return null;
        }
        try {
            List<R> types = new ArrayList<>(events.size());
            for (int i = 0; i < events.size(); i++) {
                if (events.getClass().equals(branchType.getKlazz())) {
                    EventsBranch orig = events.get(i);
                    if(converter != null) {
                        types.add(converter.convert((T) orig));
                    }
                    else {
                        types.add((R) orig);
                    }
                }
            }
            return types;
        }
        catch(Exception e) {
            //catch misuse
            return null;
        }
    }

    public List<EventsBranch> getEvents() {
        return events;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }


}
