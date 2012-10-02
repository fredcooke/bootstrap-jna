package com.fredcooke.bootstrapjna.demo;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TooManyListenersException;

import purejavacomm.CommPort;
import purejavacomm.CommPortIdentifier;
import purejavacomm.NoSuchPortException;
import purejavacomm.PortInUseException;
import purejavacomm.SerialPort;
import purejavacomm.SerialPortEvent;
import purejavacomm.SerialPortEventListener;
import purejavacomm.UnsupportedCommOperationException;

public final class DemonstrateDecoding extends Thread implements SerialPortEventListener {
	private static final int BAUD = 115200;
	private static final int TIMEOUT = 2000;

	private static final long GLANCE_AWAY = 50; // 1 = 9%, 2 = 8%, 5 = 5%, 10/20 = 3%, 50/100 = 0.3%, 250 = 0.2% listening/per byte = 14% all with 4000 buffer

	private final boolean tail;

	public DemonstrateDecoding(final InputStream is, final OutputStream os, final boolean tail) {
		this.tail = tail;
	}

	/**
	 * Demonstration application to show how to read FreeEMS data streams using the library.
	 *
	 * @param args verb and subject
	 */
	public static void main(final String[] args) {
		if (args.length != 2) {
			System.out.println(args.length + " is the wrong number of arguments!");
		} else {
			final String verb = args[0];
			if ("tail".equals(verb)) {
				tailFile(args[1]);
			} else if ("events".equals(verb)) {
				listenToDevice(args[1], true);
			} else if ("watch".equals(verb)) {
				listenToDevice(args[1], false);
			} else {
				System.out.println(verb + " is not a valid command word!");
			}
		}
	}

	@Override
	public void serialEvent(final SerialPortEvent arg0) {
	}

	@Override
	public void run() {
		try {
			while (true) {
				if (tail) {
					sleep(GLANCE_AWAY);
				} else {
					sleep(Long.MAX_VALUE); // Sleep for 9223372036854775.807 seconds TODO better way to keep this alive?
				}
			}
		} catch (InterruptedException e) {
			System.out.println("Interrupted");
			e.printStackTrace();
		}
	}

	private static void listenToDevice(final String devicePath, final boolean eventDriven) {
		try {
			final File device = new File(devicePath);
			final String deviceName = device.getName();
			final String logFilePath = System.getProperty("user.home") + "/" + deviceName + "-" + System.currentTimeMillis() + ".bin";
			System.out.println("Writing log to " + logFilePath);
			final OutputStream os = new BufferedOutputStream(new FileOutputStream(logFilePath));
			final CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(devicePath);
			if (portIdentifier.isCurrentlyOwned()) {
				System.out.println("Port is currently in use, kill the other app, or choose another!");
			} else {
				final CommPort commPort = portIdentifier.open(DemonstrateDecoding.class.getName(), TIMEOUT);
				if (commPort instanceof SerialPort) {
					final SerialPort serialPort = (SerialPort) commPort;
					serialPort.setSerialPortParams(BAUD, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_ODD);
					final InputStream is = serialPort.getInputStream();

					final DemonstrateDecoding dd = new DemonstrateDecoding(is, os, !eventDriven);
					if (eventDriven) {
						serialPort.addEventListener(dd);
						serialPort.notifyOnDataAvailable(true);
					}
					dd.start(); // TODO Is there a cleaner way to stay alive?
				} else {
					System.out.println("Device specified is not a serial port!");
				}
			}
		} catch (FileNotFoundException e) {
			System.out.println("Issue writing log to home dir!");
			e.printStackTrace();
		} catch (PortInUseException e) {
			System.out.println("Supplied device is in use!");
			e.printStackTrace();
		} catch (NoSuchPortException e) {
			System.out.println("Supplied device does not exist!");
			e.printStackTrace();
		} catch (UnsupportedCommOperationException e) {
			System.out.println("Supplied device does not allow 115200 8O1 operation!");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Failed to get input stream!");
			e.printStackTrace();
		} catch (TooManyListenersException e) {
			System.out.println("Too many listeners on this device!");
			e.printStackTrace();
		}
	}

	private static void tailFile(final String filename) {
		try {
			final InputStream is = new FileInputStream(filename);
			final DemonstrateDecoding dd = new DemonstrateDecoding(is, null, true);
			dd.start();
		} catch (FileNotFoundException e) {
			System.out.println("Issue tailing supplied file!");
			e.printStackTrace();
		}
	}
}
