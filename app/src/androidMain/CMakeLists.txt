cmake_minimum_required(VERSION 3.4.1)

project(midis2jam2)

add_library(
    # Specifies the name of the library.
    native-lib

    # Sets the library as a shared library.
    SHARED

    # Provides a relative path to your source file(s).
    cpp/native-lib.cpp
)

add_library(libfluidsynth SHARED IMPORTED)
set(SHARED_LIBRARY_SO ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/fluidsynth/${CMAKE_ANDROID_ARCH_ABI}/libfluidsynth.so)
set_target_properties(libfluidsynth PROPERTIES IMPORTED_LOCATION ${SHARED_LIBRARY_SO})

add_library(libFLAC SHARED IMPORTED)
set_target_properties(libFLAC PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/fluidsynth/${CMAKE_ANDROID_ARCH_ABI}/libFLAC.so)
add_library(libfluidsynth-assetloader SHARED IMPORTED)
set_target_properties(libfluidsynth-assetloader PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/fluidsynth/${CMAKE_ANDROID_ARCH_ABI}/libfluidsynth-assetloader.so)
add_library(libgio-2.0 SHARED IMPORTED)
set_target_properties(libgio-2.0 PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/fluidsynth/${CMAKE_ANDROID_ARCH_ABI}/libgio-2.0.so)
add_library(libglib-2.0 SHARED IMPORTED)
set_target_properties(libglib-2.0 PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/fluidsynth/${CMAKE_ANDROID_ARCH_ABI}/libglib-2.0.so)
add_library(libgmodule-2.0 SHARED IMPORTED)
set_target_properties(libgmodule-2.0 PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/fluidsynth/${CMAKE_ANDROID_ARCH_ABI}/libgmodule-2.0.so)
add_library(libgobject-2.0 SHARED IMPORTED)
set_target_properties(libgobject-2.0 PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/fluidsynth/${CMAKE_ANDROID_ARCH_ABI}/libgobject-2.0.so)
add_library(libgthread-2.0 SHARED IMPORTED)
set_target_properties(libgthread-2.0 PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/fluidsynth/${CMAKE_ANDROID_ARCH_ABI}/libgthread-2.0.so)
add_library(libinstpatch-1.0 SHARED IMPORTED)
set_target_properties(libinstpatch-1.0 PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/fluidsynth/${CMAKE_ANDROID_ARCH_ABI}/libinstpatch-1.0.so)
add_library(liboboe SHARED IMPORTED)
set_target_properties(liboboe PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/fluidsynth/${CMAKE_ANDROID_ARCH_ABI}/liboboe.so)
add_library(libogg SHARED IMPORTED)
set_target_properties(libogg PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/fluidsynth/${CMAKE_ANDROID_ARCH_ABI}/libogg.so)
add_library(libopus SHARED IMPORTED)
set_target_properties(libopus PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/fluidsynth/${CMAKE_ANDROID_ARCH_ABI}/libopus.so)
add_library(libpcre SHARED IMPORTED)
set_target_properties(libpcre PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/fluidsynth/${CMAKE_ANDROID_ARCH_ABI}/libpcre.so)
add_library(libpcreposix SHARED IMPORTED)
set_target_properties(libpcreposix PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/fluidsynth/${CMAKE_ANDROID_ARCH_ABI}/libpcreposix.so)
add_library(libsndfile SHARED IMPORTED)
set_target_properties(libsndfile PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/fluidsynth/${CMAKE_ANDROID_ARCH_ABI}/libsndfile.so)
add_library(libvorbis SHARED IMPORTED)
set_target_properties(libvorbis PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/fluidsynth/${CMAKE_ANDROID_ARCH_ABI}/libvorbis.so)
add_library(libvorbisenc SHARED IMPORTED)
set_target_properties(libvorbisenc PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/fluidsynth/${CMAKE_ANDROID_ARCH_ABI}/libvorbisenc.so)
add_library(libvorbisfile SHARED IMPORTED)
set_target_properties(libvorbisfile PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/fluidsynth/${CMAKE_ANDROID_ARCH_ABI}/libvorbisfile.so)

find_package(OpenMP REQUIRED)

add_library(fluidsynth INTERFACE)
target_link_libraries(fluidsynth INTERFACE
        libFLAC
        libfluidsynth-assetloader
        libgio-2.0
        libglib-2.0
        libgmodule-2.0
        libgobject-2.0
        libgthread-2.0
        libinstpatch-1.0
        liboboe
        libogg
        libopus
        libpcre
        libpcreposix
        libsndfile
        libvorbis
        libvorbisenc
        libvorbisfile
        libfluidsynth
        OpenMP::OpenMP_CXX
)

target_link_libraries(native-lib PRIVATE fluidsynth log)