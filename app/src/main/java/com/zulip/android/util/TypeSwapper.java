package com.zulip.android.util;

/**
 * Created by patrykpoborca on 8/26/16.
 */

public interface TypeSwapper<GIVEN, RETURN> {
    RETURN convert(GIVEN given);
}
