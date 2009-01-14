package org.jvnet.hudson.plugins.backup.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.AutoCloseInputStream;

public class ZipBackupEngine extends DirectoryWalker {
	private PrintWriter logFile;
	private File source;
	
	/**
	 * the lengt of the source string, to speed up walk
	 */
	private int sourceLength;
	private ZipOutputStream target;

	public ZipBackupEngine(PrintWriter logFile, String sourceDirectory,
			String targetName, FileFilter filter) throws IOException {
		super(filter, -1);
		this.logFile = logFile;
		this.source = new File(sourceDirectory);
		this.sourceLength = sourceDirectory.length();

		if (!targetName.endsWith(".zip")) {
			targetName = targetName + ".zip";
		}
		
		File targetFile = new File(targetName);
		logFile.println("Full backup file name : " + targetFile.getAbsolutePath());
		
		target = new ZipOutputStream(new FileOutputStream(targetFile));
		target.setLevel(9);
		
	}

	@Override
	protected void handleFile(File file, int depth, Collection results)
			throws IOException {
		String name = getInArchiveName(file.getAbsolutePath());
		
		logFile.println(name + " file");

		ZipEntry entry = new ZipEntry(name);
		entry.setTime(file.lastModified());
		target.putNextEntry(entry);
		
		InputStream stream = new AutoCloseInputStream(new FileInputStream(file));
		IOUtils.copy(stream, target);
		
		super.handleFile(file, depth, results);
	}

	@Override
	protected void handleEnd(Collection results) throws IOException {
		target.close();
	}

	public void doBackup() throws IOException {
		this.walk(source, new ArrayList<Object>());
	}
	
	/**
	 * Suppress the path to hudson working dir on the beginning of the path.
	 * +1 because of the ending /
	 * 
	 * @param absoluteName the name including dat path
	 * @return the archive name
	 */
	private String getInArchiveName(String absoluteName) {
		System.out.println(absoluteName);
		return absoluteName.substring(sourceLength + 1);
	}

}
