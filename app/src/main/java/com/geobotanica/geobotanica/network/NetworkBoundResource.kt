package com.geobotanica.geobotanica.network

import androidx.lifecycle.liveData
import com.geobotanica.geobotanica.network.ResourceStatus.*
import com.geobotanica.geobotanica.util.Lg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import java.lang.IllegalStateException

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

    emit(Resource.loading())
    withContext(SupervisorJob() + Dispatchers.IO) {
        val dbResult = loadFromDb()
        if (shouldFetch(dbResult)) {
            try {
                emit(Resource.loading(dbResult))
                saveToDb(fetchData())
                Lg.v("NetworkBoundResource: Returning data fetched from network")
                emit(Resource.success(loadFromDb() ?: throw IllegalStateException()))
            } catch (e: Exception) {
                Lg.e("NetworkBoundResource Error: $e")
                emit(Resource.error(e, dbResult))
            }
        } else {
            Lg.v("NetworkBoundResource: Returning data from local database")
            emit(Resource.success(dbResult ?: throw IllegalStateException()))
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