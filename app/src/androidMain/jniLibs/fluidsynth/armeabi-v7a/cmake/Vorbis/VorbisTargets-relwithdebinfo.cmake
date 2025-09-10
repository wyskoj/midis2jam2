#----------------------------------------------------------------
# Generated CMake target import file for configuration "RelWithDebInfo".
#----------------------------------------------------------------

# Commands may need to know the format version.
set(CMAKE_IMPORT_FILE_VERSION 1)

# Import target "Vorbis::vorbis" for configuration "RelWithDebInfo"
set_property(TARGET Vorbis::vorbis APPEND PROPERTY IMPORTED_CONFIGURATIONS RELWITHDEBINFO)
set_target_properties(Vorbis::vorbis PROPERTIES
  IMPORTED_LOCATION_RELWITHDEBINFO "${_IMPORT_PREFIX}/lib/libvorbis.so"
  IMPORTED_SONAME_RELWITHDEBINFO "libvorbis.so"
  )

list(APPEND _cmake_import_check_targets Vorbis::vorbis )
list(APPEND _cmake_import_check_files_for_Vorbis::vorbis "${_IMPORT_PREFIX}/lib/libvorbis.so" )

# Import target "Vorbis::vorbisenc" for configuration "RelWithDebInfo"
set_property(TARGET Vorbis::vorbisenc APPEND PROPERTY IMPORTED_CONFIGURATIONS RELWITHDEBINFO)
set_target_properties(Vorbis::vorbisenc PROPERTIES
  IMPORTED_LOCATION_RELWITHDEBINFO "${_IMPORT_PREFIX}/lib/libvorbisenc.so"
  IMPORTED_SONAME_RELWITHDEBINFO "libvorbisenc.so"
  )

list(APPEND _cmake_import_check_targets Vorbis::vorbisenc )
list(APPEND _cmake_import_check_files_for_Vorbis::vorbisenc "${_IMPORT_PREFIX}/lib/libvorbisenc.so" )

# Import target "Vorbis::vorbisfile" for configuration "RelWithDebInfo"
set_property(TARGET Vorbis::vorbisfile APPEND PROPERTY IMPORTED_CONFIGURATIONS RELWITHDEBINFO)
set_target_properties(Vorbis::vorbisfile PROPERTIES
  IMPORTED_LOCATION_RELWITHDEBINFO "${_IMPORT_PREFIX}/lib/libvorbisfile.so"
  IMPORTED_SONAME_RELWITHDEBINFO "libvorbisfile.so"
  )

list(APPEND _cmake_import_check_targets Vorbis::vorbisfile )
list(APPEND _cmake_import_check_files_for_Vorbis::vorbisfile "${_IMPORT_PREFIX}/lib/libvorbisfile.so" )

# Commands beyond this point should not need to know the version.
set(CMAKE_IMPORT_FILE_VERSION)
