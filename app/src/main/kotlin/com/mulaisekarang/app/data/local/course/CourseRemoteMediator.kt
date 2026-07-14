package com.mulaisekarang.app.data.local.course

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.mulaisekarang.app.data.local.AppDatabase
import com.mulaisekarang.app.data.network.ApiService

/**
 * Caches only the unfiltered default course feed (no search/category/sort). The backend's
 * custom {data, meta} response has no links.next, and CourseController hardcodes 12 items
 * per page server-side, so `meta.perPage`/`meta.currentPage`/`meta.lastPage` are the only
 * signals used to drive paging and sort order.
 */
@OptIn(ExperimentalPagingApi::class)
class CourseRemoteMediator(
    private val api: ApiService,
    private val database: AppDatabase,
) : RemoteMediator<Int, CourseEntity>() {

    private val dao = database.courseDao()

    override suspend fun load(loadType: LoadType, state: PagingState<Int, CourseEntity>): MediatorResult {
        return try {
            val page = when (loadType) {
                LoadType.REFRESH -> 1
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> dao.remoteKey()?.nextPage
                    ?: return MediatorResult.Success(endOfPaginationReached = true)
            }

            val response = api.courses(page = page)

            database.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    dao.clearCourses()
                    dao.clearRemoteKeys()
                }
                val entities = response.data.mapIndexed { index, course ->
                    CourseEntity.fromDomain(course, sortOrder = (page - 1) * response.meta.perPage + index)
                }
                dao.upsertAll(entities)

                val nextPage = (page + 1).takeIf { response.meta.currentPage < response.meta.lastPage }
                dao.upsertRemoteKey(RemoteKeyEntity(nextPage = nextPage))
            }

            MediatorResult.Success(endOfPaginationReached = response.meta.currentPage >= response.meta.lastPage)
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }
}
