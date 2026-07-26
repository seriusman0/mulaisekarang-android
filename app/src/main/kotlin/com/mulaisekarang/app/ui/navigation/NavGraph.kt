package com.mulaisekarang.app.ui.navigation

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mulaisekarang.app.data.network.SESSION_EXPIRED_MESSAGE
import com.mulaisekarang.app.data.network.SessionEventBus
import com.mulaisekarang.app.ui.components.GlassBottomNavBar
import com.mulaisekarang.app.ui.screens.CartScreen
import com.mulaisekarang.app.ui.screens.ChatConversationScreen
import com.mulaisekarang.app.ui.screens.ChatListScreen
import com.mulaisekarang.app.ui.screens.AssignmentScreen
import com.mulaisekarang.app.ui.screens.ChatPaywallScreen
import com.mulaisekarang.app.ui.screens.CheckoutSummaryScreen
import com.mulaisekarang.app.ui.screens.CourseDetailScreen
import com.mulaisekarang.app.ui.screens.CourseEditorScreen
import com.mulaisekarang.app.ui.screens.EditProfileScreen
import com.mulaisekarang.app.ui.screens.InstructorCoursesScreen
import com.mulaisekarang.app.ui.screens.InstructorDashboardScreen
import com.mulaisekarang.app.ui.screens.InstructorGradingScreen
import com.mulaisekarang.app.ui.screens.InstructorWalletScreen
import com.mulaisekarang.app.ui.screens.ForgotPasswordScreen
import com.mulaisekarang.app.ui.screens.HomeScreen
import com.mulaisekarang.app.ui.screens.InstructorProfileScreen
import com.mulaisekarang.app.ui.screens.LessonPlayerScreen
import com.mulaisekarang.app.ui.screens.LoginScreen
import com.mulaisekarang.app.ui.screens.MarketplaceScreen
import com.mulaisekarang.app.ui.screens.MyCoursesScreen
import com.mulaisekarang.app.ui.screens.MyReviewsScreen
import com.mulaisekarang.app.ui.screens.NewGroupScreen
import com.mulaisekarang.app.ui.screens.NotificationsScreen
import com.mulaisekarang.app.ui.screens.PaymentSuccessScreen
import com.mulaisekarang.app.ui.screens.ProfileScreen
import com.mulaisekarang.app.ui.screens.QuizAttemptHistoryScreen
import com.mulaisekarang.app.ui.screens.QuizScreen
import com.mulaisekarang.app.ui.screens.RegisterScreen
import com.mulaisekarang.app.ui.screens.ResetPasswordScreen
import com.mulaisekarang.app.ui.screens.SplashScreen
import com.mulaisekarang.app.ui.screens.SubmissionHistoryScreen
import com.mulaisekarang.app.ui.screens.TransactionHistoryScreen
import com.mulaisekarang.app.viewmodel.AssignmentViewModel
import com.mulaisekarang.app.viewmodel.AuthViewModel
import com.mulaisekarang.app.viewmodel.CartViewModel
import com.mulaisekarang.app.viewmodel.ChatConversationViewModel
import com.mulaisekarang.app.viewmodel.ChatListViewModel
import com.mulaisekarang.app.viewmodel.ChatPaywallViewModel
import com.mulaisekarang.app.viewmodel.CourseDetailViewModel
import com.mulaisekarang.app.viewmodel.CourseEditorViewModel
import com.mulaisekarang.app.viewmodel.ForgotPasswordViewModel
import com.mulaisekarang.app.viewmodel.InstructorCoursesViewModel
import com.mulaisekarang.app.viewmodel.InstructorDashboardViewModel
import com.mulaisekarang.app.viewmodel.InstructorGradingViewModel
import com.mulaisekarang.app.viewmodel.InstructorWalletViewModel
import com.mulaisekarang.app.viewmodel.HomeViewModel
import com.mulaisekarang.app.viewmodel.InstructorProfileViewModel
import com.mulaisekarang.app.viewmodel.LessonPlayerEvent
import com.mulaisekarang.app.viewmodel.LessonPlayerViewModel
import com.mulaisekarang.app.viewmodel.MarketplaceViewModel
import com.mulaisekarang.app.viewmodel.MyCoursesViewModel
import com.mulaisekarang.app.viewmodel.MyReviewsViewModel
import com.mulaisekarang.app.viewmodel.NewGroupViewModel
import com.mulaisekarang.app.viewmodel.NotificationsViewModel
import com.mulaisekarang.app.viewmodel.PaymentSuccessViewModel
import com.mulaisekarang.app.viewmodel.QuizAttemptHistoryViewModel
import com.mulaisekarang.app.viewmodel.QuizViewModel
import com.mulaisekarang.app.viewmodel.ResetPasswordViewModel
import com.mulaisekarang.app.viewmodel.SubmissionHistoryViewModel
import com.mulaisekarang.app.viewmodel.TransactionHistoryViewModel

