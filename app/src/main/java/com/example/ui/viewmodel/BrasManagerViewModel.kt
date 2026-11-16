package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.CareerSave
import com.example.data.firebase.ManagerProfile
import com.example.data.firebase.TransferRecord
import com.example.data.firebase.TrophyRecord
import com.example.data.local.LocalCareerRepository
import com.example.data.local.SyncLogEntity
import com.example.data.remote.SquadSyncRepository
import com.example.data.seed.LeagueCatalog
import com.example.domain.engine.CupAndCareerEngine
import com.example.domain.engine.FinanceEngine
import com.example.domain.engine.NewsGenerator
import com.example.domain.engine.SeasonEngine
import com.example.domain.model.Club
import com.example.domain.model.CupCompetitionState
import com.example.domain.model.CupMatchTie
import com.example.domain.model.Division
import com.example.domain.model.Fixture
import com.example.domain.model.Formation
import com.example.domain.model.IncomingTransferOffer
import com.example.domain.model.LiveMatchState
import com.example.domain.model.ManagerJobOffer
import com.example.domain.model.MarkingStyle
import com.example.domain.model.MatchEvent
import com.example.domain.model.MatchEventType
import com.example.domain.model.Mentality
import com.example.domain.model.MonthlyFinancePoint
import com.example.domain.model.MonthlyLedgerRecord
import com.example.domain.model.NewsCategory
import com.example.domain.model.NewsItem
import com.example.domain.model.Player
import com.example.domain.model.SeasonAwardSummary
import com.example.domain.model.SeasonSnapshot
import com.example.domain.model.SponsorDeal
import com.example.domain.model.SquadSyncReport
import com.example.domain.model.TicketPriceLevel
import com.example.domain.model.TrainingFocus
import com.example.domain.model.YouthProspect
import com.example.ui.components.formatCurrency
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BrasManagerUiState(
    val isInitializing: Boolean = true,
    val profile: ManagerProfile? = null,
    val savedCareers: List<CareerSave> = emptyList(),
    val activeCareer: CareerSave? = null,
    val clubs: List<Club> = emptyList(),
    val freeAgents: List<Player> = emptyList(),
    val fixtures: List<Fixture> = emptyList(),
    val liveMatch: LiveMatchState? = null,
    val incomingOffers: List<IncomingTransferOffer> = emptyList(),
    val transfers: List<TransferRecord> = emptyList(),
    val trophies: List<TrophyRecord> = emptyList(),
    val newsItems: List<NewsItem> = emptyList(),
    val monthlyLedger: Map<Int, MonthlyLedgerRecord> = emptyMap(),
    val monthlyFinances: List<MonthlyFinancePoint> = emptyList(),
    val seasonTicketRevenue: Long = 0L,
    val cups: List<CupCompetitionState> = emptyList(),
    val activeSponsor: SponsorDeal? = null,
    val youthProspects: List<YouthProspect> = emptyList(),
    val jobOffers: List<ManagerJobOffer> = emptyList(),
    val isSacked: Boolean = false,
    val seasonAwards: SeasonAwardSummary? = null,
    val soundEnabled: Boolean = true,
    val syncLogs: List<SyncLogEntity> = emptyList(),
    val lastSyncReport: SquadSyncReport? = null,
    val isSyncing: Boolean = false,
    val ticketPriceLevel: TicketPriceLevel = TicketPriceLevel.NORMAL,
    val trainingFocus: TrainingFocus = TrainingFocus.EQUILIBRADO,
    val formation: Formation = Formation.F_4_3_3,
    val mentality: Mentality = Mentality.EQUILIBRADO,
    val marking: MarkingStyle = MarkingStyle.ZONAL,
    val seasonFinishedMessage: String? = null,
    val snackMessage: String? = null,
    val errorMessage: String? = null
) {
    val userClub: Club?
        get() = activeCareer?.let { car -> clubs.firstOrNull { it.id == car.clubId } }

    val nextUserFixture: Fixture?
        get() {
            val car = activeCareer ?: return null
            return fixtures.firstOrNull {
                it.round == car.currentRound.toInt() &&
                    (it.homeClubId == car.clubId || it.awayClubId == car.clubId)
            }
        }

    val availableSponsors: List<SponsorDeal>
        get() = userClub?.let { CupAndCareerEngine.availableSponsorsForClub(it) } ?: emptyList()

    val pendingCupTie: CupMatchTie?
        get() {
            val car = activeCareer ?: return null
            val completedRounds = (car.currentRound.toInt() - 1).coerceAtLeast(0)
            return CupAndCareerEngine.findPendingUserCupTie(cups, car.clubId, completedRounds)
        }
}

