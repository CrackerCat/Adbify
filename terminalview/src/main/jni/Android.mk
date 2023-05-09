LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE:= libadbify-term
LOCAL_SRC_FILES:= term.c
include $(BUILD_SHARED_LIBRARY)
