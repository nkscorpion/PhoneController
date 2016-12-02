@echo off
for /f %%i in ('adb shell "ls /data/app/cn.iotguard.phonecontroller-1/base.apk"') do set result=%%i
if "%result:~-1%" == ":" (
	adb install app-release.apk
)
adb forward tcp:56789 tcp:56789
adb shell "export CLASSPATH=/data/app/cn.iotguard.phonecontroller-1/base.apk;exec app_process /system/bin cn.iotguard.phonecontroller.Main"