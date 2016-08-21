package com.zulip.android.helper;

import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.ViewAssertion;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;

import com.zulip.android.ZulipApp;
import com.zulip.android.activities.RecyclerMessageAdapter;
import com.zulip.android.activities.ZulipActivity;
import com.zulip.android.models.Message;
import com.zulip.android.models.MessageType;
import com.zulip.android.util.ZLog;
import com.zulip.android.viewholders.MessageHeaderParent;

import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static com.zulip.android.activities.RecyclerViewTests.LOG_TAG;
import static junit.framework.Assert.assertTrue;

public class ViewAssertions {
    public static ViewAssertion checkMessagesOnlyFromToday() {
        return new ViewAssertion() {
            @Override
            public void check(View view, NoMatchingViewException e) {
                if (!(view instanceof RecyclerView)) {
                    throw e;
                }
                RecyclerView rv = (RecyclerView) view;
                RecyclerMessageAdapter recyclerMessageAdapter = (RecyclerMessageAdapter) rv.getAdapter();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("OfDate-" + (new Date()).toString() + " { ");
                for (int index = 0; index < recyclerMessageAdapter.getItemCount(); index++) {
                    if (recyclerMessageAdapter.getItem(index) instanceof Message) {
                        Message message = (Message) recyclerMessageAdapter.getItem(index);
                        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
                        stringBuilder.append(message.getID() + "-" + sdf.format(message.getTimestamp()) + " , ");
                        assertTrue("This message is not of today ID=" + message.getID() + ":" + message.getIdForHolder() + "\ncontent=" + message.getContent(), DateUtils.isToday(message.getTimestamp().getTime()));
                    }
                }
                stringBuilder.append(" }");
                printLogInPartsIfExceeded(stringBuilder, "checkMessagesOnlyFromToday");
            }
        };
    }

    public static ViewAssertion checkOrderOfMessages(final ZulipActivity zulipActivity) {
        //ZulipActivity is needed for generating detailed info about position if doesn't matches
        return new ViewAssertion() {
            @Override
            public void check(View view, NoMatchingViewException noMatchingViewException) {
                if (!(view instanceof RecyclerView)) {
                    throw noMatchingViewException;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append('{');
                RecyclerMessageAdapter recyclerMessageAdapter = (RecyclerMessageAdapter) ((RecyclerView) view).getAdapter();
                for (int i = 0; i < recyclerMessageAdapter.getItemCount() - 1; i++) {
                    Object object = recyclerMessageAdapter.getItem(i);
                    if (object instanceof Message) {
                        if (recyclerMessageAdapter.getItem(i + 1) instanceof Message) {
                            boolean checkOrder = (((Message) object).getID() < ((Message) recyclerMessageAdapter.getItem(i + 1)).getID());
                            assertTrue(generateDetailedInfoForOrder(((Message) object), ((Message) recyclerMessageAdapter.getItem(i + 1)), zulipActivity, i), checkOrder);
                        }
                        stringBuilder.append(((Message) object).getID() + ",");
                    } else if (object instanceof MessageHeaderParent) {
                        stringBuilder.append("} | { ");
                    }
                }
                stringBuilder.append('}');
                printLogInPartsIfExceeded(stringBuilder, "checkOrderOfMessages");
            }
        };
    }

    private static String generateDetailedInfoForOrder(Message msg1, Message msg2, ZulipActivity zulipActivity, int index) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("atIndex: " + index);
        stringBuilder.append("Info for Message 1");
        stringBuilder.append("Message ID:" + msg1.getID() + "\n");
        stringBuilder.append("Message idForHolder:" + msg1.getIdForHolder() + "\n");
        stringBuilder.append("Message content:" + msg1.getContent() + "\n");
        stringBuilder.append("Info for Message 2\n");
        stringBuilder.append("Message ID:" + msg2.getID() + "\n");
        stringBuilder.append("Message idForHolder:" + msg2.getIdForHolder() + "\n");
        stringBuilder.append("Message content:" + msg2.getContent() + "\n");
        if (zulipActivity.currentList.filter != null) {
            try {
                stringBuilder.append("JSONFilter json:" + zulipActivity.currentList.filter.getJsonFilter());
            } catch (JSONException e) {
                ZLog.logException(e);
            }
            stringBuilder.append("currentTitle: " + zulipActivity.currentList.filter.getTitle());
            stringBuilder.append("currentSubTitle: " + zulipActivity.currentList.filter.getSubtitle());
        } else {
            stringBuilder.append("currentZulipActivity: " + "homeList");
        }

        return stringBuilder.toString();
    }

