package com.humbughq.android;

import org.apache.commons.lang.builder.HashCodeBuilder;

public class Person {

    private String name;
    private String email;

    public Person(String name, String email) {
        this.setName(name);
        this.setEmail(email);
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
        this.email = email;
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

    public boolean equals(Person obj) {
        return (this.name.equals(obj.getName()) && this.email.equals(obj
                .getEmail()));
    }

    public int hashCode() {
        // arbitrary numbers
        return new HashCodeBuilder(741, 737).append(name).append(email)
                .toHashCode();
    }
}
