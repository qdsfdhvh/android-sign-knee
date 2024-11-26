#!/bin/bash
set -e

#TARGET_ARCHS=("armv7" "arm64" "x86" "x86_64")
TARGET_ARCHS=("arm64")

CURL_VERSION="8.11.0"
OPENSSL_VERSION="3.4.0"
ZLIB_VERSION="1.3.1"

ANDROID_MIN_API=24

ROOT_DIR=$(pwd)
WORKER=$(getconf _NPROCESSORS_ONLN)

BUILD_DIR_CURL="build/android/curl"
BUILD_DIR_OPENSSL="build/android/openssl"
BUILD_DIR_ZLIB="build/android/zlib"

COLOR_GREEN="\033[38;5;48m"
COLOR_END="\033[0m"

if [[ -z "$NDK_ROOT" ]]; then
    echo "set NDK_ROOT env variable"
    exit 1
fi

export ANDROID_NDK_ROOT=$NDK_ROOT
export ANDROID_TOOLCHAIN="$NDK_ROOT/toolchains/llvm/prebuilt/darwin-x86_64"
export PATH="$ANDROID_TOOLCHAIN/bin:$PATH"

error() {
    echo -e "$@" 1>&2
}

fail() {
    error "$@"
    exit 1
}

reCreateFiles() {
  local projectDir=$1
  if [ -d "$projectDir/tar" ]; then
    rm -rf "$projectDir/tar"
  fi
  if [ -d "$projectDir/src" ]; then
    rm -rf "$projectDir/src"
  fi
  if [ -d "$projectDir/install" ]; then
    rm -rf "$projectDir/install"
  fi

  mkdir -p "$projectDir/tar"
  mkdir -p "$projectDir/src"
  mkdir -p "$projectDir/install"
}


# OpenSSL

reCreateFiles $BUILD_DIR_OPENSSL

LOG_FILE="$ROOT_DIR/$BUILD_DIR_OPENSSL/build.log"

if [ -f "$LOG_FILE" ]; then
    rm "$LOG_FILE"
    touch "$LOG_FILE"
fi

echo "Downloading OpenSSL..."
curl -Lo "$BUILD_DIR_OPENSSL/tar/openssl-$OPENSSL_VERSION.tar.gz" "https://www.openssl.org/source/openssl-$OPENSSL_VERSION.tar.gz" >> "$LOG_FILE" 2>&1 || fail "Error Downloading OpenSSL"

echo "Uncompressing OpenSSL..."
tar xzf "${BUILD_DIR_OPENSSL}/tar/openssl-$OPENSSL_VERSION.tar.gz" -C "$BUILD_DIR_OPENSSL/src" || fail "Error Uncompressing OpenSSL"

cd "$BUILD_DIR_OPENSSL/src/openssl-$OPENSSL_VERSION"

export ANDROID_NDK_HOME="$NDK_ROOT"

