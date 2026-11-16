package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.CareerFirestoreRepository
import com.example.data.firebase.CareerSave
import com.example.data.firebase.ManagerProfile
import com.example.data.firebase.TransferRecord
import com.example.data.firebase.TrophyRecord
import com.example.data.local.SyncLogEntity
import com.example.data.remote.SquadSyncRepository
import com.example.data.seed.SerieASeedData
import com.example.data.seed.SerieBSeedData
import com.example.domain.engine.SeasonEngine
import com.example.domain.model.Club
import com.example.domain.model.Division
import com.example.domain.model.Fixture
import com.example.domain.model.Formation
import com.example.domain.model.IncomingTransferOffer
import com.example.domain.model.LiveMatchState
import com.example.domain.model.MarkingStyle
import com.example.domain.model.MatchEvent
import com.example.domain.model.MatchEventType
import com.example.domain.model.Mentality
import com.example.domain.model.Player
import com.example.domain.model.SquadSyncReport
import com.example.domain.model.TicketPriceLevel
import com.example.domain.model.TrainingFocus
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
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
}

class BrasManagerViewModel(
    private val firestoreRepo: CareerFirestoreRepository,
    private val syncRepo: SquadSyncRepository,
    private val currentUserId: String,
    private val initialDisplayName: String
) : ViewModel() {

    private val _state = MutableStateFlow(BrasManagerUiState())
    val uiState: StateFlow<BrasManagerUiState> = _state.asStateFlow()

    private val activeCareerIdFlow = MutableStateFlow<String?>(null)
    private var simTickerJob: Job? = null

    val catalogClubs: List<Club> by lazy {
        SerieASeedData.clubs() + SerieBSeedData.clubs()
    }

    init {
        bootstrap()
        observeCloudStreams()
    }

    private fun bootstrap() {
        viewModelScope.launch {
            firestoreRepo.ensureManagerProfile(initialDisplayName.ifBlank { "Técnico Brasileiro" })
                .onFailure { e ->
                    Log.w(TAG, "Erro ao garantir perfil do técnico: ${e.message}")
                }
            val seededClubs = syncRepo.applyCachedUpdates(catalogClubs)
            _state.update {
                it.copy(
                    isInitializing = false,
                    clubs = seededClubs,
                    freeAgents = SerieBSeedData.freeAgents()
                )
            }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun observeCloudStreams() {
        viewModelScope.launch {
            firestoreRepo.observeManagerProfile(currentUserId)
                .catch { error ->
                    val root = unwrapFirestoreError(error) ?: error
                    if (root is CancellationException) throw root
                    Log.w(TAG, "Erro ao observar perfil", root)
                }
                .collect { profile ->
                    _state.update { it.copy(profile = profile) }
                }
        }

        viewModelScope.launch {
            firestoreRepo.observeCareers(currentUserId)
                .catch { error ->
                    val root = unwrapFirestoreError(error) ?: error
                    if (root is CancellationException) throw root
                    Log.w(TAG, "Erro ao observar carreiras", root)
                    _state.update { it.copy(errorMessage = "Falha ao carregar carreiras: ${root.message}") }
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
                        firestoreRepo.observeTransfers(currentUserId, careerId),
                        firestoreRepo.observeTrophies(currentUserId, careerId)
                    ) { transfers, trophies -> transfers to trophies }
                }
            }.catch { error ->
                val root = unwrapFirestoreError(error) ?: error
                if (root is CancellationException) throw root
                Log.w(TAG, "Erro ao observar histórico da carreira", root)
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

            val result = firestoreRepo.createCareer(newCareer)
            result.onSuccess { careerId ->
                val fixturesA = SeasonEngine.generateLeagueFixtures(initializedClubs, Division.SERIE_A)
                val fixturesB = SeasonEngine.generateLeagueFixtures(initializedClubs, Division.SERIE_B)
                val saved = newCareer.copy(id = careerId)
                activeCareerIdFlow.value = careerId
                _state.update {
                    it.copy(
                        activeCareer = saved,
                        clubs = initializedClubs,
                        freeAgents = SerieBSeedData.freeAgents(),
                        fixtures = fixturesA + fixturesB,
                        liveMatch = null,
                        incomingOffers = emptyList(),
                        ticketPriceLevel = TicketPriceLevel.NORMAL,
                        trainingFocus = TrainingFocus.EQUILIBRADO,
                        formation = Formation.F_4_3_3,
                        mentality = Mentality.EQUILIBRADO,
                        marking = MarkingStyle.ZONAL,
                        seasonFinishedMessage = null,
                        snackMessage = "Carreira iniciada no comando do ${selectedClub.name}!"
                    )
                }
                if (_state.value.profile?.autoSyncEnabled != false) {
                    syncLiveSquads(automatic = true)
                }
            }.onFailure { e ->
                _state.update { it.copy(errorMessage = "Não foi possível salvar carreira: ${e.message}") }
            }
        }
    }

    fun resumeCareer(career: CareerSave) {
        viewModelScope.launch {
            val baseClubs = syncRepo.applyCachedUpdates(catalogClubs)
            val formation = Formation.fromCode(career.formation)
            val starterSet = career.activeStarterIds.toSet()

            // Reconstruct career points & budget onto user's club cleanly
            val restoredClubs = baseClubs.map { club ->
                if (club.id == career.clubId) {
                    val updatedPlayers = if (starterSet.isNotEmpty()) {
                        club.players.map { p -> p.copy(isStarter = p.id in starterSet) }
                    } else {
                        SeasonEngine.autoSelectBestLineup(club.players, formation)
                    }
                    club.copy(
                        division = if (career.division == Division.SERIE_B.code) Division.SERIE_B else Division.SERIE_A,
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

            val fixturesA = SeasonEngine.generateLeagueFixtures(restoredClubs, Division.SERIE_A)
            val fixturesB = SeasonEngine.generateLeagueFixtures(restoredClubs, Division.SERIE_B)
            activeCareerIdFlow.value = career.id

            _state.update {
                it.copy(
                    activeCareer = career,
                    clubs = restoredClubs,
                    freeAgents = SerieBSeedData.freeAgents(),
                    fixtures = fixturesA + fixturesB,
                    liveMatch = null,
                    ticketPriceLevel = TicketPriceLevel.fromCode(career.ticketPriceLevel),
                    trainingFocus = TrainingFocus.fromCode(career.trainingFocus),
                    formation = formation,
                    mentality = Mentality.fromCode(career.mentality),
                    marking = MarkingStyle.fromCode(career.marking),
                    seasonFinishedMessage = null,
                    snackMessage = "Bem-vindo de volta ao ${career.clubName}!"
                )
            }

            val lastSync = _state.value.profile?.lastSquadSyncAt ?: 0L
            val sixHoursMs = 6L * 60L * 60L * 1000L
            if (_state.value.profile?.autoSyncEnabled != false && System.currentTimeMillis() - lastSync > sixHoursMs) {
                syncLiveSquads(automatic = true)
            }
        }
    }

    fun exitToCareerHub() {
        simTickerJob?.cancel()
        activeCareerIdFlow.value = null
        _state.update { it.copy(activeCareer = null, liveMatch = null) }
    }

    fun deleteCareer(careerId: String) {
        viewModelScope.launch {
            firestoreRepo.deleteCareer(careerId)
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
            firestoreRepo.updateManagerSettings(
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

    fun syncLiveSquads(automatic: Boolean = false) {
        if (_state.value.isSyncing) return
        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true) }
            val currentClubs = _state.value.clubs.ifEmpty { catalogClubs }
            val (updatedClubs, report) = syncRepo.syncLiveRosters(
                currentClubs = currentClubs,
                preserveUserSignings = true
            )

            // Update profile last sync timestamp
            _state.value.profile?.let { profile ->
                firestoreRepo.updateManagerSettings(
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
                    firestoreRepo.addTransfer(
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
                it.copy(
                    clubs = updatedClubs,
                    lastSyncReport = report,
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
        _state.update { it.copy(ticketPriceLevel = level, activeCareer = updated) }
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
            state.copy(
                activeCareer = updatedCareer,
                clubs = state.clubs.map { if (it.id == userClub.id) updatedClub else it },
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
            state.copy(
                activeCareer = updatedCareer,
                clubs = state.clubs.map { if (it.id == userClub.id) updatedClub else it },
                snackMessage = "Empréstimo de R$ 10,0M creditado no caixa do clube."
            )
        }
        persistCareerUpdate(updatedCareer)
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

        _state.update { state ->
            state.copy(
                activeCareer = updatedCareer,
                clubs = state.clubs.map {
                    when (it.id) {
                        updatedUser.id -> updatedUser
                        updatedSeller.id -> updatedSeller
                        else -> it
                    }
                },
                snackMessage = "CONTRATADO! ${player.name} agora veste a camisa do ${userClub.name}!"
            )
        }
        persistCareerUpdate(updatedCareer)
        viewModelScope.launch {
            firestoreRepo.addTransfer(
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

        _state.update { state ->
            state.copy(
                activeCareer = updatedCareer,
                clubs = state.clubs.map { if (it.id == updatedUser.id) updatedUser else it },
                freeAgents = state.freeAgents.filterNot { it.id == player.id },
                snackMessage = "${player.name} assinou contrato sem custos de transferência!"
            )
        }
        persistCareerUpdate(updatedCareer)
        viewModelScope.launch {
            firestoreRepo.addTransfer(
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
        _state.update { state ->
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
                snackMessage = "Vendido! ${offer.player.name} transferido para ${offer.offeringClubName}."
            )
        }
        persistCareerUpdate(updatedCareer)
        viewModelScope.launch {
            firestoreRepo.addTransfer(
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
        val round = career.currentRound.toInt()
        if (round > 38) return

        // Ensure 11 available starters before kickoff
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
        val career = _state.value.activeCareer ?: return
        val userClub = _state.value.userClub ?: return

        val (updatedClubs, updatedFixtures) = SeasonEngine.applyRoundResults(
            clubs = _state.value.clubs,
            fixtures = _state.value.fixtures,
            liveState = live,
            userClubId = userClub.id,
            trainingFocus = _state.value.trainingFocus,
            marking = _state.value.marking
        )

        val updatedUser = updatedClubs.first { it.id == userClub.id }
        val table = SeasonEngine.sortStandings(updatedClubs, updatedUser.division)
        val newPos = (table.indexOfFirst { it.id == updatedUser.id } + 1).coerceIn(1, 20)

        val userScore = if (live.userIsHome) live.userMatch.homeScore else live.userMatch.awayScore
        val oppScore = if (live.userIsHome) live.userMatch.awayScore else live.userMatch.homeScore
        val confDelta = when {
            userScore > oppScore -> 4L
            userScore == oppScore -> 0L
            else -> -5L
        }
        val newRound = (career.currentRound + 1L).coerceAtMost(39L)
        val starterIds = updatedUser.players.filter { it.isStarter }.map { it.id }.take(11)

        // Copa do Brasil progressive milestones
        val newCopaStage = when (newRound.toInt()) {
            in 1..8 -> "3ª Fase"
            in 9..16 -> "Oitavas"
            in 17..24 -> "Quartas"
            in 25..31 -> "Semifinal"
            else -> if (newPos <= 6) "Final" else "Eliminado"
        }

        val updatedCareer = career.copy(
            currentRound = newRound,
            budget = updatedUser.budget,
            boardConfidence = (career.boardConfidence + confDelta).coerceIn(10L, 100L),
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
        val seasonDone = newRound > 38L
        val seasonSummary = if (seasonDone) {
            val divLabel = updatedUser.division.label
            when {
                newPos == 1 -> "CAMPEÃO! O ${updatedUser.name} levantou a taça do $divLabel!"
                updatedUser.division == Division.SERIE_A && newPos <= 4 ->
                    "Classificado para a Fase de Grupos da Libertadores (${newPos}º lugar)!"
                updatedUser.division == Division.SERIE_B && newPos <= 4 ->
                    "ACESSO GARANTIDO! O ${updatedUser.name} subiu para a Série A!"
                updatedUser.division == Division.SERIE_A && newPos >= 17 ->
                    "Rebaixado para a Série B (${newPos}º lugar). Hora de reconstruir!"
                else -> "Temporada ${career.seasonYear} encerrada em ${newPos}º lugar no $divLabel."
            }
        } else null

        _state.update {
            it.copy(
                activeCareer = updatedCareer,
                clubs = updatedClubs,
                fixtures = updatedFixtures,
                liveMatch = null,
                incomingOffers = newOffers,
                seasonFinishedMessage = seasonSummary
            )
        }
        persistCareerUpdate(updatedCareer)

        if (userScore > oppScore) {
            _state.value.profile?.let { profile ->
                viewModelScope.launch {
                    firestoreRepo.updateManagerSettings(
                        managerName = profile.managerName,
                        autoSyncEnabled = profile.autoSyncEnabled,
                        preferredSimSpeed = profile.preferredSimSpeed,
                        totalWinsDelta = 1L
                    )
                }
            }
        }

        // Award championship trophy if finished in 1st place
        if (seasonDone && newPos == 1) {
            viewModelScope.launch {
                firestoreRepo.addTrophy(
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
                    firestoreRepo.updateManagerSettings(
                        managerName = profile.managerName,
                        autoSyncEnabled = profile.autoSyncEnabled,
                        preferredSimSpeed = profile.preferredSimSpeed,
                        totalTitlesDelta = 1L
                    )
                }
            }
        }

        // Automatic mid-season squad sync at transfer window checkpoints
        val roundInt = newRound.toInt()
        if (!seasonDone && _state.value.profile?.autoSyncEnabled == true && roundInt in listOf(5, 14, 22, 30)) {
            syncLiveSquads(automatic = true)
        }
    }

    fun startNextSeason() {
        val career = _state.value.activeCareer ?: return
        val nextClubs = SeasonEngine.advanceToNextSeason(_state.value.clubs)
        val userClub = nextClubs.first { it.id == career.clubId }
        val fixturesA = SeasonEngine.generateLeagueFixtures(nextClubs, Division.SERIE_A)
        val fixturesB = SeasonEngine.generateLeagueFixtures(nextClubs, Division.SERIE_B)

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
            copaStage = "3ª Fase"
        )
        _state.update {
            it.copy(
                activeCareer = updatedCareer,
                clubs = nextClubs,
                fixtures = fixturesA + fixturesB,
                seasonFinishedMessage = null,
                snackMessage = "Temporada ${updatedCareer.seasonYear} iniciada na ${userClub.division.label}!"
            )
        }
        persistCareerUpdate(updatedCareer)
    }

    private fun persistCareerUpdate(career: CareerSave) {
        if (career.id.isBlank()) return
        viewModelScope.launch {
            firestoreRepo.updateCareer(career)
                .onFailure { e ->
                    Log.w(TAG, "Falha ao sincronizar carreira no Firestore: ${e.message}")
                }
        }
    }

    private fun unwrapFirestoreError(error: Throwable): Throwable? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    private companion object {
        const val TAG = "BrasManagerVM"
    }
}
