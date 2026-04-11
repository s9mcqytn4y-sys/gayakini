// Minimal script plugin using task names for decoupling when types aren't easily visible
// Use 'named' and 'configure' with strings to avoid dependency on plugin classes in script scope

tasks.named("ktlintCheck") {
    group = "verification"
}

tasks.named("detekt") {
    group = "verification"
    mustRunAfter("detektBaseline")
}

tasks.register("qualityCheck") {
    group = "verification"
    description = "Combined quality gate: Lint and Detekt (Renamed from qualityGate for doc parity)."
    dependsOn("ktlintCheck", "detekt")
}

// Ensure standard 'check' task includes our qualityCheck
tasks.named("check") {
    dependsOn("qualityCheck")
}
