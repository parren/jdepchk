package api.impl.two;

import api.impl.one.OneA;
import api.impl.one.OneB;

public class TwoA extends OneA {

	public void foo() {
		OneB b = new OneB();
		if (b.hashCode() == 1)
			;
	}

}
