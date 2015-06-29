# Command Line Options #

JDepChk can be run from the command line as follows:

```
java -jar build/jdepchk.jar [OPTION]...
```

Use the `--help` option to list the [available options](http://code.google.com/p/jdepchk/source/browse/src/main/java/ch/parren/jdepchk/help.txt).

# Ant Support #

You can run JDepChk from within Ant build scripts via Ant's java task. This is encapsulated in an Ant macro provided by [build-macro-jdepchk.xml](http://code.google.com/p/jdepchk/source/browse/build-macro-jdepchk.xml). An example can be seen [in JDepChk's own build file](http://code.google.com/p/jdepchk/source/browse/build.xml#125).

# Rules #

In addition to the rules shown in the options help, you can also look at [JDepChk's own rules](http://code.google.com/p/jdepchk/source/browse/src/main/rules.jdep) and the two tutorial tests:

  * [TutorialTest](http://code.google.com/p/jdepchk/source/browse/src/tutorial/java/ch/parren/jdepchk/tutorial/TutorialTest.java)
  * [AnnotationsTutorialTest](http://code.google.com/p/jdepchk/source/browse/src/tutorial/java/ch/parren/jdepchk/tutorial/AnnotationsTutorialTest.java)

# Demo #

There is a [full-blown demo project](http://code.google.com/p/jdepchk/source/browse/demo/) included with the source.