private object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot-password"
    const val RESET_PASSWORD = "reset-password/{email}"
    const val HOME = "home"
    const val MARKETPLACE = "marketplace"
    const val MY_COURSES = "my-courses"
    const val CHAT_LIST = "chat"
    const val CHAT_CONVERSATION = "chat/{conversationId}"
    const val NEW_GROUP = "new-group"
    const val PROFILE = "profile"
    const val EDIT_PROFILE = "edit-profile"
    const val TRANSACTIONS = "transactions"
    const val CART = "cart"
    const val NOTIFICATIONS = "notifications"
    const val MY_REVIEWS = "my-reviews"
    const val QUIZ_HISTORY = "quiz-attempts"
    const val SUBMISSION_HISTORY = "assignment-submissions"
    const val CHAT_PAYWALL = "chat-paywall"
    const val COURSE_DETAIL = "course/{courseId}"
    const val LESSON_PLAYER = "course/{courseId}/lesson/{lessonId}"
    const val INSTRUCTOR_PROFILE = "instructor/{username}"
    const val QUIZ = "quiz/{quizId}"
    const val ASSIGNMENT = "assignment/{assignmentId}"
    const val CHECKOUT_SUMMARY = "course/{courseId}/checkout"
    const val PAYMENT_SUCCESS = "payment-success/{referenceId}"

    // Instructor portal
    const val INSTRUCTOR_DASHBOARD = "instructor-portal"
    const val INSTRUCTOR_COURSES = "instructor-portal/courses"
    const val INSTRUCTOR_COURSE_CREATE = "instructor-portal/courses/create"
    const val INSTRUCTOR_COURSE_EDIT = "instructor-portal/courses/{courseId}/edit"
    const val INSTRUCTOR_WALLET = "instructor-portal/wallet"
    const val INSTRUCTOR_GRADING = "instructor-portal/grading"

    fun chatConversation(id: Int) = "chat/$id"
    fun courseDetail(id: Int) = "course/$id"
    fun resetPassword(email: String) = "reset-password/${android.net.Uri.encode(email)}"
    fun instructorProfile(username: String) = "instructor/${android.net.Uri.encode(username)}"
    fun quiz(id: Int) = "quiz/$id"
    fun assignment(id: Int) = "assignment/$id"
    fun lessonPlayer(courseId: Int, lessonId: Int) = "course/$courseId/lesson/$lessonId"
    fun checkoutSummary(courseId: Int) = "course/$courseId/checkout"
    fun paymentSuccess(referenceId: String) = "payment-success/${android.net.Uri.encode(referenceId)}"
    fun instructorCourseEdit(courseId: Int) = "instructor-portal/courses/$courseId/edit"

    val BOTTOM_NAV_ROUTES = setOf(HOME, MARKETPLACE, MY_COURSES, CHAT_LIST, PROFILE, COURSE_DETAIL)
}

