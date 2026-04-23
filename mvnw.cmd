@echo off
set MAVEN_PROJECTBASEDIR=%~dp0
mvn %* --file "%MAVEN_PROJECTBASEDIR%pom.xml"
