package com.zulip.android;

import android.database.Cursor;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.json.JSONException;
import org.json.JSONObject;

import com.j256.ormlite.android.AndroidDatabaseResults;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "people")
public class Person {
    public static final String ID_FIELD = "id";
    public static final String NAME_FIELD = "name";
    public static final String MESSAGESPARTICIPATEDIN_FIELD = "messagesParticipatedIn";
    public static final String EMAIL_FIELD = "email";
    public static final String AVATARURL_FIELD = "avatarUrl";
    public static final String ISBOT_FIELD = "isBot";
    public static final String ISACTIVE_FIELD = "isActive";

    @DatabaseField(columnName = ID_FIELD, generatedId = true)
    protected int id;
    @DatabaseField(columnName = NAME_FIELD)
    private String name;
    @DatabaseField(columnName = EMAIL_FIELD, uniqueIndex = true)
    private String email;
    @DatabaseField(columnName = AVATARURL_FIELD)
    private String avatarURL;
    @DatabaseField(columnName = ISBOT_FIELD)
    private boolean isBot;
    @DatabaseField(columnName = ISACTIVE_FIELD)
    boolean isActive;

    public Person(String name, String email) {
        this.setName(name);
        this.setEmail(email);
    }

    public Person(String name, String email, String avatarURL) {
        this(name, email);
        this.setAvatarURL(avatarURL);
        this.isActive = false;
    }

    /**
     * Construct an empty Person object.
     */
    public Person() {

    }

    public String getName() {
        return name;
    }

    private void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public boolean getIsBot() {
        return isBot;
    }

    private void setEmail(String email) {
        if (email != null) {
            this.email = email.toLowerCase();
        }
    }

    public String getAvatarURL() {
        return avatarURL;
    }

    public void setAvatarURL(String avatarURL) {
        this.avatarURL = avatarURL;
    }

    /**
     * Calculate the Humbug realm for the person, currently by splitting the
     * email address.
     * 
     * In the future, realms may be distinct from your email hostname.
     * 
     * @return the Person's realm.
     */
    public String getRealm() {
        String[] split_email = this.getEmail().split("@");
        return split_email[split_email.length - 1];
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Person)) {
            return false;
        }
        Person per = (Person) obj;
        return (this.name.equals(per.getName()) && this.email.equals(per
                .getEmail()));
    }

    public int hashCode() {
        return new HashCodeBuilder(17, 31).append(name).append(email)
                .toHashCode();
    }

    static Person getByEmail(ZulipApp app, String email) {
        try {
            Dao<Person, Integer> dao = app.getDatabaseHelper().getDao(
                    Person.class);
            return dao.queryBuilder().where()
                    .eq(Person.EMAIL_FIELD, new SelectArg(email.toLowerCase()))
                    .queryForFirst();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Person getOrUpdate(ZulipApp app, String email, String name,
            String avatarURL, HashMap<String, Person> personCache) {

        Person person = null;

        if (personCache != null) {
            person = personCache.get(email);
        }

        if (person == null) {
            person = getByEmail(app, email);
            if (personCache != null) {
                personCache.put(email, person);
            }
        }

        if (person == null) {
            person = new Person(name, email, avatarURL);
            app.getDao(Person.class).create(person);
        } else {
            boolean changed = false;
            if (name != null && !name.equals(person.name)) {
                person.name = name;
                changed = true;
            }
            if (avatarURL != null && !avatarURL.equals(person.avatarURL)) {
                person.avatarURL = avatarURL;
                changed = true;
            }
            if (changed) {
                app.getDao(Person.class).update(person);
            }
        }
        return person;
    }

    public static Person getOrUpdate(ZulipApp app, String email, String name,
            String avatarURL) {
        return getOrUpdate(app, email, name, avatarURL, null);
    }

    void updateFromJSON(JSONObject jPerson) throws JSONException {
        name = jPerson.getString("full_name");
        isBot = jPerson.getBoolean("is_bot");
        // It would be nice if the server gave us avatarURL here, but it doesn't
    }

    static Person getFromJSON(ZulipApp app, JSONObject jPerson)
            throws JSONException {
        String email = jPerson.getString("email");
        Person person = getByEmail(app, email);
        if (person == null) {
            person = new Person(null, email);
        }
        person.updateFromJSON(jPerson);
        return person;
    }

    public static Person getById(ZulipApp app, int id) {
        RuntimeExceptionDao<Person, Object> dao = app.getDao(Person.class);
        return dao.queryForId(id);
    }

    public static void sortByPresence(ZulipApp app, List<Person> people) {
        final Map<String, Presence> presenceCopy = new HashMap<String, Presence>(
                app.presences);

        Collections.sort(people, new Comparator<Person>() {
            @Override
            public int compare(Person a, Person b) {
                Presence aPresence = presenceCopy.get(a.getEmail());
                Presence bPresence = presenceCopy.get(b.getEmail());

                final int inactiveTimeout = 2 * 60;

                if (aPresence == null && bPresence == null) {
                    return a.getName().toLowerCase()
                            .compareTo(b.getName().toLowerCase());
                } else if (aPresence == null) {
                    return 1;
                } else if (bPresence == null) {
                    return -1;
                } else if (aPresence.getAge() > inactiveTimeout
                        && bPresence.getAge() > inactiveTimeout) {
                    return a.getName().toLowerCase()
                            .compareTo(b.getName().toLowerCase());
                } else if (aPresence.getAge() > inactiveTimeout) {
                    return 1;
                } else if (bPresence.getAge() > inactiveTimeout) {
                    return -1;
                } else if (aPresence.getStatus() == bPresence.getStatus()) {
                    return a.getName().toLowerCase()
                            .compareTo(b.getName().toLowerCase());
                } else if (aPresence.getStatus() == PresenceType.ACTIVE) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });
    }
}
