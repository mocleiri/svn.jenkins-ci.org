package com.thalesgroup.dtkit.tusar;

import com.thalesgroup.dtkit.util.converter.ConversionService;
import org.custommonkey.xmlunit.Diff;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.util.HashMap;
import java.util.Map;


public class WithParam extends AbstractTest {

    @Test
    public void testcase1() throws Exception {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("myParameter", "myvalue");

        ConversionService conversionService = new ConversionService();
        File outputXMLFile = File.createTempFile("result", "xml");
        conversionService.convert(
                new StreamSource(this.getClass().getResourceAsStream("withParam/withparam.xsl")),
                new File(this.getClass().getResource("withParam/input.xml").toURI()),
                outputXMLFile,
                params);


        Diff myDiff = new Diff(XSLUtil.readXmlAsString(new File(this.getClass().getResource("withParam/result.xml").toURI())), XSLUtil.readXmlAsString(outputXMLFile));
        Assert.assertTrue("XSL transformation did not work" + myDiff, myDiff.similar());
    }
}
