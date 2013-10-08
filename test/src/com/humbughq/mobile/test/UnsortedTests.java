package com.humbughq.mobile.test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.test.ActivityUnitTestCase;

import com.humbughq.mobile.HumbugActivity;
import com.humbughq.mobile.Message;
import com.humbughq.mobile.MessageListFragment;
import com.humbughq.mobile.MessageListener.LoadPosition;
import com.humbughq.mobile.MessageRange;
import com.humbughq.mobile.MessageType;
import com.humbughq.mobile.Person;
import com.humbughq.mobile.ZulipApp;
import com.humbughq.mobile.test.mutated.FakeAsyncGetOldMessages;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.misc.TransactionManager;

public class UnsortedTests extends ActivityUnitTestCase<HumbugActivity> {

    private ZulipApp app;
    RuntimeExceptionDao<Message, Object> messageDao;

    public UnsortedTests() {
        super(HumbugActivity.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
        app = new ZulipApp();

    }

    public void testMessageCreation() throws JSONException {
        prepTests();
        JSONObject jsonObject;
        jsonObject = new JSONObject(
                "{\"recipient_id\": 314, \"sender_email\": \"lfaraone@zulip.com\", \"timestamp\": 1379966441, \"display_recipient\": \"test\", \"sender_id\": 15, \"sender_full_name\": \"Luke Faraone\", \"sender_domain\": \"zulip.com\", \"content\": \"Hello!\", \"gravatar_hash\": \"9cd8e1981dc89f3221c5077dd8a22515\", \"avatar_url\": \"https://secure.gravatar.com/avatar/9cd8e1981dc89f3221c5077dd8a22515?d=identicon\", \"client\": \"website\", \"content_type\": \"text/x-markdown\", \"subject_links\": [], \"sender_short_name\": \"lfaraone\", \"type\": \"stream\", \"id\": 10594623, \"subject\": \"toast\"}");
        Message msg = new Message((ZulipApp) getActivity().getApplication(),
                jsonObject);
        assertEquals(msg.getID(), 10594623);
        assertEquals(msg.getSender(), new Person("Luke Faraone",
                "lfaraone@zulip.com"));
    }

    public void testMessageTrim() throws SQLException {
        prepTests();
        TransactionManager.callInTransaction(app.getDatabaseHelper()
                .getConnectionSource(), new Callable<Void>() {
            public Void call() throws Exception {
                for (int i = 1; i <= 300; i++) {
                    sampleMessage(app, i);
                }

                for (int i = 501; i <= 800; i++) {
                    sampleMessage(app, i);
                }

                app.getDao(MessageRange.class).create(new MessageRange(1, 300));
                app.getDao(MessageRange.class).create(
                        new MessageRange(501, 800));
                return null;
            }
        });

        RuntimeExceptionDao<MessageRange, Integer> messageRangeDao = app
                .getDao(MessageRange.class);

        assertEquals(600, messageDao.countOf());
        Message.trim(100, app);
        @SuppressWarnings("unused")
        List<Message> messages = this.messageDao.queryForAll();
        assertEquals(100, messageDao.countOf());
        assertEquals(1, messageRangeDao.countOf());
        MessageRange r = messageRangeDao.queryBuilder().queryForFirst();
        // We have messages 701 through 800, which is 100 messages.
        assertEquals(800, r.high);
        assertEquals(800 - 99, r.low);

    }

    public void testAGOMFetch() throws SQLException, InterruptedException,
            ExecutionException {
        prepTests();
        MessageListFragment fragment = MessageListFragment.newInstance(null);
        fragment.app = app;
        FakeAsyncGetOldMessages request = new FakeAsyncGetOldMessages(fragment);
        request.shouldFmSucceed = true;
        request.appendTheseMessages = new ArrayList<Message>();
        Message m1 = sampleMessage(app, 40);
        Message m2 = sampleMessage(app, 45);
        Message m3 = sampleMessage(app, 60);
        request.appendTheseMessages.add(m1);
        request.appendTheseMessages.add(m2);
        request.appendTheseMessages.add(m3);
        request.execute(50, LoadPosition.INITIAL, 10, 10, null);
        request.get();
        // Should result in a MR of 40, 60

        MessageRange mr = app.getDao(MessageRange.class).queryForAll().get(0);
        assertEquals(40, mr.low);
        assertEquals(60, mr.high);

        // Now fetching inside that range should be safe.
        request = new FakeAsyncGetOldMessages(fragment);
        request.execute(45, LoadPosition.INITIAL, 1, 0, null);
        request.get();
        assertFalse(request.fmCalled);
        assertEquals(2, request.receivedMessages.size());

        // Now let's test coalescing...
        // The fetch won't be in cache here, but one message will already be
        // retrieved.
        request = new FakeAsyncGetOldMessages(fragment);
        request.shouldFmSucceed = true;
        request.appendTheseMessages = new ArrayList<Message>();
        Message m0 = sampleMessage(app, 35);
        request.appendTheseMessages.add(m0);
        request.appendTheseMessages.add(m1);
        request.execute(36, LoadPosition.INITIAL, 1, 1, null);
        request.get();
        assertEquals(2, request.receivedMessages.size());
        List<MessageRange> mrs = app.getDao(MessageRange.class).queryForAll();
        assertEquals(1, mrs.size());
        assertEquals(35, mrs.get(0).low);
        assertEquals(60, mrs.get(0).high);

        // And test partial hits for good measure!

        request = new FakeAsyncGetOldMessages(fragment);
        request.shouldFmSucceed = true;
        request.execute(36, LoadPosition.INITIAL, 2, 0, null);
        request.get();

        // 35 should be in cache
        assertEquals(1, request.receivedMessages.size());
        assertEquals(false, request.fmCalled);
        // Recursing in one direction
        assertEquals(1, request.recurseRequestsReceived.size());

        request = request.recurseRequestsReceived.get(0);
        request.shouldFmSucceed = true;
        assertEquals(1, request.fmNumBefore);
        request.appendTheseMessages = new ArrayList<Message>();
        Message mn1 = sampleMessage(app, 33);
        request.appendTheseMessages.add(mn1);
        request.executeBasedOnPresetValues();
        request.get();

        assertEquals(true, request.fmCalled);
        assertEquals(1, request.receivedMessages.size());
        mrs = app.getDao(MessageRange.class).queryForAll();
        assertEquals(1, mrs.size());
        assertEquals(33, mrs.get(0).low);

    }

    protected Message sampleMessage(ZulipApp app, int id) throws SQLException {
        Message rtr = new Message(app);
        rtr.setSender(Person.getOrUpdate(app, "Test User",
                "testuser@example.com", ""));
        rtr.setContent("Test message");
        rtr.setType(MessageType.PRIVATE_MESSAGE);
        rtr.setRecipient(new String[] { "testuser@example.com" });
        rtr.setID(id);
        messageDao.create(rtr);
        return rtr;
    }

    /**
     * Run this before each test to set up the activity.
     */
    protected void prepTests() {
        setApplication(app);
        this.startActivity(new Intent(getInstrumentation().getTargetContext(),
                HumbugActivity.class), null, null);
        this.getInstrumentation().waitForIdleSync();
        app.setContext(getInstrumentation().getTargetContext());
        // Need to setEmail twice to reinitialise the database after destroying
        // it.
        app.setEmail("testuser@example.com");
        app.deleteDatabase(app.getDatabaseHelper().getDatabaseName());
        app.setEmail("testuser@example.com");
        messageDao = app.getDao(Message.class);

    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

}
