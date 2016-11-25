usage:
```
# 1. 安装编译后的apk
adb install app.apk

# 2. 将本地端口55000转发至手机55000端口
adb forward tcp:55000 tcp:55000

# 3. 启动服务
adb shell "export CLASSPATH=/data/app/cn.iotguard.phonecontroller-1/base.apk;exec app_process /system/bin cn.iotguard.phonecontroller.Main"

# 4. 打开浏览器，访问http://localhost:55000即可
```