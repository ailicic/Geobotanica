package com.geobotanica.geobotanica.data.repo

import com.geobotanica.geobotanica.data.dao.PhotoDao
import com.geobotanica.geobotanica.data.entity.Photo
import javax.inject.Inject


class PhotoRepo @Inject constructor(val photoDao: PhotoDao) {
    fun get(id: Long): Photo = photoDao.get(id)

    fun getAllPhotosOfPlant(plantId: Long) {
        photoDao.getAllPhotosOfPlant(plantId)
    }

    fun insert(photo: Photo): Long = photoDao.insert(photo)
}