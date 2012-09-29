<?xml version="1.0" encoding="UTF-8"?>
<!--
/*******************************************************************************
* Copyright (c) 2012 Thales Corporate Services SAS                             *
* Author : Gregory Boissinot, Julien Dort, Aravindan Mahendran                 *
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
                xmlns:violations="http://www.thalesgroup.com/tusar/violations/v4"
                xmlns:xs="http://www.w3.org/2001/XMLSchema">
    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>

    <xsl:template match="results">

        <tusar:tusar
                xmlns:violations="http://www.thalesgroup.com/tusar/violations/v4"
                xmlns:tusar="http://www.thalesgroup.com/tusar/v5"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                version="5.0">

            <xsl:element name="tusar:violations">

                <xsl:attribute name="toolname">cppcheck</xsl:attribute>
                
                <xsl:choose>
                    <xsl:when test="@version=2">
                        <xsl:attribute name="version">
                            <xsl:value-of select="cppcheck/@version"/>
                        </xsl:attribute>
                        <xsl:apply-templates select="errors"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:attribute name="version">1.43</xsl:attribute>
                        <xsl:for-each-group select="error" group-by="@file">

                            <xsl:element name="violations:file">
                                <xsl:attribute name="path">
                                    <xsl:value-of select="@file"/>
                                </xsl:attribute>

                                <xsl:for-each select="current-group()">

                                    <xsl:element name="violations:violation">
                                        <xsl:attribute name="line">
                                            <xsl:value-of select="@line"/>
                                        </xsl:attribute>

                                        <xsl:attribute name="message">
                                            <xsl:value-of select="@msg"/>
                                        </xsl:attribute>

                                        <xsl:attribute name="key">
                                            <xsl:value-of select="@id"/>
                                        </xsl:attribute>

                                        <xsl:attribute name="severity">
                                            <xsl:value-of select="@severity"/>
                                        </xsl:attribute>

                                    </xsl:element>

                                </xsl:for-each>
                            </xsl:element>

                        </xsl:for-each-group>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:element>
        </tusar:tusar>
    </xsl:template>
    
    <xsl:template match="errors">
         <xsl:for-each-group select="error" group-by="location/@file">

            <xsl:element name="violations:file">
                <xsl:attribute name="path">
                    <xsl:value-of select="location/@file"/>
                </xsl:attribute>

                    <xsl:for-each select="current-group()">
                        <xsl:element name="violations:violation">
                            <xsl:attribute name="line">
                                <xsl:value-of select="location/@line"/>
                            </xsl:attribute>

                            <xsl:attribute name="message">
                                <xsl:value-of select="@verbose"/>
                            </xsl:attribute>

                            <xsl:attribute name="key">
                                <xsl:value-of select="@id"/>
                            </xsl:attribute>

                            <xsl:attribute name="severity">
                                <xsl:value-of select="@severity"/>
                            </xsl:attribute>

                        </xsl:element>
                    </xsl:for-each>

            </xsl:element>
            
        </xsl:for-each-group>        
    </xsl:template>
    
    
    
</xsl:stylesheet>