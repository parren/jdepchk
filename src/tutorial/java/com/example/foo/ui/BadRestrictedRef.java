package com.example.foo.ui;

import com.example.foo.core.CoreClass;

public class BadRestrictedRef {

	public void badAccess() {
		new CoreClass().foo(null, null);
	}

}
