# Android Client

This is the Android client app for the messaging service. Upon opening, it starts a service which connects to the central server and opens a host peer server socket which allows other users to connect if the device's IP is known.

The IP and port of the central server will need to be modified based on the server you are trying to pair the client with.

## Build Instructions

1. Install Android Studio (we used Flamingo 2022.2.1 Patch 2)
2. Clean and rebuild project, then build an APK or directly to a device.
3. Run the Mercury Messaging app.