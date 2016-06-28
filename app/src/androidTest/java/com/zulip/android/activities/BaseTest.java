package com.zulip.android.activities;


import android.support.test.espresso.Root;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.zulip.android.R;
import com.zulip.android.ToastMatcher;
import com.zulip.android.ZulipApp;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertNull;

@LargeTest
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BaseTest {

    private static final String PASSWORD_TEST = "yourpasswordhere";
    private static final String EMAIL_TEST = "iago@zulip.com";

    @Rule
    public ActivityTestRule<ZulipActivity> mActivityTestRule = new ActivityTestRule<>(ZulipActivity.class);

    @Before
    public void setUp() {
        if (ZulipApp.get().getApiKey() != null) {
            logout();
        }
    }

    @Test
    public void WrongLoginPasswordToast() {
        if (ZulipApp.get().getApiKey() != null) {
            logout();
        }
        //Fill Username
        ViewInteraction usernameVI = onView(
                allOf(withId(R.id.username), isDisplayed()));
        usernameVI.perform(replaceText(EMAIL_TEST));

        //Fill Password
        ViewInteraction passwordVI = onView(
                allOf(withId(R.id.password), isDisplayed()));
        passwordVI.perform(replaceText("WRONG_PASSWORD"));

        //Click Login
        ViewInteraction button = onView(
                allOf(withId(R.id.zulip_login), withText("Log in"), isDisplayed()));
        button.perform(click());

        onView(withText("Your username or password is incorrect.")).inRoot(isToast()).check(matches(isDisplayed()));
    }

    public static Matcher<Root> isToast() {
        return new ToastMatcher();
    }

    @Test
    public void logout() {
        //Open overflow menu
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());

        //Click Logout Button
        onView(withText(R.string.logout)).perform(click());

        //Check API Key for verifying
        assertNull(ZulipApp.get().getApiKey());
    }

    public void login() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Fill Username
        ViewInteraction usernameVI = onView(
                allOf(withId(R.id.username), isDisplayed()));
        usernameVI.perform(replaceText(EMAIL_TEST));

        //Fill Password
        ViewInteraction passwordVI = onView(
                allOf(withId(R.id.password), isDisplayed()));
        passwordVI.perform(replaceText(PASSWORD_TEST));

        //Click Login
        ViewInteraction button = onView(
                allOf(withId(R.id.zulip_login), withText("Log in"), isDisplayed()));
        button.perform(click());
    }
}
