#----------------------------------------------------------------
# Generated CMake target import file for configuration "Release".
#----------------------------------------------------------------

# Commands may need to know the format version.
set(CMAKE_IMPORT_FILE_VERSION 1)

# Import target "fq_hll::fq_hll" for configuration "Release"
set_property(TARGET fq_hll::fq_hll APPEND PROPERTY IMPORTED_CONFIGURATIONS RELEASE)
set_target_properties(fq_hll::fq_hll PROPERTIES
  IMPORTED_LINK_INTERFACE_LANGUAGES_RELEASE "CXX"
  IMPORTED_LOCATION_RELEASE "${_IMPORT_PREFIX}/lib/libfq_hll.a"
  )

list(APPEND _cmake_import_check_targets fq_hll::fq_hll )
list(APPEND _cmake_import_check_files_for_fq_hll::fq_hll "${_IMPORT_PREFIX}/lib/libfq_hll.a" )

# Commands beyond this point should not need to know the version.
set(CMAKE_IMPORT_FILE_VERSION)
