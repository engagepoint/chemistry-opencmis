package org.apache.chemistry.opencmis.client.bindings.webservices.sso;

import com.engagepoint.idp.client.IdPClient;
import org.apache.chemistry.opencmis.client.bindings.impl.SessionImpl;
import org.apache.chemistry.opencmis.client.bindings.spi.BindingSession;
import org.apache.chemistry.opencmis.client.bindings.spi.webservices.sso.SamlTokenAuthenticationProvider;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SamlTokenAuthenticationProviderTest {

    private static final String SAML_NS = "urn:oasis:names:tc:SAML:2.0:assertion";
    private static final String USER = "test";
    private static final String PASSWORD = "test";
    private static final String CATCHER_URL = "https://localhost/catcher";
    private static final String TOKEN_ISSUER_URL = "https://localhost/roken/isser";
    private static final String CALCULATED_DIGEST_VALUE = "3OQYy83A1fvIrQH1rruY8j/SJKY=";
    private static final String ASSERTION_EXPIRE_TIME_ATTR_NAME = "NotOnOrAfter";
    private static final String SAML_ASSERTION = "samlAssertion.xml";
    private static final String ISSUED_SAML_ASSERTION = "issuedSamlAssertion.xml";
    private static final String EXPIRED_TIME = "2015-10-22T10:37:19Z";
    private static final String SSO_SAML_TOKEN = "sso.saml.token";


    private SamlTokenAuthenticationProvider samlTokenAuthenticationProvider;

    @Before
    public void setUp() throws Exception {
        samlTokenAuthenticationProvider = new SamlTokenAuthenticationProvider();
        setIdPClientMock();
    }

    @Test
    public void testSoapAssertion() {
        samlTokenAuthenticationProvider.setSession(initSessionWithCorrectParameters());

        Element soapHeaders = samlTokenAuthenticationProvider.getSOAPHeaders(new Object());

        String digestValue = getDigestValueFromAssertionNode(soapHeaders);
        assertEquals(digestValue, CALCULATED_DIGEST_VALUE);
    }

    @Test(expected = IllegalStateException.class)
    public void testSoapAssertionWhenWrongCredential() {
        samlTokenAuthenticationProvider.setSession(new SessionImpl());

        samlTokenAuthenticationProvider.getSOAPHeaders(new Object());
    }

    @Test
    public void testSoapAssertionWhenDataExpired() throws Exception {
        BindingSession session = initSessionWithCorrectParameters();
        samlTokenAuthenticationProvider.setSession(session);
        samlTokenAuthenticationProvider.getSOAPHeaders(new Object());
        setSamlTokenMockToSession(session);

        Element token = samlTokenAuthenticationProvider.getSOAPHeaders(new Object());

        String expiredTime = geConditionElement(token).getAttribute(ASSERTION_EXPIRE_TIME_ATTR_NAME);
        assertEquals(expiredTime, EXPIRED_TIME);
    }

    private BindingSession initSessionWithCorrectParameters() {
        SessionImpl session = new SessionImpl();
        session.put(SessionParameter.USER, USER);
        session.put(SessionParameter.PASSWORD, PASSWORD);
        session.put(SessionParameter.AUTH_SSO_TOKEN_ISSUER_URL, TOKEN_ISSUER_URL);
        session.put(SessionParameter.AUTH_SSO_CATCHER_URL, CATCHER_URL);
        return session;
    }

    private void setSamlTokenMockToSession(BindingSession session) {
        SamlTokenAuthenticationProvider.SamlTokenHolder samlTokenHolderMock =
                mock(SamlTokenAuthenticationProvider.SamlTokenHolder.class);
        when(samlTokenHolderMock.isExpired()).thenReturn(true);
        session.put(SSO_SAML_TOKEN, samlTokenHolderMock);
    }

    private void setIdPClientMock() throws IOException {
        IdPClient idPClient = buildIdPClientMock();
        Whitebox.setInternalState(samlTokenAuthenticationProvider, "idPClient", idPClient);
    }

    private String getDigestValueFromAssertionNode(Element element) {
        Node item = element.getElementsByTagName("ds:DigestValue").item(0);
        return item.getFirstChild().getNodeValue();
    }

    private IdPClient buildIdPClientMock() throws IOException {
        IdPClient idPClient = mock(IdPClient.class);

        String assertion = getSamlAssertion(SAML_ASSERTION);
        String issuedAssertion = getSamlAssertion(ISSUED_SAML_ASSERTION);
        when(idPClient.getSamlToken(USER, PASSWORD))
                .thenReturn(assertion)
                .thenReturn(issuedAssertion);
        return idPClient;
    }

    private Element geConditionElement(Element token) {
        NodeList conditions = token.getElementsByTagNameNS(SAML_NS, "Conditions");
        return (Element) conditions.item(0);
    }

    private URL getTestResourceURL(String resource) {
        URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
        assertNotNull(url);
        return url;
    }

    private String getSamlAssertion(String name) throws IOException {
        List<String> lines = IOUtils.readLines(getTestResourceURL(name).openStream());
        String xml = "";
        for (String line : lines) {
            xml += line;
        }
        return xml;
    }
}
