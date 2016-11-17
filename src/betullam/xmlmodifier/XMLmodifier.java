/**
 * This code executes the actual XML modification.
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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class XMLmodifier {

	//public void add(String mdFolder, String condStructureElements, String addMdName, String addMdValue) {
	public void add(String mdFolder, String condStructureElements, String elementNames, String attrNames, String attrValues, String textValue, boolean allowDuplicate) {

		// Get the files that should be modified:
		Set<File> filesForInsertion = getFilesForInsertion(condStructureElements, mdFolder);

		// Backup XML-files before modifing them:
		makeBackupFiles(filesForInsertion);

		// Iterate over all files in given directory and it's subdirectories. Works with Apache commons-io library (FileUtils).
		for (File xmlFile : filesForInsertion) {

			// DOM Parser:
			String filePath = xmlFile.getAbsolutePath();
			DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder;
			Document xmlDoc = null;

			try {
				documentBuilder = documentFactory.newDocumentBuilder();
				xmlDoc = documentBuilder.parse(filePath);

				// Get the elements we want to add another element. The name-attribute of the <goobi:metadata ...>-tag is important.
				List<Element> elementsForInsertion = getElementsForInsertion(condStructureElements, xmlDoc);

				if (!elementsForInsertion.isEmpty()) {

					List<String> lstElementNames = Arrays.asList(elementNames.split("\\s*,\\s*"));
					List<String> lstAttrNames = Arrays.asList(attrNames.split("\\s*,\\s*"));
					List<String> lstAttrValues = Arrays.asList(attrValues.split("\\s*,\\s*"));
					if (!lstElementNames.isEmpty()) {
						int noOfElements = lstElementNames.size();
						if ((noOfElements == lstAttrNames.size()) && (noOfElements == lstAttrValues.size())) {
							Element lastElement = null;
							Element newElement = null;
							for (String elementName : lstElementNames) {
								int index = lstElementNames.indexOf(elementName);

								newElement = xmlDoc.createElement(elementName);
								String attrName = lstAttrNames.get(index);
								String attrValue = lstAttrValues.get(index);

								// Set attribute name and value:
								if (!attrName.equals("null") && !attrValue.equals("null")) {
									if (!attrName.isEmpty() && !attrValue.isEmpty()) {
										newElement.setAttribute(attrName, attrValue);
									}
								}

								// Set text content to the inner-most element:
								if ((noOfElements-1) == index) {
									newElement.setTextContent(textValue);
								}

								// Add element to document:
								if (newElement != null) {
									for (Element rootElement : elementsForInsertion) {
										if (index == 0) { // Append element to root node

											if (allowDuplicate == false) {
												// Add only if element with same value does not already exist.
												if (!hasDuplicate(rootElement, elementName, attrName, attrValue, textValue)) {
													rootElement.appendChild(newElement);
												}
											} else {
												rootElement.appendChild(newElement);
											}
										} else { // Append element to previous element
											if (allowDuplicate == false) {
												// Add only if element with same value does not already exist.
												if (!hasDuplicate(lastElement, elementName, attrName, attrValue, textValue)) {
													lastElement.appendChild(newElement);
												}
											} else {
												lastElement.appendChild(newElement);
											}
										}
										lastElement = newElement;
									}
								}
							}
						} else {
							System.err.println("The number of attribute names and values must be the same as the number of element names. Use \"null\" for attribute name and values if you don't want to add them.");
						}
					} else {
						System.err.println("You have to supply at least one element name.");
					}

					DOMSource source = new DOMSource(xmlDoc);

					TransformerFactory transformerFactory = TransformerFactory.newInstance();
					Transformer transformer = transformerFactory.newTransformer();
					transformer.setOutputProperty(OutputKeys.INDENT, "yes");
					StreamResult result = new StreamResult(filePath);
					transformer.transform(source, result);
				}
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (TransformerException e) {
				e.printStackTrace();
			}
		}

	}



	/**
	 * @param args
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws XPathExpressionException 
	 * @throws TransformerException 
	 */
	public void modify(String mdFolder, String fileName, String condStructureType, String condMdNameFile, String condMdValueFile, String condMdNameElement, String condMdValueElement, String newMdName, String newMdValue) {

		// Get the files that should be modified:
		Set<File> filesToModify = getFilesToModify(mdFolder, fileName, condStructureType, condMdNameFile, condMdValueFile);

		// Backup XML-files before modifing them:
		makeBackupFiles(filesToModify);

		// Iterate over all files in given directory and it's subdirectories. Works with Apache commons-io library (FileUtils).
		for (File xmlFile : filesToModify) {
			
			

			// DOM Parser:
			String filePath = xmlFile.getAbsolutePath();
			DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
			Document xmlDoc = null;
			try {
				DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
				xmlDoc = documentBuilder.parse(filePath);

				// Get the elements to change. The name-attribute of the <goobi:metadata ...>-tag is important.
				// E. g. to change metadata "AccessLicense", the value of "mdName" must be the same as the name-attribute: <goobi:metadata name="AccessLicense">
				List<Node> nodeArrayList = getElementsToModify(condMdNameElement, condMdValueElement, xmlDoc);

				// Set the metadata (modMdName) to the given value (modMdValue):
				//for (int i = 0; i < node.getLength(); i++) {
				for (Node node : nodeArrayList) {
					Element xmlElement = (Element)node;
					
					// Change element text content:
					if (!newMdValue.equals("null")) {
						xmlElement.setTextContent(newMdValue);
					}
					
					// Change element content
					if (!newMdName.equals("null")) {
						NamedNodeMap attributes = xmlElement.getAttributes();
						for (int j = 0; j < attributes.getLength(); j++) {
							//Element attributeElement = (Attribute)attributes.item(j);
							//xmlElement.setAttribute(attributes.item(j).getNodeName(), newMdName);
							xmlElement.setAttribute("name", newMdName);
						}
					}
				}

				// Save modifications to XML file:
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				DOMSource domSource = new DOMSource(xmlDoc);
				StreamResult streamResult = new StreamResult(new File(filePath));
				transformer.transform(domSource, streamResult);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (XPathExpressionException e) {
				e.printStackTrace();
			} catch (TransformerConfigurationException e) {
				e.printStackTrace();
			} catch (TransformerException e) {
				e.printStackTrace();
			}

			System.out.println("File modified: " + xmlFile.getAbsoluteFile());
		}
	}


	private Set<File> getFilesToModify(String mdDirectory, String fileName, String condStructureType, String condMdName, String condMdValue) {

		Set<File> setFilesToModify = new HashSet<File>();

		// Iterate over all files in the given directory and it's subdirectories. Works with "FileUtils" in Apache "commons-io" library.
		//for (File mdFile : FileUtils.listFiles(new File(mdDirectory), new WildcardFileFilter(new String[]{"meta.xml", "meta_anchor.xml"}, IOCase.INSENSITIVE), TrueFileFilter.INSTANCE)) {
		if (fileName.equals("meta") || fileName.equals("meta_anchor")) {
			
			for (File mdFile : FileUtils.listFiles(new File(mdDirectory), new WildcardFileFilter(fileName+".xml", IOCase.INSENSITIVE), TrueFileFilter.INSTANCE)) {
				// DOM Parser:
				String filePath = mdFile.getAbsolutePath();
				DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder documentBuilder = null;
				Document xmlDoc = null;
				try {
					documentBuilder = documentFactory.newDocumentBuilder();
					xmlDoc = documentBuilder.parse(filePath);

					// Only get files that match a certain condition (e. g. "AccessLicense" of a "Monograph" is "OpenAccess"). Only they should be modified.
					boolean isFileToModify = checkCondition(condStructureType, condMdName, condMdValue, xmlDoc);
					if (isFileToModify) {
						setFilesToModify.add(mdFile);
					}
				} catch (ParserConfigurationException e) {
					e.printStackTrace();
				} catch (SAXException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (XPathExpressionException e) {
					e.printStackTrace();
				}
			}
		} else {
			System.err.println("Name of meta files can only be \"meta\" or \"meta_anchor\".");
		}

		return setFilesToModify;
	}



	private boolean checkCondition(String condStructureType, String condMdName, String condMdValue, Document xmlDoc)  throws XPathExpressionException {
		String textContent;

		// Get the IDs for the requestet structure type, e. g. for "Monograph", "JournalVolume", etc.
		List<String> dmdIDs = getDMDIDs(condStructureType, xmlDoc);

		// Loop over the structure types and check if the metadata and it's value matches the requestet metadata and value.
		// E. g.: Only change a file if "TitleDocMain" (=condMdName) of a "Monograph" (=condMdValue) has the value "Title of the Monograph" (=condMdValue)
		for (String dmdID : dmdIDs) {

			// If we do not check for the value of the "name" attribute, we always have a match, so return true
			if (condMdName.equals("null")) {
				return true;
			} else {
				String xPathString = "//dmdSec[@ID=\"" + dmdID + "\"]//goobi/metadata[@name=\""+condMdName+"\"]";
				XPathFactory xPathFactory = XPathFactory.newInstance();
				XPath xPath = xPathFactory.newXPath();
				XPathExpression xPathExpr = xPath.compile(xPathString);
				NodeList nodeList = (NodeList) xPathExpr.evaluate(xmlDoc, XPathConstants.NODESET);

				// Check if there is at least 1 element with the given value (condMdName)
				if (nodeList.getLength() > 0) {
					if (condMdValue.equals("null")) { // If we do not check for the value of the element, we always have a match, so return true
						return true;
					} else {
						for (int i = 0; i < nodeList.getLength(); i++) {
							Element xmlElement = (Element)nodeList.item(i);
							textContent = xmlElement.getTextContent();
							if (textContent.equals(condMdValue)) {
								// Return true immediatly if the condition is met, because there could be other values for a duplicate metadata, which could return false.
								return true;
							}
						}
					}
				}
			}
		}
		return false;	
	}



	private List<String> getDMDIDs(String structureType, Document xmlDoc) throws XPathExpressionException {
		List<String> lstDMDIDs = new ArrayList<String>();
		XPathFactory xPathFactory = XPathFactory.newInstance();
		XPath xPath = xPathFactory.newXPath();
		XPathExpression xPathExpr = xPath.compile("//structMap[@TYPE=\"LOGICAL\"]//div[@DMDID][@TYPE=\"" + structureType + "\"]");
		NodeList nodeList = (NodeList) xPathExpr.evaluate(xmlDoc, XPathConstants.NODESET);

		for (int i = 0; i < nodeList.getLength(); i++) {
			Element xmlElement = (Element)nodeList.item(i);
			String dmdId = xmlElement.getAttribute("DMDID");
			//System.out.println(dmdId + " = " + xmlElement.getAttribute("TYPE"));
			lstDMDIDs.add(dmdId);
		}
		return lstDMDIDs;
	}



	// Backup only files that are modified
	private void makeBackupFiles(Set<File> filesToModify) {

		for (File fileToModify : filesToModify) {

			String fileToModifyName = fileToModify.getName();
			File backupDirectory = fileToModify.getParentFile();

			// Copy existing backup file to a new temporary file (add + 1 to number-suffix), so that nothing gets overwritten
			for (File existingBackupFile : FileUtils.listFiles(backupDirectory, new RegexFileFilter(fileToModifyName+"(\\.\\d+)") , TrueFileFilter.INSTANCE)) {
				String fileName = existingBackupFile.getName();


				// Get number-suffix from existing backup-files and add 1
				Pattern p = Pattern.compile("xml\\.\\d+");
				Matcher m = p.matcher(fileName);
				boolean b = m.find();
				int oldBackupNo = 0;
				int newBackupNo = 0;
				if (b) {
					oldBackupNo = Integer.parseInt(m.group(0).replace("xml.", ""));
					newBackupNo = oldBackupNo + 1;					
				}

				// Create temporary files:
				String newTempBackupFilename = "";
				File newTempBackupFile = null;

				if (fileName.matches(fileToModifyName+".[\\d]*")) {
					newTempBackupFilename = fileToModifyName + "." + newBackupNo + ".temp";
					newTempBackupFile = new File(backupDirectory+File.separator+newTempBackupFilename);
				}



				try {
					// Copy existing file to temporary backup-file with new filename:
					FileUtils.copyFile(existingBackupFile, newTempBackupFile);
				} catch (IOException e) {
					e.printStackTrace();
				}

				// Delete the existing old backup file:
				existingBackupFile.delete();
			}

			// Remove the ".temp" suffix from the newly created temporary backup-files
			for (File tempBackupFile : FileUtils.listFiles(backupDirectory, new RegexFileFilter(".*\\.temp") , TrueFileFilter.INSTANCE)) {
				String newBackupFilename = tempBackupFile.getName().replace(".temp", "");
				File newBackupFile = new File(backupDirectory+File.separator+newBackupFilename);

				try {
					// Copy temporary file to real backup-file with new filename:
					FileUtils.copyFile(tempBackupFile, newBackupFile);
				} catch (IOException e) {
					e.printStackTrace();
				}

				// Delete temporary backup file:
				tempBackupFile.delete();
			}

			// Copy meta.xml and/or meta_anchor.xml and append the suffix ".1" to it, so that it becomes the newest backup file
			for (File productiveFile : FileUtils.listFiles(backupDirectory, new WildcardFileFilter(fileToModifyName, IOCase.INSENSITIVE), TrueFileFilter.INSTANCE)) {
				// Copy current productive file and append ".1" so that it gets the newes backup-file: 
				File newBackupFile = new File(productiveFile.getAbsoluteFile()+".1");
				try {
					FileUtils.copyFile(productiveFile, newBackupFile);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			// Remove all files with a suffix bigger than 9, because Goobi keeps only max. 9 backup files:
			for (File backupFileHigher9 : FileUtils.listFiles(backupDirectory, new RegexFileFilter(fileToModifyName+"(\\.\\d{2,})") , TrueFileFilter.INSTANCE)) {
				backupFileHigher9.delete();
			}
		}
	}

	private List<Node> getElementsToModify(String mdName, String mdValue, Document xmlDoc) throws XPathExpressionException {
		List<Node> nodeArrayList = new ArrayList<Node>();

		if (!mdName.equals("null")) {
			XPathFactory xPathFactory = XPathFactory.newInstance();
			XPath xPath = xPathFactory.newXPath();
			XPathExpression xPathExpr = xPath.compile("//goobi/metadata[@name=\""+mdName+"\"]");
			NodeList nodeList = (NodeList) xPathExpr.evaluate(xmlDoc, XPathConstants.NODESET);
			if (mdValue.equals("null")) {
				for (int n = 0; n < nodeList.getLength(); n++) {
					nodeArrayList.add(nodeList.item(n));
				}
			} else {
				for (int n = 0; n < nodeList.getLength(); n++) {
					Element xmlElement = (Element)nodeList.item(n);
					String textContent = xmlElement.getTextContent();
					if (textContent.equals(mdValue)) {
						nodeArrayList.add(nodeList.item(n));
					}
				}
			}
		}

		return nodeArrayList;
	}


	private Set<File> getFilesForInsertion(String condStructureElements, String mdDirectory) {
		Set<File> filesForInsertion = new HashSet<File>();
		List<String> lstStructureElements = Arrays.asList(condStructureElements.split("\\s*,\\s*"));
		// Iterate over all files in the given directory and it's subdirectories. Works with "FileUtils" in Apache "commons-io" library.

		//for (File mdFile : FileUtils.listFiles(new File(mdDirectory), new WildcardFileFilter(new String[]{"meta.xml", "meta_anchor.xml"}, IOCase.INSENSITIVE), TrueFileFilter.INSTANCE)) {
		for (File mdFile : FileUtils.listFiles(new File(mdDirectory), new WildcardFileFilter("*.xml", IOCase.INSENSITIVE), TrueFileFilter.INSTANCE)) {
			// DOM Parser:
			String filePath = mdFile.getAbsolutePath();
			DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = null;
			Document xmlDoc = null;
			try {
				documentBuilder = documentFactory.newDocumentBuilder();
				xmlDoc = documentBuilder.parse(filePath);

				// Only get files with a structure element that is listed in condStructureElements
				for (String structureElement : lstStructureElements) {

					String xPathString = "/mets/structMap[@TYPE='LOGICAL']//div[@TYPE='"+structureElement+"']";
					XPathFactory xPathFactory = XPathFactory.newInstance();
					XPath xPath = xPathFactory.newXPath();
					XPathExpression xPathExpr = xPath.compile(xPathString);
					NodeList nodeList = (NodeList)xPathExpr.evaluate(xmlDoc, XPathConstants.NODESET);

					if (nodeList.getLength() > 0) {
						filesForInsertion.add(mdFile);
					} else {
					}
				}

			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (XPathExpressionException e) {
				e.printStackTrace();
			}
		}
		return filesForInsertion;


	}

	private List<Element> getElementsForInsertion(String condStructureElements, Document xmlDoc) {

		List<Element> elementsForInsertion = new ArrayList<Element>();
		List<String> dmdLogIds = new ArrayList<String>();
		List<String> structureElements = Arrays.asList(condStructureElements.split("\\s*,\\s*"));
		XPath xPath = XPathFactory.newInstance().newXPath();
		XPathExpression xPathExpression;



		// First, get all DMDLOG-IDs from the structMap-Node:
		for (String structureElement : structureElements) {

			try {
				xPathExpression = xPath.compile("//mets/structMap[@TYPE='LOGICAL']//div[@TYPE='" + structureElement + "']");
				NodeList nodeList = (NodeList)xPathExpression.evaluate(xmlDoc, XPathConstants.NODESET);

				if (nodeList.getLength() > 0) {
					for (int i = 0; i < nodeList.getLength(); i++) {
						if (nodeList.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE ) {
							Element structMapElement = (Element)nodeList.item(i);
							String dmdLogId = (!structMapElement.getAttribute("DMDID").isEmpty()) ? structMapElement.getAttribute("DMDID") : null;
							if (dmdLogId != null) {	
								dmdLogIds.add(dmdLogId);
							}
						}
					}
				}
			} catch (XPathExpressionException e) {
				e.printStackTrace();
			}
		}

		// Now get all subnodes of the mets:dmdSec nodes with the right ID where we want to insert the new element:
		for (String dmdLogId : dmdLogIds) {
			try {
				if (isModsMets(xmlDoc) == true) {
					xPathExpression = xPath.compile("//mets/dmdSec[@ID='" + dmdLogId + "']/mdWrap/xmlData/mods");
				} else {
					xPathExpression = xPath.compile("//mets/dmdSec[@ID='" + dmdLogId + "']/mdWrap/xmlData/mods/extension/goobi");
				}
				NodeList nodeList = (NodeList)xPathExpression.evaluate(xmlDoc, XPathConstants.NODESET);
				if (nodeList.getLength() > 0) {
					for (int i = 0; i < nodeList.getLength(); i++) {
						if (nodeList.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE ) {
							Element elementForInsertion = (Element)nodeList.item(i);
							elementsForInsertion.add(elementForInsertion);
						}
					}
				}
			} catch (XPathExpressionException e) {
				e.printStackTrace();
			}
		}

		return elementsForInsertion;

	}

	// Check if we have a classical MODS/METS document or a document with a goobi extension:
	private boolean isModsMets(Document xmlDoc) {
		boolean isMets = true;
		XPath xPath = XPathFactory.newInstance().newXPath();
		XPathExpression xPathExpression;

		// Check for the goobi mods extension. If we find it, the XML document is not a classical MODS/METS document:
		try {
			xPathExpression = xPath.compile("/mets/dmdSec/mdWrap/xmlData/mods/extension/goobi");
			NodeList nodeList = (NodeList)xPathExpression.evaluate(xmlDoc, XPathConstants.NODESET);
			if (nodeList.getLength() > 0) {
				isMets = false;
			}
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}

		return isMets;
	}

	// Check if the element already exists:	
	private boolean hasDuplicate(Element parentElement, String elementName, String attrName, String attrValue, String textContent) {
		boolean hasDuplicate = false;
		NodeList childNodes = parentElement.getChildNodes();
		if (childNodes.getLength() > 0) {
			for (int i = 0; i < childNodes.getLength(); i++) {
				if (childNodes.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
					Element childElement = (Element)childNodes.item(i);

					// Get name of child element:
					String childElementName = childElement.getTagName();

					// Check if the element with the given element name exists
					if (childElementName.equals(elementName)) {
						boolean elementExists = true;
						if (elementExists) { // The given element exists

							// Check if given text content exists
							String childElementTextContent = childElement.getTextContent().trim();
							boolean textContentExists = (childElementTextContent.equals(textContent)) ? true : false;

							// If attribute value and name are null, we don't check for them (in this case, only the text content is relevant).
							if (attrName.equals("null") && attrValue.equals("null")) {
								if (textContentExists) { // Element exists with the given text content. 
									hasDuplicate = true; // The new element would be a duplicate
								} else { // Element exists but not with the given text content.
									hasDuplicate = false; // The new element wouldn't be a duplicate
								}
							} else { // If attribute value and name are not null, check if they are the same as the given value.

								// Check if child element has the given attribute
								boolean elementHasAttr = childElement.hasAttribute(attrName);

								if (elementHasAttr) { // The given element has the given attribute
									// Check if the attribute has the given value
									String childElementAttrValue = childElement.getAttribute(attrName);
									if (childElementAttrValue.equals(attrValue)) {
										if (textContentExists) { // Element exists with the given text content. 
											hasDuplicate = true; // The attribute contains the given attribute value, so the new element would be a duplicate.
										} else { // Element exists but not with the given text content.
											hasDuplicate = false; // The new element wouldn't be a duplicate
										}
									} else {
										hasDuplicate = false; // The attribute does not contain the given attribute value, so the new element would not be a duplicate.
									}
								} else {
									hasDuplicate = false; // The attribute does not exist, so the new element would not be a duplicate.
								}
							}
						}
					}
				}
			}
		}

		return hasDuplicate;
	}



}
