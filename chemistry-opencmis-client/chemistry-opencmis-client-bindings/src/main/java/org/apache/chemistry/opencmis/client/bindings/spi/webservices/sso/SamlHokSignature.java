package org.apache.chemistry.opencmis.client.bindings.spi.webservices.sso;

import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSEncryptionPart;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoFactory;
import org.apache.ws.security.message.WSSecHeader;
import org.apache.ws.security.message.WSSecSignature;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Properties;
import java.util.Vector;

public class SamlHokSignature {

    private static final String ALL_NAMESPACE = "*";
    private static final String SECURITY_TOKEN_WSSECURIRY_ATTRIBUTE = "http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd";
    private static final String CUSTOM_TOKEN_VALUE_TYPE = "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLID";
    private static final String SECURITY_SAML_TOKEN_PROFILE = "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0";
    private static final String DIGEST_ALGORITHM = "http://www.w3.org/2000/09/xmldsig#sha1";

    private static final String SECURITY_TOKEN_TAG_NAME = "SecurityTokenReference";

    private static final String DEFAULT_SECURITY_CRYPTO_PROVIDER = "org.apache.ws.security.components.crypto.Merlin";

    private static final String NAME_PARTS = "Body";
    private static final String NAMESPACE_PARTS = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String ENCODE_PARTS = "Element";
    private static final String ID_ATTRIBUTE = "ID";

    private final Properties properties;
    private final Crypto crypto;

    public SamlHokSignature(Properties properties) {
        if (properties == null) {
            properties = new Properties();
        }
        this.properties = properties;
        this.properties.setProperty(SessionParameter.SECURITY_CRYPTO_PROVIDER, DEFAULT_SECURITY_CRYPTO_PROVIDER);
        this.crypto = CryptoFactory.getInstance(properties);
    }

    public Document sign(Document document, Element samlToken) {
        WSSecHeader secHeader = new WSSecHeader();
        secHeader.insertSecurityHeader(document);
        String samlTokenId = samlToken.getAttribute(ID_ATTRIBUTE);
        WSSecSignature sign = initWSSecSignature(samlTokenId);
        try {
            sign.prepare(document, crypto, secHeader);
            Vector<WSEncryptionPart> parts = new Vector<WSEncryptionPart>();
            WSEncryptionPart singPart =
                    new WSEncryptionPart(
                            NAME_PARTS,
                            NAMESPACE_PARTS,
                            ENCODE_PARTS);
            parts.add(singPart);
            sign.addReferencesToSign(parts, secHeader);
            sign.prependToHeader(secHeader);
            sign.computeSignature();
            addWsse11AttrToSTR(secHeader);

        } catch (WSSecurityException e) {
            throw new IllegalStateException(e);
        }

        insertSamlAssertionBeforeSignature(document, samlToken, secHeader);

        return document;
    }

    private WSSecSignature initWSSecSignature(String samlTokenId) {
        WSSecSignature sign = new WSSecSignature();
        sign.setUserInfo(properties.getProperty(SessionParameter.SECURITY_CRYPTO_KEYSTORE_ALIAS),
                properties.getProperty(SessionParameter.SECURITY_CRYPTO_KEYSTORE_PRIVATE_PASSWORD));
        sign.setKeyIdentifierType(WSConstants.CUSTOM_KEY_IDENTIFIER);
        sign.setCustomTokenValueType(CUSTOM_TOKEN_VALUE_TYPE);
        sign.setCustomTokenId(samlTokenId);
        sign.setSignatureAlgorithm(WSConstants.RSA);
        sign.setDigestAlgo(DIGEST_ALGORITHM);

        return sign;
    }

    private static void addWsse11AttrToSTR(WSSecHeader secHeader) {
        NodeList securityTokenReference = secHeader.getSecurityHeader().getElementsByTagNameNS(ALL_NAMESPACE, SECURITY_TOKEN_TAG_NAME);
        for (int i = 0; i < securityTokenReference.getLength(); i++) {
            Node item = securityTokenReference.item(i);
            if (item.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) item;
                element.setAttribute("xmlns:wsse11", SECURITY_TOKEN_WSSECURIRY_ATTRIBUTE);
                element.setAttribute("wsse11:TokenType", SECURITY_SAML_TOKEN_PROFILE);
            }
        }
    }

    private void insertSamlAssertionBeforeSignature(Document document, Element samlToken, WSSecHeader secHeader) {
        Node firstChild = secHeader.getSecurityHeader().getFirstChild();
        secHeader.getSecurityHeader().insertBefore(document.importNode(samlToken, true), firstChild);
    }
}
