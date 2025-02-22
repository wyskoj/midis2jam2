import io.gitlab.arturbosch.detekt.DetektPlugin
import io.gitlab.arturbosch.detekt.extensions.DetektExtension

plugins {
    id("io.gitlab.arturbosch.detekt") version "1.23.8" apply false
}
allprojects {
    apply<DetektPlugin>()
    configure<DetektExtension> {
        config.setFrom("$rootDir/detekt.yml")
        buildUponDefaultConfig = true
    }
}