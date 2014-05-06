<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:coverage="http://www.thalesgroup.com/tusar/coverage/v4"
                xmlns:line-coverage="http://www.thalesgroup.com/tusar/line-coverage/v1"
                version="2.0"
        >
    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>


    <xsl:template match="coverage">

        <tusar:tusar
                xmlns:coverage="http://www.thalesgroup.com/tusar/coverage/v4"
                xmlns:line-coverage="http://www.thalesgroup.com/tusar/line-coverage/v1"
                xmlns:tusar="http://www.thalesgroup.com/tusar/v10"
                version="10.0">

            <xsl:element name="tusar:coverage">

                <xsl:attribute name="toolname">purecoverage</xsl:attribute>
                <xsl:element name="coverage:line-coverage">
                    <xsl:for-each select="/coverage/source">
                        <xsl:element name="line-coverage:file">
                            <xsl:attribute name="path">
                                <xsl:value-of select="@file"/>
                            </xsl:attribute>

                            <xsl:for-each select="line">
                                <xsl:element name="line-coverage:line">

                                    <xsl:attribute name="number">
                                        <xsl:value-of select="substring-before(@LineNumber,'.')"/>
                                    </xsl:attribute>

                                    <xsl:attribute name="hits">
                                        <xsl:value-of select="@LineCoverage"/>
                                    </xsl:attribute>

                                </xsl:element>
                            </xsl:for-each>
                        </xsl:element>

                    </xsl:for-each>
                </xsl:element>


            </xsl:element>
        </tusar:tusar>

    </xsl:template>
</xsl:stylesheet>
