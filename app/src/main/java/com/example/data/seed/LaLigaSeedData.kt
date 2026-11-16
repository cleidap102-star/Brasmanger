package com.example.data.seed

import com.example.domain.model.Club
import com.example.domain.model.Division

object LaLigaSeedData {
    fun clubs(): List<Club> = listOf(
        makeLL("real_madrid", "Real Madrid", "RMA", 0xFFFFFFFFL, 0xFFFEBE10L, "Santiago Bernabéu", 85000, 5, 360_000_000L, "86", 85,
            listOf("Courtois", "Carvajal", "Rüdiger", "Éder Militão", "Mendy", "Valverde", "Tchouaméni", "Jude Bellingham", "Rodrygo", "Kylian Mbappé", "Vinícius Jr", "Lunin", "Camavinga", "Modric", "Arda Güler", "Endrick")),
        makeLL("barcelona", "Barcelona", "BAR", 0xFFA50044L, 0xFF004D98L, "Camp Nou", 99354, 5, 310_000_000L, "83", 84,
            listOf("Ter Stegen", "Koundé", "Cubarsí", "Iñigo Martínez", "Balde", "Pedri", "Casadó", "Dani Olmo", "Lamine Yamal", "Lewandowski", "Raphinha", "Iñaki Peña", "Araújo", "Gavi", "Fermín", "Ferran Torres")),
        makeLL("atletico_madrid", "Atlético Madrid", "ATM", 0xFFCB3524L, 0xFF272E61L, "Metropolitano", 70460, 5, 250_000_000L, "1068", 82,
            listOf("Oblak", "Llorente", "Giménez", "Le Normand", "Galán", "De Paul", "Barrios", "Koke", "Griezmann", "Julián Álvarez", "Sørloth", "Musso", "Lenglet", "Gallagher", "Lino", "Correa")),
        makeLL("athletic_bilbao", "Athletic Bilbao", "ATH", 0xFFEE2523L, 0xFFFFFFFFL, "San Mamés", 53331, 4, 140_000_000L, "93", 79,
            listOf("Unai Simón", "De Marcos", "Vivian", "Paredes", "Yuri", "Ruiz de Galarreta", "Jauregizar", "Sancet", "Iñaki Williams", "Guruzeta", "Nico Williams", "Agirrezabala", "Yeray", "Prados", "Berenguer", "Djaló")),
        makeLL("villarreal", "Villarreal", "VIL", 0xFFFFE667L, 0xFF005187L, "La Cerámica", 23008, 4, 125_000_000L, "102", 78,
            listOf("Diego Conde", "Femenía", "Albiol", "Costa", "Cardona", "Parejo", "Comesaña", "Álex Baena", "Pépé", "Barry", "Ayoze Pérez", "Luiz Júnior", "Foyth", "Pape Gueye", "Yeremy Pino", "Gerard Moreno")),
        makeLL("real_sociedad", "Real Sociedad", "RSO", 0xFF0067B1L, 0xFFFFFFFFL, "Reale Arena", 39313, 4, 130_000_000L, "89", 78,
            listOf("Remiro", "Aramburu", "Zubeldia", "Aguerd", "Aihen", "Zubimendi", "Sucic", "Brais Méndez", "Kubo", "Oyarzabal", "Barrenetxea", "Marrero", "Aritz", "Sergio Gómez", "Becker", "Óskarsson")),
        makeLL("real_betis", "Real Betis", "BET", 0xFF0BB363L, 0xFFFFFFFFL, "Benito Villamarín", 60721, 4, 115_000_000L, "244", 77,
            listOf("Rui Silva", "Sabaly", "Llorente", "Natan", "Perraud", "Johnny Cardoso", "Fornals", "Lo Celso", "Antony", "Vitor Roque", "Abde", "Adrián", "Bartra", "Marc Roca", "Isco", "Chimy Ávila")),
        makeLL("girona", "Girona", "GIR", 0xFFCD2534L, 0xFFFFFFFFL, "Montilivi", 14624, 3, 95_000_000L, "9812", 76,
            listOf("Gazzaniga", "Arnau Martínez", "David López", "Krejci", "Miguel Gutiérrez", "Yangel Herrera", "Iván Martín", "Van de Beek", "Tsygankov", "Abel Ruiz", "Bryan Gil", "Pau López", "Blind", "Oriol Romeu", "Portu", "Stuani")),
        makeLL("sevilla", "Sevilla", "SEV", 0xFFD00027L, 0xFFFFFFFFL, "Sánchez-Pizjuán", 43883, 4, 105_000_000L, "243", 76,
            listOf("Nyland", "Carmona", "Badé", "Salas", "Pedrosa", "Agoumé", "Lokonga", "Saúl", "Lukebakio", "Isaac Romero", "Ejuke", "Álvaro Fernández", "Marcão", "Gudelj", "Suso", "Iheanacho")),
        makeLL("osasuna", "Osasuna", "OSA", 0xFFD91A21L, 0xFF0A346FL, "El Sadar", 23576, 3, 75_000_000L, "97", 75,
            listOf("Sergio Herrera", "Areso", "Catena", "Boyomo", "Abel Bretones", "Torró", "Moncayola", "Aimar Oroz", "Rubén García", "Budimir", "Bryan Zaragoza", "Aitor", "Juan Cruz", "Iker Muñoz", "Moi Gómez", "Raúl García")),
        makeLL("celta", "Celta de Vigo", "CEL", 0xFF8AC3EEL, 0xFFFFFFFFL, "Balaídos", 24870, 3, 80_000_000L, "85", 75,
            listOf("Guaita", "Mingueza", "Starfelt", "Marcos Alonso", "Hugo Álvarez", "Beltrán", "Ilaix Moriba", "Swedberg", "Iago Aspas", "Borja Iglesias", "Bamba", "Iván Villar", "Javi Rodríguez", "Sotelo", "Cervi", "Douvikas")),
        makeLL("mallorca", "Mallorca", "MAL", 0xFFE20613L, 0xFF111111L, "Son Moix", 26020, 3, 70_000_000L, "84", 74,
            listOf("Greif", "Maffeo", "Valjent", "Raíllo", "Mojica", "Samú Costa", "Morlanes", "Sergi Darder", "Dani Rodríguez", "Muriqi", "Larin", "Leo Román", "Copete", "Mascarell", "Asano", "Abdón")),
        makeLL("rayo", "Rayo Vallecano", "RAY", 0xFFE53027L, 0xFFFFFFFFL, "Vallecas", 14708, 3, 65_000_000L, "101", 74,
            listOf("Batalla", "Ratiu", "Lejeune", "Mumin", "Chavarría", "Pathé Ciss", "Óscar Valentín", "Isi Palazón", "De Frutos", "Camello", "Álvaro García", "Cárdenas", "Aridane", "Unai López", "James Rodríguez", "Nteka")),
        makeLL("valencia", "Valencia", "VAL", 0xFFEE3524L, 0xFF111111L, "Mestalla", 49430, 4, 90_000_000L, "94", 75,
            listOf("Mamardashvili", "Thierry", "Mosquera", "Tárrega", "Gayà", "Pepelu", "Barrenechea", "Javi Guerra", "Diego López", "Hugo Duro", "Rioja", "Dimitrievski", "Foulquier", "Almeida", "Canós", "Rafa Mir")),
        makeLL("getafe", "Getafe", "GET", 0xFF005999L, 0xFFFFFFFFL, "Coliseum", 16500, 3, 62_000_000L, "2922", 73,
            listOf("David Soria", "Iglesias", "Djené", "Alderete", "Diego Rico", "Luis Milla", "Arambarri", "Uche", "Carles Pérez", "Mayoral", "Sola", "Letacek", "Duarte", "Santiago", "Aleñá", "Yildirim")),
        makeLL("alaves", "Alavés", "ALA", 0xFF005CA9L, 0xFFFFFFFFL, "Mendizorroza", 19840, 3, 58_000_000L, "96", 73,
            listOf("Sivera", "Tenaglia", "Abqar", "Mouriño", "Manu Sánchez", "Guevara", "Blanco", "Guridi", "Carlos Vicente", "Kike García", "Carlos Martín", "Owono", "Sedlar", "Jordán", "Conechny", "Toni Martínez")),
        makeLL("las_palmas", "Las Palmas", "LPA", 0xFFFFE400L, 0xFF0067B1L, "Gran Canaria", 32392, 3, 60_000_000L, "98", 73,
            listOf("Cillessen", "Viti", "Álex Suárez", "McKenna", "Álex Muñoz", "Essugo", "Kirian", "Moleiro", "Sandro", "Fábio Silva", "Januzaj", "Horkas", "Mármol", "Campaña", "Manu Fuster", "McBurnie")),
        makeLL("espanyol", "Espanyol", "ESP", 0xFF007FC8L, 0xFFFFFFFFL, "RCDE Stadium", 40000, 3, 64_000_000L, "88", 72,
            listOf("Joan García", "El Hilali", "Kumbulla", "Cabrera", "Romero", "Kral", "Pol Lozano", "Tejero", "Jofre", "Puado", "Véliz", "Pacheco", "Calero", "Gragera", "Aguado", "Cheddira")),
        makeLL("leganes", "Leganés", "LEG", 0xFF005CA9L, 0xFFFFFFFFL, "Butarque", 12454, 2, 50_000_000L, "17534", 71,
            listOf("Dmitrovic", "Rosier", "Sergio González", "Nastasic", "Javi Hernández", "Tapia", "Neyou", "Cissé", "Juan Cruz", "Haller", "De la Fuente", "Soriano", "Sáenz", "Brasanac", "Óscar", "Munir")),
        makeLL("valladolid", "Valladolid", "VLL", 0xFF5A2D81L, 0xFFFFFFFFL, "José Zorrilla", 27618, 2, 52_000_000L, "95", 71,
            listOf("Hein", "Luis Pérez", "Javi Sánchez", "Bah", "Rosa", "Juric", "Comert", "Amallah", "Anuar", "Sylla", "Raúl Moro", "Ferreira", "Torres", "Kike Pérez", "Meseguer", "Latasa"))
    )

    private fun makeLL(
        id: String, name: String, shortName: String,
        primaryHex: Long, secondaryHex: Long, stadium: String,
        capacity: Int, prestige: Int, budget: Long, espnId: String,
        baseOvr: Int, names: List<String>
    ): Club = makeLeagueClub(
        id = id, name = name, shortName = shortName, division = Division.LA_LIGA,
        primaryHex = primaryHex, secondaryHex = secondaryHex, stadium = stadium,
        capacity = capacity, prestige = prestige, budget = budget, espnId = espnId,
        names = names, baseOverall = baseOvr, maxOverall = 90, minOverall = 67
    )
}
