package org.jvnet.hudson.plugins.backup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jvnet.hudson.plugins.backup.utils.compress.CompressionMethodEnum;

/**
 * User: vsellier Date: Apr 20, 2009 Time: 11:35:57 PM
 */
public class BackupConfig {

	private final static String DEFAULT_FILE_NAME_TEMPLATE = "backup_@date@.@extension@";
	private final static CompressionMethodEnum DEFAULT_COMPRESSION_METHOD = CompressionMethodEnum.ZIP;

	private String targetDirectory;
	private boolean verbose;
	private String fileNameTemplate = DEFAULT_FILE_NAME_TEMPLATE;
	private CompressionMethodEnum archiveType = CompressionMethodEnum.TARGZIP;
	private boolean keepWorkspaces;
	private boolean keepFingerprints;
	private boolean keepBuilds;
	private boolean keepArchives;
	
	/**
	 * files or directory names not included into the backup
	 */
	private List<String> customExclusions = new ArrayList<String>();

	public List<String> getCustomExclusions() {
		return customExclusions;
	}

	public void setCustomExclusions(List<String> exclusions) {
		this.customExclusions.clear();
		this.customExclusions.addAll(exclusions);
	}

	public void addExclusion(String exclusion) {
		this.customExclusions.add(exclusion);
	}

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public String getTargetDirectory() {

		return targetDirectory;
	}

	public void setTargetDirectory(String targetDirectory) {
		this.targetDirectory = targetDirectory;
	}

	public String getFileNameTemplate() {
		if (StringUtils.isNotEmpty(fileNameTemplate)) {
			return fileNameTemplate;
		} else {
			return DEFAULT_FILE_NAME_TEMPLATE;
		}
	}

	public void setFileNameTemplate(String fileNameTemplate) {
		this.fileNameTemplate = fileNameTemplate;
	}

	public CompressionMethodEnum getArchiveType() {
		if (archiveType != null) {
			return archiveType;
		} else {
			return DEFAULT_COMPRESSION_METHOD;
		}
	}

	public void setArchiveType(CompressionMethodEnum archiveType) {
		this.archiveType = archiveType;
	}
	
	public void setKeepWorkspaces(boolean keeping) {
		this.keepWorkspaces = keeping;
	}

	public boolean getKeepWorkspaces() {
		return keepWorkspaces;
	}

	public void setKeepFingerprints(boolean keepFingerprints) {
		this.keepFingerprints = keepFingerprints;
	}

	public boolean getKeepFingerprints() {
		return keepFingerprints;
	}

	public void setKeepBuilds(boolean keepBuilds) {
		this.keepBuilds = keepBuilds;
	}

	public boolean getKeepBuilds() {
		return keepBuilds;
	}

	public void setKeepArchives(boolean keepArchives) {
		this.keepArchives = keepArchives;
	}

	public boolean getKeepArchives() {
		return keepArchives;
	}

}
