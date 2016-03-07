# Sensor Recorder

 This Android Application records sensor data of a Smartphone and of connected Wearables Devices. The data is stored locally on each device in binary format. Each sensor gets its own file and recording rates can be specified per sensors.

## Building and Deploying

 For the Smartphone app, with the Smartphone connected via USB:

    cd mobile && ../gradlew installDebug

 and for each Wearable connected via USB:

    cd wear && ../gradlew installDebug

## Starting a Recording

 Recordings can be started via broadcast intents, for example from the adb shell:

    adb shell am broadcast -e -i acceleration -a android.intent.action.SENSOR_RECORD

 which will record only the acceleromteter for the default duration of 5 seconds, at the default rate of 50 Hz.

 The start command can take these arguments:

    -i select the input sensor you like to read [string or list of strings]
    -r the rates in Hz at which to recored [float or list of floats]
    -o output directory beneath to write files to [defaults to /sdcard/DCIM/<current time>/]
    -d number of seconds to record for [float, defaults to 5 seconds]

 The ```-i``` and ```-r``` can either be a list or a single value. For the input (-i) multiple sensor can be recorded in parallel. A single rate (-r) will be used for all sensors, or if a list is given the matching rate will be chosen for the respective sensor. In the latter case the number of supplied rates must match the number of supplied inputs.

 The allowed values for the input can be displayed by running the intent and monitoring the logcat output, e.g.:

    adb shell am broadcast -e -i alop -a android.intent.action.SENSOR_RECORD

 The output on an LG G Watch W100 looks like this:

    adb logcat | grep Recorder
    de.uni_freiburg.es.sensorrecordingtool.Recorder: alop: no matches for alop found.Options are: 
    de.uni_freiburg.es.sensorrecordingtool.Recorder: android.sensor.wrist_tilt_gesture
    de.uni_freiburg.es.sensorrecordingtool.Recorder: android.sensor.gyroscope
    de.uni_freiburg.es.sensorrecordingtool.Recorder: android.sensor.gyroscope_uncalibrated
    de.uni_freiburg.es.sensorrecordingtool.Recorder: android.sensor.accelerometer
    de.uni_freiburg.es.sensorrecordingtool.Recorder: android.sensor.magnetic_field
    de.uni_freiburg.es.sensorrecordingtool.Recorder: android.sensor.magnetic_field_uncalibrated
    de.uni_freiburg.es.sensorrecordingtool.Recorder: android.sensor.orientation
    de.uni_freiburg.es.sensorrecordingtool.Recorder: android.sensor.rotation_vector
    de.uni_freiburg.es.sensorrecordingtool.Recorder: android.sensor.game_rotation_vector
    de.uni_freiburg.es.sensorrecordingtool.Recorder: android.sensor.linear_acceleration
    de.uni_freiburg.es.sensorrecordingtool.Recorder: android.sensor.gravity
    de.uni_freiburg.es.sensorrecordingtool.Recorder: android.sensor.significant_motion
    de.uni_freiburg.es.sensorrecordingtool.Recorder: android.sensor.step_detector
    de.uni_freiburg.es.sensorrecordingtool.Recorder: android.sensor.step_counter
    de.uni_freiburg.es.sensorrecordingtool.Recorder: android.sensor.geomagnetic_rotation_vector
    de.uni_freiburg.es.sensorrecordingtool.Recorder: android.hardware.sensor.Location

 You can use any string of the ones listed there. Matching works by choosing the shortest possible match, i.e. if looking for ```step``` sensor, the android.sensor.step_counter will be selected as it is shorter than android.sensor.step_detector.

 Multiple sensor can be recored at multiple rates like this:

    adb shell am broadcast --esa -i accel,gyro --efa -r 50,25 --ef -d 120 -a android.intent.action.SENSOR_RECORD

 This will record acceleration at 50Hz and the gyroscope at 25Hz for a total time of 2minutes. If you have multiple nodes in a Wearable network (for example by starting this on a smartwatch), all other nodes will also start to record sensor data.

## Results

 Results from recording sessions and broadcast invocations will be communicated back by broadcast intents. Those can only be captured programmatically...

## Notifcations

 Notifications on both the Wearable and the Smartphone will be displayed throughout a recordings session. Dismissing the notification, as well as pressing cancel, will cancel the current recording and only what has been recorded so far will be left over.

## Gotchas

 The broadcast receiver does not receive the above mentioned when the package has been just installed. In order for it to work the application must have been started at least once!
