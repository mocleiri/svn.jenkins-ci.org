<?xml version="1.0" encoding="UTF-8"?>
<!--
/*******************************************************************************
* Copyright (c) 2009 Thales Corporate Services SAS                             *
* Author : Joel Forner                                                         *
*          from a script by  Jan De Bleser (jan at commsquare dot com)         *
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

    <xsl:template match="/">

        <xsl:element name="tusar">
            <xsl:attribute name="xmlns_xsi">
                <xsl:text>http://www.w3.org/2001/XMLSchema-instance</xsl:text>
            </xsl:attribute>

            <xsl:attribute name="version">
                <xsl:text>1.0</xsl:text>
            </xsl:attribute>

            <xsl:element name="tests">

				<xsl:for-each select="TestResults[1]//TestListing[1]//TestSuite">
					<testsuite>
						<xsl:attribute name="name">
							<xsl:value-of select="@Name"/>
						</xsl:attribute>
						<xsl:attribute name="tests">
							<xsl:value-of select="@NumberOfRunTests"/>
						</xsl:attribute>
						<xsl:attribute name="time">
							<xsl:value-of select="@ElapsedTime"/>
						</xsl:attribute>
						<xsl:attribute name="failures">
							<xsl:value-of select="@NumberOfFailures"/>
						</xsl:attribute>
						<xsl:attribute name="errors">
							<xsl:value-of select="@NumberOfErrors"/>
						</xsl:attribute>
						<xsl:attribute name="skipped">
							<xsl:value-of select="@NumberOfIgnoredTests"/>
						</xsl:attribute>
						<xsl:for-each select="Test">
							<testcase>
								<xsl:attribute name="fulltestname">
									<xsl:value-of select="../@Name"/>
								</xsl:attribute>
								<xsl:attribute name="testname">
									<xsl:value-of select="@Name"/>
								</xsl:attribute>
								<xsl:attribute name="time">
									<xsl:value-of select="@ElapsedTime"/>
								</xsl:attribute>
								<xsl:if test="@Result='Failed'">
									<failure>
										<xsl:attribute name="message">
											<xsl:value-of select="Message"/>
										</xsl:attribute>
										<xsl:attribute name="type">
											<xsl:value-of select="ExceptionClass"/>
										</xsl:attribute>
										<xsl:value-of select="ExceptionMessage"/>
									</failure>
								</xsl:if>
								<xsl:if test="@Result='Error'">
									<failure>
										<xsl:attribute name="message">
											<xsl:value-of select="Message"/>
										</xsl:attribute>
										<xsl:attribute name="type">
											<xsl:value-of select="ExceptionClass"/>
										</xsl:attribute>
										<xsl:value-of select="SourceUnitName"/> (line <xsl:value-of select="LineNumber"/>):[<xsl:value-of select="FailedMethodName"/>]:<xsl:value-of select="ExceptionMessage"/>
									</failure>
								</xsl:if>
							</testcase>
						</xsl:for-each>
					</testsuite>
				</xsl:for-each>
				
			</xsl:element>

        </xsl:element>
    </xsl:template>
	
    <xsl:template match="text()|@*"/>
</xsl:stylesheet>