package com.mulaisekarang.app.data.local.course

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mulaisekarang.app.data.model.Category
import com.mulaisekarang.app.data.model.CourseDetail
import com.mulaisekarang.app.data.model.Mentor
import com.mulaisekarang.app.data.model.Review
import com.mulaisekarang.app.data.model.Tag
import com.mulaisekarang.app.data.model.Topic

@Entity(tableName = "course_details")
data class CourseDetailEntity(
    @PrimaryKey val id: Int,
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
    val lessonsCount: Int,
    val mentor: Mentor?,
    val category: Category?,
    val tags: List<Tag>,
    val topics: List<Topic>,
    val reviews: List<Review>,
    val nextLessonId: Int?,
    val updatedAt: String?,
) {
    fun toDomain(): CourseDetail = CourseDetail(
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
        lessonsCount = lessonsCount,
        mentor = mentor,
        category = category,
        tags = tags,
        topics = topics,
        reviews = reviews,
        nextLessonId = nextLessonId,
        updatedAt = updatedAt,
    )

    companion object {
        fun fromDomain(detail: CourseDetail): CourseDetailEntity = CourseDetailEntity(
            id = detail.id,
            title = detail.title,
            description = detail.description,
            coverImageUrl = detail.coverImageUrl,
            type = detail.type,
            level = detail.level,
            regularPrice = detail.regularPrice,
            salePrice = detail.salePrice,
            currentPrice = detail.currentPrice,
            hasDiscount = detail.hasDiscount,
            duration = detail.duration,
            isFeatured = detail.isFeatured,
            reviewsAvgRating = detail.reviewsAvgRating,
            enrollmentsCount = detail.enrollmentsCount,
            isEnrolled = detail.isEnrolled,
            lessonsCount = detail.lessonsCount,
            mentor = detail.mentor,
            category = detail.category,
            tags = detail.tags,
            topics = detail.topics,
            reviews = detail.reviews,
            nextLessonId = detail.nextLessonId,
            updatedAt = detail.updatedAt,
        )
    }
}
