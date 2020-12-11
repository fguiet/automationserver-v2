#Debian Buster Slim 10.6
FROM debian:10.6-slim

LABEL maintainer="fguiet@gmail.com"

#Update distro
RUN apt-get -y clean
RUN apt-get -y update
RUN apt-get -y upgrade

#Fix to enable Java installation on Buster slim
#See : https://github.com/balena-io-library/base-images/issues/273
RUN mkdir -p /usr/share/man/man1

#Install Open JDK 11
RUN apt-get install -y openjdk-11-jdk-headless

#Install some need packages
RUN apt-get install -y wget git
RUN apt-get install -y sudo make gcc

#Make full directory
RUN mkdir -p /applications/builder/output

#Set work dir
WORKDIR /applications/builder

#Wiring source code are from WiringPi unofficial mirror : https://github.com/WiringPi/WiringPi.git
#Branch master as of 2020/12/05 - could not use tags as they do not exists.. :(
RUN git clone https://github.com/fguiet/automationserver-v2.git

#Build WiringPi library
RUN cd automationserver-v2/libpi4j/builder/vendor/WiringPi/master-20201205 && chmod u+x build && ./build

#Getting Pi4J last release version (1.2 as of 2020/12/05)
RUN git clone https://github.com/Pi4J/pi4j.git
RUN cd /applications/builder/pi4j && git checkout tags/release/1.2

#Copy custom Makefile
#Folder to Java Home and WiringPi should be modified accordingly in INCLUDE section that's all
COPY ./vendor/pi4j/Makefile /applications/builder/pi4j/pi4j-native/src/main/native

#Build libpi4j.so library
RUN cd /applications/builder/pi4j/pi4j-native/src/main/native && make clean dynamic

#Move artefact to output directory
RUN cp /applications/builder/pi4j/pi4j-native/src/main/native/libpi4j.so /applications/builder/output
