package ch.parren.jdepchk.classes;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import ch.parren.java.lang.New;

// FIXME drop lazy reading as we nearly always access everything

/**
 * JVM class file format parser based on ASM's ClassReader class, but tuned for
 * speed with JDepChk's requirements.
 */
public final class ClassParser implements Closeable {

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
	private boolean shouldClose = false;
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
	private Map<String, Visibility> refdClasses = New.hashMap();

	public ClassParser(int size, InputStream stream) throws IOException {
		this(size, stream, false);
	}

	public ClassParser(File file) throws IOException {
		this((int) file.length(), new FileInputStream(file), true);
	}

	public Map<String, Visibility> referencedClasses() {
		return Collections.unmodifiableMap(refdClasses);
	}

	public Visibility visibility() {
		return visibility;
	}

	private ClassParser(int streamSize, InputStream stream, boolean shouldClose) throws IOException {
		this.bytes = new byte[streamSize];
		this.stream = stream;
		this.shouldClose = shouldClose;

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
			final String className = addClassItemRef(readUnsignedShort(memberRefs[i]), Visibility.PRIV);
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
		final Visibility found = refdClasses.get(name);
		if (null == found || vis.compareTo(found) > 0)
			refdClasses.put(name, vis);
		return name;
	}

	private String addClassNameRef(int nameItem, Visibility vis) throws IOException {
		return addClassRef(toClassName(readUTF8Item(nameItem)), vis);
	}

	private String addClassItemRef(int classItem, Visibility vis) throws IOException {
		return addClassNameRef(readUnsignedShort(items[classItem]), vis);
	}

	private void addDescriptorRef(int descriptorStringIndex, Visibility vis) throws IOException {
		if (0 == descriptorStringIndex)
			return;
		final String d = readUTF8Item(descriptorStringIndex);
		int i = 0, n = d.length();
		while (i < n) {
			if (d.charAt(i++) == 'L') {
				int i0 = i;
				char c;
				while ((c = d.charAt(i++)) != ';' && c != '<') {} // the '<' is for parsing generics
				addClassRef(d.substring(i0, i - 1), vis);
			}
		}
	}

	private void addSignatureRef(int signatureStringIndex, Visibility vis) throws IOException {
		// A signature, when we only want to extract class refs, parses just like a descriptor.
		addDescriptorRef(signatureStringIndex, vis);
	}

	private void addMemberRef(String className, String memberName, String memberDescriptor) {
		if (null == className)
			return;
		// TODO member
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

	@Override public void close() throws IOException {
		if (null == stream)
			return;
		if (shouldClose)
			stream.close();
		stream = null;
		nFilesRead++;
		nBytesAvail += bytes.length;
		nBytesRead += read;
		nBytesUsed += accessed + 1;
	}

}
