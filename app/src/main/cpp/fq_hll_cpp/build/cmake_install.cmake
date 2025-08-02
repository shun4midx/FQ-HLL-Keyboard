# Install script for directory: /Users/shun4midx/Documents/Shun4miCodes/Shun4miPersonal/Projects/HLL_Autocorrect/FQ-HLL-Keyboard/app/src/main/cpp/FQ-HyperLogLog-Autocorrect/fq_hll_cpp

# Set the install prefix
if(NOT DEFINED CMAKE_INSTALL_PREFIX)
  set(CMAKE_INSTALL_PREFIX "/usr/local")
endif()
string(REGEX REPLACE "/$" "" CMAKE_INSTALL_PREFIX "${CMAKE_INSTALL_PREFIX}")

# Set the install configuration name.
if(NOT DEFINED CMAKE_INSTALL_CONFIG_NAME)
  if(BUILD_TYPE)
    string(REGEX REPLACE "^[^A-Za-z0-9_]+" ""
           CMAKE_INSTALL_CONFIG_NAME "${BUILD_TYPE}")
  else()
    set(CMAKE_INSTALL_CONFIG_NAME "Release")
  endif()
  message(STATUS "Install configuration: \"${CMAKE_INSTALL_CONFIG_NAME}\"")
endif()

# Set the component getting installed.
if(NOT CMAKE_INSTALL_COMPONENT)
  if(COMPONENT)
    message(STATUS "Install component: \"${COMPONENT}\"")
    set(CMAKE_INSTALL_COMPONENT "${COMPONENT}")
  else()
    set(CMAKE_INSTALL_COMPONENT)
  endif()
endif()

# Is this installation the result of a crosscompile?
if(NOT DEFINED CMAKE_CROSSCOMPILING)
  set(CMAKE_CROSSCOMPILING "FALSE")
endif()

# Set path to fallback-tool for dependency-resolution.
if(NOT DEFINED CMAKE_OBJDUMP)
  set(CMAKE_OBJDUMP "/usr/bin/objdump")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "Unspecified" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib" TYPE STATIC_LIBRARY FILES "/Users/shun4midx/Documents/Shun4miCodes/Shun4miPersonal/Projects/HLL_Autocorrect/FQ-HLL-Keyboard/app/src/main/cpp/FQ-HyperLogLog-Autocorrect/fq_hll_cpp/build/src/libfq_hll.a")
  if(EXISTS "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/libfq_hll.a" AND
     NOT IS_SYMLINK "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/libfq_hll.a")
    execute_process(COMMAND "/usr/bin/ranlib" "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/libfq_hll.a")
  endif()
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "Unspecified" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include" TYPE DIRECTORY FILES "/Users/shun4midx/Documents/Shun4miCodes/Shun4miPersonal/Projects/HLL_Autocorrect/FQ-HLL-Keyboard/app/src/main/cpp/FQ-HyperLogLog-Autocorrect/fq_hll_cpp/src/include/")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "Unspecified" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/bin" TYPE PROGRAM FILES
    "/Users/shun4midx/Documents/Shun4miCodes/Shun4miPersonal/Projects/HLL_Autocorrect/FQ-HLL-Keyboard/app/src/main/cpp/FQ-HyperLogLog-Autocorrect/fq_hll_cpp/scripts/fq_hll_g++"
    "/Users/shun4midx/Documents/Shun4miCodes/Shun4miPersonal/Projects/HLL_Autocorrect/FQ-HLL-Keyboard/app/src/main/cpp/FQ-HyperLogLog-Autocorrect/fq_hll_cpp/scripts/fq_hll_clang++"
    )
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "Unspecified" OR NOT CMAKE_INSTALL_COMPONENT)
  if(EXISTS "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/cmake/fq_hll/fq_hllTargets.cmake")
    file(DIFFERENT _cmake_export_file_changed FILES
         "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/cmake/fq_hll/fq_hllTargets.cmake"
         "/Users/shun4midx/Documents/Shun4miCodes/Shun4miPersonal/Projects/HLL_Autocorrect/FQ-HLL-Keyboard/app/src/main/cpp/FQ-HyperLogLog-Autocorrect/fq_hll_cpp/build/CMakeFiles/Export/68f306c03c3753e683e29cff4aafcc3c/fq_hllTargets.cmake")
    if(_cmake_export_file_changed)
      file(GLOB _cmake_old_config_files "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/cmake/fq_hll/fq_hllTargets-*.cmake")
      if(_cmake_old_config_files)
        string(REPLACE ";" ", " _cmake_old_config_files_text "${_cmake_old_config_files}")
        message(STATUS "Old export file \"$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/cmake/fq_hll/fq_hllTargets.cmake\" will be replaced.  Removing files [${_cmake_old_config_files_text}].")
        unset(_cmake_old_config_files_text)
        file(REMOVE ${_cmake_old_config_files})
      endif()
      unset(_cmake_old_config_files)
    endif()
    unset(_cmake_export_file_changed)
  endif()
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib/cmake/fq_hll" TYPE FILE FILES "/Users/shun4midx/Documents/Shun4miCodes/Shun4miPersonal/Projects/HLL_Autocorrect/FQ-HLL-Keyboard/app/src/main/cpp/FQ-HyperLogLog-Autocorrect/fq_hll_cpp/build/CMakeFiles/Export/68f306c03c3753e683e29cff4aafcc3c/fq_hllTargets.cmake")
  if(CMAKE_INSTALL_CONFIG_NAME MATCHES "^([Rr][Ee][Ll][Ee][Aa][Ss][Ee])$")
    file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib/cmake/fq_hll" TYPE FILE FILES "/Users/shun4midx/Documents/Shun4miCodes/Shun4miPersonal/Projects/HLL_Autocorrect/FQ-HLL-Keyboard/app/src/main/cpp/FQ-HyperLogLog-Autocorrect/fq_hll_cpp/build/CMakeFiles/Export/68f306c03c3753e683e29cff4aafcc3c/fq_hllTargets-release.cmake")
  endif()
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "Unspecified" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib/cmake/fq_hll" TYPE FILE FILES
    "/Users/shun4midx/Documents/Shun4miCodes/Shun4miPersonal/Projects/HLL_Autocorrect/FQ-HLL-Keyboard/app/src/main/cpp/FQ-HyperLogLog-Autocorrect/fq_hll_cpp/build/fq_hllConfig.cmake"
    "/Users/shun4midx/Documents/Shun4miCodes/Shun4miPersonal/Projects/HLL_Autocorrect/FQ-HLL-Keyboard/app/src/main/cpp/FQ-HyperLogLog-Autocorrect/fq_hll_cpp/build/fq_hllConfigVersion.cmake"
    )
