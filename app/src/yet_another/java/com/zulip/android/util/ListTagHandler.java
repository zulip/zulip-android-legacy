package com.zulip.android.util;

import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.text.style.LeadingMarginSpan;
import android.util.Log;

import org.xml.sax.XMLReader;

import java.util.Stack;

/**
 * Custom TagHandler {@link Html.TagHandler} to support ordered and unordered list in
 * TextView {@link android.widget.TextView}.
 */

public class ListTagHandler implements Html.TagHandler {
    private static final String LOG_TAG = ListTagHandler.class.getSimpleName();

    private static final String OL_TAG = "ol";
    private static final String UL_TAG = "ul";
    private static final String LI_TAG = "li";

    /**
     * List indentation in pixels.
     */
    private static final int INDENT_PX = 10;
    private static final int LIST_ITEM_INDENT_PX = INDENT_PX * 2;
    private static final BulletSpan BULLET_SPAN = new BulletSpan(INDENT_PX);

    /**
     * Keeps track of lists (ol, ul). On bottom of Stack is the outermost list
     * and on top of Stack is the most nested list.
     */
    private final Stack<ListTag> lists = new Stack<ListTag>();

    @Override
    public void handleTag(final boolean opening, final String tag, final Editable output, final XMLReader xmlReader) {
        if (UL_TAG.equalsIgnoreCase(tag)) {
            if (opening) {
                // handle <ul>
                lists.push(new Ul());
            } else {
                // handle </ul>
                lists.pop();
            }
        } else if (OL_TAG.equalsIgnoreCase(tag)) {
            if (opening) {
                // handle <ol>
                // use default start index of 1
                lists.push(new Ol());
            } else {
                // handle </ol>
                lists.pop();
            }
        } else if (LI_TAG.equalsIgnoreCase(tag)) {
            if (opening) {
                // handle <li>
                lists.peek().openItem(output);
            } else {
                // handle </li>
                lists.peek().closeItem(output, lists.size());
            }
        } else {
            Log.d(LOG_TAG, "Found an unsupported tag " + tag);
        }
    }

    /**
     * Abstract super class for {@link Ul} and {@link Ol}.
     */
    private abstract static class ListTag {
        /**
         * Opens a new list item.
         *
         * @param text
         */
        public void openItem(final Editable text) {
            if (text.length() > 0 && text.charAt(text.length() - 1) != '\n') {
                text.append("\n");
            }
            final int len = text.length();
            text.setSpan(this, len, len, Spanned.SPAN_MARK_MARK);
        }

        /**
         * Closes a list item.
         *
         * @param text
         * @param indentation
         */
        public final void closeItem(final Editable text, final int indentation) {
            if (text.length() > 0 && text.charAt(text.length() - 1) != '\n') {
                text.append("\n");
            }
            final Object[] replaces = getReplaces(text, indentation);
            final int len = text.length();
            final ListTag listTag = getLast(text);
            final int where = text.getSpanStart(listTag);
            text.removeSpan(listTag);
            if (where != len) {
                for (Object replace : replaces) {
                    text.setSpan(replace, where, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }

        protected abstract Object[] getReplaces(final Editable text, final int indentation);

        /**
         * Note: This knows that the last returned object from getSpans() will be the most recently added.
         */
        private ListTag getLast(final Spanned text) {
            final ListTag[] listTags = text.getSpans(0, text.length(), ListTag.class);
            if (listTags.length == 0) {
                return null;
            }
            return listTags[listTags.length - 1];
        }
    }

    /**
     * Class representing the unordered list <ul></ul> HTML tag.
     */
    private static class Ul extends ListTag {

        @Override
        protected Object[] getReplaces(final Editable text, final int indentation) {
            // Nested BulletSpans increases distance between BULLET_SPAN and text, so we must prevent it.
            int bulletMargin = INDENT_PX;
            if (indentation > 1) {
                bulletMargin = INDENT_PX - BULLET_SPAN.getLeadingMargin(true);
                if (indentation > 2) {
                    // This get's more complicated when we add a LeadingMarginSpan into the same line:
                    // we have also counter it's effect to BulletSpan
                    bulletMargin -= (indentation - 2) * LIST_ITEM_INDENT_PX;
                }
            }
            return new Object[]{
                    new LeadingMarginSpan.Standard(LIST_ITEM_INDENT_PX * (indentation - 1)),
                    new BulletSpan(bulletMargin)
            };
        }
    }

    /**
     * Class representing the ordered list <ol></ol> HTML tag.
     */
    private static class Ol extends ListTag {
        private int nextIdx;

        /**
         * Creates a new <ol></ol> with start index of 1.
         */
        public Ol() {
            this(1); // default start index
        }

        /**
         * Creates a new <ol></ol> with given start index.
         *
         * @param startIdx
         */
        public Ol(final int startIdx) {
            this.nextIdx = startIdx;
        }

        @Override
        public void openItem(final Editable text) {
            super.openItem(text);
            text.append(Integer.toString(nextIdx++)).append(". ");
        }

        @Override
        protected Object[] getReplaces(final Editable text, final int indentation) {
            int numberMargin = LIST_ITEM_INDENT_PX * (indentation - 1);
            if (indentation > 2) {
                // Same as in ordered lists: counter the effect of nested Spans
                numberMargin -= (indentation - 2) * LIST_ITEM_INDENT_PX;
            }
            return new Object[]{new LeadingMarginSpan.Standard(numberMargin)};
        }
    }
}
