package com.mulaisekarang.app.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.mulaisekarang.app.data.local.AppDatabase
import com.mulaisekarang.app.data.local.course.CourseDao
import com.mulaisekarang.app.data.local.course.CourseDetailEntity
import com.mulaisekarang.app.data.local.course.CourseEntity
import com.mulaisekarang.app.data.local.course.CourseRemoteMediator
import com.mulaisekarang.app.data.local.course.FilteredCoursePagingSource
import com.mulaisekarang.app.data.model.Category
import com.mulaisekarang.app.data.model.CheckoutResult
import com.mulaisekarang.app.data.model.Course
import com.mulaisekarang.app.data.model.CourseDetail
import com.mulaisekarang.app.data.model.CoursesResponse
import com.mulaisekarang.app.data.model.toCourseSummary
import com.mulaisekarang.app.data.network.ApiService
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val PAGE_SIZE = 20

class CourseRepository @Inject constructor(
    private val api: ApiService,
    private val courseDao: CourseDao,
    private val database: AppDatabase,
) {

    /** Small, non-paginated fetches (e.g. Home/MyCourses "featured" strips) — not part of the SSOT pilot. */
    suspend fun courses(
        search: String? = null,
        category: String? = null,
        sort: String? = null,
        featured: Boolean? = null,
        page: Int = 1,
    ): CoursesResponse = api.courses(
        search = search?.takeIf { it.isNotBlank() },
        categories = category?.let { listOf(it) } ?: emptyList(),
        sort = sort,
        featured = featured,
        page = page,
    )

    @OptIn(ExperimentalPagingApi::class)
    fun pagedCourses(): Flow<PagingData<Course>> = Pager(
        config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
        remoteMediator = CourseRemoteMediator(api, database),
        pagingSourceFactory = { courseDao.pagingSource() },
    ).flow.map { pagingData -> pagingData.map(CourseEntity::toDomain) }

    fun pagedCoursesFiltered(search: String?, category: String?, sort: String?): Flow<PagingData<Course>> = Pager(
        config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
        pagingSourceFactory = { FilteredCoursePagingSource(api, search, category, sort) },
    ).flow

    fun courseDetail(id: Int): Flow<CourseDetail?> = courseDao.observeDetail(id).map { it?.toDomain() }

    suspend fun refreshCourseDetail(id: Int) {
        val detail = api.courseDetail(id).data
        val existingDetail = courseDao.getDetailById(id)
        if (existingDetail != null && existingDetail.updatedAt != null && existingDetail.updatedAt == detail.updatedAt) {
            return
        }

        courseDao.upsertDetail(CourseDetailEntity.fromDomain(detail))

        val existingCourseRow = courseDao.getById(id)
        val sortOrder = existingCourseRow?.sortOrder ?: Int.MAX_VALUE
        courseDao.upsertCourse(CourseEntity.fromDomain(detail.toCourseSummary(), sortOrder = sortOrder))
    }

    suspend fun categories(): List<Category> = api.categories().data

    suspend fun checkout(courseId: Int): CheckoutResult = api.checkout(courseId).data

    suspend fun paymentStatus(referenceId: String): String = api.paymentStatus(referenceId).data.status
}
