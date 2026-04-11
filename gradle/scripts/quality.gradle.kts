// Script plugin for quality orchestration

tasks.named("ktlintCheck") {
    group = "verification"
}

tasks.named("detekt") {
    group = "verification"
    mustRunAfter("detektBaseline")
}

tasks.register("qualityCheck") {
    group = "verification"
    description = "Combined quality gate: Lint and Detekt."
    dependsOn("ktlintCheck", "detekt")
}

tasks.named("check") {
    dependsOn("qualityCheck")
}
