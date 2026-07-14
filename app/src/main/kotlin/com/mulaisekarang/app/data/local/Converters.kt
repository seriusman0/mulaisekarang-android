package com.mulaisekarang.app.data.local

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.mulaisekarang.app.data.model.Category
import com.mulaisekarang.app.data.model.Mentor
import com.mulaisekarang.app.data.model.Review
import com.mulaisekarang.app.data.model.Tag
import com.mulaisekarang.app.data.model.Topic
import javax.inject.Inject
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@ProvidedTypeConverter
class Converters @Inject constructor(private val json: Json) {

    @TypeConverter
    fun mentorToJson(mentor: Mentor?): String? = mentor?.let { json.encodeToString(it) }

    @TypeConverter
    fun jsonToMentor(value: String?): Mentor? = value?.let { json.decodeFromString(it) }

    @TypeConverter
    fun categoryToJson(category: Category?): String? = category?.let { json.encodeToString(it) }

    @TypeConverter
    fun jsonToCategory(value: String?): Category? = value?.let { json.decodeFromString(it) }

    @TypeConverter
    fun tagsToJson(tags: List<Tag>): String = json.encodeToString(tags)

    @TypeConverter
    fun jsonToTags(value: String): List<Tag> = if (value.isBlank()) emptyList() else json.decodeFromString(value)

    @TypeConverter
    fun topicsToJson(topics: List<Topic>): String = json.encodeToString(topics)

    @TypeConverter
    fun jsonToTopics(value: String): List<Topic> = if (value.isBlank()) emptyList() else json.decodeFromString(value)

    @TypeConverter
    fun reviewsToJson(reviews: List<Review>): String = json.encodeToString(reviews)

    @TypeConverter
    fun jsonToReviews(value: String): List<Review> = if (value.isBlank()) emptyList() else json.decodeFromString(value)
}
