package com.interswitchng.interswitchpos.di

import org.koin.standalone.StandAloneContext.loadKoinModules



class LoadKoinModulesApp {

        fun loadKoin() {
            val modules = listOf(appModule)
            loadKoinModules(modules)
        }

}