for CURRENT_ARCH in "${TARGET_ARCHS[@]}"; do
    echo "Building OpenSSL for $CURRENT_ARCH build..."

    make clean 1>& /dev/null || true

    echo "-> Configuring OpenSSL for $CURRENT_ARCH build..."
    case $CURRENT_ARCH in
        # armv7)
        #     export CC="armv7a-linux-androideabi16-clang"
        #     export CXX="armv7a-linux-androideabi16-clang++"
        #     export AR="llvm-ar"
        #     export AS=$CC
        #     export LD="ld"
        #     export RANLIB="llvm-ranlib"
        #     export STRIP="llvm-strip"

        #     ./Configure android-arm no-ssl2 no-ssl3 no-comp no-hw no-engine no-shared no-tests no-ui no-deprecated zlib -Wl,--fix-cortex-a8 -fPIC -DANDROID -D__ANDROID_API__=16 -Os -fuse-ld="$ANDROID_TOOLCHAIN/bin/arm-linux-androideabi-ld" >> "$LOG_FILE" 2>&1 || fail "-> Error Configuring OpenSSL for $CURRENT_ARCH"
        # ;;
        arm64)
            export CC="aarch64-linux-android$ANDROID_MIN_API-clang"
            export CXX="aarch64-linux-android$ANDROID_MIN_API-clang++"
            export AR="aarch64-linux-android-ar"
            export AS=$CC
            export LD="ld"
            export RANLIB="llvm-ranlib"
            export STRIP="llvm-strip"

            ./Configure --prefix="$ROOT_DIR/$BUILD_DIR_OPENSSL/install/$CURRENT_ARCH" \
              android-arm64 \
              no-asm \
              no-comp \
              no-dso \
              no-dtls \
              no-engine \
              no-hw \
              no-idea \
              no-nextprotoneg \
              no-psk \
              no-srp \
              no-ssl3 \
              no-weak-ssl-ciphers \
              no-shared \
              no-tests \
              no-ui\
              no-deprecated \
              zlib -fPIC -DANDROID \
              -D__ANDROID_API__=$ANDROID_MIN_API \
              -Os -fuse-ld="$ANDROID_TOOLCHAIN/bin/ld" >> "$LOG_FILE" 2>&1 || fail "-> Error Configuring OpenSSL for $CURRENT_ARCH"
        ;;
        # x86)
        #     export CC="i686-linux-android16-clang"
        #     export CXX="i686-linux-android16-clang++"
        #     export AR="i686-linux-android-ar"
        #     export AS="i686-linux-android-as"
        #     export LD="i686-linux-android-ld"
        #     export RANLIB="i686-linux-android-ranlib"
        #     export NM="i686-linux-android-nm"
        #     export STRIP="i686-linux-android-strip"

        #     ./Configure android-x86 no-ssl2 no-ssl3 no-comp no-hw no-engine no-shared no-tests no-ui no-deprecated zlib -mtune=intel -mssse3 -mfpmath=sse -m32 -fPIC -DANDROID -D__ANDROID_API__=16 -Os -fuse-ld="$ANDROID_TOOLCHAIN/bin/i686-linux-android-ld" >> "$LOG_FILE" 2>&1 || fail "-> Error Configuring OpenSSL for $CURRENT_ARCH"
        # ;;
        # x86_64)
        #     export CC="x86_64-linux-android21-clang"
        #     export CXX="x86_64-linux-android21-clang++"
        #     export AR="x86_64-linux-android-ar"
        #     export AS="x86_64-linux-android-as"
        #     export LD="x86_64-linux-android-ld"
        #     export RANLIB="x86_64-linux-android-ranlib"
        #     export NM="x86_64-linux-android-nm"
        #     export STRIP="x86_64-linux-android-strip"

        #     ./Configure android-x86_64 no-ssl2 no-ssl3 no-comp no-hw no-engine no-shared no-tests no-ui no-deprecated zlib -mtune=intel -mssse3 -mfpmath=sse -m64 -fPIC -DANDROID -D__ANDROID_API__=21 -Os -fuse-ld="$ANDROID_TOOLCHAIN/bin/x86_64-linux-android-ld" >> "$LOG_FILE" 2>&1 || fail "-> Error Configuring OpenSSL for $CURRENT_ARCH"
        # ;;
    esac
    sed -i '' -e "s!-O3!-Os!g" "Makefile" || exit 1
    echo "-> Configured OpenSSL for $CURRENT_ARCH"

    echo "-> Compiling OpenSSL for $CURRENT_ARCH..."
    make -j "$WORKER" >> "$LOG_FILE" 2>&1 || fail "-> Error Compiling OpenSSL for $CURRENT_ARCH"
    echo "-> Compiled OpenSSL for $CURRENT_ARCH"

    echo "-> Installing OpenSSL for $CURRENT_ARCH to $ROOT_DIR/$BUILD_DIR_OPENSSL/install/$CURRENT_ARCH..."
    make install_sw >> "$LOG_FILE" 2>&1 || fail "-> Error Installing OpenSSL for $CURRENT_ARCH"
    echo "-> Installed OpenSSL for $CURRENT_ARCH"

    echo "Successfully built OpenSSL for $CURRENT_ARCH"
done

echo -e "${COLOR_GREEN}OpenSSL Built Successfully for all ARCH targets.$COLOR_END"

# zlib
cd "$ROOT_DIR" || exit 1

reCreateFiles $BUILD_DIR_ZLIB

LOG_FILE="$ROOT_DIR/$BUILD_DIR_ZLIB/build.log"
if [ -f "$LOG_FILE" ]; then
    rm "$LOG_FILE"
    touch "$LOG_FILE"
fi

echo "Downloading zlib..."
curl -Lo "$BUILD_DIR_ZLIB/tar/zlib-$ZLIB_VERSION.tar.gz" "http://zlib.net/zlib-$ZLIB_VERSION.tar.gz" >> "$LOG_FILE" 2>&1 || fail "Error Downloading zlib"

echo "Uncompressing zlib..."
tar xzf "$BUILD_DIR_ZLIB/tar/zlib-$ZLIB_VERSION.tar.gz" -C "$BUILD_DIR_ZLIB/src" || fail "Error Uncompressing zlib"

cd "$BUILD_DIR_ZLIB/src/zlib-$ZLIB_VERSION"

