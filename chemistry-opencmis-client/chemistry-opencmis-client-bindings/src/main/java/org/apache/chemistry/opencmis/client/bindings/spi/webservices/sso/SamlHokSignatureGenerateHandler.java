package org.apache.chemistry.opencmis.client.bindings.spi.webservices.sso;

import org.apache.chemistry.opencmis.client.bindings.spi.BindingSession;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.util.Properties;
import java.util.Set;

public class SamlHokSignatureGenerateHandler implements SOAPHandler<SOAPMessageContext> {

    private static final String ASSERTION_TAG_NAME = "Assertion";
    private static final String SECURITY_TAG_NAME = "Security";
    private static final String ALL_NAMESPACE = "*";
    private static final String ERROR_MESSAGE = "Could not sign SOAP body: ";

    private final SamlHokSignature samlHokSignature;


    public SamlHokSignatureGenerateHandler(BindingSession session) {
        Properties properties = prepareCryptoConfiguration(session);
        this.samlHokSignature = new SamlHokSignature(properties);
    }

    public boolean handleMessage(SOAPMessageContext context) {
        boolean outbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        if (outbound) {
            Source source = getMessageContent(context);
            Document document =  DocumentUtil.toDocument(source);
            Element header = getSoapHeader(document);
            Element securityHeader = getSecurityHeader(document);
            Element samlToken = getSamlToken(document);
            removeSecurityFromHeader(securityHeader, header);
            samlHokSignature.sign(document, samlToken);
        }
        return true;
    }

    public boolean handleFault(SOAPMessageContext context) {
        return true;
    }

    public void close(MessageContext context) {
    }

    public Set<QName> getHeaders() {
        return null;
    }

    private void setCryptoPropertyFromSession(String key, BindingSession session, Properties properties) {
        properties.setProperty(key, session.get(key, "").toString());
    }

    private void removeSecurityFromHeader(Element securityHeader, Element header) {
        header.removeChild(securityHeader);
    }

    private Element getSoapHeader(Document document) {
        Node header = getSecurityHeader(document).getParentNode();
        return (Element) header;
    }

    private Element getSamlToken(Document document) {
        return DocumentUtil.findNotNullFirstElementNs(document,
                ALL_NAMESPACE, ASSERTION_TAG_NAME);
    }

    private Element getSecurityHeader(Document document) {
        return DocumentUtil.findNotNullFirstElementNs(document,
                ALL_NAMESPACE, SECURITY_TAG_NAME);
    }

    private Source getMessageContent(SOAPMessageContext context)  {
        try {
            SOAPMessage message = context.getMessage();
            return message.getSOAPPart().getContent();
        } catch (SOAPException e) {
            throw new CmisRuntimeException(ERROR_MESSAGE + e.getMessage(), e);
        }
    }

    private Properties prepareCryptoConfiguration(BindingSession session) {
        Properties properties = new Properties();
        setCryptoPropertyFromSession(SessionParameter.SECURITY_CRYPTO_FILE, session, properties);
        setCryptoPropertyFromSession(SessionParameter.SECURITY_CRYPTO_KEYSTORE_TYPE, session, properties);
        setCryptoPropertyFromSession(SessionParameter.SECURITY_CRYPTO_KEYSTORE_PASSWORD, session, properties);
        setCryptoPropertyFromSession(SessionParameter.SECURITY_CRYPTO_KEYSTORE_ALIAS, session, properties);
        setCryptoPropertyFromSession(SessionParameter.SECURITY_CRYPTO_KEYSTORE_PRIVATE_PASSWORD, session, properties);
        return properties;
    }
}
