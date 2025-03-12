#include <jni.h>
#include <string>
#include <cstdlib>
#include <android/log.h>
#include "node.h"

#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, "NodeJS", __VA_ARGS__))
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, "NodeJS", __VA_ARGS__))

// 全局变量用于跟踪 Node.js 状态
static bool node_started = false;

//node's libUV requires all arguments being on contiguous memory.
extern "C" jint JNICALL
Java_com_example_zhuomianshijie_MainActivity_startNodeWithArguments(
        JNIEnv *env,
        jobject /* this */,
        jobjectArray arguments) {

    if (node_started) {
        LOGI("Node.js already started");
        return jint(0);
    }

    //argc
    jsize argument_count = env->GetArrayLength(arguments);
    LOGI("Starting Node.js with %d arguments", argument_count);

    try {
        //Compute byte size need for all arguments in contiguous memory.
        int c_arguments_size = 0;
        for (int i = 0; i < argument_count ; i++) {
            jstring arg = (jstring)env->GetObjectArrayElement(arguments, i);
            const char* str = env->GetStringUTFChars(arg, nullptr);
            if (str == nullptr) {
                LOGE("Failed to get string at index %d", i);
                return jint(-1);
            }
            c_arguments_size += strlen(str) + 1;
            env->ReleaseStringUTFChars(arg, str);
        }

        LOGI("Allocated %d bytes for arguments", c_arguments_size);

        //Stores arguments in contiguous memory.
        char* args_buffer = (char*) calloc(c_arguments_size, sizeof(char));
        if (args_buffer == nullptr) {
            LOGE("Failed to allocate memory for arguments");
            return jint(-1);
        }

        //argv to pass into node.
        char* argv[argument_count];

        //To iterate through the expected start position of each argument in args_buffer.
        char* current_args_position = args_buffer;

        //Populate the args_buffer and argv.
        for (int i = 0; i < argument_count ; i++) {
            jstring arg = (jstring)env->GetObjectArrayElement(arguments, i);
            const char* str = env->GetStringUTFChars(arg, nullptr);
            if (str == nullptr) {
                LOGE("Failed to get string at index %d", i);
                free(args_buffer);
                return jint(-1);
            }
            LOGI("Argument %d: %s", i, str);

            //Copy current argument to its expected position in args_buffer
            size_t str_len = strlen(str);
            strncpy(current_args_position, str, str_len);
            current_args_position[str_len] = '\0';

            //Save current argument start position in argv
            argv[i] = current_args_position;

            //Increment to the next argument's expected position.
            current_args_position += str_len + 1;

            env->ReleaseStringUTFChars(arg, str);
        }

        //Start node, with argc and argv.
        LOGI("Starting Node.js runtime with argc=%d", argument_count);
        for(int i = 0; i < argument_count; i++) {
            LOGI("argv[%d]=%s", i, argv[i]);
        }
        
        LOGI("Calling node::Start...");
        int node_result = node::Start(argument_count, argv);
        LOGI("After node::Start call, result=%d", node_result);
        
        if (node_result == 0) {
            node_started = true;
            LOGI("Node.js started successfully");
        } else {
            LOGE("Node.js failed to start with error code: %d", node_result);
        }

        free(args_buffer);
        return jint(node_result);

    } catch (const std::exception& e) {
        LOGE("Exception in native code: %s", e.what());
        return jint(-1);
    } catch (...) {
        LOGE("Unknown exception in native code");
        return jint(-1);
    }
}