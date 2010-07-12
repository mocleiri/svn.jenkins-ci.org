<?xml version="1.0" encoding="UTF-8"?>
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

            <xsl:element name="measures">

                <xsl:for-each select="//FILE">

                    <xsl:element name="resource">
                        <xsl:attribute name="type">
                            <xsl:text>FILE</xsl:text>
                        </xsl:attribute>

                        <xsl:attribute name="value">
                            <xsl:value-of select="substring(@name, 3)"/>
                        </xsl:attribute>

                        <xsl:for-each select="METRIC">

                            <xsl:element name="measure">

                                <xsl:choose>
                                    <xsl:when test="contains(@name, 'LT')">
                                        <xsl:attribute name="key">
                                            <xsl:text>LINES</xsl:text>
                                        </xsl:attribute>
                                    </xsl:when>

                                    <xsl:when test="contains(@name, 'LS')">
                                        <xsl:attribute name="key">
                                            <xsl:text>NCLOC</xsl:text>
                                        </xsl:attribute>
                                    </xsl:when>

                                    <xsl:otherwise>
                                        <xsl:attribute name="key">
                                            <xsl:value-of select="@name"/>
                                        </xsl:attribute>
                                    </xsl:otherwise>

                                </xsl:choose>

                                <xsl:attribute name="value">
                                    <xsl:value-of select="@value"/>
                                </xsl:attribute>

                            </xsl:element>
                        </xsl:for-each>
                    </xsl:element>

                </xsl:for-each>
            </xsl:element>
        </xsl:element>
    </xsl:template>

</xsl:stylesheet>
