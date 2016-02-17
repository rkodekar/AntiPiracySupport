include $(CLEAR_VARS)
LOCAL_PATH := $(call my-dir)

NDK_TOOLCHAIN_VERSION := clang
APP_ABI := all
APP_STL := gnustl_static

#LOCAL_STATIC_LIBRARIES := stlport

# Enable c++11 extentions in source code and turn off warnings/debugging
APP_CPPFLAGS += \
     -std=c++11 \
     -g0 \
     -w

#STL_INCLUDE := ${ANDROID_NDK}/sources/cxx-stl/stlport/stlport
