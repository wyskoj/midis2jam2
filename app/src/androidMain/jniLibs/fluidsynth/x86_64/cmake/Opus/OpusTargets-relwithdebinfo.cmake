#----------------------------------------------------------------
# Generated CMake target import file for configuration "RelWithDebInfo".
#----------------------------------------------------------------

# Commands may need to know the format version.
set(CMAKE_IMPORT_FILE_VERSION 1)

# Import target "Opus::opus" for configuration "RelWithDebInfo"
set_property(TARGET Opus::opus APPEND PROPERTY IMPORTED_CONFIGURATIONS RELWITHDEBINFO)
set_target_properties(Opus::opus PROPERTIES
  IMPORTED_LOCATION_RELWITHDEBINFO "${_IMPORT_PREFIX}/lib/libopus.so"
  IMPORTED_SONAME_RELWITHDEBINFO "libopus.so"
  )

list(APPEND _cmake_import_check_targets Opus::opus )
list(APPEND _cmake_import_check_files_for_Opus::opus "${_IMPORT_PREFIX}/lib/libopus.so" )

# Commands beyond this point should not need to know the version.
set(CMAKE_IMPORT_FILE_VERSION)
