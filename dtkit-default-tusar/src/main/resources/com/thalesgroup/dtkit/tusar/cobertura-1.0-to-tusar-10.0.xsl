<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:coverage="http://www.thalesgroup.com/tusar/coverage/v4"
                xmlns:line-coverage="http://www.thalesgroup.com/tusar/line-coverage/v1"
                xmlns:branch-coverage="http://www.thalesgroup.com/tusar/branch-coverage/v1"
                version="2.0"
        >
    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>


    <xsl:template match="coverage">

        <tusar:tusar
                xmlns:coverage="http://www.thalesgroup.com/tusar/coverage/v4"
                xmlns:line-coverage="http://www.thalesgroup.com/tusar/line-coverage/v1"
                xmlns:branch-coverage="http://www.thalesgroup.com/tusar/branch-coverage/v1"
                xmlns:tusar="http://www.thalesgroup.com/tusar/v10"
                version="10.0">

            <xsl:element name="tusar:coverage">

                <xsl:attribute name="toolname">cobertura</xsl:attribute>
                <xsl:element name="coverage:line-coverage">
                    <xsl:for-each-group select="packages/package/classes/class" group-by="@filename">
                        <xsl:variable name="filename" select="@filename"/>
                        <xsl:if test="/coverage/packages/package/classes/class[@filename = $filename]/lines/line">
                            <xsl:element name="line-coverage:file">
                                <xsl:attribute name="path">
                                    <xsl:value-of select="@filename"/>
                                </xsl:attribute>

                                <xsl:for-each
                                        select="/coverage/packages/package/classes/class[@filename = $filename]/lines/line">
                                    <xsl:element name="line-coverage:line">

                                        <xsl:attribute name="number">
                                            <xsl:value-of select="@number"/>
                                        </xsl:attribute>

                                        <xsl:attribute name="hits">
                                            <xsl:value-of select="@hits"/>
                                        </xsl:attribute>

                                    </xsl:element>
                                </xsl:for-each>
                            </xsl:element>
                        </xsl:if>
                    </xsl:for-each-group>
                </xsl:element>

                <xsl:element name="coverage:branch-coverage">

                    <xsl:for-each-group select="packages/package/classes/class" group-by="@filename">
                        <xsl:variable name="filename" select="@filename"/>
                        <xsl:if test="/coverage/packages/package/classes/class[@filename = $filename]/lines/line[@branch = 'true']">
                            <xsl:element name="branch-coverage:resource">
                                <xsl:attribute name="fullname">
                                    <xsl:value-of select="@filename"/>
                                </xsl:attribute>
                                <xsl:for-each
                                        select="/coverage/packages/package/classes/class[@filename = $filename]/lines/line[@branch = 'true']">
                                    <xsl:element name="branch-coverage:line">
                                        <xsl:attribute name="number">
                                            <xsl:value-of select="@number"/>
                                        </xsl:attribute>

                                        <xsl:variable name="first-substring"
                                                      select="substring-after(@condition-coverage,'(')"/>
                                        <xsl:variable name="coverage-info"
                                                      select="substring-before($first-substring,')')"/>
                                        <xsl:variable name="numberOfBranches"
                                                      select="substring-after($coverage-info,'/')"/>
                                        <xsl:variable name="coveredBranches"
                                                      select="substring-before($coverage-info,'/')"/>
                                        <xsl:variable name="uncoveredBranches"
                                                      select="number($numberOfBranches) - number($coveredBranches)"/>

                                        <xsl:attribute name="numberOfBranches">
                                            <xsl:value-of select="$numberOfBranches"/>
                                        </xsl:attribute>

                                        <xsl:attribute name="uncoveredBranches">
                                            <xsl:value-of select="$uncoveredBranches"/>
                                        </xsl:attribute>
                                    </xsl:element>
                                </xsl:for-each>
                            </xsl:element>
                        </xsl:if>
                    </xsl:for-each-group>
                </xsl:element>
            </xsl:element>
        </tusar:tusar>

    </xsl:template>
</xsl:stylesheet>