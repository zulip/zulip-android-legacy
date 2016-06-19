package com.zulip.android.activities;


import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.ViewAssertion;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.widget.RecyclerView;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import android.widget.Button;

import com.zulip.android.R;
import com.zulip.android.ZulipApp;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withHint;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AllOf.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class LoginDevAuthTest {

    @Rule
    public ActivityTestRule<ZulipActivity> mActivityTestRule = new ActivityTestRule<>(ZulipActivity.class);

    private static String EMAIL_TEST = "";
    private static String SERVER_URL = "http://10.0.3.2:9991/api/";

    // Convenience helper
    @Test
    public void TestSerially() {
        getDevEmails();
        loginThroughDevMail();
    }

    @Before
    public void setup() {
        closeSoftKeyboard();
    }

    public void getDevEmails() {

        closeSoftKeyboard();
        //Uncheck Checkbox
        ViewInteraction checkBox = onView(
                allOf(withId(R.id.checkbox_usezulip), withText("Use Zulip.com"),
                        withParent(withId(R.id.server)),
                        isDisplayed()));
        checkBox.perform(click());

        //Give a Server URL
        ViewInteraction editText = onView(allOf(withId(R.id.server_url), withHint(R.string.server_domain), withParent(withId(R.id.server)), isDisplayed()));
        editText.perform(replaceText(SERVER_URL));

        closeSoftKeyboard();

        //Click DevAuth TextView Button
        ViewInteraction textView = onView(allOf(withId(R.id.local_server_button), withText("Dev Backend testing server"), isDisplayed()));
        textView.perform(click());

        //Check if there are Emails
        onView(withId(R.id.devAuthRecyclerView)).check(hasItemsCount());
    }

    private static boolean matchedBefore = false;

    //This filter returns only one View!
    private Matcher<View> emailFilter() {

        return new BoundedMatcher<View, Button>(Button.class) {
            @Override
            public void describeTo(Description description) {
                description.appendText("ERROR");
            }

            @Override
            protected boolean matchesSafely(Button item) {
                if (!matchedBefore && item.getText().toString().contains("@")) {
                    EMAIL_TEST = item.getText().toString();
                    matchedBefore = true;
                    return true;
                }
                return false;
            }
        };
    }


    public void loginThroughDevMail() {

        //If EMAIL not specified click on first EMAIL.
        if (EMAIL_TEST.equals("")) {
            onView(allOf(withId(android.R.id.text1), emailFilter())).perform(click());
        } else {
            //Find and click the E-Mail Button.
            ViewInteraction button = onView(Matchers.allOf(withId(android.R.id.text1), withText(EMAIL_TEST), isDisplayed()));
            button.perform(click());
        }

        //Verify Correct E-Mail is Stored
        assertThat(ZulipApp.get().getEmail(), is(EMAIL_TEST));
    }

    public static ViewAssertion hasItemsCount() {
        return new ViewAssertion() {
            @Override
            public void check(View view, NoMatchingViewException e) {
                if (!(view instanceof RecyclerView)) {
                    throw e;
                }
                RecyclerView rv = (RecyclerView) view;
                assertThat("Items less than 2, which means no E-Mails!", rv.getAdapter().getItemCount(), Matchers.greaterThan(2));
            }
        };
    }
}
