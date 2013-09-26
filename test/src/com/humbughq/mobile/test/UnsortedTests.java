package com.humbughq.mobile.test;

import org.json.JSONException;
import org.json.JSONObject;

import com.humbughq.mobile.HumbugActivity;
import com.humbughq.mobile.Message;
import com.humbughq.mobile.Person;
import com.humbughq.mobile.ZulipApp;

import android.content.Intent;
import android.test.ActivityUnitTestCase;

public class UnsortedTests extends ActivityUnitTestCase<HumbugActivity> {

    private ZulipApp app;

    public UnsortedTests() {
        super(HumbugActivity.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
        app = new ZulipApp();
    }

    public void testMessageCreation() throws JSONException {
        testInit();
        JSONObject jsonObject;
        jsonObject = new JSONObject(
                "{\"recipient_id\": 314, \"sender_email\": \"lfaraone@zulip.com\", \"timestamp\": 1379966441, \"display_recipient\": \"test\", \"sender_id\": 15, \"sender_full_name\": \"Luke Faraone\", \"sender_domain\": \"zulip.com\", \"content\": \"Hello!\", \"gravatar_hash\": \"9cd8e1981dc89f3221c5077dd8a22515\", \"avatar_url\": \"https://secure.gravatar.com/avatar/9cd8e1981dc89f3221c5077dd8a22515?d=identicon\", \"client\": \"website\", \"content_type\": \"text/x-markdown\", \"subject_links\": [], \"sender_short_name\": \"lfaraone\", \"type\": \"stream\", \"id\": 10594623, \"subject\": \"toast\"}");
        Message msg = new Message((ZulipApp) getActivity().getApplication(),
                jsonObject);
        assertEquals(msg.getID(), 10594623);
        assertEquals(msg.getSender(), new Person("Luke Faraone",
                "lfaraone@zulip.com"));
    }

    /**
     * Run this before each test to set up the activity.
     */
    private void testInit() {
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

    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

}
