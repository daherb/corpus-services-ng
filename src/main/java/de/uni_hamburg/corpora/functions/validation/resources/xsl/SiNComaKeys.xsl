<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">

  <!-- Identity template, copies everything as is -->
  <xsl:template match="@* | node()">
    <xsl:copy>
      <xsl:apply-templates select="@* | node()"/>
    </xsl:copy>
  </xsl:template>


  <!-- Override for target element -->
  <xsl:template match="Speaker/Location[not(@Type)]">
    <xsl:variable name="locationdesc" select="Description/Key[@Name = 'Name']/text()"/>
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:attribute name="Type">
        <xsl:value-of select="$locationdesc"/>
      </xsl:attribute>
      <xsl:apply-templates select="node()"/>
    </xsl:copy>
    <!--<xsl:copy>
      <xsl:apply-templates/>
    </xsl:copy>-->
    <!-- Add new node (or whatever else you wanna do) -->
    <!-- <xsl:if test="descendant::code='SCBP' or preceding-sibling::policyKey/policyFormCd/code='UB'">-->
    <!-- <xsl:variable name="erhebungsort" select="Key[@Name = 'Ort']/text()"/>
    <xsl:variable name="erhebungsplz" select="Key[@Name = 'PLZ']/text()"/>-->
    <!--<xsl:if test="Description/Key[@Name = 'Name']">
<Location>
      <xsl:attribute name="Type">
        <xsl:value-of select="$locationdesc"/>
      </xsl:attribute>
      <xsl:copy>
        <xsl:apply-templates/>
      </xsl:copy>
</Location>
    </xsl:if>
  </xsl:template>-->
    <!--  <!-\- Override for target element -\->
  <xsl:template match="Speaker/Description">
    <xsl:copy>
      <xsl:apply-templates/>
    </xsl:copy>
    <!-\- Add new node (or whatever else you wanna do) -\->
    <!-\- <xsl:if test="descendant::code='SCBP' or preceding-sibling::policyKey/policyFormCd/code='UB'">-\->
    <xsl:variable name="erhebungsort" select="Key[@Name = 'Ort']/text()"/>
    <xsl:variable name="erhebungsplz" select="Key[@Name = 'PLZ']/text()"/>
    <xsl:if test="Key[@Name = 'Ort']">
      <Location Type="Erhebungsort">
        <City>
          <xsl:value-of select="$erhebungsort"/>
        </City>
        <Postalcode>
          <xsl:value-of select="$erhebungsplz"/>
        </Postalcode>
        <Description/>
      </Location>
    </xsl:if>-->
  </xsl:template>

</xsl:stylesheet>
