/*
Based on code from the ASM project [http://asm.ow2.org/],
licensed under the following terms:
 
Copyright (c) 2000-2005 INRIA, France Telecom
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

3. Neither the name of the copyright holders nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
THE POSSIBILITY OF SUCH DAMAGE. 
 */
package ch.parren.jdepchk.classes;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.SortedMap;

import ch.parren.java.lang.New;

/**
 * JVM class file format parser based on ASM's ClassReader class, but tuned for
 * speed with JDepChk's requirements.
 */
public final class RefsOnlyClassParser implements Closeable {

	private static final int UTF8 = 1;
	private static final int INT = 3;
	private static final int FLOAT = 4;
	private static final int LONG = 5;
	private static final int DOUBLE = 6;
	private static final int CLASS = 7;
	// private static final int STR = 8;
	private static final int FIELD = 9;
	private static final int METH = 10;
	private static final int IMETH = 11;
	private static final int NAME_TYPE = 12;

	private static final int ACC_PUBLIC = 0x0001;
	private static final int ACC_PRIVATE = 0x0002;
	private static final int ACC_PROTECTED = 0x0004;
	private static final int ACC_MASK = ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED;

	public static long nFilesRead = 0;
	public static long nBytesAvail = 0;
	public static long nBytesRead = 0;
	public static long nBytesUsed = 0;

	private InputStream stream;
	private byte[] bytes;
	private int accessed = 0;
	private int read = 0;

	// caches
	private int[] items;
	private String[] strings;
	private char[] strBuf;

	// information
	private String ownName;
	private Visibility visibility;
	private SortedMap<String, Visibility> refdElements = New.treeMap(); // want sorting here

	public RefsOnlyClassParser(int size, InputStream stream) throws IOException {
		this.bytes = new byte[size];
		this.stream = stream;
		parse();
	}

	public RefsOnlyClassParser(byte[] bytes) throws IOException {
		this.bytes = bytes;
		this.read = bytes.length;
		parse();
	}

	public SortedMap<String, Visibility> referencedElementNames() {
		return Collections.unmodifiableSortedMap(refdElements);
	}

	public Visibility visibility() {
		return visibility;
	}

	private void parse() throws IOException {

		// Parse the constant pool.
		final int n = readUnsignedShort(8);
		items = new int[n];
		strings = new String[n];
		final int[] nameTypes = new int[n], classes = new int[n], memberRefs = new int[n];
		int nNameTypes = 0, nClasses = 0, nMemberRefs = 0;
		int maxStringLength = 0;
		int at = 10;
		readTo(n * 3); // minimum pool size
		for (int i = 1; i < n; ++i) {
			final int itemAt = at + 1;
			items[i] = itemAt;
			int size;
			readTo(at);
			switch (bytes[at]) {
			case FIELD:
			case METH:
			case IMETH:
				memberRefs[nMemberRefs++] = itemAt;
				size = 5;
				break;
			case INT:
			case FLOAT:
				size = 5;
				break;
			case NAME_TYPE:
				nameTypes[nNameTypes++] = itemAt;
				size = 5;
				break;
			case LONG:
			case DOUBLE:
				size = 9;
				++i;
				break;
			case UTF8:
				size = 3 + readUnsignedShort(itemAt);
				if (size > maxStringLength)
					maxStringLength = size;
				break;
			case CLASS:
				classes[nClasses++] = itemAt;
				size = 3;
				break;
			// case STR:
			default:
				size = 3;
				break;
			}
			at += size;
		}
		strBuf = new char[maxStringLength];

		// Parse access flags and class name
		visibility = flagsToVis(readUnsignedShort(at));
		ownName = readUTF8Item(readUnsignedShort(items[readUnsignedShort(at + 2)]));

		// Parse extends and implements
		addClassItemRef(readUnsignedShort(at + 4), Visibility.PUBL);
		final int nIntf = readUnsignedShort(at + 6);
		at += 8;
		for (int i = 0; i < nIntf; i++) {
			addClassItemRef(readUnsignedShort(at), Visibility.PUBL);
			at += 2;
		}

		// Parse members (fields, then methods).
		for (int k = 0; k < 2; k++) {
			final int nMembers = readUnsignedShort(at);
			at += 2;
			for (int i = 0; i < nMembers; i++) {
				final Visibility vis = flagsToVis(readUnsignedShort(at) & ACC_MASK);
				addDescriptorRef(readUnsignedShort(at + 4), vis);
				at = parseAttrs(at + 6, vis);
			}
		}

		// Parse class attributes.
		at = parseAttrs(at, Visibility.PUBL);

		// Add remaining, internal refs.
		for (int i = 0; i < nMemberRefs; i++) {
			final String className = readClassItemRef(readUnsignedShort(memberRefs[i]));
			final int nameTypeAt = items[readUnsignedShort(memberRefs[i] + 2)];
			addMemberRef(className, //
					readUTF8Item(readUnsignedShort(nameTypeAt)), // member name
					readUTF8Item(readUnsignedShort(nameTypeAt + 2)) // member descriptor
			);
		}
		for (int i = 0; i < nClasses; i++)
			addClassNameRef(readUnsignedShort(classes[i]), Visibility.PRIV);
		for (int i = 0; i < nNameTypes; i++)
			addDescriptorRef(readUnsignedShort(nameTypes[i] + 2), Visibility.PRIV);

	}

