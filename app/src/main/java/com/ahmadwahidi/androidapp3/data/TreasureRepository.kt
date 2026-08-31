package com.ahmadwahidi.androidapp3.data

/** Keeps Room database work separate from Activity code. */
class TreasureRepository(private val dao: TreasurePlaceDao) {

    suspend fun seedDatabaseIfNeeded() {
        if (dao.getAllPlaces().isEmpty()) dao.insertAll(TreasureSeedData.places)
    }

    suspend fun getAllPlaces(): List<TreasurePlace> = dao.getAllPlaces()
    suspend fun getPlaceById(id: Int): TreasurePlace? = dao.getPlaceById(id)
    suspend fun getCurrentPlace(): TreasurePlace? = dao.getCurrentPlace()
    suspend fun getVisitedCount(): Int = dao.getVisitedCount()
    suspend fun markVisited(id: Int) = dao.markVisited(id)
    suspend fun updateNotes(id: Int, notes: String) = dao.updateNotes(id, notes)
    suspend fun updatePhoto(id: Int, photoUri: String?) = dao.updatePhoto(id, photoUri)
    suspend fun resetProgress() = dao.resetProgress()
}
