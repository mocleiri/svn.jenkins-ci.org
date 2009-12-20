package com.ikokoon.serenity.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;

import com.ikokoon.toolkit.Toolkit;

/**
 * @author Michael Couck
 * @since 12.08.09
 * @version 01.00
 */
@Entity
@Unique(fields = { Composite.NAME })
public class Class<E, F> extends Composite<Package<?, ?>, Method<?, ?>> implements Comparable<Class<?, ?>>, Serializable {

	private String name;
	private String source;

	private double coverage;
	private double complexity;
	private double stability;

	private double lines;
	private double executed;
	private double efference;
	private double afference;

	private boolean interfaze;

	private List<Efferent> efferent = new ArrayList<Efferent>();
	private List<Afferent> afferent = new ArrayList<Afferent>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public double getLines() {
		return lines;
	}

	public void setLines(double lines) {
		this.lines = lines;
	}

	public double getExecuted() {
		return executed;
	}

	public void setExecuted(double totalLinesExecuted) {
		this.executed = totalLinesExecuted;
	}

	public String getNameTrimmed() {
		if (getName() != null) {
			int index = getName().lastIndexOf('.');
			if (index > -1) {
				return getName().substring(index + 1, getName().length());
			}
		}
		return null;
	}

	public double getComplexity() {
		return Toolkit.format(complexity, PRECISION);
	}

	public void setComplexity(double complexity) {
		this.complexity = complexity;
	}

	public double getCoverage() {
		return Toolkit.format(coverage, PRECISION);
	}

	public void setCoverage(double coverage) {
		this.coverage = coverage;
	}

	public double getStability() {
		return Toolkit.format(stability, PRECISION);
	}

	public void setStability(double stability) {
		this.stability = stability;
	}

	public double getEfference() {
		return Toolkit.format(efference, PRECISION);
	}

	public void setEfference(double efferent) {
		this.efference = efferent;
	}

	public double getAfference() {
		return Toolkit.format(afference, PRECISION);
	}

	public void setAfference(double afferent) {
		this.afference = afferent;
	}

	public boolean getInterfaze() {
		return interfaze;
	}

	public void setInterfaze(boolean interfaze) {
		this.interfaze = interfaze;
	}

	@ManyToMany(cascade = { CascadeType.MERGE, CascadeType.REFRESH }, fetch = FetchType.LAZY)
	public List<Efferent> getEfferent() {
		return efferent;
	}

	public void setEfferent(List<Efferent> efferent) {
		this.efferent = efferent;
	}

	@ManyToMany(cascade = { CascadeType.MERGE, CascadeType.REFRESH }, fetch = FetchType.LAZY)
	public List<Afferent> getAfferent() {
		return afferent;
	}

	public void setAfferent(List<Afferent> afferent) {
		this.afferent = afferent;
	}

	public String toString() {
		return getId() + ":" + name;
	}

	public int compareTo(Class<?, ?> o) {
		int comparison = 0;
		if (this.getId() != null && o.getId() != null) {
			comparison = this.getId().compareTo(o.getId());
		} else {
			if (this.getName() != null && o.getName() != null) {
				comparison = this.getName().compareTo(o.getName());
			}
		}
		return comparison;
	}

}