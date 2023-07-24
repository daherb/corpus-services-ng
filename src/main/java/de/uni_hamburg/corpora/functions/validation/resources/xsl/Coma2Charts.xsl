<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:hzsk-pi="https://corpora.uni-hamburg.de/hzsk/xmlns/processing-instruction" 
    exclude-result-prefixes="#all" version="2.0">
    
    
    <xsl:param name="corpus-name" select="'EXMARaLDA Corpus'" as="xs:string"/>
    <xsl:param name="mode" select="'default'" as="xs:string"/>
    
    <xsl:variable name="root" select="/"/>
    
    
    <xsl:template match="/">
       <html>
       
       <head>
           <title>Charts for <xsl:value-of select="$corpus-name"/></title>
           <script type="text/javascript" charset="utf8" src="https://inel.corpora.uni-hamburg.de/charts/js/Chart-2.8.0.js"/>
           <script type="text/javascript" charset="utf8" src="https://inel.corpora.uni-hamburg.de/charts/js/Chart-options.js"/>
           <style>
            canvas {
                 -moz-user-select: none;
                 -webkit-user-select: none;
                 -ms-user-select: none;
            }
           </style>
       </head>
       <body>
           <div class="container" style="width: 75%">
           <canvas id="Words/Communications-per-Genre" height="250"></canvas>
           </div>
           <div class="container" style="width: 75%">
           	<canvas id="Words-per-Dialect" height="250"></canvas>
           </div>
           <div class="container" style="width: 75%">
           	<canvas id="Words/Communications-per-Decade" height="250"></canvas>
           </div>
           <div class="container" style="width: 75%">
           	<canvas id="Speakers-per-Sex" height="200"></canvas>
           </div>
           <script>
           	window.onload = function() {
           		window.myBar = new Chart(document.getElementById('Words/Communications-per-Genre').getContext('2d'), {
         "data" : {
           "labels" : [ 
            <xsl:for-each select="distinct-values(//*:Communication/*:Description/*:Key[@Name='1 Genre'])">
                <xsl:sort select="sum($root//*:Communication[*:Description/*:Key[@Name='1 Genre'] = current()]/*:Transcription/*:Description/*:Key[@Name='# HIAT:w'])" data-type="number" order="descending"/>
                <xsl:value-of select="concat('&quot;', ., '&quot;')"/>
                <xsl:if test="position() != last()"><xsl:text>, </xsl:text></xsl:if>
            </xsl:for-each>
           ],
           "datasets" : [ {
             "data" : [ 
               <xsl:for-each select="distinct-values(//*:Communication/*:Description/*:Key[@Name='1 Genre'])">
                   <xsl:sort select="sum($root//*:Communication[*:Description/*:Key[@Name='1 Genre'] = current()]/*:Transcription/*:Description/*:Key[@Name='# HIAT:w'])" data-type="number" order="descending"/>
                   <xsl:value-of select="sum($root//*:Communication[*:Description/*:Key[@Name='1 Genre'] = current()]/*:Transcription/*:Description/*:Key[@Name='# HIAT:w'])"/>
                   <xsl:if test="position() != last()"><xsl:text>, </xsl:text></xsl:if>
               </xsl:for-each>
              ],
             "backgroundColor" : "rgba(75,192,192,0.500)",
             "borderColor" : "rgba(75,192,192,1.000)",
             "borderWidth" : 1,
             "label" : "Words",
             "yAxisID" : "words"
           }, {
             "data" : [ 
               <xsl:for-each select="distinct-values(//*:Communication/*:Description/*:Key[@Name='1 Genre'])">
                   <xsl:sort select="sum($root//*:Communication[*:Description/*:Key[@Name='1 Genre'] = current()]/*:Transcription/*:Description/*:Key[@Name='# HIAT:w'])" data-type="number" order="descending"/>
                   <xsl:value-of select="count($root//*:Communication[*:Description/*:Key[@Name='1 Genre'] = current()])"/>
                   <xsl:if test="position() != last()"><xsl:text>, </xsl:text></xsl:if>
               </xsl:for-each>
             ],
             "backgroundColor" : "rgba(54,162,235,0.500)",
             "borderColor" : "rgba(54,162,235,0.500)",
             "borderWidth" : 1,
             "label" : "Communications",
             "yAxisID" : "comms"
           } ]
         },
         "options" : {
           "responsive" : true,
           "title" : {
             "display" : true,
             "text" : "Words/Communications per Genre"
           },
           "legend" : {
             "display" : false,
             "position" : "top"
           },
           "scales" : {
             "yAxes" : [ {
               "type" : "linear",
               "display" : true,
               "id" : "words",
               "position" : "left"
             }, {
               "type" : "linear",
               "display" : true,
               "id" : "comms",
               "position" : "right"
             } ]
           }
         },
         "type" : "bar"
       });
           		window.myBar = new Chart(document.getElementById('Words-per-Dialect').getContext('2d'), {
         "data" : {
           "labels" : [ 
               <xsl:for-each select="distinct-values(//*:Communication/*:Description/*:Key[@Name='3b Dialect'])">
                   <xsl:sort select="sum($root//*:Communication[*:Description/*:Key[@Name='3b Dialect'] = current()]/*:Transcription/*:Description/*:Key[@Name='# HIAT:w'])" data-type="number" order="descending"/>
                   <xsl:value-of select="concat('&quot;', ., '&quot;')"/>
                   <xsl:if test="position() != last()"><xsl:text>, </xsl:text></xsl:if>
               </xsl:for-each>
           ],
           "datasets" : [ {
             "data" : [              
               <xsl:for-each select="distinct-values(//*:Communication/*:Description/*:Key[@Name='3b Dialect'])">
                   <xsl:sort select="sum($root//*:Communication[*:Description/*:Key[@Name='3b Dialect'] = current()]/*:Transcription/*:Description/*:Key[@Name='# HIAT:w'])" data-type="number" order="descending"/>
                   <xsl:value-of select="sum($root//*:Communication[*:Description/*:Key[@Name='3b Dialect'] = current()]/*:Transcription/*:Description/*:Key[@Name='# HIAT:w'])"/>
                   <xsl:if test="position() != last()"><xsl:text>, </xsl:text></xsl:if>
               </xsl:for-each>
             ],
             "backgroundColor" : "rgba(75,192,192,0.500)",
             "borderColor" : "rgba(75,192,192,1.000)",
             "borderWidth" : 1,
             "label" : "<xsl:value-of select="$corpus-name"/>"
           } ]
         },
         "options" : {
           "responsive" : true,
           "title" : {
             "display" : true,
             "text" : "Words per Dialect"
           },
           "legend" : {
             "display" : false,
             "position" : "top"
           },
           "scales" : {
             "xAxes" : [ {
               "barPercentage" : 0.4
             } ]
           }
         },
         "type" : "bar"
       });
           		window.myBar = new Chart(document.getElementById('Words/Communications-per-Decade').getContext('2d'), {
         "data" : {
           "labels" : [ 
               <xsl:for-each select="distinct-values(//*:Communication/*:Description/*:Key[@Name='2b Date of recording']/substring(., 1, 3))[. castable as xs:integer]">
                   <xsl:sort select="." data-type="number" order="ascending"/>
                   <xsl:value-of select="concat('&quot;', ., '0-', substring(., 3,1), '9&quot;')"/>
                   <xsl:if test="position() != last()"><xsl:text>, </xsl:text></xsl:if>
               </xsl:for-each>
           ],
           "datasets" : [ {
             "data" : [              
               <xsl:for-each select="distinct-values(//*:Communication/*:Description/*:Key[@Name='2b Date of recording']/substring(., 1, 3))[. castable as xs:integer]">
                   <xsl:sort select="." data-type="number" order="ascending"/>
                   <xsl:value-of select="sum($root//*:Communication[starts-with(*:Description/*:Key[@Name='2b Date of recording'], current())]/*:Transcription/*:Description/*:Key[@Name='# HIAT:w'])"/>
                   <xsl:if test="position() != last()"><xsl:text>, </xsl:text></xsl:if>
               </xsl:for-each>
             ],
             "backgroundColor" : "rgba(75,192,192,0.500)",
             "borderColor" : "rgba(75,192,192,1.000)",
             "borderWidth" : 1,
             "label" : "Words",
             "yAxisID" : "words"
           }, {
             "data" : [ 73, 5 ],
             "backgroundColor" : "rgba(54,162,235,0.500)",
             "borderColor" : "rgba(54,162,235,0.500)",
             "borderWidth" : 1,
             "label" : "Communications",
             "yAxisID" : "comms"
           } ]
         },
         "options" : {
           "responsive" : true,
           "title" : {
             "display" : true,
             "text" : "Words/Communications per Decade"
           },
           "legend" : {
             "display" : false,
             "position" : "top"
           },
           "scales" : {
             "yAxes" : [ {
               "type" : "linear",
               "display" : true,
               "id" : "words",
               "position" : "left"
             }, {
               "type" : "linear",
               "display" : true,
               "id" : "comms",
               "position" : "right"
             } ]
           }
         },
         "type" : "bar"
       });
           		window.myBar = new Chart(document.getElementById('Speakers-per-Sex').getContext('2d'), {
         "type" : "pie",
         "data" : {
           "labels" : [ 
               <xsl:for-each select="distinct-values(//*:Speaker/*:Sex)">
                   <xsl:sort select="count($root//*:Speaker[*:Sex = current()])" data-type="number" order="descending"/>
                   <xsl:value-of select="concat('&quot;', ., '&quot;')"/>
                   <xsl:if test="position() != last()"><xsl:text>, </xsl:text></xsl:if>
               </xsl:for-each>
           ],
           "datasets" : [ {
             "data" : [              
               <xsl:for-each select="distinct-values(//*:Speaker/*:Sex)">
                   <xsl:sort select="count($root//*:Speaker[*:Sex = current()])" data-type="number" order="descending"/>
                   <xsl:value-of select="count($root//*:Speaker[*:Sex = current()])"/>
                   <xsl:if test="position() != last()"><xsl:text>, </xsl:text></xsl:if>
               </xsl:for-each>
             ],
             "backgroundColor" : [ "rgba(75,192,192,0.500)", "rgba(54,162,235,0.500)", "rgba(201,203,207,0.700)" ],
             "label" : "<xsl:value-of select="$corpus-name"/>"
           } ]
         },
         "options" : {
           "responsive" : true,
           "title" : {
             "display" : true,
             "text" : "Speakers per Sex"
           },
           "legend" : {
             "display" : true,
             "position" : "top"
           }
         }
       });
       };	</script>
       </body>
       </html>
       

        
    </xsl:template>
    
</xsl:stylesheet>