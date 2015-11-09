package org.apache.chemistry.opencmis.client.bindings.spi;

import com.engagepoint.idp.client.IdPClient;
import com.engagepoint.idp.client.http.HttpIdPClient;
import org.apache.chemistry.opencmis.commons.SessionParameter;

public abstract class AbstractSSOAuthenticationProvider<T> extends AbstractAuthenticationProvider {

    private static final String AUTHENTICATION_TOKEN = "authentication.token";

    private IdPClient idPClient;

    public T getAuthenticationToken() {
        AuthenticationTokenHolder tokenHolder = (AuthenticationTokenHolder) getSession().get(AUTHENTICATION_TOKEN);
        if (tokenHolder == null || tokenHolder.isExpired()) {
            tokenHolder = issueAuthenticationToken();
            getSession().put(AUTHENTICATION_TOKEN, tokenHolder, false);
        }
        return (T)tokenHolder.getToken();
    }

    protected IdPClient getHttpIdPClient() {
        if (idPClient == null) {
            idPClient = new HttpIdPClient()
                    .tokenIssuerUrl(getTokenIssuerUrl())
                    .catcherUrl(getCatherUrl())
                    .useXmlSchemaValidation(false)
                    .skipSSLCertificateValidation(true)
                    .skipSSLHostnameValidation(true);
        }
        return idPClient;
    }

    protected String getUser() {
        return getNotNullStringSessionParameter(SessionParameter.USER);
    }

    protected String getPassword() {
        String password = super.getPassword();
        return password != null ? password : "";
    }

    protected String getTokenIssuerUrl() {
        return getNotNullStringSessionParameter(SessionParameter.AUTH_SSO_TOKEN_ISSUER_URL);
    }

    protected String getCatherUrl() {
        return getNotNullStringSessionParameter(SessionParameter.AUTH_SSO_CATCHER_URL);
    }

    private String getNotNullStringSessionParameter(String key) {
        Object passwordObject = getSession().get(key);
        if (passwordObject instanceof String) {
            return (String) passwordObject;
        }
        throw new IllegalStateException(String.format("Could not find the object from a given key \"%s\"", key));
    }

    public abstract AuthenticationTokenHolder<T> issueAuthenticationToken();
}
