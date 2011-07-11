package com.example.b;

import com.example.a.A;

public class B extends A {

	public static void friendUseOfA() {
		A.onlyInB();
	}
	
}
