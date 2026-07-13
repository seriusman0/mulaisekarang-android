package com.mulaisekarang.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mulaisekarang.app.AppContainer
import com.mulaisekarang.app.data.network.SESSION_EXPIRED_MESSAGE
import com.mulaisekarang.app.ui.components.GlassBottomNavBar
import com.mulaisekarang.app.ui.screens.ChatConversationScreen
import com.mulaisekarang.app.ui.screens.ChatListScreen
import com.mulaisekarang.app.ui.screens.CourseDetailScreen
import com.mulaisekarang.app.ui.screens.LessonPlayerScreen
import com.mulaisekarang.app.ui.screens.LoginScreen
import com.mulaisekarang.app.ui.screens.MarketplaceScreen
import com.mulaisekarang.app.ui.screens.MyCoursesScreen
import com.mulaisekarang.app.ui.screens.ProfileScreen
import com.mulaisekarang.app.ui.screens.RegisterScreen
import com.mulaisekarang.app.ui.screens.SplashScreen
import com.mulaisekarang.app.viewmodel.AuthViewModel
import com.mulaisekarang.app.viewmodel.ChatConversationViewModel
import com.mulaisekarang.app.viewmodel.ChatListViewModel
import com.mulaisekarang.app.viewmodel.CourseDetailViewModel
import com.mulaisekarang.app.viewmodel.LessonPlayerEvent
import com.mulaisekarang.app.viewmodel.LessonPlayerViewModel
import com.mulaisekarang.app.viewmodel.MarketplaceViewModel
import com.mulaisekarang.app.viewmodel.MyCoursesViewModel
import kotlinx.coroutines.launch

private object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MARKETPLACE = "marketplace"
    const val MY_COURSES = "my-courses"
    const val CHAT_LIST = "chat"
    const val CHAT_CONVERSATION = "chat/{conversationId}"
    const val PROFILE = "profile"
    const val COURSE_DETAIL = "course/{courseId}"
    const val LESSON_PLAYER = "course/{courseId}/lesson/{lessonId}"

    fun chatConversation(id: Int) = "chat/$id"
    fun courseDetail(id: Int) = "course/$id"
    fun lessonPlayer(courseId: Int, lessonId: Int) = "course/$courseId/lesson/$lessonId"

    val BOTTOM_NAV_ROUTES = setOf(MARKETPLACE, MY_COURSES, CHAT_LIST, PROFILE, COURSE_DETAIL)
}