    public static ViewAssertion checkIfMessagesMatchTheHeaderParent(final ZulipActivity zulipActivity) {
        return new ViewAssertion() {
            @Override
            public void check(View view, NoMatchingViewException noMatchingViewException) {
                if (!(view instanceof RecyclerView)) {
                    throw noMatchingViewException;
                }

                RecyclerMessageAdapter recyclerMessageAdapter = (RecyclerMessageAdapter) ((RecyclerView) view).getAdapter();
                String lastHolderId = null;
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i < recyclerMessageAdapter.getItemCount() - 1; i++) {
                    Object object = recyclerMessageAdapter.getItem(i);
                    if (object instanceof MessageHeaderParent) {
                        lastHolderId = ((MessageHeaderParent) object).getId();
                        stringBuilder.append("} \'MHP\'-\'" + lastHolderId + " {");
                    } else if (object instanceof Message) {
                        Message message = (Message) object;
                        boolean checkOrder = (lastHolderId.equals(message.getIdForHolder()));
                        assertTrue(generateDetailDebugInfoForCorrectHeaderParent(message, zulipActivity, lastHolderId, i), checkOrder);
                        stringBuilder.append(message.getIdForHolder() + "-" + message.getID() + " , ");
                    }
                }
                stringBuilder.append('}');
                printLogInPartsIfExceeded(stringBuilder, "checkIfMessagesMatchTheHeaderParent");
            }
        };
    }

    private static String generateDetailDebugInfoForCorrectHeaderParent(Message message, ZulipActivity zulipActivity, String lastHolderId, int index) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("AtIndex:" + index + "\n");
        stringBuilder.append("LastHolderId MessageHeaderParent:" + lastHolderId + "\n");
        stringBuilder.append("Info for Message 1\n");
        stringBuilder.append("Message ID:" + message.getID() + "\n");
        stringBuilder.append("Message idForHolder:" + message.getIdForHolder() + "\n");
        stringBuilder.append("Message content:" + message.getContent() + "\n");
        if (zulipActivity.currentList.filter != null) {

            try {
                stringBuilder.append("JSONFilter json:" + zulipActivity.currentList.filter.getJsonFilter());
            } catch (JSONException e) {
                ZLog.logException(e);
            }
            stringBuilder.append("currentTitle: " + zulipActivity.currentList.filter.getTitle());
            stringBuilder.append("currentSubTitle: " + zulipActivity.currentList.filter.getSubtitle());
        } else {
            stringBuilder.append("currentZulipActivity: " + "homeList");

        }
        return stringBuilder.toString();
    }

    public static ViewAssertion checkIfBelongToSameNarrow(final ZulipActivity zulipActivity) {
        return new ViewAssertion() {
            @Override
            public void check(View view, NoMatchingViewException noMatchingViewException) {
                if (!(view instanceof RecyclerView)) {
                    throw noMatchingViewException;
                }
                RecyclerMessageAdapter recyclerMessageAdapter = (RecyclerMessageAdapter) ((RecyclerView) view).getAdapter();
                String lastHolderId = null;
                int headerParentsCount = 0;
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i < recyclerMessageAdapter.getItemCount() - 1; i++) {
                    Object object = recyclerMessageAdapter.getItem(i);
                    if (object instanceof MessageHeaderParent) {
                        headerParentsCount++;
                        assertTrue(generateDetailInfoSameNarrow((MessageHeaderParent) object, i, zulipActivity, lastHolderId), headerParentsCount <= 1);
                        lastHolderId = ((MessageHeaderParent) object).getId();
                        stringBuilder.append(lastHolderId + " { ");
                    } else if (object instanceof Message) {
                        boolean checkId = ((Message) object).getIdForHolder().equals(lastHolderId);
                        assertTrue(generateDetailInfoSameNarrow(((Message) object), zulipActivity, lastHolderId, i), checkId);
                        stringBuilder.append(((Message) object).getIdForHolder() + " , ");
                    }
                }
                stringBuilder.append(" }");
                printLogInPartsIfExceeded(stringBuilder, "checkIfBelongToSameNarrow");
            }
        };
    }

    private static String generateDetailInfoSameNarrow(Message message, ZulipActivity zulipActivity, String lastHolderId, int index) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("AtIndex:" + index + "\n");
        stringBuilder.append("lastHolderId:" + lastHolderId + "\n");
        stringBuilder.append("message id:" + message.getIdForHolder() + "\n");
        stringBuilder.append("vmessageType:" + message.getType().toString() + "\n");
        stringBuilder.append("message displayRecipent:" + message.getDisplayRecipient(ZulipApp.get()) + "\n");
        if (message.getType() == MessageType.STREAM_MESSAGE) {
            stringBuilder.append("message getStream:" + message.getStream().getName() + "\n");
            stringBuilder.append("message getSubject:" + message.getSubject() + "\n");
        } else {
            stringBuilder.append("message getRawRecipients:" + message.getRawRecipients() + "\n");
        }
        stringBuilder.append("Info for Message 1\n");
        stringBuilder.append("\nCurrentListInfo:\n");
        if (zulipActivity.currentList.filter != null) {
            try {
                stringBuilder.append("JSONFilter json:" + zulipActivity.currentList.filter.getJsonFilter());
            } catch (JSONException e) {
                ZLog.logException(e);
            }
            stringBuilder.append("currentTitle: " + zulipActivity.currentList.filter.getTitle());
            stringBuilder.append("currentSubTitle: " + zulipActivity.currentList.filter.getSubtitle());
        } else {
            stringBuilder.append("currentZulipActivity: " + "homeList");
        }
        return stringBuilder.toString();

    }

    private static String generateDetailInfoSameNarrow(MessageHeaderParent object, int index, ZulipActivity zulipActivity, String lastHolderId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("AtIndex:" + index + "\n");
        stringBuilder.append("lastHolderId:" + lastHolderId + "\n");
        stringBuilder.append("MessageHeaderParent id:" + object.getId() + "\n");
        stringBuilder.append("MessageHeaderParent messageType:" + object.getMessageType().toString() + "\n");
        stringBuilder.append("MessageHeaderParent displayRecipent:" + object.getDisplayRecipent() + "\n");
        stringBuilder.append("MessageHeaderParent getStream:" + object.getStream() + "\n");
        stringBuilder.append("MessageHeaderParent getSubject:" + object.getSubject() + "\n");
        stringBuilder.append("Info for Message 1\n");
        if (zulipActivity.currentList.filter != null) {

            try {
                stringBuilder.append("JSONFilter json:" + zulipActivity.currentList.filter.getJsonFilter());
            } catch (JSONException e) {
                ZLog.logException(e);
            }
            stringBuilder.append("currentTitle: " + zulipActivity.currentList.filter.getTitle());
            stringBuilder.append("currentSubTitle: " + zulipActivity.currentList.filter.getSubtitle());
        } else {
            stringBuilder.append("currentZulipActivity: " + "homeList");

        }
        return stringBuilder.toString();
    }


    private static void printLogInPartsIfExceeded(StringBuilder stringBuilder, String methodName) {
        if (stringBuilder.length() > 1800) {
            int chunkCount = stringBuilder.length() / 4000;
            for (int i = 0; i <= chunkCount; i++) {
                int max = 4000 * (i + 1);
                if (max >= stringBuilder.length()) {
                    Log.d(LOG_TAG, methodName + ": " + stringBuilder.substring(4000 * i));
                } else {
                    Log.d(LOG_TAG, methodName + ": " + stringBuilder.substring(4000 * i, max));
                }
            }
        } else {
            Log.d(LOG_TAG, stringBuilder.toString());
        }
    }


    public static ViewAssertion hasItemsCount() {
        return new ViewAssertion() {
            @Override
            public void check(View view, NoMatchingViewException e) {
                if (!(view instanceof RecyclerView)) {
                    throw e;
                }
                RecyclerView rv = (RecyclerView) view;
                assertThat("Items less than 2, which means no E-Mails!", rv.getAdapter().getItemCount(), org.hamcrest.Matchers.greaterThan(2));
            }
        };
    }
}