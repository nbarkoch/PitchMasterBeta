#pragma once

#include <android/log.h>

#ifndef NDEBUG
#define LOGD(args...) \
__android_log_print(android_LogPriority::ANDROID_LOG_DEBUG, "NativeCPPBridge", args)
#else
#define LOGD(args...)
#endif