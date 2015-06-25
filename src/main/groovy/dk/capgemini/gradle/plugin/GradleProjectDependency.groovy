package dk.capgemini.gradle.plugin


class GradleProjectDependency {
    File path

    GradleProjectDependency(File path) {
        this.path = path
    }


    @Override
    public String toString() {
        return "projectDependency: $path"
    }
}
