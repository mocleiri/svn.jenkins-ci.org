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


@XmlType(name = "purecoverage", namespace = "tusar")
@InputMetric
public class PureCoverage extends InputMetricOther {

    @Override
    public InputType getToolType() {
        return InputType.COVERAGE;
    }

    @Override
    public String getToolName() {
        return "Purecoverage";
    }

    @Override
    public String getToolVersion() {
        return "7.0";
    }

    @Override
    public boolean isDefault() {
        return true;
    }


    public String getXslName() {
        return "purecoverage-7.0-to-tusar-10.xsl";
    }


    @Override
    public OutputMetric getOutputFormatType() {
        return TusarModel.OUTPUT_TUSAR_10_0;
    }

    @Override
    public void convert(File inputFile, File outFile, Map<String, Object> params) throws ConversionException {
        try {
            File inputFile2 = new File(inputFile.getAbsolutePath().replace(".txt", ".xml"));//targetFile
            PureCoverageParser.parse(inputFile.getAbsolutePath(), inputFile2.getAbsolutePath());
            StreamSource ss = new StreamSource(this.getClass().getResourceAsStream(getXslName()));
            ConversionService conversion = ConversionServiceFactory.getInstance();
            conversion.convert(ss, inputFile2, outFile);
        } catch (Exception e) {
            throw new ConversionException("conversion happening..." + e);
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

