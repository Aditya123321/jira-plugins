@echo off
RMDIR /S /Q target\classes
RMDIR /S /Q target\dependency-maven-plugin-markers
DEL target\*.jar
DEL target\*.obr
cd ..\customization-core
atlas-mvn clean install && cd ..\defect-density-report && atlas-package