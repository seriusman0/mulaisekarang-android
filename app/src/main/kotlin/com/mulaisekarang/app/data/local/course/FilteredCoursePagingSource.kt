package com.mulaisekarang.app.data.local.course

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.mulaisekarang.app.data.model.Course
import com.mulaisekarang.app.data.network.ApiService

/**
 * Network-only paging for search/filtered Marketplace results — ephemeral by nature,
 * so unlike the default feed (see CourseRemoteMediator) they aren't cached in Room.
 */
class FilteredCoursePagingSource(
    private val api: ApiService,
    private val search: String?,
    private val category: String?,
    private val sort: String?,
) : PagingSource<Int, Course>() {

    override fun getRefreshKey(state: PagingState<Int, Course>): Int? =
        state.anchorPosition?.let { anchor ->
            val page = state.closestPageToPosition(anchor)
            page?.prevKey?.plus(1) ?: page?.nextKey?.minus(1)
        }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Course> {
        val page = params.key ?: 1
        return try {
            val response = api.courses(
                search = search?.takeIf { it.isNotBlank() },
                categories = category?.let { listOf(it) } ?: emptyList(),
                sort = sort,
                page = page,
            )
            LoadResult.Page(
                data = response.data,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (response.meta.currentPage < response.meta.lastPage) page + 1 else null,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
