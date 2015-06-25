package dk.capgemini.gradle.plugin


class GradleMavenDependency {

    String name
    String group
    String version

    GradleMavenDependency(String name, String group, String version) {
        this.name = name
        this.group = group
        this.version = version
    }

    @Override
    public String toString() {
        return "$group:$name:$version"
    }
}
