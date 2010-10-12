<?xml version="1.0" encoding="UTF-8"?>
<!--
/*******************************************************************************
* Copyright (c) 2010 Thales Corporate Services SAS                             *
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

				  <xsl:element name="testsuite">
					 <xsl:attribute name="errors">
						<xsl:value-of select="unittest-results/@failedtests" />
					 </xsl:attribute>

					 <xsl:attribute name="failures">
						<xsl:value-of select="unittest-results/@failures" />
					 </xsl:attribute>
					 
					 <xsl:attribute name="tests">
						<xsl:value-of select="unittest-results/@tests" />
					 </xsl:attribute>         

					 <xsl:attribute name="name">unittest</xsl:attribute>  
					 
					 <xsl:apply-templates />
				  </xsl:element>
				
            </xsl:element>
        </xsl:element>
    </xsl:template>
	
   <xsl:template match="/unittest-results/test">
      <xsl:element name="testcase">


         <xsl:attribute name="fulltestname">
            <xsl:value-of select="@suite" />
         </xsl:attribute>

         <xsl:attribute name="testname">
            <xsl:value-of select="@name" />
         </xsl:attribute>

         <xsl:attribute name="time">0</xsl:attribute>
         
          <xsl:copy-of select="child::*" />

      </xsl:element>
   </xsl:template>

    <xsl:template match="text()|@*"/>
</xsl:stylesheet>