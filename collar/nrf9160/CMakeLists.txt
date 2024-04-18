cmake_minimum_required(VERSION 3.20.0)
find_package(Zephyr REQUIRED HINTS $ENV{ZEPHYR_BASE})

project(NRF9160)

# Entry point
target_sources(app PRIVATE src/main.c)

# App directories
add_subdirectory(src/modem)
add_subdirectory(src/server_exchange)
add_subdirectory(src/uart_channel)