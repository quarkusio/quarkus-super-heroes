# Contributing guide

**Want to contribute? Great!** 
We try to make it easy, and all contributions, even the smaller ones, are more than welcome.
This includes bug reports, fixes, documentation, examples... 
But first, read this page (including the small print at the end).

* [Legal](#legal)
* [Reporting an issue](#reporting-an-issue)
* [Before you contribute](#before-you-contribute)
  + [Discuss non-trivial contribution ideas with committers](#discuss-non-trivial-contribution-ideas-with-committers)
  + [Create your branch from main](#create-your-branch-from-main)
  + [Keep commits focused](#keep-commits-focused)
  + [Code reviews](#code-reviews)
  + [Coding Guidelines](#coding-guidelines)
  + [Continuous Integration](#continuous-integration)
  + [Tests and documentation are not optional](#tests-and-documentation-are-not-optional)
* [Setup](#setup)
  + [IDE Config and Code Style](#ide-config-and-code-style)
* [Build](#build)
* [The small print](#the-small-print)

## Legal

All original contributions to Quarkus are licensed under the
[ASL - Apache License](https://www.apache.org/licenses/LICENSE-2.0),
version 2.0 or later, or, if another license is specified as governing the file or directory being
modified, such other license.

All contributions are subject to the [Developer Certificate of Origin (DCO)](https://developercertificate.org/).
The DCO text is also included verbatim in the [dco.txt](dco.txt) file in the root directory of the repository.

## Reporting an issue

This project uses GitHub issues to manage the issues. Open an issue directly in GitHub.

If you believe you found a bug, please indicate a way to reproduce it, what you are seeing and what you would expect to see.
Don't forget to indicate your Quarkus, Java, Maven/Gradle and GraalVM version. 

## Before you contribute

To contribute, use GitHub Pull Requests, from your **own** fork.

Also, make sure you have set up your Git authorship correctly:

```
git config --global user.name "Your Full Name"
git config --global user.email your.email@example.com
```

If you use different computers to contribute, please make sure the name is the same on all your computers.

We may use this information to acknowledge your contributions in release announcements.

### Discuss non-trivial contribution ideas with committers
If you're considering anything more than correcting a typo or fixing a minor bug, please discuss it by [creating an issue on our issue tracker](https://github.com/quarkusio/quarkus-super-heroes/issues?q=is%3Aissue) before submitting a pull request. We're happy to provide guidance but please spend an hour or two researching the subject on your own including searching the forums for prior discussions.

### Create your branch from main
Create your topic branch to be submitted as a pull request from `main`.

### Keep commits focused
Remember each ticket should be focused on a single item of interest since the tickets are used to produce the changelog. Since each commit should be tied to a single GitHub issue, ensure that your commits are focused. For example, do not include an update to a transitive library in your commit unless the GitHub is to update the library. Reviewing your commits is essential before sending a pull request.

### Code reviews

All submissions, including submissions by project members, need to be reviewed by at least one committer before being merged.

[GitHub Pull Request Review Process](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/reviewing-changes-in-pull-requests/about-pull-request-reviews) is followed for every pull request.

### Coding Guidelines

 * Commits should be atomic and semantic. Please properly squash your pull requests before submitting them. Fixup commits can be used temporarily during the review process but things should be squashed at the end to have meaningful commits.
 We use merge commits so the GitHub Merge button cannot do that for us. If you don't know how to do that, just ask in your pull request, we will be happy to help!

### Continuous Integration

Because we are all humans, all changes must go through continuous integration. CI is based on GitHub Actions, which means that everyone has the ability to automatically execute CI in their forks as part of the process of making changes. We ask that all non-trivial changes go through this process, so that the contributor gets immediate feedback, while at the same time keeping our CI fast and healthy for everyone.

The process requires only one additional step to enable Actions on your fork (clicking the green button in the actions tab). [See the full video walkthrough](https://youtu.be/egqbx-Q-Cbg) for more details on how to do this.

To keep the caching of non-Quarkus artifacts efficient (speeding up CI), you should occasionally sync the `main` branch of your fork with `main` of this repo (e.g. monthly).

### Tests and documentation are not optional

Don't forget to include tests in your pull requests. If any tests fail (regardless if they are tests you wrote/modified or not) then it is your responsibility to figure out what went wrong before submitting. We don't keep code that doesn't pass tests in the codebase. When you first cloned the repository all tests would have passed. If a test you didn't touch now doesn't pass, then something you did in the changes you made has broken an existing test.

Also don't forget the documentation (reference documentation, javadoc...).

Be sure to test your pull request in:

1. Java mode
2. Native mode

## Setup

If you have not done so on this machine, you need to:
 
* Install Git and configure your GitHub access
* Install Java SDK 11+ (OpenJDK recommended)
* Install [GraalVM](https://quarkus.io/guides/building-native-image)
* Install platform C developer tools:
    * Linux
        * Make sure headers are available on your system (you'll hit 'Basic header file missing (<zlib.h>)' error if they aren't).
            * On Fedora `sudo dnf install zlib-devel`
            * Otherwise `sudo apt-get install libz-dev`
    * macOS
        * `xcode-select --install` 
* Set `GRAALVM_HOME` to your GraalVM Home directory e.g. `/opt/graalvm` on Linux or `$location/JDK/GraalVM/Contents/Home` on macOS

Docker (or podman) is necessary: it is used by Quarkus Dev Services
* Check [the installation guide](https://docs.docker.com/install/), and [the MacOS installation guide](https://docs.docker.com/docker-for-mac/install/)
* If you just install docker, be sure that your current user can run a container (no root required). 
On Linux, check [the post-installation guide](https://docs.docker.com/install/linux/linux-postinstall/)

### IDE Config and Code Style

The project has a `.editorconfig` file checked into the root. Please make sure the rules in there are adhered to by your IDE.

## Build

Each application (`event-statistics`, `rest-fights`, `rest-heroes`, `rest-villains`, and `ui-super-heroes`) is a self-contained application. Details on how to run each are included in the `README.me` in each project's sub-directory.

## The small print

This project is an open source project, please act responsibly, be nice, polite and enjoy!
