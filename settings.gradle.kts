pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "CustomBrowser"
include(":app", ":engine-html", ":engine-css", ":engine-js", ":engine-render", ":engine-net", ":adblock", ":privacy", ":search", ":media", ":storage", ":tools", ":diagnostics")
