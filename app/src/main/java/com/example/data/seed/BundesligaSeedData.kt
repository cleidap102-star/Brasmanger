package com.example.data.seed

import com.example.domain.model.Club
import com.example.domain.model.Division

object BundesligaSeedData {
    fun clubs(): List<Club> = listOf(
        makeBL("bayern", "Bayern de Munique", "BAY", 0xFFDC052DL, 0xFFFFFFFFL, "Allianz Arena", 75000, 5, 340_000_000L, "132", 85,
            listOf("Neuer", "Guerreiro", "Upamecano", "Kim Min-jae", "Alphonso Davies", "Kimmich", "Pavlovic", "Jamal Musiala", "Michael Olise", "Harry Kane", "Coman", "Ulreich", "Laimer", "Goretzka", "Sané", "Gnabry")),
        makeBL("leverkusen", "Bayer Leverkusen", "B04", 0xFFE32221L, 0xFF111111L, "BayArena", 30210, 5, 220_000_000L, "131", 83,
            listOf("Hradecky", "Frimpong", "Tapsoba", "Jonathan Tah", "Hincapié", "Grimaldo", "Granit Xhaka", "Andrich", "Florian Wirtz", "Boniface", "Schick", "Kovar", "Mukiele", "Aleix García", "Palacios", "Terrier")),
        makeBL("eintracht", "Eintracht Frankfurt", "SGE", 0xFFE1000FL, 0xFF111111L, "Deutsche Bank Park", 58000, 4, 145_000_000L, "125", 79,
            listOf("Trapp", "Kristensen", "Tuta", "Koch", "Theate", "Skhiri", "Larsson", "Götze", "Chaïbi", "Marmoush", "Ekitiké", "Santos", "Nkounkou", "Dahoud", "Knauff", "Matanovic")),
        makeBL("rb_leipzig", "RB Leipzig", "RBL", 0xFFDD0741L, 0xFFFFFFFFL, "Red Bull Arena", 47069, 4, 200_000_000L, "11420", 80,
            listOf("Gulácsi", "Henrichs", "Orbán", "Lukeba", "Raum", "Kampl", "Haidara", "Xavi Simons", "Baumgartner", "Sesko", "Openda", "Vandevoordt", "Geertruida", "Vermeeren", "Nusa", "Poulsen")),
        makeBL("dortmund", "Borussia Dortmund", "BVB", 0xFFFDE100L, 0xFF111111L, "Signal Iduna Park", 81365, 5, 215_000_000L, "124", 81,
            listOf("Kobel", "Ryerson", "Anton", "Schlotterbeck", "Bensebaini", "Can", "Gross", "Brandt", "Adeyemi", "Guirassy", "Gittens", "Meyer", "Süle", "Nmecha", "Sabitzer", "Beier")),
        makeBL("stuttgart", "VfB Stuttgart", "VFB", 0xFFE32219L, 0xFFFFFFFFL, "MHPArena", 60058, 4, 130_000_000L, "134", 78,
            listOf("Nübel", "Vagnoman", "Rouault", "Chabot", "Mittelstädt", "Karazor", "Stiller", "Millot", "Leweling", "Undav", "Demirovic", "Bredlow", "Chase", "Hendriks", "Führich", "Touré")),
        makeBL("freiburg", "Freiburg", "SCF", 0xFF181818L, 0xFFDC2626L, "Europa-Park", 34700, 3, 95_000_000L, "126", 76,
            listOf("Atubolu", "Kübler", "Ginter", "Lienhart", "Günter", "Eggestein", "Osterhage", "Doan", "Grifo", "Höler", "Adamu", "Müller", "Rosenfelder", "Höfler", "Dinkçi", "Gregoritsch")),
        makeBL("mainz", "Mainz 05", "M05", 0xFFC3141EL, 0xFFFFFFFFL, "MEWA Arena", 33305, 3, 78_000_000L, "2950", 75,
            listOf("Zentner", "Da Costa", "Bell", "Kohr", "Mwene", "Sano", "Amiri", "Caci", "Nebel", "Burkardt", "Lee Jae-sung", "Riess", "Leitsch", "Hanche-Olsen", "Hong", "Sieb")),
        makeBL("gladbach", "B. Mönchengladbach", "BMG", 0xFF181818L, 0xFF16A34AL, "Borussia-Park", 54042, 4, 98_000_000L, "268", 75,
            listOf("Nicolas", "Scally", "Itakura", "Elvedi", "Ullrich", "Weigl", "Reitz", "Stöger", "Honorat", "Kleindienst", "Plea", "Omlin", "Lainer", "Sander", "Hack", "Cvancara")),
        makeBL("werder_bremen", "Werder Bremen", "SVW", 0xFF1D9053L, 0xFFFFFFFFL, "Weserstadion", 42100, 3, 82_000_000L, "137", 75,
            listOf("Zetterer", "Weiser", "Stark", "Friedl", "Jung", "Lynen", "Stage", "Schmid", "Bittencourt", "Ducksch", "Njinmah", "Backhaus", "Pieper", "Köhn", "Grüll", "Topp")),
        makeBL("wolfsburg", "Wolfsburg", "WOB", 0xFF65B32EL, 0xFFFFFFFFL, "Volkswagen Arena", 28917, 4, 115_000_000L, "138", 76,
            listOf("Grabara", "Fischer", "Vavro", "Koulierakis", "Maehle", "Arnold", "Dardai", "Baku", "Wimmer", "Wind", "Amoura", "Müller", "Bornauw", "Gerhardt", "Majer", "Tiago Tomás")),
        makeBL("augsburg", "Augsburg", "FCA", 0xFFBA3733L, 0xFFFFFFFFL, "WWK Arena", 30660, 3, 68_000_000L, "3841", 73,
            listOf("Labrovic", "Wolf", "Gouweleeuw", "Matsima", "Giannoulis", "Onyeka", "Jakic", "Rexhbecaj", "Claude-Maurice", "Tietz", "Essende", "Dahmen", "Bauer", "Maier", "Vargas", "Mounié")),
        makeBL("union_berlin", "Union Berlin", "FCU", 0xFFEB1923L, 0xFFFFFFFFL, "An der Alten Försterei", 22012, 3, 76_000_000L, "598", 74,
            listOf("Rønnow", "Doekhi", "Vogt", "Leite", "Trimmel", "Khedira", "Kemlein", "Rothe", "Jeong", "Hollerbach", "Jordan", "Schwolow", "Querfeld", "Tousart", "Skarke", "Vertessen")),
        makeBL("st_pauli", "St. Pauli", "STP", 0xFF6E3B23L, 0xFFFFFFFFL, "Millerntor", 29546, 2, 55_000_000L, "270", 71,
            listOf("Vasilj", "Wahl", "Smith", "Mets", "Saliakas", "Irvine", "Boukhalfa", "Treu", "Afolayan", "Eggestein", "Guilavogui", "Voll", "Dzwigala", "Wagner", "Sinani", "Saad")),
        makeBL("hoffenheim", "Hoffenheim", "TSG", 0xFF1961B5L, 0xFFFFFFFFL, "PreZero Arena", 30150, 3, 90_000_000L, "7911", 74,
            listOf("Baumann", "Kaderábek", "Stach", "Chaves", "Prass", "Grillitsch", "Bischof", "Kramaric", "Hlozek", "Moerstedt", "Bülter", "Philipp", "Akpoguma", "Geiger", "Bruun Larsen", "Tabakovic")),
        makeBL("heidenheim", "Heidenheim", "HDH", 0xFFE2001AL, 0xFF003B79L, "Voith-Arena", 15000, 2, 54_000_000L, "6418", 72,
            listOf("Müller", "Traoré", "Mainka", "Gimber", "Föhrenbach", "Schöppner", "Dorsch", "Wanner", "Scienza", "Pieringer", "Conteh", "Eicher", "Siersleben", "Kerber", "Beck", "Kaufmann")),
        makeBL("bochum", "Bochum", "BOC", 0xFF005CA9L, 0xFFFFFFFFL, "Vonovia Ruhrstadion", 26000, 2, 48_000_000L, "121", 70,
            listOf("Drewes", "Passlack", "Ordets", "Medić", "Wittek", "Losilla", "Sissoko", "Bero", "De Wit", "Hofmann", "Broschinski", "Horn", "Oermann", "Miyoshi", "Daschner", "Boadu")),
        makeBL("holstein_kiel", "Holstein Kiel", "KIE", 0xFF005CA9L, 0xFFDC2626L, "Holstein-Stadion", 15034, 2, 45_000_000L, "7884", 70,
            listOf("Weiner", "Becker", "Erras", "Geschwill", "Porath", "Remberg", "Knudsen", "Gigovic", "Holtby", "Machino", "Harres", "Dähne", "Ivezic", "Puchacz", "Skrybski", "Arp"))
    )

    private fun makeBL(
        id: String, name: String, shortName: String,
        primaryHex: Long, secondaryHex: Long, stadium: String,
        capacity: Int, prestige: Int, budget: Long, espnId: String,
        baseOvr: Int, names: List<String>
    ): Club = makeLeagueClub(
        id = id, name = name, shortName = shortName, division = Division.BUNDESLIGA,
        primaryHex = primaryHex, secondaryHex = secondaryHex, stadium = stadium,
        capacity = capacity, prestige = prestige, budget = budget, espnId = espnId,
        names = names, baseOverall = baseOvr, maxOverall = 90, minOverall = 67
    )
}
