package com.example.members;

import java.io.UnsupportedEncodingException;

public class UsesBytes {

	public byte[] bad() {
		return "Test".getBytes();
	}

	public byte[] good() {
		try {
			return "Test".getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

}
