package com.zulip.android.activities;


import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.zulip.android.R;
import com.zulip.android.ZulipApp;
import com.zulip.android.filters.NarrowFilterToday;
import com.zulip.android.helper.ViewAssertions;
import com.zulip.android.models.MessageType;
import com.zulip.android.util.ZLog;

import org.apache.commons.lang.RandomStringUtils;
import org.hamcrest.core.AllOf;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.zulip.android.helper.Matchers.withFirstId;
import static com.zulip.android.helper.Matchers.withMessageHeaderHolder;
import static com.zulip.android.helper.Matchers.withMessageHolder;
import static com.zulip.android.helper.Matchers.withMessageHolderAndClick;
import static com.zulip.android.helper.ViewAssertions.checkMessagesOnlyFromToday;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;


/**
 * Make sure you have entered the login details here{@link BaseTest#EMAIL_TEST}
 * And your password here{@link BaseTest#PASSWORD_TEST} to login if you have not logged in!
 */
@LargeTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
public class RecyclerViewTests {
    @Rule
    public ActivityTestRule<ZulipActivity> mActivityTestRule = new ActivityTestRule<>(ZulipActivity.class);

    public static String LOG_TAG = RecyclerViewTests.class.getName();
    private static String testMessageStream;
    private static String testMessagePrivate;

    private ZulipApp app;

