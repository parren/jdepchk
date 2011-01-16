package user;

import api.MyAPI;
import api.MyAnn;
import api.impl.MyImpl;

@MyAnn
public class MyUser {

	public static void main(String[] args) {
		// legal access to the API
		MyAPI api = new MyAPI();
		// illegal access to a detail
		MyImpl impl = new MyImpl();
	}
	
}
