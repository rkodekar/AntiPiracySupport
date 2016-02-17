include $(call all-makefiles-under,$(LOCAL_PATH))

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := org.contentguard.support
LOCAL_SDK_VERSION := 21
LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_JAVA_LIBRARIES := framework
LOCAL_STATIC_JAVA_LIBRARIES += android-support-v4
LOCAL_STATIC_LIBRARIES := contentguard

include $(BUILD_STATIC_JAVA_LIBRARY)
