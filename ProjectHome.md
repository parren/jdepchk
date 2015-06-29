JDepChk is a dependency checker for Java virtual machine (JVM) class files. It can be used for any code that runs on the JVM. Highlights:

  * Specify **components and dependencies** using high-level rules.
  * Optional **transitive dependencies** (if B extends A, users of B inherit A).
  * Regular expression matchers and **include/exclude lists**.
  * Check for accesses to entire classes or individual members.
  * Multiple rule sets loadable from different files.
  * Use **annotations**, on `package-info.java`, and on restricted elements.
  * **Very fast.** Based on a stripped-down version of ASM's class reader.
  * Processes class files one by one (unlike some other such tools).
  * Has an [Eclipse plugin](http://code.google.com/a/eclipselabs.org/p/edepchk/) with smart error highlighting.

RunningIt has all the details. See VersionHistory for what's new in each release.

JDepChk was directly inspired by [Macker](http://innig.net/macker), but is a lot faster and easier to use in typical scenarios (due to component-oriented rules).

News:

  * Version 0.5.0 supports defining exceptions to component rules.