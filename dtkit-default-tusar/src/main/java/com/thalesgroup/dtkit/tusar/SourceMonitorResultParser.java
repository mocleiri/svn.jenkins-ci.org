package com.thalesgroup.dtkit.tusar;

import com.thalesgroup.dtkit.tusar.model.FileMetrics;
import com.thalesgroup.dtkit.tusar.model.MethodMetric;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.xpath.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import com.sun.xml.internal.ws.util.StringUtils;

/**
 * Parses a source monitor result file
 *
 * @author Mohamed Koundoussi
 */
public class SourceMonitorResultParser {

    //private final static Logger LOGGER = LoggerFactory.getLogger(SourceMonitorResultParser.class);

    private XPath xpath;
    private File baseDir;
    private Map<String, XPathExpression> expressions;

    public SourceMonitorResultParser() {
        XPathFactory factory = XPathFactory.newInstance();
        xpath = factory.newXPath();
        expressions = new HashMap<String, XPathExpression>();
    }

    /**
     * Parses the report.
     *
     * @param reportFile
     */
    public List<FileMetrics> parse(File directory, File reportFile) {
        this.baseDir = directory;
        List<FileMetrics> result = new ArrayList<FileMetrics>();

        try {
            URL reportURL = reportFile.toURI().toURL();
            InputSource source = new InputSource(reportURL.openStream());
            // source.
            XPathExpression expression = xpath.compile("//file");
            NodeList nodes = (NodeList) expression.evaluate(source, XPathConstants.NODESET);

            int length = nodes.getLength();

            for (int idxNode = 0; idxNode < length; idxNode++) {
                Element fileNode = (Element) nodes.item(idxNode);
                FileMetrics fileMetrics = createMetrics(fileNode);

                result.add(fileMetrics);
            }
        } catch (XPathExpressionException e) {
            // LOGGER.error("Unexpected error while parsing xml source monitor report", e);
        } catch (MalformedURLException e) {
            // LOGGER.error("Unexpected error while parsing xml source monitor report", e);
        } catch (IOException e) {
            // LOGGER.error("Unexpected error while parsing xml source monitor report", e);
        }
        return result;
    }

    /**
     * Creates the metrics for a file
     *
     * @param fileNode
     * @return
     */
    private FileMetrics createMetrics(Element fileNode) {
        String rawFileName = fileNode.getAttribute("file_name");
        String directoryPath = "";
        Element projectElement = (Element) fileNode.getParentNode().getParentNode().getParentNode().getParentNode();
        NodeList projectDirElements = projectElement.getElementsByTagName("project_directory");
        if (projectDirElements.getLength() > 0) {
            Element projectDirElement = (Element) projectDirElements.item(0);
            directoryPath = projectDirElement.getTextContent();
        }


        int countMethods
                = getIntMetric(fileNode, "method_metrics/@method_count");

        FileMetrics result = new FileMetrics();


        String tmpPath = this.baseDir.getPath();
        if (!directoryPath.equals("")) {

            directoryPath = FilenameUtils.separatorsToUnix(directoryPath);
            // We only want to remove the ../ if it exists.
            if (StringUtils.startsWith(directoryPath, "../")) {
                tmpPath = StringUtils.substringAfter(directoryPath, "../");
            } else {
                tmpPath = directoryPath;
            }

        }

        File path = toFullPath(rawFileName, tmpPath);
        int countBlankLines = BlankLineCounter.countBlankLines(path);

        result.setSourcePath(path);
        result.setCountBlankLines(countBlankLines);

        result.setProjectDirectory(new File(directoryPath));


        List<MethodMetric> extractMethods = extractMethods(fileNode, path);
        for (MethodMetric methodMetric : extractMethods) {
            result.addMethod(methodMetric);
        }

        return result;
    }

    /**
     * Parses the method metrics
     *
     * @param fileNode
     * @return
     */
    private List<MethodMetric> extractMethods(Element fileNode, File file) {
        List<MethodMetric> result = new ArrayList<MethodMetric>();
        // We extract the method metrics
        try {
            NodeList methodNodes = (NodeList) xpath.evaluate("method_metrics/method", fileNode, XPathConstants.NODESET);
            int countNodes = methodNodes.getLength();
            // We extract all the methods metrics
            for (int idxMethod = 0; idxMethod < countNodes; idxMethod++) {
                Element methodNode = (Element) methodNodes.item(idxMethod);
                MethodMetric methodMetrics = generateMethod(methodNode, file);
                result.add(methodMetrics);
            }
        } catch (XPathExpressionException e) {
            //LOGGER.error("Unexpected error while parsing xml source monitor report", e);
        }
        return result;
    }