@Composable
fun MulaiSekarangNavGraph(appContainer: AppContainer) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val authViewModel: AuthViewModel = viewModel(
        factory = viewModelFactoryOf { AuthViewModel(appContainer.authRepository) },
    )
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(Unit) {
        appContainer.sessionEventBus.events.collect {
            authViewModel.reportError(SESSION_EXPIRED_MESSAGE)
            navController.navigate(Routes.LOGIN) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (currentRoute in Routes.BOTTOM_NAV_ROUTES) {
                GlassBottomNavBar(
                    currentRoute = currentRoute,
                    onTabSelected = { tab ->
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { scaffoldPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.padding(
                bottom = if (currentRoute in Routes.BOTTOM_NAV_ROUTES) scaffoldPadding.calculateBottomPadding() else 0.dp,
            ),
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(
                    authViewModel = authViewModel,
                    onLoggedIn = {
                        navController.navigate(Routes.MARKETPLACE) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    },
                    onLoggedOut = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    },
                )
            }

            composable(Routes.LOGIN) {
                LoginScreen(
                    authViewModel = authViewModel,
                    onLoggedIn = {
                        navController.navigate(Routes.MARKETPLACE) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                )
            }

            composable(Routes.REGISTER) {
                RegisterScreen(
                    authViewModel = authViewModel,
                    onRegistered = {
                        navController.navigate(Routes.MARKETPLACE) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = { navController.popBackStack() },
                )
            }

            composable(Routes.MARKETPLACE) {
                val marketplaceViewModel: MarketplaceViewModel = viewModel(
                    factory = viewModelFactoryOf { MarketplaceViewModel(appContainer.courseRepository) },
                )
                MarketplaceScreen(
                    viewModel = marketplaceViewModel,
                    onCourseClick = { navController.navigate(Routes.courseDetail(it)) },
                )
            }

            composable(Routes.MY_COURSES) {
                val myCoursesViewModel: MyCoursesViewModel = viewModel(
                    factory = viewModelFactoryOf { MyCoursesViewModel(appContainer.enrollmentRepository) },
                )
                MyCoursesScreen(
                    viewModel = myCoursesViewModel,
                    onCourseClick = { navController.navigate(Routes.courseDetail(it)) },
                    onBrowseMarketplace = {
                        navController.navigate(Routes.MARKETPLACE) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }

            composable(Routes.CHAT_LIST) { backStack ->
                val chatListViewModel: ChatListViewModel = viewModel(
                    factory = viewModelFactoryOf { ChatListViewModel(appContainer.chatRepository) },
                )
                DisposableEffect(backStack) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) chatListViewModel.load()
                    }
                    backStack.lifecycle.addObserver(observer)
                    onDispose { backStack.lifecycle.removeObserver(observer) }
                }
                ChatListScreen(
                    viewModel = chatListViewModel,
                    onConversationClick = { navController.navigate(Routes.chatConversation(it)) },
                )
            }

            composable(
                Routes.CHAT_CONVERSATION,
                arguments = listOf(navArgument("conversationId") { type = NavType.IntType }),
            ) { backStack ->
                val conversationId = backStack.arguments?.getInt("conversationId") ?: return@composable
                val chatConversationViewModel: ChatConversationViewModel = viewModel(
                    factory = viewModelFactoryOf {
                        ChatConversationViewModel(appContainer.chatRepository, conversationId)
                    },
                )
                ChatConversationScreen(
                    viewModel = chatConversationViewModel,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                Routes.COURSE_DETAIL,
                arguments = listOf(navArgument("courseId") { type = NavType.IntType }),
            ) { backStack ->
                val courseId = backStack.arguments?.getInt("courseId") ?: return@composable
                val detailViewModel: CourseDetailViewModel = viewModel(
                    factory = viewModelFactoryOf { CourseDetailViewModel(appContainer.courseRepository) },
                )
                DisposableEffect(backStack) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) detailViewModel.checkPendingPayment()
                    }
                    backStack.lifecycle.addObserver(observer)
                    onDispose { backStack.lifecycle.removeObserver(observer) }
                }
                CourseDetailScreen(
                    courseId = courseId,
                    viewModel = detailViewModel,
                    onBack = { navController.popBackStack() },
                    onChatWithMentor = { username ->
                        coroutineScope.launch {
                            runCatching { appContainer.chatRepository.startConversation(username) }
                                .onSuccess { conversation ->
                                    navController.navigate(Routes.chatConversation(conversation.id))
                                }
                        }
                    },
                    onLessonClick = { lessonId ->
                        navController.navigate(Routes.lessonPlayer(courseId, lessonId))
                    },
                )
            }

            composable(
                Routes.LESSON_PLAYER,
                arguments = listOf(
                    navArgument("courseId") { type = NavType.IntType },
                    navArgument("lessonId") { type = NavType.IntType },
                ),
            ) { backStack ->
                val lessonCourseId = backStack.arguments?.getInt("courseId") ?: return@composable
                val lessonId = backStack.arguments?.getInt("lessonId") ?: return@composable
                val lessonPlayerViewModel: LessonPlayerViewModel = viewModel(
                    factory = viewModelFactoryOf {
                        LessonPlayerViewModel(appContainer.lessonRepository, appContainer.tokenStore, lessonCourseId, lessonId)
                    },
                )
                LessonPlayerScreen(
                    viewModel = lessonPlayerViewModel,
                    onNavigateToLesson = { nextLessonId ->
                        navController.navigate(Routes.lessonPlayer(lessonCourseId, nextLessonId)) {
                            popUpTo(Routes.lessonPlayer(lessonCourseId, lessonId)) { inclusive = true }
                        }
                    },
                    onCourseCompleted = {
                        navController.navigate(Routes.courseDetail(lessonCourseId)) {
                            popUpTo(Routes.COURSE_DETAIL) { inclusive = false }
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    authViewModel = authViewModel,
                    onLoggedOut = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.MARKETPLACE) { inclusive = true }
                        }
                    },
                )
            }
        }
    }
}
