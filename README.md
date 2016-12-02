usage:
```
1. app/src/main/res/raw目录下有两个文件，全部拷贝出来放到电脑上的一个目录
2. 编译好的apk（必须命令为app-release.apk）也放入上面的目录，运行`run_server.cmd`。脚本内容如下：
```
@echo off
for /f %%i in ('adb shell "ls /data/app/cn.iotguard.phonecontroller-1/base.apk"') do set result=%%i
if "%result:~-1%" == ":" (
	adb install app-release.apk
)
adb forward tcp:56789 tcp:56789
adb shell "export CLASSPATH=/data/app/cn.iotguard.phonecontroller-1/base.apk;exec app_process /system/bin cn.iotguard.phonecontroller.Main"
```
做的事情很简单，如果手机上没有安装该apk则安装，然后进行了端口映射，最后启动服务程序。
3. 打开`viewer.html`即可浏览。
```