/**
 * The {@link ch.parren.jdepchk.annotations.Uses} annotation corresponds to
 * the "uses:" clause in JDepChk's configuration files.
 * 
 * @see ch.parren.jdepchk.annotations.Extends
 * @see ch.parren.jdepchk.annotations.AppliesTo
 * @see ch.parren.jdepchk.annotations.Allows
 */
@Uses({ "com.example.foo.core", "javax.swing" })// references other components
@Checked package com.example.foo.ui;

import ch.parren.jdepchk.annotations.Checked;
import ch.parren.jdepchk.annotations.Uses;

