package com.example.caltrack.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.caltrack.R
import com.example.caltrack.data.AppSettings
import com.example.caltrack.data.ThemeMode
import com.example.caltrack.ui.menu.MenuScreen
import com.example.caltrack.ui.meals.MealsRoute
import com.example.caltrack.ui.meals.MealsViewModel
import com.example.caltrack.ui.stats.StatisticsRoute
import com.example.caltrack.ui.stats.StatisticsViewModel
import com.example.caltrack.ui.today.TodayRoute
import com.example.caltrack.ui.today.TodayViewModel
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

enum class AppSection(
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
    @param:DrawableRes val selectedIcon: Int,
) {
    Today(R.string.today, R.drawable.ic_today, R.drawable.ic_today_filled),
    Stats(R.string.section_stats, R.drawable.ic_bar_chart_outlined, R.drawable.ic_bar_chart),
    Meals(R.string.section_meals, R.drawable.ic_restaurant_outlined, R.drawable.ic_restaurant),
    Menu(R.string.section_menu, R.drawable.ic_menu, R.drawable.ic_menu_filled),
}

private val FabSize = 56.dp
private val FabGap = 16.dp

/** Extra bottom space on Azi so the last row also clears the + button above the bar. */
private val FabClearance = FabSize + FabGap

/** How long a snackbar with an Undo action stays up. */
private val SnackbarTimeout = 5.seconds

@Composable
fun CalTrackAppShell(
    settings: AppSettings,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
) {
    var section by rememberSaveable { mutableStateOf(AppSection.Today) }
    val todayViewModel: TodayViewModel = viewModel(factory = TodayViewModel.Factory)
    val mealsViewModel: MealsViewModel = viewModel(factory = MealsViewModel.Factory)
    val statisticsViewModel: StatisticsViewModel = viewModel(factory = StatisticsViewModel.Factory)
    val snackbarHostState = remember { SnackbarHostState() }
    // Keeps each section's saveable state (scroll position, search text) while another one is shown
    val sectionStates = rememberSaveableStateHolder()
    val motionScheme = MaterialTheme.motionScheme
    val layoutDirection = LocalLayoutDirection.current

    BackHandler(enabled = section != AppSection.Today) { section = AppSection.Today }

    val showSnackbar: suspend (String, String?) -> Boolean = { message, actionLabel ->
        // Only the newest snackbar matters, e.g. only the last deletion can be undone
        snackbarHostState.currentSnackbarData?.dismiss()
        withTimeoutOrNull(SnackbarTimeout) {
            snackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = SnackbarDuration.Indefinite,
            )
        } == SnackbarResult.ActionPerformed
    }

    Scaffold { innerPadding ->
        val fabVisible = section == AppSection.Today
        fun screenPadding(extraBottom: Dp = 0.dp) = PaddingValues(
            start = innerPadding.calculateStartPadding(layoutDirection),
            top = innerPadding.calculateTopPadding(),
            end = innerPadding.calculateEndPadding(layoutDirection),
            bottom = innerPadding.calculateBottomPadding() + FloatingNavBarClearance + extraBottom,
        )

        Box(Modifier.fillMaxSize()) {
            // The entering section fades and settles in; a fresh Animatable per section starts it
            // invisible on its very first frame. Rotation restores the section without animating.
            var settledSection by rememberSaveable { mutableStateOf(section) }
            val enter = remember(section) { Animatable(if (section == settledSection) 1f else 0f) }
            LaunchedEffect(section) {
                settledSection = section
                enter.animateTo(1f, motionScheme.defaultEffectsSpec())
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = enter.value
                        val scale = 0.97f + 0.03f * enter.value
                        scaleX = scale
                        scaleY = scale
                    },
            ) {
                sectionStates.SaveableStateProvider(section.name) {
                    when (section) {
                        AppSection.Today -> TodayRoute(
                            viewModel = todayViewModel,
                            contentPadding = screenPadding(extraBottom = FabClearance),
                            onShowSnackbar = showSnackbar,
                        )
                        AppSection.Stats -> StatisticsRoute(
                            viewModel = statisticsViewModel,
                            contentPadding = screenPadding(),
                        )
                        AppSection.Meals -> MealsRoute(
                            viewModel = mealsViewModel,
                            selectedDate = todayViewModel::selectedDate,
                            contentPadding = screenPadding(),
                            onShowSnackbar = showSnackbar,
                        )
                        AppSection.Menu -> MenuScreen(
                            settings = settings,
                            onThemeModeChange = onThemeModeChange,
                            onDynamicColorChange = onDynamicColorChange,
                            contentPadding = screenPadding(),
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = fabVisible,
                enter = scaleIn(motionScheme.fastSpatialSpec()) + fadeIn(motionScheme.fastEffectsSpec()),
                exit = scaleOut(motionScheme.fastSpatialSpec()) + fadeOut(motionScheme.fastEffectsSpec()),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(end = 24.dp, bottom = FloatingNavBarBottomGap + FloatingNavBarHeight + FabGap),
            ) {
                FloatingActionButton(
                    onClick = todayViewModel::startAdding,
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = stringResource(R.string.add_food),
                    )
                }
            }

            FloatingNavBar(
                selected = section,
                onSelect = { section = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = 24.dp, end = 24.dp, bottom = FloatingNavBarBottomGap),
            )

            // Above the bar, and above the + button while it's shown
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(
                        bottom = FloatingNavBarBottomGap + FloatingNavBarHeight + 4.dp +
                            if (fabVisible) FabClearance else 0.dp,
                    ),
            )
        }
    }
}
