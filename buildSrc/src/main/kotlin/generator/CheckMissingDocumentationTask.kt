package generator

import generator.files.RemoteFileManager
import generator.lm.getActualDocumentation
import generator.lm.getDocumentationKeys
import generator.tasks.ModelGenerator
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class CheckMissingDocumentationTask : DefaultTask() {

    init {
        group = "generator"
    }

    @TaskAction
    fun launch() = runBlocking {
        val remoteFileManager = RemoteFileManager(project.projectDir.toPath())
        val context = ModelGenerator(remoteFileManager).context
        val documentationFile =
            remoteFileManager.specificationsSourcePath.resolve(RemoteFileManager.Files.documentationJson)
        val currentDocumentation = getActualDocumentation(documentationFile)
        val missingKeys = context.interfaces
            .map { it to it.getDocumentationKeys() }
            .filter { (_, expectedKeys) -> expectedKeys.filter { it in currentDocumentation.keys }.size != expectedKeys.size } +
                context.commonEnumerations
                    .map { it to it.getDocumentationKeys() }
                    .filter { (_, expectedKeys) -> expectedKeys.filter { it in currentDocumentation.keys }.size != expectedKeys.size }

        println("Missing keys:\n${missingKeys.flatMap { it.second }}")
    }
}