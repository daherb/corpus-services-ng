<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    exclude-result-prefixes="xs"
    version="2.0">
    
    <!-- Is the VisualizationFormat still needed? -->
    <xsl:variable name="CSS_PATH" select="concat($TOP_LEVEL_PATH, 'VisualizationFormat.css')" as="xs:string"/>
    <xsl:variable name="CSS_PATH_LIST" select="css/ListGATFormat.css"/>
    <xsl:template match="/">
        <html>
            <head>
                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
                <link rel="stylesheet" type="text/css" href="{$CSS_PATH_LIST}"/>         
                <xsl:call-template name="INSERT_JAVA_SCRIPT"/>                
            </head>
            <body>
                <xsl:call-template name="MAKE_TITLE"/>
                <!-- ... with one table... -->
                <div id="main">
                    <table>
                        <!-- ... and process the speaker contributions -->
                        <xsl:apply-templates select="//gat-line"/>
                    </table>
                    <p> <br/> <br/> <br/> <br/> <br/> <br/> <br/> <br/> <br/> <br/> <br/> <br/> <br/> <br/> <br/> <br/> <br/> <br/> <br/> <br/> <br/> <br/> <br/> <br/> <br/></p>					
                </div>			
            </body>
        </html>
    </xsl:template>
    
    <!-- <gat-line start="3137.6343039999997" end="3501.824" number="007">
        <speaker><![CDATA[A:]]></speaker>
        <text><![CDATA[sicher tobi! ]]></text>
    </gat-line> -->    
    <xsl:template match="gat-line">
        <tr>
            <xsl:attribute name="data-start" select="@start"/>
            <xsl:attribute name="data-end" select="@end"/>
            <!-- ... with one cell for audio link ... -->
            <td class="audioLink">
                <xsl:element name="a">
                    <xsl:attribute name="onClick">
                        <xsl:text>jump('</xsl:text>
                        <xsl:value-of select="format-number((@start - 0.1), '#.##')"/><xsl:text>');</xsl:text>
                    </xsl:attribute>
                    <xsl:attribute name="title">Click to start player at <xsl:value-of select="@start"/></xsl:attribute>
                    <xsl:text>&#x00A0;</xsl:text>
                </xsl:element>
            </td>
            
            
            <!-- ... with one cell for numbering ... -->
            <td class="numbering">
                <xsl:value-of select="@number"/>
            </td>
            
            
            <!-- ... one cell for the speaker abbreviation ... -->
            <td class="abbreviation">
                <xsl:value-of select="translate(speaker/text(), ' ', '&#x00A0;')"/>                
            </td>
        
            <!-- ... one cell for the text ... -->
            <td class="text">
                <xsl:value-of select="translate(text/text(), ' ', '&#x00A0;')"/>                
            </td>
        </tr>
            
    </xsl:template>
    
    <!-- makes the navigation bar displayed at the top of diverse documents -->
    <xsl:template name="MAKE_TITLE">
        <div id="head">
            <xsl:call-template name="EMBED_AUDIO_PLAYER"/>            
        </div>		
    </xsl:template>
    
    
    <xsl:template name="EMBED_AUDIO_PLAYER">
        <audio controls="controls">
            <xsl:element name="source">
                <xsl:attribute name="src"><xsl:value-of select="/*/@audio"/></xsl:attribute>
                <xsl:attribute name="type">audio/wav</xsl:attribute>
            </xsl:element>
        </audio>				
    </xsl:template>
    
    <xsl:template name="INSERT_JAVA_SCRIPT">
        <script type="text/javascript">                    
            <xsl:text disable-output-escaping="yes" >
            <![CDATA[
            function jump(time){
                document.getElementsByTagName('audio')[0].currentTime=time;
                document.getElementsByTagName('audio')[0].play();
            }			
            
            function stop(){
                document.getElementsByTagName('audio')[0].pause();
            }
                        
            function registerAudioListener(){
                document.getElementsByTagName('audio')[0].addEventListener("timeupdate", updateTime, true);                     
                document.getElementsByTagName('audio')[0].addEventListener("onpause", updateTime, true);                     
            }
            
            function updateTime(){
                var player = document.getElementsByTagName('audio')[0]; 
                var elapsedTime = player.currentTime;        
                var trs = document.getElementsByTagName('tr');
                for (var i = 0; i < trs.length; i++) {
                    tr = trs[i];
                    start = tr.getAttribute('data-start');
                    end = tr.getAttribute('data-end');
                    if ((!player.paused) && (start < elapsedTime) && (end > elapsedTime)){
                        var children = tr.children;
                        for (var j = 0; j < children.length; j++) {
                          var td = children[j];
                          td.style.backgroundColor = '#E0FFFF';
                        }                        
                    } else {
                        var children = tr.children;
                        for (var j = 0; j < children.length; j++) {
                          var td = children[j];
                          td.style.backgroundColor='';
                        }                        
                    }
                }                        
            }
            window.addEventListener("DOMContentLoaded", registerAudioListener, false);
            ]]>
            </xsl:text>
        </script>                
        
    </xsl:template>
    
    
</xsl:stylesheet>