# Zulip Android plan

We are merging the Zulip Android and iOS development communities, to
focus on the React Native app codebase (zulip-mobile.git).  We’ve
found with the iOS React Native project that we’ve been able to build
an app that performs well using the platform, and the benefits of
being able to share a codebase (so that we only have to implement each
feature and design each detail once) are huge, and outweigh the
advantages of continuing to invest in the existing, more mature Java
Android app.

So, in preparation for Google Summer of Code applications opening on
March 20th, we’re planning to merge the communities now so that we can
direct students interested in Android to propose projects for the
React Native app.  Our goal is to have the best possible Zulip Android
app by the end of the summer, and we think the best way to achieve
that is to combine the efforts of both the Zulip iOS/React Native
developers and the Zulip Java/Android development team on a single app
codebase.  This will also as a side effect benefit both the iOS app
and potential future apps for other React-Native supported platforms
like Windows Phone.

Here’s how this will work practically:

- The Java Android app will remain in the app store until the React
  Native app is good enough to completely replace it.  This probably
  means 3-6 months, depending how quickly RN development goes.  We’ll
  continue doing releases both to fix bugs and to roll out features
  that are already partially implemented, but want to avoid putting a
  lot of work into totally new features for the Java app.
- Folks who have open PRs on the Java Android app should work on
  finishing them so that we can close out those features.  It’s still
  super valuable.
- The goal is to get the React Native app to be better than the Java
  Android app as fast as possible (probably by end of August at the
  latest), so that we can minimize total work.
- This Android strategy means it doesn’t make sense for us to have
  GSoC projects working on the Java Android app; those students would
  be able to help Zulip more working on the React Native app.  So
  students who were planning to do GSoC with the Java Android project
  should write their proposals for improvements to the React Native
  app instead.  A few important details are worth highlighting:
- Zulip’s GSoC selection process is focused more on general
  engineering skills and approach than specific language knowledge, so
  good work done on the Java Android app is just as valuable for your
  application as work done on the React Native app.
- Good engineers can learn new tools and languages, so if you’re shown
  your skills well on the Java project, we’re confident you’ll be able
  to effectively contribute to the React Native project after a bit of
  learning time.  Students are encouraged to start learning React
  Native and contributing to the React Native app now to help prepare
  for a successful summer.
- Proposals should highlight any work done on either app as well as
  describe a plan for the improvements they want to make to the React
  Native app over the summer.


Logistical details checklist for migration:

* [x] Make sure all existing Java contributors understand the plan and
  can give feedback on it and this plan.
* [x] Announce on chat.zulip.org
* [ ] Update GSoC ideas page to clarify the plan
* [ ] Update zulip/zulip-mobile README.md
* [x] Update zulip/zulip-android README.md
* [ ] Update zulip/zulip README.md
* [ ] Email zulip-devel@ and zulip-ios@ and zulip-android@ with the
  announcement
* [ ] Announce on Twitter @zuliposs, linking to email
* [ ] Use @zulipbot to update open PRs and issues for Android app with
  a heads-up about the plan

Logistical details that can be follow-up items but are important to
making the transition successful:

* [ ] Get the Zulip RN app working on Android (main issue is
implementing the scrolling extension)

* [ ] Attempt to clear out Java Android open PRs and open bugs to buy
  us 3-6 months of time without major development to the Java Android
  app, since it may take a while for the React Native app to achieve
  feature/quality parity.

* [ ] Audit features of Java Android app and open issues for them in
  the RN app project.
* [ ] Audit open issues for feature ideas in the Java Android app and
  open corresponding ones in the RN app project.
* [ ] Add a bunch of links to JS, React, and RN tutorials to the RN app docs
* [ ] Create good onboarding docs for RN for developers who only have Linux (and no Mac).
* [ ] Move/rename/merge, as appropriate, the Zulip mobile mailing lists

