<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:xs="http://www.w3.org/2001/XMLSchema"
   xmlns:cmd="http://www.clarin.eu/cmd/"
   exclude-result-prefixes="#all" version="2.0">
    
   <xsl:template match="/">
       <doc>
           <add>
              <field name="id">
                 <xsl:value-of select="//cmd:GeneralInfo/cmd:PID"/>
              </field>
              <field name="name">
                 <xsl:value-of select="//cmd:GeneralInfo/concat(cmd:Title, concat(' (', cmd:Name, ')')[current()/cmd:Title != current()/cmd:Name])"/>
              </field>
              <field name="description">
                 <xsl:value-of select="//cmd:GeneralInfo/cmd:Description"/>
              </field>
              <xsl:for-each select="distinct-values(//cmd:GeneralInfo/cmd:Keyword)">
                 <field name="keyword">
                    <xsl:value-of select="."/>
                 </field>
              </xsl:for-each>
              <field name="publicationdate">
                 <xsl:value-of select="//cmd:GeneralInfo/cmd:PublicationDate"/>
              </field>
           </add>
       </doc>
   </xsl:template>
   
</xsl:stylesheet>