package com.geobotanica.geobotanica.data.repo

import androidx.lifecycle.LiveData
import com.geobotanica.geobotanica.data.dao.PhotoDao
import com.geobotanica.geobotanica.data.entity.Photo
import javax.inject.Inject


class PhotoRepo @Inject constructor(private val photoDao: PhotoDao) {
    fun get(id: Long): LiveData<Photo> = photoDao.get(id)

    fun getAllPhotosOfPlant(plantId: Long): LiveData<List<Photo>> = photoDao.getAllPhotosOfPlant(plantId)

    fun getMainPhotoOfPlant(plantId:Long): LiveData<Photo> = photoDao.getMainPhotoOfPlant(plantId, Photo.Type.COMPLETE)

    fun insert(photo: Photo): Long = photoDao.insert(photo)
}