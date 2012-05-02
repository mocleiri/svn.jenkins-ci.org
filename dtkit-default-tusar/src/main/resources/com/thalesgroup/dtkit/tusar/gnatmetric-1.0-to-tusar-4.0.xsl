<?xml version="1.0" encoding="UTF-8"?>
<!--
/*******************************************************************************
* Copyright (c) 2009 Thales Corporate Services SAS                             *
* Author : Gregory Boissinot, Joel Forner                                      *
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
                xmlns:measures="http://www.thalesgroup.com/tusar/measures/v4"
                xmlns:xs="http://www.w3.org/2001/XMLSchema">
    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>

    <xsl:template match="/">

        <tusar:tusar
                xmlns:measures="http://www.thalesgroup.com/tusar/measures/v4"
                xmlns:tusar="http://www.thalesgroup.com/tusar/v4"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

            <xsl:element name="tusar:measures">

                <xsl:attribute name="toolname">gnatmetric</xsl:attribute>

                <xsl:for-each select="//file">

                    <xsl:element name="measures:resource">
                        <xsl:attribute name="type">
                            <xsl:text>FILE</xsl:text>
                        </xsl:attribute>

                        <xsl:attribute name="value">
                            <xsl:value-of select="@name"/>
                        </xsl:attribute>

                        <xsl:for-each select="metric">

                            <xsl:if test="contains(@name, 'all_lines') or contains(@name, 'code_lines')">
                                <xsl:element name="measures:measure">

                                    <xsl:choose>
                                        <xsl:when test="contains(@name, 'all_lines')">
                                            <xsl:attribute name="key">
                                                <xsl:text>LINES</xsl:text>
                                            </xsl:attribute>

                                            <xsl:attribute name="value">
                                                <xsl:value-of select="text()"/>
                                            </xsl:attribute>
                                        </xsl:when>

                                        <xsl:when test="contains(@name, 'code_lines')">
                                            <xsl:attribute name="key">
                                                <xsl:text>NCLOC</xsl:text>
                                            </xsl:attribute>

                                            <xsl:attribute name="value">
                                                <xsl:value-of select="text()"/>
                                            </xsl:attribute>
                                        </xsl:when>

                                    </xsl:choose>

                                </xsl:element>
                            </xsl:if>
                        </xsl:for-each>
                    </xsl:element>

                </xsl:for-each>

            </xsl:element>
        </tusar:tusar>
    </xsl:template>

</xsl:stylesheet>