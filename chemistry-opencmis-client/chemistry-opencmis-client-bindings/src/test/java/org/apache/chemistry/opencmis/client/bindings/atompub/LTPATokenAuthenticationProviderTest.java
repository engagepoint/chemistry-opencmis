package org.apache.chemistry.opencmis.client.bindings.atompub;

import org.apache.chemistry.opencmis.client.bindings.spi.BindingSession;
import org.apache.chemistry.opencmis.client.bindings.spi.LTPATokenAuthenticationProvider;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.apache.chemistry.opencmis.client.bindings.AuthenticationProviderTestHelper.*;
import static org.junit.Assert.assertTrue;

public class LTPATokenAuthenticationProviderTest {

    LTPATokenAuthenticationProvider authenticationProvider;

    @Before
    public void setUp() throws Exception {
        authenticationProvider = buildIdPClientMock(LTPATokenAuthenticationProvider.class);
    }

    @Test
    public void testGetHttpHeader() {
        authenticationProvider.setSession(initSessionWithCorrectParameters());

        Map<String, List<String>> httpHeaders = authenticationProvider.getHTTPHeaders("");

        assertTrue(getLtpaCookie(httpHeaders).contains(LTPA_TOKEN));
    }

    @Test
    public void testGetHttpHeaderWhenTokenExpired() {

        BindingSession session = initSessionWithCorrectParameters();
        authenticationProvider.setSession(session);
        authenticationProvider.getHTTPHeaders("");
        setSamlTokenMockToSession(session);

        Map<String, List<String>> httpHeaders = authenticationProvider.getHTTPHeaders("");

        assertTrue(getLtpaCookie(httpHeaders).contains(ISSUED_LTPA_TOKEN));
    }

    private String getLtpaCookie(Map<String, List<String>> httpHeaders) {
        return httpHeaders.get("Cookie").get(0);
    }

}
