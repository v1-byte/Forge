package com.v1byte.forge.engine

import android.content.Context
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/**
 * Small coroutine-friendly facade over the llama.cpp-backed inference runtime.
 *
 * Model files should already have been copied to app-private storage by the caller. This class
 * therefore accepts an absolute path rather than reading a document/content URI itself.
 *
 * Limitations: this repository uses the supplied llama.cpp runtime; a GGUF file must use a model
 * architecture supported by that runtime, and the packaged native runtime only supports its
 * shipped ABIs (currently arm64-v8a and x86_64). A valid GGUF file does not guarantee that every
 * architecture or device can run it.
 */
class ForgeInferenceRepository(
    context: Context,
    private val initializationTimeoutMillis: Long = DEFAULT_INITIALIZATION_TIMEOUT_MILLIS,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val applicationContext = context.applicationContext
    private val repositoryScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val operationMutex = Mutex()

    private val _state = MutableStateFlow<State>(State.Idle)

    /** Observable initialization, model, generation, and error state for the UI. */
    val state: StateFlow<State> = _state.asStateFlow()

    private var engine: InferenceEngine? = null
    private var engineStateJob: kotlinx.coroutines.Job? = null

    /** Load a model from an absolute path in app-private storage. */
    suspend fun loadModel(absolutePath: String) = withContext(ioDispatcher) {
        operationMutex.withLock {
            try {
                checkNotDestroyed()
                require(File(absolutePath).isAbsolute) {
                    "Model path must be absolute: $absolutePath"
                }
                val inferenceEngine = getOrCreateInitializedEngine()
                when (inferenceEngine.state.value) {
                    is InferenceEngine.State.ModelReady,
                    is InferenceEngine.State.Error -> {
                        _state.value = State.CleaningUp
                        inferenceEngine.cleanUp()
                    }
                    is InferenceEngine.State.Initialized -> Unit
                    else -> error("Cannot replace model while inference engine is ${inferenceEngine.state.value}")
                }
                check(inferenceEngine.state.value is InferenceEngine.State.Initialized) {
                    "Cannot load a model while the inference engine is ${inferenceEngine.state.value}"
                }
                _state.value = State.LoadingModel
                inferenceEngine.loadModel(absolutePath)
                _state.value = State.ModelReady
            } catch (failure: Throwable) {
                recordFailureUnlessCancelled(failure)
                throw failure
            }
        }
    }

    /**
     * Generate a response as a stream of text tokens. Collect this Flow from a lifecycle-aware
     * scope in the UI; native inference and repository work are dispatched away from the main thread.
     */
    fun generate(prompt: String): Flow<String> = flow {
        try {
            operationMutex.withLock {
                checkNotDestroyed()
                require(prompt.isNotBlank()) { "Prompt must not be blank" }
                val inferenceEngine = getOrCreateInitializedEngine()
                check(inferenceEngine.state.value is InferenceEngine.State.ModelReady) {
                    "Load a model before generating; engine state is ${inferenceEngine.state.value}"
                }
                inferenceEngine.sendUserPrompt(prompt).collect { token -> emit(token) }
                _state.value = mapEngineState(inferenceEngine.state.value)
            }
        } catch (failure: Throwable) {
            recordFailureUnlessCancelled(failure)
            throw failure
        }
    }.flowOn(ioDispatcher)
        .catch { failure ->
            // Keep exceptions available to the caller while also making failures observable in state.
            recordFailureUnlessCancelled(failure)
            throw failure
        }

    /** Unload the current model, if an engine has been created. */
    suspend fun cleanUp() = withContext(ioDispatcher) {
        operationMutex.withLock {
            try {
                checkNotDestroyed()
                val inferenceEngine = engine ?: return@withLock
                _state.value = State.CleaningUp
                inferenceEngine.cleanUp()
                _state.value = mapEngineState(inferenceEngine.state.value)
            } catch (failure: Throwable) {
                recordFailureUnlessCancelled(failure)
                throw failure
            }
        }
    }

    /** Release the runtime. This repository instance cannot be used again after this call. */
    suspend fun destroy() = withContext(ioDispatcher) {
        operationMutex.withLock {
            if (_state.value is State.Destroyed) return@withLock
            try {
                engineStateJob?.cancel()
                engineStateJob = null
                engine?.destroy()
                _state.value = State.Destroyed
            } catch (failure: Throwable) {
                recordFailureUnlessCancelled(failure)
                throw failure
            } finally {
                engineStateJob?.cancel()
                engineStateJob = null
                repositoryScope.cancel()
            }
        }
    }

    /** Called with [operationMutex] held and on [ioDispatcher]. */
    private suspend fun getOrCreateInitializedEngine(): InferenceEngine {
        engine?.let { return it }

        _state.value = State.Initializing
        val created = try {
            AiChat.getInferenceEngine(applicationContext)
        } catch (failure: Throwable) {
            recordFailureUnlessCancelled(failure)
            throw failure
        }
        engine = created
        engineStateJob = repositoryScope.launch {
            created.state.collect { engineState ->
                // Initialization timeout/error states set by this facade take precedence until
                // a usable engine state is observed.
                _state.value = mapEngineState(engineState)
            }
        }

        val terminalState = withTimeoutOrNull(initializationTimeoutMillis) {
            created.state.first { engineState ->
                engineState is InferenceEngine.State.Initialized ||
                    engineState is InferenceEngine.State.ModelReady ||
                    engineState is InferenceEngine.State.Error
            }
        } ?: throw IllegalStateException(
            "Timed out after ${initializationTimeoutMillis}ms waiting for inference engine initialization",
        )

        when (terminalState) {
            is InferenceEngine.State.Error -> _state.value = State.Error(terminalState.exception)
            is InferenceEngine.State.Initialized -> _state.value = State.Initialized
            is InferenceEngine.State.ModelReady -> _state.value = State.ModelReady
            else -> error("Unexpected inference engine state: $terminalState")
        }
        return created
    }

    private fun checkNotDestroyed() {
        check(_state.value !is State.Destroyed) {
            "This inference repository has been destroyed"
        }
    }

    private fun mapEngineState(engineState: InferenceEngine.State): State = when (engineState) {
        is InferenceEngine.State.Uninitialized -> State.Idle
        is InferenceEngine.State.Initializing -> State.Initializing
        is InferenceEngine.State.Initialized -> State.Initialized
        is InferenceEngine.State.LoadingModel -> State.LoadingModel
        is InferenceEngine.State.UnloadingModel -> State.CleaningUp
        is InferenceEngine.State.ModelReady -> State.ModelReady
        is InferenceEngine.State.Benchmarking,
        is InferenceEngine.State.ProcessingSystemPrompt,
        is InferenceEngine.State.ProcessingUserPrompt,
        is InferenceEngine.State.Generating -> State.Generating
        is InferenceEngine.State.Error -> State.Error(engineState.exception)
    }

    private fun recordFailureUnlessCancelled(failure: Throwable) {
        if (failure is CancellationException) throw failure
        _state.value = State.Error(failure)
    }

    sealed class State {
        object Idle : State()
        object Initializing : State()
        object Initialized : State()
        object LoadingModel : State()
        object ModelReady : State()
        object Generating : State()
        object CleaningUp : State()
        object Destroyed : State()
        data class Error(val cause: Throwable) : State()
    }

    private companion object {
        const val DEFAULT_INITIALIZATION_TIMEOUT_MILLIS = 30_000L
    }
}
