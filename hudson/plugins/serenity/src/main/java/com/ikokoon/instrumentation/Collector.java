package com.ikokoon.instrumentation;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.ikokoon.IConstants;
import com.ikokoon.instrumentation.model.Afferent;
import com.ikokoon.instrumentation.model.Class;
import com.ikokoon.instrumentation.model.Efferent;
import com.ikokoon.instrumentation.model.Line;
import com.ikokoon.instrumentation.model.Method;
import com.ikokoon.instrumentation.model.Package;
import com.ikokoon.persistence.IDataBase;
import com.ikokoon.toolkit.Toolkit;

/**
 * TODO - make this class non static
 * 
 * This class collects the data from the processing. It adds the metrics to the packages, classes, methods and lines and persists the data in the
 * database. This is the central collection class for the coverage and dependency functionality.
 * 
 * @author Michael Couck
 * @since 12.07.09
 * @version 01.00
 */
public class Collector implements IConstants {

	/** The logger. */
	private static final Logger LOGGER = Logger.getLogger(Collector.class);
	/** The timestamp for the build. */
	public static Date timestamp = new Date();
	/** The database/persistence object. */
	private static IDataBase dataBase;
	static {
		try {
			dataBase = IDataBase.DataBase.getDataBase();
			// Reset the counter for all the lines
			List<Line> lines = dataBase.find(Line.class, 0, Integer.MAX_VALUE);
			for (Line line : lines) {
				line.setCounter(0d);
				dataBase.merge(line);
			}
		} catch (Exception e) {
			LOGGER.error("Exception initilizing the database", e);
		}
	}

	/**
	 * This method accumulates the number of times a thread goes through each line in a method.
	 * 
	 * @param className
	 *            the name of the class that is calling this method
	 * @param lineNumber
	 *            the line number of the line that is calling this method
	 * @param methodName
	 *            the name of the method that the line is in
	 * @param methodDescription
	 *            the description of the method
	 */
	public static final void collectCoverage(String className, String lineNumber, String methodName, String methodDescription) {
		Line line = getLine(className, methodName, methodDescription, lineNumber);
		line.increment();
		dataBase.merge(line);
	}

	/**
	 * This method collects the number of lines in a method. Note that for constructors the instance variables that are instanciated and allocated
	 * space on the stack are also counted as a line in the constructor.
	 * 
	 * @param className
	 *            the name fo the class
	 * @param methodName
	 *            the name of the method
	 * @param methodDescription
	 *            the description or signature of the method
	 * @param lineCounter
	 *            the number of lines in the method
	 */
	public static final void collectCoverage(String className, String methodName, String methodDescription, double lineCounter) {
		Method method = getMethod(className, methodName, methodDescription);
		method.setLines(lineCounter);
		dataBase.merge(method);
	}

	/**
	 * This method is called after each jumps in the method graph. Every time there is a jump the complexity goes up one point. Jumps include if else
	 * statements, or just if, throws statements, switch and so on.
	 * 
	 * @param className
	 *            the name of the class the method is in
	 * @param methodName
	 *            the name of the method
	 * @param methodDescription
	 *            the methodDescriptionription of the method
	 */
	public static final void collectComplexity(String className, String methodName, String methodDescription, double complexity) {
		Method method = getMethod(className, methodName, methodDescription);
		method.setComplexity(complexity);
		dataBase.merge(method);
	}

	/**
	 * Collects the packages that the class references and adds them to the document.
	 * 
	 * @param className
	 *            the name of the classes
	 * @param targetClassNames
	 *            the referenced class names
	 */
	public static final void collectMetrics(String className, String... targetClassNames) {
		String packageName = Toolkit.classNameToPackageName(className);
		for (String targetClassName : targetClassNames) {
			// Is the target name outside the package for this class
			String targetPackageName = Toolkit.classNameToPackageName(targetClassName);
			if (targetPackageName.trim().equals("")) {
				continue;
			}
			// Is the target and the source the same package name
			if (targetPackageName.equals(packageName)) {
				continue;
			}
			// Exclude java.lang classes and packages
			if (Configuration.getConfiguration().excluded(packageName) || Configuration.getConfiguration().excluded(targetPackageName)) {
				continue;
			}
			// Add the target package name to the afferent packages for this package
			Class klass = getClass(className);
			Afferent afferent = getAfferent(klass, targetPackageName);
			if (!klass.getAfferentPackages().contains(afferent)) {
				klass.getAfferentPackages().add(afferent);
				dataBase.merge(klass);
			}
			// Add this package to the eferent packages of the target
			Class targetClass = getClass(targetClassName);
			Efferent efferent = getEfferent(targetClass, packageName);
			if (!targetClass.getEfferentPackages().contains(efferent)) {
				targetClass.getEfferentPackages().add(efferent);
				dataBase.merge(targetClass);
			}
		}
	}

