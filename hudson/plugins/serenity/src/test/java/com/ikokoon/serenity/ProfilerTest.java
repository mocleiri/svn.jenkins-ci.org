package com.ikokoon.serenity;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ikokoon.serenity.model.Class;
import com.ikokoon.serenity.model.Method;
import com.ikokoon.serenity.persistence.DataBaseOdb;
import com.ikokoon.serenity.persistence.IDataBase;

/**
 * This test needs to have assertions. TODO implement the real tests.
 *
 * @author Michael Couck
 * @since 19.06.10
 * @version 01.00
 */
public class ProfilerTest extends ATest implements IConstants {

	private Logger logger = Logger.getLogger(this.getClass());
	private static IDataBase dataBase;

	@BeforeClass
	public static void beforeClass() {
		ATest.beforeClass();
		String dataBaseFile = "./src/test/resources/isearch/serenity.odb";
		dataBase = IDataBase.DataBaseManager.getDataBase(DataBaseOdb.class, dataBaseFile, mockInternalDataBase);
	}

	@AfterClass
	public static void afterClass() {
		ATest.afterClass();
		dataBase.close();
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void averageMethodNetTime() {
		List<Class> classes = dataBase.find(Class.class);
		for (Class klass : classes) {
			List<Method<?, ?>> methods = klass.getChildren();
			for (Method<?, ?> method : methods) {
				double averageMethodNetTime = Profiler.averageMethodNetTime(method);
				logger.debug("Average net method time : method : " + method.getName() + " - " + averageMethodNetTime);
			}
		}
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void averageMethodTime() {
		List<Class> classes = dataBase.find(Class.class);
		for (Class klass : classes) {
			List<Method<?, ?>> methods = klass.getChildren();
			for (Method<?, ?> method : methods) {
				double averageMethodTime = Profiler.averageMethodTime(method);
				logger.debug("Average method : method : " + method.getName() + " - " + averageMethodTime);
			}
		}
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void methodChange() {
		List<Class> classes = dataBase.find(Class.class);
		for (Class klass : classes) {
			List<Method<?, ?>> methods = klass.getChildren();
			for (Method<?, ?> method : methods) {
				double methodChange = Profiler.methodChange(method);
				logger.debug("Method change : method : " + method.getName() + " - " + methodChange);
			}
		}
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void methodChangeSeries() {
		List<Class> classes = dataBase.find(Class.class);
		for (Class klass : classes) {
			List<Method<?, ?>> methods = klass.getChildren();
			for (Method<?, ?> method : methods) {
				List<Double> series = Profiler.methodChangeSeries(method);
				logger.debug("Method change series : " + method.getName() + " - " + series);
			}
		}
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void methodNetChange() {
		List<Class> classes = dataBase.find(Class.class);
		for (Class klass : classes) {
			List<Method<?, ?>> methods = klass.getChildren();
			for (Method<?, ?> method : methods) {
				double methodNetChange = Profiler.methodNetChange(method);
				logger.debug("Method net change : method : " + method.getName() + " - " + methodNetChange);
			}
		}
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void methodNetChangeSeries() {
		List<Class> classes = dataBase.find(Class.class);
		for (Class klass : classes) {
			List<Method<?, ?>> methods = klass.getChildren();
			for (Method<?, ?> method : methods) {
				List<Double> methodNetChangeSeries = Profiler.methodNetChangeSeries(method);
				logger.debug("Method net change series : method : " + method.getName() + " - " + methodNetChangeSeries);
			}
		}
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void methodNetSeries() {
		List<Class> classes = dataBase.find(Class.class);
		for (Class klass : classes) {
			List<Method<?, ?>> methods = klass.getChildren();
			for (Method<?, ?> method : methods) {
				List<Double> methodNetSeries = Profiler.methodNetSeries(method);
				logger.debug("Method net series : method : " + method.getName() + " - " + methodNetSeries);
			}
		}
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void methodSeries() {
		List<Class> classes = dataBase.find(Class.class);
		for (Class klass : classes) {
			List<Method<?, ?>> methods = klass.getChildren();
			for (Method<?, ?> method : methods) {
				List<Double> series = Profiler.methodSeries(method);
				logger.debug("Method series : " + method.getName() + " - " + series);
			}
		}
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void totalMethodTime() {
		List<Class> classes = dataBase.find(Class.class);
		for (Class klass : classes) {
			List<Method<?, ?>> methods = klass.getChildren();
			for (Method<?, ?> method : methods) {
				double totalMethodTime = Profiler.totalMethodTime(method);
				logger.debug("Total method time : method : " + method.getName() + " - " + totalMethodTime);
			}
		}
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void totalNetMethodTime() {
		List<Class> classes = dataBase.find(Class.class);
		for (Class klass : classes) {
			List<Method<?, ?>> methods = klass.getChildren();
			for (Method<?, ?> method : methods) {
				double totalNetMethodTime = Profiler.totalNetMethodTime(method);
				logger.debug("Total net method time : method : " + method.getName() + " - " + totalNetMethodTime);
			}
		}
	}

}