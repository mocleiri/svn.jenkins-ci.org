/*******************************************************************************
 * Copyright (c) 2010 Thales Corporate Services SAS                             *
 * Author : Gregory Boissinot, Guillaume Tanier                                 *
 *                                                                              *
 * Permission is hereby granted, free of charge, to any person obtaining a copy *
 * of this software and associated documentation files (the "Software"), to deal*
 * in the Software without restriction, including without limitation the rights *
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell    *
 * copies of the Software, and to permit persons to whom the Software is        *
 * furnished to do so, subject to the following conditions:                     *
 *                                                                              *
 * The above copyright notice and this permission notice shall be included in   *
 * all copies or substantial portions of the Software.                          *
 *                                                                              *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR   *
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,     *
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE  *
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER       *
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,*
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN    *
 * THE SOFTWARE.                                                                *
 *******************************************************************************/

package com.thalesgroup.dtkit.util.converter;

import net.sf.saxon.s9api.*;

import javax.xml.transform.stream.StreamSource;
import java.io.*;

public class ConversionService implements Serializable {


    /**
     * Launches an XSLT conversion from a source to an OutputStream.
     *
     * @param xslFile   the xsl file
     * @param inputFile the input file
     * @param outFile   the output file
     * @throws ConversionException the convert exception
     */
    public void convert(File xslFile, File inputFile, File outFile) throws ConversionException {
        try {
            convert(new StreamSource(new FileReader(xslFile)), inputFile, outFile);
        } catch (FileNotFoundException e) {
            throw new ConversionException("Can't find " + xslFile);
        }
    }


    /**
     * Launches an XSLT conversion from a source to an OutputStream.
     *
     * @param xslNamespace the xsl namespace
     * @param xslName      the xsl name
     * @param inputFile    the input file
     * @param outFile      the output file
     * @throws ConversionException the convert exception
     */
    public void convert(Class xslNamespace, String xslName, File inputFile, File outFile) throws ConversionException {
        convert(new StreamSource(xslNamespace.getResourceAsStream(xslName)), inputFile, outFile);
    }

    /**
     * Launches an XSLT conversion from a source to an OutputStream.
     * This methods uses the net.sf.saxon packages.
     *
     * @param xslSource the source of the xsl
     * @param inputFile the input file
     * @param outFile   the output file
     * @throws ConversionException the convert exception
     */
    private void convert(StreamSource xslSource, File inputFile, File outFile) throws ConversionException {

        try {

            // create the conversion processor with a Xslt compiler
            Processor processor = new Processor(false);
            XsltCompiler compiler = processor.newXsltCompiler();

            // compile and load the XSL file
            XsltExecutable xsltExecutable = compiler.compile(xslSource);
            XsltTransformer xsltTransformer = xsltExecutable.load();

            // create the input
            XdmNode xdmNode = processor.newDocumentBuilder().build(inputFile);

            // create the output with its options
            Serializer out = new Serializer();
            out.setOutputProperty(Serializer.Property.METHOD, "xml");
            out.setOutputProperty(Serializer.Property.INDENT, "yes");
            out.setOutputStream(new FileOutputStream(outFile));

            // run the conversion
            xsltTransformer.setInitialContextNode(xdmNode);
            xsltTransformer.setDestination(out);
            xsltTransformer.transform();
        }
        catch (FileNotFoundException fne) {
            throw new ConversionException("Error to convert - A file not found", fne);
        }
        catch (SaxonApiException sae) {
            throw new ConversionException("Error to convert the input XML document", sae);
        }
    }

}