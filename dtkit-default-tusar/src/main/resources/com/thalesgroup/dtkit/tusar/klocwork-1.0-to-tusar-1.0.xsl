<?xml version="1.0" encoding="UTF-8"?>
<!--
/*******************************************************************************
* Copyright (c) 2009 Thales Corporate Services SAS                             *
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
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
        >
    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>

    <xsl:template match="errorList">

        <xsl:element name="tusar">
            <xsl:attribute name="xmlns_xsi">
                <xsl:text>http://www.w3.org/2001/XMLSchema-instance</xsl:text>
            </xsl:attribute>
            <xsl:attribute name="version">
                <xsl:text>1.0</xsl:text>
            </xsl:attribute>

            <xsl:element name="violations">

                <xsl:for-each-group select="problem" group-by="file">

                    <xsl:element name="file">
                        <xsl:attribute name="path">
                            <xsl:value-of select="file"/>
                        </xsl:attribute>

                        <xsl:for-each select="current-group()">

                            <xsl:element name="violation">
                                <xsl:attribute name="line">
                                    <xsl:value-of select="line"/>
                                </xsl:attribute>

                                <xsl:attribute name="message">
                                    <xsl:value-of select="message"/>
                                </xsl:attribute>

                                <xsl:attribute name="key">
                                    <xsl:value-of select="code"/>
                                </xsl:attribute>

                                <xsl:attribute name="column">
                                    <xsl:value-of select="column"/>
                                </xsl:attribute>

                                <xsl:attribute name="severity">
                                    <xsl:value-of select="severity"/>
                                </xsl:attribute>

                            </xsl:element>

                        </xsl:for-each>
                    </xsl:element>
                </xsl:for-each-group>
            </xsl:element>
        </xsl:element>
    </xsl:template>
</xsl:stylesheet>