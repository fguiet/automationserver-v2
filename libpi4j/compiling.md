# Pi4J :: Java I/O Library for Raspberry Pi

## Foreword

As of 2020/12/05, the last `pi4j` java library version is 1.2.

This library does not support the `Raspberry 4` (See <https://pi4j.com/1.2/index.html>, Supported Hardware)

Until soon, I was using `automation server` on a Raspberry 3B+ so far so good...but recently I wanted to move the project on Raspberry 4...at this troubles began...

First, I moved to `arm64` architecture instead of `armhf`.

Second, `pi4j` has a native dependency to `WiringPi` (See <https://pi4j.com/1.2/dependency.html>).

Sadly, `Gordon` (the WiringPi library's dad) decided to stop supporting his baby (See <http://wiringpi.com/wiringpi-deprecated/>...

Fortunately, a last effort was made by `Gordon` and he released a `WiringPi` library compatible with the Raspberry 4...but...this library is only `armhf` compatible...

Well...with that said...let's make this `pi4j` java library works with a Raspberry 4 and `arm64` compatible...

## Let's write a Dockerfile for that stuff

We will use the [unofficial WiringPi mirror](https://github.com/WiringPi/WiringPi) to build the WiringPi libray on a `arm64` linux distribution.
