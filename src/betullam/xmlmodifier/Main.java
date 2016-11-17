/**
 * This is the main file of XmlModifier that is started when the programm is invoked.
 *
 * Copyright (C) Michael Birkner, 2016
 * 
 * This file is part of XmlModifier.
 * 
 * XmlModifier is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XmlModifier is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AkImporter.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @author   Michael Birkner
 * @license  http://www.gnu.org/licenses/gpl-3.0.html
 */
package betullam.xmlmodifier;

public class Main {


	public static void main(String[] args)  {

		if (args.length < 1) {
			System.out.println("Use argument \"add\" or \"modify\" to get further help. E. g.: java -jar /path/to/XmlModifier.jar add");
			return;
		}


		String addOrModify = args[0];


		if (addOrModify.equals("modify") ) {

			if (args.length != 10) {
				System.out.println("Modify. Please supply following additional arguments to effectively run the command:" + 
						"\n\t1: Path to directory and/or subdirectories with XML files that should be modified." + 
						"\n\t2: Name of meta files to modify: use \"meta\" or \"meta_anchor\"" + 
						"\n\t3: Structure element condition: modify elements of these structure elements only. Comma separated string. E. g.: Monograph,Periodical" + 
						"\n\t4: File filter: Value of \"name\" attribute. Modify only files containing the given value of the \"name\" attribute, e. g.: <goobi:metadata name=\"ATTRIBUTE_VALUE\">...</goobi:metadata>. Use \"null\" if you don't want to check for the attribute value. If you use \"null\" here, the element value condition will be skiped also." + 
						"\n\t5: File filter: Value of \"goobi:metadata\" tag. Modify only files with the given element value, e. g.: <goobi:metadata name=\"...\">ELEMENT VALUE</goobi:metadata>. Use \"null\" if you don't want to check for the element value. This is also skiped if the you set \"null\" for the attribute value." + 
						"\n\t6: Element filter: Value of \"name\" attribute. Modify only XML elements with the value of this \"name\" attribute e. g.: <goobi:metadata name=\"ATTRIBUTE_VALUE\">...</goobi:metadata>. Use \"null\" to skip this check." + 
						"\n\t7: Element filter: Value of \"goobi:metadata\" tag. Modify only XML elements with this element value, e. g.: <goobi:metadata name=\"...\">ELEMENT VALUE</goobi:metadata>. Use \"null\" to skip this check." +
						"\n\t8: New attribute value: Set the attribut value of the element to this value, e. g.: <goobi:metadata name=\"NEW_ATTRIBUTE_VALUE\">...</goobi:metadata>. Use \"null\" to use the old value." +
						"\n\t9: New element value: Set the element value of the element to this value, e. g.: \"<goobi:metadata name=\"...\">NEW ELEMENT VALUE</goobi:metadata>. Use \"null\" to use the old value."
						);	
				return;
			} else {				
				String mdFolder = args[1];
				String fileName = args[2];
				String condStructureType = args[3];
				String condMdNameFile = args[4];
				String condMdValueFile = args[5];
				String condMdNameElement = args[6];
				String condMdValueElement = args[7];
				String newMdName = args[8];
				String newMdValue = args[9];

				//System.out.println("Command Line Arguments: " + mdFolder + "; " + condStructureType + "; " + condMdName + "; " + condMdValue + "; " + modMdName + "; " + modMdValue);
				new XMLmodifier().modify(mdFolder, fileName, condStructureType, condMdNameFile, condMdValueFile, condMdNameElement, condMdValueElement, newMdName, newMdValue);
				System.out.println("Done modifing XML-files.");
				
			}
		}




		if (addOrModify.equals("add")) {

			if (args.length != 8) {
				System.out.println("Add. Please supply following additional arguments to effectively run the command:" + 
						"\n\t1: Metadata-Directory: path to directory and/or subdirectories with xml files" + 
						"\n\t2: Structure element condition: add new element to these structure elements only. Comma separated string. E. g.: Monograph,Periodical" + 
						"\n\t3: Element name(s): the added element(s) will have this name. Separate multiple elements by comma for nested elements. E. g.: FIRST:ELEMENT,SECOND:ELEMENT will result in: <FIRST:ELEMENT attr1=\"attrvalue1\"><SECOND:ELEMENT attr2=\"attrvalue2\">Text Content</SECOND:ELEMENT></FIRST:ELEMENT>." + 
						"\n\t4: Attribute name(s): name of attribute(s) you want to add. Use \"null\" if you don't want to add an attribute. Separate multiple attributes for multiple elements (but only one per element is possible!). E. g.: ATTR1,ATTR2 will result in: <first:element ATTR1=\"attrvalue1\"><second:element ATTR2=\"attrvalue2\">Text Content</second:element></first:element>" + 
						"\n\t5: Attribute value(s): value of attribute(s) you want to add. Use \"null\" if you don't want to add an attribute.  Separate multiple attribute values for multiple elements (but only one per element is possible!) E. g.: ATTRVALUE1,ATTRVALUE2 will result in: <first:element attr1=\"ATTRVALUE1\"><second:element attr2=\"ATTRVALUE2\">Text Content</second:element></first:element>" +
						"\n\t6: Text value: value of the text content of the last element in the set of given elements. E. g.: \"TEXT CONTENT\" will result in: <first:element attr1=\"attrvalue1\"><second:element attr2=\"attrvalue2\">TEXT CONTENT</second:element></first:element>" +
						"\n\t7: Allow duplicate: use \"false\" if the new element should NOT be added if a similar one exists."

						);
				return;
			} else {
				String mdFolder = args[1];
				String condStructureElements = args[2];
				String elementNames = args[3];
				String attrNames = args[4];
				String attrValues = args[5];
				String textValue = args[6];
				boolean allowDuplicate = Boolean.parseBoolean(args[7]);
				
				//System.out.println("Command Line Arguments: " + mdFolder + "; " + condStructureType + "; " + condMdName + "; " + condMdValue + "; " + modMdName + "; " + modMdValue);
				new XMLmodifier().add(mdFolder, condStructureElements, elementNames, attrNames, attrValues, textValue, allowDuplicate);
				System.out.println("Done adding element to XML-files.");
			}
		}


		

		if (!addOrModify.equals("add") && !addOrModify.equals("modify")) {
			System.out.println("Use argument \"add\" or \"modify\" to get further help. E. g.: java -jar /path/to/XmlModifier.jar add");
			return;
		}
		
	}

	
}
