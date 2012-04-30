APP_STL      := gnustl_static
APP_ABI      := armeabi-v7a
APP_PLATFORM := android-9
APP_CPPFLAGS += -mthumb
APP_CPPFLAGS += -Os
APP_CPPFLAGS += -fno-strict-aliasing
APP_CPPFLAGS += -O2
APP_CPPFLAGS += -DNDEBUG
APP_CPPFLAGS += -g
APP_CPPFLAGS += -fexceptions
APP_CPPFLAGS += -frtti
APP_CPPFLAGS += -lstdc++
APP_CPPFLAGS += -D__GLIBC__

