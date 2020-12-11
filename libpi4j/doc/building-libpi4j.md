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

Instead of using the source code from the git repository, I duplicate the necessary code of the master branch to compile the `WiringPi` library. I had to do that because the actual repo does not have any tags management.

To compile the Pi4J JNI library (`libpi4j.so`) against the `WiringPi` from <https://github.com/WiringPi/WiringPi>, I just modified the `Makefile` from the `pi4j-native` project ([tags : release 1.2](https://github.com/Pi4J/pi4j/releases/tag/release%2F1.2)). 

```bash
# Clone this repo
https://github.com/fguiet/automationserver-v2.git

# Go to builder folder
cd automationserver-v2/libpi4j/builder

# Build the image
docker build -t guiet/libpi4j:latest .

# Get the artefact from the image
docker run -it --rm -v /tmp:/tmp guiet/libpi4j:latest /bin/bash -c "cp -r /applications/builder/output/libpi4j.so /tmp"
```

## References

* A new version of pi4j is under developpement. No ready for production yet...this is why I had to do that...See <https://github.com/Pi4J/pi4j-v2>

* Unofficial mirror and ports of WiringPi. See <https://github.com/WiringPi>

* Gordon Web page (WiringPi creator, thank for that man) : <https://projects.drogon.net/> 

* WiringPi : See <http://wiringpi.com/wiringpi-deprecated/>, <http://wiringpi.com/wiringpi-updated-to-2-52-for-the-raspberry-pi-4b/>