package com.example.xml;

import java.io.InputStream;

public interface ParseXML {

	public org.w3c.dom.Node parse(InputStream xmlStream, javax.xml.validation.Schema schema);

}
