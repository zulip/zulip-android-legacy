package com.zulip.android.util;

import java.util.ArrayList;
import java.util.List;


public class FunctionalUtils {

    public static <GIVEN, RETURN>List<RETURN> mapOn(List<GIVEN> originalList, TypeSwapper<GIVEN, RETURN> mapFunc) {
        List<RETURN> returnList = new ArrayList<>(originalList.size());
        for (int i = 0; i < originalList.size(); i++) {
            RETURN res = mapFunc.convert(originalList.get(i));
            returnList.add(res);
        }
        return returnList;
    }
}
