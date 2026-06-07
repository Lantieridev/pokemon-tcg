@echo off
echo ===================================================
echo Starting Pokemon TCG Backend Server
echo Database migrations are already successfully applied!
echo ===================================================
cd BE
mvn spring-boot:run
pause
