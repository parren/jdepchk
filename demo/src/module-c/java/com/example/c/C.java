package com.example.c;

import com.example.a.A;
import com.example.b.B;

public class C extends B {

	A a;

	public static void badUseOfA() {
		A.onlyInB();
	}

}
