package co.spin

import timber.log.*

actual fun initTimber() {
    Timber.plant(NSLogTree(2))
}