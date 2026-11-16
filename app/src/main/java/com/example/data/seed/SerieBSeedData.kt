package com.example.data.seed

import com.example.domain.model.Club
import com.example.domain.model.Division
import com.example.domain.model.Player
import com.example.domain.model.PlayerPosition.ATA
import com.example.domain.model.PlayerPosition.GOL
import com.example.domain.model.PlayerPosition.LAT
import com.example.domain.model.PlayerPosition.MEI
import com.example.domain.model.PlayerPosition.ZAG

object SerieBSeedData {
    fun clubs(): List<Club> = listOf(
        makeSerieBClub("athletico_pr", "Athletico-PR", "CAP", 0xFFC8102EL, 0xFF141414L, "Ligga Arena", 42372, 4, 48_000_000L, "3444",
            listOf("Mycael", "Léo Godoy", "Tobias Figueiredo", "Léo Pelé", "Esquivel", "Felipinho", "Raul", "Bruno Zapelli", "Cuello", "Luiz Fernando", "Alan Kardec", "Santos", "Dudu", "Belezi", "Falcão", "João Cruz")),
        makeSerieBClub("criciuma", "Criciúma", "CRI", 0xFFFACC15L, 0xFF111111L, "Heriberto Hülse", 19300, 3, 28_000_000L, "6268",
            listOf("Kauã", "Jonathan", "Rodrigo", "Luciano Castán", "Marcelo Hermes", "Morelli", "Fellipe Mateus", "Matheus Trindade", "Popó", "Talisson", "Neto Pessoa", "Caíque", "Léo Alaba", "W. Angel", "Juninho", "Werik Popó")),
        makeSerieBClub("goias", "Goiás", "GOI", 0xFF006B3FL, 0xFFFFFFFFL, "Serrinha", 14525, 3, 27_000_000L, "3395",
            listOf("Tadeu", "Diego Caito", "Lucas Ribeiro", "Messias", "Lucas Lovat", "Marcão", "Juninho", "Rafael Gava", "Welliton", "Pedrinho", "Edu", "Thiago Rodrigues", "Willean Lepo", "Luiz Felipe", "Gonzalo Freitas", "Arthur Caíke")),
        makeSerieBClub("coritiba", "Coritiba", "CFC", 0xFF00573FL, 0xFFFFFFFFL, "Couto Pereira", 40502, 3, 34_000_000L, "3456",
            listOf("Pedro Morisco", "Rafinha", "Maicon", "Bruno Melo", "Jamerson", "Sebastián Gómez", "Filipe Machado", "Josué", "Lucas Ronier", "Everaldo", "Dellatorre", "Gabriel Vasconcelos", "Felipe Guimarães", "Thalisson", "Geovane Meurer", "Coutinho")),
        makeSerieBClub("america_mg", "América-MG", "AME", 0xFF006B3FL, 0xFF141414L, "Independência", 23018, 3, 26_000_000L, "6154",
            listOf("Matheus Mendes", "Mariano", "Lucão", "Ricardo Silva", "Marlon", "Alê", "Cauã Barros", "Benítez", "Fabinho", "Adyson", "Jonathas", "Jori", "Júlio", "Pedro Barcelos", "Miqueias", "Renato Marques")),
        makeSerieBClub("cuiaba", "Cuiabá", "CUI", 0xFF046A38L, 0xFFFACC15L, "Arena Pantanal", 42968, 3, 30_000_000L, "9970",
            listOf("Mateus Pasinato", "Mateusinho", "Alan Empereur", "Bruno Alves", "Sander", "Lucas Mineiro", "Denilson", "Max", "Pedrinho", "Derik Lacerda", "Clayson", "Arthur Bittencourt", "Railan", "Nathan", "Calebe", "Jadson")),
        makeSerieBClub("atletico_go", "Atlético-GO", "ACG", 0xFFDC2626L, 0xFF141414L, "Antônio Accioly", 12500, 3, 25_000_000L, "10513",
            listOf("Ronaldo", "Marcinho", "Alix", "Pedro Henrique", "Guilherme Romão", "Rhaldney", "Willian Maranhão", "Shaylon", "Marcelinho", "Alejo Cruz", "Caio Dantas", "Anderson", "Raí Ramos", "Matheus Felipe", "Kauan", "Sandro Lima")),
        makeSerieBClub("novorizontino", "Novorizontino", "NOV", 0xFFEAB308L, 0xFF181818L, "Jorjão", 12398, 3, 22_000_000L, "19773",
            listOf("Jordi", "Rodrigo Soares", "Rafael Donato", "Patrick", "Reverson", "Luís Oyama", "Jean Irmer", "Marlon", "Waguininho", "Airton", "Pablo Dyego", "Airton", "Igor Formiga", "Dantas", "Fábio Matheus", "Lucca")),
        makeSerieBClub("vila_nova", "Vila Nova", "VIL", 0xFFB91C1CL, 0xFFFFFFFFL, "Onésio Brasileiro", 11788, 2, 18_000_000L, "10515",
            listOf("Halls", "Elias", "Tiago Pagnussat", "Bernardo Schappo", "Willian Formiga", "Ralf", "Arilson", "Igor Henrique", "Emerson Urso", "Júnior Todinho", "Gabriel Poveda", "Kozlinski", "Igor Inocêncio", "Walisson Maia", "João Vieira", "Bruno Mendes")),
        makeSerieBClub("operario_pr", "Operário-PR", "OPE", 0xFF181818L, 0xFFFFFFFFL, "Germano Krüger", 10632, 2, 16_000_000L, "19774",
            listOf("Elias", "Diogo Mateus", "Joseph", "Allan Godói", "Gabriel Feliciano", "Jacy", "Índio", "Boschilia", "Rodrigo Rodrigues", "Allano", "Mingotti", "Lucas Wingert", "Thales Oleques", "Cristiano", "Neto Paraíba", "Kleiton")),
        makeSerieBClub("avai", "Avaí", "AVA", 0xFF005CA9L, 0xFFFFFFFFL, "Ressacada", 17800, 3, 21_000_000L, "9966",
            listOf("César Augusto", "Marcos Vinícius", "Eduardo Brock", "Jonathan Costa", "Mário Sérgio", "Zé Ricardo", "João Vitor", "Giovanni", "Hygor", "Alef Manga", "Vagner Love", "Igor Bohn", "Pedrão", "Wanderson", "Marquinhos Gabriel", "Gaspar")),
        makeSerieBClub("crb", "CRB", "CRB", 0xFFB91C1CL, 0xFFFFFFFFL, "Rei Pelé", 17126, 2, 17_000_000L, "10514",
            listOf("Matheus Albino", "Hereda", "Segovia", "Miranda", "Willian Bahia", "Nathan Melo", "Higor Meritão", "Gegê", "Léo Pereira", "David da Hora", "Anselmo Ramon", "Vitor Caetano", "Hayner", "Yan Souto", "Danielzinho", "Rafinha")),
        makeSerieBClub("paysandu", "Paysandu", "PAY", 0xFF0284C7L, 0xFFFFFFFFL, "Curuzu", 16200, 2, 16_500_000L, "6271",
            listOf("Matheus Nogueira", "Edilson", "Quintana", "Luan Freitas", "Kevyn", "Leandro Vilela", "Matheus Vargas", "Giovanni", "Rossi", "Marlon", "Nicolas", "Alisson", "Bryan Borges", "Yeferson", "PK", "Borasi")),
        makeSerieBClub("remo", "Remo", "REM", 0xFF0F2C59L, 0xFFFFFFFFL, "Mangueirão", 53635, 2, 16_000_000L, "10516",
            listOf("Marcelo Rangel", "Marcelinho", "Klaus", "Reynaldo", "Sávio", "Caio Vinícius", "Jaderson", "Dodô", "Janderson", "Pedro Rocha", "Ytalo", "Léo Lang", "Kadu", "Rafael Castro", "Pavani", "Maxwell")),
        makeSerieBClub("chapecoense", "Chapecoense", "CHA", 0xFF007A33L, 0xFFFFFFFFL, "Arena Condá", 20089, 2, 15_500_000L, "18061",
            listOf("Léo Vieira", "Maílton", "Bruno Leonardo", "Eduardo Doma", "Walter Clar", "Bruno Matias", "Jiménez", "Giovanni Augusto", "Marcinho", "Dentinho", "Mário Sérgio", "João Paulo", "Gabriel Inocêncio", "Victor Caetano", "Carvalheira", "Ítalo")),
        makeSerieBClub("amazonas", "Amazonas", "AMA", 0xFFD97706L, 0xFF141414L, "Arena da Amazônia", 44000, 2, 15_000_000L, "21501",
            listOf("Renan", "Akapo", "Wellington Nascimento", "Fabiano", "Sidcley", "Larry Vásquez", "Eliton Júnior", "Rafael Tavares", "Sassá", "Luan Silva", "Gustavo Ermel", "João Lopes", "Tiago Cametá", "Alvariño", "Diego Torres", "Bruno Lopes")),
        makeSerieBClub("botafogo_sp", "Botafogo-SP", "BSP", 0xFFDC2626L, 0xFF111111L, "Santa Cruz", 29292, 2, 14_500_000L, "19771",
            listOf("João Carlos", "Wallison", "Alisson Cassiano", "Edson", "Jean Victor", "Sabit", "Leandro Maciel", "Dramisino", "Jonathan Cafú", "Douglas Baggio", "Alexandre Jesus", "Victor Souza", "Ericson", "Rafael Milhorim", "Alejo", "Pablo Thomaz")),
        makeSerieBClub("volta_redonda", "Volta Redonda", "VRE", 0xFFFACC15L, 0xFF141414L, "Raulino de Oliveira", 20255, 2, 12_500_000L, "21502",
            listOf("Jean Drosny", "Wellington Silva", "Gabriel Bahia", "Fabrício", "Sánchez", "Pierre", "Robinho", "Chay", "MV", "Mirandinha", "Bruno Santos", "Felipe Avelino", "Jhonny", "Lucas Tocantins", "Luciano Naninho", "Hyuri")),
        makeSerieBClub("ferroviaria", "Ferroviária", "FER", 0xFF7F1D1DL, 0xFFFFFFFFL, "Fonte Luminosa", 21441, 2, 13_000_000L, "21503",
            listOf("Dênis Júnior", "Lucas Rodrigues", "Gustavo Medina", "Ronaldo Alves", "Zé Mário", "Ricardinho", "Tárik", "Alencar", "Juninho", "Thayllon", "Carlão", "Léo Wall", "Kevin", "Jackson", "Ian Luccas", "Quirino")),
        makeSerieBClub("athletic_mg", "Athletic-MG", "ATH", 0xFF181818L, 0xFFFFFFFFL, "Arena Sicredi", 6000, 2, 12_000_000L, "21504",
            listOf("Diego Vitor", "Wesley Gasolina", "Sidimar", "Edson", "Yuri", "Diego Fumaça", "Sandry", "David Braga", "Welinton Torrão", "Neto Costa", "Lincoln", "Jefferson", "Douglas Pelé", "Jhonatan", "Amorim", "Max"))
    )

