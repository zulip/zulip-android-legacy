

=================
Zulip for Android
=================

This is the Android client for `Zulip <http://www.zulip.org>`_.  It is
available `in the Google Play store
<https://play.google.com/store/apps/details?id=com.zulip.android>`_.
If you have a taste for adventure, you can also `join our beta channel
<https://play.google.com/apps/testing/com.zulip.android>`_ after which
the Play store should auto-update you. (Caution: beta releases will
have more bugs than final releases, including (potentially) security
or data loss bugs.)

This is a Gradle project, and can be built via the provided ``gradlew``
or by using Android Studio.

**Index**
---------------


-  `Getting Started`_
-  `Getting Help`_
-  `Build instructions (Android Studio)`_)
-  `Build instructions (without Android Studio)`_)
-  `Running server on device`_
-  `Coding Style`_
-  `Mailing List`_
-  `Export`_
-  `License`_

.. _Getting Started: #getting-started
.. _Getting Help: #getting-help
.. _Build instructions (Android Studio): #build-instructions-android-studio
.. _Build instructions (without Android Studio): #build-instructions-without-android-studio
.. _Running server on device: #browsing-server-on-device
.. _Coding Style: #coding-style
.. _Mailing List: #mailing-list
.. _Export: #export
.. _License: #license

Getting Started
---------------

- First, clone this repository and follow the Android Studio build
  instructions (below) to compile the project.  If you don't have an
  existing Zulip account to login with, you can create one on the
  Zulip development community server at https://zulip.tabbott.net/.
  Note that this server is used for development discussions, so don't
  use it for sending lots of test messages!

- For testing, it can be helpful to have your own Zulip development
  server that the app can connect to.  Instructions for setting up a
  Zulip development server are here:
  http://zulip.readthedocs.io/en/latest/dev-overview.html

- And then follow the `Connecting to a development server`_ guide to get the
  Android app to connect to the Zulip development server.

.. _Connecting to a development server: #connecting-to-a-development-server

Getting Help
---------------

- For real-time help, you can join the Zulip development community's
  Zulip server at https://zulip.tabbott.net and post on the `android`
  stream.

- For questions on development for the Zulip Android App you can post here:
https://groups.google.com/forum/#!forum/zulip-android

- For questions on development for the Zulip Server you can post here:
https://groups.google.com/forum/#!forum/zulip-devel

Any issues or bugs should be posted in the Github Issue Tracker!


Build instructions (Android Studio)
-----------------------------------

0. Fork the zulip-android repository from GitHub and clone your fork.

1. Open the project in the IDE.
    a) From the "Welcome to Android Studio" menu, select "Open an
       existing Android Studio project" option, or
    b) If you already have an opened project, select "File > Open..."

2. If you want to test Google sign in, add the required metadata:
    1. Go to https://developers.google.com/mobile/add?platform=android
    2. Type in "Zulip" as "App name" and "com.zulip.android" as
       "Android package name" and continue to Choose and configure services.
    3. Select the "Google Sign-In" service, you'll need to provide the
       SHA-1 of your signing certificate. For that, first follow the manual instructions
       to Sign Your Release Build and generate a new Key Store
       if you haven't already:
       https://developer.android.com/studio/publish/app-signing.html#release-mode
    4. Use keytool to get the SHA-1 fingerprint of the certificate:
       ``keytool -exportcert -list -v \    -alias <your-key-name> -keystore <path-to-production-keystore>``
    5. Put the generated file in the "app/" directory of the project.
    6. Get your Google app id from the Google Developer console, and
       add it as the following string resource in
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

Connecting to a development server
----------------------------------

Once you are running a Zulip development server on your laptop, you
will need to do some additional configuration of the Android
development environment to connect to your development server.

| If you are using a Genymotion Emulator you can access the server by
  browsing to http://10.0.3.2:9991 or http://10.0.3.1:9991 (one of these
  two URL’s)

To access the vagrant server on a physical device:

* connect the computer and Android device to the same network (router)

* modify ``VagrantFile`` `here`_ in the server change the host\_ip
  ‘127.0.0.1’ to ‘0.0.0.0’ Like this-

    config.vm.network “forwarded\_port”, guest: 9991, host: host\_port,
    host\_ip: “0.0.0.0”

* Now find the IP address of the computer, and start the Android app.
  For the server, specify the computer's IP address and port number as
  the Zulip Server. For example:

    192.168.0.1:9991

  Note that if you have internet access on your android device through
  a proxy server, you might have to use a VPN application like `this
  one`_ to connect to the server.

| You can also route the IP address to a domain name like
  www.local.test.com (this routing is useful when testing the Google OAuth
  Backend)
| No need to modify the ``VagrantFile`` to achieve this

-  Remap the hosts by fiddler by adding this line in TOOLS> HOSTS

    localhost:9991 www.local.test.com

If unclear you can follow tutorial here `Host Remapping`_

-  Now configure your android device following `this`_ detailed tutorial

Another way to connect android device to local server is via adb

-  Run server on laptop(or Desktop)

-  By default, it run's on port 9991

-  Connect your Android device with laptop and enable usb debugging

-  Run ``adb reverse tcp:9991 tcp:9991`` in terminal

-  Connect to local server by entering url (**http://127.0.0.0.1:9991**) in app

.. _here: https://github.com/zulip/zulip/blob/1c40df9363b70af0e275c44a03f9627808852616/Vagrantfile#L37
.. _Host Remapping: http://docs.telerik.com/fiddler/KnowledgeBase/HOSTS
.. _this one: https://play.google.com/store/apps/details?id=com.psiphon3&hl=en
.. _this: http://docs.telerik.com/fiddler/Configure-Fiddler/Tasks/ConfigureForAndroid


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

