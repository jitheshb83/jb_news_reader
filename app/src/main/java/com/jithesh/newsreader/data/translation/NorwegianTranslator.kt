package com.jithesh.newsreader.data.translation

import com.google.android.gms.tasks.Task
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Wraps ML Kit's on-device Translate API for Norwegian -> English. The Norwegian<->English
 * model (~30MB) downloads once on first use and is cached on-device by ML Kit afterwards —
 * no API key, no server round-trip per translation.
 */
@Singleton
class NorwegianTranslator @Inject constructor() {

    private val translator by lazy {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.NORWEGIAN)
            .setTargetLanguage(TranslateLanguage.ENGLISH)
            .build()
        Translation.getClient(options)
    }

    suspend fun translate(text: String): Result<String> {
        if (text.isBlank()) return Result.success(text)
        return try {
            translator.downloadModelIfNeeded().await()
            Result.success(translator.translate(text).await())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> cont.resume(result) }
    addOnFailureListener { exception -> cont.resumeWithException(exception) }
    addOnCanceledListener { cont.cancel() }
}