    private fun makeSerieBClub(
        id: String,
        name: String,
        shortName: String,
        primaryHex: Long,
        secondaryHex: Long,
        stadium: String,
        capacity: Int,
        prestige: Int,
        budget: Long,
        espnId: String,
        names: List<String>
    ): Club {
        val positions = listOf(
            GOL, LAT, ZAG, ZAG, LAT, MEI, MEI, MEI, ATA, ATA, ATA,
            GOL, LAT, ZAG, MEI, ATA
        )
        val baseOvr = if (prestige >= 4) 74 else if (prestige == 3) 71 else 68
        val specs = names.take(16).mapIndexed { idx, playerName ->
            val ovr = (baseOvr - (idx / 5) + if (idx == 7 || idx == 10) 2 else 0).coerceIn(63, 77)
            val age = 21 + ((idx * 3) % 13)
            PlayerSpec(
                name = playerName,
                pos = positions[idx % positions.size],
                ovr = ovr,
                age = age,
                shirt = idx + 1
            )
        }
        return buildClub(
            id = id,
            name = name,
            shortName = shortName,
            division = Division.SERIE_B,
            primaryHex = primaryHex,
            secondaryHex = secondaryHex,
            stadium = stadium,
            capacity = capacity,
            prestige = prestige,
            budget = budget,
            espnId = espnId,
            specs = specs
        )
    }

    fun freeAgents(): List<Player> = listOf(
        makePlayer("fa", 1, "Diego Costa", ATA, 75, 37, 19, "Espanha"),
        makePlayer("fa", 2, "Willian", MEI, 77, 37, 88),
        makePlayer("fa", 3, "Mariano", LAT, 73, 39, 2),
        makePlayer("fa", 4, "Renato Augusto", MEI, 75, 37, 8),
        makePlayer("fa", 5, "Léo Duarte", ZAG, 74, 29, 43),
        makePlayer("fa", 6, "Matheus Babi", ATA, 72, 28, 9),
        makePlayer("fa", 7, "Lucas Evangelista", MEI, 76, 30, 8),
        makePlayer("fa", 8, "Rafael Tolói", ZAG, 77, 35, 2, "Itália"),
        makePlayer("fa", 9, "Marcelo Grohe", GOL, 76, 38, 1),
        makePlayer("fa", 10, "Kauan Basile", MEI, 71, 18, 10),
        makePlayer("fa", 11, "Gabriel Veron", ATA, 74, 23, 7),
        makePlayer("fa", 12, "Jean Pyerre", MEI, 72, 27, 21)
    )
}
