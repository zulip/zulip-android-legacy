package com.humbughq.mobile;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import android.util.Log;

import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.misc.BaseDaoEnabled;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Ranges of messages we have received.
 * 
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
}
