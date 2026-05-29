package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "saved_posters")
data class SavedPoster(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val textA: String,
    val emojiA: String,
    val textB: String,
    val emojiB: String,
    val fontSize: Int = 36, // size index or sp
    val emojiSize: Int = 48,
    val textColorHex: String = "#000000",
    val isVerticalLayout: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val imagePathA: String? = null,
    val imagePathB: String? = null,
    val imageSizeA: Float = 120f,
    val imageSizeB: Float = 120f,
    val featherA: Float = 0f,
    val featherB: Float = 0f
)

@Dao
interface SavedPosterDao {
    @Query("SELECT * FROM saved_posters ORDER BY timestamp DESC")
    fun getAllPosters(): Flow<List<SavedPoster>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoster(poster: SavedPoster)

    @Query("DELETE FROM saved_posters WHERE id = :id")
    suspend fun deletePosterById(id: Int)
}

@Database(entities = [SavedPoster::class], version = 2, exportSchema = false)
abstract class PosterDatabase : RoomDatabase() {
    abstract fun savedPosterDao(): SavedPosterDao

    companion object {
        @Volatile
        private var INSTANCE: PosterDatabase? = null

        fun getDatabase(context: Context): PosterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PosterDatabase::class.java,
                    "poster_database"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class PosterRepository(private val dao: SavedPosterDao) {
    val allPosters: Flow<List<SavedPoster>> = dao.getAllPosters()

    suspend fun insert(poster: SavedPoster) {
        dao.insertPoster(poster)
    }

    suspend fun deleteById(id: Int) {
        dao.deletePosterById(id)
    }
}
