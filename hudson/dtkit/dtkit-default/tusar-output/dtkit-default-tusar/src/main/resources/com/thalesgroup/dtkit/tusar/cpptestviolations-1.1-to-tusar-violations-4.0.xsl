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
                xmlns:violations="http://www.thalesgroup.com/tusar/violations/v4"
                xmlns:xs="http://www.w3.org/2001/XMLSchema">
    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>

    <xsl:template match="ResultsSession">

        <tusar:tusar
                xmlns:violations="http://www.thalesgroup.com/tusar/violations/v4"
                xmlns:tusar="http://www.thalesgroup.com/tusar/v11"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                version="11.0">

            <tusar:violations>

                <xsl:attribute name="toolname">cpptest</xsl:attribute>
                <xsl:attribute name="version">7.3</xsl:attribute>

                <xsl:for-each-group select="CodingStandards/StdViols/StdViol" group-by="@locFile">
                    <!-- AM : getting the @fsPath corresponding to the @locFile (if it exists)-->
                    <xsl:variable name="locFile">
                        <xsl:value-of select="@locFile"/>
                    </xsl:variable>
                    <xsl:variable name="fsPath">
                        <xsl:choose>
                            <xsl:when test="distinct-values(/ResultsSession/Locations/Loc[@loc=$locFile]/@fsPath)">
                                <xsl:value-of
                                        select="distinct-values(/ResultsSession/Locations/Loc[@loc=$locFile]/@fsPath)"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="$locFile"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:variable>
                    <xsl:element name="violations:file">
                        <!-- AM : changing the @locFile by @fsPath in tag Locations/Loc-->
                        <xsl:attribute name="path">
                            <xsl:value-of select="$fsPath"/>
                            <!--<xsl:value-of select="@locFile"/>-->
                        </xsl:attribute>

                        <xsl:for-each select="current-group()">
                            <xsl:if test="not(@supp) or @supp!='true'">
                                <xsl:element name="violations:violation">
                                    <xsl:attribute name="line">
                                        <xsl:value-of select="@ln"/>
                                    </xsl:attribute>

                                    <xsl:attribute name="message">
                                        <xsl:value-of select="@msg"/>
                                    </xsl:attribute>

                                    <xsl:attribute name="key">
                                        <xsl:value-of select="@rule"/>
                                    </xsl:attribute>

                                    <xsl:attribute name="severity">
                                        <!-- Les conditions ci-dessous peuvent ├óΓÇ¥┼ô├é┬¼tre modifi├óΓÇ¥┼ô├ó┼Æ┬Ées pour trier les niveaux info, warning et error qui d├óΓÇ¥┼ô├ó┼Æ┬Épendent du choix de l'utilisateur-->
                                        <xsl:if test="(@sev=0)">
                                            <xsl:text>error</xsl:text>
                                        </xsl:if>
                                        <xsl:if test="(@sev=1)or(@sev=2)or((@sev=3)and(@urgent))">
                                            <xsl:text>warning</xsl:text>
                                        </xsl:if>
                                        <xsl:if test="(@sev>3)or((@sev=3)and(not (@urgent)))">
                                            <xsl:text>info</xsl:text>
                                        </xsl:if>
                                    </xsl:attribute>
                                </xsl:element>
                            </xsl:if>
                        </xsl:for-each>
                    </xsl:element>

                </xsl:for-each-group>

                <xsl:for-each-group select="CodingStandards/StdViols/FlowViol" group-by="@locFile">
                    <!-- AM : getting the @fsPath corresponding to the @locFile (if it exists)-->
                    <xsl:variable name="locFile">
                        <xsl:value-of select="@locFile"/>
                    </xsl:variable>
                    <xsl:variable name="fsPath">
                        <xsl:choose>
                            <xsl:when test="distinct-values(/ResultsSession/Locations/Loc[@loc=$locFile]/@fsPath)">
                                <xsl:value-of
                                        select="distinct-values(/ResultsSession/Locations/Loc[@loc=$locFile]/@fsPath)"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="$locFile"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:variable>
                    <xsl:element name="violations:file">
                        <!-- AM : changing the @locFile by @fsPath in tag Locations/Loc-->
                        <xsl:attribute name="path">
                            <xsl:value-of select="$fsPath"/>
                            <!--<xsl:value-of select="@locFile"/>-->
                        </xsl:attribute>

                        <xsl:for-each select="current-group()">

                            <xsl:variable name="l_msg" select="@msg"/>
                            <xsl:variable name="l_rule" select="@rule"/>
                            <xsl:variable name="l_sev" select="@sev"/>
                            <xsl:variable name="l_urgent" select="@urgent"/>

                            <xsl:for-each select=".//ElDesc">
                                <xsl:if test="not(@supp) or @supp!='true'">
                                    <xsl:element name="violations:violation">
                                        <xsl:attribute name="line">
                                            <xsl:value-of select="@ln"/>
                                        </xsl:attribute>

                                        <xsl:attribute name="message">
                                            <xsl:value-of select="$l_msg"/>
                                        </xsl:attribute>

                                        <xsl:attribute name="key">
                                            <xsl:value-of select="$l_rule"/>
                                        </xsl:attribute>

                                        <xsl:attribute name="severity">
                                            <!-- Les conditions ci-dessous peuvent ├óΓÇ¥┼ô├é┬¼tre modifi├óΓÇ¥┼ô├ó┼Æ┬Ées pour trier les niveaux info, warning et error qui d├óΓÇ¥┼ô├ó┼Æ┬Épendent du choix de l'utilisateur-->
                                            <xsl:if test="($l_sev=0)">
                                                <xsl:text>error</xsl:text>
                                            </xsl:if>
                                            <xsl:if test="($l_sev=1)or($l_sev=2)or(($l_sev=3)and($l_urgent))">
                                                <xsl:text>warning</xsl:text>
                                            </xsl:if>
                                            <xsl:if test="($l_sev>3)or(($l_sev=3)and(not ($l_urgent)))">
                                                <xsl:text>info</xsl:text>
                                            </xsl:if>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                            </xsl:for-each>

                        </xsl:for-each>

                    </xsl:element>
                </xsl:for-each-group>

                <xsl:for-each-group select="CodingStandards/StdViols/MetViol" group-by="@locFile">
                    <!-- AM : getting the @fsPath corresponding to the @locFile (if it exists)-->
                    <xsl:variable name="locFile">
                        <xsl:value-of select="@locFile"/>
                    </xsl:variable>
                    <xsl:variable name="fsPath">
                        <xsl:choose>
                            <xsl:when test="distinct-values(/ResultsSession/Locations/Loc[@loc=$locFile]/@fsPath)">
                                <xsl:value-of
                                        select="distinct-values(/ResultsSession/Locations/Loc[@loc=$locFile]/@fsPath)"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="$locFile"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:variable>
                    <xsl:element name="violations:file">
                        <!-- AM : changing the @locFile by @fsPath in tag Locations/Loc-->
                        <xsl:attribute name="path">
                            <xsl:value-of select="$fsPath"/>
                            <!--<xsl:value-of select="@locFile"/>-->
                        </xsl:attribute>

                        <xsl:for-each select="current-group()">
                            <xsl:if test="not(@supp) or @supp!='true'">
                                <xsl:element name="violations:violation">
                                    <xsl:attribute name="line">
                                        <xsl:value-of select="@ln"/>
                                    </xsl:attribute>

                                    <xsl:attribute name="message">
                                        <xsl:value-of select="@msg"/>
                                    </xsl:attribute>

                                    <xsl:attribute name="key">
                                        <xsl:value-of select="@rule"/>
                                    </xsl:attribute>

                                    <xsl:attribute name="severity">
                                        <!-- Les conditions ci-dessous peuvent ├óΓÇ¥┼ô├é┬¼tre modifi├óΓÇ¥┼ô├ó┼Æ┬Ées pour trier les niveaux info, warning et error qui d├óΓÇ¥┼ô├ó┼Æ┬Épendent du choix de l'utilisateur-->
                                        <xsl:if test="(@sev=0)">
                                            <xsl:text>error</xsl:text>
                                        </xsl:if>
                                        <xsl:if test="(@sev=1)or(@sev=2)or((@sev=3)and(@urgent))">
                                            <xsl:text>warning</xsl:text>
                                        </xsl:if>
                                        <xsl:if test="(@sev>3)or((@sev=3)and(not (@urgent)))">
                                            <xsl:text>info</xsl:text>
                                        </xsl:if>
                                    </xsl:attribute>
                                </xsl:element>
                            </xsl:if>
                        </xsl:for-each>
                    </xsl:element>

                </xsl:for-each-group>

            </tusar:violations>
        </tusar:tusar>
    </xsl:template>
</xsl:stylesheet>
