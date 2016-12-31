package com.dnake.smart.core.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UDPRecord {
	private String ip;
	private String sn;
	private int port;
	private long happen;
}