endif()

if(NOT CMAKE_INSTALL_LOCAL_ONLY)
  # Include the install script for each subdirectory.
  include("/Users/shun4midx/Documents/Shun4miCodes/Shun4miPersonal/Projects/HLL_Autocorrect/FQ-HLL-Keyboard/app/src/main/cpp/FQ-HyperLogLog-Autocorrect/fq_hll_cpp/build/src/cmake_install.cmake")

endif()

string(REPLACE ";" "\n" CMAKE_INSTALL_MANIFEST_CONTENT
       "${CMAKE_INSTALL_MANIFEST_FILES}")
if(CMAKE_INSTALL_LOCAL_ONLY)
  file(WRITE "/Users/shun4midx/Documents/Shun4miCodes/Shun4miPersonal/Projects/HLL_Autocorrect/FQ-HLL-Keyboard/app/src/main/cpp/FQ-HyperLogLog-Autocorrect/fq_hll_cpp/build/install_local_manifest.txt"
     "${CMAKE_INSTALL_MANIFEST_CONTENT}")
endif()
if(CMAKE_INSTALL_COMPONENT)
  if(CMAKE_INSTALL_COMPONENT MATCHES "^[a-zA-Z0-9_.+-]+$")
    set(CMAKE_INSTALL_MANIFEST "install_manifest_${CMAKE_INSTALL_COMPONENT}.txt")
  else()
    string(MD5 CMAKE_INST_COMP_HASH "${CMAKE_INSTALL_COMPONENT}")
    set(CMAKE_INSTALL_MANIFEST "install_manifest_${CMAKE_INST_COMP_HASH}.txt")
    unset(CMAKE_INST_COMP_HASH)
  endif()
else()
  set(CMAKE_INSTALL_MANIFEST "install_manifest.txt")
endif()

if(NOT CMAKE_INSTALL_LOCAL_ONLY)
  file(WRITE "/Users/shun4midx/Documents/Shun4miCodes/Shun4miPersonal/Projects/HLL_Autocorrect/FQ-HLL-Keyboard/app/src/main/cpp/FQ-HyperLogLog-Autocorrect/fq_hll_cpp/build/${CMAKE_INSTALL_MANIFEST}"
     "${CMAKE_INSTALL_MANIFEST_CONTENT}")
endif()
