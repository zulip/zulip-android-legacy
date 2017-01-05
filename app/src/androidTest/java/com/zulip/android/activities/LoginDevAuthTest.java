package com.zulip.android.activities;


import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import android.widget.Button;

import com.zulip.android.R;
import com.zulip.android.ZulipApp;
import com.zulip.android.util.ZLog;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.zulip.android.helper.ViewAssertions.hasItemsCount;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AllOf.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LoginDevAuthTest {

    @Rule
    public ActivityTestRule<ZulipActivity> mActivityTestRule = new ActivityTestRule<>(ZulipActivity.class);

    private static String EMAIL_TEST = "";
    private static String SERVER_URL = "http://www.local.test.com";


    // Convenience helper
    @Test
    public void TestSerially() {
        if (!ZulipApp.get().getApiKey().isEmpty()) {
            BaseTest baseTest = new BaseTest();
            baseTest.logout();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                ZLog.logException(e);
            }
        }
        getDevEmails();
        loginThroughDevMail();
    }


    public void getDevEmails() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Fill Server URL
        ViewInteraction serverURLInteraction = onView(Matchers.allOf(withId(R.id.server_url_in), isDisplayed()));
        serverURLInteraction.perform(replaceText(SERVER_URL));

        //Click Enter server URL
        ViewInteraction enterServerUrl = onView(Matchers.allOf(withId(R.id.server_btn), withText(R.string.enter), isDisplayed()));
        enterServerUrl.perform(click());

        //Click DevAuth TextView Button
        ViewInteraction devServerTextViewInteraction = onView(allOf(withId(R.id.local_server_button), withText(R.string.local_server), isDisplayed()));
        devServerTextViewInteraction.perform(click());

        //Check if there are Emails
        onView(withId(R.id.devAuthRecyclerView)).check(hasItemsCount());
    }

    private static boolean matchedBefore = false;

    private void loginThroughDevMail() {
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


}
