package org.apache.chemistry.opencmis.client.bindings.webservices.sso;


import org.apache.chemistry.opencmis.client.bindings.impl.SessionImpl;
import org.apache.chemistry.opencmis.client.bindings.spi.BindingSession;
import org.apache.chemistry.opencmis.client.bindings.spi.webservices.sso.DocumentUtil;
import org.apache.chemistry.opencmis.client.bindings.spi.webservices.sso.SamlHokSignatureGenerateHandler;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.io.IOException;
import java.net.URL;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SamlHokSignatureGenerateHandlerTest {

    private static final String KEY_STORE_FILE = "key.jks";
    private static final String TEST_SOAP_REQUEST_XML = "soapRequest.xml";
    private static final String CALCULATED_DIGEST_VALUE = "Bkr2HD7bmNYYw4lJa6FJyE/sBYc=";
    private static final int SIGNED_NODE_INDEX = 1;

    private SamlHokSignatureGenerateHandler samlHokSignatureGenerateHandler;
    private Source domSource;

    @Before
    public void setUp() throws Exception {
        samlHokSignatureGenerateHandler = new SamlHokSignatureGenerateHandler(initSession());
    }

    @Test
    public void testSignCorrectSoapMessage() throws Exception {
        initDomSource();
        SOAPMessageContext soapMessageContextMock = buildSoapMessageContextMock(false);

        samlHokSignatureGenerateHandler.handleMessage(soapMessageContextMock);
        Document document = DocumentUtil.toDocument(domSource);
        String digestValue = getDigestValueFromSignedNode(document);
        Assert.assertEquals(digestValue, CALCULATED_DIGEST_VALUE);
    }

    @Test(expected = CmisRuntimeException.class)
    public void testHandleWrongSoapMessage() throws Exception {
        SOAPMessageContext soapMessageContextMock = buildSoapMessageContextMock(true);

        samlHokSignatureGenerateHandler.handleMessage(soapMessageContextMock);
    }

    private SOAPMessageContext buildSoapMessageContextMock(boolean thenThrowException) throws SOAPException {
        SOAPMessageContext soapMessageContextMock = mock(SOAPMessageContext.class);
        SOAPMessage soapMessageMock = mock(SOAPMessage.class);
        SOAPPart soapPartMock = mock(SOAPPart.class);
        when(soapMessageContextMock.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY)).thenReturn(true);
        when(soapMessageContextMock.getMessage()).thenReturn(soapMessageMock);
        when(soapMessageMock.getSOAPPart()).thenReturn(soapPartMock);
        if (thenThrowException) {
            when(soapPartMock.getContent()).thenThrow(SOAPException.class);
        } else {
            when(soapPartMock.getContent()).thenReturn(domSource);
        }
        return soapMessageContextMock;
    }

    private void initDomSource() throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilder builder = initDocumentBuilder();
        Document parse = builder.parse(getTestResourceURL(TEST_SOAP_REQUEST_XML).openStream());
        domSource = new DOMSource(parse);
    }

    private static DocumentBuilder initDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", true);
        return documentBuilderFactory.newDocumentBuilder();
    }

    private BindingSession initSession() {
        BindingSession session = new SessionImpl();
        session.put(SessionParameter.SECURITY_CRYPTO_FILE, getTestResourceURL(KEY_STORE_FILE).getPath());
        session.put(SessionParameter.SECURITY_CRYPTO_KEYSTORE_TYPE, "JKS");
        session.put(SessionParameter.SECURITY_CRYPTO_KEYSTORE_PASSWORD, "12345");
        session.put(SessionParameter.SECURITY_CRYPTO_KEYSTORE_ALIAS, "default");
        session.put(SessionParameter.SECURITY_CRYPTO_KEYSTORE_PRIVATE_PASSWORD, "12345");
        return session;
    }

    private String getDigestValueFromSignedNode(Document document) {
        Node item = document.getElementsByTagName("ds:DigestValue").item(SIGNED_NODE_INDEX);
        return item.getFirstChild().getNodeValue();
    }

    private URL getTestResourceURL(String resource) {
        return Thread.currentThread().getContextClassLoader().getResource(resource);
    }
}
