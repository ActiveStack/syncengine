package com.percero.framework.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

public class ThreadedStreamHandler extends Thread {

	InputStream inStream;
	String adminPassword;
	OutputStream outStream;
	PrintWriter printWriter;
	StringBuilder outBuffer = new StringBuilder();
	private boolean sudoIsRequired = false;
	
	public ThreadedStreamHandler(InputStream inStream) {
		this.inStream = inStream;
	}
	
	public ThreadedStreamHandler(InputStream inStream, OutputStream outStream, String adminPassword) {
		this.inStream = inStream;
		this.outStream = outStream;
		this.adminPassword = adminPassword;
		this.printWriter = new PrintWriter(this.outStream);
		this.sudoIsRequired = true;
	}
	
	public void run() {
		if (sudoIsRequired) {
			printWriter.println(adminPassword);
			printWriter.flush();
		}
		
		BufferedReader bufferedReader = null;
		
		try {
			bufferedReader = new BufferedReader(new InputStreamReader(inStream));
			String line = null;
			
			while((line = bufferedReader.readLine()) != null) {
				outBuffer.append(line + "\n");
			}
		} catch(IOException e) {
			e.printStackTrace();
		} catch(Throwable t) {
			t.printStackTrace();
		} finally {
			try {
				if (bufferedReader != null) {
					bufferedReader.close();
				}
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public StringBuilder getOutputBuffer() {
		return outBuffer;
	}
}
