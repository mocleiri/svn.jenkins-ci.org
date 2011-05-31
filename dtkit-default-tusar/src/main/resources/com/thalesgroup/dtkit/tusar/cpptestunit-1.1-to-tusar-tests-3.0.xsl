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

                <xsl:apply-templates select="ResultsSession/Exec"></xsl:apply-templates>

            </xsl:element>
        </tusar:tusar>
    </xsl:template>

    <xsl:template match="Exec">
        <tests:testsuite name="{Summary/Projects/Project/@name}" time="0" tests="{Summary/Projects/Project/@testCases}"
                         failures="{Summary/Projects/Project/@fail}">
            <xsl:apply-templates select="*"/>
        </tests:testsuite>
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
        <!-- AM : getting the @fsPath corresponding to the @locFile (if it exists)-->
        <xsl:variable name="locFile">
            <xsl:value-of select="@locFile"/>
        </xsl:variable>
        <xsl:variable name="fsPath">
            <xsl:choose>
                <xsl:when test="distinct-values(/ResultsSession/Locations/Loc[@loc=$locFile]/@fsPath)">
                    <xsl:value-of select="distinct-values(/ResultsSession/Locations/Loc[@loc=$locFile]/@fsPath)"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$locFile"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <!-- AM : changing the @locFile by @fsPath in tag Locations/Loc-->
        <!--<tests:testcase fulltestname="{@locFile}" testname="{@testName}" time="0">-->
        <tests:testcase fulltestname="{$fsPath}" testname="{@testName}" time="0">
            <xsl:apply-templates select="Thr"/>
        </tests:testcase>
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

    <xsl:template match="text()|@*"/>
</xsl:stylesheet>