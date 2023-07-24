<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">

  <!--<xsl:variable name="document-uri" select="document-uri(.)"/>
<xsl:variable name="filename" select="(tokenize($document-uri,'/'))[last()]"/>
<xsl:variable name="newfilename" select="substring-before($filename, '.')"/>-->

  <!-- Identity template, copies everything as is -->
  <xsl:template match="@* | node()">
    <xsl:copy>
      <xsl:apply-templates select="@* | node()"/>
    </xsl:copy>
  </xsl:template>

  <!-- Override for target element -->
  <xsl:template match="referenced-file">
    <!-- Add new node (or whatever else you wanna do) -->
    <xsl:if test="contains(@url, '.wav')">
    <xsl:variable name="filename" select="substring-before(@url, '.wav')"/>
    <referenced-file>
      <xsl:attribute name="url"><xsl:value-of select="$filename"/>.wav</xsl:attribute>
    </referenced-file>
    <referenced-file>
      <xsl:attribute name="url"><xsl:value-of select="$filename"/>.mp3</xsl:attribute>
    </referenced-file>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
