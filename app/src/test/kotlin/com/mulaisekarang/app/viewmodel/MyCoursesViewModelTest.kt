package com.mulaisekarang.app.viewmodel

import com.mulaisekarang.app.data.CourseRepository
import com.mulaisekarang.app.data.EnrollmentRepository
import com.mulaisekarang.app.data.model.CoursesResponse
import com.mulaisekarang.app.data.model.EnrollmentsResponse
import com.mulaisekarang.app.data.model.PaginationMeta
import com.mulaisekarang.app.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class MyCoursesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val enrollmentRepository: EnrollmentRepository = mockk()
    private val courseRepository: CourseRepository = mockk()

    @Test
    fun `myCourses failure surfaces error message in ui state`() = runTest {
        coEvery { enrollmentRepository.myCourses() } throws RuntimeException("Gagal memuat kursus saya.")
        coEvery { courseRepository.courses(featured = true) } returns
            CoursesResponse(emptyList(), PaginationMeta(1, 1, 15, 0))

        val viewModel = MyCoursesViewModel(enrollmentRepository, courseRepository)
        advanceUntilIdle()

        assertEquals("Gagal memuat kursus saya.", viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.enrollments.isEmpty())
    }

    @Test
    fun `recommended-courses failure does not clobber a successful main load`() = runTest {
        coEvery { enrollmentRepository.myCourses() } returns EnrollmentsResponse(emptyList(), PaginationMeta(1, 1, 15, 0))
        coEvery { courseRepository.courses(featured = true) } throws RuntimeException("boom")

        val viewModel = MyCoursesViewModel(enrollmentRepository, courseRepository)
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.recommended.isEmpty())
    }
}
