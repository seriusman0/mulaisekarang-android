package com.mulaisekarang.app.data.local.course

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mulaisekarang.app.data.model.Category
import com.mulaisekarang.app.data.model.Course
import com.mulaisekarang.app.data.model.Mentor
import com.mulaisekarang.app.data.model.Tag

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey val id: Int,
    val sortOrder: Int,
    val title: String,
    val description: String?,
    val coverImageUrl: String?,
    val type: String?,
    val level: String?,
    val regularPrice: Double,
    val salePrice: Double?,
    val currentPrice: Double,
    val hasDiscount: Boolean,
    val duration: String?,
    val isFeatured: Boolean,
    val reviewsAvgRating: Double?,
    val enrollmentsCount: Int,
    val isEnrolled: Boolean,
    val mentor: Mentor?,
    val category: Category?,
    val tags: List<Tag>,
    val updatedAt: String?,
) {
    fun toDomain(): Course = Course(
        id = id,
        title = title,
        description = description,
        coverImageUrl = coverImageUrl,
        type = type,
        level = level,
        regularPrice = regularPrice,
        salePrice = salePrice,
        currentPrice = currentPrice,
        hasDiscount = hasDiscount,
        duration = duration,
        isFeatured = isFeatured,
        reviewsAvgRating = reviewsAvgRating,
        enrollmentsCount = enrollmentsCount,
        isEnrolled = isEnrolled,
        mentor = mentor,
        category = category,
        tags = tags,
        updatedAt = updatedAt,
    )

    companion object {
        fun fromDomain(course: Course, sortOrder: Int): CourseEntity = CourseEntity(
            id = course.id,
            sortOrder = sortOrder,
            title = course.title,
            description = course.description,
            coverImageUrl = course.coverImageUrl,
            type = course.type,
            level = course.level,
            regularPrice = course.regularPrice,
            salePrice = course.salePrice,
            currentPrice = course.currentPrice,
            hasDiscount = course.hasDiscount,
            duration = course.duration,
            isFeatured = course.isFeatured,
            reviewsAvgRating = course.reviewsAvgRating,
            enrollmentsCount = course.enrollmentsCount,
            isEnrolled = course.isEnrolled,
            mentor = course.mentor,
            category = course.category,
            tags = course.tags,
            updatedAt = course.updatedAt,
        )
    }
}
