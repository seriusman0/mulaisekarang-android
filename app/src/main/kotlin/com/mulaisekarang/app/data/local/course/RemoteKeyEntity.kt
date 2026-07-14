package com.mulaisekarang.app.data.local.course

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row table: the unfiltered Marketplace feed is the only query cached offline
 * (see CourseRepository design), so there is exactly one paging cursor to track.
 */
@Entity(tableName = "course_remote_keys")
data class RemoteKeyEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val nextPage: Int?,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
