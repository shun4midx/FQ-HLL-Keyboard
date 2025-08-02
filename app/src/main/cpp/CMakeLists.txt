cmake_minimum_required(VERSION 3.22.1)
project("keyboard")

# Set C++17 and enable -O2 optimization
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
add_compile_options(-O2)

# Add FQ-HLL subdirectory (must contain its own CMakeLists.txt)
add_subdirectory(FQ-HyperLogLog-Autocorrect/fq_hll_cpp)

# JNI shared library for Android
add_library(${CMAKE_PROJECT_NAME} SHARED
        native-lib.cpp
)

# Include directories
target_include_directories(${CMAKE_PROJECT_NAME} PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/FQ-HyperLogLog-Autocorrect/fq_hll_cpp/src/include
)

# Link FQ-HLL and Android libraries
target_link_libraries(${CMAKE_PROJECT_NAME}
        fq_hll
        android
        log
)

# Copies test_files
if(ANDROID)
    file(GLOB TEST_FILES "${CMAKE_CURRENT_SOURCE_DIR}/FQ-HyperLogLog-Autocorrect/fq_hll_cpp/src/test_files/*")

    foreach(f ${TEST_FILES})
        get_filename_component(f_name "${f}" NAME)
        file(COPY "${f}" DESTINATION "${CMAKE_SOURCE_DIR}/../app/src/main/assets/test_files")
    endforeach()
endif()