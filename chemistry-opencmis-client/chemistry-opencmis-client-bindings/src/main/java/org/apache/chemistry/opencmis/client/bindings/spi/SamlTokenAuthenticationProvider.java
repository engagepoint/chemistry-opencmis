package org.apache.chemistry.opencmis.client.bindings.spi;

import org.apache.chemistry.opencmis.client.bindings.spi.webservices.sso.DocumentUtil;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;

public class SamlTokenAuthenticationProvider extends AbstractSSOAuthenticationProvider<Element> {

    private static final String WSSE_NAMESPACE = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    private static final String SAML_NS = "urn:oasis:names:tc:SAML:2.0:assertion";
    private static final String ERROR_MESSAGE = "Could not build SOAP header: ";
    private static final String ASSERTION_EXPIRE_TIME_ATTR_NAME = "NotOnOrAfter";
    private static final String ASSERTION_START_TIME_ATTR_NAME = "NotBefore";
    private static final String SECURITY_TAG_NAME = "Security";


    @Override
    public Element getSOAPHeaders(Object portObject) {
        try {
            Document document = XMLUtils.newDomDocument();
            Element wsseSecurityElement = document.createElementNS(WSSE_NAMESPACE, SECURITY_TAG_NAME);
            Element element = getAuthenticationToken();
            Node samlNode = document.importNode(element, true);
            wsseSecurityElement.appendChild(samlNode);
            return wsseSecurityElement;
        } catch (ParserConfigurationException e) {
            throw new CmisRuntimeException(ERROR_MESSAGE + e.getMessage(), e);
        }
    }

    public SamlTokenHolder issueAuthenticationToken() {
        String samlToken = getHttpIdPClient().getSamlToken(getUser(), getPassword());
        return new SamlTokenHolder(convertStrToElement(samlToken));
    }

    private Element convertStrToElement(String string)  {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            return documentBuilder.parse(new ByteArrayInputStream(string.getBytes())).getDocumentElement();
        } catch (SAXException e) {
            throw new IllegalStateException(ERROR_MESSAGE + e.getMessage(), e);
        } catch (IOException e) {
            throw new IllegalStateException(ERROR_MESSAGE + e.getMessage(), e);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(ERROR_MESSAGE + e.getMessage(), e);
        }
    }

    private static class SamlTokenHolder implements AuthenticationTokenHolder<Element> {
        private static final String CONDITIONS_TAG_NAME = "Conditions";

        private Element samlTokenElement;
        private long expireIn;

        public SamlTokenHolder(Element samlTokenElement) {
            this.samlTokenElement = samlTokenElement;
            this.expireIn = calculateExpireInTime(samlTokenElement);
        }

        public Element getToken() {
            return samlTokenElement;
        }

        public boolean isExpired() {
            return new Date(expireIn).before(new Date());
        }

        private long calculateExpireInTime(Element token) {
            long notOnOrAfterTime = extractTime(token, ASSERTION_EXPIRE_TIME_ATTR_NAME);
            long notBeforeTime = extractTime(token, ASSERTION_START_TIME_ATTR_NAME);
            long diffTime = notOnOrAfterTime - notBeforeTime;
            return new Date().getTime() + diffTime;
        }

        private long extractTime(Element token, String attributeName) {
            String notOnOrAfter = geConditionElement(token).getAttribute(attributeName);
            return DatatypeConverter.parseDateTime(notOnOrAfter).getTime().getTime();
        }

        private Element geConditionElement(Element document) {
            return DocumentUtil.findNotNullFirstElementNs(document,SAML_NS, CONDITIONS_TAG_NAME);
        }
    }
}
