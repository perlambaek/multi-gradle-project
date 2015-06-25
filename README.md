# multi-gradle-project

This is a gradle plugin, providing functionality for setting up dependencies on standalone gradle projects, e.g. if you have an API consisting of only interfaces, and another project implementing it. In our case we wanted to be able to build the API interfaces seperately in a jar file that we could distribute, but we also wanted to develope the API as we did the implementation, thus needing both the API and the implementation in the same project.

We also wanted to be able to switch on and off whether you needed the local copy of the API.

gradle currently has the support for module dependencies with the ```compile project(':project')``` syntax. We introduce a new type of dependency, gradleProject: ```compile gradleProject('project', 'subModule')``` for our needs here.

Currently the only IDE supported is Intellij Idea.

*Requirement for subproject wishing to be available for multi project inclusion (e.g. API project):*

```gradle
multiProject {
    export = true
}
```

Simply running "gradle build export" will then generated the needed files.

*Usage example for main project depending on subprojects (e.g. API impl project):*

In the project where you wish to include other gradle projects.
```gradle
multiProject {

    /* 
        The override file can override any properties set in this, very useful as this file is committed, 
        and the override file can be left uncommitted and ignored in VCS, 
        if only some developers require the local checkout of the projects 
    */
    overrideFile = file("$rootProject.projectDir/multiProjectOverride.gradle")

    //project dependency descriptor, becomes available as a dependency in build.gradle files with the following syntax:
    //gradleProject('api', 'subModuleFromApiProject')
    api {
        //denotes the directory where the source is checked out, must be a gradle project with a settings.gradle
        directory = file("${rootProject.projectDir}/../api")
        /*
            version and group available on configured repository, for anyone running useLocal = false. 
            gradle will look for a maven dependency specified as: 'some.group:api:0.1' given the data in this example.
        */
        version = '0.1'
        groupId = 'some.group'
        
        //whether gradle uses the locally checked out project, or uses the maven dependency.
        useLocal = false
    }

    'otherProject' {
        directory = file("${rootProject.projectDir}/../otherProject")
        version = '1.4-SNAPSHOT'
        groupId = 'some.group'
        useLocal = false
        //denotes whether submodules exists or not, requires the refered project to have a settings.gradle if set to true.
        hasSubModules = false
    }
}
```