	/**
	 * Adds the interface attribute to the class element.
	 * 
	 * @param className
	 *            the name of the class
	 * @param access
	 *            the access opcode associated to the class
	 */
	public static final void collectMetrics(String className, Integer access) {
		if (access.intValue() == 1537) {
			Class klass = getClass(className);
			if (!klass.getInterfaze()) {
				klass.setInterfaze(true);
				dataBase.merge(klass);
			}
		}
	}

	private static final Package getPackage(String className) {
		className = Toolkit.slashToDot(className);
		String packageName = Toolkit.classNameToPackageName(className);

		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(NAME, packageName);
		Package pakkage = dataBase.find(Package.class, parameters);

		if (pakkage == null) {
			pakkage = new Package();
			pakkage.setName(packageName);
			pakkage.setComplexity(1d);
			pakkage.setCoverage(0d);
			pakkage.setAbstractness(0d);
			pakkage.setStability(0d);
			pakkage.setDistance(0d);
			pakkage.setInterfaces(0d);
			pakkage.setImplementations(0d);
			pakkage.setTimestamp(timestamp);
			pakkage = dataBase.persist(pakkage);
			LOGGER.debug("Added package : " + pakkage);
		}
		return pakkage;
	}

	private static final Class getClass(String className) {
		className = Toolkit.slashToDot(className);

		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(NAME, className);
		Class klass = dataBase.find(Class.class, parameters);

		if (klass == null) {
			klass = new Class();

			klass.setName(className);
			klass.setComplexity(1d);
			klass.setCoverage(0d);
			klass.setStability(0d);
			klass.setEfferent(0d);
			klass.setAfferent(0d);
			klass.setInterfaze(false);
			klass.setTimestamp(timestamp);

			Package pakkage = getPackage(className);
			pakkage.getChildren().add(klass);
			klass.setParent(pakkage);

			klass = dataBase.persist(klass);
			LOGGER.debug("Added class  : " + klass);
		}
		return klass;
	}

	private static final Method getMethod(String className, String methodName, String methodDescription) {
		Method method = null;

		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(CLASS_NAME, className);
		parameters.put(NAME, methodName);
		parameters.put(DESCRIPTION, methodDescription);
		method = dataBase.find(Method.class, parameters);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Looking for method : " + className + ", " + methodName + ", " + methodDescription + ", " + method);
		}

		if (method == null) {
			method = new Method();

			method.setName(methodName);
			method.setClassName(className);
			method.setDescription(methodDescription);
			method.setComplexity(0d);
			method.setChildren(new TreeSet<Line>());
			method.setCoverage(0d);
			method.setTimestamp(timestamp);

			Class klass = getClass(className);
			method.setParent(klass);
			if (klass.getChildren() == null) {
				List<Method> children = new ArrayList<Method>();
				klass.setChildren(children);
			}
			klass.getChildren().add(method);

			dataBase.persist(method);
			LOGGER.debug("Added method : " + method);
		}
		return method;
	}

	protected static final Line getLine(String className, String methodName, String methodDescription, String lineNumber) {
		Line line = null;
		double lineNumberDouble = Double.parseDouble(lineNumber);

		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(CLASS_NAME, className);
		parameters.put(METHOD_NAME, methodName);
		parameters.put(NUMBER, lineNumberDouble);
		line = dataBase.find(Line.class, parameters);

		if (line == null) {
			line = new Line();

			line.setNumber(Double.parseDouble(lineNumber));
			line.setCounter(0d);
			line.setTimestamp(timestamp);
			line.setClassName(className);
			line.setMethodName(methodName);

			Method method = getMethod(className, methodName, methodDescription);
			line.setParent(method);
			method.getChildren().add(line);

			dataBase.persist(line);
			LOGGER.debug("Added line : " + line);
		}
		return line;
	}

	private static final Efferent getEfferent(Class klass, String packageName) {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(NAME, packageName);
		Efferent efferent = dataBase.find(Efferent.class, parameters);
		if (efferent == null) {
			efferent = new Efferent();
			efferent.setName(packageName);
			efferent.setTimestamp(timestamp);

			efferent.getBases().add(klass);
			klass.getEfferentPackages().add(efferent);

			dataBase.persist(efferent);
		}
		return efferent;
	}

	private static final Afferent getAfferent(Class klass, String packageName) {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(NAME, packageName);
		Afferent afferent = dataBase.find(Afferent.class, parameters);
		if (afferent == null) {
			afferent = new Afferent();
			afferent.setName(packageName);
			afferent.setTimestamp(timestamp);

			afferent.getBases().add(klass);
			klass.getAfferentPackages().add(afferent);

			dataBase.persist(afferent);
		}
		return afferent;
	}

}