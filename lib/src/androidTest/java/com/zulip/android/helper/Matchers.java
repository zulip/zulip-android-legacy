package com.zulip.android.helper;

import android.content.res.Resources;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.zulip.android.R;
import com.zulip.android.models.Message;
import com.zulip.android.models.MessageType;
import com.zulip.android.viewholders.MessageHeaderParent;
import com.zulip.android.viewholders.MessageHolder;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class Matchers {

    public static Matcher<View> withFirstId(final int id) {
        return new TypeSafeMatcher<View>() {
            Resources resources = null;
            boolean found = false;

            @Override
            public void describeTo(Description description) {
                String idDescription = Integer.toString(id);
                if (resources != null) {
                    try {
                        idDescription = resources.getResourceName(id);
                    } catch (Resources.NotFoundException e) {
                        // No big deal, will just use the int value.
                        idDescription = String.format("%s (resource name not found)", id);
                    }
                }
                description.appendText("with id: " + idDescription);
            }

            @Override
            public boolean matchesSafely(View view) {
                if (found) return false;
                resources = view.getResources();
                if (id == view.getId()) {
                    found = true;
                    return true;
                }
                return false;
            }
        };
    }

    public static Matcher<RecyclerView.ViewHolder> withMessageHolderAndClick(final MessageType messageType, final int id) {
        return new BoundedMatcher<RecyclerView.ViewHolder, MessageHolder>(MessageHolder.class) {
            private boolean found = false;

            @Override
            public void describeTo(Description description) {
                description.appendText("No ViewHolder found with text: " + messageType.toString());
            }

            @Override
            protected boolean matchesSafely(MessageHolder item) {
                if (found) return false;
                if (item.getLayoutPosition() < 0) return false;
                Message message = item.onItemClickListener.getMessageAtPosition(item.getLayoutPosition());
                if (message == null) return false;
                if (messageType == message.getType()) {
                    if (id == R.id.contentView) {
                        item.onItemClickListener.onItemClick(R.id.contentView, item.getLayoutPosition());
                    }
                    found = true;
                    return true;
                } else {
                    return false;
                }
            }
        };
    }

    public static Matcher<RecyclerView.ViewHolder> withMessageHolder(final String text, final int textViewId) {
        return new BoundedMatcher<RecyclerView.ViewHolder, MessageHolder>(MessageHolder.class) {
            private boolean found = false;

            @Override
            public void describeTo(Description description) {
                description.appendText("No ViewHolder found with text: ");
            }

            @Override
            protected boolean matchesSafely(MessageHolder item) {
                if (found) return false;
                found = ((TextView) item.itemView.findViewById(textViewId)).getText().toString().matches(text);
                return found;
            }
        };
    }

    public static Matcher<RecyclerView.ViewHolder> withMessageHeaderHolder(final MessageType messageType) {
        return new BoundedMatcher<RecyclerView.ViewHolder, MessageHeaderParent.MessageHeaderHolder>(MessageHeaderParent.MessageHeaderHolder.class) {
            private boolean found = false;

            @Override
            public void describeTo(Description description) {
                description.appendText("No ViewHolder found with text: " + messageType.toString());
            }

            @Override
            protected boolean matchesSafely(MessageHeaderParent.MessageHeaderHolder item) {
                if (found) return false;
                MessageHeaderParent messageHeaderParent = item.onItemClickListener.getMessageHeaderParentAtPosition(item.getLayoutPosition());
                found = (messageType == messageHeaderParent.getMessageType());
                return found;
            }
        };
    }
}