    /**
     * Generates the metrics for a method
     *
     * @param methodNode
     * @param file
     * @return
     */
    private MethodMetric generateMethod(Element methodNode, File file) {
        String rawName = methodNode.getAttribute("name");
        String className;
        String methodName;

        boolean isAccessor = false;
        if (rawName.endsWith(".get()") || rawName.endsWith(".set()")) {
            isAccessor = true;
            String fullPropertyName = StringUtils.substringBeforeLast(rawName, ".");
            String propertyName = StringUtils.substringAfterLast(fullPropertyName, ".");
            className = StringUtils.substringBeforeLast(fullPropertyName, ".");
            methodName = StringUtils.removeEnd(StringUtils.substringAfterLast(rawName, "."), "()") + "_" + propertyName + "()";
        } else {
            methodName = StringUtils.substringAfterLast(rawName, ".");
            className = StringUtils.substringBeforeLast(rawName, ".");
        }
        int complexity = getIntElement(methodNode, "complexity");

        MethodMetric methodMetrics = new MethodMetric();
        methodMetrics.setClassName(className);
        methodMetrics.setFile(file);
        methodMetrics.setMethodName(methodName);
        methodMetrics.setComplexity(complexity);

        methodMetrics.setAccessor(isAccessor);
        return methodMetrics;
    }

    /**
     * Gets an element content as an integer
     */
    private int getIntAttribute(Element element, String attributeName) {
        String value = getElementAttribute(element, attributeName);
        int result = convertToInteger(value);
        return result;
    }

    /**
     * Gets an element content as an integer
     */
    private int getIntElement(Element element, String attributeName) {
        Element subElement = getSubElement(element, attributeName);
        if (subElement == null) {
            return 0;
        }
        String value = subElement.getTextContent();
        int result = convertToInteger(value);
        return result;
    }

    /**
     * @param value
     * @return
     */
    private int convertToInteger(String value) {
        if (value == null) {
            return 0;
        }
        int result = 0;
        try {
            if (value != null) {
                // We need a double here since source monitor has sometime a
                // strange
                // behaviour
                result = (int) Double.parseDouble(value);
            }
        } catch (NumberFormatException nfe) {
            //LOGGER.error("Error while parsing source monitor measure " + value, nfe);
        }
        return result;
    }

    /**
     * Gets the attribute of an element, having the given name.
     *
     * @param element       the element that contains the attribute
     * @param attributeName
     * @return
     */
    private String getElementAttribute(Element element, String attributeName) {
        if (element == null) {
            return null;
        }
        String value = element.getAttribute(attributeName);
        return value;
    }

    /**
     * Gets the first sub element having the given name, if it exists.
     *
     * @param element     the node on which the element is sought
     * @param elementName the element name
     * @return
     */
    private Element getSubElement(Element element, String elementName) {
        if (element == null) {
            return null;
        }
        NodeList subElements = element.getElementsByTagName(elementName);
        if (subElements.getLength() == 0) {

            return null;
        }
        return (Element) subElements.item(0);
    }


    private File toFullPath(String rawFileName, String path) {
        File file;
        try {
            file = new File(path, rawFileName).getCanonicalFile();
        } catch (IOException e) {
            file = new File(path, rawFileName);
        }
        return file;
    }

    /**
     * Gets a metric as an integer
     *
     * @param node
     * @param path
     * @return
     */
    public int getIntMetric(Node node, String path) {
        String value = getAttributeMetric(node, path);
        int result = 0;
        try {
            if (value != null) {
                // We need a double here since source monitor has sometime a
                // strange
                // behaviour
                result = (int) Double.parseDouble(value);
            }
        } catch (NumberFormatException nfe) {
            // Nothing
        }
        return result;
    }

    /**
     * Gets a metric as a double.
     *
     * @param node
     * @param path
     * @return
     */
    public double getDoubleMetric(Node node, String path) {
        String value = getAttributeMetric(node, path);
        double result = 0;
        try {
            if (value != null) {
                result = Double.parseDouble(value);
            }
        } catch (NumberFormatException nfe) {
            // Nothing
        }
        return result;
    }

    public String getAttributeMetric(Node node, String path) {
        try {
            XPathExpression expression = expressions.get(path);
            if (expression == null) {
                expression = xpath.compile(path);
                expressions.put(path, expression);
            }
            String result = expression.evaluate(node);
            return result;
        } catch (XPathExpressionException e) {
            // Nothing
        }
        return null;
    }
}
