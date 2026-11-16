package com.example.data.seed

import com.example.domain.model.Club
import com.example.domain.model.Division

object SerieAItaSeedData {
    fun clubs(): List<Club> = listOf(
        makeIta("inter_milan", "Inter de Milão", "INT", 0xFF010E80L, 0xFF111111L, "Giuseppe Meazza", 75817, 5, 240_000_000L, "110", 83,
            listOf("Sommer", "Dumfries", "Pavard", "Acerbi", "Bastoni", "Dimarco", "Barella", "Çalhanoglu", "Mkhitaryan", "Lautaro Martínez", "Marcus Thuram", "Martínez", "De Vrij", "Carlos Augusto", "Frattesi", "Taremi")),
        makeIta("napoli", "Napoli", "NAP", 0xFF12A0D7L, 0xFFFFFFFFL, "Diego Maradona", 54726, 5, 195_000_000L, "114", 82,
            listOf("Meret", "Di Lorenzo", "Rrahmani", "Buongiorno", "Olivera", "Anguissa", "Lobotka", "McTominay", "Politano", "Lukaku", "Neres", "Caprile", "Spinazzola", "Gilmour", "Raspadori", "Simeone")),
        makeIta("atalanta", "Atalanta", "ATA", 0xFF1E71B8L, 0xFF111111L, "Gewiss Stadium", 24950, 4, 155_000_000L, "105", 81,
            listOf("Carnesecchi", "Djimsiti", "Hien", "Kolasinac", "Bellanova", "De Roon", "Éderson", "Ruggeri", "De Ketelaere", "Retegui", "Lookman", "Rui Patrício", "Kossounou", "Zappacosta", "Pasalic", "Samardzic")),
        makeIta("juventus", "Juventus", "JUV", 0xFF181818L, 0xFFFFFFFFL, "Allianz Stadium", 41507, 5, 225_000_000L, "111", 81,
            listOf("Di Gregorio", "Savona", "Kalulu", "Gatti", "Cambiaso", "Locatelli", "Thuram", "Koopmeiners", "Conceição", "Vlahovic", "Yildiz", "Perin", "Danilo", "McKennie", "Douglas Luiz", "Nico González")),
        makeIta("milan", "AC Milan", "MIL", 0xFFFB090BL, 0xFF111111L, "San Siro", 75817, 5, 220_000_000L, "103", 81,
            listOf("Maignan", "Emerson Royal", "Tomori", "Gabbia", "Theo Hernández", "Fofana", "Reijnders", "Pulisic", "Loftus-Cheek", "Morata", "Rafael Leão", "Sportiello", "Thiaw", "Musah", "Chukwueze", "Abraham")),
        makeIta("lazio", "Lazio", "LAZ", 0xFF87D8F7L, 0xFFFFFFFFL, "Olímpico", 70634, 4, 130_000_000L, "112", 79,
            listOf("Provedel", "Marusic", "Gila", "Romagnoli", "Nuno Tavares", "Guendouzi", "Rovella", "Isaksen", "Dia", "Castellanos", "Zaccagni", "Mandas", "Lazzari", "Vecino", "Pedro", "Noslin")),
        makeIta("fiorentina", "Fiorentina", "FIO", 0xFF482E92L, 0xFFFFFFFFL, "Artemio Franchi", 43147, 4, 125_000_000L, "109", 78,
            listOf("De Gea", "Dodô", "Comuzzo", "Ranieri", "Gosens", "Adli", "Cataldi", "Colpani", "Gudmundsson", "Moise Kean", "Sottil", "Terracciano", "Pongracic", "Mandragora", "Beltrán", "Kouamé")),
        makeIta("bologna", "Bologna", "BOL", 0xFF1A2F48L, 0xFFA21C26L, "Renato Dall'Ara", 36462, 4, 110_000_000L, "107", 77,
            listOf("Skorupski", "Posch", "Beukema", "Lucumí", "Miranda", "Freuler", "Pobega", "Ferguson", "Orsolini", "Castro", "Ndoye", "Ravaglia", "Holm", "Moro", "Odgaard", "Dallinga")),
        makeIta("roma", "AS Roma", "ROM", 0xFF8E1F2FL, 0xFFF0BC42L, "Olímpico", 70634, 4, 165_000_000L, "104", 79,
            listOf("Svilar", "Celik", "Mancini", "Ndicka", "Angeliño", "Koné", "Cristante", "Pellegrini", "Dybala", "Dovbyk", "Saelemaekers", "Ryan", "Hummels", "Paredes", "Soulé", "El Shaarawy")),
        makeIta("udinese", "Udinese", "UDI", 0xFF181818L, 0xFFFFFFFFL, "Bluenergy Stadium", 25132, 3, 75_000_000L, "118", 74,
            listOf("Okoye", "Ehizibue", "Bijol", "Giannetti", "Kamara", "Lovric", "Karlstrom", "Payero", "Thauvin", "Lucca", "Davis", "Sava", "Kabasele", "Zarraga", "Ekkelenkamp", "Brenner")),
        makeIta("torino", "Torino", "TOR", 0xFF8A1E03L, 0xFFFFFFFFL, "Olímpico Grande Torino", 28177, 3, 85_000_000L, "239", 74,
            listOf("Milinkovic-Savic", "Walukiewicz", "Coco", "Masina", "Pedersen", "Ricci", "Linetty", "Vlasic", "Lazaro", "Che Adams", "Sanabria", "Paleari", "Maripán", "Ilic", "Gineitis", "Karamoh")),
        makeIta("genoa", "Genoa", "GEN", 0xFFAD1927L, 0xFF0A1D37L, "Luigi Ferraris", 33205, 3, 72_000_000L, "3263", 73,
            listOf("Leali", "Sabelli", "Bani", "Vásquez", "Martín", "Frendrup", "Badelj", "Thorsby", "Zanoli", "Pinamonti", "Miretti", "Gollini", "Matturro", "Masini", "Messias", "Vitinha")),
        makeIta("cagliari", "Cagliari", "CAG", 0xFFA21C26L, 0xFF002D5AL, "Unipol Domus", 16416, 3, 62_000_000L, "2925", 72,
            listOf("Sherri", "Zappa", "Mina", "Luperto", "Augello", "Adopo", "Makoumbou", "Marin", "Zortea", "Piccoli", "Luvumbo", "Scuffet", "Palomino", "Deiola", "Gaetano", "Lapadula")),
        makeIta("como", "Como", "COM", 0xFF0055A4L, 0xFFFFFFFFL, "Giuseppe Sinigaglia", 13602, 3, 88_000_000L, "2572", 73,
            listOf("Reina", "Van der Brempt", "Dossena", "Kempf", "Moreno", "Perrone", "Sergi Roberto", "Nico Paz", "Strefezza", "Cutrone", "Fadera", "Audero", "Goldaniga", "Mazzitelli", "Da Cunha", "Belotti")),
        makeIta("verona", "Hellas Verona", "VER", 0xFF002F6CL, 0xFFFACC15L, "Bentegodi", 39211, 3, 58_000_000L, "119", 72,
            listOf("Montipò", "Tchatchoua", "Dawidowicz", "Coppola", "Bradaric", "Duda", "Serdar", "Suslov", "Lazovic", "Tengstedt", "Sarr", "Perilli", "Magnani", "Belahyane", "Harroui", "Mosquera")),
        makeIta("empoli", "Empoli", "EMP", 0xFF005CA9L, 0xFFFFFFFFL, "Carlo Castellani", 16284, 3, 55_000_000L, "2574", 72,
            listOf("Vásquez", "Goglichidze", "Ismajli", "Viti", "Gyasi", "Henderson", "Grassi", "Pezzella", "Fazzini", "Colombo", "Esposito", "Seghetti", "De Sciglio", "Maleh", "Anjorin", "Pellegri")),
        makeIta("parma", "Parma", "PAR", 0xFF1D4ED8L, 0xFFFACC15L, "Ennio Tardini", 22352, 3, 68_000_000L, "115", 73,
            listOf("Suzuki", "Coulibaly", "Delprato", "Balogh", "Valeri", "Bernabé", "Sohm", "Man", "Hernani", "Bonny", "Mihaila", "Chichizola", "Leoni", "Keita", "Cancellieri", "Charpentier")),
        makeIta("lecce", "Lecce", "LEC", 0xFFDC2626L, 0xFFFACC15L, "Via del Mare", 31533, 2, 54_000_000L, "113", 71,
            listOf("Falcone", "Guilbert", "Baschirotto", "Gaspar", "Gallo", "Coulibaly", "Ramadani", "Rafia", "Dorgu", "Krstovic", "Morente", "Fruchtl", "Jean", "Pierret", "Oudin", "Rebic")),
        makeIta("monza", "Monza", "MON", 0xFFE30613L, 0xFFFFFFFFL, "U-Power Stadium", 16917, 3, 60_000_000L, "4007", 72,
            listOf("Turati", "Izzo", "Marí", "Carboni", "Pereira", "Bondo", "Pessina", "Kyriakopoulos", "Maldini", "Djuric", "Dany Mota", "Pizzignacco", "Caldirola", "Bianco", "Caprari", "Petagna")),
        makeIta("venezia", "Venezia", "VEN", 0xFFEA580CL, 0xFF15803DL, "Pier Luigi Penzo", 11150, 2, 48_000_000L, "4050", 70,
            listOf("Stankovic", "Idzes", "Svoboda", "Sverko", "Candela", "Nicolussi", "Duncan", "Zampano", "Busio", "Pohjanpalo", "Oristanio", "Joronen", "Altare", "Ellertsson", "Yeboah", "Gytkjaer"))
    )

    private fun makeIta(
        id: String, name: String, shortName: String,
        primaryHex: Long, secondaryHex: Long, stadium: String,
        capacity: Int, prestige: Int, budget: Long, espnId: String,
        baseOvr: Int, names: List<String>
    ): Club = makeLeagueClub(
        id = id, name = name, shortName = shortName, division = Division.SERIE_A_ITA,
        primaryHex = primaryHex, secondaryHex = secondaryHex, stadium = stadium,
        capacity = capacity, prestige = prestige, budget = budget, espnId = espnId,
        names = names, baseOverall = baseOvr, maxOverall = 89, minOverall = 67
    )
}
