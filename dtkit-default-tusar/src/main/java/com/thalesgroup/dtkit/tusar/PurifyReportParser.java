package com.thalesgroup.dtkit.tusar;

import com.thalesgroup.dtkit.tusar.model.FilePurifyMetrics;
import com.thalesgroup.dtkit.util.converter.ConversionException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.xpath.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;

public class PurifyReportParser {

    private XPath xpath;
    private int numberOfErrors = 0;
    private int numberOfMemoryLeaks = 0;
    private int bytesLost = 0;

    private String str_filehead =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<tusar:tusar xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n" +
                    "             xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                    "             xmlns:measures=\"http://www.thalesgroup.com/tusar/measures/v7\"\n" +
                    "             xmlns:memory=\"http://www.thalesgroup.com/tusar/memory/v1\"\n" +
                    "             xmlns:tusar=\"http://www.thalesgroup.com/tusar/v11\"\n" +
                    "             version=\"11\">\n" +
                    "    <tusar:measures toolname=\"purify\">\n" +
                    "    	<measures:memory>\n";


    private String str_filetail =
            "    </measures:memory>\n" +
                    "  	</tusar:measures>\n" +
                    "	</tusar:tusar>";


    public PurifyReportParser() {
        XPathFactory factory = XPathFactory.newInstance();
        xpath = factory.newXPath();
    }

    public static void main(String[] args) {

        PurifyReportParser mrp = new PurifyReportParser();
        File inputFile = new File("C:\\Users\\GENIATIS\\.jenkins\\jobs\\helloPurify\\workspace\\PurifyPlusProject\\Purify\\my_junk.txt");
        File reportFile = new File(inputFile.getAbsolutePath().replace(".txt", "_txt") + ".xml");
        PurifyTextParser.parse(inputFile.getAbsolutePath(), reportFile.getAbsolutePath());
        HashMap<String, FilePurifyMetrics> map = new HashMap<String, FilePurifyMetrics>();
        try {
            /*mrp.parse(reportFile, map);*/
            mrp.convert(reportFile, new File("C:\\Users\\GENIATIS\\.jenkins\\jobs\\helloPurify\\workspace\\PurifyPlusProject\\Purify\\my_junk_tusar.xml"));
        } catch (Exception e) {
            throw new ConversionException(" parse exception :" + e.getMessage(), e);
        }
        System.out.println("projectBytesLost: " + mrp.getNumberOfBytesLost());
        System.out.println("projectErros: " + mrp.getNumberOfErrors());
        System.out.println("projectleaks: " + mrp.getNumberOfMemoryLeaks());
        for (String x : map.keySet()) {
            System.out.println(x + "= " + map.get(x).getFilename() + " * " + map.get(x).getNumberOfBytesLost() + " * " + map.get(x).getNumberOfErrors() + " * " + map.get(x).getNumberOfMemoryLeaks() + " * " + map.get(x).getSourcePath() + " ;\n");
        }
    }

    public int getNumberOfErrors() {
        return numberOfErrors;
    }

    public int getNumberOfMemoryLeaks() {
        return numberOfMemoryLeaks;
    }

    public int getNumberOfBytesLost() {
        return bytesLost;
    }

    /**
     * Parses the report.
     *
     * @param reportFile
     */
    public void parse(File reportFile, Map<String, FilePurifyMetrics> hashMap)
            throws XPathExpressionException, MalformedURLException, IOException {
        URL reportURL = reportFile.toURI().toURL();
        InputSource source = new InputSource(reportURL.openStream());

        XPathExpression rootExpression = xpath.compile("//purify");
        NodeList rootNode = (NodeList) rootExpression.evaluate(source, XPathConstants.NODESET);

        // We only have the one <purify> tag.
        Element rootElement = (Element) rootNode.item(0);

        // Get number of errors <problem> tags.
        XPathExpression otherErrorsExpression = xpath.compile("//problem");
        NodeList otherErrorsNodes = (NodeList) otherErrorsExpression.evaluate(rootElement, XPathConstants.NODESET);
        this.numberOfErrors = otherErrorsNodes.getLength();

        // Run through our <problem> tags and populate our list.
        for (int i = 0; i < numberOfErrors; i++) {
            Element problemElement = (Element) otherErrorsNodes.item(i);
            createOtherErrorsMetrics(problemElement, hashMap);
        }

        // Get number of memory leaks.
        XPathExpression memoryLeaksExpression = xpath.compile("//memory_leak");
        NodeList memoryLeaksNodes = (NodeList) memoryLeaksExpression.evaluate(rootElement, XPathConstants.NODESET);
        this.numberOfMemoryLeaks = memoryLeaksNodes.getLength();

        for (int i = 0; i < numberOfMemoryLeaks; i++) {
            Element mlkElement = (Element) memoryLeaksNodes.item(i);
            String defaultFilename = mlkElement.getAttribute("executable");
            createMemoryLeakMetrics(mlkElement, hashMap, defaultFilename);
        }

        // Get number of bytes lost.
        XPathExpression bytesLostExpression = xpath.compile("//memory_leaks");
        NodeList bytesLostNodes = (NodeList) bytesLostExpression.evaluate(rootElement, XPathConstants.NODESET);
        // We only have the one <memory_leaks> tag.
        Element bytesLostFileNode = (Element) bytesLostNodes.item(0);
        this.bytesLost = Integer.parseInt(bytesLostFileNode.getAttribute("total_bytes"));
    }

