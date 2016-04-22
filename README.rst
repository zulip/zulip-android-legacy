=================
Zulip for Android
=================

This is a Gradle project, and can be built via the provided ``gradlew``
or by using Android Studio.

Build instructions (Android Studio)
-----------------------------------

1. Open the project in the IDE.
    a) From the "Welcome to Android Studio" menu, select "Open an
       existing Android Studio project" option, or
    b) If you already have an opened project, select "File > Open..."

2. If you want to test Google sign in, add the required metadata:
    1. Go to https://developers.google.com/mobile/add?platform=android
    2. Type in "Zulip" as "App name" and "com.zulip.android" as
       "Android package name".)
    3. Put the generated file in the "app/" directory of the project.
    4. Google app id. You will also get it from the above given link.
       This id should be written as the following string resource in
       ``app/src/main/res/values/strings.xml``::
            <string name="google_app_id">GOOGLE_APP_ID</string>

If you have a device running Android go to the settings and enable USB
debugging in developer options. Then plug your device in the computer
and select "Run > Run...".  You will be shown "Device chooser" window.
Select your device in the given list and press "OK".

If you do not have an Android device you will have to run it on an
emulator. Here are instructions for creating an Android virtual device
(AVD):

http://developer.android.com/tools/devices/managing-avds.html#createavd

Build instructions (without Android Studio)
-------------------------------------------

1. Install the Android SDK including at least the API 23 (Android 6.0),
   Build Tools, API Platform, Google APIs, Google Play Services,
   Android Support Library, the Local Maven Repository for Support and
   the Google Repository.

   All of these can be installed, together with their dependencies,
   using the Android SDK manager.

2. Comment out or remove references to Crashlytics. The following are
   known references as at ``5de0b0e``. For future versions,
   ``grep -ir crashlytics .`` is your friend.

   If you do not remove Crashlytics then the app will crash on startup
   unless Crashlytics has been set up correctly as a member of the zulip
   team.

* In ``app/src/main/java/com/zulip/android/ZLog.java``:

  * Line 5: ``import com.crashlytics.android.Crashlytics;``

  * Line 13: ``Crashlytics.logException(e);``

* In ``app/src/main/java/com/zulip/android/ZulipActivity.java``:

  * Line 50: ``import com.crashlytics.android.Crashlytics;``

  * Line 162: ``Crashlytics.start(this);``

3. Run ``./gradlew`` (or ``gradlew.bat`` on Windows). This should
   automatically build the application, downloading anything it
   needs to do so.

   If you get a failed build with
   ``A problem occurred configuring project ':app'.`` then you might
   not have all the required SDK libraries. Make sure that you have
   all the dependencies of the libraries listed above, and that all
   versions match precisely.

   If the appropriate tools cannot be found by gradle, make sure that
   ``ANDROID_HOME`` is properly set (this should point to the root
   directory for the Android SDK i.e. the one which contains the add-ons,
   build-tools, docs and other directories).

4. To build the APK, run ``./gradlew assemble``. Your APKs will be
   placed in ``app/build/outputs/apk``.

   The ``app-debug.apk`` can be installed directly on the device, or
   loaded over USB using ``./gradlew installDebug`` or
   ``adb install /path/to/app/build/outputs/apk/app-debug.apk``.

   Note that ``app-release-unsigned.apk`` will **not** install by
   default because it is unsigned. You will be told the APK cannot be
   parsed.

Coding Style
------------

Please read the Zulip coding style conventions documented at
https://zulip.readthedocs.org/en/latest/code-style.html#version-control
carefully.

Mailing List
------------

There's a mailing list for questions and development discussions
related to the Zulip Android app:
https://groups.google.com/forum/#!forum/zulip-android.

Export
------
This distribution includes cryptographic software. The country in
which you currently reside may have restrictions on the import,
possession, use, and/or re-export to another country, of encryption
software. BEFORE using any encryption software, please check your
country's laws, regulations and policies concerning the import,
possession, or use, and re-export of encryption software, to see if
this is permitted. See http://www.wassenaar.org/ for more information.

The U.S. Government Department of Commerce, Bureau of Industry and
Security (BIS), has classified this software as Export Commodity
Control Number (ECCN) 5D002.C.1, which includes information security
software using or performing cryptographic functions with asymmetric
algorithms. The form and manner of this distribution makes it
eligible for export under the License Exception ENC Technology
Software Unrestricted (TSU) exception (see the BIS Export
Administration Regulations, Section 740.13) for both object code and
source code.

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
