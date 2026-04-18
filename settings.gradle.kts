import org.apache.tools.ant.taskdefs.condition.Os

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "Luanalysis"

if (Os.isFamily(Os.FAMILY_WINDOWS)) {
    include("debugger:attach:windows")
}
