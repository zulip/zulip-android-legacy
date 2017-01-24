package com.zulip.android.customlintrules.detectors;

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.ClassContext;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/**
 * Custom lint rule to check e.printStackTrace() is used or not
 */
public class PrintStackTraceDetector extends Detector
        implements Detector.ClassScanner {

    private static final Class<? extends Detector> DETECTOR_CLASS = PrintStackTraceDetector.class;
    private static final EnumSet<Scope> DETECTOR_SCOPE = Scope.CLASS_FILE_SCOPE;

    private static final Implementation IMPLEMENTATION = new Implementation(
            DETECTOR_CLASS,
            DETECTOR_SCOPE
    );

    private static final String ISSUE_ID = "ZLog";
    private static final String ISSUE_DESCRIPTION = "Use `ZLog.logException(e);` instead of `e.printStackTrace();` ";
    private static final String ISSUE_EXPLANATION = "`ZLog.logException(e);` print stacktrace as well as report that to the Crashlytics. " +
            "It provides real-time crash reporting, down to the exact line of code that caused the crash and we can start work as soon as it is reported. "+
            "All logged exceptions are appeared as 'non-fatal' issue in the Fabric dashboard. It helps in knowing when app gone to unexpected state like" +
            " malformed network data, misunderstanding of requirements, a logic error etc. Whenever exception is caught it prevent app to crash and continue the app flow(as well as report to Dashboard)";
    private static final Category ISSUE_CATEGORY = Category.USABILITY;
    private static final int ISSUE_PRIORITY = 9;
    private static final Severity ISSUE_SEVERITY = Severity.ERROR;

    public static final Issue ISSUE = Issue.create(
            ISSUE_ID,
            ISSUE_DESCRIPTION,
            ISSUE_EXPLANATION,
            ISSUE_CATEGORY,
            ISSUE_PRIORITY,
            ISSUE_SEVERITY,
            IMPLEMENTATION
    );
    private static final String FUNCTION_NAME = "printStackTrace";

    @Override
    public List<String> getApplicableCallNames() {
        return Collections.singletonList(FUNCTION_NAME);
    }

    @Override
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList(FUNCTION_NAME);
    }

    @Override
    public void checkCall(@NonNull ClassContext context,
                          @NonNull ClassNode classNode,
                          @NonNull MethodNode method,
                          @NonNull MethodInsnNode call) {
        context.report(ISSUE,
                method,
                call,
                context.getLocation(call),
                "You must use our `ZLog.logException(e);` ");

    }
}