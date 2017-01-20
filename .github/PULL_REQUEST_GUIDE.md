**How to formate code?**

Mac: `Option + Command + L` Windows: `Ctrl + Alt + L Linux:` Go to `File->Settings->(under IDE settings)->Keymap` and change shortcut as `Ctrl + Alt + L` locks linux.

**Lint issues**

- Run the lint tests with `./gradlew lintDebug`.

- It ensure that code has no structural problems. Poorly structured code can impact the reliability and efficiency of Android apps and make code harder to maintain. For example, if XML resource files contain unused namespaces, this takes up space and incurs unnecessary processing. Other structural issues, such as use of deprecated elements or API calls that are not supported by the target API versions, might lead to code failing to run correctly.

- If BUILD fails check report at `zulip-android/app/build/outputs/lint-results-debug.html` or `zulip-android/app/build/outputs/lint-results-debug.xml`

**Night Mode**

zulip-android contains a theme for night, so if new UI is designed or UI is altered make sure that it is compatible with Night theme too.

**Commit message**

- Usally starts with capital letter.

- Include `Fix:#{issue-number}` in the commit message so that issue is automatically closed on merging Pull Request.

- Get more at details at https://zulip.readthedocs.io/en/latest/version-control.html#commit-messages
