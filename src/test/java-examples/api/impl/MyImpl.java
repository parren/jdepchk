package api.impl;

public class MyImpl {

	public void foo() {
		// Illegal reference
		Class clazz = javax.net.SocketFactory.class;
	}

}
