package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.BrasManagerDatabase
import com.example.data.local.LocalCareerRepository
import com.example.data.remote.EspnSoccerApi
import com.example.data.remote.SquadSyncRepository
import com.example.ui.screens.CareerHubScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FinancesScreen
import com.example.ui.screens.LiveMatchScreen
import com.example.ui.screens.MarketSyncScreen
import com.example.ui.screens.NewsFeedScreen
import com.example.ui.screens.SquadTacticsScreen
import com.example.ui.screens.TableCalendarScreen
import com.example.ui.theme.BrasManagerTheme
import com.example.ui.viewmodel.BrasManagerViewModel

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_SQUAD = "squad"
const val ROUTE_TABLE = "table"
const val ROUTE_NEWS = "news"
const val ROUTE_MARKET = "market"
const val ROUTE_FINANCES = "finances"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BrasManagerTheme {
                BrasManagerRoot()
            }
        }
    }
}

@Composable
fun BrasManagerRoot() {
    BrasManagerApp()
}

@Composable
private fun BrasManagerApp() {
    val viewModel: BrasManagerViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) { "APPLICATION_KEY missing" } as Application
                val localDb = BrasManagerDatabase.getInstance(app)
                val careerRepo = LocalCareerRepository(localDb.careerLocalDao())
                val syncRepo = SquadSyncRepository(EspnSoccerApi.create(), localDb.squadCacheDao())
                BrasManagerViewModel(
                    careerRepo = careerRepo,
                    syncRepo = syncRepo
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.saveCurrentCareerNow(silent = true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(uiState.snackMessage, uiState.errorMessage) {
        val msg = uiState.snackMessage ?: uiState.errorMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackMessage()
        }
    }

    if (uiState.isInitializing) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Live Match Full-Screen Mode
    val liveMatch = uiState.liveMatch
    if (liveMatch != null) {
        LiveMatchScreen(
            liveMatch = liveMatch,
            uiState = uiState,
            onTogglePause = viewModel::toggleMatchPause,
            onSetSpeed = viewModel::setMatchSpeed,
            onSetMentality = viewModel::setLiveMentality,
            onMakeSub = viewModel::makeLiveSubstitution,
            onSkipToFullTime = viewModel::skipToFullTime,
            onFinishRound = viewModel::finishMatchAndAdvanceRound,
            onToggleSound = viewModel::toggleMatchSound
        )
        return
    }

    // Career Hub vs Active Club Dashboard
    if (uiState.activeCareer == null) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.navigationBars
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                CareerHubScreen(
                    uiState = uiState,
                    onResumeCareer = viewModel::resumeCareer,
                    onStartNewCareer = viewModel::startNewCareer,
                    onDeleteCareer = viewModel::deleteCareer,
                    onSyncNow = { viewModel.syncLiveSquads(automatic = false) },
                    onUpdateManagerName = viewModel::updateManagerName
                )
            }
        }
    } else {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_DASHBOARD

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                NavigationBar(windowInsets = WindowInsets.navigationBars) {
                    NavigationBarItem(
                        selected = currentRoute == ROUTE_DASHBOARD,
                        onClick = {
                            navController.navigate(ROUTE_DASHBOARD) {
                                popUpTo(ROUTE_DASHBOARD) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                if (currentRoute == ROUTE_DASHBOARD) Icons.Filled.Shield else Icons.Outlined.Shield,
                                contentDescription = "Clube"
                            )
                        },
                        label = { Text("Clube") },
                        modifier = Modifier.testTag("nav_clube")
                    )
                    NavigationBarItem(
                        selected = currentRoute == ROUTE_SQUAD,
                        onClick = {
                            navController.navigate(ROUTE_SQUAD) {
                                popUpTo(ROUTE_DASHBOARD) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                if (currentRoute == ROUTE_SQUAD) Icons.Filled.Groups else Icons.Outlined.Groups,
                                contentDescription = "Elenco"
                            )
                        },
                        label = { Text("Elenco") },
                        modifier = Modifier.testTag("nav_elenco")
                    )
                    NavigationBarItem(
                        selected = currentRoute == ROUTE_TABLE,
                        onClick = {
                            navController.navigate(ROUTE_TABLE) {
                                popUpTo(ROUTE_DASHBOARD) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                if (currentRoute == ROUTE_TABLE) Icons.Filled.Leaderboard else Icons.Outlined.Leaderboard,
                                contentDescription = "Tabela"
                            )
                        },
                        label = { Text("Tabela") },
                        modifier = Modifier.testTag("nav_tabela")
                    )
                    NavigationBarItem(
                        selected = currentRoute == ROUTE_NEWS,
                        onClick = {
                            navController.navigate(ROUTE_NEWS) {
                                popUpTo(ROUTE_DASHBOARD) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                if (currentRoute == ROUTE_NEWS) Icons.Filled.Newspaper else Icons.Outlined.Newspaper,
                                contentDescription = "Notícias"
                            )
                        },
                        label = { Text("Notícias") },
                        modifier = Modifier.testTag("nav_noticias")
                    )
                    NavigationBarItem(
                        selected = currentRoute == ROUTE_MARKET,
                        onClick = {
                            navController.navigate(ROUTE_MARKET) {
                                popUpTo(ROUTE_DASHBOARD) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                if (currentRoute == ROUTE_MARKET) Icons.Filled.ShoppingBag else Icons.Outlined.ShoppingBag,
                                contentDescription = "Mercado & Sync"
                            )
                        },
                        label = { Text("Mercado") },
                        modifier = Modifier.testTag("nav_mercado")
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = ROUTE_DASHBOARD,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(ROUTE_DASHBOARD) {
                    DashboardScreen(
                        uiState = uiState,
                        onPlayRound = viewModel::startLiveMatch,
                        onPlayCupMatch = viewModel::startCupMatch,
                        onPlayChampionsMatch = viewModel::startChampionsLeagueMatch,
                        onPlayContinentalMatch = viewModel::startContinentalMatch,
                        onStartNextSeason = viewModel::startNextSeason,
                        onTicketPriceChange = viewModel::setTicketPriceLevel,
                        onTrainingChange = viewModel::setTrainingFocus,
                        onExpandStadium = viewModel::expandStadium,
                        onTakeLoan = viewModel::takeBankLoan,
                        onAcceptOffer = viewModel::acceptIncomingOffer,
                        onRejectOffer = viewModel::rejectIncomingOffer,
                        onAcceptJobOffer = viewModel::acceptJobOffer,
                        onTakeEmergencyRehire = viewModel::takeEmergencyRehire,
                        onSaveCareer = { viewModel.saveCurrentCareerNow(silent = false) },
                        onVotePresidentialElection = viewModel::votePresidentialElection,
                        onOpenFinances = {
                            navController.navigate(ROUTE_FINANCES) {
                                launchSingleTop = true
                            }
                        },
                        onExitCareer = viewModel::exitToCareerHub
                    )
                }
                composable(ROUTE_FINANCES) {
                    FinancesScreen(
                        uiState = uiState,
                        onTicketPriceChange = viewModel::setTicketPriceLevel,
                        onExpandStadium = viewModel::expandStadium,
                        onTakeLoan = viewModel::takeBankLoan,
                        onSelectSponsor = viewModel::selectSponsor,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(ROUTE_SQUAD) {
                    SquadTacticsScreen(
                        uiState = uiState,
                        onToggleStarter = viewModel::toggleStarter,
                        onAutoBestLineup = viewModel::autoSelectBestLineup,
                        onUpdateTactics = viewModel::updateTactics,
                        onToggleForSale = viewModel::togglePlayerForSale,
                        onPromoteYouth = viewModel::promoteYouthProspect,
                        onRenewContract = viewModel::renewPlayerContract,
                        onTreatInjury = viewModel::treatInjuredPlayer
                    )
                }
                composable(ROUTE_TABLE) {
                    TableCalendarScreen(
                        uiState = uiState,
                        onVoteBallonDor = viewModel::voteBallonDor
                    )
                }
                composable(ROUTE_NEWS) {
                    NewsFeedScreen(uiState = uiState)
                }
                composable(ROUTE_MARKET) {
                    MarketSyncScreen(
                        uiState = uiState,
                        onBuyPlayer = viewModel::buyPlayer,
                        onSignFreeAgent = viewModel::signFreeAgent,
                        onToggleAutoSync = viewModel::toggleAutoSync,
                        onSyncNow = { viewModel.syncLiveSquads(automatic = false) }
                    )
                }
            }
        }
    }
}
