package org.apache.chemistry.opencmis.client.bindings.webservices.sso;

import org.apache.chemistry.opencmis.client.bindings.impl.SessionImpl;
import org.apache.chemistry.opencmis.client.bindings.spi.BindingSession;
import org.apache.chemistry.opencmis.client.bindings.spi.SamlTokenAuthenticationProvider;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static org.apache.chemistry.opencmis.client.bindings.AuthenticationProviderTestHelper.*;
import static org.junit.Assert.assertEquals;

public class SamlTokenAuthenticationProviderTest {

    private static final String SAML_NS = "urn:oasis:names:tc:SAML:2.0:assertion";
    private static final String ASSERTION_EXPIRE_TIME_ATTR_NAME = "NotOnOrAfter";


    private SamlTokenAuthenticationProvider samlTokenAuthenticationProvider;

    @Before
    public void setUp() throws Exception {
        samlTokenAuthenticationProvider = buildIdPClientMock(SamlTokenAuthenticationProvider.class);
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

    private Element geConditionElement(Element token) {
        NodeList conditions = token.getElementsByTagNameNS(SAML_NS, "Conditions");
        return (Element) conditions.item(0);
    }

    private String getDigestValueFromAssertionNode(Element element) {
        Node item = element.getElementsByTagName("ds:DigestValue").item(0);
        return item.getFirstChild().getNodeValue();
    }
}
