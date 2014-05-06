<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>
    <xsl:param name="myParameter"/>

    <xsl:template match="/">
        <root>
            <xsl:value-of select="$myParameter"/>
        </root>
    </xsl:template>
</xsl:stylesheet>