package org.apache.chemistry.opencmis.client.bindings.spi.webservices.sso;

import com.engagepoint.idp.client.IdPClient;
import com.engagepoint.idp.client.http.HttpIdPClient;
import org.apache.chemistry.opencmis.client.bindings.spi.StandardAuthenticationProvider;
import org.apache.chemistry.opencmis.commons.SessionParameter;
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
import java.io.Serializable;
import java.util.Date;

public class SamlTokenAuthenticationProvider extends StandardAuthenticationProvider {

    private static final String WSSE_NAMESPACE = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    private static final String SAML_NS = "urn:oasis:names:tc:SAML:2.0:assertion";
    private static final String ERROR_MESSAGE = "Could not build SOAP header: ";
    private static final String ASSERTION_EXPIRE_TIME_ATTR_NAME = "NotOnOrAfter";
    private static final String ASSERTION_START_TIME_ATTR_NAME = "NotBefore";
    private static final String SSO_SAML_TOKEN = "sso.saml.token";
    private static final String SECURITY_TAG_NAME = "Security";

    private IdPClient idPClient;


    @Override
    public Element getSOAPHeaders(Object portObject) {
        initHttpIdPClient();
        String user = getUser();
        String password = getPassword();

        try {
            Document document = XMLUtils.newDomDocument();
            Element wsseSecurityElement = document.createElementNS(WSSE_NAMESPACE, SECURITY_TAG_NAME);
            Element element = getSamlTokenElement(user, password);
            Node samlNode = document.importNode(element, true);
            wsseSecurityElement.appendChild(samlNode);
            return wsseSecurityElement;
        } catch (ParserConfigurationException e) {
            throw new CmisRuntimeException(ERROR_MESSAGE + e.getMessage(), e);
        }
    }

    private void initHttpIdPClient() {
        if (idPClient == null) {
            idPClient = new HttpIdPClient()
                    .tokenIssuerUrl(getTokenIssuerUrl())
                    .catcherUrl(getCatherUrl())
                    .useXmlSchemaValidation(false)
                    .skipSSLCertificateValidation(true)
                    .skipSSLHostnameValidation(true);
        }
    }

    private Element getSamlTokenElement(String user, String password) {
        SamlTokenHolder samlTokenHolder = (SamlTokenHolder) getSession().get(SSO_SAML_TOKEN);
        if (samlTokenHolder == null || samlTokenHolder.isExpired()) {
            samlTokenHolder = issueSamlToken(user, password);
            getSession().put(SSO_SAML_TOKEN, samlTokenHolder, false);
        }
        return samlTokenHolder.getElement();
    }

    private SamlTokenHolder issueSamlToken(String username, String password) {
        String samlToken = idPClient.getSamlToken(username, password);
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

    protected String getUser() {
        return getNotNullStringSessionParameter(SessionParameter.USER);
    }

    protected String getPassword() {
        String password = super.getPassword();
        return password != null ? password : "";
    }

    private String getTokenIssuerUrl() {
        return getNotNullStringSessionParameter(SessionParameter.AUTH_SSO_TOKEN_ISSUER_URL);
    }

    private String getCatherUrl() {
        return getNotNullStringSessionParameter(SessionParameter.AUTH_SSO_CATCHER_URL);
    }

    private String getNotNullStringSessionParameter(String key) {
        Object passwordObject = getSession().get(key);
        if (passwordObject instanceof String) {
            return (String) passwordObject;
        }
        throw new IllegalStateException(String.format("Could not find the object from a given key \"%s\"", key));
    }

    public static class SamlTokenHolder implements Serializable {
        private static final String CONDITIONS_TAG_NAME = "Conditions";

        private Element samlTokenElement;
        private long expiredTime;

        public SamlTokenHolder(Element samlTokenElement) {
            this.samlTokenElement = samlTokenElement;
            this.expiredTime = calculateExpiredTime(samlTokenElement);
        }

        public Element getElement() {
            return samlTokenElement;
        }

        public boolean isExpired() {
            return new Date(expiredTime).before(new Date());
        }

        private long calculateExpiredTime(Element token) {
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
