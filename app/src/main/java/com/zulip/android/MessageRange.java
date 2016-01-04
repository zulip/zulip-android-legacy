package com.zulip.android;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import android.util.Log;

import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.misc.BaseDaoEnabled;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Ranges of messages we have received.
 */
@DatabaseTable(tableName = "ranges")
public class MessageRange extends BaseDaoEnabled<MessageRange, Integer> {
    @DatabaseField(generatedId = true)
    private int id;
    @DatabaseField()
    public int low;
    @DatabaseField()
    public int high;

    public MessageRange() {

    }

    public MessageRange(int low, int high) {
        this.low = low;
        this.high = high;
    }

    public static MessageRange getRangeContaining(int value,
            RuntimeExceptionDao<MessageRange, Integer> messageRangeDao) {
        List<MessageRange> ranges;
        try {
            ranges = messageRangeDao.queryBuilder().where().le("low", value)
                    .and().ge("high", value).query();
            if (ranges.size() == 1) {
                return ranges.get(0);
            } else if (ranges.size() != 0) {
                Log.wtf("rangecheck",
                        "Expected one range, got " + ranges.size()
                                + " when looking for ID " + value);
            }
        } catch (SQLException e) {
            // This is nonfatal.
            ZLog.logException(e);
        }

        return null;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31).append(low).append(high)
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        MessageRange rhs = (MessageRange) obj;
        return new EqualsBuilder().append(this.low, rhs.low)
                .append(this.high, rhs.high).isEquals();
    }

    // / Update or create the final range for new messages from server events
    public static void updateNewMessagesRange(ZulipApp app, int maxId) {
        synchronized (app.updateRangeLock) {
            RuntimeExceptionDao<MessageRange, Integer> rangeDao = app
                    .getDao(MessageRange.class);

            MessageRange currentRange = MessageRange.getRangeContaining(
                    app.getMaxMessageId(), rangeDao);
            if (currentRange == null) {
                currentRange = new MessageRange(app.getMaxMessageId(),
                        app.getMaxMessageId());
            }

            if (currentRange.high <= maxId) {
                currentRange.high = maxId;
                rangeDao.createOrUpdate(currentRange);
            }
        }

        app.setMaxMessageId(maxId);
    }

    // Create a range for fetched messages, merging with other ranges if
    // necessary. Messages between low and high (both inclusive) must exist in
    // the DB.
    public static void markRange(ZulipApp app, final int low, final int high) {
        final RuntimeExceptionDao<MessageRange, Integer> messageRangeDao = app
                .getDao(MessageRange.class);
        try {
            synchronized (app.updateRangeLock) {
                TransactionManager.callInTransaction(app.getDatabaseHelper()
                        .getConnectionSource(), new Callable<Void>() {
                    public Void call() throws Exception {
                        Where<MessageRange, Integer> where = messageRangeDao
                                .queryBuilder().orderBy("low", true).where();
                        @SuppressWarnings("unchecked")
                        List<MessageRange> ranges = where.or(
                                where.and(where.ge("high", low - 1),
                                        where.le("high", high + 1)),
                                where.and(where.ge("low", low - 1),
                                        where.le("low", high + 1))).query();

                        MessageRange rng = new MessageRange(low, high);
                        if (ranges.size() > 0) {
                            Log.i("", "our low: " + rng.low + ", our high: "
                                    + rng.high);
                            int db_low = ranges.get(0).low;
                            int db_high = ranges.get(ranges.size() - 1).high;
                            Log.i("", "their low: " + db_low + ", their high: "
                                    + db_high);
                            if (db_low < rng.low) {
                                rng.low = db_low;
                            }
                            if (db_high > rng.high) {
                                rng.high = db_high;
                            }
                            messageRangeDao.delete(ranges);
                        }
                        messageRangeDao.createOrUpdate(rng);
                        return null;
                    }
                });
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
