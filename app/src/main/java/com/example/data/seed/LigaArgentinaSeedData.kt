package com.example.data.seed

import com.example.domain.model.Club
import com.example.domain.model.Division

object LigaArgentinaSeedData {
    fun clubs(): List<Club> = listOf(
        makeARG("river_plate", "River Plate", "RIV", 0xFFDA291CL, 0xFFFFFFFFL, "Monumental", 84567, 5, 95_000_000L, "16", 79,
            listOf("Armani", "Bustos", "Pezzella", "Paulo Díaz", "Acuña", "Kranevitter", "Simón", "Meza", "Mastantuono", "Borja", "Colidio", "Ledesma", "González Pírez", "Lanzini", "Echeverri", "Solari")),
        makeARG("boca_juniors", "Boca Juniors", "BOC", 0xFF003B79L, 0xFFFACC15L, "La Bombonera", 54000, 5, 88_000_000L, "5", 78,
            listOf("Romero", "Advíncula", "Lema", "Marcos Rojo", "Blanco", "Miramón", "Pol Fernández", "Medina", "Zenón", "Cavani", "Merentiel", "Brey", "Figal", "Belmonte", "Zeballos", "Giménez")),
        makeARG("racing_arg", "Racing Club", "RAC", 0xFF38BDF8L, 0xFFFFFFFFL, "El Cilindro", 55000, 4, 68_000_000L, "15", 77,
            listOf("Arias", "Martirena", "Di Césare", "García Basso", "Rojas", "Sosa", "Nardoni", "Almendra", "Quintero", "Adrián Martínez", "Salas", "Cambeses", "Colombo", "Zuculini", "Vietto", "Carbonero")),
        makeARG("velez", "Vélez Sarsfield", "VEL", 0xFF0055A4L, 0xFFFFFFFFL, "José Amalfitani", 49540, 4, 64_000_000L, "19", 77,
            listOf("Marchiori", "Joaquín García", "Mammana", "Valentín Gómez", "Elías Gómez", "Bouzat", "Ordóñez", "Aquino", "Pizzini", "Braian Romero", "Thiago Fernández", "Garzón", "Pernicone", "Jalil Elías", "Carrizo", "Santos")),
        makeARG("talleres", "Talleres", "TAL", 0xFF0F2C59L, 0xFFFFFFFFL, "Mario Kempes", 57000, 4, 58_000_000L, "17", 76,
            listOf("Herrera", "Benavídez", "Catalán", "Portillo", "Navarro", "Ortegoza", "Portilla", "Botta", "Galarsa", "Girotti", "Palacios", "Morales", "Suárez", "Bustos", "Depietri", "Tarragona")),
        makeARG("estudiantes", "Estudiantes LP", "EST", 0xFFD00027L, 0xFFFFFFFFL, "UNO Jorge Hirschi", 32530, 4, 60_000_000L, "8", 76,
            listOf("Mansilla", "Meza", "Boselli", "Lollo", "Arzamendia", "Ascacíbar", "Enzo Pérez", "Neves", "Tiago Palacios", "Carrillo", "Cetré", "Iacovich", "Funes Mori", "Sosa", "Piatti", "Giménez")),
        makeARG("independiente", "Independiente", "IND", 0xFFDC2626L, 0xFFFFFFFFL, "Libertadores de América", 42069, 4, 55_000_000L, "11", 75,
            listOf("Rey", "Vera", "Lomónaco", "Laso", "Sporle", "Marcone", "Loyola", "Cabral", "Montiel", "Ávalos", "Hidalgo", "Segovia", "Fedórco", "Mancuello", "Luna", "Maestro Puch")),
        makeARG("san_lorenzo", "San Lorenzo", "SLO", 0xFF1D4ED8L, 0xFFDC2626L, "Nuevo Gasómetro", 47964, 4, 52_000_000L, "18", 74,
            listOf("Gómez", "Luján", "Romaña", "Campi", "Báez", "Irala", "Sosa", "Muniain", "Cerutti", "Vombergar", "Cuello", "Altamirano", "Hernández", "Remedi", "Leguizamón", "Bustos")),
        makeARG("huracan", "Huracán", "HUR", 0xFFFFFFFFL, 0xFFDC2626L, "Tomás Ducó", 48314, 3, 45_000_000L, "10", 74,
            listOf("Galíndez", "Souto", "Pereyra", "Tobio", "Ibáñez", "Fattori", "Alarcón", "Echeverría", "Mazzantti", "Ábila", "Cabral", "Meza", "Carrizo", "Siles", "Pusetto", "Ramírez")),
        makeARG("rosario_central", "Rosario Central", "ROS", 0xFF003B79L, 0xFFFACC15L, "Gigante de Arroyito", 46955, 3, 46_000_000L, "602", 74,
            listOf("Broun", "Coronel", "Mallo", "Quintana", "Sández", "Ibarra", "Ortiz", "Malcorra", "Campaz", "Ruben", "Copetti", "Werner", "Komar", "Gómez", "Lovera", "Modica")),
        makeARG("newells", "Newell's Old Boys", "NOB", 0xFFDC2626L, 0xFF111111L, "Marcelo Bielsa", 42000, 3, 44_000_000L, "14", 73,
            listOf("Macagno", "Méndez", "Velázquez", "Salcedo", "Martino", "Fernández", "Cedrés", "Banega", "Silvetti", "Ramírez", "González", "Hoyos", "Glavinovich", "Miljevic", "Cardozo", "Juanchón")),
        makeARG("lanus", "Lanús", "LAN", 0xFF7F1D1DL, 0xFFFFFFFFL, "La Fortaleza", 47027, 3, 48_000_000L, "12", 74,
            listOf("Losada", "Cáceres", "Izquierdoz", "Luciatti", "Soler", "Peña Biafore", "Boggio", "Marcelino Moreno", "Salvio", "Walter Bou", "Carrera", "Aguerre", "Muñoz", "Loaiza", "Acosta", "Díaz")),
        makeARG("argentinos_jrs", "Argentinos Juniors", "AAAJ", 0xFFDC2626L, 0xFFFFFFFFL, "Diego Maradona", 26000, 3, 42_000_000L, "3", 73,
            listOf("Rodríguez", "Santamaría", "Galván", "Álvarez", "Vega", "Fattori", "Gómez", "Lescano", "Oroz", "Gondou", "Herrera", "Siri", "Palacio", "Cardozo", "Romero", "Verón")),
        makeARG("belgrano", "Belgrano", "BEL", 0xFF0284C7L, 0xFFFFFFFFL, "Julio César Villagra", 34830, 3, 40_000_000L, "4", 73,
            listOf("Espínola", "Compagnucci", "Rébola", "Troilo", "Velázquez", "Quignón", "Rolón", "Metilli", "González", "Jara", "Uvita Fernández", "Chicco", "Delgado", "Longo", "Reyna", "Passerini")),
        makeARG("defensa_justicia", "Defensa y Justicia", "DYJ", 0xFF15803DL, 0xFFFACC15L, "Norberto Tomaghello", 20000, 3, 38_000_000L, "603", 72,
            listOf("Bologna", "Cannavo", "Ferreira", "Ramos Mingo", "Soto", "Gutiérrez", "López", "Molinas", "Palavecino", "Osorio", "Bogarín", "Fiermarín", "Aguilera", "Schamine", "Togni", "Miritello")),
        makeARG("union_sf", "Unión Santa Fe", "UNI", 0xFFDC2626L, 0xFFFFFFFFL, "15 de Abril", 30000, 3, 35_000_000L, "605", 72,
            listOf("Cardozo", "Vargas", "Pardo", "Corvalán", "Bruno Pittón", "Mosqueira", "Mauro Pittón", "Rivero", "Luna Diale", "Balboa", "Orsini", "Meuli", "Paz", "Roldán", "Del Blanco", "Morales")),
        makeARG("godoy_cruz", "Godoy Cruz", "GOD", 0xFF1D4ED8L, 0xFFFFFFFFL, "Malvinas Argentinas", 42500, 3, 39_000_000L, "9", 73,
            listOf("Petroli", "Arce", "Barrios", "Rasmussen", "Meli", "Leyes", "Fernández", "Poggi", "Altamira", "Salomón Rodríguez", "Andino", "Ramírez", "Butti", "Abrego", "Cejas", "Pino")),
        makeARG("platense", "Platense", "PLA", 0xFF6B3E26L, 0xFFFFFFFFL, "Ciudad de Vicente López", 34530, 2, 32_000_000L, "604", 71,
            listOf("Cozzani", "Saborido", "Vázquez", "Suso", "Marcich", "Picco", "Juárez", "Gómez", "Taborda", "Pellegrino", "Mainero", "Desábato", "Salomón", "Roldán", "Minerva", "Martínez")),
        makeARG("banfield", "Banfield", "BAN", 0xFF15803DL, 0xFFFFFFFFL, "Florencio Sola", 34900, 3, 34_000_000L, "2", 71,
            listOf("Sanguinetti", "Bonifacio", "Maciel", "Maldonado", "Insúa", "Núñez", "Rodríguez", "Cañete", "Rivera", "Sepúlveda", "Garate", "Devecchi", "Recalde", "Soraire", "Álvarez", "Bisanz")),
        makeARG("tigre", "Tigre", "TIG", 0xFF1D4ED8L, 0xFFDC2626L, "José Dellagiovanna", 26282, 2, 33_000_000L, "606", 71,
            listOf("Zenobio", "Ortega", "Paz", "Nardelli", "Banegas", "González", "Scipioni", "Cardozo", "Maroni", "Contín", "Armoa", "Sosa", "Ferloni", "Garay", "Fornari", "Monzón"))
    )

    private fun makeARG(
        id: String, name: String, shortName: String,
        primaryHex: Long, secondaryHex: Long, stadium: String,
        capacity: Int, prestige: Int, budget: Long, espnId: String,
        baseOvr: Int, names: List<String>
    ): Club = makeLeagueClub(
        id = id, name = name, shortName = shortName, division = Division.LIGA_ARGENTINA,
        primaryHex = primaryHex, secondaryHex = secondaryHex, stadium = stadium,
        capacity = capacity, prestige = prestige, budget = budget, espnId = espnId,
        names = names, baseOverall = baseOvr, maxOverall = 85, minOverall = 66
    )
}
