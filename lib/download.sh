#!/bin/sh
curl -o - https://www.mpi.nl/tools/elan/ELAN_6-2_linux.tar.gz | tar xzv ELAN_6-2/opt/elan-6.2/lib/app/elan-6.2.jar --transform='s#ELAN_6-2/opt/elan-6.2/lib/app/##'
mvn install:install-file -Dfile=elan-6.2.jar -DgroupId=nl.mpi -DartifactId=ELAN -Dversion=6.2 -Dpackaging=jar  -DgeneratePom=true
curl -o - https://www.exmaralda.org/files/officialDL/EXMARaLDA_linux.tar.gz | tar xzv exmaralda1.11/lib/EXMARaLDA.jar  --transform='s#exmaralda1.11/lib/##'
mvn install:install-file -Dfile=EXMARaLDA.jar -DgroupId=org.exmaralda -DartifactId=EXMARaLDA -Dversion=1.0 -Dpackaging=jar  -DgeneratePom=true
