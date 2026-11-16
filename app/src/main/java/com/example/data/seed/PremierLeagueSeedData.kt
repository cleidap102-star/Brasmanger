package com.example.data.seed

import com.example.domain.model.Club
import com.example.domain.model.Division

object PremierLeagueSeedData {
    fun clubs(): List<Club> = listOf(
        makePL("arsenal", "Arsenal", "ARS", 0xFFEF0107L, 0xFFFFFFFFL, "Emirates Stadium", 60704, 5, 290_000_000L, "359", 83,
            listOf("David Raya", "Ben White", "William Saliba", "Gabriel Magalhães", "Timber", "Declan Rice", "Martin Odegaard", "Mikel Merino", "Bukayo Saka", "Kai Havertz", "Gabriel Martinelli", "Neto", "Calafiori", "Partey", "Trossard", "Gabriel Jesus")),
        makePL("liverpool", "Liverpool", "LIV", 0xFFC8102EL, 0xFFFFFFFFL, "Anfield", 61276, 5, 300_000_000L, "364", 84,
            listOf("Alisson", "Alexander-Arnold", "Konaté", "Virgil van Dijk", "Robertson", "Gravenberch", "Mac Allister", "Szoboszlai", "Mohamed Salah", "Darwin Núñez", "Luis Díaz", "Kelleher", "Gomez", "Jones", "Gakpo", "Diogo Jota")),
        makePL("man_city", "Manchester City", "MCI", 0xFF6CABDDL, 0xFFFFFFFFL, "Etihad Stadium", 53400, 5, 340_000_000L, "382", 84,
            listOf("Ederson", "Walker", "Rúben Dias", "Gvardiol", "Aké", "Rodri", "Kevin De Bruyne", "Bernardo Silva", "Phil Foden", "Erling Haaland", "Savinho", "Ortega", "Stones", "Kovacic", "Grealish", "Doku")),
        makePL("chelsea", "Chelsea", "CHE", 0xFF034694L, 0xFFFFFFFFL, "Stamford Bridge", 40341, 5, 280_000_000L, "363", 81,
            listOf("Robert Sánchez", "Malo Gusto", "Fofana", "Colwill", "Cucurella", "Caicedo", "Enzo Fernández", "Cole Palmer", "Noni Madueke", "Nicolas Jackson", "Pedro Neto", "Jörgensen", "James", "Lavia", "Nkunku", "João Félix")),
        makePL("man_utd", "Manchester United", "MUN", 0xFFDA291CL, 0xFFFBE122L, "Old Trafford", 74310, 5, 260_000_000L, "360", 80,
            listOf("Onana", "Mazraoui", "De Ligt", "Lisandro Martínez", "Dalot", "Ugarte", "Mainoo", "Bruno Fernandes", "Amad Diallo", "Højlund", "Garnacho", "Bayindir", "Maguire", "Casemiro", "Mount", "Zirkzee")),
        makePL("tottenham", "Tottenham", "TOT", 0xFF132257L, 0xFFFFFFFFL, "Tottenham Stadium", 62850, 4, 210_000_000L, "367", 80,
            listOf("Vicario", "Pedro Porro", "Romero", "Van de Ven", "Udogie", "Bissouma", "Bentancur", "Maddison", "Kulusevski", "Solanke", "Son Heung-min", "Forster", "Dragusin", "Sarr", "Johnson", "Richarlison")),
        makePL("newcastle", "Newcastle", "NEW", 0xFF241F20L, 0xFFFFFFFFL, "St. James' Park", 52305, 4, 220_000_000L, "361", 79,
            listOf("Nick Pope", "Livramento", "Schär", "Burn", "Lewis Hall", "Bruno Guimarães", "Tonali", "Joelinton", "Murphy", "Alexander Isak", "Anthony Gordon", "Dubravka", "Trippier", "Botman", "Barnes", "Wilson")),
        makePL("aston_villa", "Aston Villa", "AVL", 0xFF670E36L, 0xFF95BFE5L, "Villa Park", 42657, 4, 185_000_000L, "362", 79,
            listOf("Emiliano Martínez", "Cash", "Konsa", "Pau Torres", "Digne", "Onana", "Tielemans", "McGinn", "Morgan Rogers", "Ollie Watkins", "Bailey", "Olsen", "Maatsen", "Kamara", "Buendía", "Malen")),
        makePL("brighton", "Brighton", "BHA", 0xFF0057B8L, 0xFFFFFFFFL, "Amex Stadium", 31876, 4, 130_000_000L, "331", 77,
            listOf("Verbruggen", "Veltman", "Van Hecke", "Dunk", "Estupiñán", "Baleba", "Hinshelwood", "Rutter", "Minteh", "João Pedro", "Mitoma", "Steele", "Igor", "Wieffer", "Adingra", "Welbeck")),
        makePL("west_ham", "West Ham", "WHU", 0xFF7A263AL, 0xFF1BB1E7L, "London Stadium", 62500, 4, 135_000_000L, "371", 76,
            listOf("Areola", "Wan-Bissaka", "Todibo", "Kilman", "Emerson", "Álvarez", "Soucek", "Lucas Paquetá", "Jarrod Bowen", "Füllkrug", "Kudus", "Fabianski", "Mavropanos", "Soler", "Summerville", "Antonio")),
        makePL("fulham", "Fulham", "FUL", 0xFF111111L, 0xFFFFFFFFL, "Craven Cottage", 25700, 3, 110_000_000L, "370", 76,
            listOf("Leno", "Tete", "Andersen", "Bassey", "Robinson", "Berge", "Andreas Pereira", "Smith Rowe", "Adama Traoré", "Raúl Jiménez", "Iwobi", "Benda", "Diop", "Lukic", "Wilson", "Rodrigo Muniz")),
        makePL("bournemouth", "Bournemouth", "BOU", 0xFFDA291CL, 0xFF111111L, "Vitality Stadium", 11307, 3, 95_000_000L, "349", 76,
            listOf("Kepa", "Smith", "Zabarnyi", "Huijsen", "Kerkez", "Cook", "Christie", "Kluivert", "Semenyo", "Evanilson", "Tavernier", "Travers", "Senesi", "Adams", "Brooks", "Ouattara")),
        makePL("brentford", "Brentford", "BRE", 0xFFE30613L, 0xFFFFFFFFL, "Gtech Community", 17250, 3, 95_000_000L, "337", 75,
            listOf("Flekken", "van den Berg", "Collins", "Pinnock", "Lewis-Potter", "Nørgaard", "Janelt", "Damsgaard", "Mbeumo", "Wissa", "Schade", "Valdimarsson", "Ajer", "Jensen", "Carvalho", "Thiago")),
        makePL("nottm_forest", "Nottingham Forest", "NFO", 0xFFDD0000L, 0xFFFFFFFFL, "City Ground", 30445, 3, 120_000_000L, "393", 77,
            listOf("Sels", "Aina", "Milenkovic", "Murillo", "Moreno", "Yates", "Anderson", "Gibbs-White", "Elanga", "Chris Wood", "Hudson-Odoi", "Miguel", "Morato", "Domínguez", "Sosa", "Awoniyi")),
        makePL("crystal_palace", "Crystal Palace", "CRY", 0xFF1B458FL, 0xFFC4122EL, "Selhurst Park", 25486, 3, 105_000_000L, "384", 75,
            listOf("Henderson", "Muñoz", "Guéhi", "Lacroix", "Mitchell", "Wharton", "Lerma", "Kamada", "Sarr", "Mateta", "Eze", "Turner", "Richards", "Doucouré", "Schlupp", "Nketiah")),
        makePL("everton", "Everton", "EVE", 0xFF003399L, 0xFFFFFFFFL, "Goodison Park", 39414, 3, 100_000_000L, "368", 74,
            listOf("Pickford", "Young", "Tarkowski", "Branthwaite", "Mykolenko", "Gueye", "Mangala", "Doucouré", "McNeil", "Calvert-Lewin", "Ndiaye", "Virgínia", "O'Brien", "Garner", "Harrison", "Beto")),
        makePL("wolves", "Wolverhampton", "WOL", 0xFFFDB913L, 0xFF231F20L, "Molineux", 31750, 3, 95_000_000L, "380", 74,
            listOf("José Sá", "Semedo", "Bueno", "Toti", "Ait-Nouri", "André", "João Gomes", "Lemina", "Sarabia", "Matheus Cunha", "Strand Larsen", "Johnstone", "Dawson", "Doyle", "Bellegarde", "Hwang")),
        makePL("leicester", "Leicester City", "LEI", 0xFF003090L, 0xFFFDBE11L, "King Power", 32261, 3, 85_000_000L, "375", 73,
            listOf("Hermansen", "Justin", "Faes", "Vestergaard", "Kristiansen", "Ndidi", "Winks", "Buonanotte", "Fatawu", "Jamie Vardy", "Mavididi", "Ward", "Coady", "Skipp", "El Khannouss", "Daka")),
        makePL("ipswich", "Ipswich Town", "IPS", 0xFF3A64A3L, 0xFFFFFFFFL, "Portman Road", 30017, 2, 70_000_000L, "373", 71,
            listOf("Muric", "Tuanzebe", "O'Shea", "Greaves", "Davis", "Morsy", "Cajuste", "Hutchinson", "Burns", "Liam Delap", "Szmodics", "Walton", "Woolfenden", "Phillips", "Clarke", "Broadhead")),
        makePL("southampton", "Southampton", "SOU", 0xFFD71920L, 0xFFFFFFFFL, "St Mary's", 32384, 2, 72_000_000L, "376", 71,
            listOf("Ramsdale", "Sugawara", "Harwood-Bellis", "Bednarek", "Walker-Peters", "Downes", "Mateus Fernandes", "Aribo", "Dibling", "Archer", "Armstrong", "McCarthy", "Stephens", "Ugochukwu", "Lallana", "Onuachu"))
    )

    private fun makePL(
        id: String, name: String, shortName: String,
        primaryHex: Long, secondaryHex: Long, stadium: String,
        capacity: Int, prestige: Int, budget: Long, espnId: String,
        baseOvr: Int, names: List<String>
    ): Club = makeLeagueClub(
        id = id, name = name, shortName = shortName, division = Division.PREMIER_LEAGUE,
        primaryHex = primaryHex, secondaryHex = secondaryHex, stadium = stadium,
        capacity = capacity, prestige = prestige, budget = budget, espnId = espnId,
        names = names, baseOverall = baseOvr, maxOverall = 90, minOverall = 68
    )
}
