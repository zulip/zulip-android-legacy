package com.zulip.android.models;

/**
 * Modal for People Drawer List
 * Order of Person with most recent chat to be highest
 */

public class PeopleDrawerList {
    //Person whose order is given by
    private Person person;
    //Order of the person
    private int order = Integer.MAX_VALUE;
    //Group ID
    private int groupId;
    //Group name
    private String groupName;

    public PeopleDrawerList(int order, Person person, int groupId, String groupName) {
        this.order = order;
        this.person = person;
        this.groupId = groupId;
        this.groupName = groupName;
    }

    public PeopleDrawerList(Person person, String groupName, int groupId) {
        this.person = person;
        this.groupName = groupName;
        this.groupId = groupId;
    }

    public int getOrder() {
        return order;
    }

    public Person getPerson() {
        return person;
    }

    public int getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
