package com.example

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Custom Application class that serves as the entry point for Dagger Hilt dependency injection.
 * 
 * Required by Hilt to trigger application-level code generation and dependency scope attachment.
 */
@HiltAndroidApp
class ScenicScoutApp : Application()
