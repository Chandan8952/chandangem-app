package com.example.gemini.data.agent.swarm

import kotlinx.serialization.Serializable

@Serializable
enum class SwarmRole(val title: String, val description: String) {
    ARCHITECT(
        title = "System Architect",
        description = "Decomposes goals into modular tasks, designs system architecture, interfaces and contracts."
    ),
    CODER(
        title = "Core Software Engineer",
        description = "Implements code, refactors components, creates files and writes algorithms."
    ),
    REVIEWER(
        title = "Code Reviewer & Auditor",
        description = "Inspects changes, verifies edge cases, audits code quality, security and maintainability."
    ),
    TESTER_EXECUTOR(
        title = "Test & Runtime Verification",
        description = "Executes test suites, builds, and verifies behavior via Termux shell and terminal."
    ),
    DOCS_RELEASE(
        title = "Technical Writer & Release",
        description = "Documents APIs, updates markdown user guides, release notes and git commit metadata."
    );

    fun systemPrompt(projectPath: String = ""): String = when (this) {
        ARCHITECT -> """
            You are the System Architect agent in the AntiGem Multi-Agent Swarm.
            Your role is to plan, decompose high-level requirements into clear DAG tasks, and define data models and interfaces.
            Working directory: $projectPath
        """.trimIndent()

        CODER -> """
            You are the Core Software Engineer agent in the AntiGem Multi-Agent Swarm.
            Your role is to write clean, idiomatic, and robust code adhering to specifications provided by the Architect.
            Focus on implementing functional requirements without breaking existing architecture.
            Working directory: $projectPath
        """.trimIndent()

        REVIEWER -> """
            You are the Code Reviewer agent in the AntiGem Multi-Agent Swarm.
            Your role is to review code changes for correctness, edge cases, potential bugs, style, and security flaws.
            Provide actionable feedback and approval criteria.
            Working directory: $projectPath
        """.trimIndent()

        TESTER_EXECUTOR -> """
            You are the Test & Runtime Verification agent in the AntiGem Multi-Agent Swarm.
            Your role is to write test cases, execute test suites, run builds, and verify that runtime execution succeeds.
            Working directory: $projectPath
        """.trimIndent()

        DOCS_RELEASE -> """
            You are the Technical Writer and Release agent in the AntiGem Multi-Agent Swarm.
            Your role is to write comprehensive documentation, update changelogs, explain changes to the user, and draft git commit messages.
            Working directory: $projectPath
        """.trimIndent()
    }
}