    @Before
    public void setUp() {
        app = ZulipApp.get();

        if (ZulipApp.get().getApiKey() == null) {
            BaseTest baseTest = new BaseTest();
            baseTest.login();
            sleep(4000);
        }

        //This is to make sure the latest recieved messages will be added to the list!
        app.setPointer(app.getMaxMessageId());
        setTestMessageStream((testMessageStream == null) ? RandomStringUtils.randomAlphanumeric(10) : testMessageStream);
        setTestMessagePrivate((testMessagePrivate == null) ? RandomStringUtils.randomAlphanumeric(15) : testMessagePrivate);

    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            ZLog.logException(e);
        }
    }

    @Test
    public void sendStreamMessage() {
        //Wait to make sure the messages are loaded
        sleep(2000);

        onView(withId(R.id.recyclerView)).perform(RecyclerViewActions.scrollToHolder(withMessageHolderAndClick(MessageType.STREAM_MESSAGE, R.id.contentView)));
        sleep(1000);

        //Fill message EditText
        ViewInteraction messageInteraction = onView(allOf(withId(R.id.message_et), isDisplayed()));
        messageInteraction.perform(replaceText(testMessageStream));

        //Click Send Button
        ViewInteraction imageView = onView(allOf(withId(R.id.send_btn), isDisplayed()));
        imageView.perform(click());

        sleep(2000);

        //Scroll And check the new sent message
        onView(withId(R.id.recyclerView)).perform(RecyclerViewActions.scrollToHolder(withMessageHolder(testMessageStream, R.id.contentView)));

        onView(AllOf.allOf(withId(R.id.contentView), withText(testMessageStream))).check(matches(isDisplayed()));

        checkIfMessagesMatchTheHeaderParent();
        checkOrderOfMessagesCurrentList();
    }

    private void hideToolBar() {
        try {
            mActivityTestRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mActivityTestRule.getActivity().hideView(mActivityTestRule.getActivity().findViewById(R.id.appBarLayout));
                }
            });
        } catch (Throwable throwable) {
            ZLog.logException(throwable);
        }
    }

    @Test
    public void checkAfterNarrowToStream() {
        sleep(2000);

        hideToolBar();

        sleep(2000);

        //Scroll to Stream MessageHeaderParent
        onView(withId(R.id.recyclerView)).perform(RecyclerViewActions.scrollToHolder(withMessageHeaderHolder(MessageType.STREAM_MESSAGE)));

        //Perform a click on the subject textView to narrow to the topic
        onView(withFirstId(R.id.instance)).perform(click());

        sleep(4000);
        //Check if all messages belong to this subject
        onView(withId(R.id.recyclerView)).check(ViewAssertions.checkIfBelongToSameNarrow(mActivityTestRule.getActivity()));
    }

    @Test
    public void checkAfterNarrowToPrivate() {
        sleep(2000);

        try {
            mActivityTestRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mActivityTestRule.getActivity().hideView(mActivityTestRule.getActivity().findViewById(R.id.appBarLayout));
                }
            });
        } catch (Throwable throwable) {
            ZLog.logException(throwable);
        }
        sleep(2000);

        //Scroll to private MessageHeaderParent
        onView(withId(R.id.recyclerView)).perform(RecyclerViewActions.scrollToHolder(withMessageHeaderHolder(MessageType.PRIVATE_MESSAGE)));

        //Perform a click on the recipients to narrow to that group or single person
        onView(withFirstId(R.id.instance)).perform(click());

        sleep(4000);

        //Check if all messages belong to this private message group
        onView(withId(R.id.recyclerView)).check(ViewAssertions.checkIfBelongToSameNarrow(mActivityTestRule.getActivity()));
    }


    @Test
    public void checkOrderOfMessagesCurrentList() {
        sleep(2000);
        onView(withId(R.id.recyclerView)).check(ViewAssertions.checkOrderOfMessages(mActivityTestRule.getActivity()));
    }

    @Test
    public void checkIfMessagesMatchTheHeaderParent() {
        sleep(2000);
        onView(withId(R.id.recyclerView)).check(ViewAssertions.checkIfMessagesMatchTheHeaderParent(mActivityTestRule.getActivity()));
    }


    @Test
    public void checkTodaysFilter() {
        sleep(2000);
        /**
         * Narrow to today messages
         * runOnUiThread for changing fragment here{@link ZulipActivity#pushListFragment(MessageListFragment, String)}
         */
        try {
            mActivityTestRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mActivityTestRule.getActivity().doNarrow(new NarrowFilterToday());
                }
            });
        } catch (Throwable throwable) {
            ZLog.logException(throwable);
        }

        sleep(2000);
        //Check messages if only they are from today
        onView(withId(R.id.recyclerView)).check(checkMessagesOnlyFromToday());
    }


    @Test
    public void sendPrivateMessage() {
        //Wait to make sure the messages are loaded
        sleep(2000);

        onView(withId(R.id.recyclerView)).perform(RecyclerViewActions.scrollToHolder(withMessageHolderAndClick(MessageType.PRIVATE_MESSAGE, R.id.contentView)));
        sleep(1000);

        //Fill message EditText
        ViewInteraction messageInteraction = onView(allOf(withId(R.id.message_et), isDisplayed()));
        messageInteraction.perform(replaceText(testMessagePrivate));

        //Click Send Button
        ViewInteraction imageView = onView(allOf(withId(R.id.send_btn), isDisplayed()));
        imageView.perform(click());

        sleep(2000);

        //Scroll And check the new sent message
        onView(withId(R.id.recyclerView)).perform(RecyclerViewActions.scrollToHolder(withMessageHolder(testMessagePrivate, R.id.contentView)));

        onView(AllOf.allOf(withId(R.id.contentView), withText(testMessagePrivate))).check(matches(isDisplayed()));

        checkIfMessagesMatchTheHeaderParent();
        checkOrderOfMessagesCurrentList();
    }

    @Test
    public void switchChatBox() {
        sleep(2000);
        //Show the Fab button
        try {
            mActivityTestRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mActivityTestRule.getActivity().showView(mActivityTestRule.getActivity().fab);
                }
            });
        } catch (Throwable throwable) {
            ZLog.logException(throwable);
        }

        sleep(500);
        //Click Fab Button
        ViewInteraction fabInteraction = onView(allOf(withId(R.id.fab), isDisplayed()));
        fabInteraction.perform(click());

        sleep(500);
        //Click Switch Button
        ViewInteraction switchBtnInteraction = onView(allOf(withId(R.id.togglePrivateStream_btn), isDisplayed()));
        switchBtnInteraction.perform(click());

        sleep(500);
        //Check if Arrow TextView is not displayed for switchingChatBox
        onView(withId(R.id.textView)).check(matches(not(isDisplayed())));
    }

    public static String getTestMessageStream() {
        return testMessageStream;
    }

    public static void setTestMessageStream(String testMessageStream) {
        RecyclerViewTests.testMessageStream = testMessageStream;
    }

    public static String getTestMessagePrivate() {
        return testMessagePrivate;
    }

    public static void setTestMessagePrivate(String testMessagePrivate) {
        RecyclerViewTests.testMessagePrivate = testMessagePrivate;
    }
}
