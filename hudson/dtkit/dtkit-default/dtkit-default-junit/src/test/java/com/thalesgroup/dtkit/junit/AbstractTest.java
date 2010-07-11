package com.thalesgroup.dtkit.junit;

import com.thalesgroup.dtkit.util.converter.ConvertUtil;
import com.thalesgroup.dtkit.util.validator.ValidatorError;
import com.thalesgroup.dtkit.metrics.api.InputMetricXSL;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Assert;
import org.junit.Before;

import java.io.File;

public class AbstractTest {

    @Before
    public void setUp() {
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setNormalizeWhitespace(true);
        XMLUnit.setIgnoreComments(true);
    }


    public void convertAndValidate(Class<? extends InputMetricXSL> classType, String inputXMLPath, String expectedResultPath) throws Exception {
        InputMetricXSL inputMetricXSL = classType.newInstance();
        File outputXMLFile = File.createTempFile("result", "xml");
        File inputXMLFile = new File(this.getClass().getResource(inputXMLPath).toURI());

        //The input file must be valid
        boolean inputResult = inputMetricXSL.validateInputFile(inputXMLFile);
        for (ValidatorError validatorError:inputMetricXSL.getInputValidationErrors()){
            System.out.println(validatorError);
        }
        Assert.assertTrue(inputResult);

        ConvertUtil.convert(inputMetricXSL.getClass(), inputMetricXSL.getXslName(), inputXMLFile, outputXMLFile);
        Diff myDiff = new Diff(XSLUtil.readXmlAsString(new File(this.getClass().getResource(expectedResultPath).toURI())), XSLUtil.readXmlAsString(outputXMLFile));
        Assert.assertTrue("XSL transformation did not work" + myDiff, myDiff.similar());

        //The generated output file must be valid
        boolean outputResult = inputMetricXSL.validateOutputFile(outputXMLFile);
        for (ValidatorError validatorError:inputMetricXSL.getOutputValidationErrors()){
            System.out.println(validatorError);
        }
        Assert.assertTrue(outputResult);

        outputXMLFile.deleteOnExit();
    }

}
