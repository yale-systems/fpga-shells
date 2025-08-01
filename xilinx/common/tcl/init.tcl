# See LICENSE for license details.

set_param general.maxThreads 16

# Include helper functions
source [file join $scriptdir "util.tcl"]

# Create the directory for IPs
file mkdir $ipdir

# Update the IP catalog
update_ip_catalog -rebuild

# Generate CMAC Ultrascale IPs
source [file join $commondir tcl cmac cmac_usplus.tcl]
source [file join $commondir tcl cmac cmac_gty.tcl]

# Generate IP implementations. Vivado TCL emitted from Chisel Blackboxes
foreach ip_vivado_tcl $ip_vivado_tcls {
  source $ip_vivado_tcl
}

# Optional board-specific ip script
set boardiptcl [file join $boarddir tcl ip.tcl]
if {[file exists $boardiptcl]} {
  source $boardiptcl
}

# AR 58526 <http://www.xilinx.com/support/answers/58526.html>
set xci_files [get_files -all {*.xci}]
foreach xci_file $xci_files {
  set_property GENERATE_SYNTH_CHECKPOINT {false} -quiet $xci_file
}

# Get a list of IPs in the current design
set obj [get_ips]

# Generate target data for the included IPs in the design
generate_target all $obj

# Export the IP user files
export_ip_user_files -of_objects $obj -no_script -force

# Adding timing constraints
set obj [current_fileset -constrset]
add_files -quiet -norecurse -fileset $obj [glob -directory $syndir -nocomplain {*.tcl}]

puts "Current constraints fileset: $obj"
puts "Files in constraints fileset $obj:"
foreach f [get_files -of_objects [get_filesets $obj]] {
    puts $f
}

# Get the list of active source and constraint files
set obj [current_fileset]

#Xilinx bug workaround
#scrape IP tree for directories containing .vh files
#[get_property include_dirs] misses all IP core subdirectory includes if user has specified -dir flag in create_ip
set property_include_dirs [get_property include_dirs $obj]

# Include generated files for the IPs in the design
set ip_include_dirs [concat $property_include_dirs [findincludedir $ipdir "*.vh"]]
set ip_include_dirs [concat $ip_include_dirs [findincludedir $srcdir "*.h"]]
set ip_include_dirs [concat $ip_include_dirs [findincludedir $srcdir "*.vh"]]