	private int parseAttrs(int at, Visibility vis) throws IOException {
		final int nAttrs = readUnsignedShort(at);
		at += 2;
		for (int i = 0; i < nAttrs; i++) {
			final String name = readUTF8Item(readUnsignedShort(at));
			final int attrLen = readInt(at + 2);
			at += 6;
			final int atEnd = at + attrLen;
			if ("Exceptions".equals(name)) {
				final int nExc = readUnsignedShort(at);
				at += 2;
				for (int k = 0; k < nExc; k++) {
					addClassItemRef(readUnsignedShort(at), vis);
					at += 2;
				}
			} else if ("Signature".equals(name)) {
				addSignatureRef(readUnsignedShort(at), vis);
			} else if ("RuntimeVisibleAnnotations".equals(name)) {
				parseAnns(at, vis);
			} else if ("RuntimeInvisibleAnnotations".equals(name)) {
				parseAnns(at, Visibility.PRIV);
			}
			at = atEnd;
		}
		return at;
	}

	@SuppressWarnings("unused")//
	private void parseAnns(int at, Visibility vis) throws IOException {
		// TODO annotations
	}

	private Visibility flagsToVis(int flags) {
		switch (flags & ACC_MASK) {
		case ACC_PRIVATE:
			return Visibility.PRIV;
		case 0:
			return Visibility.DEF;
		case ACC_PROTECTED:
			return Visibility.PROT;
		default:
			return Visibility.PUBL;
		}
	}

	private String addClassRef(String name, Visibility vis) {
		if (null == name || ownName.equals(name))
			return null;
		final Visibility found = refdElements.get(name);
		if (null == found || vis.compareTo(found) > 0)
			refdElements.put(name, vis);
		return name;
	}

	private String addClassNameRef(int nameItem, Visibility vis) throws IOException {
		return addClassRef(toClassName(readUTF8Item(nameItem)), vis);
	}

	private String addClassItemRef(int classItem, Visibility vis) throws IOException {
		return addClassNameRef(readUnsignedShort(items[classItem]), vis);
	}

	protected String readClassItemRef(int classItem) throws IOException {
		final int nameItem = readUnsignedShort(items[classItem]);
		return toClassName(readUTF8Item(nameItem));
	}

	private void addDescriptorRef(int descriptorStringIndex, Visibility vis) throws IOException {
		if (0 == descriptorStringIndex)
			return;
		addDescriptor(readUTF8Item(descriptorStringIndex), vis);
	}

	/**
	 * Extracts type references from type descriptors. We can simply scan for
	 * 'L', which starts a type ref.
	 */
	private void addDescriptor(String desc, Visibility vis) {
		int i = 0, n = desc.length();
		while (i < n) {
			if (desc.charAt(i++) == 'L') {
				int i0 = i;
				while (desc.charAt(i++) != ';') {}
				addClassRef(desc.substring(i0, i - 1), vis);
			}
		}
	}

	private void addSignatureRef(int signatureStringIndex, Visibility vis) throws IOException {
		if (0 == signatureStringIndex)
			return;
		addSignature(readUTF8Item(signatureStringIndex), vis);
	}

	/**
	 * Extracts type references from generic type signatures. We need to fairly
	 * properly parse these.
	 */
	private void addSignature(String sig, Visibility vis) {
		new SigParser(sig, vis).parse();
	}

	private final class SigParser {

		private final String sig;
		private final Visibility vis;
		private int at;

