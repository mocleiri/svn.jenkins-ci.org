<?xml version="1.0" encoding="UTF-8"?>
<!--
/*******************************************************************************
* Copyright (c) 2009 Thales Corporate Services SAS                             *
* Author : Joel Forner                                                         *
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
                xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:fn="http://www.w3.org/2005/xpath-functions">
    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>

    <xsl:template match="/TestLog">

        <xsl:element name="tusar">
            <xsl:attribute name="xmlns_xsi">
                <xsl:text>http://www.w3.org/2001/XMLSchema-instance</xsl:text>
            </xsl:attribute>

            <xsl:attribute name="version">
                <xsl:text>1.0</xsl:text>
            </xsl:attribute>

            <xsl:element name="tests">
                <xsl:element name="testsuite">
                    <xsl:attribute name="tests">
                        <xsl:value-of select="count(//TestCase)"/>
                    </xsl:attribute>

                    <xsl:attribute name="errors">
                        <xsl:value-of select="count(//TestCase/FatalError)+count(//TestCase/Exception)"/>
                    </xsl:attribute>

                    <xsl:attribute name="failures">
                        <xsl:value-of select="count(//TestCase/Error)"/>
                    </xsl:attribute>

                    <xsl:attribute name="name">MergedTestSuite</xsl:attribute>

                    <xsl:attribute name="skipped">0</xsl:attribute>

                    <xsl:for-each select="//TestCase">
                        <xsl:call-template name="testCase"/>
                    </xsl:for-each>
                </xsl:element>
            </xsl:element>

        </xsl:element>
    </xsl:template>


    <xsl:template name="testCaseContent">
        <xsl:for-each select="child::*">
            <xsl:variable name="currElt" select="."/>
            <xsl:variable name="currEltName" select="name(.)"/>
            <xsl:choose>
                <xsl:when test="$currEltName='Error'">
                    <xsl:text>&#13;</xsl:text>
                    <xsl:text>[Error] - </xsl:text>
                    <xsl:value-of select="($currElt)"/>
                    <xsl:text>&#13;</xsl:text>
                    <xsl:text> == [File] - </xsl:text><xsl:value-of select="($currElt)/@file"/>
                    <xsl:text>&#13;</xsl:text>
                    <xsl:text> == [Line] - </xsl:text><xsl:value-of select="($currElt)/@line"/>
                    <xsl:text>&#13;</xsl:text>
                </xsl:when>

                <xsl:when test="$currEltName='FatalError' or $currEltName='Exception'">
                    <xsl:text>&#13;</xsl:text>
                    <xsl:text>[Exception] - </xsl:text>
                    <xsl:value-of select="($currElt)"/>
                    <xsl:text>&#13;</xsl:text>
                    <xsl:text> == [File] - </xsl:text><xsl:value-of select="($currElt)/@file"/>
                    <xsl:text>&#13;</xsl:text>
                    <xsl:text> == [Line] -</xsl:text><xsl:value-of select="($currElt)/@line"/>
                    <xsl:text>&#13;</xsl:text>
                </xsl:when>
            </xsl:choose>
        </xsl:for-each>
    </xsl:template>


    <xsl:template name="testCase">

        <xsl:variable name="curElt" select="."/>
        <xsl:variable name="suiteName" select="($curElt/parent::*)[1]/@name"/>
        <xsl:variable name="packageName" select="concat($suiteName, '.')"/>


        <xsl:element name="testcase">
            <xsl:variable name="elt" select="(child::*[position()=1])"/>
            <xsl:variable name="time" select="TestingTime"/>

            <xsl:attribute name="fulltestname">
                <xsl:value-of select="concat($packageName,  substring-before(($elt)/@file, '.'))"/>
            </xsl:attribute>

            <xsl:attribute name="testname">
                <xsl:value-of select="@name"/>
            </xsl:attribute>


            <xsl:attribute name="time">
                <xsl:value-of select="$time div 1000000"/>
            </xsl:attribute>

            <xsl:variable name="nbErrors" select="count(Error)"/>
            <xsl:variable name="nbFatalErrors" select="count(FatalError)+count(Exception)"/>

            <xsl:choose>
                <xsl:when test="$nbFatalErrors&gt;0">
                    <xsl:element name="error">
                        <xsl:call-template name="testCaseContent"/>
                    </xsl:element>
                </xsl:when>

                <xsl:when test="$nbErrors&gt;0">
                    <xsl:element name="failure">
                        <xsl:call-template name="testCaseContent"/>
                    </xsl:element>
                </xsl:when>
            </xsl:choose>


            <xsl:if test="(count(child::Info)+ count(child::Warning) + count(child::Message))>0">
                <xsl:element name="system-out">
                    <xsl:for-each select="child::Info">
                        <xsl:variable name="currElt" select="."/>
                        <xsl:text>&#13;</xsl:text>
                        <xsl:text>[Info] - </xsl:text>
                        <xsl:value-of select="($currElt)"/>
                        <xsl:text>&#13;</xsl:text>
                        <xsl:text> == [File] - </xsl:text><xsl:value-of select="($currElt)/@file"/>
                        <xsl:text>&#13;</xsl:text>
                        <xsl:text> == [Line] - </xsl:text><xsl:value-of select="($currElt)/@line"/>
                        <xsl:text>&#13;</xsl:text>
                    </xsl:for-each>

                    <xsl:for-each select="child::Warning">
                        <xsl:variable name="currElt" select="."/>
                        <xsl:text>&#13;</xsl:text>
                        <xsl:text>[Warning] - </xsl:text>
                        <xsl:value-of select="($currElt)"/>
                        <xsl:text>&#13;</xsl:text>
                        <xsl:text> == [File] - </xsl:text><xsl:value-of select="($currElt)/@file"/>
                        <xsl:text>&#13;</xsl:text>
                        <xsl:text> == [Line] - </xsl:text><xsl:value-of select="($currElt)/@line"/>
                        <xsl:text>&#13;</xsl:text>
                    </xsl:for-each>

                    <xsl:for-each select="child::Message">
                        <xsl:variable name="currElt" select="."/>
                        <xsl:text>&#13;</xsl:text>
                        <xsl:text>[Message] - </xsl:text>
                        <xsl:value-of select="($currElt)"/>
                        <xsl:text>&#13;</xsl:text>
                        <xsl:text> == [File] - </xsl:text><xsl:value-of select="($currElt)/@file"/>
                        <xsl:text>&#13;</xsl:text>
                        <xsl:text> == [Line] - </xsl:text><xsl:value-of select="($currElt)/@line"/>
                        <xsl:text>&#13;</xsl:text>
                    </xsl:for-each>

                </xsl:element>
            </xsl:if>
        </xsl:element>

        <xsl:if test="count(child::Exception)>0">
            <xsl:element name="system-err">
                <xsl:for-each select="child::Exception">
                    <xsl:variable name="currElt" select="."/>
                    <xsl:text>&#13;</xsl:text>
                    <xsl:text>[Exception] - </xsl:text>
                    <xsl:value-of select="($currElt)"/>
                    <xsl:text>&#13;</xsl:text>
                    <xsl:text> == [File] - </xsl:text><xsl:value-of select="($currElt)/@file"/>
                    <xsl:text>&#13;</xsl:text>
                    <xsl:text> == [Line] - </xsl:text><xsl:value-of select="($currElt)/@line"/>
                    <xsl:text>&#13;</xsl:text>
                </xsl:for-each>
            </xsl:element>
        </xsl:if>

    </xsl:template>

    <xsl:template match="text()|@*"/>
</xsl:stylesheet>