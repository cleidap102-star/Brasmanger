package com.example.data.seed

import com.example.domain.model.Club
import com.example.domain.model.Division

object SerieDSeedData {
    fun clubs(): List<Club> = listOf(
        makeD("santa_cruz", "Santa Cruz", "STA", 0xFFDC2626L, 0xFF111111L, "Arruda", 60044, 3, 7_500_000L,
            listOf("Rokenedy", "Toty", "William Alves", "Matheus Vinícius", "Rodrigues", "Wagner Balotelli", "Lucas Siqueira", "João Pedro", "Thiaguinho", "Gilvan", "Vini Moura", "Moisés", "Yuri Ferraz", "Genílson", "Henrique Lordelo", "Jhonatan")),
        makeD("portuguesa", "Portuguesa", "POR", 0xFFDC2626L, 0xFF006B3FL, "Canindé", 21004, 2, 7_000_000L,
            listOf("Rafael Santos", "Talles", "Gustavo Henrique", "Eduardo Biazus", "Lucas Hipólito", "Tauã", "Hudson", "Cristiano", "Jajá", "Maceió", "Everton", "Bruno Bertinato", "Sciencia", "Maurício", "Daniel Júnior", "Henrique")),
        makeD("america_rn", "América-RN", "ARN", 0xFFB91C1CL, 0xFFFFFFFFL, "Arena das Dunas", 31375, 2, 6_500_000L,
            listOf("Renan Bragança", "Ricardo Luz", "Lucas Rodrigues", "Guilherme Paraíba", "Davi Gabriel", "Ferreira", "Mikael", "Souza", "Thailor", "Luidy", "Giva", "Gabriel Bernard", "Lucas Mendes", "Iran", "Ítalo", "Hebert")),
        makeD("brasil_pelotas", "Brasil de Pelotas", "BRA", 0xFFDC2626L, 0xFF111111L, "Bento Freitas", 18000, 2, 5_500_000L,
            listOf("Gabriel Oliveira", "Murillo", "Guilherme Cerqueira", "Áquila", "Alan", "Juliano", "Kauê", "Higor", "Mayco", "Giovani", "Sorriso", "Wellington", "Danilo", "Bruno", "Rael", "Matheus")),
        makeD("joinville", "Joinville", "JEC", 0xFFDC2626L, 0xFF111111L, "Arena Joinville", 22400, 2, 6_000_000L,
            listOf("Bruno Pianissolla", "Danilo Belão", "Naldinho", "Léo Rigo", "Ernandes", "Yalle", "Breno Santos", "Lucas de Sá", "Julio Cesar", "Cristian", "Keké", "Bazílio", "Carlos Alexandre", "Guti", "Vini Paulista", "Marcos Brazion")),
        makeD("parana_clube", "Paraná Clube", "PAR", 0xFF1D4ED8L, 0xFFDC2626L, "Vila Capanema", 20083, 2, 6_200_000L,
            listOf("Thiago Santos", "Daniel Guedes", "Lucas Ryan", "Diego Ivo", "Kevin", "Geilson", "Julio Rusch", "Gustavo Xuxa", "Diego Tavares", "Rikelmi", "Eduardo Tanque", "Gabriel Gasparotto", "Felix", "Yan", "Bruno", "Leandro")),
        makeD("sampaio_correa", "Sampaio Corrêa", "SAM", 0xFF16A34AL, 0xFFFACC15L, "Castelão-MA", 40149, 2, 6_800_000L,
            listOf("Alan", "Ray", "Fábio Aguiar", "Galvão", "Thiago Rosa", "Cavi", "Jair", "Alan Stence", "Pimentinha", "Alan James", "Verdão", "Rhuan", "Maurício", "Ítalo", "Balão", "João")),
        makeD("treze", "Treze", "TRE", 0xFF181818L, 0xFFFFFFFFL, "Amigão", 23000, 2, 5_200_000L,
            listOf("Igor Rayan", "Van", "Marcelo", "Renan Diniz", "Arthurzinho", "Karl", "Juninho", "Dione", "Rafael Ibiapino", "Gustavinho", "Jeam", "Andrade", "Guilherme", "Kiko", "Pedro", "Schutz")),
        makeD("asa_arapiraca", "ASA", "ASA", 0xFF181818L, 0xFFFFFFFFL, "Coaracy da Mata", 12500, 1, 4_800_000L,
            listOf("Matheus Vinicius", "Paulinho", "Cristian Lucca", "Zulu", "Charles", "Sousa Tibiri", "Fabrício Bigode", "Sammuel", "Keliton", "Júnior Viçosa", "Thiago Alagoano", "Jerfesson", "Carlos", "Thiago Papel", "Allefe", "Uelber")),
        makeD("manaus", "Manaus", "MAN", 0xFF006B3FL, 0xFFFFFFFFL, "Arena da Amazônia", 44000, 2, 5_900_000L,
            listOf("Gustavo", "Caio Hila", "Erik Henrique", "Keslley", "Miliano", "Zé Augusto", "Bombom", "Gabriel Masson", "Vitinho", "Renanzinho", "Lelê", "Vitor Luiz", "Jefferson", "Brenner", "Emerson", "Joel")),
        makeD("sergipe", "Sergipe", "SER", 0xFFDC2626L, 0xFFFFFFFFL, "Batistão", 15575, 1, 4_500_000L,
            listOf("Ravel", "Ronaldo", "Lucão", "Júnior Goiano", "Renan", "Adriano", "Pedro", "Tinga", "Léo Fernandes", "Wesley", "Bambam", "Igor", "Mateus", "Gabriel", "Claudinho", "Erick")),
        makeD("agua_santa", "Água Santa", "AGS", 0xFF0284C7L, 0xFFFFFFFFL, "Distrital Inamar", 10000, 2, 6_400_000L,
            listOf("Ygor Vinhas", "Ynaiã", "Fábio Sanches", "Rafael Vaz", "Renan Castro", "Netinho", "Villian", "Luan Dias", "Neílton", "Ademilson", "Robinho", "Matheus", "Guilherme", "Andrés", "Davi", "Neto")),
        makeD("inter_limeira", "Inter de Limeira", "INL", 0xFF181818L, 0xFFFFFFFFL, "Limeirão", 18000, 2, 6_000_000L,
            listOf("André Luiz", "Felipe Albuquerque", "Gui Mariano", "Roberto", "Leocovick", "Flávio", "Lucas Buchecha", "Albano", "Rhuan", "Pablo Thomaz", "Ruan", "Igo", "Eduardo", "Alysson", "Bernardo", "Roni")),
        makeD("portuguesa_rj", "Portuguesa-RJ", "PRJ", 0xFFDC2626L, 0xFF15803DL, "Luso-Brasileiro", 5044, 1, 5_000_000L,
            listOf("Douglas Borges", "Joazi", "Thomas Kayck", "Victor Pereira", "Marcus Vinícius", "Wellington Cézar", "Anderson Rosa", "Romarinho", "Lohan", "Elicley", "Joãozinho", "Vinicius", "Lucas", "Ian", "Hélio", "Miguel")),
        makeD("ceilandia", "Ceilândia", "CEI", 0xFF181818L, 0xFFFFFFFFL, "Abadião", 4000, 1, 4_200_000L,
            listOf("Sucuri", "Paulinho", "Badhuga", "Wallace", "Danrlei", "Lagoa", "Tarta", "Pedro Bambu", "Kennedy", "Clemente", "Felipe", "Elias", "Júlio", "Milla", "Nolasco", "Edson")),
        makeD("aparecidense", "Aparecidense", "APA", 0xFF1D4ED8L, 0xFFFFFFFFL, "Annibal Batista", 4800, 1, 4_600_000L,
            listOf("Matheus Alves", "David", "Vanderley", "Wellington", "Mário", "Dyego", "Enzo", "Higor", "Kaio", "Júlio César", "João Marcos", "Pedro", "Ivan", "Stéfano", "Rodrigues", "Breno")),
        makeD("cascavel", "FC Cascavel", "FCC", 0xFFFACC15L, 0xFF111111L, "Olímpico Regional", 28125, 1, 5_100_000L,
            listOf("Passarelli", "Libano", "Paulo", "Geovane", "Maranhão", "Garraty", "Alex", "Peçanha", "Josiel", "Rodrigo", "Lucas", "Jean", "Thiago", "Tailson", "Paulo Baya", "Victor")),
        makeD("iguatu", "Iguatu", "IGU", 0xFF1D4ED8L, 0xFFFFFFFFL, "Morenão", 3600, 1, 3_800_000L,
            listOf("Mauro Iguatu", "Alison", "Max", "Diguinho", "Fernando", "Guidio", "Anderson", "Pedrinho", "Otacílio", "Tiaguinho", "Caxito", "Geferson", "Wagner", "Léo", "Diego", "Thalison")),
        makeD("ferroviario_ce", "Ferroviário", "FAC", 0xFFDC2626L, 0xFF111111L, "Presidente Vargas", 20268, 2, 5_700_000L,
            listOf("Matheus Cavichioli", "Kauê", "Wesley", "Jeffão", "Pablo", "Willyam", "Gharib", "Janeudo", "Allanzinho", "Ciel", "Kiuan", "Sávio", "Igor", "Ramires", "João", "Bruno")),
        makeD("mixto", "Mixto", "MIX", 0xFF181818L, 0xFFFFFFFFL, "Dutrinha", 7000, 1, 4_000_000L,
            listOf("Ravel", "Joazi", "Pablo", "Daniel", "Vinícius", "Jair", "Raynan", "Geovani", "Dionathã", "Emerson", "Gabriel", "Lucas", "Felipe", "Otávio", "Juninho", "Caio"))
    )

    private fun makeD(
        id: String, name: String, shortName: String,
        primaryHex: Long, secondaryHex: Long, stadium: String,
        capacity: Int, prestige: Int, budget: Long, names: List<String>
    ): Club = makeLeagueClub(
        id = id, name = name, shortName = shortName, division = Division.SERIE_D,
        primaryHex = primaryHex, secondaryHex = secondaryHex, stadium = stadium,
        capacity = capacity, prestige = prestige, budget = budget, espnId = "",
        names = names, baseOverall = if (prestige >= 3) 64 else 61, maxOverall = 69, minOverall = 56
    )
}
