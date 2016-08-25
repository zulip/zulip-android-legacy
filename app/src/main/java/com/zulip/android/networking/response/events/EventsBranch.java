package com.zulip.android.networking.response.events;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * Created by patrykpoborca on 8/26/16.
 */

public class EventsBranch {
    @SerializedName("id")
    private int id;

    @SerializedName("type")
    private String type;

    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public enum BranchType {
        MESSAGE(MessageWrapper.class, "message"),
        PRESENCE(PresenceWrapper.class, "presence");

        private final Class<? extends EventsBranch> klazz;
        private final String key;

        BranchType(@NonNull Class<? extends EventsBranch> klazz, @NonNull String serializeKey) {
            this.klazz = klazz;
            this.key = serializeKey;
        }

        public String getKey() {
            return key;
        }

        public Class<?> getKlazz() {
            return klazz;
        }

        @Nullable
        public static Class<? extends EventsBranch> fromRawType(@NonNull EventsBranch branch) {
            for (BranchType t : values()) {
                if(t.key.equalsIgnoreCase(branch.getType())) {
                    return t.klazz;
                }
            }
            return null;
        }
    }
}
