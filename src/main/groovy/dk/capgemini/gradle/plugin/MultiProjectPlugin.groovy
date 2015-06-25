package dk.capgemini.gradle.plugin

import groovy.xml.MarkupBuilder
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.internal.DefaultDomainObjectSet
import org.gradle.api.internal.artifacts.DefaultDependencySet
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.internal.artifacts.dependencies.DefaultSelfResolvingDependency
import org.gradle.api.tasks.GradleBuild

class MultiProjectPlugin implements Plugin<Project> {
    Project project
    MultiProjectConvention convention
    static Map<GString, DependencySet> dependencyMap = [:]

    static def convertToPath(String gradleProjectPath, delimiter, drop) {
        String[] splitted = gradleProjectPath.split(":")
        splitted = splitted.dropWhile { String s -> s.isEmpty() }.drop(drop)
        return splitted.join(delimiter)
    }

    @Override
    void apply(Project project) {
        this.project = project

        convention = new MultiProjectConvention(project)

        project.ext.gradleProject = getDevelopmentDependency(project, true, true);
        project.ext.gradleProjectJarOnly = getDevelopmentDependency(project, true, false);
        project.ext.gradleProjectTransitiveOnly = getDevelopmentDependency(project, false, true);

        project.convention.plugins.multiProject = convention

        convention.whenConfigured {
            convention.projects.each {
                GradleProjectDescriptor descriptor ->
                    if (descriptor.useLocal) {
                        def subModules = descriptor.getSubModules()

                        if (project.rootProject == project) {
                            project.idea.project.ipr.withXml {
                                def root = it.asNode()
                                def moduleManagerNode = root.component.find { it.@name == 'ProjectModuleManager' }.modules[0]

                                def dir = descriptor.directory.toString().replace('\\', '/')

                                File mainImlFile = null
                                descriptor.directory.eachFile {
                                    if (it.name.endsWith(".iml")) {
                                        mainImlFile = it
                                    }
                                }

                                new Node(moduleManagerNode, 'module', [fileurl: "file://${dir}/${mainImlFile.name}", filepath: "${dir}/${mainImlFile.name}"])
                            }
                            addIdeaModules(subModules, descriptor)
                        }

                        subModules.each {
                            String subModule ->
                                addExportTask(subModule, descriptor)
                        }
                    }
            }
            if (convention.export) {
                println "adding export"
                project.allprojects {
                    Project subProject ->
                        println " on project $subProject"
                        subProject.task('exportDefaultConfiguration') {
                            File dependencies = new File("${subProject.buildDir}/export/", "exported.dep")

                            inputs.files subProject.configurations.default
                            outputs.file dependencies

                            doLast {
                                StringWriter stringWriter = new StringWriter()
                                MarkupBuilder xml = new MarkupBuilder(stringWriter)
                                new File("${subProject.buildDir}/export/").mkdirs()
                                if (!dependencies.exists()) {
                                    dependencies.createNewFile()
                                }
                                dependencies.text = ''

                                List<GradleMavenDependency> transitiveMavenDependencies = []
                                List<GradleProjectDependency> transitiveProjectDependencies = []

                                configurations.default.allDependencies.each {
                                    if (it instanceof ExternalModuleDependency) {
                                        ExternalModuleDependency externalModuleDependency = it;
                                        transitiveMavenDependencies << new GradleMavenDependency(externalModuleDependency.name, externalModuleDependency.group, externalModuleDependency.version)
                                    } else if (it instanceof ProjectDependency) {
                                        ProjectDependency projectDependency = it
                                        transitiveProjectDependencies << new GradleProjectDependency(projectDependency.getDependencyProject().jar.archivePath)
                                    } else if (it instanceof DefaultSelfResolvingDependency) {
                                        DefaultSelfResolvingDependency selfResolvingDependency = it
                                        it.source.each {
                                            transitiveProjectDependencies << new GradleProjectDependency(it)
                                        }
                                    } else {
                                        println it.getClass()
                                        System.exit(0)
                                    }
                                }

                                xml.root {
                                    transitive {
                                        maven {
                                            transitiveMavenDependencies.each {
                                                mavenDep ->
                                                    dependency(name: mavenDep.name, group: mavenDep.group, version: mavenDep.version)
                                            }
                                        }
                                        otherProject {
                                            transitiveProjectDependencies.each {
                                                projectDep ->
                                                    dependency(path: projectDep.path)
                                            }
                                        }
                                    }
                                    projectJar {
                                        dependency(path: subProject.jar.archivePath)

                                    }
                                }

                                dependencies << stringWriter.toString()
                            }

                        }
                }
            }
        }
    }

