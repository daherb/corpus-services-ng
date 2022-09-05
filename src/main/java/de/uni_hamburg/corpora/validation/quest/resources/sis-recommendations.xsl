<?xml version="1.0"?>
<!--
XML transformation to convert the multiple CLARIN sis files into one single coonfiguration used for the checker
Before using, clone the CLARIN sis repository https://github.com/clarin-eric/standards/ and adjust the path variable
requires Saxon 9 to run, e.g.
e.g. saxon9-transform -xsl:sis-recommendations.xsl clarin-sis/SIS/clarin/data/centres.xml | xml fo > sis-recommendations.xml
-->
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <!-- the path to the SIS documents -->
  <xsl:variable name="path" select="'clarin-sis/SIS/clarin/data'" />

  <!-- read all the centres -->
  <xsl:template name="readCentres">
    <xsl:variable name="centreDoc" select="document(concat($path,'/centres.xml'))" />
    <centres>
    <xsl:for-each select="$centreDoc//centre">
      <centre>
      <id><xsl:value-of select="@id" /></id>
      <name><xsl:value-of select="./name" /></name>
      <url><xsl:value-of select="./a/@href" /> </url>
      </centre>
    </xsl:for-each>
    </centres>
  </xsl:template>

  <!-- for each centre read all the formats supported -->
  <xsl:template name="readFormats">
    <xsl:param name="centreIds" />
    <formats>
      <xsl:for-each select="$centreIds">
	<xsl:variable name="centreId" select="text()" />
	<xsl:variable name="centreFormats" select="document(concat($path,'/recommendations/',$centreId,'-recommendation.xml'))" />
	<xsl:for-each select="$centreFormats//format">
	  <format>
	    <centreInfo>
	      <id><xsl:value-of select="$centreId" /></id>
	      <xsl:copy-of select="domain" />
	      <xsl:copy-of select="level" />
	    </centreInfo>
	    <formatInfo>
	      <id>
		<xsl:value-of select="@id" />
	      </id>
	    </formatInfo>
	  </format>
	</xsl:for-each>
      </xsl:for-each>
    </formats>
  </xsl:template>

  <!-- update centre formats to include all necessary information -->
  <xsl:template name="updateFormats">
    <xsl:param name="allCentres" />
    <xsl:param name="allFormats" />
    <formats>
      <xsl:for-each select="distinct-values($allFormats//formatInfo/id)">
	<xsl:sort select="." />
	<xsl:variable name="formatId"><xsl:value-of select="." /></xsl:variable>
	<!-- gets all information about the format -->
	<xsl:variable name="formatInfo" select="document(concat($path,'/formats/',$formatId,'.xml'))" />
	<xsl:variable name="formatAbbr"> <xsl:value-of select="$formatInfo//abbr" /> </xsl:variable>
	<!-- only print format if the abbreviation is not empty -->
	<xsl:if test="string-length($formatAbbr) > 0">
	  <format>
	    <formatInfo>
	      <abbr><xsl:value-of select="$formatInfo//abbr" /></abbr>
	      <name><xsl:value-of select="$formatInfo//title" /></name>
	      <!-- both mime types and file extensions can have several values -->
	      <mimeTypes>
		<xsl:copy-of select="$formatInfo//mimeType" />
	      </mimeTypes>
	      <fileExts>
		<xsl:copy-of select="$formatInfo//fileExt" />
	      </fileExts>
	    </formatInfo>
	    <centreInfo>
	      <xsl:for-each select="$allFormats//format[formatInfo/id/text()=$formatId]">
		<centre>
		  <xsl:variable name="centreId"><xsl:value-of select="centreInfo/id" /></xsl:variable>
		  <!-- all relevant info about the centre -->
		  <xsl:copy-of select="centreInfo/id" />
		  <xsl:copy-of select="$allCentres//centre[id/text()=$centreId]/name" />
		  <xsl:copy-of select="$allCentres//centre[id/text()=$centreId]/url" />
		  <!-- al info about the format use in the centre -->
		  <xsl:copy-of select="centreInfo/domain" />
		  <xsl:copy-of select="centreInfo/level" />
		</centre>
	      </xsl:for-each>
	    </centreInfo>
	  </format>
      </xsl:if>
      </xsl:for-each>
    </formats>
  </xsl:template>
    
  <!-- "main function" --> 
  <xsl:template match="/">
    <xsl:variable name="allCentres"><xsl:call-template name="readCentres" /></xsl:variable>
    <xsl:variable name="allCentreIds" select="$allCentres//id" />
    <xsl:variable name="allCentreFormats">
      <xsl:call-template name="readFormats">
	<xsl:with-param name="centreIds" select="$allCentreIds" />
      </xsl:call-template>
    </xsl:variable>
    <xsl:copy>
      <xsl:call-template name="updateFormats">
	<xsl:with-param name="allCentres" select="$allCentres" />
	<xsl:with-param name="allFormats" select="$allCentreFormats" />
      </xsl:call-template>
    </xsl:copy>
  </xsl:template>
</xsl:stylesheet>
