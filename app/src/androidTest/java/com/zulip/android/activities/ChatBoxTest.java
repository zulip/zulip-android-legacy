package com.zulip.android.activities;


import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.text.TextUtils;

import com.j256.ormlite.stmt.QueryBuilder;
import com.zulip.android.R;
import com.zulip.android.ZulipApp;
import com.zulip.android.models.Message;
import com.zulip.android.models.MessageType;
import com.zulip.android.models.Person;

import org.apache.commons.lang.RandomStringUtils;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;


@LargeTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
public class ChatBoxTest extends BaseTest {
    @Rule
    public ActivityTestRule<ZulipActivity> mActivityTestRule = new ActivityTestRule<>(ZulipActivity.class);

    private static String testMessageStream;
    private static String testMessagePrivate;

    private static String testStreamName;
    private static String testSubjectStream;
    private List<Person> testPersons;
    private ZulipApp app;

    @Before
    public void setUp() {
        setTestMessageStream((testMessageStream == null) ? RandomStringUtils.randomAlphanumeric(10) : testMessageStream);
        setTestMessagePrivate((testMessagePrivate == null) ? RandomStringUtils.randomAlphanumeric(15) : testMessagePrivate);
        if (ZulipApp.get().getApiKey() == null) {
            login();
        }
        app = ZulipApp.get();
        setUpTestStream();
        setUpPrivate();
    }

    private void setUpPrivate() {
        try {
            QueryBuilder<Message, Object> orderQb = app.getDao(Message.class).queryBuilder();
            orderQb.where().eq(Message.TYPE_FIELD, MessageType.PRIVATE_MESSAGE).and()
                    .isNotNull(Message.RECIPIENTS_FIELD);
            Message message = orderQb.queryForFirst();
            testPersons = Arrays.asList(message.getPersonalReplyTo(app));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setUpTestStream() {
        try {
            QueryBuilder<Message, Object> messageQueryBuilder = app.getDao(Message.class).queryBuilder();
            messageQueryBuilder.where().eq(Message.TYPE_FIELD, MessageType.STREAM_MESSAGE).and()
                    .isNotNull(Message.SUBJECT_FIELD).and()
                    .isNotNull(Message.STREAM_FIELD);
            Message message = messageQueryBuilder.queryForFirst();
            testStreamName = message.getStream().getName();
            testSubjectStream = message.getSubject();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void sendStreamMessage() {
        //Fill message EditText
        ViewInteraction streamET = onView(allOf(withId(R.id.stream_actv), isDisplayed()));
        streamET.perform(replaceText(testStreamName));

        //Fill message EditText
        ViewInteraction subjectET = onView(allOf(withId(R.id.topic_actv), isDisplayed()));
        subjectET.perform(replaceText(testSubjectStream));

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

    private String getDisplayRecipents() {
        ArrayList<String> names = new ArrayList<String>();
        for (Person person : testPersons) {
            if (person.getId() != app.getYou().getId()) {
                names.add(person.getEmail());
            }
        }
        return TextUtils.join(", ", names);
    }

    @Test
    public void sendPrivateMessage() {
        //Click Switch Button
        ViewInteraction imageView = onView(allOf(withId(R.id.togglePrivateStream_btn), isDisplayed()));
        imageView.perform(click());

        //Fill message EditText
        ViewInteraction recipentET = onView(allOf(withId(R.id.topic_actv), isDisplayed()));
        recipentET.perform(replaceText(getDisplayRecipents()));

        //Fill message EditText
        ViewInteraction editText2 = onView(allOf(withId(R.id.message_et), isDisplayed()));
        editText2.perform(replaceText(testMessagePrivate));

        //Click Send Button
        ViewInteraction sendBtn = onView(allOf(withId(R.id.send_btn), isDisplayed()));
        sendBtn.perform(click());

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

        //Check if Arrow TextView is not displayed for switchingChatBox
        onView(withId(R.id.textView)).check(matches(not(isDisplayed())));
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

    public static String getTestMessageStream() {
        return testMessageStream;
    }

    public static void setTestMessageStream(String testMessageStream) {
        ChatBoxTest.testMessageStream = testMessageStream;
    }

    public static String getTestMessagePrivate() {
        return testMessagePrivate;
    }

    public static void setTestMessagePrivate(String testMessagePrivate) {
        ChatBoxTest.testMessagePrivate = testMessagePrivate;
    }
}
