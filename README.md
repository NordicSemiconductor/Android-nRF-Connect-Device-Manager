# Android McuManager 

A transport agnostic implementation of the McuManager protocol (aka Newt Manager, Simple Management Protocol) for Android. 

## Download

You may clone/download the repository and add the source as a module in your app, or simply use gradle.

### Gradle

Add jitpack as a maven repository under all projects (root `build.gradle`):

```
	allprojects {
		repositories {
			// ...
			maven { url 'https://jitpack.io' }
		}
	}
```

Add the dependency to your app:

```
dependencies {
	// ...
	implementation 'com.github.runtimeco:mcumgr-android:<latest-release>'
}
```

## Sample App Quick Start

Before integrating McuManager into your Android app, you may want to test the sample app with your device to make sure the device has been set up correctly. 

// TODO

# Introduction

McuManager is a application layer protocol used to manage and montior microcontrollers running Apache Mynewt and Zephyr. More specifically, McuManagr implements over-the-air (OTA) firmware upgrades, log and stat collection, and file-system and configuration management. 

# API Overview

Since most apps will be using Bluetooth Low Energy (BLE) as the transport and likely already contain custom application protocols running over BLE to control the remote device, the primary design goal of this library is to allow developers to integrate mcumgr-android into their custom BLE state machine. Furthermore, because McuManager is capable of running over mutliple transports (BLE, UDP) and allows for commands to be sent on top of [CoAP](https://tools.ietf.org/html/rfc7252) the transport implementation is separated from the packet formation.

// TODO


