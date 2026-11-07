#include <jni.h>

#include "llama.h"

#include <algorithm>
#include <cstdint>
#include <limits>
#include <memory>
#include <mutex>
#include <random>
#include <stdexcept>
#include <string>
#include <vector>

namespace {
std::mutex g_mutex;
llama_model * g_model = nullptr;
bool g_backend_initialized = false;
uint32_t g_context_size = 2048;
int32_t g_threads = 2;

void throw_java(JNIEnv * env, const char * message) {
    jclass exception_class = env->FindClass("java/lang/IllegalStateException");
    if (exception_class != nullptr) {
        env->ThrowNew(exception_class, message);
        env->DeleteLocalRef(exception_class);
    }
}

std::string from_java(JNIEnv * env, jstring input) {
    if (input == nullptr) {
        throw std::invalid_argument("Input kosong.");
    }
    const jsize length = env->GetStringLength(input);
    const jchar * chars = env->GetStringChars(input, nullptr);
    if (chars == nullptr) {
        throw std::runtime_error("Gagal membaca teks dari Kotlin.");
    }
    std::string result;
    result.reserve(static_cast<size_t>(length) * 3);
    for (jsize i = 0; i < length; ++i) {
        uint32_t cp = chars[i];
        if (cp >= 0xD800 && cp <= 0xDBFF && i + 1 < length && chars[i + 1] >= 0xDC00 && chars[i + 1] <= 0xDFFF) {
            cp = 0x10000 + ((cp - 0xD800) << 10) + (static_cast<uint32_t>(chars[++i]) - 0xDC00);
        } else if (cp >= 0xD800 && cp <= 0xDFFF) {
            cp = 0xFFFD;
        }
        if (cp <= 0x7F) result.push_back(static_cast<char>(cp));
        else if (cp <= 0x7FF) {
            result.push_back(static_cast<char>(0xC0 | (cp >> 6)));
            result.push_back(static_cast<char>(0x80 | (cp & 0x3F)));
        } else if (cp <= 0xFFFF) {
            result.push_back(static_cast<char>(0xE0 | (cp >> 12)));
            result.push_back(static_cast<char>(0x80 | ((cp >> 6) & 0x3F)));
            result.push_back(static_cast<char>(0x80 | (cp & 0x3F)));
        } else {
            result.push_back(static_cast<char>(0xF0 | (cp >> 18)));
            result.push_back(static_cast<char>(0x80 | ((cp >> 12) & 0x3F)));
            result.push_back(static_cast<char>(0x80 | ((cp >> 6) & 0x3F)));
            result.push_back(static_cast<char>(0x80 | (cp & 0x3F)));
        }
    }
    env->ReleaseStringChars(input, chars);
    return result;
}

jstring to_java(JNIEnv * env, const std::string & input) {
    std::vector<jchar> output;
    output.reserve(input.size());
    const auto * bytes = reinterpret_cast<const unsigned char *>(input.data());
    for (size_t i = 0; i < input.size();) {
        uint32_t cp = 0xFFFD;
        const unsigned char first = bytes[i];
        size_t count = 1;
        if (first <= 0x7F) cp = first;
        else if ((first & 0xE0) == 0xC0) { cp = first & 0x1F; count = 2; }
        else if ((first & 0xF0) == 0xE0) { cp = first & 0x0F; count = 3; }
        else if ((first & 0xF8) == 0xF0) { cp = first & 0x07; count = 4; }
        bool valid = count > 1 && i + count <= input.size();
        if (count == 1 && first <= 0x7F) valid = true;
        if (valid && count > 1) {
            for (size_t j = 1; j < count; ++j) {
                if ((bytes[i + j] & 0xC0) != 0x80) { valid = false; break; }
                cp = (cp << 6) | (bytes[i + j] & 0x3F);
            }
            const uint32_t minimum = count == 2 ? 0x80 : count == 3 ? 0x800 : 0x10000;
            if (cp < minimum || cp > 0x10FFFF || (cp >= 0xD800 && cp <= 0xDFFF)) valid = false;
        }
        if (!valid) { cp = 0xFFFD; count = 1; }
        i += count;
        if (cp <= 0xFFFF) output.push_back(static_cast<jchar>(cp));
        else {
            cp -= 0x10000;
            output.push_back(static_cast<jchar>(0xD800 + (cp >> 10)));
            output.push_back(static_cast<jchar>(0xDC00 + (cp & 0x3FF)));
        }
        if (output.size() > static_cast<size_t>(std::numeric_limits<jsize>::max())) {
            throw std::runtime_error("Teks output melampaui batas JNI.");
        }
    }
    static const jchar empty_string = 0;
    const jchar * chars = output.empty() ? &empty_string : output.data();
    return env->NewString(chars, static_cast<jsize>(output.size()));
}

std::vector<llama_token> tokenize(const llama_vocab * vocab, const std::string & text) {
    if (text.empty()) {
        throw std::invalid_argument("Prompt tidak boleh kosong.");
    }
    if (text.size() > static_cast<size_t>(std::numeric_limits<int32_t>::max())) {
        throw std::invalid_argument("Prompt terlalu besar.");
    }

    const auto text_len = static_cast<int32_t>(text.size());
    int32_t needed = llama_tokenize(vocab, text.data(), text_len, nullptr, 0, true, false);
    if (needed == std::numeric_limits<int32_t>::min()) {
        throw std::runtime_error("Jumlah token prompt melampaui batas API.");
    }
    if (needed > 0) {
        // Some vocabularies may report the required count even for a null output buffer.
    } else {
        needed = -needed;
    }
    if (needed <= 0) {
        throw std::runtime_error("Prompt tidak menghasilkan token.");
    }

    std::vector<llama_token> tokens(static_cast<size_t>(needed));
    int32_t count = llama_tokenize(vocab, text.data(), text_len, tokens.data(), needed, true, false);
    if (count < 0) {
        if (count == std::numeric_limits<int32_t>::min()) {
            throw std::runtime_error("Tokenisasi prompt melampaui batas API.");
        }
        tokens.resize(static_cast<size_t>(-count));
        count = llama_tokenize(vocab, text.data(), text_len, tokens.data(), static_cast<int32_t>(tokens.size()), true, false);
    }
    if (count <= 0) {
        throw std::runtime_error("Tokenisasi prompt gagal.");
    }
    tokens.resize(static_cast<size_t>(count));
    return tokens;
}

std::string token_piece(const llama_vocab * vocab, llama_token token) {
    std::vector<char> buffer(128);
    int32_t length = llama_token_to_piece(vocab, token, buffer.data(), static_cast<int32_t>(buffer.size()), 0, false);
    if (length < 0) {
        if (length == std::numeric_limits<int32_t>::min()) {
            throw std::runtime_error("Token output terlalu besar.");
        }
        buffer.resize(static_cast<size_t>(-length));
        length = llama_token_to_piece(vocab, token, buffer.data(), static_cast<int32_t>(buffer.size()), 0, false);
    }
    if (length < 0 || static_cast<size_t>(length) > buffer.size()) {
        throw std::runtime_error("Gagal mengubah token ke teks.");
    }
    return std::string(buffer.data(), static_cast<size_t>(length));
}
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_v1byte_forge_NativeLlamaEngine_nativeLoadModel(JNIEnv * env, jobject, jstring model_path, jint context_size, jint threads) {
    try {
        const std::string path = from_java(env, model_path);
        if (path.empty()) {
            throw std::invalid_argument("Path model kosong.");
        }

        std::lock_guard<std::mutex> lock(g_mutex);
        if (g_model != nullptr) {
            llama_model_free(g_model);
            g_model = nullptr;
        }
        if (!g_backend_initialized) {
            llama_backend_init();
            g_backend_initialized = true;
        }

        llama_model_params model_params = llama_model_default_params();
        model_params.n_gpu_layers = 0; // Initial Android slice deliberately uses the CPU backend.
        g_model = llama_model_load_from_file(path.c_str(), model_params);
        if (g_model == nullptr) {
            throw std::runtime_error("llama.cpp gagal memuat model GGUF. Periksa file, ruang memori, dan dukungan arsitektur model.");
        }

        g_context_size = static_cast<uint32_t>(std::clamp(context_size, 512, 8192));
        g_threads = std::clamp(static_cast<int32_t>(threads), 1, 8);
        return JNI_TRUE;
    } catch (const std::exception & error) {
        throw_java(env, error.what());
        return JNI_FALSE;
    } catch (...) {
        throw_java(env, "Kesalahan native tidak diketahui saat memuat model.");
        return JNI_FALSE;
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_v1byte_forge_NativeLlamaEngine_nativeGenerate(JNIEnv * env, jobject, jstring prompt_text, jint max_tokens) {
    try {
        const std::string prompt = from_java(env, prompt_text);
        std::lock_guard<std::mutex> lock(g_mutex);
        if (g_model == nullptr) {
            throw std::runtime_error("Muat model GGUF terlebih dahulu.");
        }

        llama_context_params context_params = llama_context_default_params();
        context_params.n_ctx = g_context_size;
        context_params.n_batch = std::min<uint32_t>(512, g_context_size);
        context_params.n_ubatch = std::min<uint32_t>(128, context_params.n_batch);
        context_params.n_threads = g_threads;
        context_params.n_threads_batch = g_threads;
        std::unique_ptr<llama_context, decltype(&llama_free)> context(
            llama_init_from_model(g_model, context_params), &llama_free);
        if (!context) {
            throw std::runtime_error("llama.cpp gagal membuat context; model mungkin terlalu besar untuk memori perangkat.");
        }

        const llama_vocab * vocab = llama_model_get_vocab(g_model);
        std::vector<llama_token> prompt_tokens = tokenize(vocab, prompt);
        if (prompt_tokens.size() >= g_context_size) {
            throw std::runtime_error("Prompt memenuhi atau melampaui panjang context yang dipilih.");
        }

        llama_batch prompt_batch = llama_batch_get_one(prompt_tokens.data(), static_cast<int32_t>(prompt_tokens.size()));
        if (llama_decode(context.get(), prompt_batch) != 0) {
            throw std::runtime_error("llama.cpp gagal memproses prompt.");
        }

        llama_sampler_chain_params sampler_params = llama_sampler_chain_default_params();
        std::unique_ptr<llama_sampler, decltype(&llama_sampler_free)> sampler(
            llama_sampler_chain_init(sampler_params), &llama_sampler_free);
        if (!sampler) {
            throw std::runtime_error("Gagal membuat sampler.");
        }
        llama_sampler_chain_add(sampler.get(), llama_sampler_init_temp(0.8f));
        std::random_device random_device;
        const uint32_t seed = random_device();
        llama_sampler_chain_add(sampler.get(), llama_sampler_init_dist(seed));

        const uint32_t available = g_context_size - static_cast<uint32_t>(prompt_tokens.size());
        const int32_t limit = std::clamp(max_tokens, 1, static_cast<int32_t>(std::min<uint32_t>(available, 512)));
        std::string output;
        output.reserve(static_cast<size_t>(limit) * 4);
        for (int32_t i = 0; i < limit; ++i) {
            const llama_token token = llama_sampler_sample(sampler.get(), context.get(), -1);
            if (token == LLAMA_TOKEN_NULL || llama_vocab_is_eog(vocab, token)) {
                break;
            }
            output += token_piece(vocab, token);
            if (i + 1 < limit) {
                llama_token next_token = token;
                llama_batch next_batch = llama_batch_get_one(&next_token, 1);
                if (llama_decode(context.get(), next_batch) != 0) {
                    throw std::runtime_error("llama.cpp gagal melanjutkan generation.");
                }
            }
        }
        return to_java(env, output);
    } catch (const std::exception & error) {
        throw_java(env, error.what());
        return nullptr;
    } catch (...) {
        throw_java(env, "Kesalahan native tidak diketahui saat generation.");
        return nullptr;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_v1byte_forge_NativeLlamaEngine_nativeUnload(JNIEnv * env, jobject) {
    (void) env;
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_model != nullptr) {
        llama_model_free(g_model);
        g_model = nullptr;
    }
    if (g_backend_initialized) {
        llama_backend_free();
        g_backend_initialized = false;
    }
}
