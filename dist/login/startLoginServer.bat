@echo off
title Login Server
:start
echo Starting Login Server.
echo.
java -server -Dfile.encoding=UTF-8 -Xms128m -Xmx128m  -cp config;./../libs/* l2e.loginserver.LoginServer
if ERRORLEVEL 2 goto restart
if ERRORLEVEL 1 goto error
goto end
:restart
echo.
echo Server Restart ...
echo.
goto start
:error
echo.
echo Server terminated abnormally
echo.
:end
echo.
echo server terminated
echo.
pause
