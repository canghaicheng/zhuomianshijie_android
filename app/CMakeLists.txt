cmake_minimum_required(VERSION 3.4.1)

# 添加 libnode 的头文件路径
include_directories(libnode/include/node/)

# 导入 libnode.so
add_library(libnode
           SHARED
           IMPORTED)

set_target_properties(libnode
                     PROPERTIES IMPORTED_LOCATION
                     ${CMAKE_SOURCE_DIR}/libnode/bin/${ANDROID_ABI}/libnode.so)

# 添加 native-lib 库
add_library(native-lib
           SHARED
           src/main/cpp/native-lib.cpp)

find_library(log-lib log)

target_link_libraries(native-lib
                     libnode
                     ${log-lib}) 