for CURRENT_ARCH in "${TARGET_ARCHS[@]}"; do
    echo "Building zlib for $CURRENT_ARCH build..."

    make clean 1>& /dev/null || true

    echo "-> Configuring zlib for $CURRENT_ARCH build..."

    case $CURRENT_ARCH in
        arm64)
            export CC="aarch64-linux-android$ANDROID_MIN_API-clang"
            export CXX="aarch64-linux-android$ANDROID_MIN_API-clang++"
            export AR="llvm-ar"
            export AS=$CC
            export LD="ld"
            export RANLIB="llvm-ranlib"
            export STRIP="llvm-strip"
            export CHOST="aarch64-linux-android"
       ;;
    esac

    ./configure --static --prefix="$ROOT_DIR/$BUILD_DIR_ZLIB/install/$CURRENT_ARCH" >> "$LOG_FILE" 2>&1 || fail "-> Error Configuring zlib for $CURRENT_ARCH"

    echo "-> Configured zlib for $CURRENT_ARCH"

    echo "-> Compiling zlib for $CURRENT_ARCH..."
    make -j "$WORKER" >> "$LOG_FILE" 2>&1 || fail "-> Error Compiling zlib for $CURRENT_ARCH"
    echo "-> Compiled zlib for $CURRENT_ARCH"

    echo "-> Installing zlib for $CURRENT_ARCH..."
    make install >> "$LOG_FILE" 2>&1 || fail "-> Error Installing zlib for $CURRENT_ARCH"
    echo "-> Installing zlib for $CURRENT_ARCH to $ROOT_DIR/$BUILD_DIR_ZLIB/install/$CURRENT_ARCH..."

    echo "Successfully built zlib for $CURRENT_ARCH"
done

echo -e "${COLOR_GREEN}zlib built successfully for all ARCH targets.${COLOR_END}"

# cURL
cd "$ROOT_DIR" || exit 1

reCreateFiles $BUILD_DIR_CURL

LOG_FILE="$ROOT_DIR/$BUILD_DIR_CURL/build.log"
if [ -f "$LOG_FILE" ]; then
    rm "$LOG_FILE"
    touch "$LOG_FILE"
fi

echo "Downloading curl..."
curl -Lo "$BUILD_DIR_CURL/tar/curl-$CURL_VERSION.tar.gz" "https://curl.haxx.se/download/curl-$CURL_VERSION.tar.gz" >> "$LOG_FILE" 2>&1 || fail "Error Downloading curl"

echo "Uncompressing curl..."
tar xzf "$BUILD_DIR_CURL/tar/curl-$CURL_VERSION.tar.gz" -C "$BUILD_DIR_CURL/src" || fail "Error Uncompressing curl"

cd "$BUILD_DIR_CURL/src/curl-$CURL_VERSION"

