package com.example.okane

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * App entry point. @HiltAndroidApp makes Hilt generate and attach the
 * dependency container when the app process starts — required for any
 * injection anywhere in the app. Registered in AndroidManifest.xml
 * via android:name=".OkaneApp".
 */
@HiltAndroidApp
class OkaneApp : Application()
