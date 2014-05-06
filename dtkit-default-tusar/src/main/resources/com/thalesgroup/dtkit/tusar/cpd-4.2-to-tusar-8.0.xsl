<!--
/*******************************************************************************
* Copyright (c) 2009 Thales Corporate Services SAS                             *
* Author : Mohamed Koundoussi                                                  *
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
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:measures="http://www.thalesgroup.com/tusar/measures/v6"
                xmlns:duplications="http://www.thalesgroup.com/tusar/duplications/v1"
                version="2.0"
        >
    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"
                cdata-section-elements="duplications:codefragment"/>
    <xsl:template match="pmd-cpd">
        <tusar:tusar
                xmlns:measures="http://www.thalesgroup.com/tusar/measures/v6"
                xmlns:duplications="http://www.thalesgroup.com/tusar/duplications/v1"
                xmlns:tusar="http://www.thalesgroup.com/tusar/v8"
                version="8.0">
            <xsl:element name="tusar:measures">
                <xsl:attribute name="toolname">cpd</xsl:attribute>
                <xsl:element name="measures:duplications">
                    <xsl:for-each select="duplication">
                        <xsl:element name="duplications:set">
                            <xsl:attribute name="lines">
                                <xsl:value-of select="@lines"/>
                            </xsl:attribute>
                            <xsl:attribute name="tokens">
                                <xsl:value-of select="@tokens"/>
                            </xsl:attribute>
                            <xsl:for-each select="file">
                                <xsl:element name="duplications:resource">
                                    <xsl:attribute name="path">
                                        <xsl:value-of select="@path"/>
                                    </xsl:attribute>
                                    <xsl:attribute name="line">
                                        <xsl:value-of select="@line"/>
                                    </xsl:attribute>
                                </xsl:element>
                            </xsl:for-each>
                            <xsl:element name="duplications:codefragment">
                                <xsl:copy-of select="codefragment/text()"/>
                            </xsl:element>
                        </xsl:element>
                    </xsl:for-each>
                </xsl:element>
            </xsl:element>
        </tusar:tusar>
    </xsl:template>

</xsl:stylesheet>