package dk.capgemini.gradle.plugin

import java.util.regex.Matcher


class GradleProjectDescriptor {
    String name
    File directory
    String version
    String groupId
    boolean hasSubModules = true
    boolean useLocal = false

    def GradleProjectDescriptor(String name) {
        this.name = name
    }

    List<String> getSubModules() {
        if(hasSubModules) {
            return getModulesFromSettings("${directory}/settings.gradle")
        }
        return [name]
    }

    private List<String> getModulesFromSettings(String settingsPath) {
        List res = []
        new File(settingsPath).text.eachLine {
            if (it.trim().contains("include")) {
                Matcher foo = it =~ /include \'(.*)\'/
                if (foo.matches()) {
                    res << foo.group(1)
                }
            }
        }
        res
    }

    @Override
    public String toString() {
        return "GradleProjectDescriptor{" +
                "name='" + name + '\'' +
                ", directory='" + directory + '\'' +
                ", version='" + version + '\'' +
                ", groupId='" + groupId + '\'' +
                '}' + super.toString();
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        GradleProjectDescriptor that = (GradleProjectDescriptor) o

        if (name != that.name) return false

        return true
    }

    int hashCode() {
        return name.hashCode()
    }
}
