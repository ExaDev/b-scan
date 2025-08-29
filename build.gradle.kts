plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// Shared commit-msg hook content for DRY principle
val commitMsgHookContent = """#!/bin/sh
# Validate conventional commit format
# This hook is automatically installed by Gradle

# Skip validation for merge commits
if grep -qE '^Merge (branch|remote-tracking branch|pull request)' "${'$'}1"; then
    exit 0
fi

commit_regex='^(feat|fix|docs|style|refactor|perf|test|chore)(\(.+\))?: .{1,50}'

if ! grep -qE "${'$'}commit_regex" "${'$'}1"; then
    echo ""
    echo "❌ Commit message does not follow conventional commit format!"
    echo ""
    echo "Expected format: type(scope): description"
    echo ""
    echo "Types: feat, fix, docs, style, refactor, perf, test, chore"
    echo "Scope: optional, e.g. (nfc), (ui), (decoder)"
    echo ""
    echo "Examples:"
    echo "  feat(nfc): add support for new tag format"
    echo "  fix(decoder): handle corrupted data gracefully"
    echo "  docs: update README installation steps"
    echo ""
    echo "Your commit message:"
    echo "${'$'}(cat "${'$'}1")"
    echo ""
    exit 1
fi
"""

// Automatically install git hooks when gradle syncs
tasks.register("installGitHooks") {
    description = "Install git hooks for conventional commits"
    group = "git"
    
    doLast {
        val gitDir = file(".git")
        if (!gitDir.exists()) {
            logger.info("Skipping git hooks installation - not a git repository")
            return@doLast
        }
        
        val hooksDir = file(".git/hooks")
        hooksDir.mkdirs()
        
        // Create commit-msg hook for validation
        val commitMsgHook = file("$hooksDir/commit-msg")
        commitMsgHook.writeText(commitMsgHookContent)
        commitMsgHook.setExecutable(true)
        
        // Configure git to use the commit template for guidance
        try {
            val gitConfigProcess = ProcessBuilder("git", "config", "commit.template", ".gitmessage")
                .directory(rootProject.projectDir)
                .start()
            gitConfigProcess.waitFor()
        } catch (e: Exception) {
            logger.warn("Could not configure git commit template: ${e.message}")
        }
        
        println("✅ Git hooks installed successfully!")
        println("✅ Commit template configured for interactive guidance!")
        println("Conventional commit format will now be enforced.")
    }
}

// Automatically run when gradle configures (only if .git exists)
if (file(".git").exists()) {
    gradle.projectsEvaluated {
        // Run the install task immediately after configuration
        val installTask = tasks.named("installGitHooks").get()
        
        // Execute directly in doLast block to avoid configuration cache issues
        val gitDir = file(".git")
        if (gitDir.exists()) {
            val hooksDir = file(".git/hooks")
            hooksDir.mkdirs()
            
            // Create commit-msg hook for validation
            val commitMsgHook = file("$hooksDir/commit-msg")
            commitMsgHook.writeText(commitMsgHookContent)
            commitMsgHook.setExecutable(true)
            
            // Configure git to use the commit template for guidance
            try {
                val gitConfigProcess = ProcessBuilder("git", "config", "commit.template", ".gitmessage")
                    .directory(projectDir)
                    .start()
                gitConfigProcess.waitFor()
            } catch (e: Exception) {
                println("Could not configure git commit template: ${e.message}")
            }
            
            println("✅ Git hooks auto-installed during Gradle sync!")
            println("✅ Commit template configured for interactive guidance!")
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}