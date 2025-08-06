chcp 65001
@echo off
title Game Server
:start
echo Starting Game Server.
echo.
REM -------------------------------------
REM If you have a big server and lots of memory, you could experiment for example with
java -Dfile.encoding=UTF-8 -server -Xms4G -Xmx8G -XX:+OptimizeFill -XX:+UseZGC -Duser.timezone=GMT+3 -cp config;./../libs/* l2e.gameserver.GameServer
REM -------------------------------------
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
