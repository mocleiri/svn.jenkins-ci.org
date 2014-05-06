package com.thalesgroup.dtkit.tusar;

import com.thalesgroup.dtkit.metrics.model.InputMetricOther;
import com.thalesgroup.dtkit.metrics.model.InputType;
import com.thalesgroup.dtkit.processor.InputMetric;
import com.thalesgroup.dtkit.tusar.model.TusarModel;
import com.thalesgroup.dtkit.util.converter.ConversionException;
import com.thalesgroup.dtkit.util.validator.ValidationError;
import com.thalesgroup.dtkit.util.validator.ValidationException;

import java.io.File;
import java.util.List;
import java.util.Map;

@InputMetric
public class Gnatcheck extends InputMetricOther {

    @Override
    public InputType getToolType() {
        return InputType.VIOLATION;
    }

    @Override
    public String getToolName() {
        return "Gnatcheck";
    }

    @Override
    public String getToolVersion() {
        return "6.2.1";
    }

    @Override
    public boolean isDefault() {
        return true;
    }

    /**
     * Convert an input file to an output file
     * Give your conversion process
     * Input and Output files are relatives to the filesystem where the process is executed on (like Hudson agent)
     *
     * @param inputFile the input file to convert
     * @param outFile   the output file to convert
     * @param params    the xsl parameters
     * @throws com.thalesgroup.dtkit.util.converter.ConversionException an application Exception to throw when there is an error of conversion
     *                                                                  The exception is catch by the API client (as Hudson plugin)
     */
    @Override
    public void convert(File inputFile, File outFile, Map<String, Object> params) throws ConversionException {
        GnatcheckParser parser = new GnatcheckParser();
        parser.convert(inputFile, outFile);
    }

    /*
     *  Gives the validation process for the input file
     *
     * @return true if the input file is valid, false otherwise
     */
    @Override
    public boolean validateInputFile(File inputXMLFile) throws ValidationException {
        GnatcheckParser parser = new GnatcheckParser();
        parser.validateInputFile(inputXMLFile);
        return true;
    }

    /*
     *  Gives the validation process for the output file
     *
     * @return true if the input file is valid, false otherwise
     */
    @Override
    public boolean validateOutputFile(File inputXMLFile) throws ValidationException {
        List<ValidationError> errors = TusarModel.OUTPUT_TUSAR_10_0.validate(inputXMLFile);
        this.setOutputValidationErrors(errors);
        return errors.isEmpty();
    }

}
