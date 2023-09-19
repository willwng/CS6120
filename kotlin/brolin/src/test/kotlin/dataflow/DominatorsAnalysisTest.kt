package dataflow

import TestFixture
import org.junit.jupiter.api.Test
import util.CFGProgram


class DominatorsAnalysisTest {

    private fun checkDominanceFrontier(cfgProgram: CFGProgram) {
        val dominanceFrontiers = DominatorsAnalysis.getDominanceFrontiers(cfgProgram)
    }

    @Test
    fun computeDominanceFrontier() {
        TestFixture.cfgPrograms.map(::checkDominanceFrontier)
    }

    @Test
    fun getDominatorTrees() {
    }

    @Test
    fun getDominatorsTest() {
    }

    @Test
    fun getDominanceFrontiers() {
    }
}