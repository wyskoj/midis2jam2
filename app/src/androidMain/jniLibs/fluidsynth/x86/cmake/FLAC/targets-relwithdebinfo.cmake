#----------------------------------------------------------------
# Generated CMake target import file for configuration "RelWithDebInfo".
#----------------------------------------------------------------

# Commands may need to know the format version.
set(CMAKE_IMPORT_FILE_VERSION 1)

# Import target "FLAC::FLAC" for configuration "RelWithDebInfo"
set_property(TARGET FLAC::FLAC APPEND PROPERTY IMPORTED_CONFIGURATIONS RELWITHDEBINFO)
set_target_properties(FLAC::FLAC PROPERTIES
  IMPORTED_LOCATION_RELWITHDEBINFO "${_IMPORT_PREFIX}/lib/libFLAC.so"
  IMPORTED_SONAME_RELWITHDEBINFO "libFLAC.so"
  )

list(APPEND _cmake_import_check_targets FLAC::FLAC )
list(APPEND _cmake_import_check_files_for_FLAC::FLAC "${_IMPORT_PREFIX}/lib/libFLAC.so" )

# Commands beyond this point should not need to know the version.
set(CMAKE_IMPORT_FILE_VERSION)
