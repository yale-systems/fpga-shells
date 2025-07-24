# Set the bitstream file path
while {[llength $argv]} {
  set argv [lassign $argv[set argv {}] flag]
  switch -glob $flag {
    -bit-file {
      set argv [lassign $argv[set argv {}] bitfile]
    }
    default {
      return -code error [list {unknown option} $flag]
    }
  }
}

# Check if the bitstream file exists
if {![info exists bitfile]} {
  return -code error "No bitstream file specified. Use -bit-file <path>"
}
if {![file exists $bitfile]} {
  return -code error "Bitstream file not found: $bitfile"
}

# Open the hardware target
open_hw_manager
connect_hw_server

# Scan for devices
open_hw_target
current_hw_target [get_hw_targets *]

# Get the FPGA device
set device [lindex [get_hw_devices] 0]
refresh_hw_device $device

# Program the FPGA
set_property PROGRAM.FILE $bitfile $device
program_hw_devices $device
refresh_hw_device $device

puts "FPGA programming completed successfully."