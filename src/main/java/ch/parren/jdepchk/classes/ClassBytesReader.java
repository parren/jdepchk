package ch.parren.jdepchk.classes;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public final class ClassBytesReader {

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

	public static long nBytesRead = 0;
	public static long nBytesUsed = 0;
	private int highMark = 0;

	/**
	 * The class to be parsed. <i>The content of this array must not be
	 * modified.
	 */
	public final byte[] b;

	/**
	 * The start index of each constant pool item in {@link #b b}, plus one. The
	 * one byte offset skips the constant pool item tag that indicates its type.
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
	 * {@link #b b}.
	 */
	public final int header;

	/** Indexes to the pool items of type CLASS. */
	private final int[] classItems;
	private final int nClassItems;


	// FIXME Maybe read the bytes lazily
	public ClassBytesReader(InputStream stream) throws IOException {
		this(readClassBytes(stream));
	}

	private static byte[] readClassBytes(InputStream stream) throws IOException {
		byte[] bytes = new byte[32 * 1024];
		int len = bytes.length;
		int offs = 0;
		int red;
		while ((red = stream.read(bytes, offs, len)) > 0) {
			if (red < len)
				break;
			bytes = Arrays.copyOf(bytes, bytes.length * 2);
			offs += red;
			len = bytes.length - offs;
		}
		nBytesRead += offs + red;
		return bytes;
	}

	public ClassBytesReader(byte[] b) {
		this(b, 0, b.length);
	}

	/**
	 * Constructs a new {@link ClassBytesReader} object.
	 * 
	 * @param b the bytecode of the class to be read.
	 * @param off the start offset of the class data.
	 * @param len the length of the class data.
	 */
	public ClassBytesReader(byte[] b, int off, @SuppressWarnings("unused") int len) {
		this.b = b;
		// parses the constant pool
		int n = readUnsignedShort(off + 8);
		items = new int[n];
		strings = new String[n];
		classItems = new int[n];
		int nCls = 0;
		int max = 0;
		int index = off + 10;
		for (int i = 1; i < n; ++i) {
			items[i] = index + 1;
			int size;
			switch (b[index]) {
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
	public String getClassName() {
		return readClass(header + 2, new char[maxStringLength]);
	}

	/**
	 * Returns the internal of name of the super class.
	 * 
	 * @return the internal name of super class, or <tt>null</tt> for
	 *         {@link Object} class.
	 */
	public String getSuperName() {
		int n = items[readUnsignedShort(header + 4)];
		return n == 0 ? null : readUTF8(n, new char[maxStringLength]);
	}

	/**
	 * Returns the internal names of the class's interfaces.
	 * 
	 * @return the array of internal names for all implemented interfaces or
	 *         <tt>null</tt>.
	 */
	public String[] getInterfaces() {
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
	public String[] getRefdClasses() {
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

	private void readTo(int index) {
		if (index > highMark)
			highMark = index;
	}

	/**
	 * Reads a byte value in {@link #b b}.
	 * 
	 * @param index the start index of the value to be read in {@link #b b}.
	 * @return the read value.
	 */
	public int readByte(final int index) {
		readTo(index);
		return b[index] & 0xFF;
	}

	/**
	 * Reads an unsigned short value in {@link #b b}.
	 * 
	 * @param index the start index of the value to be read in {@link #b b}.
	 * @return the read value.
	 */
	public int readUnsignedShort(final int index) {
		readTo(index + 1);
		byte[] b = this.b;
		return ((b[index] & 0xFF) << 8) | (b[index + 1] & 0xFF);
	}

	/**
	 * Reads a signed short value in {@link #b b}.
	 * 
	 * @param index the start index of the value to be read in {@link #b b}.
	 * @return the read value.
	 */
	public short readShort(final int index) {
		readTo(index + 1);
		byte[] b = this.b;
		return (short) (((b[index] & 0xFF) << 8) | (b[index + 1] & 0xFF));
	}

	/**
	 * Reads a signed int value in {@link #b b}.
	 * 
	 * @param index the start index of the value to be read in {@link #b b}.
	 * @return the read value.
	 */
	public int readInt(final int index) {
		readTo(index + 3);
		byte[] b = this.b;
		return ((b[index] & 0xFF) << 24) | ((b[index + 1] & 0xFF) << 16) | ((b[index + 2] & 0xFF) << 8)
				| (b[index + 3] & 0xFF);
	}

	/**
	 * Reads a signed long value in {@link #b b}.
	 * 
	 * @param index the start index of the value to be read in {@link #b b}.
	 * @return the read value.
	 */
	public long readLong(final int index) {
		readTo(index + 7);
		long l1 = readInt(index);
		long l0 = readInt(index + 4) & 0xFFFFFFFFL;
		return (l1 << 32) | l0;
	}

	/**
	 * Reads an UTF8 string constant pool item in {@link #b b}.
	 * 
	 * @param index the start index of an unsigned short value in {@link #b b},
	 *            whose value is the index of an UTF8 constant pool item.
	 * @param buf buffer to be used to read the item. This buffer must be
	 *            sufficiently large. It is not automatically resized.
	 * @return the String corresponding to the specified UTF8 item.
	 */
	public String readUTF8(int index, final char[] buf) {
		int item = readUnsignedShort(index);
		String s = strings[item];
		if (s != null) {
			return s;
		}
		index = items[item];
		return strings[item] = readUTF8(index + 2, readUnsignedShort(index), buf);
	}

	/**
	 * Reads UTF8 string in {@link #b b}.
	 * 
	 * @param index start offset of the UTF8 string to be read.
	 * @param utfLen length of the UTF8 string to be read.
	 * @param buf buffer to be used to read the string. This buffer must be
	 *            sufficiently large. It is not automatically resized.
	 * @return the String corresponding to the specified UTF8 string.
	 */
	private String readUTF8(int index, final int utfLen, final char[] buf) {
		int endIndex = index + utfLen;
		readTo(endIndex);
		byte[] b = this.b;
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
	 * Reads a class constant pool item in {@link #b b}.
	 * 
	 * @param index the start index of an unsigned short value in {@link #b b},
	 *            whose value is the index of a class constant pool item.
	 * @param buf buffer to be used to read the item. This buffer must be
	 *            sufficiently large. It is not automatically resized.
	 * @return the String corresponding to the specified class item.
	 */
	public String readClass(final int index, final char[] buf) {
		// computes the start index of the CONSTANT_Class item in b
		// and reads the CONSTANT_Utf8 item designated by
		// the first two bytes of this CONSTANT_Class item
		return readUTF8(items[readUnsignedShort(index)], buf);
	}

	public void release() {
		nBytesUsed += highMark;
	}

}
