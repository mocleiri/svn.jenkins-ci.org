package com.thalesgroup.dtkit.tusar;

import com.thalesgroup.dtkit.metrics.model.InputMetricOther;
import com.thalesgroup.dtkit.metrics.model.InputType;
import com.thalesgroup.dtkit.metrics.model.OutputMetric;
import com.thalesgroup.dtkit.processor.InputMetric;
import com.thalesgroup.dtkit.tusar.model.TusarModel;
import com.thalesgroup.dtkit.util.converter.ConversionException;
import com.thalesgroup.dtkit.util.converter.ConversionService;
import com.thalesgroup.dtkit.util.converter.ConversionServiceFactory;
import com.thalesgroup.dtkit.util.validator.ValidationException;

import javax.xml.bind.annotation.XmlType;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.util.Map;


@XmlType(name = "fitnesse", namespace = "tusar")
@InputMetric
public class Fitnesse extends InputMetricOther {

    @Override
    public String getToolName() {
        return "fitnesse";
    }

    @Override
    public String getToolVersion() {
        return "2013xxxx";
    }

    @Override
    public InputType getToolType() {
        return InputType.TEST;
    }


    @Override
    public boolean isDefault() {
        return true;
    }

    private String getXslName() {
        return "fitnesse-junit-3-to-tusar-6.xsl";
    }

    private String getFitnesseXsl() {
        return "fitnesse2junit.xsl";
    }

    @Override
    public OutputMetric getOutputFormatType() {
        return TusarModel.OUTPUT_TUSAR_6_0;
    }


    @Override
    public void convert(File inputFile, File outFile, Map<String, Object> params) throws ConversionException {
        try {
            File inputFile2 = new File(inputFile.getAbsolutePath().replace(".xml", "-juform.xml"));
            StreamSource ss = new StreamSource(this.getClass().getResourceAsStream(getFitnesseXsl()));
            ConversionService conversion = ConversionServiceFactory.getInstance();
            conversion.convert(ss, inputFile, inputFile2);
            ss = new StreamSource(this.getClass().getResourceAsStream(getXslName()));
            conversion.convert(ss, inputFile2, outFile);
        } catch (Exception e) {
            throw new ConversionException("conversion happening..." + e);
        }
    }

    @Override
    public boolean validateInputFile(File inputXMLFile) throws ValidationException {
        return true;
    }

    @Override
    public boolean validateOutputFile(File arg0) throws ValidationException {
        return true;
    }

}