for CURRENT_ARCH in "${TARGET_ARCHS[@]}"; do
    echo "Building curl for $CURRENT_ARCH build..."

    make clean 1>& /dev/null || true

    echo "-> Configuring curl for $CURRENT_ARCH build..."
    case $CURRENT_ARCH in
        # armv7)
        #     export HOST="arm-linux-androideabi"

        #     export CC="armv7a-linux-androideabi16-clang"
        #     export CXX="armv7a-linux-androideabi16-clang++"
        #     export AR="arm-linux-androideabi-ar"
        #     export AS="arm-linux-androideabi-as"
        #     export LD="arm-linux-androideabi-ld"
        #     export RANLIB="arm-linux-androideabi-ranlib"
        #     export NM="arm-linux-androideabi-nm"
        #     export STRIP="arm-linux-androideabi-strip"

        #     export CFLAGS="--sysroot=$ANDROID_TOOLCHAIN/sysroot -Wl,--fix-cortex-a8 -fPIC -DANDROID -D__ANDROID_API__=16 -Os"
        #     export CPPFLAGS="$CFLAGS"
        #     export CXXFLAGS="$CFLAGS -fno-exceptions -fno-rtti"
        #     export LDFLAGS="-Wl,--fix-cortex-a8"
        # ;;
        arm64)
            export HOST="aarch64-linux-android"

            export CC="aarch64-linux-android$ANDROID_MIN_API-clang"
            export CXX="aarch64-linux-android$ANDROID_MIN_API-clang++"
            export AR="llvm-ar"
            export AS=$CC
            export LD="ld"
            export RANLIB="llvm-ranlib"
            export STRIP="llvm-strip"

            export CFLAGS="--sysroot=$ANDROID_TOOLCHAIN/sysroot -fPIC -DANDROID -D__ANDROID_API__=$ANDROID_MIN_API -Os"
            export CPPFLAGS="$CFLAGS"
            export CXXFLAGS="$CFLAGS -fno-exceptions -fno-rtti"
            export LDFLAGS=""
        ;;
        # x86)
        #     export HOST="i686-linux-android"

        #     export CC="i686-linux-android16-clang"
        #     export CXX="i686-linux-android16-clang++"
        #     export AR="i686-linux-android-ar"
        #     export AS="i686-linux-android-as"
        #     export LD="i686-linux-android-ld"
        #     export RANLIB="i686-linux-android-ranlib"
        #     export NM="i686-linux-android-nm"
        #     export STRIP="i686-linux-android-strip"

        #     export CFLAGS="--sysroot=$ANDROID_TOOLCHAIN/sysroot -fPIC -mtune=intel -mssse3 -mfpmath=sse -m32 -DANDROID -D__ANDROID_API__=16 -Os"
        #     export CPPFLAGS="$CFLAGS"
        #     export CXXFLAGS="$CFLAGS -fno-exceptions -fno-rtti"
        #     export LDFLAGS=""
        # ;;
        # x86_64)
        #     export HOST="x86_64-linux-android"

        #     export CC="x86_64-linux-android21-clang"
        #     export CXX="x86_64-linux-android21-clang++"
        #     export AR="x86_64-linux-android-ar"
        #     export AS="x86_64-linux-android-as"
        #     export LD="x86_64-linux-android-ld"
        #     export RANLIB="x86_64-linux-android-ranlib"
        #     export NM="x86_64-linux-android-nm"
        #     export STRIP="x86_64-linux-android-strip"

        #     export CFLAGS="--sysroot=$ANDROID_TOOLCHAIN/sysroot -fPIC -mtune=intel -mssse3 -mfpmath=sse -m64 -DANDROID -D__ANDROID_API__=21 -Os"
        #     export CPPFLAGS="$CFLAGS"
        #     export CXXFLAGS="$CFLAGS -fno-exceptions -fno-rtti"
        #     export LDFLAGS=""
        # ;;
    esac

    ./configure --host="$HOST" \
        --prefix="$ROOT_DIR/$BUILD_DIR_CURL/install/$CURRENT_ARCH" \
        --with-openssl="$ROOT_DIR/$BUILD_DIR_OPENSSL/install/$CURRENT_ARCH" \
        --with-zlib="$ROOT_DIR/$BUILD_DIR_ZLIB/install/$CURRENT_ARCH" \
        --enable-static \
        --disable-shared \
        --disable-debug \
        --disable-curldebug \
        --enable-symbol-hiding \
        --enable-optimize \
        --disable-ares \
        --enable-threaded-resolver \
        --disable-manual \
        --disable-ipv6 \
        --enable-proxy \
        --enable-http \
        --disable-rtsp \
        --disable-ftp \
        --disable-file \
        --disable-ldap \
        --disable-ldaps \
        --disable-rtsp \
        --disable-dict \
        --disable-telnet \
        --disable-tftp \
        --disable-pop3 \
        --disable-imap \
        --disable-smtp \
        --disable-gopher \
        --disable-ntlm \
        --without-libssh2 \
        --without-librtmp \
        --without-libidn \
        --without-ca-bundle \
        --without-ca-path \
        --without-winidn \
        --without-nghttp2 \
        --without-cyassl \
        --without-polarssl \
        --without-gnutls \
        --without-winssl \
        --without-libpsl >> "$LOG_FILE" 2>&1 || fail "-> Error Configuring curl for $CURRENT_ARCH"
    # sed -i '' -e 's~#define HAVE_STRDUP~//#define HAVE_STRDUP~g' configure
    echo "-> Configured curl for $CURRENT_ARCH"

    echo "-> Compiling curl for $CURRENT_ARCH..."
    make -j "$WORKER" >> "$LOG_FILE" 2>&1 || fail "-> Error Compiling curl for $CURRENT_ARCH"
    echo "-> Compiled curl for $CURRENT_ARCH"

    echo "-> Installing curl for $CURRENT_ARCH to $ROOT_DIR/$BUILD_DIR_CURL/install/$CURRENT_ARCH..."
    make install >> "$LOG_FILE" 2>&1 || fail "-> Error Installing curl for $CURRENT_ARCH"
    echo "-> Installed curl for $CURRENT_ARCH"

    echo "Successfully built curl for $CURRENT_ARCH"
done

echo -e "${COLOR_GREEN}curl built successfully for all ARCH targets.${COLOR_END}"
cd "$ROOT_DIR"
exit 0