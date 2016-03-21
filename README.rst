=================
Zulip for Android
=================

This is a Gradle project, and can be built via the provided `gradlew` or by
using Android Studio.

License
-------

Copyright 2012-2016 Dropbox, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Build instructions (Android Studio)
-----------------------------------

Open the project in the IDE:

- select "Open an existing Android Studio project" option
if you are in "Welcome to Android Studio" menu;

- select "File > Open..." if you already have an opened
project.

In both cases you will have to provide the path to the cloned
project (to the "zulip-android/" directory).

When the project is opened in Android Studio you will have to
add some data needed by google services:

1) A configuration file which you can generate here:

https://developers.google.com/mobile/add?platform=android

(Type in "Zulip" as "App name" and "com.zulip.android"
 as "Android package name".)
 Put the generated file in the "app/" directory of the project.

2) Google app id. You will also get it from the above given link.
This id should be written as the following string resource in
app/src/main/res/values/strings.xml:

<string name="google_app_id">GOOGLE_APP_ID</string>

Now you are ready for running the app on a device or
on an emulator.

If you have a device running Android go to the settings
and  enable USB debugging in developer options. Then plug
your device in the computer and select "Run > Run...".
You will be shown "Device chooser" window. Select your
device in the given list and press "OK".

If you do not have an Android device you will have to run
it on an emulator. Here are instructions for creating an
Android virtual device (AVD):

http://developer.android.com/tools/devices/managing-avds.html#createavd