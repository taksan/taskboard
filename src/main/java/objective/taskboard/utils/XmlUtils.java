/*-
 * [LICENSE]
 * Taskboard
 * - - -
 * Copyright (C) 2015 - 2016 Objective Solutions
 * - - -
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * [/LICENSE]
 */
package objective.taskboard.utils;

import org.apache.xalan.processor.TransformerFactoryImpl;
import org.apache.xerces.jaxp.DocumentBuilderFactoryImpl;
import org.apache.xpath.jaxp.XPathFactoryImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;

public class XmlUtils {

    public static NodeList xpath(File xmlFile, String locator) {
        return xpath(asDocument(xmlFile), locator);
    }

    public static NodeList xpath(String xmlString, String locator) {
        return xpath(asDocument(xmlString), locator);
    }

    private static NodeList xpath(Document doc, String locator) {
        try {
            // create XPath
            XPathFactory xPathfactory = new XPathFactoryImpl();
            XPath xpath = xPathfactory.newXPath();
            XPathExpression expr = xpath.compile(locator);

            // searches Document using XPath
            return (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        } catch (Exception e) {
            throw new InvalidXPathOperationException(e);
        }
    }

    public static Document asDocument(String xmlString) {
        try {
            return asDocument(new ByteArrayInputStream(xmlString.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            throw new InvalidXmlException(e);
        }
    }

    public static Document asDocument(File xmlFile) {
        try {
            DocumentBuilderFactory factory = new DocumentBuilderFactoryImpl();
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(xmlFile);
        } catch (Exception e) {
            throw new InvalidXmlException(e);
        }
    }

    public static Document asDocument(InputStream stream) {
        return asDocument(new InputSource(stream));
    }

    public static Document asDocument(Path path) {
        try {
            return asDocument(Files.newInputStream(path));
        } catch (IOException e) {
            throw new InvalidXmlException(e);
        }
    }

    public static Document asDocument(InputSource source) {
        try {
            DocumentBuilderFactory factory = new DocumentBuilderFactoryImpl();
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(source);
        } catch (Exception e) {
            throw new InvalidXmlException(e);
        }
    }

    public static String asString(NodeList nodeList) throws TransformerException {
        if(nodeList.getLength() > 0) {
            StringWriter writer = new StringWriter();
            for(int i = 0; i < nodeList.getLength(); ++i) {
                Node node = nodeList.item(i);
                if (i > 0)
                    writer.append("\n");
                switch (node.getNodeType()) {
                    case Node.ATTRIBUTE_NODE:
                    case Node.TEXT_NODE:
                        writer.append(node.getNodeValue());
                        break;
                    default:
                        TransformerFactoryImpl factory = new TransformerFactoryImpl();
                        Transformer transformer = factory.newTransformer();
                        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                        transformer.transform(new DOMSource(node), new StreamResult(writer));
                }
            }
            return writer.toString();
        }
        return "";
    }

    public static void format(File inputXmlFile, File outputXmlFile) {
        try {
            Document document = asDocument(inputXmlFile);
            TransformerFactory transFactory = TransformerFactory.newInstance();
            Transformer idTransform = transFactory.newTransformer();
            idTransform.setOutputProperty(OutputKeys.METHOD, "xml");
            idTransform.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            idTransform.setOutputProperty(OutputKeys.INDENT, "yes");
            idTransform.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            Source input = new DOMSource(document);
            Result output = new StreamResult(outputXmlFile);
            idTransform.transform(input, output);
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    public static class InvalidXPathOperationException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        private static final String MESSAGE = "Invalid XPath Operation";
        public InvalidXPathOperationException() {
            super(MESSAGE);
        }
        public InvalidXPathOperationException(Exception e) {
            super(MESSAGE, e);
        }
    }

    public static class InvalidXmlException extends RuntimeException {

    	private static final long serialVersionUID = 1L;

    	private static final String MESSAGE = "Invalid Xml";
        public InvalidXmlException() {
            super(MESSAGE);
        }
        public InvalidXmlException(Exception e) {
            super(MESSAGE, e);
        }
    }
}
