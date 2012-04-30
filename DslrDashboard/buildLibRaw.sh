#!/bin/bash

INSTALL_DIR="`pwd`/jni/libraw"
SRC_DIR="`pwd`/../LibRaw-0.14.5"

cd $SRC_DIR

export TOOLCHAIN="$HOME/toolchain-9"
export NDK="$HOME/NVPACK/android-ndk-r7b"
export SYSROOT="$TOOLCHAIN/sysroot"
export PATH="$TOOLCHAIN/bin:$SYSROOT/usr/local/bin:$PATH"
export ARCH="armv7-a"
export SYS_ROOT="$NDK_ROOT/platforms/android-9/arch-arm/"
export CROSS_COMPILE="arm-linux-androideabi"
export CC="${CROSS_COMPILE}-gcc --sysroot=$SYSROOT"
export CXX="${CROSS_COMPILE}-g++ --sysroot=$SYSROOT"
export LD="${CROSS_COMPILE}-ld"
export AR="${CROSS_COMPILE}-ar"
export AS="${CROSS_COMPILE}-as"
export RANLIB="arm-linux-androideabi-ranlib"
export STRIP="arm-linux-androideabi-strip"
export CPPFLAGS="-I$SYSROOT/usr/include/"
export CFLAGS="-DANDROID -D__ARM_ARCH_5__ -D__ARM_ARCH_5T__ -D__ARM_ARCH_5E__ -D__ARM_ARCH_5TE__ -mandroid -fomit-frame-pointer --sysroot $SYSROOT -mfloat-abi=softfp -mfpu=vfp -mfpu=neon -fpic -ffunction-sections -funwind-tables -fstack-protector -fno-short-enums -mthumb -nostdlib -O2 -march=$ARCH -I${NDK}/sources/cxx-stl/gnu-libstdc++/include -I${NDK}/sources/cxx-stl/gnu-libstdc++/libs/armeabi-v7a/include"
export CXXFLAGS="$CFLAGS"
export LDFLAGS="-Wl,-rpath-link=$SYSROOT/usr/lib/ -L$SYROOT/usr/lib/"

mkdir -p $INSTALL_DIR
./configure --disable-shared --disable-examples --host=arm-linux-androideabi --target=arm-linux-androideabi --prefix=$INSTALL_DIR LIBS="-lc -lgcc"

make
make install

exit 0

