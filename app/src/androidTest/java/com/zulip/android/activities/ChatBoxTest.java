package com.zulip.android.activities;


import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.zulip.android.R;
import com.zulip.android.ZulipApp;
import com.zulip.android.models.Message;
import com.zulip.android.models.MessageType;

import org.apache.commons.lang.RandomStringUtils;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;


@LargeTest
@RunWith(AndroidJUnit4.class)
public class ChatBoxTest extends BaseTest {
    @Rule
    public ActivityTestRule<ZulipActivity> mActivityTestRule = new ActivityTestRule<>(ZulipActivity.class);

    private static String testMessageStream;
    private static String testMessagePrivate;

    @Before
    public void setUp() {
        testMessageStream = (testMessageStream == null) ? RandomStringUtils.randomAlphanumeric(10) : testMessageStream;
        testMessagePrivate = (testMessagePrivate == null) ? RandomStringUtils.randomAlphanumeric(15) : testMessagePrivate;
        if (ZulipApp.get().getApiKey() == null) {
            login();
        }
    }


    @Test
    public void sendStreamMessage() {
        //Fill ChatBox by clicking content TextView in the ListView
        onData(allOf(instanceOf(Message.class),
                getMessageFromType(MessageType.STREAM_MESSAGE)))
                .inAdapterView(withId(R.id.listview))
                .atPosition(0)
                .onChildView(withId(R.id.contentView))
                .perform(click());

        //Fill message EditText
        ViewInteraction editText2 = onView(allOf(withId(R.id.message_et), isDisplayed()));
        editText2.perform(replaceText(testMessageStream));

        //Click Send Button
        ViewInteraction imageView = onView(allOf(withId(R.id.send_btn), isDisplayed()));
        imageView.perform(click());

        //Verify sent Message
        onData(allOf(instanceOf(Message.class),
                getMessageFromTypeAndContentString(MessageType.STREAM_MESSAGE, testMessageStream)))
                .inAdapterView(withId(R.id.listview))
                .check(matches(isDisplayed()));
    }


    @Test
    public void sendPrivateMessage() {
        //Fill ChatBox by clicking content TextView in the ListView
        onData(allOf(instanceOf(Message.class),
                getMessageFromType(MessageType.PRIVATE_MESSAGE)))
                .inAdapterView(withId(R.id.listview)).atPosition(0)
                .onChildView(withId(R.id.contentView))
                .perform(click());

        //Fill message EditText
        ViewInteraction editText2 = onView(allOf(withId(R.id.message_et), isDisplayed()));
        editText2.perform(replaceText(testMessagePrivate));

        //Click Send Button
        ViewInteraction imageView = onView(allOf(withId(R.id.send_btn), isDisplayed()));
        imageView.perform(click());

        //Verify sent Message
        onData(allOf(instanceOf(Message.class),
                getMessageFromTypeAndContentString(MessageType.PRIVATE_MESSAGE, testMessagePrivate)))
                .inAdapterView(withId(R.id.listview))
                .check(matches(isDisplayed()));
    }

    @Test
    public void switchChatBox() {
        //Click Switch Button
        ViewInteraction imageView = onView(allOf(withId(R.id.togglePrivateStream_btn), isDisplayed()));
        imageView.perform(click());
    }


    public static Matcher<Object> getMessageFromType(final MessageType messageType) {
        return new BoundedMatcher<Object, Message>(Message.class) {
            @Override
            public void describeTo(org.hamcrest.Description description) {
                description.appendText("Error");
            }

            @Override
            protected boolean matchesSafely(Message item) {
                return item.getType() == messageType;
            }
        };
    }

    public static Matcher<Object> getMessageFromTypeAndContentString(final MessageType messageType, final String text) {
        return new BoundedMatcher<Object, Message>(Message.class) {
            @Override
            public void describeTo(org.hamcrest.Description description) {
                description.appendText("Error");
            }

            @Override
            protected boolean matchesSafely(Message item) {
                return item.getType() == messageType && item.getContent().contains(text);
            }
        };
    }
}