class BrasManagerViewModel(
    private val careerRepo: LocalCareerRepository,
    private val syncRepo: SquadSyncRepository,
    private val currentUserId: String = LocalCareerRepository.DEFAULT_LOCAL_USER_ID,
    private val initialDisplayName: String = "Técnico Brasileiro"
) : ViewModel() {

    private val _state = MutableStateFlow(BrasManagerUiState())
    val uiState: StateFlow<BrasManagerUiState> = _state.asStateFlow()

    private val activeCareerIdFlow = MutableStateFlow<String?>(null)
    private var simTickerJob: Job? = null

    val catalogClubs: List<Club> by lazy {
        LeagueCatalog.allClubs()
    }

    init {
        bootstrap()
        observeLocalStreams()
    }

    private fun bootstrap() {
        viewModelScope.launch {
            careerRepo.ensureManagerProfile(initialDisplayName.ifBlank { "Técnico Brasileiro" })
                .onFailure { e ->
                    Log.w(TAG, "Erro ao garantir perfil do técnico: ${e.message}")
                }
            val seededClubs = syncRepo.applyCachedUpdates(catalogClubs)
            _state.update {
                it.copy(
                    isInitializing = false,
                    clubs = seededClubs,
                    freeAgents = LeagueCatalog.freeAgents()
                )
            }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun observeLocalStreams() {
        viewModelScope.launch {
            careerRepo.observeManagerProfile(currentUserId)
                .catch { error ->
                    if (error is CancellationException) throw error
                    Log.w(TAG, "Erro ao observar perfil", error)
                }
                .collect { profile ->
                    _state.update { it.copy(profile = profile) }
                }
        }

        viewModelScope.launch {
            careerRepo.observeCareers(currentUserId)
                .catch { error ->
                    if (error is CancellationException) throw error
                    Log.w(TAG, "Erro ao observar carreiras", error)
                    _state.update { it.copy(errorMessage = "Falha ao carregar carreiras: ${error.message}") }
                }
                .collect { careers ->
                    _state.update { state ->
                        val updatedActive = state.activeCareer?.let { cur ->
                            careers.firstOrNull { it.id == cur.id } ?: cur
                        }
                        state.copy(savedCareers = careers, activeCareer = updatedActive)
                    }
                }
        }

        viewModelScope.launch {
            activeCareerIdFlow.flatMapLatest { careerId ->
                if (careerId.isNullOrBlank()) {
                    flowOf(emptyList<TransferRecord>() to emptyList<TrophyRecord>())
                } else {
                    combine(
                        careerRepo.observeTransfers(careerId),
                        careerRepo.observeTrophies(careerId)
                    ) { transfers, trophies -> transfers to trophies }
                }
            }.catch { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "Erro ao observar histórico da carreira", error)
            }.collect { (transfers, trophies) ->
                _state.update { it.copy(transfers = transfers, trophies = trophies) }
            }
        }

        viewModelScope.launch {
            syncRepo.syncLogs.collect { logs ->
                _state.update { it.copy(syncLogs = logs) }
            }
        }
    }

    fun clearSnackMessage() {
        _state.update { it.copy(snackMessage = null, errorMessage = null) }
    }

    fun updateManagerName(newName: String) {
        viewModelScope.launch {
            careerRepo.updateManagerName(newName)
                .onSuccess {
                    _state.update { it.copy(snackMessage = "Nome do técnico atualizado para ${newName.trim().ifBlank { "Técnico Brasileiro" }}!") }
                }
        }
    }

    fun toggleMatchSound() {
        _state.update { it.copy(soundEnabled = !it.soundEnabled) }
    }

    fun startNewCareer(clubId: String) {
        viewModelScope.launch {
            val baseClubs = syncRepo.applyCachedUpdates(catalogClubs)
            val chosen = baseClubs.firstOrNull { it.id == clubId } ?: return@launch
            val initializedClubs = baseClubs.map { club ->
                val autoLineup = SeasonEngine.autoSelectBestLineup(club.players, Formation.F_4_3_3)
                club.copy(players = autoLineup)
            }
            val selectedClub = initializedClubs.first { it.id == clubId }
            val starterIds = selectedClub.players.filter { it.isStarter }.map { it.id }.take(11)

            val newCareer = CareerSave(
                userId = currentUserId,
                clubId = selectedClub.id,
                clubName = selectedClub.name,
                division = selectedClub.division.code,
                seasonYear = 2026L,
                currentRound = 1L,
                budget = selectedClub.budget,
                stadiumCapacity = selectedClub.stadiumCapacity.toLong(),
                ticketPriceLevel = TicketPriceLevel.NORMAL.code,
                trainingFocus = TrainingFocus.EQUILIBRADO.code,
                formation = Formation.F_4_3_3.code,
                mentality = Mentality.EQUILIBRADO.code,
                marking = MarkingStyle.ZONAL.code,
                boardConfidence = 75L,
                points = 0L,
                leaguePosition = 1L,
                copaStage = "3ª Fase",
                activeStarterIds = starterIds,
                status = "active"
            )

            val result = careerRepo.createCareer(newCareer)
            result.onSuccess { careerId ->
                val allFixtures = SeasonEngine.generateAllFixtures(initializedClubs)
                val initialCups = CupAndCareerEngine.buildInitialCups(selectedClub, initializedClubs)
                val initialSponsor = CupAndCareerEngine.availableSponsorsForClub(selectedClub).firstOrNull()
                val initialYouth = CupAndCareerEngine.generateYouthIntake(selectedClub, TrainingFocus.EQUILIBRADO)
                val saved = newCareer.copy(id = careerId)
                val managerName = _state.value.profile?.managerName ?: initialDisplayName.ifBlank { "Técnico" }
                val initialNews = NewsGenerator.buildInitialNews(
                    userClub = selectedClub,
                    allClubs = initializedClubs,
                    seasonYear = saved.seasonYear,
                    managerName = managerName
                )
                val initialTrends = FinanceEngine.buildMonthlyTrends(
                    club = selectedClub,
                    fixtures = allFixtures,
                    currentRound = 1,
                    ticketPriceLevel = TicketPriceLevel.NORMAL,
                    realizedLedger = emptyMap(),
                    activeSponsor = initialSponsor
                )
                activeCareerIdFlow.value = careerId
                _state.update {
                    it.copy(
                        activeCareer = saved,
                        clubs = initializedClubs,
                        freeAgents = LeagueCatalog.freeAgents(),
                        fixtures = allFixtures,
                        liveMatch = null,
                        incomingOffers = emptyList(),
                        newsItems = initialNews,
                        monthlyLedger = emptyMap(),
                        monthlyFinances = initialTrends,
                        seasonTicketRevenue = 0L,
                        cups = initialCups,
                        activeSponsor = initialSponsor,
                        youthProspects = initialYouth,
                        jobOffers = emptyList(),
                        isSacked = false,
                        seasonAwards = null,
                        ticketPriceLevel = TicketPriceLevel.NORMAL,
                        trainingFocus = TrainingFocus.EQUILIBRADO,
                        formation = Formation.F_4_3_3,
                        mentality = Mentality.EQUILIBRADO,
                        marking = MarkingStyle.ZONAL,
                        seasonFinishedMessage = null,
                        snackMessage = "Carreira iniciada no comando do ${selectedClub.name} (${selectedClub.division.shortLabel})!"
                    )
                }
                persistFullSnapshot(careerId)
                if (_state.value.profile?.autoSyncEnabled != false) {
                    syncLiveSquads(automatic = true, priorityDivision = selectedClub.division)
                }
            }.onFailure { e ->
                _state.update { it.copy(errorMessage = "Não foi possível salvar carreira: ${e.message}") }
            }
        }
    }

    fun resumeCareer(career: CareerSave) {
        viewModelScope.launch {
            val snapshot = careerRepo.loadSeasonSnapshot(career.id)
            val formation = Formation.fromCode(career.formation)
            val trainingFocus = TrainingFocus.fromCode(career.trainingFocus)
            val ticketLevel = TicketPriceLevel.fromCode(career.ticketPriceLevel)
            val careerDivision = Division.fromCode(career.division)
            val managerName = _state.value.profile?.managerName ?: initialDisplayName.ifBlank { "Técnico" }

            val restoredClubs: List<Club>
            val restoredFixtures: List<Fixture>
            val restoredLedger: Map<Int, MonthlyLedgerRecord>
            val restoredTicketRev: Long
            val restoredCups: List<CupCompetitionState>
            val restoredSponsor: SponsorDeal?
            val restoredYouth: List<YouthProspect>
            val restoredJobs: List<ManagerJobOffer>
            val restoredSacked: Boolean

            if (snapshot != null && snapshot.clubs.isNotEmpty() && snapshot.fixtures.isNotEmpty()) {
                restoredClubs = snapshot.clubs
                restoredFixtures = snapshot.fixtures
                restoredLedger = snapshot.monthlyLedger
                restoredTicketRev = snapshot.seasonTicketRevenue
                val userClub = restoredClubs.first { it.id == career.clubId }
                restoredCups = snapshot.cups.ifEmpty { CupAndCareerEngine.buildInitialCups(userClub, restoredClubs) }
                restoredSponsor = snapshot.activeSponsor ?: CupAndCareerEngine.availableSponsorsForClub(userClub).firstOrNull()
                restoredYouth = snapshot.youthProspects.ifEmpty { CupAndCareerEngine.generateYouthIntake(userClub, trainingFocus) }
                restoredJobs = snapshot.pendingJobOffers
                restoredSacked = snapshot.isSacked
            } else {
                val baseClubs = syncRepo.applyCachedUpdates(catalogClubs)
                val starterSet = career.activeStarterIds.toSet()
                restoredClubs = baseClubs.map { club ->
                    if (club.id == career.clubId) {
                        val updatedPlayers = if (starterSet.isNotEmpty()) {
                            club.players.map { p -> p.copy(isStarter = p.id in starterSet) }
                        } else {
                            SeasonEngine.autoSelectBestLineup(club.players, formation)
                        }
                        club.copy(
                            division = careerDivision,
                            budget = career.budget,
                            stadiumCapacity = career.stadiumCapacity.toInt(),
                            points = career.points.toInt(),
                            played = (career.wins + career.draws + career.losses).toInt(),
                            wins = career.wins.toInt(),
                            draws = career.draws.toInt(),
                            losses = career.losses.toInt(),
                            goalsFor = career.goalsFor.toInt(),
                            goalsAgainst = career.goalsAgainst.toInt(),
                            players = updatedPlayers
                        )
                    } else {
                        val auto = SeasonEngine.autoSelectBestLineup(club.players, Formation.F_4_3_3)
                        club.copy(players = auto)
                    }
                }
                restoredFixtures = SeasonEngine.generateAllFixtures(restoredClubs)
                val userClub = restoredClubs.first { it.id == career.clubId }
                restoredLedger = FinanceEngine.buildInitialLedgerFromPlayedRounds(
                    club = userClub,
                    fixtures = restoredFixtures,
                    currentRound = career.currentRound.toInt(),
                    ticketPriceLevel = ticketLevel
                )
                restoredTicketRev = restoredLedger.values.sumOf { it.ticketIncome }
                restoredCups = CupAndCareerEngine.buildInitialCups(userClub, restoredClubs)
                restoredSponsor = CupAndCareerEngine.availableSponsorsForClub(userClub).firstOrNull()
                restoredYouth = CupAndCareerEngine.generateYouthIntake(userClub, trainingFocus)
                restoredJobs = emptyList()
                restoredSacked = false
            }

            val restoredUserClub = restoredClubs.first { it.id == career.clubId }
            val initialNews = NewsGenerator.buildInitialNews(
                userClub = restoredUserClub,
                allClubs = restoredClubs,
                seasonYear = career.seasonYear,
                managerName = managerName
            )
            val initialTrends = FinanceEngine.buildMonthlyTrends(
                club = restoredUserClub,
                fixtures = restoredFixtures,
                currentRound = career.currentRound.toInt(),
                ticketPriceLevel = ticketLevel,
                realizedLedger = restoredLedger,
                activeSponsor = restoredSponsor
            )
            val awards = if (career.currentRound > restoredUserClub.division.totalRounds.toLong()) {
                CupAndCareerEngine.computeSeasonAwards(restoredClubs, restoredUserClub, career.seasonYear, managerName)
            } else null

            activeCareerIdFlow.value = career.id
            _state.update {
                it.copy(
                    activeCareer = career,
                    clubs = restoredClubs,
                    freeAgents = LeagueCatalog.freeAgents(),
                    fixtures = restoredFixtures,
                    liveMatch = null,
                    newsItems = initialNews,
                    monthlyLedger = restoredLedger,
                    monthlyFinances = initialTrends,
                    seasonTicketRevenue = restoredTicketRev,
                    cups = restoredCups,
                    activeSponsor = restoredSponsor,
                    youthProspects = restoredYouth,
                    jobOffers = restoredJobs,
                    isSacked = restoredSacked,
                    seasonAwards = awards,
                    ticketPriceLevel = ticketLevel,
                    trainingFocus = trainingFocus,
                    formation = formation,
                    mentality = Mentality.fromCode(career.mentality),
                    marking = MarkingStyle.fromCode(career.marking),
                    seasonFinishedMessage = null,
                    snackMessage = "Bem-vindo de volta ao ${career.clubName}!"
                )
            }

            if (snapshot == null) {
                persistFullSnapshot(career.id)
            }

            val lastSync = _state.value.profile?.lastSquadSyncAt ?: 0L
            val sixHoursMs = 6L * 60L * 60L * 1000L
            if (_state.value.profile?.autoSyncEnabled != false && System.currentTimeMillis() - lastSync > sixHoursMs) {
                syncLiveSquads(automatic = true, priorityDivision = careerDivision)
            }
        }
    }

    fun exitToCareerHub() {
        val currentId = _state.value.activeCareer?.id
        if (!currentId.isNullOrBlank()) {
            persistFullSnapshot(currentId)
        }
        simTickerJob?.cancel()
        activeCareerIdFlow.value = null
        _state.update {
            it.copy(
                activeCareer = null,
                liveMatch = null,
                newsItems = emptyList(),
                monthlyLedger = emptyMap(),
                monthlyFinances = emptyList(),
                seasonTicketRevenue = 0L,
                cups = emptyList(),
                activeSponsor = null,
                youthProspects = emptyList(),
                jobOffers = emptyList(),
                isSacked = false,
                seasonAwards = null
            )
        }
    }

    fun deleteCareer(careerId: String) {
        viewModelScope.launch {
            careerRepo.deleteCareer(careerId)
                .onSuccess {
                    if (_state.value.activeCareer?.id == careerId) {
                        exitToCareerHub()
                    }
                    _state.update { it.copy(snackMessage = "Carreira removida.") }
                }
                .onFailure { e ->
                    _state.update { it.copy(errorMessage = "Erro ao excluir carreira: ${e.message}") }
                }
        }
    }

    fun toggleAutoSync(enabled: Boolean) {
        viewModelScope.launch {
            val profile = _state.value.profile ?: return@launch
            careerRepo.updateManagerSettings(
                managerName = profile.managerName,
                autoSyncEnabled = enabled,
                preferredSimSpeed = profile.preferredSimSpeed
            )
            _state.update {
                it.copy(
                    snackMessage = if (enabled) {
                        "Atualização automática dos elencos na temporada ativada!"
                    } else {
                        "Atualização automática pausada."
                    }
                )
            }
        }
    }

    fun syncLiveSquads(automatic: Boolean = false, priorityDivision: Division? = null) {
        if (_state.value.isSyncing) return
        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true) }
            val currentClubs = _state.value.clubs.ifEmpty { catalogClubs }
            val (updatedClubs, report) = syncRepo.syncLiveRosters(
                currentClubs = currentClubs,
                preserveUserSignings = true,
                priorityDivision = priorityDivision ?: _state.value.userClub?.division
            )

            // Update profile last sync timestamp
            _state.value.profile?.let { profile ->
                careerRepo.updateManagerSettings(
                    managerName = profile.managerName,
                    autoSyncEnabled = profile.autoSyncEnabled,
                    preferredSimSpeed = profile.preferredSimSpeed,
                    lastSquadSyncAt = report.timestamp
                )
            }

            // Record any new live signings for the user's active club in Firestore transfer log
            val career = _state.value.activeCareer
            if (career != null && report.isLiveNetwork) {
                val userChanges = report.entries.filter {
                    it.clubName == career.clubName && it.action == "NOVO"
                }.take(3)
                userChanges.forEach { entry ->
                    careerRepo.addTransfer(
                        TransferRecord(
                            userId = currentUserId,
                            careerId = career.id,
                            playerName = entry.playerName,
                            position = entry.position,
                            fromClubName = "Mercado Real",
                            toClubName = career.clubName,
                            fee = 0L,
                            transferType = "AUTO_SYNC",
                            seasonYear = career.seasonYear,
                            roundNumber = career.currentRound
                        )
                    )
                }
            }

            _state.update {
                val syncNews = if (career != null && (!automatic || report.isLiveNetwork)) {
                    listOf(
                        NewsGenerator.buildSyncNews(
                            round = career.currentRound.toInt(),
                            report = report,
                            userClub = updatedClubs.firstOrNull { c -> c.id == career.clubId }
                        )
                    )
                } else {
                    emptyList()
                }
                it.copy(
                    clubs = updatedClubs,
                    lastSyncReport = report,
                    newsItems = (syncNews + it.newsItems).take(60),
                    isSyncing = false,
                    snackMessage = if (automatic && !report.isLiveNetwork) null else report.summary
                )
            }
        }
    }

    fun updateTactics(formation: Formation, mentality: Mentality, marking: MarkingStyle) {
        val career = _state.value.activeCareer ?: return
        val userClub = _state.value.userClub ?: return
        val updatedPlayers = SeasonEngine.autoSelectBestLineup(userClub.players, formation)
        val updatedClub = userClub.copy(players = updatedPlayers)
        val starterIds = updatedPlayers.filter { it.isStarter }.map { it.id }.take(11)

        val updatedCareer = career.copy(
            formation = formation.code,
            mentality = mentality.code,
            marking = marking.code,
            activeStarterIds = starterIds
        )
        _state.update { state ->
            state.copy(
                formation = formation,
                mentality = mentality,
                marking = marking,
                activeCareer = updatedCareer,
                clubs = state.clubs.map { if (it.id == updatedClub.id) updatedClub else it }
            )
        }
        persistCareerUpdate(updatedCareer)
    }

    fun autoSelectBestLineup() {
        val career = _state.value.activeCareer ?: return
        val userClub = _state.value.userClub ?: return
        val updatedPlayers = SeasonEngine.autoSelectBestLineup(userClub.players, _state.value.formation)
        val starterIds = updatedPlayers.filter { it.isStarter }.map { it.id }.take(11)
        val updatedCareer = career.copy(activeStarterIds = starterIds)

        _state.update { state ->
            state.copy(
                activeCareer = updatedCareer,
                clubs = state.clubs.map { if (it.id == userClub.id) userClub.copy(players = updatedPlayers) else it },
                snackMessage = "Melhor escalação titular selecionada!"
            )
        }
        persistCareerUpdate(updatedCareer)
    }

    fun toggleStarter(playerId: String) {
        val career = _state.value.activeCareer ?: return
        val userClub = _state.value.userClub ?: return
        val target = userClub.players.firstOrNull { it.id == playerId } ?: return

        if (!target.isStarter && !target.isAvailable) {
            _state.update { it.copy(snackMessage = "${target.name} está suspenso/lesionado nesta rodada.") }
            return
        }

        val currentStartersCount = userClub.players.count { it.isStarter }
        if (!target.isStarter && currentStartersCount >= 11) {
            _state.update { it.copy(snackMessage = "Já existem 11 titulares. Retire um titular antes de escalar ${target.name}.") }
            return
        }

        val updatedPlayers = userClub.players.map {
            if (it.id == playerId) it.copy(isStarter = !it.isStarter) else it
        }
        val starterIds = updatedPlayers.filter { it.isStarter }.map { it.id }.take(11)
        val updatedCareer = career.copy(activeStarterIds = starterIds)

        _state.update { state ->
            state.copy(
                activeCareer = updatedCareer,
                clubs = state.clubs.map { if (it.id == userClub.id) userClub.copy(players = updatedPlayers) else it }
            )
        }
        persistCareerUpdate(updatedCareer)
    }

    fun togglePlayerForSale(playerId: String) {
        val userClub = _state.value.userClub ?: return
        val updatedPlayers = userClub.players.map {
            if (it.id == playerId) it.copy(forSale = !it.forSale) else it
        }
        _state.update { state ->
            state.copy(
                clubs = state.clubs.map { if (it.id == userClub.id) userClub.copy(players = updatedPlayers) else it }
            )
        }
    }

    fun setTicketPriceLevel(level: TicketPriceLevel) {
        val career = _state.value.activeCareer ?: return
        val updated = career.copy(ticketPriceLevel = level.code)
        _state.update { state ->
            val userClub = state.clubs.firstOrNull { it.id == career.clubId }
            val updatedTrends = if (userClub != null) {
                FinanceEngine.buildMonthlyTrends(
                    club = userClub,
                    fixtures = state.fixtures,
                    currentRound = updated.currentRound.toInt(),
                    ticketPriceLevel = level,
                    realizedLedger = state.monthlyLedger
                )
            } else state.monthlyFinances
            state.copy(
                ticketPriceLevel = level,
                activeCareer = updated,
                monthlyFinances = updatedTrends
            )
        }
        persistCareerUpdate(updated)
    }

    fun setTrainingFocus(focus: TrainingFocus) {
        val career = _state.value.activeCareer ?: return
        val updated = career.copy(trainingFocus = focus.code)
        _state.update { it.copy(trainingFocus = focus, activeCareer = updated) }
        persistCareerUpdate(updated)
    }

    fun expandStadium() {
        val career = _state.value.activeCareer ?: return
        val userClub = _state.value.userClub ?: return
        val cost = 12_000_000L
        if (userClub.budget < cost) {
            _state.update { it.copy(snackMessage = "Saldo insuficiente para ampliar o estádio (custo R$ 12,0M).") }
            return
        }
        val newCap = (userClub.stadiumCapacity + 5_000).coerceAtMost(120_000)
        val updatedClub = userClub.copy(budget = userClub.budget - cost, stadiumCapacity = newCap)
        val updatedCareer = career.copy(budget = updatedClub.budget, stadiumCapacity = newCap.toLong())
        _state.update { state ->
            val monthIdx = FinanceEngine.monthIndexForRound(career.currentRound.toInt(), updatedClub.division.totalRounds)
            val prevEntry = state.monthlyLedger[monthIdx] ?: MonthlyLedgerRecord(monthIndex = monthIdx)
            val newLedger = state.monthlyLedger + (
                monthIdx to prevEntry.copy(
                    otherDelta = prevEntry.otherDelta - cost,
                    endingBudget = updatedClub.budget
                )
            )
            val updatedTrends = FinanceEngine.buildMonthlyTrends(
                club = updatedClub,
                fixtures = state.fixtures,
                currentRound = updatedCareer.currentRound.toInt(),
                ticketPriceLevel = state.ticketPriceLevel,
                realizedLedger = newLedger
            )
            state.copy(
                activeCareer = updatedCareer,
                clubs = state.clubs.map { if (it.id == userClub.id) updatedClub else it },
                monthlyLedger = newLedger,
                monthlyFinances = updatedTrends,
                snackMessage = "Estádio ampliado para $newCap lugares!"
            )
        }
        persistCareerUpdate(updatedCareer)
    }

    fun takeBankLoan() {
        val career = _state.value.activeCareer ?: return
        val userClub = _state.value.userClub ?: return
        val loanAmount = 10_000_000L
        val updatedClub = userClub.copy(budget = userClub.budget + loanAmount)
        val updatedCareer = career.copy(
            budget = updatedClub.budget,
            boardConfidence = (career.boardConfidence - 5L).coerceAtLeast(15L)
        )
        _state.update { state ->
            val monthIdx = FinanceEngine.monthIndexForRound(career.currentRound.toInt(), updatedClub.division.totalRounds)
            val prevEntry = state.monthlyLedger[monthIdx] ?: MonthlyLedgerRecord(monthIndex = monthIdx)
            val newLedger = state.monthlyLedger + (
                monthIdx to prevEntry.copy(
                    otherDelta = prevEntry.otherDelta + loanAmount,
                    endingBudget = updatedClub.budget
                )
            )
            val updatedTrends = FinanceEngine.buildMonthlyTrends(
                club = updatedClub,
                fixtures = state.fixtures,
                currentRound = updatedCareer.currentRound.toInt(),
                ticketPriceLevel = state.ticketPriceLevel,
                realizedLedger = newLedger,
                activeSponsor = state.activeSponsor
            )
            state.copy(
                activeCareer = updatedCareer,
                clubs = state.clubs.map { if (it.id == userClub.id) updatedClub else it },
                monthlyLedger = newLedger,
                monthlyFinances = updatedTrends,
                snackMessage = "Empréstimo de R$ 10,0M creditado no caixa do clube."
            )
        }
        persistCareerUpdate(updatedCareer)
    }

    fun selectSponsor(deal: SponsorDeal) {
        val career = _state.value.activeCareer ?: return
        val userClub = _state.value.userClub ?: return
        if (userClub.prestige < deal.minPrestige) {
            _state.update { it.copy(snackMessage = "${deal.name} exige clube com ${deal.minPrestige}+ estrelas de prestígio.") }
            return
        }
        _state.update { state ->
            val updatedTrends = FinanceEngine.buildMonthlyTrends(
                club = userClub,
                fixtures = state.fixtures,
                currentRound = career.currentRound.toInt(),
                ticketPriceLevel = state.ticketPriceLevel,
                realizedLedger = state.monthlyLedger,
                activeSponsor = deal
            )
            state.copy(
                activeSponsor = deal,
                monthlyFinances = updatedTrends,
                snackMessage = "Patrocínio fechado com ${deal.name}! (+${formatCurrency(deal.perRoundBase)}/rodada)"
            )
        }
        persistFullSnapshot(career.id)
    }

    fun promoteYouthProspect(prospectId: String) {
        val career = _state.value.activeCareer ?: return
        val userClub = _state.value.userClub ?: return
        val prospect = _state.value.youthProspects.firstOrNull { it.id == prospectId } ?: return
        val usedNumbers = userClub.players.map { it.shirtNumber }.toSet()
        val promotedPlayer = CupAndCareerEngine.promoteProspectToPlayer(prospect, usedNumbers)
        val updatedClub = userClub.copy(players = userClub.players + promotedPlayer)

        val news = NewsItem(
            id = "news_youth_${prospect.id}",
            category = NewsCategory.CLUB_BUZZ,
            round = career.currentRound.toInt(),
            headline = "Joia da base: ${userClub.name} promove ${promotedPlayer.name}",
            body = "Atleta de ${promotedPlayer.age} anos (${promotedPlayer.position.code}, OVR ${promotedPlayer.overall}, potencial ${promotedPlayer.potential}) sobe para o elenco principal.",
            clubId = updatedClub.id,
            clubName = updatedClub.name,
            clubShortName = updatedClub.shortName,
            clubColorHex = updatedClub.primaryColorHex,
            clubSecondaryColorHex = updatedClub.secondaryColorHex,
            isBreaking = false,
            tagLabel = "BASE SUB-20"
        )

        _state.update { state ->
            state.copy(
                clubs = state.clubs.map { if (it.id == updatedClub.id) updatedClub else it },
                youthProspects = state.youthProspects.filterNot { it.id == prospectId },
                newsItems = (listOf(news) + state.newsItems).take(60),
                snackMessage = "${promotedPlayer.name} (${promotedPlayer.position.code}) promovido ao time profissional!"
            )
        }
        persistFullSnapshot(career.id)
    }

    fun renewPlayerContract(playerId: String) {
        val career = _state.value.activeCareer ?: return
        val userClub = _state.value.userClub ?: return
        val player = userClub.players.firstOrNull { it.id == playerId } ?: return
        val (bonus, newSalary) = CupAndCareerEngine.calculateRenewalCostAndSalary(player)
        if (userClub.budget < bonus) {
            _state.update { it.copy(snackMessage = "Caixa insuficiente para luvas de renovação (${formatCurrency(bonus)}).") }
            return
        }
        val renewed = CupAndCareerEngine.renewPlayerContract(player)
        val updatedClub = userClub.copy(
            budget = userClub.budget - bonus,
            players = userClub.players.map { if (it.id == playerId) renewed else it }
        )
        val updatedCareer = career.copy(budget = updatedClub.budget)
        _state.update { state ->
            val monthIdx = FinanceEngine.monthIndexForRound(career.currentRound.toInt(), updatedClub.division.totalRounds)
            val prevEntry = state.monthlyLedger[monthIdx] ?: MonthlyLedgerRecord(monthIndex = monthIdx)
            val newLedger = state.monthlyLedger + (
                monthIdx to prevEntry.copy(
                    otherDelta = prevEntry.otherDelta - bonus,
                    endingBudget = updatedClub.budget
                )
            )
            state.copy(
                activeCareer = updatedCareer,
                clubs = state.clubs.map { if (it.id == updatedClub.id) updatedClub else it },
                monthlyLedger = newLedger,
                snackMessage = "Contrato de ${player.name} renovado (+38 rodadas, salário ${formatCurrency(newSalary)})!"
            )
        }
        persistCareerUpdate(updatedCareer)
    }

    fun acceptJobOffer(offer: ManagerJobOffer) {
        val career = _state.value.activeCareer ?: return
        val targetClub = _state.value.clubs.firstOrNull { it.id == offer.clubId } ?: return
        val initializedClub = targetClub.copy(
            players = SeasonEngine.autoSelectBestLineup(targetClub.players, Formation.F_4_3_3)
        )
        val starterIds = initializedClub.players.filter { it.isStarter }.map { it.id }.take(11)
        val updatedClubs = _state.value.clubs.map { if (it.id == initializedClub.id) initializedClub else it }
        val newCups = CupAndCareerEngine.buildInitialCups(initializedClub, updatedClubs)
        val newSponsor = CupAndCareerEngine.availableSponsorsForClub(initializedClub).firstOrNull()
        val newYouth = CupAndCareerEngine.generateYouthIntake(initializedClub, _state.value.trainingFocus)

        val updatedCareer = career.copy(
            clubId = initializedClub.id,
            clubName = initializedClub.name,
            division = initializedClub.division.code,
            budget = initializedClub.budget,
            stadiumCapacity = initializedClub.stadiumCapacity.toLong(),
            boardConfidence = 75L,
            points = initializedClub.points.toLong(),
            wins = initializedClub.wins.toLong(),
            draws = initializedClub.draws.toLong(),
            losses = initializedClub.losses.toLong(),
            goalsFor = initializedClub.goalsFor.toLong(),
            goalsAgainst = initializedClub.goalsAgainst.toLong(),
            activeStarterIds = starterIds,
            copaStage = "Oitavas"
        )
        val managerName = _state.value.profile?.managerName ?: initialDisplayName
        val welcomeNews = NewsGenerator.buildInitialNews(initializedClub, updatedClubs, career.seasonYear, managerName).first()
        val newTrends = FinanceEngine.buildMonthlyTrends(
            club = initializedClub,
            fixtures = _state.value.fixtures,
            currentRound = updatedCareer.currentRound.toInt(),
            ticketPriceLevel = _state.value.ticketPriceLevel,
            realizedLedger = emptyMap(),
            activeSponsor = newSponsor
        )

        _state.update { state ->
            state.copy(
                activeCareer = updatedCareer,
                clubs = updatedClubs,
                cups = newCups,
                activeSponsor = newSponsor,
                youthProspects = newYouth,
                jobOffers = emptyList(),
                isSacked = false,
                monthlyLedger = emptyMap(),
                monthlyFinances = newTrends,
                seasonTicketRevenue = 0L,
                newsItems = (listOf(welcomeNews) + state.newsItems).take(60),
                snackMessage = "Novo desafio! Você assumiu o comando do ${initializedClub.name}!"
            )
        }
        persistCareerUpdate(updatedCareer)
    }

    fun takeEmergencyRehire() {
        val currentId = _state.value.activeCareer?.clubId
        val fallbackClubs = _state.value.clubs.filter { it.id != currentId }.sortedBy { it.prestige }
        val offerClub = fallbackClubs.firstOrNull() ?: return
        acceptJobOffer(
            ManagerJobOffer(
                id = "job_rehire_${offerClub.id}",
                clubId = offerClub.id,
                clubName = offerClub.name,
                division = offerClub.division,
                budget = offerClub.budget,
                avgOverall = offerClub.averageOverall,
                prestige = offerClub.prestige,
                reason = "Oportunidade de recomeço após saída da diretoria"
            )
        )
    }

    fun buyPlayer(player: Player, sellerClub: Club, offerAmount: Long) {
        val career = _state.value.activeCareer ?: return
        val userClub = _state.value.userClub ?: return
        if (userClub.budget < offerAmount) {
            _state.update { it.copy(snackMessage = "Saldo insuficiente para essa proposta!") }
            return
        }
        val minAcceptable = (player.marketValue * (0.95 + sellerClub.prestige * 0.04)).toLong()
        if (offerAmount < minAcceptable) {
            _state.update {
                it.copy(snackMessage = "${sellerClub.name} recusou a proposta por ${player.name}. Valor mínimo ~${com.example.ui.components.formatCurrency(minAcceptable)}.")
            }
            return
        }

        val boughtPlayer = player.copy(isStarter = false, forSale = false, energy = 100)
        val updatedUser = userClub.copy(
            budget = userClub.budget - offerAmount,
            players = userClub.players + boughtPlayer
        )
        val updatedSeller = sellerClub.copy(
            budget = sellerClub.budget + offerAmount,
            players = sellerClub.players.filterNot { it.id == player.id }
        )
        val updatedCareer = career.copy(budget = updatedUser.budget)

        val transferNews = NewsGenerator.buildTransferNews(
            round = career.currentRound.toInt(),
            headline = "Reforço fechado: ${userClub.name} contrata ${player.name}",
            body = "${player.name} (${player.position.code}, OVR ${player.overall}) deixa o ${sellerClub.name} e assina com o ${userClub.name} por ${formatCurrency(offerAmount)}.",
            club = updatedUser,
            feeLabel = formatCurrency(offerAmount)
        )

        _state.update { state ->
            val monthIdx = FinanceEngine.monthIndexForRound(career.currentRound.toInt(), updatedUser.division.totalRounds)
            val prevEntry = state.monthlyLedger[monthIdx] ?: MonthlyLedgerRecord(monthIndex = monthIdx)
            val newLedger = state.monthlyLedger + (
                monthIdx to prevEntry.copy(
                    otherDelta = prevEntry.otherDelta - offerAmount,
                    endingBudget = updatedUser.budget
                )
            )
            val updatedTrends = FinanceEngine.buildMonthlyTrends(
                club = updatedUser,
                fixtures = state.fixtures,
                currentRound = updatedCareer.currentRound.toInt(),
                ticketPriceLevel = state.ticketPriceLevel,
                realizedLedger = newLedger
            )
            state.copy(
                activeCareer = updatedCareer,
                clubs = state.clubs.map {
                    when (it.id) {
                        updatedUser.id -> updatedUser
                        updatedSeller.id -> updatedSeller
                        else -> it
                    }
                },
                newsItems = (listOf(transferNews) + state.newsItems).take(60),
                monthlyLedger = newLedger,
                monthlyFinances = updatedTrends,
                snackMessage = "CONTRATADO! ${player.name} agora veste a camisa do ${userClub.name}!"
            )
        }
        persistCareerUpdate(updatedCareer)
        viewModelScope.launch {
            careerRepo.addTransfer(
                TransferRecord(
                    userId = currentUserId,
                    careerId = career.id,
                    playerName = player.name,
                    position = player.position.code,
                    fromClubName = sellerClub.name,
                    toClubName = userClub.name,
                    fee = offerAmount,
                    transferType = "BUY",
                    seasonYear = career.seasonYear,
                    roundNumber = career.currentRound
                )
            )
        }
    }

    fun signFreeAgent(player: Player) {
        val career = _state.value.activeCareer ?: return
        val userClub = _state.value.userClub ?: return
        val luvas = (player.marketValue * 0.20).toLong().coerceAtLeast(250_000L)
        if (userClub.budget < luvas) {
            _state.update { it.copy(snackMessage = "Caixa insuficiente para pagar luvas de ${com.example.ui.components.formatCurrency(luvas)}.") }
            return
        }

        val signed = player.copy(isStarter = false, energy = 100)
        val updatedUser = userClub.copy(
            budget = userClub.budget - luvas,
            players = userClub.players + signed
        )
        val updatedCareer = career.copy(budget = updatedUser.budget)

        val freeAgentNews = NewsGenerator.buildTransferNews(
            round = career.currentRound.toInt(),
            headline = "Oportunidade de mercado: ${userClub.name} anuncia ${player.name}",
            body = "Livre no mercado, ${player.name} (${player.position.code}, OVR ${player.overall}) assina sem custos de transferência (luvas de ${formatCurrency(luvas)}).",
            club = updatedUser,
            feeLabel = "SEM CUSTO"
        )

        _state.update { state ->
            val monthIdx = FinanceEngine.monthIndexForRound(career.currentRound.toInt(), updatedUser.division.totalRounds)
            val prevEntry = state.monthlyLedger[monthIdx] ?: MonthlyLedgerRecord(monthIndex = monthIdx)
            val newLedger = state.monthlyLedger + (
                monthIdx to prevEntry.copy(
                    otherDelta = prevEntry.otherDelta - luvas,
                    endingBudget = updatedUser.budget
                )
            )
            val updatedTrends = FinanceEngine.buildMonthlyTrends(
                club = updatedUser,
                fixtures = state.fixtures,
                currentRound = updatedCareer.currentRound.toInt(),
                ticketPriceLevel = state.ticketPriceLevel,
                realizedLedger = newLedger
            )
            state.copy(
                activeCareer = updatedCareer,
                clubs = state.clubs.map { if (it.id == updatedUser.id) updatedUser else it },
                freeAgents = state.freeAgents.filterNot { it.id == player.id },
                newsItems = (listOf(freeAgentNews) + state.newsItems).take(60),
                monthlyLedger = newLedger,
                monthlyFinances = updatedTrends,
                snackMessage = "${player.name} assinou contrato sem custos de transferência!"
            )
        }
        persistCareerUpdate(updatedCareer)
        viewModelScope.launch {
            careerRepo.addTransfer(
                TransferRecord(
                    userId = currentUserId,
                    careerId = career.id,
                    playerName = player.name,
                    position = player.position.code,
                    fromClubName = "Sem Clube",
                    toClubName = userClub.name,
                    fee = luvas,
                    transferType = "FREE",
                    seasonYear = career.seasonYear,
                    roundNumber = career.currentRound
                )
            )
        }
    }

    fun acceptIncomingOffer(offer: IncomingTransferOffer) {
        val career = _state.value.activeCareer ?: return
        val userClub = _state.value.userClub ?: return
        if (userClub.players.size <= 13) {
            _state.update { it.copy(snackMessage = "Elenco mínimo de 13 jogadores atingido. Não é possível vender agora.") }
            return
        }
        val buyerClub = _state.value.clubs.firstOrNull { it.id == offer.offeringClubId }
        val updatedUser = userClub.copy(
            budget = userClub.budget + offer.offerAmount,
            players = userClub.players.filterNot { it.id == offer.player.id }
        )
        val updatedCareer = career.copy(
            budget = updatedUser.budget,
            activeStarterIds = updatedUser.players.filter { it.isStarter }.map { it.id }.take(11)
        )
        val saleNews = NewsGenerator.buildTransferNews(
            round = career.currentRound.toInt(),
            headline = "Venda confirmada: ${offer.player.name} vai para o ${offer.offeringClubName}",
            body = "O ${userClub.name} aceitou a proposta de ${formatCurrency(offer.offerAmount)} pelo atleta ${offer.player.name} (${offer.player.position.code}).",
            club = buyerClub ?: updatedUser,
            feeLabel = "+${formatCurrency(offer.offerAmount)}"
        )

        _state.update { state ->
            val monthIdx = FinanceEngine.monthIndexForRound(career.currentRound.toInt(), updatedUser.division.totalRounds)
            val prevEntry = state.monthlyLedger[monthIdx] ?: MonthlyLedgerRecord(monthIndex = monthIdx)
            val newLedger = state.monthlyLedger + (
                monthIdx to prevEntry.copy(
                    otherDelta = prevEntry.otherDelta + offer.offerAmount,
                    endingBudget = updatedUser.budget
                )
            )
            val updatedTrends = FinanceEngine.buildMonthlyTrends(
                club = updatedUser,
                fixtures = state.fixtures,
                currentRound = updatedCareer.currentRound.toInt(),
                ticketPriceLevel = state.ticketPriceLevel,
                realizedLedger = newLedger
            )
            state.copy(
                activeCareer = updatedCareer,
                incomingOffers = state.incomingOffers.filterNot { it.id == offer.id },
                clubs = state.clubs.map {
                    when (it.id) {
                        updatedUser.id -> updatedUser
                        buyerClub?.id -> it.copy(players = it.players + offer.player.copy(isStarter = false))
                        else -> it
                    }
                },
                newsItems = (listOf(saleNews) + state.newsItems).take(60),
                monthlyLedger = newLedger,
                monthlyFinances = updatedTrends,
                snackMessage = "Vendido! ${offer.player.name} transferido para ${offer.offeringClubName}."
            )
        }
        persistCareerUpdate(updatedCareer)
        viewModelScope.launch {
            careerRepo.addTransfer(
                TransferRecord(
                    userId = currentUserId,
                    careerId = career.id,
                    playerName = offer.player.name,
                    position = offer.player.position.code,
                    fromClubName = userClub.name,
                    toClubName = offer.offeringClubName,
                    fee = offer.offerAmount,
                    transferType = "SELL",
                    seasonYear = career.seasonYear,
                    roundNumber = career.currentRound
                )
            )
        }
    }

    fun rejectIncomingOffer(offer: IncomingTransferOffer) {
        _state.update { state ->
            state.copy(incomingOffers = state.incomingOffers.filterNot { it.id == offer.id })
        }
    }

    // --- Live Match Simulation (Brasfoot Engine) ---
    fun startLiveMatch() {
        val career = _state.value.activeCareer ?: return
        val userClub = _state.value.userClub ?: return
        if (_state.value.isSacked) return
        val round = career.currentRound.toInt()
        if (round > userClub.division.totalRounds) return

        val validStarters = userClub.players.filter { it.isStarter && it.isAvailable }
        val preparedClub = if (validStarters.size < 11) {
            val auto = SeasonEngine.autoSelectBestLineup(userClub.players, _state.value.formation)
            userClub.copy(players = auto)
        } else userClub

        val updatedClubs = _state.value.clubs.map { if (it.id == preparedClub.id) preparedClub else it }
        val roundFixtures = _state.value.fixtures.filter {
            it.round == round && it.division == preparedClub.division
        }

        val initialLive = SeasonEngine.prepareLiveMatch(
            round = round,
            userClub = preparedClub,
            allClubs = updatedClubs,
            roundFixtures = roundFixtures,
            ticketPriceLevel = _state.value.ticketPriceLevel
        )

        _state.update {
            it.copy(
                clubs = updatedClubs,
                liveMatch = initialLive.copy(
                    speed = (_state.value.profile?.preferredSimSpeed ?: 2L).toInt()
                )
            )
        }
        launchTicker()
    }

    fun startCupMatch(tie: CupMatchTie) {
        val userClub = _state.value.userClub ?: return
        if (_state.value.isSacked) return
        val cup = _state.value.cups.firstOrNull { it.cupType == tie.cupType } ?: return

        val validStarters = userClub.players.filter { it.isStarter && it.isAvailable }
        val preparedClub = if (validStarters.size < 11) {
            val auto = SeasonEngine.autoSelectBestLineup(userClub.players, _state.value.formation)
            userClub.copy(players = auto)
        } else userClub
        val updatedClubs = _state.value.clubs.map { if (it.id == preparedClub.id) preparedClub else it }

        val cupLive = CupAndCareerEngine.prepareCupLiveMatch(
            tie = tie,
            userClub = preparedClub,
            allClubs = updatedClubs,
            cup = cup,
            ticketPriceLevel = _state.value.ticketPriceLevel
        )

        _state.update {
            it.copy(
                clubs = updatedClubs,
                liveMatch = cupLive.copy(
                    speed = (_state.value.profile?.preferredSimSpeed ?: 2L).toInt()
                )
            )
        }
        launchTicker()
    }

    private fun launchTicker() {
        simTickerJob?.cancel()
        simTickerJob = viewModelScope.launch {
            while (true) {
                val live = _state.value.liveMatch ?: break
                if (live.isFinished) break
                val delayMs = when (live.speed) {
                    4 -> 220L
                    2 -> 480L
                    else -> 850L
                }
                delay(delayMs)
                val current = _state.value.liveMatch ?: break
                if (!current.isPaused && !current.isFinished) {
                    val userClub = _state.value.userClub ?: break
                    val next = SeasonEngine.simulateTick(
                        state = current,
                        userClub = userClub,
                        allClubs = _state.value.clubs,
                        formation = _state.value.formation,
                        mentality = _state.value.mentality,
                        marking = _state.value.marking
                    )
                    _state.update { it.copy(liveMatch = next) }
                }
            }
        }
    }

    fun toggleMatchPause() {
        _state.update { state ->
            val live = state.liveMatch ?: return@update state
            if (live.isFinished) return@update state
            state.copy(liveMatch = live.copy(isPaused = !live.isPaused))
        }
    }

    fun setMatchSpeed(speed: Int) {
        _state.update { state ->
            val live = state.liveMatch ?: return@update state
            state.copy(liveMatch = live.copy(speed = speed))
        }
    }

    fun setLiveMentality(mentality: Mentality) {
        _state.update { it.copy(mentality = mentality) }
    }

    fun makeLiveSubstitution(outPlayerId: String, inPlayerId: String) {
        val live = _state.value.liveMatch ?: return
        val userClub = _state.value.userClub ?: return
        if (live.substitutionsUsed >= live.maxSubstitutions) {
            _state.update { it.copy(snackMessage = "Limite de 5 substituições atingido.") }
            return
        }
        val outP = userClub.players.firstOrNull { it.id == outPlayerId } ?: return
        val inP = userClub.players.firstOrNull { it.id == inPlayerId } ?: return
        if (!inP.isAvailable) return

        val updatedPlayers = userClub.players.map { p ->
            when (p.id) {
                outP.id -> p.copy(isStarter = false)
                inP.id -> p.copy(isStarter = true)
                else -> p
            }
        }
        val subEvent = MatchEvent(
            minute = live.minute,
            type = MatchEventType.SUBSTITUTION,
            clubId = userClub.id,
            playerName = inP.name,
            assistName = outP.name,
            description = "Substituição no ${userClub.shortName}: Entra ${inP.name}, sai ${outP.name}."
        )
        _state.update { state ->
            state.copy(
                clubs = state.clubs.map { if (it.id == userClub.id) userClub.copy(players = updatedPlayers) else it },
                liveMatch = live.copy(
                    substitutionsUsed = live.substitutionsUsed + 1,
                    events = (listOf(subEvent) + live.events).take(60)
                )
            )
        }
    }

    fun skipToFullTime() {
        simTickerJob?.cancel()
        var current = _state.value.liveMatch ?: return
        val userClub = _state.value.userClub ?: return
        while (!current.isFinished) {
            current = current.copy(isPaused = false)
            current = SeasonEngine.simulateTick(
                state = current,
                userClub = userClub,
                allClubs = _state.value.clubs,
                formation = _state.value.formation,
                mentality = _state.value.mentality,
                marking = _state.value.marking
            )
        }
        _state.update { it.copy(liveMatch = current) }
    }

    fun finishMatchAndAdvanceRound() {
        simTickerJob?.cancel()
        val live = _state.value.liveMatch ?: return
        if (live.isKnockout) {
            finishCupMatch(live)
            return
        }
        val career = _state.value.activeCareer ?: return
        val userClub = _state.value.userClub ?: return

        val (afterRoundClubs, updatedFixtures) = SeasonEngine.applyRoundResults(
            clubs = _state.value.clubs,
            fixtures = _state.value.fixtures,
            liveState = live,
            userClubId = userClub.id,
            trainingFocus = _state.value.trainingFocus,
            marking = _state.value.marking
        )

        val userScore = if (live.userIsHome) live.userMatch.homeScore else live.userMatch.awayScore
        val oppScore = if (live.userIsHome) live.userMatch.awayScore else live.userMatch.homeScore
        val sponsor = _state.value.activeSponsor
        val sponsorPayout = (sponsor?.perRoundBase ?: 0L) +
            if (userScore > oppScore) (sponsor?.winBonus ?: 0L) else 0L

        val updatedClubs = if (sponsorPayout > 0L) {
            afterRoundClubs.map { c -> if (c.id == userClub.id) c.copy(budget = c.budget + sponsorPayout) else c }
        } else afterRoundClubs

        val updatedUser = updatedClubs.first { it.id == userClub.id }
        val table = SeasonEngine.sortStandings(updatedClubs, updatedUser.division)
        val teamCount = table.size.coerceAtLeast(1)
        val totalRounds = updatedUser.division.totalRounds
        val newPos = (table.indexOfFirst { it.id == updatedUser.id } + 1).coerceIn(1, teamCount)

        val confDelta = when {
            userScore > oppScore -> 4L
            userScore == oppScore -> 0L
            else -> -6L
        }
        val newConfidence = (career.boardConfidence + confDelta).coerceIn(0L, 100L)
        val newRound = (career.currentRound + 1L).coerceAtMost(totalRounds + 1L)
        val starterIds = updatedUser.players.filter { it.isStarter }.map { it.id }.take(11)
        val playedRoundNum = career.currentRound.toInt()
        val updatedCups = CupAndCareerEngine.autoAdvanceIdleCups(
            cups = _state.value.cups,
            clubs = updatedClubs,
            userClubId = updatedUser.id,
            completedLeagueRound = playedRoundNum
        )
        val primaryCup = updatedCups.firstOrNull()
        val newCopaStage = when {
            primaryCup == null -> career.copaStage
            primaryCup.championClubId == updatedUser.id -> "Campeão 🏆"
            primaryCup.ties.any {
                it.stageIndex == primaryCup.currentStageIndex &&
                    it.winnerClubId == null &&
                    (it.homeClubId == updatedUser.id || it.awayClubId == updatedUser.id)
            } -> primaryCup.cupType.stageNames.getOrElse(primaryCup.currentStageIndex) { "Final" }
            primaryCup.championClubId != null -> "Encerrada"
            else -> "Eliminado"
        }

        val seasonDone = newRound > totalRounds.toLong()
        val sackedNow = !seasonDone && newConfidence <= 10L
        val managerName = _state.value.profile?.managerName ?: initialDisplayName.ifBlank { "Técnico" }
        val awards = if (seasonDone) {
            CupAndCareerEngine.computeSeasonAwards(updatedClubs, updatedUser, career.seasonYear, managerName)
        } else null

        val div = updatedUser.division
        val seasonSummary = if (seasonDone) {
            val divLabel = div.label
            val relCutoff = teamCount - div.relegationSpots + 1
            when {
                newPos == 1 -> "CAMPEÃO! O ${updatedUser.name} levantou a taça da $divLabel!"
                div.promotionSpots > 0 && newPos <= div.promotionSpots -> {
                    val targetUp = div.promotedTo?.let { Division.fromCode(it).shortLabel } ?: "divisão acima"
                    "ACESSO GARANTIDO! O ${updatedUser.name} subiu para a $targetUp!"
                }
                div.championsSpots > 0 && newPos <= div.championsSpots -> {
                    val contName = if (div.country in listOf("Brasil", "Argentina")) "Libertadores" else "Champions League"
                    "Classificado para a $contName (${newPos}º lugar)!"
                }
                div.relegationSpots > 0 && div.relegatedTo != null && newPos >= relCutoff -> {
                    val targetDown = Division.fromCode(div.relegatedTo).shortLabel
                    "Rebaixado para a $targetDown (${newPos}º lugar). Hora de reconstruir!"
                }
                else -> "Temporada ${career.seasonYear} encerrada em ${newPos}º lugar na $divLabel."
            }
        } else null

        val updatedCareer = career.copy(
            currentRound = newRound,
            budget = updatedUser.budget,
            boardConfidence = newConfidence,
            points = updatedUser.points.toLong(),
            leaguePosition = newPos.toLong(),
            wins = updatedUser.wins.toLong(),
            draws = updatedUser.draws.toLong(),
            losses = updatedUser.losses.toLong(),
            goalsFor = updatedUser.goalsFor.toLong(),
            goalsAgainst = updatedUser.goalsAgainst.toLong(),
            copaStage = newCopaStage,
            activeStarterIds = starterIds
        )

        val newOffers = SeasonEngine.generateIncomingOffers(updatedUser, updatedClubs)
        val newJobOffers = if (playedRoundNum % 6 == 0) {
            CupAndCareerEngine.evaluateJobOffers(updatedUser, newPos, newConfidence, updatedClubs, playedRoundNum)
        } else _state.value.jobOffers

        val roundNews = NewsGenerator.buildRoundNews(
            round = playedRoundNum,
            userClub = updatedUser,
            liveState = live,
            allClubs = updatedClubs,
            updatedFixtures = updatedFixtures,
            incomingOffers = newOffers,
            seasonSummary = seasonSummary
        )

        val monthIdx = FinanceEngine.monthIndexForRound(playedRoundNum, totalRounds)
        val roundTicket = if (live.userIsHome) live.ticketRevenue else 0L
        val roundSalary = userClub.weeklyWages / 4L
        val roundOther = (if (!live.userIsHome) 320_000L else 0L) + sponsorPayout

        _state.update {
            val prevEntry = it.monthlyLedger[monthIdx] ?: MonthlyLedgerRecord(monthIndex = monthIdx)
            val newLedger = it.monthlyLedger + (
                monthIdx to prevEntry.copy(
                    ticketIncome = prevEntry.ticketIncome + roundTicket,
                    salaryExpense = prevEntry.salaryExpense + roundSalary,
                    otherDelta = prevEntry.otherDelta + roundOther,
                    roundsPlayed = prevEntry.roundsPlayed + 1,
                    homeMatchesPlayed = prevEntry.homeMatchesPlayed + if (live.userIsHome) 1 else 0,
                    endingBudget = updatedUser.budget
                )
            )
            val updatedTrends = FinanceEngine.buildMonthlyTrends(
                club = updatedUser,
                fixtures = updatedFixtures,
                currentRound = updatedCareer.currentRound.toInt(),
                ticketPriceLevel = it.ticketPriceLevel,
                realizedLedger = newLedger,
                activeSponsor = sponsor
            )
            it.copy(
                activeCareer = updatedCareer,
                clubs = updatedClubs,
                fixtures = updatedFixtures,
                liveMatch = null,
                incomingOffers = newOffers,
                cups = updatedCups,
                jobOffers = newJobOffers,
                isSacked = sackedNow,
                seasonAwards = awards,
                newsItems = (roundNews + it.newsItems).take(60),
                monthlyLedger = newLedger,
                monthlyFinances = updatedTrends,
                seasonTicketRevenue = it.seasonTicketRevenue + roundTicket,
                seasonFinishedMessage = seasonSummary,
                snackMessage = if (sackedNow) "A diretoria decidiu demitir você após queda de confiança!" else it.snackMessage
            )
        }
        persistCareerUpdate(updatedCareer)

        if (userScore > oppScore) {
            _state.value.profile?.let { profile ->
                viewModelScope.launch {
                    careerRepo.updateManagerSettings(
                        managerName = profile.managerName,
                        autoSyncEnabled = profile.autoSyncEnabled,
                        preferredSimSpeed = profile.preferredSimSpeed,
                        totalWinsDelta = 1L
                    )
                }
            }
        }

        if (seasonDone && newPos == 1) {
            viewModelScope.launch {
                careerRepo.addTrophy(
                    TrophyRecord(
                        userId = currentUserId,
                        careerId = career.id,
                        competitionName = updatedUser.division.label,
                        clubName = updatedUser.name,
                        seasonYear = career.seasonYear,
                        finalPosition = 1L
                    )
                )
                _state.value.profile?.let { profile ->
                    careerRepo.updateManagerSettings(
                        managerName = profile.managerName,
                        autoSyncEnabled = profile.autoSyncEnabled,
                        preferredSimSpeed = profile.preferredSimSpeed,
                        totalTitlesDelta = 1L
                    )
                }
            }
        }

        val roundInt = newRound.toInt()
        if (!seasonDone && _state.value.profile?.autoSyncEnabled == true && roundInt in listOf(5, 14, 22, 30)) {
            syncLiveSquads(automatic = true, priorityDivision = div)
        }
    }

    private fun finishCupMatch(live: LiveMatchState) {
        val career = _state.value.activeCareer ?: return
        val userClub = _state.value.userClub ?: return
        val cupType = live.cupType ?: return
        val cupTieId = live.cupTieId ?: return

        val afterMatchClubs = SeasonEngine.applyCupMatchResults(
            clubs = _state.value.clubs,
            liveState = live,
            userClubId = userClub.id,
            trainingFocus = _state.value.trainingFocus,
            marking = _state.value.marking
        )
        val clubMap = afterMatchClubs.associateBy { it.id }

        val targetCup = _state.value.cups.firstOrNull { it.cupType == cupType } ?: return
        val resolvedCup = CupAndCareerEngine.resolveCupStageWithResults(
            cup = targetCup,
            userTieId = cupTieId,
            userHomeScore = live.userMatch.homeScore,
            userAwayScore = live.userMatch.awayScore,
            userShootout = live.penaltyShootout,
            liveOtherMatches = live.otherMatches,
            clubMap = clubMap
        )
        val resolvedTie = resolvedCup.ties.firstOrNull { it.id == cupTieId }
        val userWon = resolvedTie?.winnerClubId == userClub.id
        val isChampion = resolvedCup.championClubId == userClub.id
        val prize = if (userWon) {
            CupAndCareerEngine.cupAdvancePrize(cupType, targetCup.currentStageIndex, isChampion)
        } else 450_000L
        val sponsorTitleBonus = if (isChampion) (_state.value.activeSponsor?.titleBonus ?: 0L) else 0L
        val totalBonus = prize + sponsorTitleBonus

        val updatedClubs = afterMatchClubs.map { c ->
            if (c.id == userClub.id) c.copy(budget = c.budget + totalBonus) else c
        }
        val updatedUser = updatedClubs.first { it.id == userClub.id }
        val updatedCups = _state.value.cups.map { if (it.cupType == cupType) resolvedCup else it }

        val cupStageText = when {
            isChampion -> "Campeão 🏆"
            userWon -> cupType.stageNames.getOrElse(resolvedCup.currentStageIndex) { "Final" }
            else -> "Eliminado (${targetCup.cupType.stageNames[targetCup.currentStageIndex]})"
        }
        val confDelta = when {
            isChampion -> 15L
            userWon -> 5L
            else -> -4L
        }
        val updatedCareer = career.copy(
            budget = updatedUser.budget,
            boardConfidence = (career.boardConfidence + confDelta).coerceIn(10L, 100L),
            copaStage = cupStageText
        )

        val cupNews = NewsGenerator.buildCupNews(
            round = career.currentRound.toInt(),
            cupTitle = cupType.title,
            stageLabel = live.cupStageLabel ?: "Mata-Mata",
            userClub = updatedUser,
            homeShort = live.userMatch.homeShort,
            homeScore = live.userMatch.homeScore,
            awayShort = live.userMatch.awayShort,
            awayScore = live.userMatch.awayScore,
            shootoutSummary = live.penaltyShootout?.summary,
            userWon = userWon,
            isChampion = isChampion
        )

        val monthIdx = FinanceEngine.monthIndexForRound(career.currentRound.toInt(), updatedUser.division.totalRounds)
        val cupGate = if (live.userIsHome) live.ticketRevenue else 0L

        _state.update { state ->
            val prevEntry = state.monthlyLedger[monthIdx] ?: MonthlyLedgerRecord(monthIndex = monthIdx)
            val newLedger = state.monthlyLedger + (
                monthIdx to prevEntry.copy(
                    ticketIncome = prevEntry.ticketIncome + cupGate,
                    otherDelta = prevEntry.otherDelta + totalBonus,
                    endingBudget = updatedUser.budget
                )
            )
            val updatedTrends = FinanceEngine.buildMonthlyTrends(
                club = updatedUser,
                fixtures = state.fixtures,
                currentRound = updatedCareer.currentRound.toInt(),
                ticketPriceLevel = state.ticketPriceLevel,
                realizedLedger = newLedger,
                activeSponsor = state.activeSponsor
            )
            state.copy(
                activeCareer = updatedCareer,
                clubs = updatedClubs,
                liveMatch = null,
                cups = updatedCups,
                newsItems = (listOf(cupNews) + state.newsItems).take(60),
                monthlyLedger = newLedger,
                monthlyFinances = updatedTrends,
                seasonTicketRevenue = state.seasonTicketRevenue + cupGate,
                snackMessage = when {
                    isChampion -> "🏆 CAMPEÃO DA ${cupType.title.uppercase()}! (+${formatCurrency(totalBonus)})"
                    userWon -> "Classificado na ${cupType.title}! Prêmio de ${formatCurrency(totalBonus)}"
                    else -> "Eliminado da ${cupType.title}."
                }
            )
        }
        persistCareerUpdate(updatedCareer)

        if (isChampion) {
            viewModelScope.launch {
                careerRepo.addTrophy(
                    TrophyRecord(
                        userId = currentUserId,
                        careerId = career.id,
                        competitionName = cupType.title,
                        clubName = updatedUser.name,
                        seasonYear = career.seasonYear,
                        finalPosition = 1L
                    )
                )
                _state.value.profile?.let { profile ->
                    careerRepo.updateManagerSettings(
                        managerName = profile.managerName,
                        autoSyncEnabled = profile.autoSyncEnabled,
                        preferredSimSpeed = profile.preferredSimSpeed,
                        totalTitlesDelta = 1L,
                        totalWinsDelta = 1L
                    )
                }
            }
        }
    }

    fun startNextSeason() {
        val career = _state.value.activeCareer ?: return
        val nextClubs = SeasonEngine.advanceToNextSeason(_state.value.clubs)
        val userClub = nextClubs.first { it.id == career.clubId }
        val allFixtures = SeasonEngine.generateAllFixtures(nextClubs)
        val nextCups = CupAndCareerEngine.buildInitialCups(userClub, nextClubs)
        val nextYouth = CupAndCareerEngine.generateYouthIntake(userClub, _state.value.trainingFocus)

        val updatedCareer = career.copy(
            division = userClub.division.code,
            seasonYear = career.seasonYear + 1L,
            currentRound = 1L,
            budget = userClub.budget,
            points = 0L,
            leaguePosition = 1L,
            wins = 0L,
            draws = 0L,
            losses = 0L,
            goalsFor = 0L,
            goalsAgainst = 0L,
            copaStage = "Oitavas"
        )
        val managerName = _state.value.profile?.managerName ?: initialDisplayName.ifBlank { "Técnico" }
        val seasonStartNews = NewsGenerator.buildInitialNews(
            userClub = userClub,
            allClubs = nextClubs,
            seasonYear = updatedCareer.seasonYear,
            managerName = managerName
        )

        val nextSeasonTrends = FinanceEngine.buildMonthlyTrends(
            club = userClub,
            fixtures = allFixtures,
            currentRound = 1,
            ticketPriceLevel = _state.value.ticketPriceLevel,
            realizedLedger = emptyMap(),
            activeSponsor = _state.value.activeSponsor
        )

        _state.update {
            it.copy(
                activeCareer = updatedCareer,
                clubs = nextClubs,
                fixtures = allFixtures,
                cups = nextCups,
                youthProspects = nextYouth,
                seasonAwards = null,
                isSacked = false,
                newsItems = (seasonStartNews + it.newsItems).take(60),
                monthlyLedger = emptyMap(),
                monthlyFinances = nextSeasonTrends,
                seasonTicketRevenue = 0L,
                seasonFinishedMessage = null,
                snackMessage = "Temporada ${updatedCareer.seasonYear} iniciada na ${userClub.division.label}!"
            )
        }
        persistCareerUpdate(updatedCareer)
    }

    private fun persistCareerUpdate(career: CareerSave) {
        if (career.id.isBlank()) return
        viewModelScope.launch {
            careerRepo.updateCareer(career)
                .onFailure { e ->
                    Log.w(TAG, "Falha ao salvar carreira localmente: ${e.message}")
                }
        }
        persistFullSnapshot(career.id)
    }

    private fun persistFullSnapshot(careerId: String) {
        val state = _state.value
        if (careerId.isBlank() || state.clubs.isEmpty() || state.fixtures.isEmpty()) return
        val snapshot = SeasonSnapshot(
            clubs = state.clubs,
            fixtures = state.fixtures,
            monthlyLedger = state.monthlyLedger,
            seasonTicketRevenue = state.seasonTicketRevenue,
            cups = state.cups,
            activeSponsor = state.activeSponsor,
            youthProspects = state.youthProspects,
            pendingJobOffers = state.jobOffers,
            isSacked = state.isSacked
        )
        viewModelScope.launch {
            careerRepo.saveSeasonSnapshot(careerId, snapshot)
                .onFailure { e ->
                    Log.w(TAG, "Falha ao salvar snapshot completo: ${e.message}")
                }
        }
    }

    private companion object {
        const val TAG = "BrasManagerVM"
    }
}

