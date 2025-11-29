# Audio / MIDI driver support
set(FLUIDSYNTH_SUPPORT_ALSA )
set(FLUIDSYNTH_SUPPORT_COREAUDIO )
set(FLUIDSYNTH_SUPPORT_COREMIDI )
set(FLUIDSYNTH_SUPPORT_DART )
set(FLUIDSYNTH_SUPPORT_DSOUND )
set(FLUIDSYNTH_SUPPORT_JACK )
set(FLUIDSYNTH_SUPPORT_KAI )
set(FLUIDSYNTH_SUPPORT_MIDISHARE )
set(FLUIDSYNTH_SUPPORT_OBOE 1)
set(FLUIDSYNTH_SUPPORT_OPENSLES 1)
set(FLUIDSYNTH_SUPPORT_OSS )
set(FLUIDSYNTH_SUPPORT_PIPEWIRE )
set(FLUIDSYNTH_SUPPORT_PORTAUDIO )
set(FLUIDSYNTH_SUPPORT_PULSE )
set(FLUIDSYNTH_SUPPORT_SDL3 )
set(FLUIDSYNTH_SUPPORT_WASAPI )
set(FLUIDSYNTH_SUPPORT_WAVEOUT )
set(FLUIDSYNTH_SUPPORT_WINMIDI )

# Files support
set(FLUIDSYNTH_SUPPORT_DLS )
set(FLUIDSYNTH_SUPPORT_LIBINSTPATCH )
set(FLUIDSYNTH_SUPPORT_LIBSNDFILE 1)
set(FLUIDSYNTH_SUPPORT_LIBSNDFILE_LEGACY )
set(FLUIDSYNTH_SUPPORT_SF3 1)

# Miscellaneous support
set(FLUIDSYNTH_SUPPORT_GLIB 1)
set(FLUIDSYNTH_SUPPORT_DBUS )
set(FLUIDSYNTH_SUPPORT_GETOPT 1)
set(FLUIDSYNTH_SUPPORT_IPV6 1)
set(FLUIDSYNTH_SUPPORT_LADSPA )
set(FLUIDSYNTH_SUPPORT_NETWORK 1)
set(FLUIDSYNTH_SUPPORT_READLINE )
set(FLUIDSYNTH_SUPPORT_SYSTEMD )

# Extra info
set(FLUIDSYNTH_OSAL glib)
set(FLUIDSYNTH_IS_SHARED 1)
set(FLUIDSYNTH_SUPPORT_COVERAGE )
set(FLUIDSYNTH_SUPPORT_FLOAT 1)
set(FLUIDSYNTH_SUPPORT_FPECHECK )
set(FLUIDSYNTH_SUPPORT_FPETRAP )
set(FLUIDSYNTH_SUPPORT_OPENMP )
set(FLUIDSYNTH_SUPPORT_PROFILING )
set(FLUIDSYNTH_SUPPORT_THREADS 1)
set(FLUIDSYNTH_SUPPORT_UBSAN )

