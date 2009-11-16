package com.ikokoon.serenity;

import java.net.URL;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class LoggingConfigurator {

	private static Logger LOGGER;
	private static boolean initilised = false;

	static {
		configure();
	}

	public static void configure() {
		if (!initilised) {
			URL url = LoggingConfigurator.class.getResource(IConstants.LOG_4_J_PROPERTIES);
			if (url != null) {
				PropertyConfigurator.configure(url);
			} else {
				Properties properties = getProperties();
				PropertyConfigurator.configure(properties);
			}
			LOGGER = Logger.getLogger(LoggingConfigurator.class);
			LOGGER.info("Loaded logging properties from : " + url);
			initilised = true;
		}
	}

	private static Properties getProperties() {
		Properties properties = new Properties();
		// Root Logger
		properties.put("log4j.rootLogger", "INFO, ikokoon, file");
		properties.put("log4j.rootCategory", "INFO, ikokoon");

		// Serenity application logging file output
		properties.put("log4j.appender.file", "org.apache.log4j.DailyRollingFileAppender");
		properties.put("log4j.appender.file.Threshold", "DEBUG");
		properties.put("log4j.appender.file.File", "./serenity/serenity.log");
		properties.put("log4j.appender.file.layout", "org.apache.log4j.PatternLayout");
		properties.put("log4j.appender.file.layout.ConversionPattern", "%d{HH:mm:ss,SSS} %-5p %C:%L - %m%n");
		properties.put("log4j.appender.file.Append", "false");

		// Serenity application logging console output
		properties.put("log4j.appender.ikokoon", "org.apache.log4j.ConsoleAppender");
		properties.put("log4j.appender.ikokoon.Threshold", "DEBUG");
		properties.put("log4j.appender.ikokoon.ImmediateFlush", "true");
		properties.put("log4j.appender.ikokoon.layout", "org.apache.log4j.PatternLayout");
		properties.put("log4j.appender.ikokoon.layout.ConversionPattern", "%d{HH:mm:ss,SSS} %-5p %C:%L - %m%n");

		// Set the Serenity categories and thresholds
		properties.put("log4j.category.net", "WARN");
		properties.put("log4j.category.com", "WARN");
		properties.put("log4j.category.org", "WARN");

		// Specific thresholds
		properties.put("log4j.category.com.ikokoon", "INFO");
		properties.put("log4j.category.com.ikokoon.toolkit", "INFO");
		properties.put("log4j.category.com.ikokoon.persistence", "INFO");
		properties.put("log4j.category.com.ikokoon.instrumentation.process", "INFO");
		properties.put("log4j.category.com.ikokoon.instrumentation.coverage", "INFO");
		properties.put("log4j.category.com.ikokoon.instrumentation.complexity", "INFO");
		properties.put("log4j.category.com.ikokoon.instrumentation.dependency", "INFO");
		properties.put("log4j.category.com.ikokoon.instrumentation.profiling", "INFO	");
		return properties;
	}

}
