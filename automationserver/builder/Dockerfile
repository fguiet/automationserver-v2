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

RUN mkdir -p /applications/automationserver/bin


#Get Java Source
RUN cd /tmp && git clone https://github.com/fguiet/automationserver-v2.git




