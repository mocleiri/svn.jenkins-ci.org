package com.thalesgroup.dtkit.tusar.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.File;

public class FilePurifyMetrics {
    private int numberOfErrors;
    private int numberOfMemoryLeaks;
    private int numberOfBytesLost;
    private String filename;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public File getSourcePath() {
        return new File(getFilename());
    }

    public void addNumberOfErrors(int numberOfErrors) {
        this.numberOfErrors += numberOfErrors;
    }

    public int getNumberOfErrors() {
        return numberOfErrors;
    }

    public void addNumberOfMemoryLeaks(int numberOfMemoryLeaks) {
        this.numberOfMemoryLeaks += numberOfMemoryLeaks;
    }

    public int getNumberOfMemoryLeaks() {
        return numberOfMemoryLeaks;
    }

    public void addNumberOfBytesLost(int numberOfBytesLost) {
        this.numberOfBytesLost += numberOfBytesLost;
    }

    public int getNumberOfBytesLost() {
        return numberOfBytesLost;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FilePurifyMetrics)) {
            return false;
        } else if (this == obj) {
            return true;
        } else {
            FilePurifyMetrics other = (FilePurifyMetrics) obj;

            return new EqualsBuilder()
                    .append(filename, other.getFilename())
                    .append(numberOfErrors, other.getNumberOfErrors())
                    .append(numberOfMemoryLeaks, other.getNumberOfMemoryLeaks())
                    .append(numberOfBytesLost, other.getNumberOfBytesLost())
                    .isEquals();
        }
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(11, 43)
                .append(filename)
                .append(numberOfErrors)
                .append(numberOfMemoryLeaks)
                .append(numberOfBytesLost)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("filename", filename)
                .append("numberOfErrors", numberOfErrors)
                .append("numberOfMemoryLeaks", numberOfMemoryLeaks)
                .append("numberOfBytesLost", numberOfBytesLost)
                .toString();
    }
}
