package ru.finni.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import ru.finni.app.ui.celebration.CelebrationScreen
import ru.finni.app.ui.game.GameViewModel
import ru.finni.app.ui.gameover.GameOverScreen
import ru.finni.app.ui.goals.GoalsScreen
import ru.finni.app.ui.history.HistoryScreen
import ru.finni.app.ui.main.MainScreen
import ru.finni.app.ui.onboarding.OnboardingScreen
import ru.finni.app.ui.period.PeriodEndScreen
import ru.finni.app.ui.pet.CreatePetScreen
import ru.finni.app.ui.plan.PlanScreen
import ru.finni.app.ui.shop.ShopScreen
import ru.finni.app.ui.tasks.TaskPlayScreen
import ru.finni.app.ui.tasks.TasksScreen
import ru.finni.app.ui.adult.AdultScreen
import ru.finni.app.ui.victory.VictoryScreen
import ru.finni.core.design.FinniTheme

object Routes {
    const val ONBOARDING = "onboarding"
    const val CREATE_PET = "create_pet"
    const val MAIN = "main"
    const val PLAN = "plan"
    const val SHOP = "shop"
    const val GOALS = "goals"
    const val TASKS = "tasks"
    const val TASK_PLAY = "task_play"
    const val PERIOD_END = "period_end"
    const val HISTORY = "history"
    const val ADULT = "adult"
    const val GAME_OVER = "game_over"
    const val CELEBRATION = "celebration"
    const val VICTORY = "victory"
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FinniTheme {
                FinniNavHost()
            }
        }
    }
}

@Composable
fun FinniNavHost() {
    val navController = rememberNavController()
    val gameViewModel: GameViewModel = hiltViewModel()

    NavHost(navController = navController, startDestination = Routes.ONBOARDING) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onDone = {
                    navController.navigate(Routes.CREATE_PET) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                },
                onDemo = {
                    navController.navigate(Routes.MAIN) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                },
                gameViewModel = gameViewModel,
            )
        }
        composable(Routes.CREATE_PET) {
            CreatePetScreen(onCreated = {
                navController.navigate(Routes.MAIN) { popUpTo(Routes.CREATE_PET) { inclusive = true } }
            })
        }
        composable(Routes.MAIN) {
            MainScreen(
                onOpenPlan = { navController.navigate(Routes.PLAN) },
                onOpenShop = { navController.navigate(Routes.SHOP) },
                onOpenGoals = { navController.navigate(Routes.GOALS) },
                onOpenTasks = { navController.navigate(Routes.TASKS) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                onPeriodClosed = { gameOver ->
                    navController.navigate(if (gameOver) Routes.GAME_OVER else Routes.PERIOD_END)
                },
                viewModel = gameViewModel,
            )
        }
        composable(Routes.PLAN) {
            PlanScreen(
                onBack = { navController.popBackStack() },
                onRestart = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                },
                viewModel = gameViewModel,
            )
        }
        composable(Routes.SHOP) {
            ShopScreen(
                onBack = { navController.popBackStack() },
                onRestart = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                },
                onVictory = { navController.navigate(Routes.VICTORY) },
                viewModel = gameViewModel,
            )
        }
        composable(Routes.GOALS) {
            GoalsScreen(
                onBack = { navController.popBackStack() },
                onRestart = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                },
                onCelebration = { goalId ->
                    navController.navigate("${Routes.CELEBRATION}/$goalId")
                },
                viewModel = gameViewModel,
            )
        }
        composable(Routes.TASKS) {
            TasksScreen(
                onBack = { navController.popBackStack() },
                onRestart = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                },
                onPlay = { id -> navController.navigate("${Routes.TASK_PLAY}/$id") },
                viewModel = gameViewModel,
            )
        }
        composable(
            route = "${Routes.TASK_PLAY}/{taskId}",
            arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
        ) { entry ->
            TaskPlayScreen(
                taskId = entry.arguments?.getString("taskId") ?: "",
                onBack = { navController.popBackStack() },
                onRestart = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                },
                viewModel = gameViewModel,
            )
        }
        composable(Routes.PERIOD_END) {
            PeriodEndScreen(
                onNextPeriod = {
                    navController.navigate(Routes.MAIN) { popUpTo(Routes.MAIN) { inclusive = true } }
                },
                viewModel = gameViewModel,
            )
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                onBack = { navController.popBackStack() },
                onRestart = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                },
                onAdult = { navController.navigate(Routes.ADULT) },
                viewModel = gameViewModel,
            )
        }
        composable(Routes.ADULT) {
            AdultScreen(
                onBack = { navController.popBackStack() },
                onRestart = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                },
                onReset = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                },
                viewModel = gameViewModel,
            )
        }
        composable(Routes.GAME_OVER) {
            GameOverScreen(
                onRestart = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                },
                onHome = { navController.popBackStack() },
                viewModel = gameViewModel,
            )
        }
        composable(Routes.VICTORY) {
            VictoryScreen(
                onRestart = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                },
                onHome = { navController.popBackStack(Routes.MAIN, false) },
                viewModel = gameViewModel,
            )
        }
        composable(
            route = "${Routes.CELEBRATION}/{goalId}",
            arguments = listOf(navArgument("goalId") { type = NavType.StringType }),
        ) { entry ->
            CelebrationScreen(
                goalId = entry.arguments?.getString("goalId") ?: "",
                onDone = { navController.popBackStack() },
                viewModel = gameViewModel,
            )
        }
    }
}