    /**
     * Stores information about a Purify run in a file.
     *
     * @param fileNode <problem> tag
     * @param map      Map of filenames verses the metrics for that file
     */
    private void createOtherErrorsMetrics(Element fileNode, Map<String, FilePurifyMetrics> map) {
        try {
            XPathExpression stackTraceExpression = xpath.compile("stack_trace/function");
            NodeList stackTraceNodes = (NodeList) stackTraceExpression.evaluate(fileNode, XPathConstants.NODESET);
            // First node is the one we care about for now.
            Element stackTrace = (Element) stackTraceNodes.item(0);

            String filename = stackTrace.getAttribute("file");

            // Check to see if we have a match in our HashMap.
            FilePurifyMetrics fileMetrics = map.get(filename);

            if (null == fileMetrics) {
                fileMetrics = new FilePurifyMetrics();
            }

            // Populate the values.
            fileMetrics.addNumberOfErrors(1);
            fileMetrics.setFilename(filename);

            map.put(filename, fileMetrics);
        } catch (Exception e) {
            throw new ConversionException(" parse exception :" + e.getMessage(), e);
        }
    }

    /**
     * Stores information about a Purify run (MLKs) in a file.
     *
     * @param fileNode <problem> tag
     * @param map      Map of filenames verses the metrics for that file
     */
    private void createMemoryLeakMetrics(Element fileNode, Map<String, FilePurifyMetrics> map, String defaultFilename) {
        try {
            XPathExpression stackTraceExpression = xpath.compile("stack_trace/function");
            NodeList stackTraceNodes = (NodeList) stackTraceExpression.evaluate(fileNode, XPathConstants.NODESET);
            String filename = defaultFilename;

            // Check that we have some <function> tags:
            // We initially extract the filename tag from the <memory_leak executable="..."> tag
            // if we have a stack trace of source code we use that instead.
            // Most important thing is recording the correct numbers even if the drilldown does
            // not work.
            if (0 < stackTraceNodes.getLength()) {
                // First node is the one we care about for now.
                Element stackTrace = (Element) stackTraceNodes.item(0);
                filename = stackTrace.getAttribute("file");
            } else {
              /*LOGGER.info("Purify::createMemoryLeakMetrics() no stack trace for memory leak using default filename of: "
                  + defaultFilename);*/
            }

            // Check to see if we have a match in our HashMap.
            FilePurifyMetrics fileMetrics = map.get(filename);

            if (null == fileMetrics) {
                fileMetrics = new FilePurifyMetrics();
            }

            // Populate the values.
            fileMetrics.addNumberOfMemoryLeaks(1);
            // fileNode is equal to our <memory_leak> tag.
            fileMetrics.addNumberOfBytesLost(Integer.parseInt(fileNode.getAttribute("bytes_lost")));
            fileMetrics.setFilename(filename);

            map.put(filename, fileMetrics);
        } catch (Exception e) {
            throw new ConversionException(" parse exception :" + e.getMessage(), e);
        }
    }

    public void convert(File InputFile, File outFile) throws ConversionException {
        PurifyReportParser parser = new PurifyReportParser();
        HashMap<String, FilePurifyMetrics> hashMap = new HashMap<String, FilePurifyMetrics>();

        try {
            parser.parse(InputFile, hashMap);

            Formatter output; // object used to output text to file
            output = new Formatter(outFile);
            output.format(str_filehead);
            String projectContent =
                    "<memory:resource type=\"PROJECT\" value=\"\" >" +
                            "<memory:measure key=\"memory_errors\" value=\"" + parser.getNumberOfErrors() + "\"/>" +
                            "<memory:measure key=\"memory_leaks\" value=\"" + parser.getNumberOfMemoryLeaks() + "\"/>" +
                            "<memory:measure key=\"bytes_lost\" value=\"" + parser.getNumberOfBytesLost() + "\"/>" +
                            "</memory:resource> ";
            output.format(projectContent);
            output.format(str_filetail);
            if (output != null)
                output.close();
        } catch (FileNotFoundException e) {
            throw new ConversionException("File Not Found Exception :" + e.getMessage(), e);
        } catch (Exception io) {
            throw new ConversionException(" parse exception :" + io.getMessage(), io);
        }
    }
}
