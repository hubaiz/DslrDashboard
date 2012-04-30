LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := libraw

LOCAL_SRC_FILES := lib/libraw.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
LOCAL_EXPORT_LDLIBS := -lz

include $(PREBUILT_STATIC_LIBRARY)



