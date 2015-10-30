package org.apache.chemistry.opencmis.client.bindings.spi.webservices.sso;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

public class DocumentUtil {

    public static Document toDocument(Source source) {
        if (source instanceof DOMSource) {
            return  (Document) ((DOMSource) source).getNode();
        }
        throw new IllegalArgumentException("Source is wrong format");
    }

    public static Element findNotNullFirstElementNs(Node node, String namespace, String tagName) {
        NodeList elements = getNodeList(node, namespace, tagName);
        for (int i = 0; i < elements.getLength(); i++) {
            Node item = elements.item(i);
            if (item.getNodeType() == Node.ELEMENT_NODE) {
                return (Element) item;
            }
        }
        throw new IllegalArgumentException(String.format("Document element with \"%s\" tag name not found!", tagName));
    }

    private static NodeList getNodeList(Node node, String namespace, String tagName) {
        if (node.getNodeType() == Node.DOCUMENT_NODE) {
            return  ((Document) node).getElementsByTagNameNS(namespace, tagName);
        } else if (node.getNodeType() == Node.ELEMENT_NODE) {
            return  ((Element) node).getElementsByTagNameNS(namespace, tagName);
        }
        throw new IllegalArgumentException("Node can only be of document or element type");
    }
}