package com.oliverspryn.android.oauthbiometrics.di.factories

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.inject.Inject

class RxJavaFactory @Inject constructor() {

    val io: Scheduler
        get() = Schedulers.io()

    val ui: Scheduler
        get() = AndroidSchedulers.mainThread()
}
