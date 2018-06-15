package com.geobotanica.geobotanica.data.repo

import com.geobotanica.geobotanica.data.dao.PhotoDao
import com.geobotanica.geobotanica.data.entity.Photo
import javax.inject.Inject


class PhotoRepo @Inject constructor(val photoDao: PhotoDao) {
    fun get(id: Int): Photo = photoDao.get(id)

    fun getAllPhotosOfPlant(plantId: Int) {
        photoDao.getAllPhotosOfPlant(plantId)
    }

    fun save(plant: Photo) {
        if (plant.id == 0L)
            photoDao.insert(plant)
        else
            photoDao.update(plant)
    }
}