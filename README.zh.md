# 伪 DC 调光

[English](README.md) | [下载最新版本](https://github.com/dantmnf/PseudoDCDimming/releases/latest)

通过软件增益为部分 OLED 屏幕在低亮度下启用类 DC 调光方式。

这是一个 Xposed (LSPosed) 模块，仅支持 Android 12 及以上版本。

## 原理

通过限制亮度控制的最小值，并通过矩阵变换功能缩小输出信号，使实际显示亮度匹配预期亮度，同时保持较高的 PWM 频率/占空比。

[详细原理](details.zh.md)

## 限制

* 严重依赖厂商对屏幕亮度控制以及响应曲线的校准。如果厂商在校准时使用了不同的响应曲线，或者亮度控制存在非线性行为，都可能影响开启模块后的显示质量；
* 可能与其他颜色变换功能冲突；
* 可能与 HDR 显示冲突。

## 配置

通过相机的高速快门模式放大频闪效应，或使用专业仪器测量 PWM 频率及占空比，选择一个可以接受的亮度值作为最小硬件亮度。

## 致谢

灵感来自 [ztc1997/FakeDCBacklight](https://github.com/ztc1997/FakeDCBacklight)。本项目额外实现了立即应用以及稳定启用前后亮度的功能。