# Only load dependencies on static builds
if(NOT FLUIDSYNTH_IS_SHARED)
  # Allows CMake to use the additional modules
  list(APPEND CMAKE_MODULE_PATH "${CMAKE_CURRENT_LIST_DIR}")

  # Make searching for packages easier on VCPKG
  if(CMAKE_VERSION VERSION_GREATER_EQUAL 3.15 AND VCPKG_TOOLCHAIN)
    set(CMAKE_FIND_PACKAGE_PREFER_CONFIG ON)
  endif()

  # Load the pkg-config helpers
  include(PkgConfigHelpers)

  # Check the system has a separated math library
  find_library(HAS_LIBM NAMES "m" NO_CACHE)
  if(HAS_LIBM)
    set(MATH_LIBRARY "m")
  endif(HAS_LIBM)

  # Load find_dependency macro
  include(CMakeFindDependencyMacro)

  # Optional dependencies
  if(NOT TARGET Threads::Threads AND NOT ((CMAKE_SYSTEM_NAME MATCHES "SunOS") OR (FLUIDSYNTH_OSAL STREQUAL "embedded")))
    find_dependency(Threads)
  endif()

  if(FLUIDSYNTH_SUPPORT_GLIB AND NOT TARGET GLib2::glib-2 OR NOT TARGET GLib2::gthread-2)
    find_dependency(GLib2 2.6.5)
  endif()

  if(FLUIDSYNTH_SUPPORT_ALSA AND NOT TARGET ALSA::ALSA)
    find_dependency(ALSA 0.9.1)
  endif()

  if(FLUIDSYNTH_SUPPORT_DBUS AND NOT TARGET dbus-1)
    find_dependency(DBus1 1.11.12)
  endif()

  if(FLUIDSYNTH_SUPPORT_JACK AND NOT TARGET Jack::Jack)
    find_dependency(Jack)
  endif()

  if(FLUIDSYNTH_SUPPORT_LADSPA AND NOT TARGET GLib2::gmodule-2)
    message(WARN "LADSPA support was built in but gmodule could not be found.")
  endif()

  if(FLUIDSYNTH_SUPPORT_LIBINSTPATCH AND NOT TARGET InstPatch::libinstpatch)
    find_dependency(InstPatch 1.1.0)
  endif()

  if(FLUIDSYNTH_SUPPORT_LIBSNDFILE_LEGACY AND NOT TARGET SndFile::sndfile)
    find_dependency(SndFileLegacy 1.0.0)
  elseif(FLUIDSYNTH_SUPPORT_LIBSNDFILE AND NOT TARGET SndFile::sndfile)
    find_dependency(SndFile 1.0.0)
  endif()

  if(FLUIDSYNTH_SUPPORT_MIDISHARE AND NOT TARGET MidiShare::MidiShare)
    find_dependency(MidiShare)
  endif()

  if(FLUIDSYNTH_SUPPORT_OBOE AND NOT TARGET oboe::oboe)
    find_dependency(oboe)
  endif()

  if(FLUIDSYNTH_SUPPORT_OPENMP AND NOT TARGET OpenMP::OpenMP_C)
    find_dependency(OpenMP COMPONENTS C)
  endif()

  if(FLUIDSYNTH_SUPPORT_OPENSLES AND NOT TARGET OpenSLES::OpenSLES)
    find_dependency(OpenSLES)
  endif()

  if(FLUIDSYNTH_SUPPORT_PIPEWIRE AND NOT TARGET PipeWire::PipeWire)
    find_dependency(PipeWire 0.3)
  endif()

  if(FLUIDSYNTH_SUPPORT_PORTAUDIO AND NOT TARGET PortAudio::PortAudio)
    find_dependency(PortAudio 2.19)
  endif()

  if(FLUIDSYNTH_SUPPORT_READLINE AND NOT TARGET Readline::Readline)
    find_dependency(Readline)
  endif()

  if(FLUIDSYNTH_SUPPORT_SDL3 AND NOT TARGET SDL3::SDL3)
    find_dependency(SDL3)
  endif()

  if(FLUIDSYNTH_SUPPORT_SYSTEMD AND NOT Systemd::libsystemd)
    find_dependency(Systemd)
  endif()

  # Restore the module path
  list(REMOVE_ITEM CMAKE_MODULE_PATH "${CMAKE_CURRENT_LIST_DIR}")
endif()

set(FluidSynth_known_comps static shared)
set(FluidSynth_comp_static NO)
set(FluidSynth_comp_shared NO)
foreach (FluidSynth_comp IN LISTS ${CMAKE_FIND_PACKAGE_NAME}_FIND_COMPONENTS)
    if (FluidSynth_comp IN_LIST FluidSynth_known_comps)
        set(FluidSynth_comp_${FluidSynth_comp} YES)
    else ()
        set(${CMAKE_FIND_PACKAGE_NAME}_NOT_FOUND_MESSAGE
            "FluidSynth does not recognize component `${FluidSynth_comp}`.")
        set(${CMAKE_FIND_PACKAGE_NAME}_FOUND FALSE)
        return()
    endif ()
endforeach ()

if (FluidSynth_comp_static AND FluidSynth_comp_shared)
    set(${CMAKE_FIND_PACKAGE_NAME}_NOT_FOUND_MESSAGE
        "FluidSynth `static` and `shared` components are mutually exclusive.")
    set(${CMAKE_FIND_PACKAGE_NAME}_FOUND FALSE)
    return()
endif ()

set(FluidSynth_static_targets "${CMAKE_CURRENT_LIST_DIR}/FluidSynth-static-targets.cmake")
set(FluidSynth_shared_targets "${CMAKE_CURRENT_LIST_DIR}/FluidSynth-shared-targets.cmake")

macro(FluidSynth_load_targets type)
    if (NOT EXISTS "${FluidSynth_${type}_targets}")
        set(${CMAKE_FIND_PACKAGE_NAME}_NOT_FOUND_MESSAGE
            "FluidSynth `${type}` libraries were requested but not found.")
        set(${CMAKE_FIND_PACKAGE_NAME}_FOUND FALSE)
        return()
    endif ()
    include("${FluidSynth_${type}_targets}")
endmacro()

if (FluidSynth_comp_static)
    FluidSynth_load_targets(static)
elseif (FluidSynth_comp_shared)
    FluidSynth_load_targets(shared)
elseif (DEFINED FluidSynth_SHARED_LIBS AND FluidSynth_SHARED_LIBS)
    FluidSynth_load_targets(shared)
elseif (DEFINED FluidSynth_SHARED_LIBS AND NOT FluidSynth_SHARED_LIBS)
    FluidSynth_load_targets(static)
elseif (BUILD_SHARED_LIBS)
    if (EXISTS "${FluidSynth_shared_targets}")
        FluidSynth_load_targets(shared)
    else ()
        FluidSynth_load_targets(static)
    endif ()
else ()
    if (EXISTS "${FluidSynth_shared_targets}")
        FluidSynth_load_targets(shared)
    else ()
        FluidSynth_load_targets(static)
    endif ()
endif ()
