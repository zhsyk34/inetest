package com.dnake.smart.core.dict;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * TCP包数据格式
 */
public class Packet {
	//header
	//public static final byte[] HEADERS = new byte[]{0x5A, (byte) 0xA5};
	public static final List<Byte> HEADERS = Collections.unmodifiableList(Arrays.asList((byte) 0x5A, (byte) 0xA5));
	//footer
	//public static final byte[] FOOTERS = new byte[]{(byte) 0xA5, 0x5A};
	public static final List<Byte> FOOTERS = Collections.unmodifiableList(Arrays.asList((byte) 0xA5, (byte) 0x5A));
	//length
	public static final int LENGTH = 2;
	//data
	public static final int MIN_DATA = 1;
	//verifyKey
	public static final int VERIFY = 2;
	//数据部分以外(冗余数据)的长度
	public static final int REDUNDANT = HEADERS.size() + LENGTH + VERIFY + FOOTERS.size();//8
	//total
	public static final int MSG_MIN_LENGTH = REDUNDANT + MIN_DATA;
}
