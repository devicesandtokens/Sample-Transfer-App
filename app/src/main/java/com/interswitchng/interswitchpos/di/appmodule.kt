package com.interswitchng.interswitchpos.di

//import com.interswitch.smartpos.emv.telpo.TelpoPOSDeviceImpl.Companion.create
import com.interswitchng.interswitchpos.data.repository.AppRepository
import com.interswitchng.interswitchpos.views.viewmodels.AppViewModel
import com.interswitchng.smartpos.emv.pax.services.POSDeviceImpl
import com.interswitchng.smartpos.emv.pax.services.POSDeviceImpl.Companion.create
import com.interswitchng.smartpos.shared.interfaces.device.POSDevice
import org.koin.android.ext.koin.androidContext
import org.koin.android.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module

val appModule = module {

    single<AppRepository> {
        val posDevice: POSDevice = create(androidContext())
        AppRepository(posDevice)
    }
    viewModel { AppViewModel(get()) }

}