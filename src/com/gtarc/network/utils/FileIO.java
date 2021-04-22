package com.gtarc.network.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class FileIO {

	public String createFolder(String folderName) {
		String workingDir = System.getProperty("user.dir");
		//System.out.println("Current working directory : " + workingDir);

		new File(workingDir + "/" + folderName).mkdirs();
		return workingDir + "/" + folderName;
	}

	public void createNewFile(String path, String fileName) {

		File file = new File(path + "/" + fileName);
		OutputStream out;
		try {
			out = new FileOutputStream(file);
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Write your data

	}

	/**
	 * 
	 * @param dateFormat
	 * @return
	 */
	public String createNewFileName(String dateFormat) {
		String fileName = "";
		try {
			Calendar cal = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
			String appendedName = sdf.format(cal.getTime());
			fileName = "file" + appendedName + ".txt";
			;
		} catch (Exception e) {
			System.out.println(FileIO.class.getName() + ": File name couldn't be created.");
		}
		return fileName;
	}

	/**
	 * 
	 * @param dateFormat
	 * @return
	 */
	public String createNewFileName(String possiblePrefix, String dateFormat) {
		String fileName = "";
		try {
			Calendar cal = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
			String appendedName = sdf.format(cal.getTime());
			fileName = possiblePrefix + appendedName + ".txt";
			;
		} catch (Exception e) {
			System.out.println(FileIO.class.getName() + ": File name couldn't be created.");
		}
		return fileName;
	}

	/**
	 * 
	 * @param folderName
	 * @param fileName
	 */
	public void readFile(String folderName, String fileName) {
		System.out.println(fileName + " file under " + folderName + " folder is now being readed...");
		String fileNameFullPath = folderName + "/" + fileName;
		try {

			/*
			 * Sets up a file reader to read the file passed on the command line one
			 * character at a time
			 */
			FileReader input = new FileReader(fileNameFullPath);

			/*
			 * Filter FileReader through a Buffered read to read a line at a time
			 */
			BufferedReader bufRead = new BufferedReader(input);

			String line; // String that holds current file line
			int count = 0; // Line number of count

			// Read first line
			line = bufRead.readLine();
			count++;

			// Read through file one line at time. Print line # and line
			while (line != null) {
				System.out.println(count + ": " + line);
				line = bufRead.readLine();
				count++;
			}

			bufRead.close();

		} catch (ArrayIndexOutOfBoundsException e) {
			/*
			 * If no file was passed on the command line, this expception is generated. A
			 * message indicating how to the class should be called is displayed
			 */
			//System.out.println("Usage: java ReadFile filename\n");

		} catch (IOException e) {
			// If another exception is generated, print a stack trace
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param fileName
	 */
	public String readFile(String fileName) {
		BufferedReader bufRead = null;
		try {

			/*
			 * Filter FileReader through a Buffered read to read a line at a time
			 */
			bufRead = new BufferedReader(new FileReader(fileName));
			StringBuilder sb = new StringBuilder();

			String line; // String that holds current file line
			int count = 0; // Line number of count

			// Read first line
			line = bufRead.readLine();
			// count++;
			sb.append(line);
			// Read through file one line at time. Print line # and line
			while (line != null) {
				// System.out.println(count+": "+line);
				sb.append(line);
				line = bufRead.readLine();
				// count++;
			}

			bufRead.close();
			return sb.toString();
		} catch (ArrayIndexOutOfBoundsException e) {
			/*
			 * If no file was passed on the command line, this expception is generated. A
			 * message indicating how to the class should be called is displayed
			 */
			//System.out.println("Usage: java ReadFile filename\n");

		} catch (IOException e) {
			// If another exception is generated, print a stack trace
			e.printStackTrace();
		} finally {
			try {
				bufRead.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}

	public String trim(String fileName) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(fileName));
			StringBuffer result = new StringBuffer();
			String line;
			while ((line = reader.readLine()) != null)
				result.append(line.trim());
			return result.toString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 
	 * @param path
	 * @param fileName
	 * @param obj
	 */
	public void storeFile(String path, String fileName, String obj) {
		String root = "";// Environment.getExternalStorageDirectory().toString();
		File myDir = new File(root);
		boolean success = false;
		if (!myDir.exists()) {
			success = myDir.mkdirs();
		}

		String fname = fileName;
		// String string = "";
		File file = new File(myDir, fname);
		// if (file.exists ()) file.delete ();
		try {
			FileOutputStream out = new FileOutputStream(file, true);
			out.write(obj.getBytes());
			out.flush();
			out.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param file
	 * @param outputString
	 */
	public void writeToFile(String file, String outputString) {
		try {
			File f;
			f = new File(file);
			// writeConsole(debugOutFileTaos2 + " file is being written");
			FileOutputStream fos;
			// we are not appending... we are writing over...
			fos = new FileOutputStream(f, false);

			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(fos));
			out.write(outputString + "\n");
			out.flush();
			out.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param file
	 * @param outputString
	 */
	public void createFile(String path, String file) {
		File f = new File(path + file);
		try {
			f.delete();
			f.createNewFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param file
	 * @param outputString
	 */

	public void appendToFile(String file, String outputString) {
		try {
			File f;
			f = new File(file);
			// writeConsole(debugOutFileTaos2 + " file is being written");
			FileOutputStream fos;
			fos = new FileOutputStream(f, true);

			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(fos));
			out.write(outputString + "\n");
			out.flush();
			out.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * 
	 * @param file
	 * @param outputString
	 */
	public void writeSynchronouslyToFile(String file, String outputString) {
		// TODO Auto-generated method stub
		try {
			File f;
			f = new File(file);
			// writeConsole(debugOutFileTaos2 + " file is being written");
			FileOutputStream fos;
			// we are not appending... we are writing over...
			fos = new FileOutputStream(f, false);
			fos.getChannel().lock();
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(fos));
			out.write(outputString + "\n");
			out.flush();
			out.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * 
	 * @param file
	 * @param outputString
	 */

	public void appendSynchronouslyToFile(String file, String outputString) {
		// TODO Auto-generated method stub
		try {

			File f;
			f = new File(file);
			// writeConsole(debugOutFileTaos2 + " file is being written");
			FileOutputStream fos;
			// we are appending...
			fos = new FileOutputStream(f, true);
			fos.getChannel().lock();
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(fos));
			out.write(outputString + "\n");
			out.flush();
			out.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}