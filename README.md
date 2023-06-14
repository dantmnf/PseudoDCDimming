# Pseudo DC Dimming

[简体中文](README.zh.md) | [下载最新版本](https://github.com/dantmnf/PseudoDCDimming/releases/latest)

Enable alternative dimming mode (likely DC-like) on low brightness for some OLED displays by using software brightness gain.

Requires Android 12+ and Xposed-compatible framework.

[Download latest release](https://github.com/dantmnf/PseudoDCDimming/releases/latest)

## How it works

By limiting the minimum brightness and scaling down the output signal through a degamma-gain-regamma transform, the actual display brightness matches the expected brightness while maintaining a higher PWM frequency and duty cycle.

[details](details.md)

## Limitations

* Rely heavily on the manufacturer's calibration of screen brightness controls and response curves. If the manufacturer uses different response curves during calibration, or the brightness control has non-linear behavior, it may affect the display quality after turning on the module;
* May conflict with other color transform functions;
* May conflict with HDR content.

## Configuration

Use the high-speed shutter mode of the camera to amplify the stroboscopic effect, or use professional instruments to measure the PWM frequency and duty cycle. Choose an acceptable brightness value as the minimum hardware brightness.

## Acknowledgements

Inspired from [ztc1997/FakeDCBacklight](https://github.com/ztc1997/FakeDCBacklight). This project additionally implements immediate application as well as stabilizing the brightness before and after enabling.
