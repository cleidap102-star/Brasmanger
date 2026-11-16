package com.example.domain.engine

import com.example.data.seed.SerieASeedData
import com.example.domain.model.MonthlyLedgerRecord
import com.example.domain.model.TicketPriceLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceEngineTest {

    @Test
    fun buildMonthlyTrends_generates10MonthsAndUpdatesWithTicketPricingAndRealizedLedger() {
        val clubs = SerieASeedData.clubs()
        val club = clubs.first { it.id == "flamengo" }
        val fixtures = SeasonEngine.generateLeagueFixtures(clubs, club.division)

        val (_, popularRev) = FinanceEngine.estimateHomeMatchAttendanceAndRevenue(club, TicketPriceLevel.POPULAR)
        val (_, caroRev) = FinanceEngine.estimateHomeMatchAttendanceAndRevenue(club, TicketPriceLevel.CARO)

        val baselineTrends = FinanceEngine.buildMonthlyTrends(
            club = club,
            fixtures = fixtures,
            currentRound = 1,
            ticketPriceLevel = TicketPriceLevel.NORMAL,
            realizedLedger = emptyMap()
        )
        assertEquals(10, baselineTrends.size)
        assertEquals("Fev", baselineTrends.first().monthLabel)
        assertEquals("Nov", baselineTrends.last().monthLabel)
        assertTrue(baselineTrends.all { it.salaryExpense > 0L })

        val ledgerWithMonth0 = mapOf(
            0 to MonthlyLedgerRecord(
                monthIndex = 0,
                ticketIncome = 6_200_000L,
                salaryExpense = club.weeklyWages,
                otherDelta = 320_000L,
                roundsPlayed = 4,
                homeMatchesPlayed = 2,
                endingBudget = club.budget + 1_500_000L
            )
        )
        val afterMonth0 = FinanceEngine.buildMonthlyTrends(
            club = club,
            fixtures = fixtures,
            currentRound = 5,
            ticketPriceLevel = TicketPriceLevel.CARO,
            realizedLedger = ledgerWithMonth0
        )
        assertFalse(afterMonth0[0].isProjected)
        assertEquals(6_200_000L, afterMonth0[0].ticketIncome)
        assertTrue(caroRev != popularRev)
    }
}
