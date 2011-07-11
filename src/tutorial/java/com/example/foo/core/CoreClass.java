package com.example.foo.core;

import ch.parren.jdepchk.annotations.RestrictedTo;

public class CoreClass {

	@RestrictedTo("com.example.foo.ui.UIClass")//
	public int[] bar;

	@RestrictedTo("com.example.foo.ui.UIClass")//
	@SuppressWarnings("unused")//
	public void foo(String[] bar, int[][] more) {}

	@RestrictedTo("com.example.foo.ui.UIClass")//
	public int foo() {
		return 0;
	}

}
