package com.geobotanica.geobotanica.data.repo

import androidx.lifecycle.LiveData
import com.geobotanica.geobotanica.data.dao.PlantPhotoDao
import com.geobotanica.geobotanica.data.entity.PlantPhoto
import javax.inject.Inject


class PlantPhotoRepo @Inject constructor(private val plantPhotoDao: PlantPhotoDao) {

    suspend fun insert(plantPhoto: PlantPhoto): Long = plantPhotoDao.insert(plantPhoto)

    suspend fun update(photo: PlantPhoto): Int = plantPhotoDao.update(photo)

    suspend fun delete(photo: PlantPhoto): Int = plantPhotoDao.delete(photo)

    fun get(id: Long): LiveData<PlantPhoto> = plantPhotoDao.get(id)

    suspend fun getAllPhotosOfPlant(plantId: Long): List<PlantPhoto> = plantPhotoDao.getAllPhotosOfPlant(plantId)

    fun getAllPhotosOfPlantLiveData(plantId: Long): LiveData<List<PlantPhoto>> = plantPhotoDao.getAllPhotosOfPlantLiveData(plantId)

}