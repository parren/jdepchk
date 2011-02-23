package com.example.usesxml;

import com.example.xml.ParseXML;

public class UseXML {

	static {
		ParseXML p = null;
		javax.xml.validation.Schema schema = null;
		org.w3c.dom.Node root = p.parse(null, schema);
	}
	
}
