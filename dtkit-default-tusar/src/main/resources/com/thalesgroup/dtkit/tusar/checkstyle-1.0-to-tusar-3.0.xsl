<?xml version="1.0" encoding="UTF-8"?>
<!--
/*******************************************************************************
* Copyright (c) 2010 Thales Corporate Services SAS                             *
* Author : Gregory Boissinot                                                         *
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
                xmlns:violations="http://www.thalesgroup.com/tusar/violations/v3"
                xmlns:xs="http://www.w3.org/2001/XMLSchema">
    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>

    <xsl:template match="checkstyle">
        <tusar:tusar
                xmlns:violations="http://www.thalesgroup.com/tusar/violations/v3"
                xmlns:tusar="http://www.thalesgroup.com/tusar/v3"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                version="3.0">

            <tusar:violations>

                <xsl:attribute name="toolname">checkstyle</xsl:attribute>

                <xsl:for-each select="file">
                    <violations:file>
                        <xsl:attribute name="path">
                            <xsl:value-of select="@name"/>
                        </xsl:attribute>

                        <xsl:for-each select="error">
                            <violations:violation>
                                <xsl:attribute name="line">
                                    <xsl:value-of select="@line"/>
                                </xsl:attribute>

                                <xsl:attribute name="message">
                                    <xsl:value-of select="@message"/>
                                </xsl:attribute>

                                <xsl:attribute name="key">
                                    <xsl:value-of select="@source"/>
                                </xsl:attribute>

                                <xsl:attribute name="column">
                                    <xsl:value-of select="@column"/>
                                </xsl:attribute>

                                <xsl:attribute name="severity">
                                    <xsl:value-of select="@severity"/>
                                </xsl:attribute>
                            </violations:violation>
                        </xsl:for-each>
                    </violations:file>
                </xsl:for-each>
            </tusar:violations>
        </tusar:tusar>
    </xsl:template>

    <xsl:template match="text()|@*"/>
</xsl:stylesheet>

