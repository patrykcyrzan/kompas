package dev.cyrzan.skeleton.feature.pdfviewer.sandbox

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Hilt's root has to live in the app module itself – the rest comes from `:sandbox`. */
@HiltAndroidApp
class PdfViewerSandboxApp : Application()
