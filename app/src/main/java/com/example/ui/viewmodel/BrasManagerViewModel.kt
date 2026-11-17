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
import com.example.domain.engine.BallonDorEngine
import com.example.domain.engine.ChampionsLeagueEngine
import com.example.domain.engine.ContinentalGroupEngine
import com.example.domain.engine.CupAndCareerEngine
import com.example.domain.engine.FinanceEngine
import com.example.domain.engine.GovernanceAndFfpEngine
import com.example.domain.engine.MedicalEngine
import com.example.domain.engine.NewsGenerator
import com.example.domain.engine.SeasonEngine
import com.example.domain.engine.TransferWindowEngine
import com.example.domain.model.AnnualAwardsRecord
import com.example.domain.model.BallonDorCandidate
import com.example.domain.model.ChampionsLeaguePhase
import com.example.domain.model.ChampionsLeagueState
import com.example.domain.model.Club
import com.example.domain.model.ClubGovernanceState
import com.example.domain.model.ContinentalCupType
import com.example.domain.model.ContinentalPhase
import com.example.domain.model.ContinentalTournamentState
import com.example.domain.model.CupCompetitionState
import com.example.domain.model.CupMatchTie
import com.example.domain.model.CupType
import com.example.domain.model.Division
import com.example.domain.model.FairPlayState
import com.example.domain.model.Fixture
import com.example.domain.model.Formation
import com.example.domain.model.IncomingTransferOffer
import com.example.domain.model.LiveMatchState
import com.example.domain.model.ManagerJobOffer
import com.example.domain.model.MarkingStyle
import com.example.domain.model.MatchEvent
import com.example.domain.model.MatchEventType
import com.example.domain.model.MedicalTreatmentType
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
import com.example.domain.model.TransferWindowStatus
import com.example.domain.model.YouthProspect
import com.example.ui.components.formatCurrency
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
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
import kotlinx.coroutines.withContext

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
    val championsLeague: ChampionsLeagueState? = null,
    val continentalTournaments: List<ContinentalTournamentState> = emptyList(),
    val ballonDorContenders: List<BallonDorCandidate> = emptyList(),
    val annualAwardsHistory: List<AnnualAwardsRecord> = emptyList(),
    val userBallonDorPlayerId: String? = null,
    val userBallonDorVoteSeason: Long = 0L,
    val fairPlay: FairPlayState = FairPlayState(),
    val governance: ClubGovernanceState = ClubGovernanceState(),
    val lastSavedAtMs: Long? = null,
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

    val pendingChampionsMatch: ChampionsLeagueEngine.PendingChampionsMatch?
        get() {
            val car = activeCareer ?: return null
            val completedRounds = (car.currentRound.toInt() - 1).coerceAtLeast(0)
            return ChampionsLeagueEngine.findPendingUserMatch(championsLeague, car.clubId, completedRounds)
        }

    val pendingContinentalMatch: ContinentalGroupEngine.PendingContinentalMatch?
        get() {
            val car = activeCareer ?: return null
            val completedRounds = (car.currentRound.toInt() - 1).coerceAtLeast(0)
            return ContinentalGroupEngine.findPendingUserMatch(continentalTournaments, car.clubId, completedRounds)
        }

    val transferWindowStatus: TransferWindowStatus
        get() {
            val round = activeCareer?.currentRound?.toInt() ?: 1
            val totalRounds = userClub?.division?.totalRounds ?: 38
            return TransferWindowEngine.windowStatus(round, totalRounds)
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
                    .filterNot { it.cupType == CupType.CHAMPIONS || it.cupType == CupType.LIBERTADORES }
                val initialUcl = ChampionsLeagueEngine.buildInitialChampionsLeague(selectedClub, initializedClubs)
                val initialContinental = ContinentalGroupEngine.buildInitialContinentalTournaments(selectedClub, initializedClubs)
                val initialRace = BallonDorEngine.computeLiveCandidates(initializedClubs, initialUcl)
                val initialGov = GovernanceAndFfpEngine.buildInitialGovernance(selectedClub)
                val initialFfp = FairPlayState(
                    allowedDeficitLimit = (selectedClub.prestige * 5_000_000L).coerceAtLeast(8_000_000L)
                )
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
                        championsLeague = initialUcl,
                        continentalTournaments = initialContinental,
                        ballonDorContenders = initialRace,
                        annualAwardsHistory = emptyList(),
                        userBallonDorPlayerId = null,
                        userBallonDorVoteSeason = 0L,
                        fairPlay = initialFfp,
                        governance = initialGov,
                        lastSavedAtMs = System.currentTimeMillis(),
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
            val restoredUcl: ChampionsLeagueState
            val restoredContinental: List<ContinentalTournamentState>
            val restoredAwardsHistory: List<AnnualAwardsRecord>
            val restoredFfp: FairPlayState
            val restoredGov: ClubGovernanceState
            val restoredVotePlayerId: String?
            val restoredVoteSeason: Long
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
                restoredCups = snapshot.cups
                    .ifEmpty { CupAndCareerEngine.buildInitialCups(userClub, restoredClubs) }
                    .filterNot { it.cupType == CupType.CHAMPIONS || it.cupType == CupType.LIBERTADORES }
                restoredUcl = snapshot.championsLeague ?: ChampionsLeagueEngine.buildInitialChampionsLeague(userClub, restoredClubs)
                restoredContinental = snapshot.continentalTournaments.ifEmpty {
                    ContinentalGroupEngine.buildInitialContinentalTournaments(userClub, restoredClubs)
                }
                restoredAwardsHistory = snapshot.annualAwardsHistory
                restoredFfp = snapshot.fairPlay
                restoredGov = snapshot.governance.let { g ->
                    if (g.electionCandidates.isEmpty()) g.copy(electionCandidates = GovernanceAndFfpEngine.generateCandidates(userClub)) else g
                }
                restoredVotePlayerId = snapshot.userBallonDorPlayerId
                restoredVoteSeason = snapshot.userBallonDorVoteSeason
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
                    .filterNot { it.cupType == CupType.CHAMPIONS || it.cupType == CupType.LIBERTADORES }
                restoredUcl = ChampionsLeagueEngine.buildInitialChampionsLeague(userClub, restoredClubs)
                restoredContinental = ContinentalGroupEngine.buildInitialContinentalTournaments(userClub, restoredClubs)
                restoredAwardsHistory = emptyList()
                restoredFfp = FairPlayState(allowedDeficitLimit = (userClub.prestige * 5_000_000L).coerceAtLeast(8_000_000L))
                restoredGov = GovernanceAndFfpEngine.buildInitialGovernance(userClub)
                restoredVotePlayerId = null
                restoredVoteSeason = 0L
                restoredSponsor = CupAndCareerEngine.availableSponsorsForClub(userClub).firstOrNull()
                restoredYouth = CupAndCareerEngine.generateYouthIntake(userClub, trainingFocus)
                restoredJobs = emptyList()
                restoredSacked = false
            }

            val restoredUserClub = restoredClubs.first { it.id == career.clubId }
            val activeVoteId = if (restoredVoteSeason == career.seasonYear) restoredVotePlayerId else null
            val liveRace = BallonDorEngine.computeLiveCandidates(restoredClubs, restoredUcl, userVotedPlayerId = activeVoteId)
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
                    championsLeague = restoredUcl,
                    continentalTournaments = restoredContinental,
                    ballonDorContenders = liveRace,
                    annualAwardsHistory = restoredAwardsHistory,
                    userBallonDorPlayerId = restoredVotePlayerId,
                    userBallonDorVoteSeason = restoredVoteSeason,
                    fairPlay = restoredFfp,
                    governance = restoredGov,
                    lastSavedAtMs = System.currentTimeMillis(),
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
                    snackMessage = "Save carregado: ${career.clubName} (Rodada ${career.currentRound})!"
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

    fun saveCurrentCareerNow(silent: Boolean = false) {
        val career = _state.value.activeCareer ?: return
        if (career.id.isBlank()) return
        val snapshot = buildCurrentSnapshot()
        viewModelScope.launch {
            withContext(NonCancellable) {
                careerRepo.updateCareer(career)
                if (snapshot != null) {
                    careerRepo.saveSeasonSnapshot(career.id, snapshot)
                }
            }
            val now = System.currentTimeMillis()
            _state.update {
                it.copy(
                    lastSavedAtMs = now,
                    snackMessage = if (!silent) "Save gravado com sucesso (${career.clubName} • Rodada ${career.currentRound})!" else it.snackMessage
                )
            }
        }
    }

    fun exitToCareerHub() {
        val currentCareer = _state.value.activeCareer
        val snapshot = buildCurrentSnapshot()
        if (currentCareer != null && currentCareer.id.isNotBlank()) {
            viewModelScope.launch {
                withContext(NonCancellable) {
                    careerRepo.updateCareer(currentCareer)
                    if (snapshot != null) {
                        careerRepo.saveSeasonSnapshot(currentCareer.id, snapshot)
                    }
                }
            }
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
                championsLeague = null,
                continentalTournaments = emptyList(),
                ballonDorContenders = emptyList(),
                annualAwardsHistory = emptyList(),
                userBallonDorPlayerId = null,
                userBallonDorVoteSeason = 0L,
                fairPlay = FairPlayState(),
                governance = ClubGovernanceState(),
                lastSavedAtMs = null,
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

    fun treatInjuredPlayer(playerId: String, treatment: MedicalTreatmentType) {
        val career = _state.value.activeCareer ?: return
        val userClub = _state.value.userClub ?: return
        val player = userClub.players.firstOrNull { it.id == playerId } ?: return
        if (player.injuredRounds <= 0) {
            _state.update { it.copy(snackMessage = "${player.name} já está recuperado e liberado pelo DM!") }
            return
        }
        if (player.injuryTreatedThisInjury && treatment.roundsReduced > 0) {
            _state.update { it.copy(snackMessage = "${player.name} já recebeu tratamento intensivo para esta lesão.") }
            return
        }
        if (userClub.budget < treatment.costReais) {
            _state.update { it.copy(snackMessage = "Caixa insuficiente para ${treatment.label} (${formatCurrency(treatment.costReais)}).") }
            return
        }
        val treated = MedicalEngine.applyTreatment(player, treatment, career.currentRound.toInt())
        val updatedClub = userClub.copy(
            budget = userClub.budget - treatment.costReais,
            players = userClub.players.map { if (it.id == playerId) treated else it }
        )
        val updatedCareer = career.copy(budget = updatedClub.budget)
        _state.update { state ->
            val monthIdx = FinanceEngine.monthIndexForRound(career.currentRound.toInt(), updatedClub.division.totalRounds)
            val prevEntry = state.monthlyLedger[monthIdx] ?: MonthlyLedgerRecord(monthIndex = monthIdx)
            val newLedger = if (treatment.costReais > 0L) {
                state.monthlyLedger + (
                    monthIdx to prevEntry.copy(
                        otherDelta = prevEntry.otherDelta - treatment.costReais,
                        endingBudget = updatedClub.budget
                    )
                )
            } else state.monthlyLedger
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
                clubs = state.clubs.map { if (it.id == updatedClub.id) updatedClub else it },
                monthlyLedger = newLedger,
                monthlyFinances = updatedTrends,
                snackMessage = if (treated.injuredRounds == 0) {
                    "Alta médica! ${player.name} está recuperado e pronto para jogar!"
                } else {
                    "${treatment.label} aplicado em ${player.name}. Restam ${treated.injuredRounds} rodada(s)."
                }
            )
        }
        persistCareerUpdate(updatedCareer)
    }

    fun voteBallonDor(playerId: String, playerName: String) {
        val career = _state.value.activeCareer ?: return
        _state.update { state ->
            val updatedRace = BallonDorEngine.computeLiveCandidates(
                clubs = state.clubs,
                championsLeague = state.championsLeague,
                userVotedPlayerId = playerId
            )
            state.copy(
                userBallonDorPlayerId = playerId,
                userBallonDorVoteSeason = career.seasonYear,
                ballonDorContenders = updatedRace,
                snackMessage = "Voto registrado na Eleição Bola de Ouro ${career.seasonYear}: $playerName!"
            )
        }
        persistFullSnapshot(career.id)
    }

    fun votePresidentialElection(candidateId: String) {
        val career = _state.value.activeCareer ?: return
        val userClub = _state.value.userClub ?: return
        val currentGov = _state.value.governance
        if (!currentGov.electionPending && career.seasonYear < currentGov.nextElectionYear) {
            _state.update {
                it.copy(
                    snackMessage = "Eleições acontecem a cada 4 anos (próxima eleição: Temporada ${currentGov.nextElectionYear})."
                )
            }
            return
        }
        val (newGov, budgetGrant) = GovernanceAndFfpEngine.resolveElection(
            governance = currentGov,
            supportedCandidateId = candidateId,
            club = userClub,
            currentSeasonYear = career.seasonYear
        )
        val updatedUser = userClub.copy(budget = userClub.budget + budgetGrant)
        val updatedCareer = career.copy(
            budget = updatedUser.budget,
            boardConfidence = (career.boardConfidence + 6L).coerceIn(10L, 100L)
        )
        val electionNews = NewsItem(
            id = "news_elec_${career.seasonYear}_${career.currentRound}",
            category = NewsCategory.CLUB_BUZZ,
            round = career.currentRound.toInt(),
            headline = "Eleição presidencial no ${userClub.name}: ${newGov.presidentName} eleito!",
            body = newGov.lastElectionSummary ?: "Novo presidente assume com aporte de ${formatCurrency(budgetGrant)}.",
            clubId = updatedUser.id,
            clubName = updatedUser.name,
            clubShortName = updatedUser.shortName,
            clubColorHex = updatedUser.primaryColorHex,
            clubSecondaryColorHex = updatedUser.secondaryColorHex,
            isBreaking = true,
            tagLabel = "ELEIÇÃO"
        )
        _state.update { state ->
            val monthIdx = FinanceEngine.monthIndexForRound(career.currentRound.toInt(), updatedUser.division.totalRounds)
            val prevEntry = state.monthlyLedger[monthIdx] ?: MonthlyLedgerRecord(monthIndex = monthIdx)
            val newLedger = state.monthlyLedger + (
                monthIdx to prevEntry.copy(
                    otherDelta = prevEntry.otherDelta + budgetGrant,
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
                clubs = state.clubs.map { if (it.id == updatedUser.id) updatedUser else it },
                governance = newGov,
                monthlyLedger = newLedger,
                monthlyFinances = updatedTrends,
                newsItems = (listOf(electionNews) + state.newsItems).take(60),
                snackMessage = "${newGov.presidentName} eleito! +${formatCurrency(budgetGrant)} no orçamento."
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
            .filterNot { it.cupType == CupType.CHAMPIONS || it.cupType == CupType.LIBERTADORES }
        val newContinental = ContinentalGroupEngine.buildInitialContinentalTournaments(initializedClub, updatedClubs)
        val newGov = GovernanceAndFfpEngine.buildInitialGovernance(initializedClub, career.seasonYear)
        val newFfp = FairPlayState(allowedDeficitLimit = (initializedClub.prestige * 5_000_000L).coerceAtLeast(8_000_000L))
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
                continentalTournaments = newContinental,
                governance = newGov,
                fairPlay = newFfp,
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
        val banRounds = _state.value.fairPlay.transferBanRounds
        if (banRounds > 0) {
            _state.update {
                it.copy(snackMessage = "Transfer Ban da FIFA ativo por $banRounds rodada(s) devido ao Fair Play Financeiro!")
            }
            return
        }
        val window = _state.value.transferWindowStatus
        if (!window.isOpen) {
            _state.update { it.copy(snackMessage = "Janela de transferências fechada! ${window.statusLabel}.") }
            return
        }
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
        val banRounds = _state.value.fairPlay.transferBanRounds
        if (banRounds > 0) {
            _state.update {
                it.copy(snackMessage = "Bloqueio FIFA de registros ativo por $banRounds rodada(s) devido ao Fair Play Financeiro!")
            }
            return
        }
        val window = _state.value.transferWindowStatus
        if (!window.isOpen) {
            _state.update { it.copy(snackMessage = "Janela de transferências fechada! ${window.statusLabel}.") }
            return
        }
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
        val window = _state.value.transferWindowStatus
        if (!window.isOpen) {
            _state.update { it.copy(snackMessage = "Janela de transferências fechada! A venda será concluída apenas com o mercado aberto.") }
            return
        }
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

    fun startChampionsLeagueMatch(pending: ChampionsLeagueEngine.PendingChampionsMatch) {
        val userClub = _state.value.userClub ?: return
        if (_state.value.isSacked) return
        val ucl = _state.value.championsLeague ?: return

        val validStarters = userClub.players.filter { it.isStarter && it.isAvailable }
        val preparedClub = if (validStarters.size < 11) {
            val auto = SeasonEngine.autoSelectBestLineup(userClub.players, _state.value.formation)
            userClub.copy(players = auto)
        } else userClub
        val updatedClubs = _state.value.clubs.map { if (it.id == preparedClub.id) preparedClub else it }

        val uclLive = when (pending) {
            is ChampionsLeagueEngine.PendingChampionsMatch.LeaguePhaseMatch ->
                ChampionsLeagueEngine.prepareLeaguePhaseLiveMatch(
                    fixture = pending.fixture,
                    userClub = preparedClub,
                    allClubs = updatedClubs,
                    ucl = ucl,
                    ticketPriceLevel = _state.value.ticketPriceLevel
                )
            is ChampionsLeagueEngine.PendingChampionsMatch.KnockoutMatch ->
                ChampionsLeagueEngine.prepareKnockoutLiveMatch(
                    tie = pending.tie,
                    userClub = preparedClub,
                    allClubs = updatedClubs,
                    ucl = ucl,
                    ticketPriceLevel = _state.value.ticketPriceLevel
                )
        }

        _state.update {
            it.copy(
                clubs = updatedClubs,
                liveMatch = uclLive.copy(
                    speed = (_state.value.profile?.preferredSimSpeed ?: 2L).toInt()
                )
            )
        }
        launchTicker()
    }

    fun startContinentalMatch(pending: ContinentalGroupEngine.PendingContinentalMatch) {
        val userClub = _state.value.userClub ?: return
        if (_state.value.isSacked) return

        val validStarters = userClub.players.filter { it.isStarter && it.isAvailable }
        val preparedClub = if (validStarters.size < 11) {
            val auto = SeasonEngine.autoSelectBestLineup(userClub.players, _state.value.formation)
            userClub.copy(players = auto)
        } else userClub
        val updatedClubs = _state.value.clubs.map { if (it.id == preparedClub.id) preparedClub else it }

        val contLive = when (pending) {
            is ContinentalGroupEngine.PendingContinentalMatch.GroupMatch ->
                ContinentalGroupEngine.prepareGroupLiveMatch(
                    tournament = pending.tournament,
                    fixture = pending.fixture,
                    userClub = preparedClub,
                    allClubs = updatedClubs,
                    ticketPriceLevel = _state.value.ticketPriceLevel
                )
            is ContinentalGroupEngine.PendingContinentalMatch.KnockoutMatch ->
                ContinentalGroupEngine.prepareKnockoutLiveMatch(
                    tournament = pending.tournament,
                    tie = pending.tie,
                    userClub = preparedClub,
                    allClubs = updatedClubs,
                    ticketPriceLevel = _state.value.ticketPriceLevel
                )
        }

        _state.update {
            it.copy(
                clubs = updatedClubs,
                liveMatch = contLive.copy(
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
        if (live.championsFixtureId != null) {
            finishChampionsLeaguePhaseMatch(live)
            return
        }
        if (live.continentalFixtureId != null) {
            finishContinentalGroupMatch(live)
            return
        }
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
            marking = _state.value.marking,
            seasonYear = career.seasonYear
        )

        val userScore = if (live.userIsHome) live.userMatch.homeScore else live.userMatch.awayScore
        val oppScore = if (live.userIsHome) live.userMatch.awayScore else live.userMatch.homeScore
        val sponsor = _state.value.activeSponsor
        val sponsorPayout = (sponsor?.perRoundBase ?: 0L) +
            if (userScore > oppScore) (sponsor?.winBonus ?: 0L) else 0L

        val updatedClubs = if (sponsorPayout > 0L) {
            afterRoundClubs.map { c -> if (c.id == userClub.id) c.copy(budget = c.budget + sponsorPayout) else c }
        } else afterRoundClubs

        val preFfpUser = updatedClubs.first { it.id == userClub.id }
        val preFfpTable = SeasonEngine.sortStandings(updatedClubs, preFfpUser.division)
        val teamCount = preFfpTable.size.coerceAtLeast(1)
        val totalRounds = preFfpUser.division.totalRounds
        val prePos = (preFfpTable.indexOfFirst { it.id == preFfpUser.id } + 1).coerceIn(1, teamCount)

        val confDelta = when {
            userScore > oppScore -> 4L
            userScore == oppScore -> 0L
            else -> -6L
        }
        val newRound = (career.currentRound + 1L).coerceAtMost(totalRounds + 1L)
        val playedRoundNum = career.currentRound.toInt()
        val seasonDone = newRound > totalRounds.toLong()

        val monthIdx = FinanceEngine.monthIndexForRound(playedRoundNum, totalRounds)
        val roundTicket = if (live.userIsHome) live.ticketRevenue else 0L
        val roundSalary = userClub.weeklyWages / 4L
        val roundOther = (if (!live.userIsHome) 320_000L else 0L) + sponsorPayout
        val prevEntryPreview = _state.value.monthlyLedger[monthIdx] ?: MonthlyLedgerRecord(monthIndex = monthIdx)
        val previewLedger = _state.value.monthlyLedger + (
            monthIdx to prevEntryPreview.copy(
                ticketIncome = prevEntryPreview.ticketIncome + roundTicket,
                salaryExpense = prevEntryPreview.salaryExpense + roundSalary,
                otherDelta = prevEntryPreview.otherDelta + roundOther,
                roundsPlayed = prevEntryPreview.roundsPlayed + 1,
                homeMatchesPlayed = prevEntryPreview.homeMatchesPlayed + if (live.userIsHome) 1 else 0,
                endingBudget = preFfpUser.budget
            )
        )

        val ffpOutcome = GovernanceAndFfpEngine.evaluateRoundCompliance(
            currentFfp = _state.value.fairPlay,
            currentGov = _state.value.governance,
            club = preFfpUser,
            userPosition = prePos,
            monthlyLedger = previewLedger,
            completedRound = playedRoundNum,
            isSeasonEnd = seasonDone,
            seasonYear = career.seasonYear
        )

        val clubsAfterFfp = updatedClubs.map { c ->
            if (c.id == preFfpUser.id) {
                c.copy(
                    budget = ffpOutcome.updatedBudget,
                    points = (c.points - ffpOutcome.pointsPenaltyApplied).coerceAtLeast(0)
                )
            } else c
        }
        val updatedUser = clubsAfterFfp.first { it.id == userClub.id }
        val table = SeasonEngine.sortStandings(clubsAfterFfp, updatedUser.division)
        val newPos = (table.indexOfFirst { it.id == updatedUser.id } + 1).coerceIn(1, teamCount)

        val newConfidence = (career.boardConfidence + confDelta + ffpOutcome.confidenceDelta).coerceIn(0L, 100L)
        val starterIds = updatedUser.players.filter { it.isStarter }.map { it.id }.take(11)

        val updatedCups = CupAndCareerEngine.autoAdvanceIdleCups(
            cups = _state.value.cups,
            clubs = clubsAfterFfp,
            userClubId = updatedUser.id,
            completedLeagueRound = playedRoundNum
        )
        val updatedUcl = ChampionsLeagueEngine.autoAdvanceIdleChampionsLeague(
            ucl = _state.value.championsLeague,
            clubs = clubsAfterFfp,
            userClubId = updatedUser.id,
            completedLeagueRound = playedRoundNum
        )
        val updatedContinental = ContinentalGroupEngine.autoAdvanceIdleTournaments(
            tournaments = _state.value.continentalTournaments,
            clubs = clubsAfterFfp,
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

        val sackedNow = !seasonDone && newConfidence <= 10L
        val managerName = _state.value.profile?.managerName ?: initialDisplayName.ifBlank { "Técnico" }
        val awards = if (seasonDone) {
            CupAndCareerEngine.computeSeasonAwards(clubsAfterFfp, updatedUser, career.seasonYear, managerName)
        } else null

        val activeVoteId = if (_state.value.userBallonDorVoteSeason == career.seasonYear) _state.value.userBallonDorPlayerId else null
        val liveRace = BallonDorEngine.computeLiveCandidates(clubsAfterFfp, updatedUcl, userVotedPlayerId = activeVoteId)
        val annualAwardRecord = if (seasonDone) {
            BallonDorEngine.computeAnnualAwards(
                clubs = clubsAfterFfp,
                championsLeague = updatedUcl,
                seasonYear = career.seasonYear,
                userClub = updatedUser,
                managerName = managerName,
                userVotedPlayerId = activeVoteId
            )
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

        val nextWindowStatus = TransferWindowEngine.windowStatus(newRound.toInt(), totalRounds)
        val newOffers = if (nextWindowStatus.isOpen) {
            SeasonEngine.generateIncomingOffers(updatedUser, clubsAfterFfp)
        } else emptyList()
        val newJobOffers = if (playedRoundNum % 6 == 0) {
            CupAndCareerEngine.evaluateJobOffers(updatedUser, newPos, newConfidence, clubsAfterFfp, playedRoundNum)
        } else _state.value.jobOffers

        val extraNews = buildList {
            TransferWindowEngine.transitionNewsForRound(newRound.toInt(), totalRounds, updatedUser)?.let(::add)
            ffpOutcome.punishmentHeadline?.let { headline ->
                add(
                    NewsItem(
                        id = "news_ffp_${playedRoundNum}_${System.currentTimeMillis()}",
                        category = NewsCategory.CLUB_BUZZ,
                        round = playedRoundNum,
                        headline = "Sancionado pelo Fair Play / FIFA: ${updatedUser.shortName}",
                        body = headline,
                        clubId = updatedUser.id,
                        clubName = updatedUser.name,
                        clubShortName = updatedUser.shortName,
                        clubColorHex = updatedUser.primaryColorHex,
                        clubSecondaryColorHex = updatedUser.secondaryColorHex,
                        isBreaking = true,
                        tagLabel = "FAIR PLAY FIFA"
                    )
                )
            }
            annualAwardRecord?.ballonDorTop3?.firstOrNull()?.let { winner ->
                add(
                    NewsItem(
                        id = "news_ballondor_${career.seasonYear}",
                        category = NewsCategory.CLUB_BUZZ,
                        round = playedRoundNum,
                        headline = "BOLA DE OURO ${career.seasonYear}: ${winner.playerName} eleito o melhor do mundo!",
                        body = "O craque do ${winner.clubName} superou a concorrência com ${winner.goals} gols e ${winner.assists} assistências na época.",
                        clubId = winner.clubId,
                        clubName = winner.clubName,
                        clubShortName = winner.clubShortName,
                        isBreaking = true,
                        tagLabel = "BOLA DE OURO"
                    )
                )
            }
        }

        val roundNews = extraNews + NewsGenerator.buildRoundNews(
            round = playedRoundNum,
            userClub = updatedUser,
            liveState = live,
            allClubs = clubsAfterFfp,
            updatedFixtures = updatedFixtures,
            incomingOffers = newOffers,
            seasonSummary = seasonSummary,
            transferWindowOpen = nextWindowStatus.isOpen
        )

        _state.update {
            val newLedger = previewLedger + (
                monthIdx to (previewLedger[monthIdx] ?: MonthlyLedgerRecord(monthIndex = monthIdx)).copy(
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
            val updatedAwardsHistory = if (annualAwardRecord != null) {
                (it.annualAwardsHistory.filterNot { rec -> rec.seasonYear == annualAwardRecord.seasonYear } + annualAwardRecord)
                    .sortedByDescending { rec -> rec.seasonYear }
            } else it.annualAwardsHistory

            it.copy(
                activeCareer = updatedCareer,
                clubs = clubsAfterFfp,
                fixtures = updatedFixtures,
                liveMatch = null,
                incomingOffers = newOffers,
                cups = updatedCups,
                championsLeague = updatedUcl,
                continentalTournaments = updatedContinental,
                ballonDorContenders = liveRace,
                annualAwardsHistory = updatedAwardsHistory,
                fairPlay = ffpOutcome.fairPlay,
                governance = ffpOutcome.governance,
                jobOffers = newJobOffers,
                isSacked = sackedNow,
                seasonAwards = awards,
                newsItems = (roundNews + it.newsItems).take(60),
                monthlyLedger = newLedger,
                monthlyFinances = updatedTrends,
                seasonTicketRevenue = it.seasonTicketRevenue + roundTicket,
                seasonFinishedMessage = seasonSummary,
                snackMessage = when {
                    sackedNow -> "A diretoria decidiu demitir você após queda de confiança!"
                    ffpOutcome.punishmentHeadline != null -> ffpOutcome.punishmentHeadline
                    else -> it.snackMessage
                }
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

    private fun finishChampionsLeaguePhaseMatch(live: LiveMatchState) {
        val career = _state.value.activeCareer ?: return
        val userClub = _state.value.userClub ?: return
        val currentUcl = _state.value.championsLeague ?: return
        val fixtureId = live.championsFixtureId ?: return

        val afterMatchClubs = SeasonEngine.applyCupMatchResults(
            clubs = _state.value.clubs,
            liveState = live,
            userClubId = userClub.id,
            trainingFocus = _state.value.trainingFocus,
            marking = _state.value.marking,
            seasonYear = career.seasonYear
        )
        val clubMap = afterMatchClubs.associateBy { it.id }
        val resolvedUcl = ChampionsLeagueEngine.resolveLeagueMatchday(
            ucl = currentUcl,
            userFixtureId = fixtureId,
            userHomeScore = live.userMatch.homeScore,
            userAwayScore = live.userMatch.awayScore,
            liveOtherMatches = live.otherMatches,
            clubMap = clubMap
        )
        val userScore = if (live.userIsHome) live.userMatch.homeScore else live.userMatch.awayScore
        val oppScore = if (live.userIsHome) live.userMatch.awayScore else live.userMatch.homeScore
        val prize = if (userScore > oppScore) {
            ChampionsLeagueEngine.prizeForChampionsStage(ChampionsLeaguePhase.LEAGUE_PHASE, isWin = true, isChampion = false)
        } else if (userScore == oppScore) {
            ChampionsLeagueEngine.prizeForChampionsStage(ChampionsLeaguePhase.LEAGUE_PHASE, isWin = false, isChampion = false)
        } else 300_000L

        val updatedClubs = afterMatchClubs.map { c ->
            if (c.id == userClub.id) c.copy(budget = c.budget + prize) else c
        }
        val updatedUser = updatedClubs.first { it.id == userClub.id }
        val updatedCareer = career.copy(
            budget = updatedUser.budget,
            boardConfidence = (career.boardConfidence + if (userScore > oppScore) 3L else if (userScore == oppScore) 1L else -2L).coerceIn(10L, 100L)
        )
        val uclNews = NewsGenerator.buildCupNews(
            round = career.currentRound.toInt(),
            cupTitle = "UEFA Champions League",
            stageLabel = live.cupStageLabel ?: "Fase de Liga",
            userClub = updatedUser,
            homeShort = live.userMatch.homeShort,
            homeScore = live.userMatch.homeScore,
            awayShort = live.userMatch.awayShort,
            awayScore = live.userMatch.awayScore,
            shootoutSummary = null,
            userWon = userScore >= oppScore,
            isChampion = false
        )
        val activeVoteId = if (_state.value.userBallonDorVoteSeason == career.seasonYear) _state.value.userBallonDorPlayerId else null
        val liveRace = BallonDorEngine.computeLiveCandidates(updatedClubs, resolvedUcl, userVotedPlayerId = activeVoteId)
        val monthIdx = FinanceEngine.monthIndexForRound(career.currentRound.toInt(), updatedUser.division.totalRounds)
        val gate = if (live.userIsHome) live.ticketRevenue else 0L

        _state.update { state ->
            val prevEntry = state.monthlyLedger[monthIdx] ?: MonthlyLedgerRecord(monthIndex = monthIdx)
            val newLedger = state.monthlyLedger + (
                monthIdx to prevEntry.copy(
                    ticketIncome = prevEntry.ticketIncome + gate,
                    otherDelta = prevEntry.otherDelta + prize,
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
                championsLeague = resolvedUcl,
                ballonDorContenders = liveRace,
                newsItems = (listOf(uclNews) + state.newsItems).take(60),
                monthlyLedger = newLedger,
                monthlyFinances = updatedTrends,
                seasonTicketRevenue = state.seasonTicketRevenue + gate,
                snackMessage = "Champions League (${live.cupStageLabel}): prêmio de ${formatCurrency(prize)}!"
            )
        }
        persistCareerUpdate(updatedCareer)
    }

    private fun finishContinentalGroupMatch(live: LiveMatchState) {
        val career = _state.value.activeCareer ?: return
        val userClub = _state.value.userClub ?: return
        val cupType = live.continentalCupType ?: return
        val fixtureId = live.continentalFixtureId ?: return
        val currentTournament = _state.value.continentalTournaments.firstOrNull { it.cupType == cupType } ?: return

        val afterMatchClubs = SeasonEngine.applyCupMatchResults(
            clubs = _state.value.clubs,
            liveState = live,
            userClubId = userClub.id,
            trainingFocus = _state.value.trainingFocus,
            marking = _state.value.marking,
            seasonYear = career.seasonYear
        )
        val clubMap = afterMatchClubs.associateBy { it.id }
        val resolvedTournament = ContinentalGroupEngine.resolveGroupMatchday(
            tournament = currentTournament,
            userFixtureId = fixtureId,
            userHomeScore = live.userMatch.homeScore,
            userAwayScore = live.userMatch.awayScore,
            liveOtherMatches = live.otherMatches,
            clubMap = clubMap
        )
        val userScore = if (live.userIsHome) live.userMatch.homeScore else live.userMatch.awayScore
        val oppScore = if (live.userIsHome) live.userMatch.awayScore else live.userMatch.homeScore
        val prize = if (userScore > oppScore) cupType.basePrizePerWin else if (userScore == oppScore) cupType.basePrizePerWin / 3 else 250_000L

        val updatedClubs = afterMatchClubs.map { c ->
            if (c.id == userClub.id) c.copy(budget = c.budget + prize) else c
        }
        val updatedUser = updatedClubs.first { it.id == userClub.id }
        val updatedCareer = career.copy(
            budget = updatedUser.budget,
            boardConfidence = (career.boardConfidence + if (userScore > oppScore) 3L else if (userScore == oppScore) 1L else -2L).coerceIn(10L, 100L)
        )
        val contNews = NewsGenerator.buildCupNews(
            round = career.currentRound.toInt(),
            cupTitle = cupType.title,
            stageLabel = live.cupStageLabel ?: "Fase de Grupos",
            userClub = updatedUser,
            homeShort = live.userMatch.homeShort,
            homeScore = live.userMatch.homeScore,
            awayShort = live.userMatch.awayShort,
            awayScore = live.userMatch.awayScore,
            shootoutSummary = null,
            userWon = userScore >= oppScore,
            isChampion = false
        )
        val monthIdx = FinanceEngine.monthIndexForRound(career.currentRound.toInt(), updatedUser.division.totalRounds)
        val gate = if (live.userIsHome) live.ticketRevenue else 0L

        _state.update { state ->
            val prevEntry = state.monthlyLedger[monthIdx] ?: MonthlyLedgerRecord(monthIndex = monthIdx)
            val newLedger = state.monthlyLedger + (
                monthIdx to prevEntry.copy(
                    ticketIncome = prevEntry.ticketIncome + gate,
                    otherDelta = prevEntry.otherDelta + prize,
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
                continentalTournaments = state.continentalTournaments.map {
                    if (it.cupType == cupType) resolvedTournament else it
                },
                newsItems = (listOf(contNews) + state.newsItems).take(60),
                monthlyLedger = newLedger,
                monthlyFinances = updatedTrends,
                seasonTicketRevenue = state.seasonTicketRevenue + gate,
                snackMessage = "${cupType.title} (${live.cupStageLabel}): prêmio de ${formatCurrency(prize)}!"
            )
        }
        persistCareerUpdate(updatedCareer)
    }

    private fun finishCupMatch(live: LiveMatchState) {
        val career = _state.value.activeCareer ?: return
        val userClub = _state.value.userClub ?: return
        val cupTieId = live.cupTieId ?: return

        val afterMatchClubs = SeasonEngine.applyCupMatchResults(
            clubs = _state.value.clubs,
            liveState = live,
            userClubId = userClub.id,
            trainingFocus = _state.value.trainingFocus,
            marking = _state.value.marking,
            seasonYear = career.seasonYear
        )
        val clubMap = afterMatchClubs.associateBy { it.id }

        // Branch 1: Continental Group Tournament Knockout (Libertadores / Sul-Americana)
        val contCupType = live.continentalCupType
        if (contCupType != null) {
            val targetCont = _state.value.continentalTournaments.firstOrNull { it.cupType == contCupType } ?: return
            val resolvedCont = ContinentalGroupEngine.resolveKnockoutStage(
                tournament = targetCont,
                userTieId = cupTieId,
                userHomeScore = live.userMatch.homeScore,
                userAwayScore = live.userMatch.awayScore,
                userShootout = live.penaltyShootout,
                liveOtherMatches = live.otherMatches,
                clubMap = clubMap
            )
            val resolvedTie = resolvedCont.knockoutTies.firstOrNull { it.id == cupTieId }
            val userWon = resolvedTie?.winnerClubId == userClub.id
            val isChampion = resolvedCont.championClubId == userClub.id
            val prize = when {
                isChampion -> contCupType.titlePrize
                userWon -> contCupType.basePrizePerWin * (targetCont.currentKnockoutStageIndex + 2)
                else -> 400_000L
            }
            val totalBonus = prize + if (isChampion) (_state.value.activeSponsor?.titleBonus ?: 0L) else 0L
            val updatedClubs = afterMatchClubs.map { c -> if (c.id == userClub.id) c.copy(budget = c.budget + totalBonus) else c }
            val updatedUser = updatedClubs.first { it.id == userClub.id }
            val confDelta = when {
                isChampion -> 16L
                userWon -> 6L
                else -> -4L
            }
            val updatedCareer = career.copy(
                budget = updatedUser.budget,
                boardConfidence = (career.boardConfidence + confDelta).coerceIn(10L, 100L),
                copaStage = if (isChampion) "Campeão ${contCupType.title} 🏆" else career.copaStage
            )
            val cupNews = NewsGenerator.buildCupNews(
                round = career.currentRound.toInt(),
                cupTitle = contCupType.title,
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
                    continentalTournaments = state.continentalTournaments.map {
                        if (it.cupType == contCupType) resolvedCont else it
                    },
                    newsItems = (listOf(cupNews) + state.newsItems).take(60),
                    monthlyLedger = newLedger,
                    monthlyFinances = updatedTrends,
                    seasonTicketRevenue = state.seasonTicketRevenue + cupGate,
                    snackMessage = when {
                        isChampion -> "🏆 CAMPEÃO DA ${contCupType.title.uppercase()}! (+${formatCurrency(totalBonus)})"
                        userWon -> "Classificado na ${contCupType.title}! (+${formatCurrency(totalBonus)})"
                        else -> "Eliminado da ${contCupType.title}."
                    }
                )
            }
            persistCareerUpdate(updatedCareer)
            if (isChampion) {
                recordCupTrophy(career, updatedUser.name, contCupType.title)
            }
            return
        }

        // Branch 2: UEFA Champions League Knockout
        val cupType = live.cupType ?: return
        if (cupType == CupType.CHAMPIONS) {
            val currentUcl = _state.value.championsLeague ?: return
            val resolvedUcl = ChampionsLeagueEngine.resolveKnockoutStage(
                ucl = currentUcl,
                userTieId = cupTieId,
                userHomeScore = live.userMatch.homeScore,
                userAwayScore = live.userMatch.awayScore,
                userShootout = live.penaltyShootout,
                liveOtherMatches = live.otherMatches,
                clubMap = clubMap
            )
            val resolvedTie = resolvedUcl.knockoutTies.firstOrNull { it.id == cupTieId }
            val userWon = resolvedTie?.winnerClubId == userClub.id
            val isChampion = resolvedUcl.championClubId == userClub.id
            val prize = if (userWon) {
                ChampionsLeagueEngine.prizeForChampionsStage(currentUcl.phase, isWin = true, isChampion = isChampion)
            } else 1_200_000L
            val totalBonus = prize + if (isChampion) (_state.value.activeSponsor?.titleBonus ?: 0L) else 0L
            val updatedClubs = afterMatchClubs.map { c -> if (c.id == userClub.id) c.copy(budget = c.budget + totalBonus) else c }
            val updatedUser = updatedClubs.first { it.id == userClub.id }
            val confDelta = when {
                isChampion -> 18L
                userWon -> 6L
                else -> -3L
            }
            val updatedCareer = career.copy(
                budget = updatedUser.budget,
                boardConfidence = (career.boardConfidence + confDelta).coerceIn(10L, 100L),
                copaStage = if (isChampion) "Campeão UCL 🏆" else career.copaStage
            )
            val uclNews = NewsGenerator.buildCupNews(
                round = career.currentRound.toInt(),
                cupTitle = "UEFA Champions League",
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
            val activeVoteId = if (_state.value.userBallonDorVoteSeason == career.seasonYear) _state.value.userBallonDorPlayerId else null
            val liveRace = BallonDorEngine.computeLiveCandidates(updatedClubs, resolvedUcl, userVotedPlayerId = activeVoteId)
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
                    championsLeague = resolvedUcl,
                    ballonDorContenders = liveRace,
                    newsItems = (listOf(uclNews) + state.newsItems).take(60),
                    monthlyLedger = newLedger,
                    monthlyFinances = updatedTrends,
                    seasonTicketRevenue = state.seasonTicketRevenue + cupGate,
                    snackMessage = when {
                        isChampion -> "🏆 CAMPEÃO DA CHAMPIONS LEAGUE! (+${formatCurrency(totalBonus)})"
                        userWon -> "Avançou na Champions League! (+${formatCurrency(totalBonus)})"
                        else -> "Eliminado da UEFA Champions League."
                    }
                )
            }
            persistCareerUpdate(updatedCareer)
            if (isChampion) {
                recordCupTrophy(career, updatedUser.name, "UEFA Champions League")
            }
            return
        }

        // Branch 3: Standard Domestic Cups (Copa do Brasil)
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
            recordCupTrophy(career, updatedUser.name, cupType.title)
        }
    }

    private fun recordCupTrophy(career: CareerSave, clubName: String, competitionName: String) {
        viewModelScope.launch {
            careerRepo.addTrophy(
                TrophyRecord(
                    userId = currentUserId,
                    careerId = career.id,
                    competitionName = competitionName,
                    clubName = clubName,
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

    fun startNextSeason() {
        val career = _state.value.activeCareer ?: return
        val nextClubs = SeasonEngine.advanceToNextSeason(_state.value.clubs)
        val userClub = nextClubs.first { it.id == career.clubId }
        val allFixtures = SeasonEngine.generateAllFixtures(nextClubs)
        val nextCups = CupAndCareerEngine.buildInitialCups(userClub, nextClubs)
            .filterNot { it.cupType == CupType.CHAMPIONS || it.cupType == CupType.LIBERTADORES }
        val nextUcl = ChampionsLeagueEngine.buildInitialChampionsLeague(userClub, nextClubs)
        val nextContinental = ContinentalGroupEngine.buildInitialContinentalTournaments(userClub, nextClubs)
        val nextRace = BallonDorEngine.computeLiveCandidates(nextClubs, nextUcl)
        val nextSeasonYear = career.seasonYear + 1L
        val prevGov = _state.value.governance
        val electionDue = nextSeasonYear >= prevGov.nextElectionYear && prevGov.lastElectionYear != nextSeasonYear
        val nextGov = GovernanceAndFfpEngine.buildInitialGovernance(userClub, nextSeasonYear).copy(
            presidentName = prevGov.presidentName,
            presidentStyle = prevGov.presidentStyle,
            nextElectionYear = prevGov.nextElectionYear,
            lastElectionYear = prevGov.lastElectionYear,
            electionPending = prevGov.electionPending || electionDue,
            lastElectionSummary = prevGov.lastElectionSummary
        )
        val nextFfp = FairPlayState(
            allowedDeficitLimit = (userClub.prestige * 5_000_000L).coerceAtLeast(8_000_000L),
            transferBanRounds = _state.value.fairPlay.transferBanRounds,
            activePunishmentNote = _state.value.fairPlay.activePunishmentNote
        )
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
                championsLeague = nextUcl,
                continentalTournaments = nextContinental,
                ballonDorContenders = nextRace,
                userBallonDorPlayerId = null,
                userBallonDorVoteSeason = 0L,
                fairPlay = nextFfp,
                governance = nextGov,
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

    private fun buildCurrentSnapshot(): SeasonSnapshot? {
        val state = _state.value
        if (state.clubs.isEmpty() || state.fixtures.isEmpty()) return null
        return SeasonSnapshot(
            clubs = state.clubs,
            fixtures = state.fixtures,
            monthlyLedger = state.monthlyLedger,
            seasonTicketRevenue = state.seasonTicketRevenue,
            cups = state.cups,
            activeSponsor = state.activeSponsor,
            youthProspects = state.youthProspects,
            pendingJobOffers = state.jobOffers,
            isSacked = state.isSacked,
            championsLeague = state.championsLeague,
            annualAwardsHistory = state.annualAwardsHistory,
            continentalTournaments = state.continentalTournaments,
            fairPlay = state.fairPlay,
            governance = state.governance,
            userBallonDorPlayerId = state.userBallonDorPlayerId,
            userBallonDorVoteSeason = state.userBallonDorVoteSeason
        )
    }

    private fun persistCareerUpdate(career: CareerSave) {
        if (career.id.isBlank()) return
        val snapshot = buildCurrentSnapshot()
        viewModelScope.launch {
            withContext(NonCancellable) {
                careerRepo.updateCareer(career)
                    .onFailure { e ->
                        Log.w(TAG, "Falha ao salvar carreira localmente: ${e.message}")
                    }
                if (snapshot != null) {
                    careerRepo.saveSeasonSnapshot(career.id, snapshot)
                        .onFailure { e ->
                            Log.w(TAG, "Falha ao salvar snapshot completo: ${e.message}")
                        }
                }
            }
            _state.update { it.copy(lastSavedAtMs = System.currentTimeMillis()) }
        }
    }

    private fun persistFullSnapshot(careerId: String) {
        val snapshot = buildCurrentSnapshot() ?: return
        if (careerId.isBlank()) return
        viewModelScope.launch {
            withContext(NonCancellable) {
                careerRepo.saveSeasonSnapshot(careerId, snapshot)
                    .onFailure { e ->
                        Log.w(TAG, "Falha ao salvar snapshot completo: ${e.message}")
                    }
            }
            _state.update { it.copy(lastSavedAtMs = System.currentTimeMillis()) }
        }
    }

    private companion object {
        const val TAG = "BrasManagerVM"
    }
}

