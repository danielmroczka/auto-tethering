# auto-tethering
The intention to create this project is a need to use android phone as a 3G Router. 
Phone is connected through 3G/4G/LTE connection to internet and shares connection using tethering functionality. 
Application triggers just after boot has been completed and switching on 3G connection and switch on tethering.
In this way phone is working as router just after switched it on without any additional support from user side.

Min. required Android version: 2.3

Functionalities should be completed in a future:
- switching on/off opening service just after boot
- switching on for provided telephone number (to avoid additional charges if user replace simcard and forget to switching service off)
- delay after boot

## Usage
Signing apk needs to set credentials in local file gradle.properties and add following settings:
```
RELEASE_STORE_FILE={path to your keystore}
RELEASE_STORE_PASSWORD=*****
RELEASE_KEY_ALIAS=*****
RELEASE_KEY_PASSWORD=*****
```

More about signing apk files you may find here: http://developer.android.com/tools/publishing/app-signing.html

## Google Play app
Built app you may install here: https://play.google.com/store/apps/details?id=com.labs.dm.auto_tethering
