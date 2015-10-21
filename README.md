#Auto WIFI Tethering [![Travis Build Status](https://travis-ci.org/danielmroczka/auto-tethering.png?branch=master)](https://travis-ci.org/danielmroczka/auto-tethering)
![App Logo](https://lh3.googleusercontent.com/GwGTGX5OuwMvOlg10Vemwk6x_Pd8EKsbpy-x9pV-t-EI29FCdFXzUH5PV64b2HRVtNCh=h80) 

The intention to create this project is a need to use android phone as a 3G Router. 
Phone is connected through 3G/4G/LTE connection to internet and shares connection using tethering functionality. 
Application triggers just after boot has been completed and switching on 3G connection and switch on tethering.
In this way phone is working as router just after switched it on without any additional support from user side.

Min. required Android version: 2.3 (less features but more devices supported). Rooting phone is not required.

Functionalities should be completed in a future:
- [x] starts immediately after operation system boot (you don't have to switch on manually tethering and internet connection on your mobile phone)
- [x] scheduler - you can define when your router will be switched off (i.e. during the night to safe the energy)
- [x] if no one is using router for configured time internet connection and tethering could be switched off
- [x] simcard's whitelist - you may define for which simcard you want to enable this features (for others simcard's service will be disabled to protect you from the additional costs)
- [x] checks permanently if your internet connection is active and reconnect if it has been lost
- [x] option to disable when roaming 

## Usage
Signing apk needs to set credentials in local file gradle.properties (expected location in folder ~/.gradle) and add following settings:
```
RELEASE_STORE_FILE={path to your keystore}
RELEASE_STORE_PASSWORD=*****
RELEASE_KEY_ALIAS=*****
RELEASE_KEY_PASSWORD=*****
```
Build app: gradle build

[Example of gradle.properties template] (https://gist.github.com/danielmroczka/b93eb61e4583c21da2a3)

[More about signing apk files you may find] (http://developer.android.com/tools/publishing/app-signing.html)

## Google Play app
Built application you may install here: [Auto Wifi Tethering] (https://play.google.com/store/apps/details?id=com.labs.dm.auto_tethering)
