package com.humbughq.mobile;

import java.sql.SQLException;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "people")
public class Person {

    public static final String ID_FIELD = "id";
    public static final String NAME_FIELD = "name";
    public static final String MESSAGESPARTICIPATEDIN_FIELD = "messagesParticipatedIn";
    public static final String EMAIL_FIELD = "email";
    public static final String AVATARURL_FIELD = "avatarUrl";

    @DatabaseField(columnName = ID_FIELD, generatedId = true)
    private int id;
    @DatabaseField(columnName = NAME_FIELD)
    private String name;
    @ForeignCollectionField(columnName = MESSAGESPARTICIPATEDIN_FIELD)
    private ForeignCollection<MessagePerson> messagesParticipatedIn;
    @DatabaseField(columnName = EMAIL_FIELD, uniqueIndex = true)
    private String email;
    @DatabaseField(columnName = AVATARURL_FIELD)
    private String avatarURL;

    public Person(String name, String email) {
        this.setName(name);
        this.setEmail(email);
    }

    public Person(String name, String email, String avatarURL) {
        this(name, email);
        this.setAvatarURL(avatarURL);
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
            return dao.queryBuilder().where().eq(Person.EMAIL_FIELD, email)
                    .queryForFirst();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    static Person getOrUpdate(ZulipApp app, String email, String name,
            String avatarURL) {
        Person person = getByEmail(app, email);
        if (person == null) {
            person = new Person(name, email, avatarURL);
            try {
                app.getDao(Person.class).create(person);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return person;
    }

    public static Person getById(ZulipApp app, int id) {
        try {
            Dao<Person, Integer> dao = app.getDatabaseHelper().getDao(
                    Person.class);
            return dao.queryForId(id);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }
}
