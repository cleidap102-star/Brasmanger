package com.example.data.seed

import com.example.domain.model.Club
import com.example.domain.model.Division

object Ligue1SeedData {
    fun clubs(): List<Club> = listOf(
        makeL1("psg", "Paris Saint-Germain", "PSG", 0xFF004170L, 0xFFDA291CL, "Parc des Princes", 47929, 5, 350_000_000L, "160", 84,
            listOf("Donnarumma", "Achraf Hakimi", "Marquinhos", "Pacho", "Nuno Mendes", "Vitinha", "João Neves", "Zaïre-Emery", "Ousmane Dembélé", "Gonçalo Ramos", "Barcola", "Safonov", "Beraldo", "Fabián Ruiz", "Lee Kang-in", "Kolo Muani")),
        makeL1("marseille", "Olympique Marseille", "OM", 0xFF2FAEE0L, 0xFFFFFFFFL, "Vélodrome", 67394, 4, 150_000_000L, "176", 79,
            listOf("Rulli", "Murillo", "Balerdi", "Kondogbia", "Merlin", "Højbjerg", "Rabiot", "Harit", "Greenwood", "Wahi", "Luis Henrique", "De Lange", "Brassier", "Rongier", "Rowe", "Maupay")),
        makeL1("monaco", "AS Monaco", "ASM", 0xFFE30613L, 0xFFFFFFFFL, "Louis II", 18523, 4, 165_000_000L, "174", 79,
            listOf("Majecki", "Vanderson", "Kehrer", "Salisu", "Caio Henrique", "Zakaria", "Camara", "Golovin", "Akliouche", "Embolo", "Ben Seghir", "Köhn", "Singo", "Magassa", "Minamino", "Balogun")),
        makeL1("lille", "Lille", "LIL", 0xFFE01E13L, 0xFF1F2A44L, "Pierre-Mauroy", 50186, 4, 130_000_000L, "166", 78,
            listOf("Chevalier", "Meunier", "Diakité", "Alexsandro", "Gudmundsson", "André", "André Gomes", "Bouaddi", "Zhegrova", "Jonathan David", "Sahraoui", "Mannone", "Mandi", "Angel Gomes", "Cabella", "Bayo")),
        makeL1("lyon", "Lyon", "OL", 0xFF004595L, 0xFFDA291CL, "Groupama Stadium", 59186, 4, 140_000_000L, "167", 78,
            listOf("Lucas Perri", "Maitland-Niles", "Mata", "Niakhaté", "Tagliafico", "Matic", "Tolisso", "Cherki", "Fofana", "Lacazette", "Mikautadze", "Descamps", "Caleta-Car", "Veretout", "Almada", "Nuamah")),
        makeL1("nice", "OGC Nice", "NIC", 0xFFCC0000L, 0xFF111111L, "Allianz Riviera", 36178, 4, 115_000_000L, "2502", 77,
            listOf("Bulka", "Clauss", "Dante", "Bombito", "Abdi", "Rosario", "Ndombélé", "Boudaoui", "Cho", "Guessand", "Boga", "Dupé", "Mendy", "Sanson", "Diop", "Laborde")),
        makeL1("lens", "RC Lens", "RCL", 0xFFEC1C24L, 0xFFFFED00L, "Bollaert-Delelis", 38223, 3, 95_000_000L, "175", 76,
            listOf("Samba", "Frankowski", "Danso", "Khusanov", "Medina", "Thomasson", "Diouf", "Fulgini", "Zaroury", "Nzola", "Sotoca", "Koffi", "Gradit", "El Aynaoui", "Pereira da Costa", "Labeau-Lascary")),
        makeL1("strasbourg", "Strasbourg", "STR", 0xFF009FE3L, 0xFFFFFFFFL, "La Meinau", 26109, 3, 85_000_000L, "180", 74,
            listOf("Petrovic", "Doué", "Sarr", "Sow", "Moreira", "Andrey Santos", "Diarra", "Lemaréchal", "Bakwa", "Emegha", "Nanasi", "Johnsson", "Sylla", "Mwanga", "Ouattara", "Mara")),
        makeL1("brest", "Brest", "SB29", 0xFFDA291CL, 0xFFFFFFFFL, "Francis-Le Blé", 15220, 3, 72_000_000L, "6997", 75,
            listOf("Bizot", "Lala", "Chardonnet", "Coulibaly", "Amavi", "Lees-Melou", "Camara", "Magnetti", "Del Castillo", "Ajorque", "Sima", "Coudert", "Le Cardinal", "Doumbia", "Faivre", "Baldé")),
        makeL1("toulouse", "Toulouse", "TFC", 0xFF512D6DL, 0xFFFFFFFFL, "Stadium Toulouse", 33150, 3, 70_000_000L, "179", 74,
            listOf("Restes", "Sidibé", "Cresswell", "McKenzie", "Suazo", "Casseres", "Sierro", "Dønnum", "Aboukhlal", "King", "Gboho", "Haug", "Nicolaisen", "Schmidt", "Babicka", "Magri")),
        makeL1("auxerre", "Auxerre", "AJA", 0xFF0055A4L, 0xFFFFFFFFL, "Abbé-Deschamps", 18541, 3, 55_000_000L, "172", 72,
            listOf("Léon", "Hoever", "Diomandé", "Jubal", "Akpa", "Owusu", "Danois", "Mensah", "Perrin", "Sinayoko", "Traorè", "De Percin", "Osho", "Raveloson", "Onaiwu", "Bair")),
        makeL1("rennes", "Rennes", "REN", 0xFFCE1126L, 0xFF111111L, "Roazhon Park", 29778, 3, 105_000_000L, "169", 75,
            listOf("Mandanda", "Hateboer", "Østigård", "Faye", "Truffert", "Matusiwa", "Santamaría", "Blas", "Gouiri", "Kalimuendo", "Grønbæk", "Gallon", "Wooh", "Kamara", "Jota", "Meister")),
        makeL1("nantes", "Nantes", "FCN", 0xFFFEE500L, 0xFF00843DL, "La Beaujoire", 35322, 3, 68_000_000L, "165", 73,
            listOf("Lafont", "Amian", "Castelletto", "Zézé", "Cozza", "Chirivella", "Douglas Augusto", "Lepenant", "Abline", "Mohamed", "Simon", "Carlgren", "Pallois", "Gbamin", "Thomas", "Kadewere")),
        makeL1("reims", "Reims", "SDR", 0xFFD00027L, 0xFFFFFFFFL, "Auguste-Delaune", 21029, 3, 74_000_000L, "3243", 73,
            listOf("Diouf", "Buta", "Okumu", "Agbadou", "Akieme", "Atangana", "Munetsi", "Teuma", "Ito", "Diakité", "Nakamura", "Olliero", "Kipré", "Koné", "Fofana", "Salama")),
        makeL1("angers", "Angers", "SCO", 0xFF181818L, 0xFFFFFFFFL, "Raymond Kopa", 19800, 2, 46_000_000L, "7868", 70,
            listOf("Fofana", "Arcus", "Biumla", "Lefort", "Hanin", "Aholou", "Belkebla", "Abdelli", "Allevinah", "Niane", "El Melali", "Zinga", "Bamba", "Belkhdim", "Ferhat", "Dieng")),
        makeL1("st_etienne", "Saint-Étienne", "ASSE", 0xFF009A44L, 0xFFFFFFFFL, "Geoffroy-Guichard", 41965, 3, 62_000_000L, "178", 71,
            listOf("Larsonneur", "Appiah", "Batubinsika", "Abdelhamid", "Pétrot", "Ekwah", "Bouchouari", "Mouton", "Cafaro", "Sissoko", "Davitashvili", "Maubleu", "Nadé", "Tardieu", "Boakye", "Stassin")),
        makeL1("montpellier", "Montpellier", "MHSC", 0xFF003B79L, 0xFFEA580CL, "La Mosson", 32900, 3, 52_000_000L, "274", 71,
            listOf("Lecomte", "Tchato", "Omeragic", "Sagnan", "Sylla", "Ferri", "Chotard", "Savanier", "Nordin", "Adams", "Khazri", "Bertaud", "Jullien", "Fayad", "Tamari", "Coulibaly")),
        makeL1("le_havre", "Le Havre", "HAC", 0xFF1D4ED8L, 0xFF7DD3FCL, "Océane", 25178, 2, 45_000_000L, "3236", 70,
            listOf("Desmas", "Nego", "Sangante", "Lloris", "Operi", "Touré", "Kechta", "Targhalline", "Casimir", "Ayew", "Soumaré", "Gorgelin", "Youté", "Kuziaev", "Joujou", "Sabbi"))
    )

    private fun makeL1(
        id: String, name: String, shortName: String,
        primaryHex: Long, secondaryHex: Long, stadium: String,
        capacity: Int, prestige: Int, budget: Long, espnId: String,
        baseOvr: Int, names: List<String>
    ): Club = makeLeagueClub(
        id = id, name = name, shortName = shortName, division = Division.LIGUE_1,
        primaryHex = primaryHex, secondaryHex = secondaryHex, stadium = stadium,
        capacity = capacity, prestige = prestige, budget = budget, espnId = espnId,
        names = names, baseOverall = baseOvr, maxOverall = 89, minOverall = 66
    )
}
