package com.example.data.seed

import com.example.domain.model.Club
import com.example.domain.model.Division

object SerieCSeedData {
    fun clubs(): List<Club> = listOf(
        makeC("nautico", "Náutico", "NAU", 0xFFD90429L, 0xFFFFFFFFL, "Aflitos", 22856, 3, 11_000_000L,
            listOf("Muriel", "Marcos Ytalo", "Rayann", "Carlinhos", "Luiz Paulo", "Sousa", "Marco Antônio", "Patrick Allan", "Bruno Mezenga", "Paulo Sérgio", "Hélio Borges", "Wellerson", "Arnaldo", "Índio", "Wenderson", "Kauan Maranhão")),
        makeC("guarani", "Guarani", "GUA", 0xFF006B3FL, 0xFFFFFFFFL, "Brinco de Ouro", 29130, 3, 12_000_000L,
            listOf("Gabriel Mesquita", "Lucas Justen", "Raphael", "Alan Santos", "Emerson Barbosa", "Nathan Camargo", "Mateus Sarará", "Anderson Leite", "João Victor", "Deni Júnior", "Matheus Régis", "Fred Conte", "Yan Henrique", "Titi", "Geovane", "Samuel")),
        makeC("ponte_preta", "Ponte Preta", "PON", 0xFF181818L, 0xFFFFFFFFL, "Moisés Lucarelli", 19728, 3, 12_500_000L,
            listOf("Diogo Silva", "Maguinho", "Sérgio Raphael", "Emerson Santos", "Danilo Barcelos", "Dudu", "Lucas Cândido", "Elvis", "Serginho", "Jeh", "Everton Brito", "Pedro Rocha", "Pacheco", "Vicente Concha", "Rodrigo Souza", "Renato")),
        makeC("figueirense", "Figueirense", "FIG", 0xFF1F2937L, 0xFFFFFFFFL, "Orlando Scarpelli", 19584, 3, 10_500_000L,
            listOf("Fabrício", "Léo Maia", "Lucas Dias", "Matheus Alves", "Samuel", "Jhony Douglas", "Breno", "Camilo", "Gabriel Santiago", "Kayke", "Marlyson", "Igo Gabriel", "Iury", "Ligger", "Rodrigo", "Nicolas")),
        makeC("ituano", "Ituano", "ITU", 0xFFDC2626L, 0xFF111111L, "Novelli Júnior", 18560, 2, 9_000_000L,
            listOf("Jefferson Paulino", "Thassio", "Erik", "Matheus Mancini", "Dal Pian", "Daniel", "Bruno Silva", "Yann Rolim", "Vinícius Popó", "Neto Berola", "Gabriel Razera", "Saulo", "Léo Oliveira", "Claudinho", "Kaio Mendes", "Zé Eduardo")),
        makeC("brusque", "Brusque", "BRU", 0xFFEAB308L, 0xFF006B3FL, "Augusto Bauer", 5000, 2, 9_200_000L,
            listOf("Matheus Nogueira", "Mateus Pivô", "Éverton Alemão", "Jhanpol", "Alex Ruan", "Serrato", "Rodolfo Potiguar", "Biel", "Guilherme Pira", "Pollero", "Diego Mathias", "Andrea", "Caio Gomes", "Ianson", "Paulista", "Olavio")),
        makeC("abc", "ABC", "ABC", 0xFF181818L, 0xFFFFFFFFL, "Frasqueirão", 15082, 2, 8_500_000L,
            listOf("Felipe Garcia", "Lucas Mota", "Windson", "Bruno Bispo", "Manoel", "Wellington Reis", "Adeílson", "Bruno Leite", "Wallyson", "Danilo Mariotto", "Emanuel", "Pedro Paulo", "Matheus Rocha", "Darlan", "Madison", "Islan")),
        makeC("csa", "CSA", "CSA", 0xFF1D4ED8L, 0xFFFFFFFFL, "Rei Pelé", 17126, 2, 9_000_000L,
            listOf("Georgemy", "Cedric", "Betão", "Wanderson", "Roberto", "Gustavo Nicola", "Vander", "Brayann", "Guilherme Cachoeira", "Tiago Marques", "Igor Bahia", "Gabriel Félix", "Raphinha", "Islan", "Buga", "Robinho")),
        makeC("confianca", "Confiança", "CON", 0xFF2563EBL, 0xFFFFFFFFL, "Batistão", 15575, 2, 7_500_000L,
            listOf("Rafael Mariano", "Weriton", "Maicon", "Eduardo Moura", "Airton", "Fábio", "André Lima", "Luiz Otávio", "Ronald Camarão", "Neto", "Breyner", "Felipe", "Valdir", "Renan", "Vitinho", "Rodriguinho")),
        makeC("londrina", "Londrina", "LON", 0xFF0284C7L, 0xFFFFFFFFL, "Café", 30000, 2, 9_500_000L,
            listOf("Luiz Daniel", "João Vitor", "Wallace", "Caio", "Maurício", "Alison", "Lucas Marques", "Gustavo França", "Henrique", "Iago Teles", "Pablo", "Elzo", "Cedric", "Yago Lincoln", "Paredes", "Robson")),
        makeC("sao_bernardo", "São Bernardo", "SBE", 0xFFFACC15L, 0xFF141414L, "1º de Maio", 12578, 2, 10_000_000L,
            listOf("Alex Alves", "Hugo Sanches", "Helder", "Augusto", "Pará", "Lucas Lima", "Romisson", "Foguinho", "Léo Jabá", "Rodolfo", "Fabrício Daniel", "Júnior Oliveira", "Rodrigo Ferreira", "Matheus Salustiano", "Emerson Santos", "Felipe Azevedo")),
        makeC("caxias", "Caxias", "CAX", 0xFF7F1D1DL, 0xFF1D4ED8L, "Centenário", 22132, 2, 8_200_000L,
            listOf("Thiago Coelho", "Thiago Ennes", "Lucas Cunha", "Alan", "Marcelo", "Pedro Cuiabá", "Vini Guedes", "Tomas Bastos", "Calyson", "Welder", "Gabriel Lima", "Victor Golas", "Ronei", "Alisson", "Mantuan", "Richard")),
        makeC("ypiranga_rs", "Ypiranga-RS", "YPI", 0xFFEAB308L, 0xFF006B3FL, "Colosso da Lagoa", 22000, 2, 7_200_000L,
            listOf("Edson", "Rian", "Heitor", "William Gomes", "Cadu", "Charles", "Lucas Ramos", "Jean Pyerre", "Galego", "Cristiano", "Emerson", "Zé Carlos", "Vitor Marinho", "Igor Morais", "João Paulo", "Roger")),
        makeC("botafogo_pb", "Botafogo-PB", "BPB", 0xFF111111L, 0xFFDC2626L, "Almeidão", 25770, 2, 8_800_000L,
            listOf("Wallace", "Erick", "Reniê", "Wendel Lomar", "Evandro", "Gama", "Thallyson", "Falcão", "Rafinha", "Henrique Dourado", "Danilo Alves", "Michael", "Lucas Lopes", "Balieiro", "Guilherme Santos", "Rodrigo Alves")),
        makeC("tombense", "Tombense", "TOM", 0xFFDC2626L, 0xFFFFFFFFL, "Almeidão Tombos", 6555, 2, 7_800_000L,
            listOf("Matheus Aurélio", "Júlio Henrique", "Roger Carvalho", "Rony", "Dudu Mandai", "João Vitor", "Fabrício Dias", "Pedro Oliveira", "Jefferson Renan", "Douglas Coutinho", "Anderson Ligeiro", "Douglas", "Iury", "Breno", "Lucas Silva", "Rafael Silva")),
        makeC("floresta", "Floresta", "FLO", 0xFF15803DL, 0xFFFFFFFFL, "Domingão", 10500, 1, 6_200_000L,
            listOf("Dalton", "Mateus Ludke", "Ícaro", "Vitão", "Furlan", "Pablo", "Marcelo", "Ruan", "Jeam", "Romarinho", "Rafael", "Wilderk", "Diego Matos", "Julio Vaz", "Guilherme", "Thiago Alagoano")),
        makeC("maringa", "Maringá", "MAR", 0xFF16A34AL, 0xFF111111L, "Willie Davids", 16226, 2, 8_400_000L,
            listOf("Dheimison", "Raphinha", "Ronald", "Tito", "Max Miller", "Rodrigo", "Zé Vitor", "Léo Ceará", "Negueba", "Maranhão", "Moraes", "Rafael William", "Cristovam", "Cauã Tavares", "Buga", "Julio")),
        makeC("retro", "Retrô", "RET", 0xFF0F172AL, 0xFF38BDF8L, "Arena Pernambuco", 44300, 2, 9_800_000L,
            listOf("Volpi", "Gedeilson", "Dankler", "Rayne", "Salomão", "Jonas", "Radsley", "Richard Franco", "Fernandinho", "Mascote", "Franklin", "Paulo Ricardo", "Sávio", "Denilson", "Fabinho", "Fialho")),
        makeC("anapolis", "Anápolis", "ANA", 0xFFDC2626L, 0xFF1D4ED8L, "Jonas Duarte", 20000, 1, 6_000_000L,
            listOf("Paulo Henrique", "Fábio", "Lucão", "Igor", "Caxambu", "André", "Pedro Thomaz", "Ariel", "Kadu", "Cardoso", "João Celeri", "Gabriel", "Rubinho", "Victor", "Rafael Mineiro", "Matheus Lagoa")),
        makeC("itabuna_itabaiana", "Itabaiana", "ITA", 0xFF1D4ED8L, 0xFFDC2626L, "Etelvino Mendonça", 11224, 1, 5_800_000L,
            listOf("Jefferson", "Chiquinho", "Gabriel Caran", "Bruno Sena", "Kevin", "Coppetti", "Romeu", "Tarcísio", "Leílson", "Jackson", "Wendel", "Léo", "Breno", "Thiago", "Pedro", "Matheus"))
    )

    private fun makeC(
        id: String, name: String, shortName: String,
        primaryHex: Long, secondaryHex: Long, stadium: String,
        capacity: Int, prestige: Int, budget: Long, names: List<String>
    ): Club = makeLeagueClub(
        id = id, name = name, shortName = shortName, division = Division.SERIE_C,
        primaryHex = primaryHex, secondaryHex = secondaryHex, stadium = stadium,
        capacity = capacity, prestige = prestige, budget = budget, espnId = "",
        names = names, baseOverall = if (prestige >= 3) 68 else 65, maxOverall = 73, minOverall = 60
    )
}