@Composable
fun MulaiSekarangNavGraph(
    sessionEventBus: SessionEventBus,
    deepLink: android.net.Uri? = null,
    onDeepLinkConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()
    val activity = LocalContext.current as ComponentActivity
    val authViewModel: AuthViewModel = hiltViewModel(activity)
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(Unit) {
        sessionEventBus.events.collect {
            authViewModel.reportError(SESSION_EXPIRED_MESSAGE)
            navController.navigate(Routes.LOGIN) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(deepLink) {
        val uri = deepLink ?: return@LaunchedEffect
        if (uri.host == "payment-return") {
            val referenceId = uri.getQueryParameter("ref")
            if (referenceId != null) {
                navController.navigate(Routes.paymentSuccess(referenceId))
            }
        }
        onDeepLinkConsumed()
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
                        navController.navigate(Routes.HOME) {
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
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                    onNavigateToForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) },
                )
            }

            composable(Routes.FORGOT_PASSWORD) {
                val forgotPasswordViewModel: ForgotPasswordViewModel = hiltViewModel()
                ForgotPasswordScreen(
                    viewModel = forgotPasswordViewModel,
                    onBack = { navController.popBackStack() },
                    onCodeSent = { email ->
                        navController.navigate(Routes.resetPassword(email))
                    },
                )
            }

            composable(
                Routes.RESET_PASSWORD,
                arguments = listOf(navArgument("email") { type = NavType.StringType }),
            ) { backStack ->
                val email = backStack.arguments?.getString("email")?.let { android.net.Uri.decode(it) } ?: ""
                val resetPasswordViewModel: ResetPasswordViewModel = hiltViewModel()
                ResetPasswordScreen(
                    viewModel = resetPasswordViewModel,
                    email = email,
                    onBack = { navController.popBackStack() },
                    onResetSuccess = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                )
            }

            composable(Routes.REGISTER) {
                RegisterScreen(
                    authViewModel = authViewModel,
                    onRegistered = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = { navController.popBackStack() },
                )
            }

            composable(Routes.HOME) {
                val homeViewModel: HomeViewModel = hiltViewModel()
                HomeScreen(
                    viewModel = homeViewModel,
                    authViewModel = authViewModel,
                    onBrowseMarketplace = {
                        navController.navigate(Routes.MARKETPLACE) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onCourseClick = { navController.navigate(Routes.courseDetail(it)) },
                )
            }

            composable(Routes.MARKETPLACE) {
                val marketplaceViewModel: MarketplaceViewModel = hiltViewModel()
                MarketplaceScreen(
                    viewModel = marketplaceViewModel,
                    onCourseClick = { navController.navigate(Routes.courseDetail(it)) },
                )
            }

            composable(Routes.MY_COURSES) {
                val myCoursesViewModel: MyCoursesViewModel = hiltViewModel()
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
                val chatListViewModel: ChatListViewModel = hiltViewModel()
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
                    onNewGroupClick = { navController.navigate(Routes.NEW_GROUP) },
                )
            }

            composable(Routes.NEW_GROUP) {
                val newGroupViewModel: NewGroupViewModel = hiltViewModel()
                NewGroupScreen(
                    viewModel = newGroupViewModel,
                    onBack = { navController.popBackStack() },
                    onGroupCreated = { conversationId ->
                        navController.navigate(Routes.chatConversation(conversationId)) {
                            popUpTo(Routes.CHAT_LIST)
                        }
                    },
                )
            }

            composable(
                Routes.CHAT_CONVERSATION,
                arguments = listOf(navArgument("conversationId") { type = NavType.IntType }),
            ) {
                val chatConversationViewModel: ChatConversationViewModel = hiltViewModel()
                ChatConversationScreen(
                    viewModel = chatConversationViewModel,
                    onBack = { navController.popBackStack() },
                    onChatPaywall = { navController.navigate(Routes.CHAT_PAYWALL) },
                )
            }

            composable(
                Routes.COURSE_DETAIL,
                arguments = listOf(navArgument("courseId") { type = NavType.IntType }),
            ) { backStack ->
                val courseId = backStack.arguments?.getInt("courseId") ?: return@composable
                val detailViewModel: CourseDetailViewModel = hiltViewModel()
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
                        detailViewModel.startConversationWithMentor(username) { conversation ->
                            conversation?.let { navController.navigate(Routes.chatConversation(it.id)) }
                        }
                    },
                    onLessonClick = { lessonId ->
                        navController.navigate(Routes.lessonPlayer(courseId, lessonId))
                    },
                    onInstructorClick = { username ->
                        navController.navigate(Routes.instructorProfile(username))
                    },
                    onQuizClick = { quizId -> navController.navigate(Routes.quiz(quizId)) },
                    onAssignmentClick = { assignmentId -> navController.navigate(Routes.assignment(assignmentId)) },
                    onBuyNow = { id -> navController.navigate(Routes.checkoutSummary(id)) },
                    onOpenCart = { navController.navigate(Routes.CART) },
                    onChatPaywall = { navController.navigate(Routes.CHAT_PAYWALL) },
                )
            }

            composable(Routes.CART) {
                val vm: CartViewModel = hiltViewModel()
                val context = LocalContext.current
                CartScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onOpenInvoice = { url ->
                        CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
                    },
                    onEnrolled = { navController.navigate(Routes.MY_COURSES) },
                )
            }

            composable(Routes.CHAT_PAYWALL) {
                val vm: ChatPaywallViewModel = hiltViewModel()
                val context = LocalContext.current
                ChatPaywallScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onOpenInvoice = { url ->
                        CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
                    },
                )
            }

            composable(Routes.NOTIFICATIONS) {
                val vm: NotificationsViewModel = hiltViewModel()
                NotificationsScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onOpenCourse = { id -> navController.navigate(Routes.courseDetail(id)) },
                )
            }

            composable(Routes.MY_REVIEWS) {
                val vm: MyReviewsViewModel = hiltViewModel()
                MyReviewsScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onOpenCourse = { id -> navController.navigate(Routes.courseDetail(id)) },
                )
            }

            composable(Routes.QUIZ_HISTORY) {
                val vm: QuizAttemptHistoryViewModel = hiltViewModel()
                QuizAttemptHistoryScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.SUBMISSION_HISTORY) {
                val vm: SubmissionHistoryViewModel = hiltViewModel()
                SubmissionHistoryScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                Routes.CHECKOUT_SUMMARY,
                arguments = listOf(navArgument("courseId") { type = NavType.IntType }),
            ) { backStack ->
                val checkoutCourseId = backStack.arguments?.getInt("courseId") ?: return@composable
                val checkoutViewModel: CourseDetailViewModel = hiltViewModel()
                CheckoutSummaryScreen(
                    courseId = checkoutCourseId,
                    viewModel = checkoutViewModel,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                Routes.PAYMENT_SUCCESS,
                arguments = listOf(navArgument("referenceId") { type = NavType.StringType }),
            ) {
                val paymentSuccessViewModel: PaymentSuccessViewModel = hiltViewModel()
                PaymentSuccessScreen(
                    viewModel = paymentSuccessViewModel,
                    onStartLearning = {
                        navController.navigate(Routes.MY_COURSES) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onGoHome = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }

            composable(
                Routes.QUIZ,
                arguments = listOf(navArgument("quizId") { type = NavType.IntType }),
            ) {
                val quizViewModel: QuizViewModel = hiltViewModel()
                QuizScreen(
                    viewModel = quizViewModel,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                Routes.ASSIGNMENT,
                arguments = listOf(navArgument("assignmentId") { type = NavType.IntType }),
            ) {
                val assignmentViewModel: AssignmentViewModel = hiltViewModel()
                AssignmentScreen(
                    viewModel = assignmentViewModel,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                Routes.INSTRUCTOR_PROFILE,
                arguments = listOf(navArgument("username") { type = NavType.StringType }),
            ) {
                val instructorProfileViewModel: InstructorProfileViewModel = hiltViewModel()
                InstructorProfileScreen(
                    viewModel = instructorProfileViewModel,
                    onBack = { navController.popBackStack() },
                    onCourseClick = { navController.navigate(Routes.courseDetail(it)) },
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
                val lessonPlayerViewModel: LessonPlayerViewModel = hiltViewModel()
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
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    },
                    onNavigateToMyCourses = {
                        navController.navigate(Routes.MY_COURSES) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToTransactions = { navController.navigate(Routes.TRANSACTIONS) },
                    onEditProfile = { navController.navigate(Routes.EDIT_PROFILE) },
                    onOpenInstructorPortal = { navController.navigate(Routes.INSTRUCTOR_DASHBOARD) },
                    onNavigateToNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                    onNavigateToMyReviews = { navController.navigate(Routes.MY_REVIEWS) },
                    onNavigateToQuizHistory = { navController.navigate(Routes.QUIZ_HISTORY) },
                    onNavigateToSubmissionHistory = { navController.navigate(Routes.SUBMISSION_HISTORY) },
                    onNavigateToCart = { navController.navigate(Routes.CART) },
                    onNavigateToChatSubscription = { navController.navigate(Routes.CHAT_PAYWALL) },
                )
            }

            composable(Routes.INSTRUCTOR_DASHBOARD) {
                val vm: InstructorDashboardViewModel = hiltViewModel()
                InstructorDashboardScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onCourses = { navController.navigate(Routes.INSTRUCTOR_COURSES) },
                    onWallet = { navController.navigate(Routes.INSTRUCTOR_WALLET) },
                    onGrading = { navController.navigate(Routes.INSTRUCTOR_GRADING) },
                    onChat = {
                        navController.navigate(Routes.CHAT_LIST) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }

            composable(Routes.INSTRUCTOR_COURSES) {
                val vm: InstructorCoursesViewModel = hiltViewModel()
                DisposableEffect(it) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) vm.load()
                    }
                    it.lifecycle.addObserver(observer)
                    onDispose { it.lifecycle.removeObserver(observer) }
                }
                InstructorCoursesScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onCreate = { navController.navigate(Routes.INSTRUCTOR_COURSE_CREATE) },
                    onEdit = { id -> navController.navigate(Routes.instructorCourseEdit(id)) },
                )
            }

            composable(Routes.INSTRUCTOR_COURSE_CREATE) {
                val vm: CourseEditorViewModel = hiltViewModel()
                CourseEditorScreen(
                    viewModel = vm,
                    courseId = null,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }

            composable(
                Routes.INSTRUCTOR_COURSE_EDIT,
                arguments = listOf(navArgument("courseId") { type = NavType.IntType }),
            ) { backStack ->
                val editCourseId = backStack.arguments?.getInt("courseId") ?: return@composable
                val vm: CourseEditorViewModel = hiltViewModel()
                CourseEditorScreen(
                    viewModel = vm,
                    courseId = editCourseId,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }

            composable(Routes.INSTRUCTOR_WALLET) {
                val vm: InstructorWalletViewModel = hiltViewModel()
                InstructorWalletScreen(viewModel = vm, onBack = { navController.popBackStack() })
            }

            composable(Routes.INSTRUCTOR_GRADING) {
                val vm: InstructorGradingViewModel = hiltViewModel()
                InstructorGradingScreen(viewModel = vm, onBack = { navController.popBackStack() })
            }

            composable(Routes.EDIT_PROFILE) {
                EditProfileScreen(
                    authViewModel = authViewModel,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.TRANSACTIONS) {
                val transactionHistoryViewModel: TransactionHistoryViewModel = hiltViewModel()
                TransactionHistoryScreen(viewModel = transactionHistoryViewModel)
            }
        }
    }
}
