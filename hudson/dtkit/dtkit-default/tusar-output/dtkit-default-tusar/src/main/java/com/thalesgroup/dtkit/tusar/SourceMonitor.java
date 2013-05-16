package com.thalesgroup.dtkit.tusar;


import com.thalesgroup.dtkit.metrics.model.InputMetricOther;
import com.thalesgroup.dtkit.metrics.model.InputType;
import com.thalesgroup.dtkit.metrics.model.OutputMetric;
import com.thalesgroup.dtkit.processor.InputMetric;
import com.thalesgroup.dtkit.tusar.model.FileMetrics;
import com.thalesgroup.dtkit.tusar.model.ProjectMetrics;
import com.thalesgroup.dtkit.tusar.model.TusarModel;
import com.thalesgroup.dtkit.util.converter.ConversionException;
import com.thalesgroup.dtkit.util.converter.ConversionService;
import com.thalesgroup.dtkit.util.converter.ConversionServiceFactory;
import com.thalesgroup.dtkit.util.validator.ValidationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.bind.annotation.XmlType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.List;
import java.util.Map;


/**
 * @author Mohamed Koundoussi
 */
@XmlType(name = "sourceMonitor", namespace = "tusar")
@InputMetric
public class SourceMonitor extends InputMetricOther {


    @Override
    public InputType getToolType() {
        return InputType.MEASURE;
    }

    @Override
    public String getToolName() {
        return "SourceMonitor";
    }

    @Override
    public String getToolVersion() {
        return "3.2";
    }

    @Override
    public boolean isDefault() {
        return true;
    }

    @Override
    public OutputMetric getOutputFormatType() {
        return TusarModel.OUTPUT_TUSAR_8_0;

    }

    @Override
    public void convert(File inputFile, File outFile, Map<String, Object> params) throws ConversionException {
        try {
            SourceMonitorResultParser tool = new SourceMonitorResultParser();
            File inputFile2 = new File(inputFile.getParentFile().getPath() + "/addings.xml");
            System.out.println("tool obtained...");
            Document inputElement = getDocumentElement(inputFile);
            System.out.println("document obtained...");
            ProjectMetrics pm = new ProjectMetrics();

            List<FileMetrics> files = tool.parse(inputFile.getParentFile(), inputFile);
            System.out.println("files obtained...");
            for (FileMetrics _fm : files) {
                pm.addFile(_fm);
            }
            System.out.println("pm feeded...");

            inputElement = appendAddings(pm, files, inputElement);
            System.out.println("appendadding finished...");
            DOM2File(inputElement, inputFile2);
            StreamSource ss = new StreamSource(this.getClass().getResourceAsStream(getXslName()));
            ConversionService conversion = ConversionServiceFactory.getInstance();
            conversion.convert(ss, inputFile2, outFile);
            System.out.println("file streamed out");
        } catch (Exception e) {
            throw new ConversionException("conversion happening..." + e);
        }
    }

    private String getXslName() {
        return "sourcemonitor-3.2-to-tusar-8.0-1-0.xsl";
    }

    private Document appendAddings(ProjectMetrics pm, List<FileMetrics> files, Document doc) throws ConversionException {
        Element addings = doc.createElement("addings");
        Element project = doc.createElement("_project");

        Element class_complexity_distrib = doc.createElement("class_complexity_distribution");
        class_complexity_distrib.setTextContent(pm.getClassComplexityDistribution());
        project.appendChild(class_complexity_distrib);

        Element function_complexity_distrib = doc.createElement("function_complexity_distribution");
        function_complexity_distrib.setTextContent(pm.getMethodComplexityDistribution());
        project.appendChild(function_complexity_distrib);

        Element _accessors = doc.createElement("accessors");
        _accessors.setTextContent(String.valueOf(pm.getCountAccessors()));
        project.appendChild(_accessors);

        Element _blanklines = doc.createElement("blankLines");
        _blanklines.setTextContent(String.valueOf(pm.getCountBlankLines()));
        project.appendChild(_blanklines);

        addings.appendChild(project);
        Element _files = doc.createElement("_files");

        for (FileMetrics fm : files) {
            Element file = doc.createElement("file");
            file.setAttribute("name", fm.getSourcePath().getPath().substring(fm.getProjectDirectory().getPath().length() + 1));

            Element blank = doc.createElement("blankLines");
            System.out.println(fm.getCountBlankLines());
            blank.setTextContent(String.valueOf(fm.getCountBlankLines()));
            file.appendChild(blank);

            Element accessors = doc.createElement("accessors");
            accessors.setTextContent(String.valueOf(fm.getCountAccessors()));
            file.appendChild(accessors);

            _files.appendChild(file);


        }

        addings.appendChild(_files);
        doc.getDocumentElement().insertBefore(addings, doc.getDocumentElement().getFirstChild());

        return doc;

    }

    private Document getDocumentElement(File inputFile) throws ConversionException {
        Document doc;
        try {
            URL reportUrl = inputFile.toURI().toURL();
            InputSource source = new InputSource(reportUrl.openStream());
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(source);

        } catch (Exception ex) {
            throw new ConversionException(ex);
        }
        return doc;
    }

    private Element getDocumentElement(String addings, DocumentBuilder db) throws ConversionException {
        Element e;
        try {
            Reader xmlAddings = new StringReader(addings);
            Document dom = db.parse(new InputSource(xmlAddings));
            e = dom.getDocumentElement();
        } catch (Exception ex) {
            throw new ConversionException(ex);
        }
        return e;
    }

    private void DOM2File(Document e, File outFile) throws ConversionException {
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = null;
        try {
            transformer = tFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(e);
            StreamResult result = new StreamResult(outFile);
            transformer.transform(source, result);
        } catch (Exception ex) {
            throw new ConversionException(ex);
        }

    }

    @Override
    public boolean validateInputFile(File arg0) throws ValidationException {
        return true;
    }

    @Override
    public boolean validateOutputFile(File arg0) throws ValidationException {
        return true;
    }

}