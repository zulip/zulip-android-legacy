package com.zulip.android.util;


public interface TypeSwapper<GIVEN, RETURN> {
    RETURN convert(GIVEN given);

}
