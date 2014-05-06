<!--
/*******************************************************************************
* Copyright (c) 2009 Thales Corporate Services SAS                             *
* Author : Mohamed Koundoussi  
* version 2.0     sourcemonitor: 3.2.7 		tusar: 8.0                    *
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
                xmlns:size="http://www.thalesgroup.com/tusar/size/v1"
                version="2.0"
        >
    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>
    <xsl:decimal-format name="euro" decimal-separator="," grouping-separator="."/>
    <xsl:template match="sourcemonitor_metrics">
        <tusar:tusar
                xmlns:measures="http://www.thalesgroup.com/tusar/measures/v6"
                xmlns:size="http://www.thalesgroup.com/tusar/size/v1"
                xmlns:tusar="http://www.thalesgroup.com/tusar/v8"
                version="8.0">
            <xsl:element name="tusar:measures">
                <xsl:attribute name="toolname">sourcemonitor</xsl:attribute>
                <xsl:attribute name="version">
                    <xsl:value-of select="/sourcemonitor_metrics/project/@version"/>
                </xsl:attribute>
                <xsl:element name="measures:size">
                    <xsl:element name="size:resource">
                        <xsl:attribute name="type">PROJECT</xsl:attribute>
                        <xsl:attribute name="value">
                            <xsl:value-of select="//project_directory"/>
                        </xsl:attribute>
                        <xsl:if test="//checkpoints/checkpoint/files[@file_count]">
                            <xsl:element name="size:measure">
                                <xsl:attribute name="key">files</xsl:attribute>
                                <xsl:attribute name="value">
                                    <xsl:value-of
                                            select="number(translate(//checkpoints/checkpoint/files/@file_count,',','.'))"/>
                                </xsl:attribute>
                            </xsl:element>
                        </xsl:if>
                        <xsl:element name="size:measure">
                            <xsl:attribute name="key">class_complexity_distribution</xsl:attribute>
                            <xsl:attribute name="value">
                                <xsl:value-of
                                        select="translate(/sourcemonitor_metrics/addings/_project/class_complexity_distribution,',','.')"/>
                            </xsl:attribute>
                        </xsl:element>
                        <xsl:element name="size:measure">
                            <xsl:attribute name="key">function_complexity_distribution</xsl:attribute>
                            <xsl:attribute name="value">
                                <xsl:value-of
                                        select="translate(/sourcemonitor_metrics/addings/_project/function_complexity_distribution,',','.')"/>
                            </xsl:attribute>
                        </xsl:element>
                    </xsl:element>
                    <xsl:if test="//project_language='Java'">
                        <xsl:for-each select="//files/file">
                            <xsl:variable name="_file_name" select="@file_name"/>
                            <xsl:element name="size:resource">
                                <xsl:attribute name="type">FILE</xsl:attribute>
                                <xsl:attribute name="value">
                                    <xsl:value-of select="@file_name"/>
                                </xsl:attribute>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">lines</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="number(translate(metrics/metric[@id='M0'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">statements</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="number(translate(metrics/metric[@id='M1'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:if test="//metric_names/metric_name[@id='M4']/@type='percent'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">comment_lines</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="(number(translate(metrics/metric[@id='M4'],',','.'))*number(translate(metrics/metric[@id='M0'],',','.')))div 100"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">ncloc</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metrics/metric[@id='M0'],',','.'))-number(translate(/sourcemonitor_metrics/addings/_files/file[@name=$_file_name]/blankLines,',','.'))-((number(translate(metrics/metric[@id='M4'],',','.'))*number(translate(metrics/metric[@id='M0'],',','.')))div 100)"/>
                                        </xsl:attribute>
                                    </xsl:element>

                                </xsl:if>
                                <xsl:if test="//metric_names/metric_name[@id='M4']/@type='number'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">comment_lines</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of select="number(translate(metrics/metric[@id='M4'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">ncloc</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metrics/metric[@id='M0'],',','.'))-number(translate(/sourcemonitor_metrics/addings/_files/file[@name=$_file_name]/blankLines,',','.'))-number(translate(metrics/metric[@id='M4'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">classes</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="number(translate(metrics/metric[@id='M5'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">accessors</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of
                                                select="number(translate(/sourcemonitor_metrics/addings/_files/file[@name=$_file_name]/accessors,',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:if test="//metric_names/metric_name[@id='M6']/@type='number'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">functions</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of select="number(translate(metrics/metric[@id='M6'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">complexity</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metrics/metric[@id='M6'],',','.'))*number(translate(metrics/metric[@id='M14'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">class_complexity</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metrics/metric[@id='M14'],',','.'))*number(translate(metrics/metric[@id='M6'],',','.')) div number(translate(metrics/metric[(@id='M4')],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:if test="//metric_names/metric_name[@id='M6']/@type='ratio'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">functions</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metrics/metric[(@id='M6')],',','.'))*number(translate(metrics/metric[(@id='M4')],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">complexity</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metrics/metric[@id='M6'],',','.'))*number(translate(metrics/metric[@id='M5'],',','.'))*number(translate(metrics/metric[@id='M14'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">class_complexity</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metrics/metric[@id='M14'],',','.'))*number(translate(metrics/metric[@id='M6'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">function_complexity</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="number(translate(metrics/metric[@id='M14'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                            </xsl:element>
                        </xsl:for-each>
                        <xsl:for-each select="//checkpoints/checkpoint/metrics">
                            <xsl:element name="size:resource">
                                <xsl:attribute name="type">PROJECT</xsl:attribute>
                                <xsl:attribute name="value">
                                    <xsl:value-of select="//project_directory"/>
                                </xsl:attribute>
                                <xsl:if test="//checkpoints/checkpoint[@checkpoint_files]">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">files</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(//checkpoints/checkpoint/@checkpoint_files,',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">lines</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="number(translate(metric[@id='M0'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">statements</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="number(translate(metric[@id='M1'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:if test="//metric_names/metric_name[@id='M4']/@type='percent'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">comment_lines</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="(number(translate(metric[@id='M4'],',','.'))*number(translate(metric[@id='M0'],',','.'))) div 100"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">ncloc</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metric[@id='M0'],',','.'))-number(translate(/sourcemonitor_metrics/addings/_project/blankLines,',','.'))-((number(translate(metric[@id='M4'],',','.'))*number(translate(metric[@id='M0'],',','.'))) div 100)"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:if test="//metric_names/metric_name[@id='M4']/@type='number'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">comment_lines</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of select="number(translate(metric[@id='M4'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">ncloc</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metric[@id='M0'],',','.'))-number(translate(/sourcemonitor_metrics/addings/_project/blankLines,',','.'))-number(translate(metric[@id='M4'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">classes</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="number(translate(metric[@id='M5'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>

                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">accessors</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of
                                                select="number(translate(/sourcemonitor_metrics/addings/_project/accessors,',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:if test="//metric_names/metric_name[@id='M6']/@type='number'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">functions</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of select="number(translate(metric[@id='M6'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">complexity</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metric[@id='M6'],',','.'))*number(translate(metric[@id='M14'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">file_complexity</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="(number(translate(metric[@id='M6'],',','.'))*number(translate(metric[@id='M14'],',','.'))) div number(translate(//checkpoints/checkpoint/@checkpoint_files,',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:if test="//metric_names/metric_name[@id='M6']/@type='ratio'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">functions</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metric[(@id='M6')],',','.'))*number(translate(metric[(@id='M5')],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">complexity</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metric[@id='M5'],',','.'))*number(translate(metric[@id='M6'],',','.'))*number(translate(metric[@id='M14'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">file_complexity</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="(number(translate(metric[@id='M5'],',','.'))*number(translate(metric[@id='M6'],',','.'))*number(translate(metric[@id='M14'],',','.'))) div number(translate(//checkpoints/checkpoint/@checkpoint_files,',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>

                            </xsl:element>
                        </xsl:for-each>

                    </xsl:if>
                    <xsl:if test="//project_language='C'">
                        <xsl:for-each select="//files/file">
                            <xsl:variable name="_file_name" select="@file_name"/>
                            <xsl:element name="size:resource">
                                <xsl:attribute name="type">FILE</xsl:attribute>
                                <xsl:attribute name="value">
                                    <xsl:value-of select="@file_name"/>
                                </xsl:attribute>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">lines</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="number(translate(metrics/metric[@id='M0'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">statements</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="number(translate(metrics/metric[@id='M1'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:if test="//metric_names/metric_name[@id='M3']/@type='percent'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">comment_lines</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="(number(translate(metrics/metric[@id='M3'],',','.'))*number(translate(metrics/metric[@id='M0'],',','.'))) div 100"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">ncloc</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metrics/metric[@id='M0'],',','.')) -((number(translate(metrics/metric[@id='M3'],',','.'))*number(translate(metrics/metric[@id='M0'],',','.'))) div 100)"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:if test="//metric_names/metric_name[@id='M3']/@type='number'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">comment_lines</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of select="number(translate(metrics/metric[@id='M3'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">ncloc</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metrics/metric[@id='M0'],',','.'))-number(translate(metrics/metric[@id='M3'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:if test="//metric_names/metric_name[@id='M4']/@type='number'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">functions</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of select="number(translate(metrics/metric[@id='M4'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">complexity</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of
                                                select="number(translate(metrics/metric[@id='M12'],',','.'))*number(translate(metrics/metric[@id='M4'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">function_complexity</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="number(translate(metrics/metric[@id='M12'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>

                            </xsl:element>
                        </xsl:for-each>
                        <xsl:for-each select="//checkpoints/checkpoint/metrics">
                            <xsl:element name="size:resource">
                                <xsl:attribute name="type">PROJECT</xsl:attribute>
                                <xsl:attribute name="value">
                                    <xsl:value-of select="//project_directory"/>
                                </xsl:attribute>
                                <xsl:if test="//checkpoints/checkpoint[@checkpoint_files]">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">files</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(//checkpoints/checkpoint/@checkpoint_files,',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">lines</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="number(translate(metric[@id='M0'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">statements</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="number(translate(metric[@id='M1'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:if test="//metric_names/metric_name[@id='M3']/@type='percent'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">comment_lines</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="(number(translate(metric[@id='M3'],',','.'))*number(translate(metric[@id='M3'],',','.'))) div 100"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">ncloc</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metric[@id='M0'],',','.'))-((number(translate(metric[@id='M3'],',','.'))*number(translate(metric[@id='M3'],',','.'))) div 100)"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:if test="//metric_names/metric_name[@id='M3']/@type='number'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">comment_lines</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of select="number(translate(metric[@id='M3'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">ncloc</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metric[@id='M0'],',','.'))-number(translate(metric[@id='M3'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:if test="//metric_names/metric_name[@id='M4']/@type='number'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">functions</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of select="number(translate(metric[@id='M4'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">complexity</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of
                                                select="number(translate(metric[@id='M12'],',','.'))*number(translate(metric[@id='M4'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">file_complexity</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of
                                                select="(number(translate(metric[@id='M12'],',','.'))*number(translate(metric[@id='M4'],',','.'))) div number(translate(//checkpoints/checkpoint/@checkpoint_files,',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">function_complexity</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="number(translate(metric[@id='M12'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                            </xsl:element>
                        </xsl:for-each>

                    </xsl:if>
                    <xsl:if test="//project_language='C++'">
                        <xsl:for-each select="//files/file">
                            <xsl:variable name="_file_name" select="@file_name"/>
                            <xsl:element name="size:resource">
                                <xsl:attribute name="type">FILE</xsl:attribute>
                                <xsl:attribute name="value">
                                    <xsl:value-of select="@file_name"/>
                                </xsl:attribute>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">lines</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="number(translate(metrics/metric[@id='M0'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">statements</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="number(translate(metrics/metric[@id='M1'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:if test="//metric_names/metric_name[@id='M3']/@type='percent'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">comment_lines</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="(number(translate(metrics/metric[@id='M3'],',','.'))*number(translate(metrics/metric[@id='M0'],',','.'))) div 100"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">ncloc</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metrics/metric[@id='M0'],',','.'))-number(translate(/sourcemonitor_metrics/addings/_files/file[@name=$_file_name]/blankLines,',','.'))-((number(translate(metrics/metric[@id='M3'],',','.'))*number(translate(metrics/metric[@id='M0'],',','.'))) div 100)"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:if test="//metric_names/metric_name[@id='M3']/@type='number'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">comment_lines</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of select="number(translate(metrics/metric[@id='M3'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">ncloc</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metrics/metric[@id='M0'],',','.'))-number(translate(/sourcemonitor_metrics/addings/_files/file[@name=$_file_name]/blankLines,',','.'))-number(translate(metrics/metric[@id='M3'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">classes</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="number(translate(metrics/metric[@id='M4'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:if test="//metric_names/metric_name[@id='M5']/@type='number'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">functions</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of select="number(translate(metrics/metric[@id='M5'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">complexity</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metrics/metric[@id='M5'],',','.'))*number(translate(metrics/metric[@id='M13'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">class_complexity</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metrics/metric[@id='M13'],',','.'))*number(translate(metrics/metric[@id='M5'],',','.')) div number(translate(metrics/metric[@id='M4'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:if test="//metric_names/metric_name[@id='M5']/@type='ratio'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">functions</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metrics/metric[(@id='M5')],',','.'))*number(translate(metrics/metric[(@id='M4')],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">complexity</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metrics/metric[@id='M5'],',','.'))*number(translate(metrics/metric[@id='M4'],',','.'))*number(translate(metrics/metric[@id='M13'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">class_complexity</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metrics/metric[@id='M13'],',','.'))*number(translate(metrics/metric[@id='M5'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">function_complexity</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="number(translate(metrics/metric[@id='M13'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                            </xsl:element>
                            <xsl:element name="size:measure">
                                <xsl:attribute name="key">accessors</xsl:attribute>
                                <xsl:attribute name="value">
                                    <xsl:value-of
                                            select="number(translate(/sourcemonitor_metrics/addings/_files/file[@name=$_file_name]/accessors,',','.'))"/>
                                </xsl:attribute>
                            </xsl:element>
                        </xsl:for-each>
                        <xsl:for-each select="//checkpoints/checkpoint/metrics">
                            <xsl:element name="size:resource">
                                <xsl:attribute name="type">PROJECT</xsl:attribute>
                                <xsl:attribute name="value">
                                    <xsl:value-of select="//project_directory"/>
                                </xsl:attribute>
                                <xsl:if test="//checkpoints/checkpoint[@checkpoint_files]">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">files</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(//checkpoints/checkpoint/@checkpoint_files,',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">lines</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="number(translate(metric[@id='M0'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">statements</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="number(translate(metric[@id='M1'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:if test="//metric_names/metric_name[@id='M3']/@type='percent'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">comment_lines</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="(number(translate(metric[@id='M3'],',','.'))*number(translate(metric[@id='M0'],',','.'))) div 100"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">ncloc</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metric[@id='M0'],',','.'))-number(translate(/sourcemonitor_metrics/addings/_project/blankLines,',','.'))-((number(translate(metric[@id='M3'],',','.'))*number(translate(metric[@id='M0'],',','.'))) div 100)"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:if test="//metric_names/metric_name[@id='M3']/@type='number'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">comment_lines</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of select="number(translate(metric[@id='M3'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">ncloc</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metric[@id='M0'],',','.'))-number(translate(/sourcemonitor_metrics/addings/_project/blankLines,',','.'))-number(translate(metric[@id='M3'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>

                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">classes</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="number(translate(metric[@id='M4'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:if test="//metric_names/metric_name[@id='M5']/@type='number'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">functions</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of select="number(translate(metric[@id='M5'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">complexity</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metric[@id='M5'],',','.'))*number(translate(metric[@id='M13'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">file_complexity</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="(number(translate(metric[@id='M5'],',','.'))*number(translate(metric[@id='M13'],',','.'))) div number(translate(//checkpoints/checkpoint/@checkpoint_files,',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:if test="//metric_names/metric_name[@id='M5']/@type='ratio'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">functions</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metric[(@id='M5')],',','.'))*number(translate(metric[(@id='M4')],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">complexity</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metric[@id='M5'],',','.'))*number(translate(metric[@id='M4'],',','.'))*number(translate(metric[@id='M13'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">file_complexity</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="(number(translate(metric[@id='M5'],',','.'))*number(translate(metric[@id='M4'],',','.'))*number(translate(metric[@id='M13'],',','.'))) div number(translate(//checkpoints/checkpoint/@checkpoint_files,',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>

                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">function_complexity</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="number(translate(metric[@id='M13'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">accessors</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of
                                                select="number(translate(/sourcemonitor_metrics/addings/_project/accessors,',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>

                            </xsl:element>
                        </xsl:for-each>
                    </xsl:if>
                    <xsl:if test="//project_language='C#'">
                        <xsl:for-each select="//files/file">
                            <xsl:variable name="_file_name" select="@file_name"/>
                            <xsl:element name="size:resource">
                                <xsl:attribute name="type">FILE</xsl:attribute>
                                <xsl:attribute name="value">
                                    <xsl:value-of select="@file_name"/>
                                </xsl:attribute>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">lines</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="number(translate(metrics/metric[@id='M0'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">statements</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="number(translate(metrics/metric[@id='M1'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:if test="//metric_names/metric_name[@id='M2']/@type='percent'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">comment_lines</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="(number(translate(metrics/metric[@id='M2'],',','.'))*number(translate(metrics/metric[@id='M0'],',','.')))div 100"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">ncloc</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metrics/metric[@id='M0'],',','.'))-number(translate(/sourcemonitor_metrics/addings/_files/file[@name=$_file_name]/blankLines,',','.'))-((number(translate(metrics/metric[@id='M2'],',','.'))*number(translate(metrics/metric[@id='M0'],',','.')))div 100)"/>
                                        </xsl:attribute>
                                    </xsl:element>

                                </xsl:if>
                                <xsl:if test="//metric_names/metric_name[@id='M2']/@type='number'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">comment_lines</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of select="number(translate(metrics/metric[@id='M2'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">ncloc</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metrics/metric[@id='M0'],',','.'))-number(translate(/sourcemonitor_metrics/addings/_files/file[@name=$_file_name]/blankLines,',','.'))-number(translate(metrics/metric[@id='M2'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">classes</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="number(translate(metrics/metric[@id='M4'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">accessors</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of
                                                select="number(translate(/sourcemonitor_metrics/addings/_files/file[@name=$_file_name]/accessors,',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:if test="//metric_names/metric_name[@id='M5']/@type='number'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">functions</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of select="number(translate(metrics/metric[@id='M5'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">complexity</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metrics/metric[@id='M5'],',','.'))*number(translate(metrics/metric[@id='M14'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">class_complexity</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metrics/metric[@id='M14'],',','.'))*number(translate(metrics/metric[@id='M5'],',','.')) div number(translate(metrics/metric[(@id='M4')],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:if test="//metric_names/metric_name[@id='M5']/@type='ratio'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">functions</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metrics/metric[(@id='M5')],',','.'))*number(translate(metrics/metric[(@id='M4')],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>

                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">complexity</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metrics/metric[@id='M5'],',','.'))*number(translate(metrics/metric[@id='M4'],',','.'))*number(translate(metrics/metric[@id='M14'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">class_complexity</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metrics/metric[@id='M14'],',','.'))*number(translate(metrics/metric[@id='M5'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">function_complexity</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="number(translate(metrics/metric[@id='M14'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                            </xsl:element>
                        </xsl:for-each>
                        <xsl:for-each select="//checkpoints/checkpoint/metrics">
                            <xsl:element name="size:resource">
                                <xsl:attribute name="type">PROJECT</xsl:attribute>
                                <xsl:attribute name="value">
                                    <xsl:value-of select="//project_directory"/>
                                </xsl:attribute>
                                <xsl:if test="//checkpoints/checkpoint[@checkpoint_files]">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">files</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(//checkpoints/checkpoint/@checkpoint_files,',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">lines</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="number(translate(metric[@id='M0'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">statements</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="number(translate(metric[@id='M1'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:if test="//metric_names/metric_name[@id='M2']/@type='percent'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">comment_lines</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="(number(translate(metric[@id='M2'],',','.'))*number(translate(metric[@id='M0'],',','.'))) div 100"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">ncloc</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metric[@id='M0'],',','.'))-number(translate(/sourcemonitor_metrics/addings/_project/blankLines,',','.'))-((number(translate(metric[@id='M2'],',','.'))*number(translate(metric[@id='M0'],',','.'))) div 100)"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:if test="//metric_names/metric_name[@id='M2']/@type='number'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">comment_lines</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of select="number(translate(metric[@id='M2'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">ncloc</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metric[@id='M0'],',','.'))-number(translate(/sourcemonitor_metrics/addings/_project/blankLines,',','.'))-number(translate(metric[@id='M2'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">classes</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="number(translate(metric[@id='M4'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">accessors</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of
                                                select="number(translate(/sourcemonitor_metrics/addings/_project/accessors,',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:if test="//metric_names/metric_name[@id='M5']/@type='number'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">functions</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of select="number(translate(metric[@id='M5'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">complexity</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metric[@id='M5'],',','.'))*number(translate(metric[@id='M14'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">file_complexity</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="(number(translate(metric[@id='M5'],',','.'))*number(translate(metric[@id='M14'],',','.'))) div number(translate(//checkpoints/checkpoint/@checkpoint_files,',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:if test="//metric_names/metric_name[@id='M5']/@type='ratio'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">functions</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metric[(@id='M5')],',','.'))*number(translate(metric[(@id='M4')],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">complexity</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metric[@id='M5'],',','.'))*number(translate(metric[@id='M4'],',','.'))*number(translate(metric[@id='M14'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">file_complexity</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="(number(translate(metric[@id='M5'],',','.'))*number(translate(metric[@id='M4'],',','.'))*number(translate(metric[@id='M14'],',','.'))) div number(translate(//checkpoints/checkpoint/@checkpoint_files,',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>

                            </xsl:element>
                        </xsl:for-each>
                    </xsl:if>
                    <xsl:if test="//project_language='VB.NET'">
                        <xsl:for-each select="//files/file">
                            <xsl:variable name="_file_name" select="@file_name"/>
                            <xsl:element name="size:resource">
                                <xsl:attribute name="type">FILE</xsl:attribute>
                                <xsl:attribute name="value">
                                    <xsl:value-of select="@file_name"/>
                                </xsl:attribute>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">lines</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="number(translate(metrics/metric[@id='M0'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">statements</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="number(translate(metrics/metric[@id='M1'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:if test="//metric_names/metric_name[@id='M2']/@type='percent'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">comment_lines</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="(number(translate(metrics/metric[@id='M2'],',','.'))*number(translate(metrics/metric[@id='M0'],',','.')))div 100"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">ncloc</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metrics/metric[@id='M0'],',','.'))-number(translate(/sourcemonitor_metrics/addings/_files/file[@name=$_file_name]/blankLines,',','.'))-((number(translate(metrics/metric[@id='M2'],',','.'))*number(translate(metrics/metric[@id='M0'],',','.')))div 100)"/>
                                        </xsl:attribute>
                                    </xsl:element>

                                </xsl:if>
                                <xsl:if test="//metric_names/metric_name[@id='M2']/@type='number'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">comment_lines</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of select="number(translate(metrics/metric[@id='M2'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">ncloc</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metrics/metric[@id='M0'],',','.'))-number(translate(/sourcemonitor_metrics/addings/_files/file[@name=$_file_name]/blankLines,',','.'))-number(translate(metrics/metric[@id='M2'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">classes</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="number(translate(metrics/metric[@id='M4'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">accessors</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of
                                                select="number(translate(/sourcemonitor_metrics/addings/_files/file[@name=$_file_name]/accessors,',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:if test="//metric_names/metric_name[@id='M5']/@type='number'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">functions</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of select="number(translate(metrics/metric[@id='M5'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">complexity</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metrics/metric[@id='M5'],',','.'))*number(translate(metrics/metric[@id='M11'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">class_complexity</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metrics/metric[@id='M11'],',','.'))*number(translate(metrics/metric[@id='M5'],',','.')) div number(translate(metrics/metric[(@id='M4')],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:if test="//metric_names/metric_name[@id='M5']/@type='ratio'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">functions</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metrics/metric[(@id='M5')],',','.'))*number(translate(metrics/metric[(@id='M4')],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>

                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">complexity</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metrics/metric[@id='M5'],',','.'))*number(translate(metrics/metric[@id='M4'],',','.'))*number(translate(metrics/metric[@id='M11'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">class_complexity</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metrics/metric[@id='M11'],',','.'))*number(translate(metrics/metric[@id='M5'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">function_complexity</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="number(translate(metrics/metric[@id='M11'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                            </xsl:element>
                        </xsl:for-each>
                        <xsl:for-each select="//checkpoints/checkpoint/metrics">
                            <xsl:element name="size:resource">
                                <xsl:attribute name="type">PROJECT</xsl:attribute>
                                <xsl:attribute name="value">
                                    <xsl:value-of select="//project_directory"/>
                                </xsl:attribute>
                                <xsl:if test="//checkpoints/checkpoint[@checkpoint_files]">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">files</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(//checkpoints/checkpoint/@checkpoint_files,',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">lines</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="number(translate(metric[@id='M0'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">statements</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="number(translate(metric[@id='M1'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:if test="//metric_names/metric_name[@id='M2']/@type='percent'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">comment_lines</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="(number(translate(metric[@id='M2'],',','.'))*number(translate(metric[@id='M0'],',','.'))) div 100"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">ncloc</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metric[@id='M0'],',','.'))-number(translate(/sourcemonitor_metrics/addings/_project/blankLines,',','.'))-((number(translate(metric[@id='M2'],',','.'))*number(translate(metric[@id='M0'],',','.'))) div 100)"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:if test="//metric_names/metric_name[@id='M2']/@type='number'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">comment_lines</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of select="number(translate(metric[@id='M2'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">ncloc</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metric[@id='M0'],',','.'))-number(translate(/sourcemonitor_metrics/addings/_project/blankLines,',','.'))-number(translate(metric[@id='M2'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">classes</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="number(translate(metric[@id='M4'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">accessors</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of
                                                select="number(translate(/sourcemonitor_metrics/addings/_project/accessors,',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:if test="//metric_names/metric_name[@id='M5']/@type='number'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">functions</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of select="number(translate(metric[@id='M5'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">complexity</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metric[@id='M5'],',','.'))*number(translate(metric[@id='M11'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">file_complexity</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="(number(translate(metric[@id='M5'],',','.'))*number(translate(metric[@id='M11'],',','.'))) div number(translate(//checkpoints/checkpoint/@checkpoint_files,',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:if test="//metric_names/metric_name[@id='M5']/@type='ratio'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">functions</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metric[(@id='M5')],',','.'))*number(translate(metric[(@id='M4')],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">complexity</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metric[@id='M5'],',','.'))*number(translate(metric[@id='M4'],',','.'))*number(translate(metric[@id='M11'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">file_complexity</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="(number(translate(metric[@id='M5'],',','.'))*number(translate(metric[@id='M4'],',','.'))*number(translate(metric[@id='M11'],',','.'))) div number(translate(//checkpoints/checkpoint/@checkpoint_files,',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>

                            </xsl:element>
                        </xsl:for-each>
                    </xsl:if>
                    <xsl:if test="//project_language='HTML'">
                        <xsl:for-each select="//files/file">
                            <xsl:variable name="_file_name" select="@file_name"/>
                            <xsl:element name="size:resource">
                                <xsl:attribute name="type">FILE</xsl:attribute>
                                <xsl:attribute name="value">
                                    <xsl:value-of select="@file_name"/>
                                </xsl:attribute>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">lines</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="number(translate(metrics/metric[@id='M0'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:if test="//metric_names/metric_name[@id='M1']/@type='number'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">comment_lines</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of select="number(translate(metrics/metric[@id='M1'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">ncloc</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metrics/metric[@id='M0'],',','.'))-number(translate(/sourcemonitor_metrics/addings/_files/file[@name=$_file_name]/blankLines,',','.'))-number(translate(metrics/metric[@id='M1'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:if test="//metric_names/metric_name[@id='M1']/@type='percent'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">comment_lines</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metrics/metric[@id='M1'],',','.'))*number(translate(metrics/metric[@id='M0'],',','.')) div 100"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">ncloc</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metrics/metric[@id='M0'],',','.'))-number(translate(/sourcemonitor_metrics/addings/_files/file[@name=$_file_name]/blankLines,',','.'))-(number(translate(metrics/metric[@id='M1'],',','.'))*number(translate(metrics/metric[@id='M0'],',','.')) div 100)"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                            </xsl:element>
                        </xsl:for-each>
                        <xsl:for-each select="//checkpoints/checkpoint/metrics">
                            <xsl:element name="size:resource">
                                <xsl:attribute name="type">PROJECT</xsl:attribute>
                                <xsl:attribute name="value">
                                    <xsl:value-of select="//project_directory"/>
                                </xsl:attribute>
                                <xsl:if test="//checkpoints/checkpoint[@checkpoint_files]">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">files</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(//checkpoints/checkpoint/@checkpoint_files,',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:element name="size:measure">
                                    <xsl:attribute name="key">lines</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="number(translate(metric[@id='M0'],',','.'))"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:if test="//metric_names/metric_name[@id='M1']/@type='number'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">comment_lines</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of select="number(translate(metric[@id='M1'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">ncloc</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metric[@id='M0'],',','.'))-number(translate(/sourcemonitor_metrics/addings/_project/blankLines,',','.'))-number(translate(metric[@id='M1'],',','.'))"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                                <xsl:if test="//metric_names/metric_name[@id='M1']/@type='percent'">
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">comment_lines</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="(number(translate(metric[@id='M1'],',','.'))*number(translate(metric[@id='M0'],',','.'))) div 100"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                    <xsl:element name="size:measure">
                                        <xsl:attribute name="key">ncloc</xsl:attribute>
                                        <xsl:attribute name="value">
                                            <xsl:value-of
                                                    select="number(translate(metric[@id='M0'],',','.'))-number(translate(/sourcemonitor_metrics/addings/_project/blankLines,',','.'))-((number(translate(metric[@id='M1'],',','.'))*number(translate(metric[@id='M0'],',','.'))) div 100)"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:if>
                            </xsl:element>
                        </xsl:for-each>
                    </xsl:if>
                </xsl:element>
            </xsl:element>
        </tusar:tusar>
    </xsl:template>
</xsl:stylesheet>


