package com.example.data.seed

import com.example.domain.model.Club
import com.example.domain.model.Player
import com.example.domain.model.PlayerPosition

object LeagueCatalog {
    fun allClubs(): List<Club> =
        SerieASeedData.clubs() +
            SerieBSeedData.clubs() +
            SerieCSeedData.clubs() +
            SerieDSeedData.clubs() +
            PremierLeagueSeedData.clubs() +
            LaLigaSeedData.clubs() +
            SerieAItaSeedData.clubs() +
            BundesligaSeedData.clubs() +
            Ligue1SeedData.clubs() +
            LigaArgentinaSeedData.clubs()

    fun freeAgents(): List<Player> = SerieBSeedData.freeAgents() + listOf(
        makePlayer("fa", 13, "Keylor Navas", PlayerPosition.GOL, 78, 39, 1, "Costa Rica"),
        makePlayer("fa", 14, "Sergio Ramos", PlayerPosition.ZAG, 79, 39, 4, "Espanha"),
        makePlayer("fa", 15, "Dele Alli", PlayerPosition.MEI, 74, 29, 20, "Inglaterra"),
        makePlayer("fa", 16, "Wissam Ben Yedder", PlayerPosition.ATA, 77, 35, 10, "França"),
        makePlayer("fa", 17, "Mats Hummels", PlayerPosition.ZAG, 78, 37, 15, "Alemanha"),
        makePlayer("fa", 18, "Miralem Pjanic", PlayerPosition.MEI, 75, 35, 8, "Bósnia")
    )
}
