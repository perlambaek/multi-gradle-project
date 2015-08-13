package dk.capgemini.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.internal.file.FileCollectionInternal
import org.gradle.api.internal.file.FileSystemSubset
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.TaskDependency


class LazyFileCollection implements FileCollectionInternal {

    Closure fileProvider
    private Project project
    private FileCollection wrapped
    private String taskDependency

    LazyFileCollection(Project project, Closure fileProvider, String taskDependency) {
        this.taskDependency = taskDependency
        this.project = project
        this.fileProvider = fileProvider
    }

    @Override
    File getSingleFile() throws IllegalStateException {
        throw new IllegalStateException()
    }

    @Override
    Set<File> getFiles() {
        FileCollection files = wrapped()
        return files.getFiles()
    }

    private FileCollection wrapped() {
        if (wrapped == null) {
            wrapped = project.files(fileProvider())
        }
        wrapped
    }

    @Override
    boolean contains(File file) {
        return wrapped().contains(file)
    }

    @Override
    String getAsPath() {
        return wrapped().getAsPath()
    }

    @Override
    FileCollection plus(FileCollection filecollection) {
        return wrapped().plus(filecollection)
    }

    @Override
    FileCollection minus(FileCollection filecollection) {
        return wrapped().minus(filecollection)
    }

    @Override
    FileCollection filter(Closure closure) {
        return wrapped().filter {closure}
    }

    @Override
    FileCollection filter(Spec<? super File> spec) {
        return wrapped().filter(spec)
    }

    @Override
    Object asType(Class<?> class1) throws UnsupportedOperationException {
        return wrapped().asType(class1)
    }

    @Override
    FileCollection add(FileCollection filecollection) throws UnsupportedOperationException {
        return null
    }

    @Override
    boolean isEmpty() {
        return wrapped().isEmpty()
    }

    @Override
    FileCollection stopExecutionIfEmpty() throws StopExecutionException {
        return wrapped().stopExecutionIfEmpty()
    }

    @Override
    FileTree getAsFileTree() {
        return wrapped().getAsFileTree()
    }

    @Override
    void addToAntBuilder(Object obj, String s, FileCollection.AntType anttype) {

    }

    @Override
    Object addToAntBuilder(Object obj, String s) {
        return null
    }

    @Override
    TaskDependency getBuildDependencies() {
        return new TaskDependency() {
            @Override
            Set<? extends Task> getDependencies(Task task) {
                return [project.rootProject.tasks.getByName(taskDependency)] as Set<Task>
            }
        }
    }

    @Override
    Iterator<File> iterator() {
        return wrapped().iterator()
    }

    @Override
    void registerWatchPoints(FileSystemSubset.Builder builder) {

    }

    @Override
    String getDisplayName() {
        return "displayName"
    }
}
