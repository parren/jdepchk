package ch.parren.jdepchk.classes;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

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

	public static long nFilesRead = 0;
	public static long nBytesAvail = 0;
	public static long nBytesRead = 0;
	public static long nBytesUsed = 0;
	private int highMark = 0;

	/**
	 * The class to be parsed. <i>The content of this array must not be
	 * modified.
	 */
	public final byte[] bytes;

	/**
	 * The start index of each constant pool item in {@link #bytes b}, plus one.
	 * The one byte offset skips the constant pool item tag that indicates its
	 * type.
	 */
	private final int[] items;

	/**
	 * The String objects corresponding to the CONSTANT_Utf8 items. This cache
	 * avoids multiple parsing of a given CONSTANT_Utf8 constant pool item,
	 * which GREATLY improves performances (by a factor 2 to 3). This caching
	 * strategy could be extended to all constant pool items, but its benefit
	 * would not be so great for these items (because they are much less
	 * expensive to parse than CONSTANT_Utf8 items).
	 */
	private final String[] strings;

	/**
	 * Maximum length of the strings contained in the constant pool of the
	 * class.
	 */
	private final int maxStringLength;

	/**
	 * Start index of the class header information (access, name...) in
	 * {@link #bytes b}.
	 */
	public final int header;

	/** Indexes to the pool items of type CLASS. */
	private final int[] classItems;
	private final int nClassItems;


	private int red = 0;
	private InputStream stream;
	private boolean shouldClose = false;

	public ClassParser(int size, InputStream stream) throws IOException {
		this(size, stream, false);
	}

	public ClassParser(File file) throws IOException {
		this((int) file.length(), new FileInputStream(file), true);
	}

	private ClassParser(int streamSize, InputStream stream, boolean shouldClose) throws IOException {
		this.bytes = new byte[streamSize];
		this.red = 0;
		this.stream = stream;
		this.shouldClose = shouldClose;
		// parses the constant pool
		int n = readUnsignedShort(8);
		items = new int[n];
		strings = new String[n];
		classItems = new int[n];
		int nCls = 0;
		int max = 0;
		int index = 10;
		for (int i = 1; i < n; ++i) {
			items[i] = index + 1;
			int size;
			readTo(index);
			switch (bytes[index]) {
			case FIELD:
			case METH:
			case IMETH:
			case INT:
			case FLOAT:
			case NAME_TYPE:
				size = 5;
				break;
			case LONG:
			case DOUBLE:
				size = 9;
				++i;
				break;
			case UTF8:
				size = 3 + readUnsignedShort(index + 1);
				if (size > max) {
					max = size;
				}
				break;
			case CLASS:
				classItems[nCls++] = index + 1;
				size = 3;
				break;
			// case STR:
			default:
				size = 3;
				break;
			}
			index += size;
		}
		maxStringLength = max;
		nClassItems = nCls;
		// the class header information starts just after the constant pool
		header = index;
		highMark = header;
	}

	/**
	 * Returns the internal name of the class.
	 * 
	 * @return the internal class name
	 */
	public String getClassName() throws IOException {
		return readClass(header + 2, new char[maxStringLength]);
	}

	/**
	 * Returns the internal of name of the super class.
	 * 
	 * @return the internal name of super class, or <tt>null</tt> for
	 *         {@link Object} class.
	 */
	public String getSuperName() throws IOException {
		int n = items[readUnsignedShort(header + 4)];
		return n == 0 ? null : readUTF8(n, new char[maxStringLength]);
	}

	/**
	 * Returns the internal names of the class's interfaces.
	 * 
	 * @return the array of internal names for all implemented interfaces or
	 *         <tt>null</tt>.
	 */
	public String[] getInterfaces() throws IOException {
		int index = header + 6;
		int n = readUnsignedShort(index);
		String[] interfaces = new String[n];
		if (n > 0) {
			char[] buf = new char[maxStringLength];
			for (int i = 0; i < n; ++i) {
				index += 2;
				interfaces[i] = readClass(index, buf);
			}
		}
		return interfaces;
	}

	/**
	 * Returns the names of all the classes referenced by the constant pool. May
	 * contain nulls.
	 */
	public String[] getRefdClasses() throws IOException {
		final String[] result = new String[nClassItems];
		final char[] buf = new char[maxStringLength];
		int n = 0;
		for (int i = 0; i < nClassItems; i++) {
			final String name = toClassName(readUTF8(classItems[i], buf));
			if (null != name)
				result[n++] = name;
		}
		return result;
	}

	private String toClassName(String name) {
		if (name.charAt(0) == '[')
			if (name.charAt(1) == 'L')
				return name.substring(2, name.length() - 1);
			else
				return null;
		return name;
	}

	/**
	 * Reads a byte value in {@link #bytes b}.
	 * 
	 * @param index the start index of the value to be read in {@link #bytes b}.
	 * @return the read value.
	 */
	public int readByte(final int index) throws IOException {
		readTo(index);
		return bytes[index] & 0xFF;
	}

	/**
	 * Reads an unsigned short value in {@link #bytes b}.
	 * 
	 * @param index the start index of the value to be read in {@link #bytes b}.
	 * @return the read value. .
	 */
	public int readUnsignedShort(final int index) throws IOException {
		readTo(index + 1);
		byte[] b = this.bytes;
		return ((b[index] & 0xFF) << 8) | (b[index + 1] & 0xFF);
	}

	/**
	 * Reads a signed short value in {@link #bytes b}.
	 * 
	 * @param index the start index of the value to be read in {@link #bytes b}.
	 * @return the read value.
	 */
	public short readShort(final int index) throws IOException {
		readTo(index + 1);
		byte[] b = this.bytes;
		return (short) (((b[index] & 0xFF) << 8) | (b[index + 1] & 0xFF));
	}

	/**
	 * Reads a signed int value in {@link #bytes b}.
	 * 
	 * @param index the start index of the value to be read in {@link #bytes b}.
	 * @return the read value.
	 */
	public int readInt(final int index) throws IOException {
		readTo(index + 3);
		byte[] b = this.bytes;
		return ((b[index] & 0xFF) << 24) | ((b[index + 1] & 0xFF) << 16) | ((b[index + 2] & 0xFF) << 8)
				| (b[index + 3] & 0xFF);
	}

	/**
	 * Reads a signed long value in {@link #bytes b}.
	 * 
	 * @param index the start index of the value to be read in {@link #bytes b}.
	 * @return the read value.
	 */
	public long readLong(final int index) throws IOException {
		readTo(index + 7);
		long l1 = readInt(index);
		long l0 = readInt(index + 4) & 0xFFFFFFFFL;
		return (l1 << 32) | l0;
	}

	/**
	 * Reads an UTF8 string constant pool item in {@link #bytes b}.
	 * 
	 * @param index the start index of an unsigned short value in {@link #bytes
	 *            b}, whose value is the index of an UTF8 constant pool item.
	 * @param buf buffer to be used to read the item. This buffer must be
	 *            sufficiently large. It is not automatically resized.
	 * @return the String corresponding to the specified UTF8 item.
	 */
	public String readUTF8(int index, final char[] buf) throws IOException {
		int item = readUnsignedShort(index);
		String s = strings[item];
		if (s != null) {
			return s;
		}
		index = items[item];
		return strings[item] = readUTF8(index + 2, readUnsignedShort(index), buf);
	}

	/**
	 * Reads UTF8 string in {@link #bytes b}.
	 * 
	 * @param index start offset of the UTF8 string to be read.
	 * @param utfLen length of the UTF8 string to be read.
	 * @param buf buffer to be used to read the string. This buffer must be
	 *            sufficiently large. It is not automatically resized.
	 * @return the String corresponding to the specified UTF8 string.
	 */
	private String readUTF8(int index, final int utfLen, final char[] buf) throws IOException {
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

	/**
	 * Reads a class constant pool item in {@link #bytes b}.
	 * 
	 * @param index the start index of an unsigned short value in {@link #bytes
	 *            b}, whose value is the index of a class constant pool item.
	 * @param buf buffer to be used to read the item. This buffer must be
	 *            sufficiently large. It is not automatically resized.
	 * @return the String corresponding to the specified class item.
	 */
	public String readClass(final int index, final char[] buf) throws IOException {
		// computes the start index of the CONSTANT_Class item in b
		// and reads the CONSTANT_Utf8 item designated by
		// the first two bytes of this CONSTANT_Class item
		return readUTF8(items[readUnsignedShort(index)], buf);
	}

	private static final int CHUNK_SIZE = 8 * 1024;

	private void readTo(int index) throws IOException {
		if (index > highMark)
			highMark = index;
		while (index >= red) {
			final int want = index + 1 - red;
			final int chunk = (want + CHUNK_SIZE - 1) / CHUNK_SIZE * CHUNK_SIZE;
			final int remain = bytes.length - red;
			final int len = remain < chunk ? remain : chunk;
			final int redNow = stream.read(this.bytes, this.red, len);
			if (redNow < 0)
				throw new IOException();
			red += redNow;
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
		nBytesRead += red;
		nBytesUsed += highMark + 1;
	}

}