# mcumgr-android
A mobile management library for devices running Apache Mynewt and Zephyr (DFU, logs, stats, config, etc.)

## Build From Source (Android Studio)

Maven repository is planned, in the meantime you can clone this repo add add it as a gradle module in Android Studio

1. Clone this repository outside of your project
2. In the target Android Studio project add the cloned repo as a gradle module.
    * File --> Project Structure
    * Click the `+` in the top left corner or (cmd/ctrl)-n
    * Select "Import Gradle Project" and hit "Next"
    * Browse to, and "Open" the directory cloned in `#1` then "Finish"
3. Add the mcumgr-android module as a dependency for your app module
    * In the Project Structure window, select the app module from the list and navigate to the "Dependencies" tab
    * Click the `+` in the bottom left corner or (cmd/ctrl)-n
    * Select "Module Dependency" from the drop down, then select the ":mcumgr-android" module and hit "OK" then "OK" again.
