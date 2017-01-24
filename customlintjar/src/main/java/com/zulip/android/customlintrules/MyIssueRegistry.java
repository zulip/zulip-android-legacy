package com.zulip.android.customlintrules;

import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.detector.api.Issue;
import com.zulip.android.customlintrules.detectors.PrintStackTraceDetector;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unused")
public class MyIssueRegistry extends IssueRegistry {
    @Override
    public List<Issue> getIssues() {
        System.out.println("********Working on custom lint check!!!********");
        return Arrays.asList(
                PrintStackTraceDetector.ISSUE);

    }
}

