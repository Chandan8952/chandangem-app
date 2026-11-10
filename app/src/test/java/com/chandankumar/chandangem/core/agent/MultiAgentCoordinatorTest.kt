package com.chandankumar.chandangem.core.agent

import org.junit.Assert.assertEquals
import org.junit.Test

class MultiAgentCoordinatorTest {

    @Test
    fun testAgentRouter_routingRules() {
        val router = AgentRouter()

        // Terminal query
        val termAgent = router.route("run ls -la /sdcard")
        assertEquals(AgentType.TERMINAL_SPECIALIST, termAgent.type)

        // MCP query
        val mcpAgent = router.route("list mcp tools and registered servers")
        assertEquals(AgentType.MCP_TOOL_DISPATCHER, mcpAgent.type)

        // Architecture query
        val archAgent = router.route("explain kotlin compose architecture and gradle plugins")
        assertEquals(AgentType.CODE_ARCHITECT, archAgent.type)

        // General / Jarvis coordinator query
        val jarvisAgent = router.route("hello who are you?")
        assertEquals(AgentType.JARVIS_COORDINATOR, jarvisAgent.type)
    }

    @Test
    fun testAgentPresets_definitions() {
        val agents = Agent.ALL_AGENTS
        assertEquals(4, agents.size)

        val jarvis = Agent.JARVIS
        assertEquals("Jarvis", jarvis.name)
        assertEquals(AgentType.JARVIS_COORDINATOR, jarvis.type)
    }
}
