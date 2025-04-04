package generator.lm

import generator.domain.Interface
import generator.domain.MapperContext
import generator.files.RemoteFileManager
import generator.lm.agent.DocumentationExplorerAgent
import generator.lm.agent.DocumentationRefinerAgent
import generator.lm.agent.DocumentationWriterAgent
import generator.lm.agent.JSonRefinerAgent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.nio.file.Path

private val prettyJson = Json {
    prettyPrint = true
}

class DocumentGeneratorManager(
    private val context: MapperContext,
    private val remoteFileManager: RemoteFileManager,
    htmlDocumentation: Path
) {
    private val body = Jsoup.parse(htmlDocumentation.toFile(), "UTF-8")
        ?: error("fail to parse html")

    private var currentDocumentation = emptyMap<String, String>()

    private val llmClient = LLMClient()
    private val documentationExplorerAgent = DocumentationExplorerAgent(llmClient)
    private val documentationWriterAgent = DocumentationWriterAgent(llmClient)
    private val jSonRefinerAgent = JSonRefinerAgent(llmClient)
    private val documentationRefinerAgent = DocumentationRefinerAgent(llmClient)

    val documentationFile = remoteFileManager.specificationsSourcePath.resolve(RemoteFileManager.Files.documentation)

    fun inferHtmlDocumentation() = runBlocking {
        currentDocumentation = getActualDocumentation()
        context.interfaces.forEach { kInterface ->
            runCatching {
                val expectedKeys = kInterface.getDocumentationKeys()
                if (currentDocumentation.keys.containsAll(expectedKeys)) return@forEach
                val name = kInterface.name.lowercase()
                val htmlNode = findRootNode(name) ?: error("fail to find root node for declaration $name")
                val htmlDocumentation = inferHtmlDocumentation(htmlNode, name)
                val kdocDocumentation = inferKdocDocumentation(kInterface, htmlDocumentation, expectedKeys)
                    .filterKeys { it in expectedKeys }
                currentDocumentation += kdocDocumentation
                val jsonString = prettyJson.encodeToString(currentDocumentation)
                java.nio.file.Files.write(documentationFile, jsonString.toByteArray())
            }
        }
    }

    private fun Interface.getDocumentationKeys(): List<String> {
        val prefix = name
        return listOf(name) +
                attributes.map { "$prefix#${it.name}" } +
                methods.map { "$prefix#${it.name}(${it.parameters.joinToString(", ") { it.name }})" }
    }

    private fun getActualDocumentation(): Map<String, String> = runCatching {
        java.nio.file.Files.readString(documentationFile)
            .let { prettyJson.decodeFromString<Map<String, String>>(it) }
    }.getOrElse { emptyMap<String, String>() }

    private suspend fun inferKdocDocumentation(
        kInterface: Interface,
        htmlDocumentation: MutableList<Element>,
        expectedKeys: List<String>
    ): Map<String, String> {
        val userPrompt = """
            Provide only the documentation for the kotlin code and skip the rest.
            This is the kotlin code :
            
            ```kotlin
            $kInterface
            ```
            
            We expect the following keys : 
            ```json
            {
                ${expectedKeys.joinToString(", ") { "\"$it\" : \"insert documentation here\""}}
            }
            ```
                    
            This is the HTML specification
            $htmlDocumentation
                       
            """.trimIndent()

        println("infer: $kInterface")


        var responseAsJson = documentationWriterAgent.generateDocumentation(userPrompt)
            .map { documentationRefinerAgent.refine(it).getOrThrow() }
            .getOrThrow()

        var remainingTry = 5
        while (remainingTry > 0) {
            remainingTry--
            runCatching {
                Json.Default.decodeFromString<Map<String, String>>(responseAsJson)
                    .let { return it }
            }
            responseAsJson = jSonRefinerAgent.refine(responseAsJson)
                .getOrThrow()
            println("refined json: $responseAsJson")
        }


        error("fail to get response")
    }

    private suspend fun inferHtmlDocumentation(htmlNode: Element, subject: String): MutableList<Element> {
        val selectedDocumentation = mutableListOf(htmlNode)

        // Parse before
        var shouldContinue = true
        var currentElement = htmlNode.previousElementSibling()
        /*println("Parse before $currentElement")
        while (shouldContinue && currentElement != null) {
            shouldContinue = documentationExplorerAgent.isRelevant(
                selectedDocumentation.toString(),
                currentElement.toString(),
                subject
            ).map { it.lowercase() == "yes" }.getOrElse { false }

            if (shouldContinue) {
                selectedDocumentation.addFirst(currentElement)
                currentElement = currentElement.previousElementSibling()
            }
            println("shouldContinue: $shouldContinue and currentElement is null ?: ${currentElement == null}")
        }*/
        // Parse after
        shouldContinue = true
        currentElement = htmlNode.nextElementSibling()
        while (shouldContinue && currentElement != null) {
            shouldContinue = documentationExplorerAgent.isRelevant(
                selectedDocumentation.toString(),
                currentElement.toString(),
                subject
            ).map { it.lowercase() == "yes" }.getOrElse { false }

            if (shouldContinue) {
                selectedDocumentation.add(currentElement)
                currentElement = currentElement.nextElementSibling()
            }
            println("shouldContinue: $shouldContinue")
        }

        return selectedDocumentation
    }

    private fun findRootNode(name: String): Element? = (body.select("dfn[id=dictdef-$name]").first()
        ?: body.select("dfn[id=typedefdef-$name]").first()
        ?: body.select("a[href=#$name]").first()
            )?.findRootNode()
}

private fun Element.findRootNode(): Element? = parent().let {
    when (it?.tagName()) {
        null, "main" -> this
        else -> it.findRootNode()
    }
}
