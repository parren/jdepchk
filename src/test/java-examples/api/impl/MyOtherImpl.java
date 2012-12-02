package api.impl;

public class MyOtherImpl {

	public void foo() {
		// Reference granted by exceptional rule.
		Class clazz = javax.net.SocketFactory.class;
	}

}
