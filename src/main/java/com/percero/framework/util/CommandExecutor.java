package com.percero.framework.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class CommandExecutor {

	public static CommandExecutor run(String command, File rootDir) {
		try {
			List<String> commands = new  ArrayList<String>();
			commands.add("/bin/sh");
			commands.add("-c");
			
			if (rootDir != null)
			{
				if (!rootDir.isDirectory() || !rootDir.exists())
					rootDir.mkdirs();
				commands.add("cd " + rootDir.getAbsolutePath());
			}
			
			commands.add(command);
			
			CommandExecutor commandExecutor = new CommandExecutor(commands);
			int exitCode = commandExecutor.execute();
			
			String stdOut = commandExecutor.getStdOutputFromCommand();
			String stdErr = commandExecutor.getStdErrorFromCommand();
			
//			if (exitCode > 0) {
			System.out.println("ExitCode: " + exitCode);
			System.out.println("Std Out: " + stdOut);
			System.out.println("Std Err: " + stdErr);
//			}
			
			return commandExecutor;
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	private List<String> commandList;
	private String adminPassword;
	private String stdOutput;
	private String stdError;
	
	public CommandExecutor(final List<String> commandList) throws IllegalStateException {
		if (commandList == null || commandList.isEmpty())
			throw new IllegalStateException("Missing required commandList");
		
		this.commandList = commandList;
		this.adminPassword = null;
	}
	
	public CommandExecutor(final List<String> commandList, final String adminPassword) throws IllegalStateException {
		if (commandList == null || commandList.isEmpty())
			throw new IllegalStateException("Missing required commandList");
		
		this.commandList = commandList;
		this.adminPassword = adminPassword;
	}
	
	public int execute() throws IOException, InterruptedException
	{
		int exitValue = -99;
		
		try {
			ProcessBuilder processBuilder = new ProcessBuilder(commandList);
			Process process = processBuilder.start();
			
			OutputStream stdOutStream = process.getOutputStream();
			
			InputStream inStream = process.getInputStream();
			InputStream errStream = process.getErrorStream();
			
			ThreadedStreamHandler inputStreamHandler = new ThreadedStreamHandler(inStream, stdOutStream, adminPassword);
			ThreadedStreamHandler errorStreamHandler = new ThreadedStreamHandler(errStream);
			
			inputStreamHandler.start();
			errorStreamHandler.start();
			
			exitValue = process.waitFor();
			
			inputStreamHandler.interrupt();
			errorStreamHandler.interrupt();
			
			inputStreamHandler.join();
			errorStreamHandler.join();
			
			this.stdOutput = inputStreamHandler.getOutputBuffer().toString();
			this.stdError = errorStreamHandler.getOutputBuffer().toString();
		} catch(IOException e) {
			e.printStackTrace();
			throw e;
		} catch(InterruptedException e) {
			e.printStackTrace();
			throw e;
		}
		
		return exitValue;
	}
	
	public String getStdOutputFromCommand() {
		return stdOutput;
	}
	
	public String getStdErrorFromCommand() {
		return stdError;
	}
}
