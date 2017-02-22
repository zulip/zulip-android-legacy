package com.zulip.android.models;

import com.google.gson.annotations.SerializedName;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.table.DatabaseTable;
import com.zulip.android.ZulipApp;
import com.zulip.android.util.ZLog;

import org.apache.commons.lang.builder.HashCodeBuilder;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@DatabaseTable(tableName = "people")
public class Person {
    public static final String NAME_FIELD = "name";
    public static final String MESSAGESPARTICIPATEDIN_FIELD = "messagesParticipatedIn";
    public static final String EMAIL_FIELD = "email";
    public static final String ISBOT_FIELD = "isBot";
    public static final String ISACTIVE_FIELD = "isActive";
    private static final String ID_FIELD = "id";
    private static final String AVATARURL_FIELD = "avatarUrl";
    @SerializedName("user_id")
    @DatabaseField(columnName = ID_FIELD, id = true)
    protected int id;

    @SerializedName("full_name")
    @DatabaseField(columnName = NAME_FIELD)
    private String name;

    @SerializedName("email")
    @DatabaseField(columnName = EMAIL_FIELD, uniqueIndex = true)
    private String email;

    @DatabaseField(columnName = AVATARURL_FIELD)
    private String avatarURL;

    @SerializedName("is_bot")
    @DatabaseField(columnName = ISBOT_FIELD)
    private boolean isBot;

    @DatabaseField(columnName = ISACTIVE_FIELD)
    private boolean isActive;

    @SerializedName("is_admin")
    private boolean isAdmin;

    @SerializedName("domain")
    private String domain;

    @SerializedName("short_name")
    private String shortName;

    @SerializedName("is_mirror_dummy")
    private boolean isMirrorDummy;

    /**
     * used to get person id in case of {@link Message.ZulipDirectMessage}
     */
    @SerializedName("id")
    private int recipientId;

    public Person(String name, String email) {
        this.setName(name);
        this.setEmail(email);
    }

    public Person(String name, String email, String avatarURL) {
        this(name, email);
        this.setAvatarURL(avatarURL);
        this.isActive = false;
    }

    public Person(int id, String name, String email, String avatarURL, boolean isBot, boolean isActive) {
        this.setId(id);
        this.setName(name);
        this.setEmail(email);
        this.setAvatarURL(avatarURL);
        this.setActive(isActive);
        this.setBot(isBot);
    }

    /**
     * Construct an empty Person object.
     */
    public Person() {

    }

    public static Person getByEmail(ZulipApp app, String email) {
        try {
            return getByEmail(app.getDatabaseHelper().getDao(
                    Person.class), email);
        } catch (SQLException e) {
            ZLog.logException(e);
        }
        return null;
    }

