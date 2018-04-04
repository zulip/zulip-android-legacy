package com.zulip.android.util;

import android.content.Context;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

import com.zulip.android.ZulipApp;
import com.zulip.android.filters.NarrowFilterPM;
import com.zulip.android.models.Person;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProfileSpan extends ClickableSpan {
    private String email;
    private int userMentionColor;

    public ProfileSpan(String email, int color) {
        this.email = email;
        userMentionColor = color;
    }

    @Override
    public void onClick(View widget) {
        Context context = widget.getContext().getApplicationContext();
        List<Person> people = new ArrayList<Person>();
        if (email.equals("*")) { //This is for "@all"
            try {
                people = Person.getAllPeople(ZulipApp.get());
            } catch (SQLException e) {
                ZLog.logException(e);
                return;
            }
        } else {
            for (String email : this.email.split(",")) {
                Person person = Person.getByEmail(ZulipApp.get(), email);
                if (person != null) {
                    people.add(person);
                }
            }
            people.add(ZulipApp.get().getYou());
        }
        (((ZulipApp) context).getZulipActivity()).doNarrow(new NarrowFilterPM(people));
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        ds.setColor(userMentionColor);
    }
}