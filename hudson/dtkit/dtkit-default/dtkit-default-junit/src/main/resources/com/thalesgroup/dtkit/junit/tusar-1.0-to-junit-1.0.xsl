<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:fn="http://www.w3.org/2005/xpath-functions">
	<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>

	<xsl:template match="/">

		<xsl:element name="testsuites">

			<xsl:for-each select="tusar/tests/testsuite">

				<xsl:element name="testsuite">

					<xsl:attribute name="name">
						<xsl:value-of select="@name" />
					</xsl:attribute>

					<xsl:attribute name="tests">
						<xsl:value-of select="@tests" />
					</xsl:attribute>

					<xsl:attribute name="failures">
						<xsl:value-of select="@failures" />
					</xsl:attribute>

					<xsl:attribute name="errors">
						<xsl:value-of select="@errors" />
					</xsl:attribute>

					<xsl:attribute name="skipped">
						<xsl:value-of select="@skipped" />
					</xsl:attribute>

					<xsl:attribute name="time">
						<xsl:value-of select="@time" />
					</xsl:attribute>

					<xsl:attribute name="timestamp">
						<xsl:value-of select="@timestamp" />
					</xsl:attribute>

					<xsl:if test="properties">
						<xsl:element name="properties">
							<xsl:for-each select="properties/property">
								<xsl:element name="property">

									<xsl:attribute name="name">
										<xsl:value-of select="@name" />
									</xsl:attribute>

									<xsl:attribute name="value">
										<xsl:value-of select="@value" />
									</xsl:attribute>

								</xsl:element>
							</xsl:for-each>
						</xsl:element>
					</xsl:if>

					<xsl:for-each select="testcase">
						<xsl:element name="testcase">

							<xsl:attribute name="classname">
								<xsl:choose>
									<xsl:when test="@fulltestname">
										<xsl:value-of select="@fulltestname" />
									</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="@filepath" />
									</xsl:otherwise>
								</xsl:choose>
							</xsl:attribute>

							<xsl:attribute name="name">
								<xsl:value-of select="@testname" />
							</xsl:attribute>

							<xsl:attribute name="time">
								<xsl:value-of select="@time" />
							</xsl:attribute>

							<xsl:attribute name="assertions">
								<xsl:value-of select="@assertions" />
							</xsl:attribute>

							<xsl:if test="error">
								<xsl:element name="error">
									<xsl:attribute name="message">
										<xsl:value-of select="error/@message" />
									</xsl:attribute>

									<xsl:attribute name="type">
										<xsl:value-of select="error/@type" />
									</xsl:attribute>

									<xsl:value-of select="error" />
								</xsl:element>
							</xsl:if>

							<xsl:if test="failure">
								<xsl:element name="failure">
									<xsl:attribute name="message">
										<xsl:value-of select="failure/@message" />
									</xsl:attribute>

									<xsl:attribute name="type">
										<xsl:value-of select="failure/@type" />
									</xsl:attribute>

									<xsl:value-of select="failure" />
								</xsl:element>
							</xsl:if>

							<xsl:if test="system-out">
								<xsl:element name="system-out">
									<xsl:value-of select="system-out" />
								</xsl:element>
							</xsl:if>

							<xsl:if test="system-err">
								<xsl:element name="system-err">
									<xsl:value-of select="system-err" />
								</xsl:element>
							</xsl:if>

						</xsl:element>
					</xsl:for-each>

					<xsl:if test="system-out">
						<xsl:element name="system-out">
							<xsl:value-of select="system-out" />
						</xsl:element>
					</xsl:if>

					<xsl:if test="system-err">
						<xsl:element name="system-err">
							<xsl:value-of select="system-err" />
						</xsl:element>
					</xsl:if>

				</xsl:element>
			</xsl:for-each>
		</xsl:element>
	</xsl:template>
</xsl:stylesheet>