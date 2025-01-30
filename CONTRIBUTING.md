# How to Contribute

We'd love to accept your patches and contributions to this project. There are
just a few small guidelines you need to follow.

## Contributor License Agreement

Contributions to this project must be accompanied by a Contributor License
Agreement. You (or your employer) retain the copyright to your contribution;
this simply gives us permission to use and redistribute your contributions as
part of the project. Head over to <https://cla.developers.google.com/> to see
your current agreements on file or to sign a new one.

You generally only need to submit a CLA once, so if you've already submitted one
(even if it was for a different project), you probably don't need to do it
again.

## Code reviews

All submissions, including submissions by project members, require review. We
use GitHub pull requests for this purpose. Consult
[GitHub Help](https://help.github.com/articles/about-pull-requests/) for more
information on using pull requests.

## Best Practices

There are several coding practices that are applied to all adapters, for code readability, code consistency, and bug avoidance purposes.
Please keep these in mind when making adapter modifications:

1. All adapters should be formatted using [Google Style](https://github.com/google/styleguide/blob/gh-pages/intellij-java-google-style.xml).
   Download the `intellij-java-google-style.xml` file and import it to Android Studio by going to **Settings > Editor > Code Style** and select **Scheme > Import Scheme**.
   Once this is imported, you can reformat your code by going to **Code > Show Reformat File Dialog** and click **Run**.
   Make sure that **Rearrange code** is not selected to avoid large diffs.

## Troubleshooting

### Android Studio errors

When importing an adapter project into Android Studio, you may run into some errors.

Here are some potential errors and ways to resolve those errors:

1. Build error: “Cannot find a Java installation on your machine matching this tasks requirements: {languageVersion=11, vendor=any, implementation=vendor-specific}”
   - Solution is to set Gradle JDK to be Java 11 (at Android Studio Settings -> Build, Execution, Deployment -> Build Tools -> Gradle, select Gradle JDK).
   - But, when using Java 11 as Gradle JDK, Gradle build may fail due to a missing library error (like "/tmp/libconscrypt_openjdk_jni-linux-x86_6417345489542840000.so: libstdc++.so.6: cannot open shared object file: No such file or directory").
   - In that case, the workaround is to remove the line kotlin.jvmToolChain(11) from the module’s build.gradle file while doing development. Note: This line removal shouldn't be included as part of the pull request.

2. Error when running the unit tests: "Failed to transform core-for-system-modules.jar to match attributes {artifactType=_internal_android_jdk_image, org.gradle.libraryelements=jar, org.gradle.usage=java-runtime}"
   - Solution: Set Gradle JDK to be <= Java 17 (at Android Studio Settings -> Build, Execution, Deployment -> Build Tools -> Gradle, select Gradle JDK).

# If you can't become a contributor

If you can't become a contributor, but wish to share some code that illustrates
an issue / shows how an issue may be fixed, then you can attach your changes on
the issue tracker. We will use this code to troubleshoot the issue and fix it,
but will not use this code in the library unless the steps to submit patches
are done.
