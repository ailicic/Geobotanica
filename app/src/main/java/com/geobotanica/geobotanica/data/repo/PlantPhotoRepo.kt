package com.geobotanica.geobotanica.data.repo

import androidx.lifecycle.LiveData
import com.geobotanica.geobotanica.data.dao.PlantPhotoDao
import com.geobotanica.geobotanica.data.entity.PlantPhoto
import javax.inject.Inject


class PlantPhotoRepo @Inject constructor(private val plantPhotoDao: PlantPhotoDao) {

    fun insert(plantPhoto: PlantPhoto): Long = plantPhotoDao.insert(plantPhoto)

    fun get(id: Long): LiveData<PlantPhoto> = plantPhotoDao.get(id)

    fun getAllPhotosOfPlant(plantId: Long): LiveData<List<PlantPhoto>> = plantPhotoDao.getAllPhotosOfPlant(plantId)

    fun getMainPhotoOfPlant(plantId:Long): LiveData<PlantPhoto> = plantPhotoDao.getMainPhotoOfPlant(plantId, PlantPhoto.Type.COMPLETE)
}