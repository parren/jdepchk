package com.example.a;

import ch.parren.jdepchk.annotations.RestrictedTo;

public class A {

	@RestrictedTo("com.example.b.**")//
	public static void onlyInB() {}

}
