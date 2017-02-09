# forked-daapd-api
[forked-daapd](https://github.com/ejurgensen/forked-daapd) is an awesome Linux music server that supports [DAAP](https://en.wikipedia.org/wiki/Digital_Audio_Access_Protocol) (Digital Audio Access Protocol) used by iTunes.

A number of remote control applications are supported and work great. However, the libraries I found that enabled programmatic control were either outdated or incomplete.

forked-daapd-api is a Java application that can control forked-daapd programmatically. It runs on the same server as forked-daapd and provides a simple REST interface to forked-daapd.

## Setup
1. [Setup forked-daapd](https://github.com/ejurgensen/forked-daapd/blob/master/INSTALL)
2. Install Java 8 
3. Download forked-daapd-api.jar

## Run

```bash
java -jar forked-daapd-api.jar
```

or with custom settings

```bash
java -jar forked-daapd-api.jar
```

## Usage
forked-daapd-api listens for HTTP requests (on port 8080 by default). Most programming languages have libraries for making requests. You can also use cURL to make requests directly from the command line or within shell scripts (shown in examples below)

### Speakers
forked-daapd is capable of outputting audio to different Airplay speakers (check out [shairport-sync](https://github.com/mikebrady/shairport-sync)!), Chromecasts and locally. Check the docs for forked-daapd for more information about supported speakers

#####List speakers

```bash
curl http://localhost:8080/speakers
```

Response

```javascript
[
  {
    "name": "Kitchen Speaker"
    "id": "0x435453"
    "active": true
    "volume": 0.5
  }, {
    "name"
    ....
```

##### Select speakers
Select which speakers should be active. Provide a list of IDs to enable, all others will be disabled.

```bash
curl http://localhost:8080/speakers?active={id1, id2, ...}
```

### Volume
forked-daapd has master volume and individual volumes for each speaker. All volumes are expressed as percentage values between 0 and 1.0.

Speaker volume is a percent of the master volume. For example, setting speaker volume to 0.5 when the master volume is set to 0.5 will cause the speaker to output at 25% volume.

##### Get master volume

```bash
curl http://localhost:8080/volume
```

##### Set master volume

```bash
curl http://localhost:8080/volume?set={value}
```

where 0 <= value <= 1.0

##### Get speaker volumes

```bash
curl http://localhost:8080/speakers
```

##### Set speaker volume

```bash
curl http://localhost:8080/speakers/{id}?volume={value}
```

where id is a valid speaker ID and 0 <= value <= 1.0

##### Fade speaker volume
Fade in/out over a set duration (in seconds)

```bash
curl http://localhost:8080/speakers/{id}?start={value}&end={value}&duration={seconds}
```

Example: fade in from silent to 75% volume over 2 seconds

```bash
curl http://localhost:8080/speakers/{id}?start=0.0&end=0.75&duration=2
```

Note that this call will return immediately (it will not wait for duration). Once a fade has started, there is no way to cancel it. If you attempt to manually set the volume while a fade is on going, it will quickly be overwritten

Fading is implemented to be as smooth as possible but could sound choppy depending on how quickly the speaker responds


## Contributing
Feel free to add any missing capabilities. If you enable debug logging in forked-daapd, you can see the requests made by other remote applications, which makes adding new functionality fairly easy.

## Building
forked-daapd-api is distributed already packaged with dependencies and should run on any Java 8 JVM (regardless of platform). You can download the latest version here.

If you need to build, you can do so with maven:

```bash
mvn package
```
Maven will download dependencies and create forked-daapd-api.jar in the target directory.

## Use as dependency
If you would rather incorporate DACP directly into your Java application, check out DacpService. This class is purposefully designed to be dropped into another Java project with modification. It has no dependencies on non-standard libraries and contains all DACP funtionality found in the API.

## Thanks
This little project wouldn't exist without the great work of:

* [forked-daapd](https://github.com/ejurgensen/forked-daapd) (of course)
* [tunesremote-se](https://github.com/nglass/tunesremote-se) (response parsing code in DacpService)