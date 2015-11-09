package org.apache.chemistry.opencmis.client.bindings.spi;

import org.apache.chemistry.opencmis.commons.SessionParameter;

import java.util.*;

public class LTPATokenAuthenticationProvider extends AbstractSSOAuthenticationProvider<String> {

    private static final String COOKIE_NAME = "LtpaToken2";
    private static final String COOKIE_HEADER = "Cookie";


    @Override
    public Map<String, List<String>> getHTTPHeaders(String url) {
        String ltpaToken = getAuthenticationToken();
        Map<String, List<String>> result = new HashMap<String, List<String>>();
        result.put(COOKIE_HEADER, Collections.singletonList(COOKIE_NAME + "=" + ltpaToken));
        return result;
    }

    public LtpaTokenHolder issueAuthenticationToken() {
        String ltpaToken = getHttpIdPClient().getLtpaToken(getUser(), getPassword());
        int expiredTime = getTokenExpiredTimeInMinutes();
        long expireIn = new Date().getTime() + expiredTime * 60 * 1000;
        return new LtpaTokenHolder(ltpaToken, expireIn);
    }

    private int getTokenExpiredTimeInMinutes() {
        return Integer.parseInt((String)getSession().get(SessionParameter.AUTH_SSO_AUTH_ATOMPUB_EXPIRED_TIME));
    }

    private class LtpaTokenHolder implements AuthenticationTokenHolder<String> {
        private String token;
        private long expireIn;

        public LtpaTokenHolder(String token, long expireIn) {
            this.token = token;
            this.expireIn = expireIn;
        }

        public String getToken() {
            return token;
        }

        public boolean isExpired() {
            return new Date(expireIn).before(new Date());
        }
    }
}