		public SigParser(String sig, Visibility vis) {
			this.sig = sig;
			this.vis = vis;
			this.at = 0;
		}

		public void parse() {
			char c = next();
			if (c == '<') {
				mtdGenParams();
				c = next();
			}
			if (c == '(') {
				mtdParams();
				c = next();
			}
			type(c);
		}

		private void mtdGenParams() {
			char c = next();
			while (c != '>') {
				while (next() != ':') {} // skip param name
				if ((c = next()) != ':')
					type(c); // super class
				else
					at--;
				while ((c = next()) == ':')
					type(next()); // super interface
			}
		}

		private void mtdParams() {
			char c;
			while ((c = next()) != ')')
				type(c);
		}

		private void type(char c) {
			switch (c) {
			case 'L':
				typeSig();
				break;
			case 'T':
				while (next() != ';') {}
				break;
			case '[':
				type(next());
				break;
			}
			// all other chars are assumed to be single letter primitives
		}

		private void typeSig() {
			final StringBuilder name = new StringBuilder();
			char c;
			do {
				final int start = at;
				while ((c = next()) != ';' && c != '<' && c != '.') {}
				name.append(sig, start, at - 1);
				if (c == '.')
					name.append('$');
				else if (c == '<')
					while ((c = next()) != '>')
						type(c);
			} while (c != ';');
			addClassRef(name.toString(), vis);
		}

		private char next() {
			return sig.charAt(at++);
		}

	}

	private void addMemberRef(String className, String memberName, String memberDescriptor) {
		if (null == className)
			return;
		refdElements.put(className + "#" + memberName + "#" + memberDescriptor, Visibility.PRIV);
	}

	private String toClassName(String name) {
		if (name.charAt(0) == '[')
			if (name.charAt(1) == 'L')
				return name.substring(2, name.length() - 1);
			else
				return null;
		return name;
	}

	private int readUnsignedShort(final int index) throws IOException {
		readTo(index + 1);
		byte[] b = this.bytes;
		return ((b[index] & 0xFF) << 8) | (b[index + 1] & 0xFF);
	}

	private int readInt(final int index) throws IOException {
		readTo(index + 3);
		byte[] b = this.bytes;
		return ((b[index] & 0xFF) << 24) | ((b[index + 1] & 0xFF) << 16) | ((b[index + 2] & 0xFF) << 8)
				| (b[index + 3] & 0xFF);
	}

	private String readUTF8Item(int item) throws IOException {
		String s = strings[item];
		if (s != null)
			return s;
		int index = items[item];
		return strings[item] = readUTF8At(index + 2, readUnsignedShort(index), this.strBuf);
	}

	private String readUTF8At(int index, final int utfLen, final char[] buf) throws IOException {
		int endIndex = index + utfLen;
		readTo(endIndex);
		byte[] b = this.bytes;
		int strLen = 0;
		int c, d, e;
		while (index < endIndex) {
			c = b[index++] & 0xFF;
			switch (c >> 4) {
			case 0:
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
			case 6:
			case 7:
				// 0xxxxxxx
				buf[strLen++] = (char) c;
				break;
			case 12:
			case 13:
				// 110x xxxx 10xx xxxx
				d = b[index++];
				buf[strLen++] = (char) (((c & 0x1F) << 6) | (d & 0x3F));
				break;
			default:
				// 1110 xxxx 10xx xxxx 10xx xxxx
				d = b[index++];
				e = b[index++];
				buf[strLen++] = (char) (((c & 0x0F) << 12) | ((d & 0x3F) << 6) | (e & 0x3F));
				break;
			}
		}
		return new String(buf, 0, strLen);
	}

	private static final int CHUNK_SIZE = 64 * 1024;

	private void readTo(int index) throws IOException {
		if (index > accessed)
			accessed = index;
		while (index >= read) {
			final int want = index + 1 - read;
			final int chunk = (want + CHUNK_SIZE - 1) / CHUNK_SIZE * CHUNK_SIZE;
			final int remain = bytes.length - read;
			if (remain <= 0)
				throw new IllegalArgumentException();
			final int len = remain < chunk ? remain : chunk;
			final int redNow = stream.read(this.bytes, this.read, len);
			if (redNow < 0)
				throw new IOException();
			read += redNow;
		}
	}

	/* @Override */public void close() throws IOException {
		nFilesRead++;
		nBytesAvail += bytes.length;
		nBytesRead += read;
		nBytesUsed += accessed + 1;
	}

}
