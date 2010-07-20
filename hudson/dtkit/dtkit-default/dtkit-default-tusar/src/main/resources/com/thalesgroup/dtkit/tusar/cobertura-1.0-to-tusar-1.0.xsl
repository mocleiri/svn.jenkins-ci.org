<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:fn="http://www.w3.org/2005/xpath-functions">
    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>

    <xsl:template match="coverage">

        <xsl:element name="tusar">
            <xsl:attribute name="xmlns_xsi">
                <xsl:text>http://www.w3.org/2001/XMLSchema-instance</xsl:text>
            </xsl:attribute>
            <xsl:attribute name="version">
                <xsl:text>1.0</xsl:text>
            </xsl:attribute>

            <xsl:element name="coverage">

                <xsl:for-each select="packages/package/classes/class">

                    <xsl:element name="file">
                        <xsl:attribute name="path">
                            <xsl:value-of select="@filename"/>
                        </xsl:attribute>

                        <xsl:for-each select="lines/line">
                            <xsl:element name="line">

                                <xsl:attribute name="number">
                                    <xsl:value-of select="@number"/>
                                </xsl:attribute>

                                <xsl:attribute name="hits">
                                    <xsl:value-of select="@hits"/>
                                </xsl:attribute>

                            </xsl:element>
                        </xsl:for-each>
                    </xsl:element>
                </xsl:for-each>
            </xsl:element>
        </xsl:element>

    </xsl:template>
</xsl:stylesheet>