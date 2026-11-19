package com.offlineai.app.engine.code

enum class CodeLanguage(
    val id: String,
    val displayName: String,
    val extension: String,
    val defaultFileName: String,
    val emoji: String,
    val starterTemplate: String
) {
    JAVASCRIPT(
        id = "javascript",
        displayName = "JavaScript",
        extension = "js",
        defaultFileName = "script.js",
        emoji = "⚡",
        starterTemplate = """// Maphula Claude Code / Codex On-Device Sandbox
function fibonacci(n) {
    if (n <= 1) return n;
    let a = 0, b = 1;
    for (let i = 2; i <= n; i++) {
        let temp = a + b;
        a = b;
        b = temp;
    }
    return b;
}

console.log("Fibonacci(12):", fibonacci(12));
console.log("Environment: 100% Offline Sovereign V8/QuickJS Simulation");
"""
    ),
    PYTHON(
        id = "python",
        displayName = "Python",
        extension = "py",
        defaultFileName = "main.py",
        emoji = "🐍",
        starterTemplate = """# Maphula Claude Code / Codex Engine
def bubble_sort(arr):
    n = len(arr)
    for i in range(n):
        for j in range(0, n - i - 1):
            if arr[j] > arr[j + 1]:
                arr[j], arr[j + 1] = arr[j + 1], arr[j]
    return arr

sample = [64, 34, 25, 12, 22, 11, 90]
print("Sorted Array:", bubble_sort(sample))
print("Executed locally on-device without cloud connectivity.")
"""
    ),
    HTML_CSS(
        id = "html",
        displayName = "HTML / Web",
        extension = "html",
        defaultFileName = "index.html",
        emoji = "🌐",
        starterTemplate = """<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<style>
  body { background: #0b0f19; color: #38bdf8; font-family: monospace; padding: 24px; text-align: center; }
  h1 { font-size: 22px; color: #a78bfa; margin-bottom: 8px; }
  .box { border: 1px solid #1e293b; background: #131b2e; padding: 16px; border-radius: 12px; }
  button { background: #38bdf8; color: #0f172a; border: none; padding: 8px 16px; border-radius: 8px; font-weight: bold; }
</style>
</head>
<body>
<div class="box">
  <h1>⚡ Claude Code Live Preview</h1>
  <p>Rendered locally via Maphula Offline Engine.</p>
  <button onclick="alert('Hello from sovereign code sandbox!')">Test Interaction</button>
</div>
</body>
</html>
"""
    ),
    KOTLIN(
        id = "kotlin",
        displayName = "Kotlin",
        extension = "kt",
        defaultFileName = "Solution.kt",
        emoji = "🟣",
        starterTemplate = """// Maphula Claude Code Architecture
data class AgentTask(val id: String, val goal: String, val completed: Boolean = false)

fun main() {
    val tasks = listOf(
        AgentTask("task-1", "Inspect AST & Syntax", true),
        AgentTask("task-2", "Generate Vector Embeddings", true),
        AgentTask("task-3", "Simulate Local Compilation", true)
    )
    println("Active Codex Plan:")
    tasks.forEach { println(" [x] ${'$'}{it.id}: ${'$'}{it.goal}") }
}
"""
    ),
    SQL(
        id = "sql",
        displayName = "SQL",
        extension = "sql",
        defaultFileName = "query.sql",
        emoji = "🗄️",
        starterTemplate = """-- Maphula Sovereign Offline Knowledge Query
CREATE TABLE bots (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    role TEXT NOT NULL,
    status TEXT DEFAULT 'active'
);

INSERT INTO bots (name, role) VALUES ('Shiva', 'Code Buddy');
INSERT INTO bots (name, role) VALUES ('Brahma', 'Science');

SELECT name, role FROM bots WHERE status = 'active';
"""
    )
}

enum class CodeAgentMode(
    val title: String,
    val icon: String,
    val description: String
) {
    AUTONOMOUS_CODER("Autonomous Coder", "🤖", "Plans, generates, and formats complete solutions like Claude Code & Codex"),
    DEBUGGER("Deep Debugger", "🐛", "Detects syntax errors, logic flaws, memory leaks & security invariants"),
    REFACTOR("Refactor & Optimize", "⚡", "Modernizes architecture, reduces algorithmic complexity, and adds clean docs"),
    TEST_GENERATOR("Test Suite Builder", "🧪", "Generates unit test cases, boundary assertions, and mock coverage"),
    EXPLAINER("Code Explainer", "💡", "Deconstructs execution flow, data structures, and edge cases step-by-step")
}

data class CodeExecutionResult(
    val success: Boolean,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val durationMs: Long,
    val lineCount: Int,
    val complexityScore: Int,
    val issuesDetected: List<CodeLintIssue> = emptyList(),
    val renderedHtml: String? = null
)

data class CodeLintIssue(
    val line: Int,
    val severity: String, // "ERROR", "WARNING", "INFO"
    val message: String,
    val suggestion: String
)

data class CodeProjectFile(
    val fileName: String,
    val language: CodeLanguage,
    var content: String,
    val timestamp: Long = System.currentTimeMillis()
)
