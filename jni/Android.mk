LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

NDK_TOOLCHAIN_VERSION := clang
APP_ABI := all
APP_STL := gnustl_static

# include /tools/android-ndk/sources/cxx-stl/stlport/Android.mk
LOCAL_C_INCLUDES := /tools/android-ndk/sources/cxx-stl/stlport/stlport
LOCAL_C_INCLUDES := /tools/android-ndk/sources/cxx-stl/gnu-libstdc++/4.9/include

LOCAL_CFLAGS    := \
    -std=c++11 \
    -Wall \
    -Werror
COMMON_PROJECT_CFLAGS := $(LOCAL_CFLAGS)

# =================================================================
# ContentGuard JNI module (Constants / Callback / Subscriber / etc)
# This is staging for implementation in the main library
# =================================================================
include $(CLEAR_VARS)

LOCAL_MODULE := contentguard
LOCAL_CFLAGS := \

LOCAL_SRC_FILES := \
    $(LOCAL_PATH)/constants.cpp

LOCAL_LDLIBS    := -llog

LOCAL_CFLAGS += $(COMMON_PROJECT_CFLAGS)

include $(BUILD_SHARED_LIBRARY)
