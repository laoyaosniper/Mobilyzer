Mobilyzer
=========

A library provides cellular network measurement for Andoird developers

=============================

##Integrating Mobilyzer for Mobile Apps

There are two ways to integrate Mobilyzer into your Android apps. You can integrate via **external library** or you can integrate via **library project**

For both approaches, you need to add those permissions in your project's manifest

    <uses-permission android:name="android.permission.WAKE_LOCK"/>

    <uses-permission android:name="android.permission.USE_CREDENTIALS" />

    <uses-permission android:name="android.permission.GET_ACCOUNTS" />

    <uses-permission android:name="android.permission.INTERNET" />

    <uses-permission android:name="android.permission.READ_PHONE_STATE" />

    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />

And add the following service inside the <application> label

    <service android:name="com.mobilyzer.MeasurementScheduler" >

        <intent-filter>

            <action android:name="com.mobilyzer.MeasurementScheduler"></action>

        </intent-filter>

    </service>

=============================

###Integrating via External Library

Clone this project and copy mobilyzer.jar and myjson-1.7.1.jar to libs folder of your app. You can also follow this guide (http://developer.android.com/guide/faq/commontasks.html#addexternallibrary) to refer external library.

=============================

###Integrating via Library Project

With Eclipse

Clone this project and import it into your Eclipse workspace. Add a reference to the Mobilyzer project library project to your app with this guide (http://developer.android.com/tools/projects/projects-eclipse.html#ReferencingLibraryProject)
