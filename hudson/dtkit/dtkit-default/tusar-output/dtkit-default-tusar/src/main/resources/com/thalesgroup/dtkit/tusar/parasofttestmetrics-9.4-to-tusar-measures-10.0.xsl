<?xml version="1.0" encoding="UTF-8"?>
<!-- 
/*******************************************************************************
* Copyright (c) 2012 Thales Corporate Services SAS                             *
* Author : Aravindan Mahendran, Nadir Mouhoubi                                 *
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
				xmlns:measures="http://www.thalesgroup.com/tusar/measures/v6"
				xmlns:size="http://www.thalesgroup.com/tusar/size/v1"
                xmlns:duplications="http://www.thalesgroup.com/tusar/duplications/v1"
				xmlns:xs="http://www.w3.org/2001/XMLSchema">
	<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" />
	<xsl:decimal-format name="euro" decimal-separator="," grouping-separator="." />

	<xsl:template match="ResultsSession">
		<tusar:tusar xmlns:xs="http://www.w3.org/2001/XMLSchema"
			xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
			xmlns:measures="http://www.thalesgroup.com/tusar/measures/v6"
			xmlns:size="http://www.thalesgroup.com/tusar/size/v1"
            xmlns:duplications="http://www.thalesgroup.com/tusar/duplications/v1"
			xmlns:tusar="http://www.thalesgroup.com/tusar/v10"
			version="10.0">
			<xsl:element name="tusar:measures">
                <xsl:element name="measures:duplications">
                    <xsl:apply-templates select="CodingStandards/StdViols/DupViol"></xsl:apply-templates>
                </xsl:element>
				<xsl:element name="measures:size">
					<xsl:attribute name="toolname"><xsl:value-of select="/ResultsSession/@toolName" /></xsl:attribute>
					<xsl:attribute name="version"><xsl:value-of	select="/ResultsSession/@toolVer" /></xsl:attribute>
					<xsl:apply-templates select="CodingStandards/StdViols"></xsl:apply-templates>
					<xsl:apply-templates select="Metrics/Metric"></xsl:apply-templates>
                    <!--<xsl:apply-templates select="//CvgInfo"></xsl:apply-templates>-->
				</xsl:element>
			</xsl:element>
		</tusar:tusar>
	</xsl:template>

    <!-- In C++test (not in JTest), Complexity is seen as a violation (in Sonar, it is a measure) -->
	<xsl:template match="StdViols">
		<xsl:for-each-group select="StdViol[@rule='METRICS-29']" group-by="@locFile">
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
            
            <xsl:element name="size:resource">
                <xsl:attribute name="type">FILE</xsl:attribute>					
                <xsl:attribute name="value"><xsl:value-of select="$fsPath" /></xsl:attribute>
                <xsl:element name="size:measure">
                    <xsl:attribute name="key">Complexity</xsl:attribute>
                    <xsl:attribute name="value"><xsl:value-of select="sum(current-group()/number(substring-after(@msg, 'has Cyclomatic Complexity value: ' )))"/></xsl:attribute>
                </xsl:element> 
            </xsl:element>
        </xsl:for-each-group>
	</xsl:template>

    
    <xsl:template match="Metric">
        
        <xsl:variable name="metric">
            <xsl:choose>
                <xsl:when test="@short = 'ID'">
                    <xsl:value-of>dit</xsl:value-of>
                </xsl:when>
                <xsl:when test="@short = 'LCOM'">
                    <xsl:value-of>lcom4</xsl:value-of>
                </xsl:when>
                <xsl:when test="@short = 'NOO' and @lang='cpp'">
                    <xsl:value-of>functions</xsl:value-of>
                </xsl:when>
                <xsl:when test="@short = 'VG'">
                    <xsl:value-of>complexity</xsl:value-of>
                </xsl:when>
                <xsl:when test="@short = 'NSC'">
                    <xsl:value-of>noc</xsl:value-of>
                </xsl:when>
                <xsl:when test="@short = 'NOC'">
                    <xsl:value-of>classes</xsl:value-of>
                </xsl:when>
                <xsl:when test="@short = 'CULOC'">
                    <xsl:value-of>ncloc</xsl:value-of>
                </xsl:when>
                <xsl:when test="@short = 'CFNL'">
                    <xsl:value-of>lines</xsl:value-of>
                </xsl:when>
                <xsl:when test="@short = 'NOP'">
                    <xsl:value-of>packages</xsl:value-of>
                </xsl:when>
                <xsl:when test="@short = 'NSTMT'">
                    <xsl:value-of>statements</xsl:value-of>
                </xsl:when>
                <xsl:when test="@short = 'RFC'">
                    <xsl:value-of>rfc</xsl:value-of>
                </xsl:when>
                <xsl:when test="@short = 'CA'">
                    <xsl:value-of>ca</xsl:value-of>
                </xsl:when>
                <xsl:when test="@short = 'CE'">
                    <xsl:value-of>ce</xsl:value-of>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="@short"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        
        <!-- Info nodes without children and with an attribute "elem" containing a '.'-->
        <xsl:for-each select="descendant::Info[not(child::*) and contains(@elem,'.')]">
            <xsl:element name="size:resource">
                <xsl:variable name="locFile">
                    <xsl:call-template name="constructResource">
                        <xsl:with-param name="resource" select="@elem"/>
                        <xsl:with-param name="node" select="current()"/>
                    </xsl:call-template>
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
                <xsl:attribute name="type">FILE</xsl:attribute>
                <xsl:attribute name="value"><xsl:value-of select="$fsPath"/></xsl:attribute>
                <xsl:element name="size:measure">
                    <xsl:attribute name="key">
                        <xsl:value-of select="$metric" />
                    </xsl:attribute>
                    <xsl:choose>
                        <xsl:when test="@val = ''">
                            <xsl:attribute name="value">0</xsl:attribute>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:attribute name="value"><xsl:value-of select="@val" /></xsl:attribute>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:element>
            </xsl:element>
        </xsl:for-each>
        
        <!-- Info nodes corresponding to a file (first ancestor of an orphan Info node with an attribute "elem" containing a '.')-->
        <xsl:for-each select="descendant::Info[not(child::*) and not(contains(@elem,'.'))]/ancestor::Info[contains(@elem,'.')][position()=1]">
            <xsl:element name="size:resource">
                <xsl:variable name="locFile">
                    <xsl:call-template name="constructResource">
                        <xsl:with-param name="resource" select="@elem"/>
                        <xsl:with-param name="node" select="current()"/>
                    </xsl:call-template>
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
                <xsl:attribute name="type">FILE</xsl:attribute>
                <xsl:attribute name="value"><xsl:value-of select="$fsPath"/></xsl:attribute>
                <xsl:element name="size:measure">
                    <xsl:attribute name="key">
                        <xsl:value-of select="$metric" />
                    </xsl:attribute>
                    <xsl:choose>
                        <xsl:when test="@avg = ''">
                            <xsl:attribute name="value">0</xsl:attribute>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:attribute name="value"><xsl:value-of select="@avg" /></xsl:attribute>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:element>
            </xsl:element>
        </xsl:for-each>
        
        <!-- Info nodes corresponding to a directory (Orphan Info node with an attribute @disp)-->
        <xsl:for-each select="descendant::Info[not(child::*)][@disp]">
            <xsl:element name="size:resource">
                <xsl:variable name="locFile">
                    <xsl:call-template name="constructResource">
                        <xsl:with-param name="resource" select="@elem"/>
                        <xsl:with-param name="node" select="current()"/>
                    </xsl:call-template>
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
                <xsl:attribute name="type">DIRECTORY</xsl:attribute>
                <xsl:attribute name="value"><xsl:value-of select="$fsPath"/></xsl:attribute>
                <xsl:element name="size:measure">
                    <xsl:attribute name="key">
                        <xsl:value-of select="$metric" />
                    </xsl:attribute>
                    <xsl:choose>
                        <xsl:when test="@val = ''">
                            <xsl:attribute name="value">0</xsl:attribute>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:attribute name="value"><xsl:value-of select="@val" /></xsl:attribute>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:element>
            </xsl:element>
        </xsl:for-each>
    </xsl:template>
    
    
    <xsl:template match="DupViol">
        <xsl:for-each select="ElDescList">
            <xsl:element name="duplications:set">
                <xsl:attribute name="lines"><xsl:value-of select="*[1]/@srcRngEndLn - *[1]/@srcRngStartln + 1"/></xsl:attribute>
                <xsl:attribute name="tokens">0</xsl:attribute>
                <xsl:for-each select="ElDesc">
                    <xsl:variable name="locFile">
                        <xsl:value-of select="@srcRngFile" />
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
                    <xsl:element name="duplications:resource">
                        <xsl:attribute name="path">
                            <xsl:value-of select="$fsPath"/>
                        </xsl:attribute>
                        <xsl:attribute name="line">
                            <xsl:value-of select="@srcRngStartln"/>
                        </xsl:attribute>
                    </xsl:element>
                </xsl:for-each>
            </xsl:element>
        </xsl:for-each>
    </xsl:template>
    
    <xsl:template name="constructResource">
        <xsl:param name="resource"/>
        <xsl:param name="node"/>
        <xsl:if test="local-name($node/parent::node()) = 'Info'">
            <xsl:call-template name="constructResource">
                <xsl:with-param name="resource" select="$node/parent::node()/@elem"/>
                <xsl:with-param name="node" select="$node/parent::node()"/>
            </xsl:call-template>
        </xsl:if>
        <xsl:text>/</xsl:text><xsl:value-of select="$node/@elem"/>      
        
    </xsl:template>
	

</xsl:stylesheet>
