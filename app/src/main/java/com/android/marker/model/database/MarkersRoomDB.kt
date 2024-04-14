package com.android.marker.model.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.android.marker.model.database.daos.MarkersDao
import com.android.marker.model.models.SavedMarkers

@Database(entities = [SavedMarkers::class], version = 1)
abstract class MarkersRoomDB:RoomDatabase() {
    abstract fun markerDao(): MarkersDao

    companion object {
        @Volatile
        private var INSTANCE: MarkersRoomDB? = null

        fun getDatabase(context: Context): MarkersRoomDB {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MarkersRoomDB::class.java,
                    "marker_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
