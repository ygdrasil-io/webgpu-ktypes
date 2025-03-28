package lm

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
class OpenAIClient(
    private val baseUrl: String = "http://127.0.0.1:1234/v1",
    private val apiKey: String? = null
) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
    }

    // Méthode pour les appels standards (non streaming)
    suspend fun chatCompletion(
        messages: List<Message>,
        model: String = "mistral-small-3.1-24b-instruct-2503",
        maxTokens: Int = 150,
        temperature: Double = 0.7,
        topP: Double = 1.0,
        n: Int = 1,
        stop: List<String>? = null,
        presencePenalty: Double = 0.0,
        frequencyPenalty: Double = 0.0,
        user: String? = null
    ): ChatCompletionResponse {
        val request = ChatCompletionRequest(
            model = model,
            messages = messages,
            max_tokens = maxTokens,
            temperature = temperature,
            top_p = topP,
            n = n,
            stream = false,
            stop = stop,
            presence_penalty = presencePenalty,
            frequency_penalty = frequencyPenalty,
            user = user
        )

        return client.post("$baseUrl/chat/completions") {
            contentType(ContentType.Application.Json)
            apiKey?.let { header("Authorization", "Bearer $it") }
            setBody(request)
        }.body()
    }

    // Méthode pratique pour les cas simples
    suspend fun generateCompletion(
        prompt: String,
        model: String = "mistral-small-3.1-24b-instruct-2503",
        maxTokens: Int = 150
    ): String {
        val messages = listOf(Message("user", prompt))
        val response = chatCompletion(
            messages = messages,
            model = model,
            maxTokens = maxTokens
        )
        return response.choices.firstOrNull()?.message?.content ?: "Pas de réponse"
    }

    // Méthode pour le streaming
    fun streamChatCompletion(
        messages: List<Message>,
        model: String = "mistral-small-3.1-24b-instruct-2503",
        maxTokens: Int = 150,
        temperature: Double = 0.7,
        topP: Double = 1.0
    ): Flow<String> = flow {
        val request = ChatCompletionRequest(
            model = model,
            messages = messages,
            max_tokens = maxTokens,
            temperature = temperature,
            top_p = topP,
            stream = true
        )

        val response = client.preparePost("$baseUrl/chat/completions") {
            contentType(ContentType.Application.Json)
            apiKey?.let { header("Authorization", "Bearer $it") }
            setBody(request)
        }.execute()

        response.bodyAsText().lineSequence()
            .filter { it.startsWith("data:") && it.contains("{") }
            .map { it.removePrefix("data:").trim() }
            .filter { it.isNotBlank() && it != "[DONE]" }
            .forEach { line ->
                try {
                    val chunk = Json.decodeFromString<ChatCompletionChunk>(line)
                    chunk.choices.firstOrNull()?.delta?.content?.let { content ->
                        if (content.isNotEmpty()) {
                            emit(content)
                        }
                    }
                } catch (e: Exception) {
                    // Ignorer les lignes malformées
                }
            }
    }

    // Support des embeddings
    suspend fun createEmbeddings(
        input: List<String>,
        model: String = "text-embedding-ada-002"
    ): EmbeddingResponse {
        val request = EmbeddingRequest(
            model = model,
            input = input
        )

        return client.post("$baseUrl/embeddings") {
            contentType(ContentType.Application.Json)
            apiKey?.let { header("Authorization", "Bearer $it") }
            setBody(request)
        }.body()
    }

    // Support de la modération de contenu
    suspend fun createModeration(
        input: String,
        model: String = "text-moderation-latest"
    ): ModerationResponse {
        val request = ModerationRequest(
            input = input,
            model = model
        )

        return client.post("$baseUrl/moderations") {
            contentType(ContentType.Application.Json)
            apiKey?.let { header("Authorization", "Bearer $it") }
            setBody(request)
        }.body()
    }

    // Support des modèles disponibles
    suspend fun listModels(): ModelsResponse {
        return client.get("$baseUrl/models") {
            apiKey?.let { header("Authorization", "Bearer $it") }
        }.body()
    }
}
