@echo off
title JSOFT - Database Installer

set PATH=%PATH%;C:\Program Files\MariaDB 10.4\bin
set USER=root
set PASS=root
set DBNAME=jsoft
set DBHOST=127.0.0.1

set COMMAND="CREATE DATABASE IF NOT EXISTS "
mysql -h %DBHOST% -u %USER% --password=%PASS% -e "%COMMAND:"=%%DBNAME:"=%;"
for /r sql %%f in (*.sql) do (
                echo Installing table %%~nf ...
		mysql -h %DBHOST% -u %USER% --password=%PASS% -D %DBNAME% < %%f
	)
:end
cls
echo ===========================================================================
echo #									                                       #
echo #                       Thanks for choising ;)                            #
echo #		                                                      			   #
echo #									                                       #
echo ===========================================================================
pause > nul
