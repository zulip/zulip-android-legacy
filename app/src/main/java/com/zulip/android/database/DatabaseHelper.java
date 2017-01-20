package com.zulip.android.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.zulip.android.R;
import com.zulip.android.ZulipApp;
import com.zulip.android.models.Emoji;
import com.zulip.android.models.Message;
import com.zulip.android.models.MessageRange;
import com.zulip.android.models.Person;
import com.zulip.android.models.Stream;
import com.zulip.android.util.MD5Util;

import java.sql.SQLException;

/**
 * Database helper class used to manage the creation and upgrading of your
 * database. This class also usually provides the DAOs used by the other
 * classes.
 */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper {

    // name of the database file for your application -- change to something
    // appropriate for your app
    private static final String DATABASE_NAME = "zulip-%s.db";
    // any time you make changes to your database objects, you may have to
    // increase the database version
    private static final int DATABASE_VERSION = 7;

    public DatabaseHelper(ZulipApp app, String email) {
        super(app, String.format(DATABASE_NAME, MD5Util.md5Hex(email)), null,
                DATABASE_VERSION, R.raw.ormlite_config);
    }

    /**
     * Escape LIKE wildcards with a backslash. Must also use ESCAPE clause
     *
     * @param likeClause string to escape
     * @return Escaped string
     */
    public static String likeEscape(String likeClause) {
        return likeClause.replace("%", "\\%").replace("_", "\\_");
    }

    /**
     * This is called when the database is first created. Usually you should
     * call createTable statements here to create the tables that will store
     * your data.
     */
    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
        try {
            Log.i(DatabaseHelper.class.getName(), "onCreate");
            // Create the databases we're using
            TableUtils.createTable(connectionSource, Person.class);
            TableUtils.createTable(connectionSource, Stream.class);
            TableUtils.createTable(connectionSource, Message.class);
            TableUtils.createTable(connectionSource, MessageRange.class);
            TableUtils.createTable(connectionSource, Emoji.class);
        } catch (SQLException e) {
            Log.e(DatabaseHelper.class.getName(), "Can't create database", e);
            throw new RuntimeException(e);
        }
    }

    public void resetDatabase(SQLiteDatabase db) {
        ZulipApp.get().onResetDatabase();
        Log.i("resetDatabase", "Resetting database");

        boolean inTransaction = db.inTransaction();

        if (!inTransaction) {
            db.beginTransaction();
        }

        Cursor c = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table'"
                        + "AND name NOT LIKE 'sqlite_%'"
                        + "AND name NOT LIKE 'android_%';", null);
        while (c.moveToNext()) {
            String name = c.getString(0);
            Log.i("resetDatabase", "Dropping " + name);
            db.execSQL("DROP TABLE " + name);
        }

        c.close();

        db.setTransactionSuccessful();
        db.endTransaction();
        db.execSQL("VACUUM;");

        if (inTransaction) {
            db.beginTransaction();
        }

        onCreate(db);
    }

    /**
     * This is called when your application is upgraded and it has a higher
     * version number. This allows you to adjust the various data to match the
     * new version number.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource,
                          int oldVersion, int newVersion) {
        resetDatabase(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        resetDatabase(db);
    }
}
