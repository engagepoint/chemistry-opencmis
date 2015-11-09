package org.apache.chemistry.opencmis.client.bindings;

import com.engagepoint.idp.client.IdPClient;
import org.apache.chemistry.opencmis.client.bindings.impl.SessionImpl;
import org.apache.chemistry.opencmis.client.bindings.spi.AuthenticationTokenHolder;
import org.apache.chemistry.opencmis.client.bindings.spi.BindingSession;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.commons.io.IOUtils;
import org.mockito.internal.util.reflection.Whitebox;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthenticationProviderTestHelper {

    public static final String CALCULATED_DIGEST_VALUE = "3OQYy83A1fvIrQH1rruY8j/SJKY=";
    public static final String EXPIRED_TIME = "2015-10-22T10:37:19Z";
    public static final String LTPA_TOKEN = "4KTmKCy9hrWCQ+HnnWziRusbyRJDj91w9wsH5Ywb0qJGO..";
    public static final String ISSUED_LTPA_TOKEN = "6UJsT56nTFgRQ+HnnWziRusbyRJDj91w9wsH5Ywb0qJGO..";
    public static final String AUTHENTICATION_TOKEN = "authentication.token";


    public static final String USER = "test";
    public static final String PASSWORD = "test";
    public static final String CATCHER_URL = "https://localhost/catcher";

    public static final String TOKEN_ISSUER_URL = "https://localhost/roken/isser";
    public static final String SAML_ASSERTION = "samlAssertion.xml";
    public static final String ISSUED_SAML_ASSERTION = "issuedSamlAssertion.xml";

    public static  <T> T buildIdPClientMock(Class<T> authenticationProviderClass) throws Exception {
        Object instance = authenticationProviderClass.newInstance();
        IdPClient idPClient = buildIdPClientMock();
        Whitebox.setInternalState(instance, "idPClient", idPClient);
        return (T)instance;
    }

    public static void setSamlTokenMockToSession(BindingSession session) {
        AuthenticationTokenHolder tokenHolder =
                mock(AuthenticationTokenHolder.class);
        when(tokenHolder.isExpired()).thenReturn(true);
        session.put(AUTHENTICATION_TOKEN, tokenHolder);
    }

    public static BindingSession initSessionWithCorrectParameters() {
        SessionImpl session = new SessionImpl();
        session.put(SessionParameter.USER, USER);
        session.put(SessionParameter.PASSWORD, PASSWORD);
        session.put(SessionParameter.AUTH_SSO_TOKEN_ISSUER_URL, TOKEN_ISSUER_URL);
        session.put(SessionParameter.AUTH_SSO_CATCHER_URL, CATCHER_URL);
        session.put(SessionParameter.AUTH_SSO_AUTH_ATOMPUB_EXPIRED_TIME, "120");
        return session;
    }

    private static IdPClient buildIdPClientMock() throws IOException {
        IdPClient idPClient = mock(IdPClient.class);

        String assertion = getSamlAssertion(SAML_ASSERTION);
        String issuedAssertion = getSamlAssertion(ISSUED_SAML_ASSERTION);
        when(idPClient.getSamlToken(USER, PASSWORD))
                .thenReturn(assertion)
                .thenReturn(issuedAssertion);
        when(idPClient.getLtpaToken(USER, PASSWORD))
                .thenReturn(LTPA_TOKEN)
                .thenReturn(ISSUED_LTPA_TOKEN);
        return idPClient;
    }

    private static URL getTestResourceURL(String resource) {
        URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
        assertNotNull(url);
        return url;
    }

    private static String getSamlAssertion(String name) throws IOException {
        List<String> lines = IOUtils.readLines(getTestResourceURL(name).openStream());
        String xml = "";
        for (String line : lines) {
            xml += line;
        }
        return xml;
    }
}
