@echo off
echo ================================
echo Spoustim Tour de App projekt...
echo ================================

cd /d "%~dp0"

if not exist uploads (
  mkdir uploads
)

mvnw.cmd spring-boot:run
