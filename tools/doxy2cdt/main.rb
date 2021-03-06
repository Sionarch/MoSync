# Copyright (C) 2010 MoSync AB
# 
# This program is free software; you can redistribute it and/or modify it under
# the terms of the GNU General Public License, version 2, as published by
# the Free Software Foundation.
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
# or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# for more details.
# 
# You should have received a copy of the GNU General Public License
# along with this program; see the file COPYING.  If not, write to the Free
# Software Foundation, 59 Temple Place - Suite 330, Boston, MA
# 02111-1307, USA.

##
# This tool converts a doxygen XML index to a similar index that the CDT dynamic
# help system can understand. The XML index used for input is generated by
# doxygen when setting the "XML_OUTPUT = yes" option in the Doxyfile.
#
# The reason for not using XSLT is that some of the attributes requires
# simple text transformations. For example the href attribute in <topic>.
#

require "./cmd_arguments"
require "./doxy_converter"
require "rexml/document"
include REXML

# Title of the index
ECLIPSE_INDEX_TITLE = "MoSync API Reference"

# Type of index
ECLIPSE_INDEX_TYPE = "HELP_TYPE_CPP"

# The indentation of the XML files
XML_INDENT = 4

##
# Reads a doxygen index.xml file and converts it to a
# documentation index file for CDT.
#
def main
  parsed_args = CmdArguments.new( ARGV )

  # Read the doxygen index file
  doxy_index_xml_file = File.new( parsed_args.doxygen_input_xml, "r" )
  doxy_index_xml_doc = Document.new( doxy_index_xml_file )

  # Create CDT index XML
  result_cdt_doc = Document.new( )
  result_cdt_doc << XMLDecl.new( "1.0", "UTF-8" )
  doxy_converter = DoxyConverter.new( parsed_args.eclipse_base_path )

  doxy_converter.doxy_to_cdt_xml( doxy_index_xml_doc,
                                  result_cdt_doc,
                                  ECLIPSE_INDEX_TITLE,
                                  ECLIPSE_INDEX_TYPE )

  # Save and close
  save_document( result_cdt_doc, parsed_args.cdt_output_xml )
  doxy_index_xml_file.close( )
end

##
# Saves the given document to the given path.
#
# @param xml_doc An XML tree to save.
# @param path The location of where to save the tree.
#
def save_document(xml_doc, path)
  output_xml = File.new( path, "w" )
  formatter = REXML::Formatters::Pretty.new( XML_INDENT )
  formatter.write( xml_doc, output_xml )
  output_xml.close( )
end


# Start main only if the script is executed from command line
if __FILE__ == $0
  main( );
end
