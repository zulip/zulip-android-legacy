package com.zulip.android.activities;


import android.support.test.espresso.ViewInteraction;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.zulip.android.R;

import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class BaseTest {

    private static final String PASSWORD_TEST = "SKIPERROR";
    private static final String EMAIL_TEST = "iago@zulip.com";

    public void login() {
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
