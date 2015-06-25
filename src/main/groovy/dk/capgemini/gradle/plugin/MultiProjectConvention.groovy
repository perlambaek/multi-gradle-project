package dk.capgemini.gradle.plugin

import org.gradle.api.Project

class MultiProjectConvention {
    List configureClosures = []
    Project project
    File overrideFile
    Set<GradleProjectDescriptor> projects = []
    boolean export = false
    Map<String, GradleProjectDescriptor> gradleProjectDescriptorMap = [:]

    MultiProjectConvention(Project project) {
        this.project = project
    }

    def multiProject(Closure closure) {
        closure.delegate = this
        closure.setResolveStrategy(Closure.DELEGATE_FIRST)

        closure()

        if (overrideFile != null && overrideFile.exists()) {
            GroovyShell shell = new GroovyShell(getClass().getClassLoader());
            Closure overrideClosure = shell.evaluate("{it -> ${overrideFile.text}}");
            overrideClosure.delegate = this
            overrideClosure.setResolveStrategy(Closure.DELEGATE_FIRST)
            overrideClosure()
        }

        configureClosures.each {
            it()
        }
    }

    void whenConfigured(Closure closure) {
        configureClosures.add(closure)
    }

    def methodMissing(String name, args) {
        GradleProjectDescriptor descriptor = gradleProjectDescriptorMap.get(name, new GradleProjectDescriptor(name))
        Closure closure = args[0];
        closure.setDelegate(descriptor)
        closure.setResolveStrategy(Closure.DELEGATE_FIRST)
        closure()

        projects << descriptor
        return null;
    }
}