    @SuppressWarnings("WeakerAccess")
    public static Person getByEmail(Dao<Person, ?> dao, String email) {
        try {
            return dao.queryBuilder().where()
                    .eq(Person.EMAIL_FIELD, new SelectArg(email))
                    .queryForFirst();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    @SuppressWarnings("WeakerAccess")
    /**
     * Returns person object {@link Person} from the database if exists or newly created object using
     * the passed parameters.
     * Note: always call this method with valid Person id {@link Person#id} passed as argument.
     *
     * @param app Zulip god object {@link ZulipApp}
     * @param email person email {@link Person#email}
     * @param name person name {@link Person#name}
     * @param avatarURL person avatar url {@link Person#avatarURL}
     * @param personId person id {@link Person#id}
     * @param personCache person cache used ofr faster fetching of results
     * @return {@link Person} object
     */
    public static Person getOrUpdate(ZulipApp app, String email, String name,
                                     String avatarURL, int personId, Map<String, Person> personCache) {

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

            // use the person id passed to generate person skeleton object which is consistent
            // with server id.
            person.setId(personId);
            app.getDao(Person.class).createOrUpdate(person);
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

    /**
     * This function is called from setEmail {@link ZulipApp#setEmail(String)}.
     * It is used to get current user person {@link Person} object if it exists in the database
     * or create a new object using {@param email}.
     *
     * @param app {@link ZulipApp}
     * @param email Person email id {@link Person#email}
     * @return {@link Person} object
     */
    public static Person get(ZulipApp app, String email) {
        Person foundPerson = getByEmail(app, email);
        if (foundPerson == null) {
            // fake it till you make it
            return new Person(null, email);
        }
        return foundPerson;
    }

    /**
     * This function is used to call {@link Person#getOrUpdate(ZulipApp, String, String, String, int, Map)}
     * with no cache.
     * Note: always call this method with valid Person id {@link Person#id} passed as argument.
     *
     * @param app Zulip god object {@link ZulipApp}
     * @param email person email {@link Person#email}
     * @param name person name {@link Person#name}
     * @param avatarURL person avatar url {@link Person#avatarURL}
     * @param personId person id {@link Person#id}
     * @return {@link Person} object
     * */
    public static Person getOrUpdate(ZulipApp app, String email, String name,
                                     String avatarURL, int personId) {
        return getOrUpdate(app, email, name, avatarURL, personId, null);
    }

    public static Person getById(ZulipApp app, int id) {
        RuntimeExceptionDao<Person, Object> dao = app.getDao(Person.class);
        return dao.queryForId(id);
    }

    public static List<Person> getAllPeople(ZulipApp app) throws SQLException {
        RuntimeExceptionDao<Person, Object> dao = app.getDao(Person.class);
        return dao.queryBuilder().where().eq(Person.ISBOT_FIELD, false).query();
    }

    public static void sortByPresence(ZulipApp app, List<Person> people) {
        final Map<String, Presence> presenceCopy = new HashMap<>(
                app.presences);

        Collections.sort(people, new Comparator<Person>() {
            @Override
            public int compare(Person a, Person b) {
                Presence aPresence = presenceCopy.get(a.getEmail());
                Presence bPresence = presenceCopy.get(b.getEmail());

                final int inactiveTimeout = 2 * 60;

                if (aPresence == null && bPresence == null) {
                    return a.getName().toLowerCase(Locale.US)
                            .compareTo(b.getName().toLowerCase(Locale.US));
                } else if (aPresence == null) {
                    return 1;
                } else if (bPresence == null) {
                    return -1;
                } else if (aPresence.getAge() > inactiveTimeout
                        && bPresence.getAge() > inactiveTimeout) {
                    return a.getName().toLowerCase(Locale.US)
                            .compareTo(b.getName().toLowerCase(Locale.US));
                } else if (aPresence.getAge() > inactiveTimeout) {
                    return 1;
                } else if (bPresence.getAge() > inactiveTimeout) {
                    return -1;
                } else if (aPresence.getStatus() == bPresence.getStatus()) {
                    return a.getName().toLowerCase(Locale.US)
                            .compareTo(b.getName().toLowerCase(Locale.US));
                } else if (aPresence.getStatus() == PresenceType.ACTIVE) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    private void setEmail(String email) {
        if (email != null) {
            this.email = email.toLowerCase(Locale.US);
        }
    }

    public boolean getIsBot() {
        return isBot;
    }

    public String getAvatarURL() {
        return avatarURL;
    }

    private void setAvatarURL(String avatarURL) {
        this.avatarURL = avatarURL;
    }

    /**
     * Calculate the Humbug realm for the person, currently by splitting the
     * email address.
     * <p/>
     * In the future, realms may be distinct from your email hostname.
     *
     * @return the Person's realm.
     */
    public String getRealm() {
        String[] splitEmail = this.getEmail().split("@");
        return splitEmail[splitEmail.length - 1];
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
        if (this.name == null || this.email == null)
            return false;
        return this.name.equals(per.getName()) && this.email.equals(per
                .getEmail());
    }

    public int hashCode() {
        return new HashCodeBuilder(17, 31).append(name).append(email)
                .toHashCode();
    }

    @SuppressWarnings("WeakerAccess")
    public void setBot(boolean isBot) {
        this.isBot = isBot;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public int getRecipientId() {
        return this.recipientId;
    }

    public void setRecipientId(int id) {
        this.recipientId = id;
    }
}
