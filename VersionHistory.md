## UPCOMING 0.5.0 ##

## 0.4.0 ##

  * Can extract dependency rules from annotations on classes, members, and package-info.java files.
  * Revamped config file format (in line with command line).

## 0.3.0 ##

  * Support for 'this' in 'allows' rules to reference the 'contains' filter. This is useful for rules where you want to restrict a subset of classes to just itself (see the tutorial test for an example).

## 0.2.0 ##

  * Compiles and runs on JDK 1.5.
  * Warnings when command-line targets don't exist.
  * Can scan single .jar file given via -j, --jar.
  * Dropped most static docs in favour of this site here.
  * Can check references to single members, not just entire classes.
  * Runs scans of multiple class dirs or jar files in parallel.

## 0.1.0 ##

Initial release.