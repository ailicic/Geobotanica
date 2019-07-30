package com.geobotanica.geobotanica.network

import androidx.lifecycle.liveData
import com.geobotanica.geobotanica.network.ResourceStatus.*
import com.geobotanica.geobotanica.util.Lg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext

// Related links
// https://proandroiddev.com/android-architecture-d7405db1361c
// https://github.com/PhilippeBoisney/ArchApp/blob/master/data/repository/src/main/java/io/philippeboisney/repository/utils/Resource.kt
// https://github.com/googlesamples/android-architecture-components/blob/88747993139224a4bb6dbe985adf652d557de621/GithubBrowserSample/app/src/main/java/com/android/example/github/repository/NetworkBoundResource.kt

inline fun <T> createNetworkBoundResource(
        crossinline loadFromDb: suspend () -> T?,
        crossinline shouldFetch: (T?) -> Boolean,
        crossinline fetchData: suspend () -> T,
        crossinline saveToDb: suspend (T) -> Unit
) = liveData {

    emit(Resource.loading<T>())
    withContext(SupervisorJob()) {
        val dbResult = loadFromDb()
        if (shouldFetch(dbResult)) {
            try {
                emit(Resource.loading(dbResult))
                withContext(Dispatchers.IO) {
                    saveToDb(fetchData())
                }
                Lg.v("NetworkBoundResource: Returning data fetched from network")
                emit(Resource.success(loadFromDb()!!))
            } catch (e: Exception) {
                Lg.e("NetworkBoundResource Error: $e")
                emit(Resource.error(e, dbResult))
            }
        } else {
            Lg.v("NetworkBoundResource: Returning data from local database")
            emit(Resource.success(dbResult!!))
        }
    }
}

data class Resource<out T>(val status: ResourceStatus, val data: T? = null, val error: Throwable? = null) {
    companion object {
        fun <T> loading(data: T? = null): Resource<T> = Resource(LOADING, data)
        fun <T> success(data: T): Resource<T> = Resource(SUCCESS, data)
        fun <T> error(error: Throwable, data: T? = null): Resource<T> = Resource(ERROR, data, error)
    }
}

enum class ResourceStatus {
    LOADING,
    SUCCESS,
    ERROR
}


//suspend inline fun <ResultType, RequestType> createNetworkBoundResource(
//        crossinline loadFromDb: suspend () -> ResultType?,
//        crossinline shouldFetch: (ResultType?) -> Boolean,
//        crossinline fetchData: suspend () -> RequestType,
//        crossinline processResponse: (RequestType) -> ResultType,
//        crossinline saveToDb: suspend (ResultType) -> Unit
//) = liveData {
//
//    emit(Resource.loading<ResultType>())
//    withContext(SupervisorJob()) {
//        val dbResult = loadFromDb()
//        Lg.d("NetworkBoundResource: Initial loadFromDb() = $dbResult")
//        if (shouldFetch(dbResult)) {
//            try {
//                emit(Resource.loading(dbResult))
//                withContext(Dispatchers.IO) {
//                    saveToDb(processResponse(fetchData()))
//                }
//                Lg.v("NetworkBoundResource: Returning data fetched from network")
//                emit(Resource.success(loadFromDb()!!))
//            } catch (e: Exception) {
//                Lg.e("NetworkBoundResource Error: $e")
//                emit(Resource.error(e, dbResult))
//            }
//        } else {
//            Lg.v("NetworkBoundResource: Returning data from local database")
//            emit(Resource.success(dbResult!!))
//        }
//    }
//}