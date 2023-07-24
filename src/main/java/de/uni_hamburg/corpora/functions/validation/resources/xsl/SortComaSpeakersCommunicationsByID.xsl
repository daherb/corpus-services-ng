<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="yes" omit-xml-declaration="yes"/>
    
    <!-- Identity template, copies everything as is -->
    <xsl:template match="@* | node()">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>
    
    
    <!-- Override for target element -->
   <!-- <xsl:template match="Speaker/Location[not(@Type)]">
        <xsl:variable name="locationdesc" select="Description/Key[@Name = 'Name']/text()"/>
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            <xsl:attribute name="Type">
                <xsl:value-of select="$locationdesc"/>
            </xsl:attribute>
            <xsl:apply-templates select="node()"/>
        </xsl:copy>
    </xsl:template>   -->

    <xsl:template match="CorpusData">
        <xsl:copy>
            <xsl:apply-templates select="Speaker">
                <xsl:sort select="@Id"/>               
            </xsl:apply-templates>
            <xsl:apply-templates select="Communication">
                <xsl:sort select="@Id"/>
            </xsl:apply-templates>
        </xsl:copy>
    </xsl:template>
    

</xsl:stylesheet>
