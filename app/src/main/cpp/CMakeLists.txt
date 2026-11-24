cmake_minimum_required(VERSION 3.22.1)
project("native_crash_lab")

add_library(native_crash_lab SHARED
    native_crash_lab.cpp
)

target_link_libraries(native_crash_lab
    log
)
