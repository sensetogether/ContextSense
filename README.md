# ContextSense
Key Features 
---------------------
#ContextSense is an Android application that supports user context inference, sensing data collection, and GPS positioning, by leveraging embedded sensors on the smart phone.

The application initially shows a sensor list of the device.

The context inference function distinguishes physical context as in-/out-pocket, in-/out-door, and under-/on-ground scenarios.
It is designed to be personalized on each device, and therefore it requires feedback from the user to update the internal machine learning models.
Both three binary classifications and one hierarchical inference are available to receive feedback.
The user can click on the feedback button once the inference result is incorrect.
The context inference function also provides a recognition of the user activity (such as still, walk, running, etc.).

The sensing data collection function measures the environmental conditions: daytime (binary), light density (lux), magnetic strength (μT), GSM connectivity (binary), abstract RSSI level, RSSI value (dBm), GPS accuracy (m), WiFi connectivity (binary), WiFi RSSI (dBm), proximity (b), sound level (dBA), temperature (C), pressure (hPa), humidity (%), and each entry contains a timestamp.
The user can give the ground-truth labels on the sensing data by using "in-pocket, in-door and under-ground" switches.
The sensing data can be stored on the device as a local file and also be sent via email.

The GPS positioning function provides information related to the GPS provider: longitude, latitude, altitude, speed, bearing, accuracy and time. It may work only in an out-door environment.

Getting started 
------------------------
Step 1 - Download or clone the source code of #ContextSense.  
 
Step 2 - Download [Android Studio](https://developer.android.com/studio/)  
Start android Studio and open the #ContextSense project by selecting the directory wherein is placed #ContextSense. 
All dependencies are located in the file "build.gradle".

Step 3 - Download the following java library:
* [Weka-Android](https://github.com/Yifan-DU/Weka-Android/blob/master/dist/weka-stable-3.8.1-SNAPSHOT.jar) (version 
3.8.1 is preferred) 

Place the jar file that just has been downloaded into the "app/libs" folder of the #ContextSense app directory, through Windows Explorer or Mac Finder.
Then open "Project" files view in android on the left side of Android Studio, find the "libs" folder, right click the "weka-stable-3.8.1-SNAPSHOT.jar" file, and select "Add as Library".

Running on the mobile phone: 
------------------------------------------------
Note that the application starts with a permission checker and you need to grant these permission requests.

The application may also request to turn on some components in runtime, such as GPS, please follow the notification to turn on them.

License 
-------------
#ContextSense is available under the terms of the GPL License, which implies that application developers are free to use #ContextSense. 
It also means that developers are invited to contribute to improve #ContextSense as long as the original source code remains open.

Contributors
---------------------

* Yifan Du, designer & developer (90%)
* Françoise Sailhan, reviewer (5%)
* Valérie Issarny, reviewer (5%) 
