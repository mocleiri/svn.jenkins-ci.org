<?xml version="1.0" encoding="UTF-8"?>
<!--
/*******************************************************************************
* Copyright (c) 2010-2011 Thales Corporate Services SAS                        *
* Author : Gregory Boissinot, Joel Forner, Aravindan Mahendran                 *
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
-->
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:tests="http://www.thalesgroup.com/tusar/tests/v3"
                xmlns:xs="http://www.w3.org/2001/XMLSchema">
    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>

    <xsl:template match="/">

        <tusar:tusar
                xmlns:t="http://www.thalesgroup.com/tusar/tests/v3"
                xmlns:tusar="http://www.thalesgroup.com/tusar/v3"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                version="3.0">

            <xsl:element name="tusar:tests">

                <xsl:attribute name="toolname">cpptest</xsl:attribute>
                <xsl:attribute name="version">7.3</xsl:attribute>

                <tests:testsuite name="{ResultsSession/Exec/Summary/Projects/Project/@name}" time="0" tests="{ResultsSession/Exec/Summary/Projects/Project/@testCases}"
                         failures="{ResultsSession/Exec/Summary/Projects/Project/@fail}">
                    <xsl:apply-templates select="ResultsSession/Exec"></xsl:apply-templates>
                    <xsl:apply-templates select="ResultsSession/ExecutedTestsDetails"></xsl:apply-templates>
                </tests:testsuite>
            </xsl:element>
        </tusar:tusar>
    </xsl:template>

    <xsl:template match="Exec">
        
            <xsl:apply-templates select="*"/>
        
    </xsl:template>

    <xsl:template match="Goals">
        <tests:properties>
            <xsl:apply-templates select="Goal"/>
        </tests:properties>
    </xsl:template>

    <xsl:template match="Goal">
        <tests:property name="{@name}" value="{@type}"/>
    </xsl:template>

    <xsl:template match="ExecViols">
        <xsl:apply-templates select="ExecViol"/>
    </xsl:template>

    <xsl:template match="ExecViol">
        <xsl:variable name="fullTestName"><xsl:value-of select="/ResultsSession/Exec/Summary/Projects/Project/@name" />TusarTestSuite</xsl:variable>
        
        <xsl:if test="@cat!=6">
            <tests:testcase testname="{@testName}" fulltestname="{$fullTestName}" time="0">
                <xsl:apply-templates select="Thr"/>
            </tests:testcase>
        </xsl:if>

    </xsl:template>

    <xsl:template match="Thr">
        <xsl:apply-templates select="ThrPart"/>
    </xsl:template>

    <xsl:template match="ThrPart">
        <tests:failure type="{@clName}" message="{@detMsg}"/>
        <tests:system-err>
            <xsl:text>Trace </xsl:text>
            <xsl:apply-templates select="Trace"/>
        </tests:system-err>
    </xsl:template>

    <xsl:template match="Trace">
        <xsl:text>Line :</xsl:text>
        <xsl:value-of select="@ln"/>
        <xsl:text>    File :</xsl:text>
        <xsl:value-of select="@fileName"/>
    </xsl:template>
    
    <!--New way to convert test results-->
    <xsl:template match="ExecutedTestsDetails">
        <xsl:apply-templates select="Total"/>
    </xsl:template>
    
    <xsl:template match="Total">
        <xsl:apply-templates select="Project"/>
    </xsl:template>
    
    <xsl:template match="Project">
        <xsl:apply-templates select="TestSuite"/>
    </xsl:template>
    
    <xsl:template match="TestSuite">
        <xsl:apply-templates select="*"/>
    </xsl:template>
    
    <xsl:template match="Test">
        <xsl:variable name="fullTestName"><xsl:value-of select="/ResultsSession/Exec/Summary/Projects/Project/@name" />TusarTestSuite</xsl:variable>
        <xsl:if test="@pass=1">
            <tests:testcase testname="{@name}" fulltestname="{$fullTestName}" time="0"/>
        </xsl:if>
    </xsl:template>

    <xsl:template match="text()|@*"/>
</xsl:stylesheet>
<?xml version="1.0" encoding="UTF-8"?>
<!--
/*******************************************************************************
* Copyright (c) 2010-2011 Thales Corporate Services SAS                        *
* Author : Gregory Boissinot, Joel Forner, Aravindan Mahendran                 *
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
-->
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:tests="http://www.thalesgroup.com/tusar/tests/v3"
                xmlns:xs="http://www.w3.org/2001/XMLSchema">
    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>

    <xsl:template match="/">

        <tusar:tusar
                xmlns:t="http://www.thalesgroup.com/tusar/tests/v3"
                xmlns:tusar="http://www.thalesgroup.com/tusar/v3"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                version="3.0">

            <xsl:element name="tusar:tests">

                <xsl:attribute name="toolname">cpptest</xsl:attribute>
                <xsl:attribute name="version">7.3</xsl:attribute>

                <tests:testsuite name="{ResultsSession/Exec/Summary/Projects/Project/@name}" time="0" tests="{ResultsSession/Exec/Summary/Projects/Project/@testCases}"
                         failures="{ResultsSession/Exec/Summary/Projects/Project/@fail}">
                    <xsl:apply-templates select="ResultsSession/Exec"></xsl:apply-templates>
                    <xsl:apply-templates select="ResultsSession/ExecutedTestsDetails"></xsl:apply-templates>
                </tests:testsuite>
            </xsl:element>
        </tusar:tusar>
    </xsl:template>

    <xsl:template match="Exec">
        
            <xsl:apply-templates select="*"/>
        
    </xsl:template>

    <xsl:template match="Goals">
        <tests:properties>
            <xsl:apply-templates select="Goal"/>
        </tests:properties>
    </xsl:template>

    <xsl:template match="Goal">
        <tests:property name="{@name}" value="{@type}"/>
    </xsl:template>

    <xsl:template match="ExecViols">
        <xsl:apply-templates select="ExecViol"/>
    </xsl:template>

    <xsl:template match="ExecViol">
        <xsl:variable name="fullTestName"><xsl:value-of select="/ResultsSession/Exec/Summary/Projects/Project/@name" />TusarTestSuite</xsl:variable>
        
        <xsl:if test="@cat!=6">
            <tests:testcase testname="{@testName}" fulltestname="{$fullTestName}" time="0">
                <xsl:apply-templates select="Thr"/>
            </tests:testcase>
        </xsl:if>

    </xsl:template>

    <xsl:template match="Thr">
        <xsl:apply-templates select="ThrPart"/>
    </xsl:template>

    <xsl:template match="ThrPart">
        <tests:failure type="{@clName}" message="{@detMsg}"/>
        <tests:system-err>
            <xsl:text>Trace </xsl:text>
            <xsl:apply-templates select="Trace"/>
        </tests:system-err>
    </xsl:template>

    <xsl:template match="Trace">
        <xsl:text>Line :</xsl:text>
        <xsl:value-of select="@ln"/>
        <xsl:text>    File :</xsl:text>
        <xsl:value-of select="@fileName"/>
    </xsl:template>
    
    <!--New way to convert test results-->
    <xsl:template match="ExecutedTestsDetails">
        <xsl:apply-templates select="Total"/>
    </xsl:template>
    
    <xsl:template match="Total">
        <xsl:apply-templates select="Project"/>
    </xsl:template>
    
    <xsl:template match="Project">
        <xsl:apply-templates select="TestSuite"/>
    </xsl:template>
    
    <xsl:template match="TestSuite">
        <xsl:apply-templates select="*"/>
    </xsl:template>
    
    <xsl:template match="Test">
        <xsl:variable name="fullTestName"><xsl:value-of select="/ResultsSession/Exec/Summary/Projects/Project/@name" />TusarTestSuite</xsl:variable>
        <xsl:if test="@pass=1">
            <tests:testcase testname="{@name}" fulltestname="{$fullTestName}" time="0"/>
        </xsl:if>
    </xsl:template>

    <xsl:template match="text()|@*"/>
</xsl:stylesheet>