    private void addExportTask(String subModule, GradleProjectDescriptor descriptor) {
        if (project.rootProject.tasks.findByName("multiProject${descriptor.name}${subModule}".toString()) == null) {
            String prefix = ''
            if (descriptor.hasSubModules) {
                prefix = "${subModule}:"
            }
            project.rootProject.task("multiProject${descriptor.name}${subModule}".toString(), type: GradleBuild) {
                buildFile = new File(descriptor.directory, 'build.gradle')
                dir = descriptor.directory
                tasks = ["${prefix}exportDefaultConfiguration".toString(), "${prefix}build".toString()]
            }
        }
    }

    private void addIdeaModules(List<String> subModules, descriptor) {
        subModules.each {
            String subModule ->
                def name = convertToPath(subModule, '', 0)
                project.idea.project.ipr.withXml {
                    def root = it.asNode()
                    def moduleManagerNode = root.component.find { it.@name == 'ProjectModuleManager' }.modules[0]

                    def dir = descriptor.directory.toString().replace('\\', '/')
                    new Node(moduleManagerNode, 'module', [fileurl: "file://${dir}/$name/${name}.iml", filepath: "${dir}/$name/${name}.iml"])
                }
        }

        if (project.rootProject.tasks.findByName("multiProjectIdea${descriptor.name}".toString()) == null) {
            project.rootProject.task("multiProjectIdea${descriptor.name}".toString(), type: GradleBuild) {
                buildFile = new File(descriptor.directory, 'build.gradle')
                dir = descriptor.directory
                tasks = ['exportDefaultConfiguration', 'idea']
            }
        }

        project.rootProject.tasks.getByName('ideaProject').dependsOn("multiProjectIdea${descriptor.name}".toString())
    }

//gradleProject('api', 'general')
    private Closure<DefaultSelfResolvingDependency> getDevelopmentDependency(Project project, boolean includeJar, boolean includeTransitive) {
        {
            String projectName, String module ->
                //check map for cached version, key = projectName:module
                def key = "${projectName}__${module}__${includeJar}__${includeTransitive}"
                project.logger.info("resolving dependencies for multiproject: $key")

                GradleProjectDescriptor descriptor = getDescriptor((projectName))
                if (descriptor.useLocal) {
                    List<File> jars = []

                    DefaultDependencySet dependencySet = dependencyMap[key]
                    if (!dependencySet) {
                        dependencySet = new DefaultDependencySet("multiProject_${projectName}_${module}", new DefaultDomainObjectSet<Dependency>(Dependency.class))

                        def file = new File(getDirectory(projectName), "$module/build/export/exported.dep")
                        def slurped = new XmlSlurper().parse(file)

                        if (includeTransitive) {
                            slurped.transitive.maven.children().each {
                                dependencySet.add(new DefaultExternalModuleDependency(it.@group as String, it.@name as String, it.@version as String))
                            }
                            slurped.transitive.otherProject.children().each {
                                otherProjectJar ->
                                    String path = otherProjectJar.@path
                                    jars << new File(path)
                                    dependencySet.add(new DefaultSelfResolvingDependency(new LazyFileCollection(project, {
                                        new File(path)
                                    }, "multiProject$projectName$module".toString())))
                            }
                        }

                        if (includeJar) {
                            slurped.projectJar.children().each {
                                projectJar ->
                                    String path = projectJar.@path
                                    jars << new File(path)
                                    dependencySet.add(new DefaultSelfResolvingDependency(new LazyFileCollection(project, {
                                        new File(path)
                                    }, "multiProject$projectName$module".toString())))
                            }
                        }
                        dependencyMap[key] = dependencySet
                    }

                    project.idea.module.iml.withXml {
                        def root = it.asNode()

                        def jarFiles = jars.collect { it.absolutePath }
                        jarFiles.take(jarFiles.size() - 1).each {
                            String jar ->
                                String[] split = jar.split("\\\\")
                                def replacedJar = split.drop(split.length - 5).join('/')

                                IdeaXmlUtil.removeJarDependency(root, replacedJar)
                        }
                        println "looking to replace $projectName/$module with $module"
                        if (descriptor.hasSubModules) {
                            IdeaXmlUtil.replaceJarWithModuleDependency(root, "$projectName/$module", module)
                        } else {
                            IdeaXmlUtil.replaceJarWithModuleDependency(root, "$module", module)
                        }
                    }
                    return dependencySet
                } else {
                    return "${descriptor.groupId}:$module:${descriptor.version}"
                }
        }
    }

    private String getDirectory(String projectName) {
        GradleProjectDescriptor descriptor = getDescriptor(projectName)
        if (descriptor.hasSubModules) {
            return descriptor.directory
        } else {
            return descriptor.directory.parent
        }
    }

    private GradleProjectDescriptor getDescriptor(String projectName) {
        GradleProjectDescriptor descriptor = convention.projects.find {
            it.name == projectName
        }
        descriptor
    }
}