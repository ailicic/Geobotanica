//package com.geobotanica.geobotanica.android
//
//import android.app.Application
//import android.location.LocationManager
//import com.geobotanica.geobotanica.android.location.LocationService
//import dagger.Module
//import dagger.Provides
//import javax.inject.Singleton
//
//@Module
//class AndroidModule(val application: Application) {
//    @Provides @Singleton fun provideLocationService(application: Application,
//                                                    locationManager: LocationManager): LocationService {
//        return LocationService(application, locationManager)
//    }
//    // Add CameraService
//}