**Auto-formatting code**

The Zulip Android app uses auto-formatted code.  In Android Studio,
you can auto-format code as follows:

* Mac: `Option + Command + L`
* Windows: `Ctrl + Alt + L`
* Linux: Go to `File->Settings->(under IDE settings)->Keymap` and
  change the shortcut as `Ctrl + Alt + L` locks the screen on linux.

**Lint issues**

- Run the lint tests with `./gradlew lintDebug`.

- It ensure that code has no structural problems. Poorly structured
  code can impact the reliability and efficiency of Android apps and
  make code harder to maintain. For example, if XML resource files
  contain unused namespaces, this takes up space and incurs
  unnecessary processing. Other structural issues, such as use of
  deprecated elements or API calls that are not supported by the
  target API versions, might lead to code failing to run correctly.

- If lint fails, check the report at
  `zulip-android/app/build/outputs/lint-results-debug.html` or
  `zulip-android/app/build/outputs/lint-results-debug.xml`

**Night Mode**

zulip-android contains a theme for night, so if new UI is designed or
UI is altered, please test to make sure that it is looks good with the
Night theme too.

**Commit messages**

- Usually starts with capital letter, and ends with a period.

- Include `Fixes: #{issue-number}` as a sentence at the end of the
  commit message, so that issue is automatically closed when the
  commits are merged.

- Read the Zulip project's detailed guide to writing good commits here:
  https://zulip.readthedocs.io/en/latest/version-control.html#commit-messages
