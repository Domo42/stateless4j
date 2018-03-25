@ECHO OFF
SET targetVersion=3.1.0
SET baseDir=%~dp0
SET targetUrl=https://oss.sonatype.org/service/local/staging/deploy/maven2
REM SET targetUrl=file://v:\temp\repo

REM change version number in project tree
CALL mvn versions:set -DgenerateBackupPoms=false -DnewVersion=%targetVersion%

REM delete old signature files
del *.asc

call mvn clean install -DperformRelease=true -Dgpg.keyname=AA9AEC3C

SET pomAscFile=stateless4j-%targetVersion%.pom.asc

gpg -u AA9AEC3C --sign --detach-sign -o %pomAscFile% -a pom.xml


REM deploy base artifacts
ECHO.
ECHO -------- Deploy Binary ----------
ECHO.
call mvn deploy:deploy-file -Durl=%targetUrl% ^
                            -DrepositoryId=sonatype-nexus-staging ^
                            -Dfile=%baseDir%\target\stateless4j-%targetVersion%.jar ^
                            -DpomFile=%baseDir%\pom.xml ^
                            -Djavadoc=%baseDir%\target\stateless4j-%targetVersion%-javadoc.jar ^
                            -Dsources=%baseDir%\target\stateless4j-%targetVersion%-sources.jar

REM deploy signatures
ECHO.
ECHO -------- Deploy POM Signature ----------
ECHO.
call mvn deploy:deploy-file -Durl=%targetUrl% ^
                            -DrepositoryId=sonatype-nexus-staging ^
                            -Dfile=%baseDir%\%pomAscFile% ^
                            -DpomFile=%baseDir%\pom.xml ^
                            -Dpackaging=pom.asc

ECHO.
ECHO -------- Deploy lib Signature ----------
ECHO.
call mvn deploy:deploy-file -Durl=%targetUrl% ^
                            -DrepositoryId=sonatype-nexus-staging ^
                            -Dfile=%baseDir%\target\stateless4j-%targetVersion%.jar.asc ^
                            -DpomFile=%baseDir%\pom.xml ^
                            -Dpackaging=jar.asc
ECHO.
ECHO -------- Deploy JavaDoc Signature ----------
ECHO.
call mvn deploy:deploy-file -Durl=%targetUrl% ^
                            -DrepositoryId=sonatype-nexus-staging ^
                            -Dfile=%baseDir%\target\stateless4j-%targetVersion%-javadoc.jar.asc ^
                            -DpomFile=%baseDir%\pom.xml ^
                            -Dclassifier=javadoc ^
                            -Dpackaging=jar.asc

ECHO.
ECHO -------- Deploy Sources Signature ----------
ECHO.
call mvn deploy:deploy-file -Durl=%targetUrl% ^
                            -DrepositoryId=sonatype-nexus-staging ^
                            -Dfile=%baseDir%\target\stateless4j-%targetVersion%-sources.jar.asc ^
                            -DpomFile=%baseDir%\pom.xml ^
                            -Dclassifier=sources ^
                            -Dpackaging=jar.asc

GOTO:eof

:error
    ECHO.
    ECHO -----------------------------------
    ECHO - Error performing project staging
    ECHO -----------------------------------
    ECHO.
    EXIT /B