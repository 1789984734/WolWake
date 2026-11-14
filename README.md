# WolWake · WOL 唤醒

通过 Wake-on-LAN 魔术包在局域网内远程唤醒电脑等设备的安卓应用。

## 功能

- 多设备管理：添加 / 编辑 / 删除设备（名称、MAC、目标地址、端口），本地持久化
- 一键唤醒：发送标准魔术包（6×0xFF + MAC×16，102 字节），连续发送 3 次提升可靠性
- MAC 地址容错解析：容忍 `:` `-` `.` 空格分隔与无分隔符写法，表单实时校验
- 目标地址支持定向 IP（可跨网段）或子网广播地址（如 `192.168.100.255`），端口默认 9

## 技术栈

Kotlin 2.4 · Jetpack Compose（Material 3）· DataStore Preferences · kotlinx.serialization

- minSdk 26 / targetSdk 37
- 仅需 `INTERNET` 权限

## 构建

```bash
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

## 使用提示

目标电脑需先开启 Wake-on-LAN（BIOS/UEFI 中启用网卡唤醒，Windows 需在设备管理器允许"魔术包唤醒"）。

## 已验证

- 单元测试：`./gradlew test`（MAC 解析与魔术包字节断言）
- 真机端到端：应用发出的 UDP 包经监听器抓取，逐字节符合标准魔术包格式
