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
