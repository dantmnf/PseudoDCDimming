# Pseudo DC Dimming

[简体中文](README.zh.md) | [下载最新版本](https://github.com/dantmnf/PseudoDCDimming/releases/latest)

[Download latest release](https://github.com/dantmnf/PseudoDCDimming/releases/latest)

## Background

Current OLED screens have a separate brightness control input to control brightness at the highest input signal. If we want to display white at a brightness of 20 cd/m<sup>2</sup>, we can do either of the following:

1. Adjust the brightness control so that the brightness at the highest input signal is 20 cd/m<sup>2</sup>, then use the highest signal ("white") as input;
2. With a higher brightness control input, say 160 cd/m<sup>2</sup>, use a lower signal ("gray") as input to produce 12.5% of maximum brightness (20 cd/m<sup>2</sup> m<sup>2</sup>) output.

Some OLED screens use a higher PWM frequency and duty cycle in the latter case, which some users feel helps reduce discomfort when viewing the screen for a long time or in dark environments.

The relationship between signal level and output brightness is not linear, so the second method is not simply multiplying the signal by a factor. However, given the following premises, we can achieve precise control:

1. The Android system assumes that the screen has an sRGB response curve;
2. Based on the sRGB response curve, the system framework and HAL provide RGB matrix transformation in linear space;
3. Starting from Android 12, the system framework and HAL interface support absolute brightness control in cd/m<sup>2</sup> (or nits).

## How it works

By limiting the minimum value of the brightness control and scaling down the output signal through a matrix transformation function, the actual display brightness matches the expected brightness while maintaining a high PWM frequency/duty cycle.

## Limitations

* Rely heavily on the manufacturer's calibration of screen brightness controls and response curves. If the manufacturer uses different response curves during calibration, or the brightness control has non-linear behavior, it may affect the display quality after turning on the module;
* May conflict with other color transform functions;
* May conflict with HDR content.

## Configuration

Use the high-speed shutter mode of the camera to amplify the stroboscopic effect, or use professional instruments to measure the PWM frequency and duty cycle. Choose an acceptable brightness value as the minimum hardware brightness.