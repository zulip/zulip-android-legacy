package com.zulip.android.database;

import com.j256.ormlite.android.apptools.OrmLiteConfigUtil;

import java.io.File;

/**
 * Run this utility on your workstation whenever changing fields in our Android
 * models.
 * <p/>
 * Ref: §4.2 of http://ormlite.com/javadoc/ormlite-core/doc-files/ormlite_4.html
 */
public class DatabaseConfigUtil extends OrmLiteConfigUtil {
    public static void main(String[] args) throws Exception {
        (new File("./res/raw")).mkdirs();

        writeConfigFile("ormlite_config.txt");
    }
}