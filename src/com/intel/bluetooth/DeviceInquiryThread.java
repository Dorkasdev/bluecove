/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2007 Vlad Skarzhevskyy
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *  @version $Id$
 */
package com.intel.bluetooth;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DiscoveryListener;

/**
 * This is Common class to solve JNI call backs problem
 *
 */
public class DeviceInquiryThread extends Thread {
	
	private BluetoothStack stack;
	
	private int accessCode;

	private DiscoveryListener listener;

	private BluetoothStateException startException;
	
	private boolean started = false;
	
	private boolean terminated = false;
	
	private DeviceInquiryThread(BluetoothStack stack, int accessCode, DiscoveryListener listener) {
		super("DeviceInquiryThread");
		this.stack = stack;
		this.accessCode = accessCode;
		this.listener = listener;
	}

	/**
	 * Start DeviceInquiry and wait for startException or deviceInquiryStartedCallback
	 */
	public static boolean startInquiry(BluetoothStack stack, int accessCode, DiscoveryListener listener) throws BluetoothStateException {
		DeviceInquiryThread t = (new DeviceInquiryThread(stack, accessCode, listener));
		synchronized (t) {
			t.start();
			while (!t.started && !t.terminated) {
				try {
					t.wait();
				} catch (InterruptedException e) {
					return false;
				}
				if (t.startException != null) {
					throw t.startException;
				}
			}
		}
		return t.started;
	}
	
	public void run() {
		int discType = DiscoveryListener.INQUIRY_ERROR;
		try {
			discType = stack.runDeviceInquiry(this, accessCode, listener);
		} catch (BluetoothStateException e) {
			startException = e;
			return;
		} finally {
			terminated = true;
			synchronized (this) {
				notifyAll();
			}
			DebugLog.debug("runDeviceInquiry ends");
		}
		listener.inquiryCompleted(discType);
	}
	
	public void deviceInquiryStartedCallback() {
		DebugLog.debug("deviceInquiryStartedCallback");
		started = true;
		synchronized (this) {
			notifyAll();
		}
	}
	
}
