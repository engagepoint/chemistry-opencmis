/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.chemistry.opencmis.workbench.model;

import org.apache.chemistry.opencmis.client.api.*;
import org.apache.chemistry.opencmis.client.bindings.CmisBindingFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.client.runtime.cache.Cache;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.CapabilityAcl;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.spi.AuthenticationProvider;
import org.apache.chemistry.opencmis.workbench.BasicLoginTab;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.Authenticator;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.X509Certificate;
import java.util.*;

public class ClientSession {

    public static final String WORKBENCH_PREFIX = "cmis.workbench.";
    public static final String OBJECT_PREFIX = WORKBENCH_PREFIX + "object.";
    public static final String FOLDER_PREFIX = WORKBENCH_PREFIX + "folder.";
    public static final String VERSION_PREFIX = WORKBENCH_PREFIX + "version.";
    public static final String ACCEPT_SELF_SIGNED_CERTIFICATES = WORKBENCH_PREFIX + "acceptSelfSignedCertificates";

    private static final String HTTPS = "https://";
    private static final String DEFAULT_TOKEN_ISSUER_PATH = "/token/issue";
    private static final String DEFAULT_CATCHER_PATH = "/engagepoint-authenticate-catcher";
    private static final String DEFAULT_CATCHER_PORT = "9443";
    private static final String DEFAULT_KEYSTORE_FILE = "keystore.jks";
    private static final String DEAULT_KEYSTORE_TYPE = "JKS";


    public enum Authentication {
        NONE, STANDARD, NTLM, SSO
    }

    private static final Set<String> FOLDER_PROPERTY_SET = new HashSet<String>();

    static {
        FOLDER_PROPERTY_SET.add(PropertyIds.OBJECT_ID);
        FOLDER_PROPERTY_SET.add(PropertyIds.OBJECT_TYPE_ID);
        FOLDER_PROPERTY_SET.add(PropertyIds.NAME);
        FOLDER_PROPERTY_SET.add(PropertyIds.CONTENT_STREAM_MIME_TYPE);
        FOLDER_PROPERTY_SET.add(PropertyIds.CONTENT_STREAM_LENGTH);
        FOLDER_PROPERTY_SET.add(PropertyIds.CONTENT_STREAM_FILE_NAME);
        FOLDER_PROPERTY_SET.add(PropertyIds.CREATED_BY);
        FOLDER_PROPERTY_SET.add(PropertyIds.CREATION_DATE);
        FOLDER_PROPERTY_SET.add(PropertyIds.LAST_MODIFIED_BY);
        FOLDER_PROPERTY_SET.add(PropertyIds.LAST_MODIFICATION_DATE);
        FOLDER_PROPERTY_SET.add(PropertyIds.IS_VERSION_SERIES_CHECKED_OUT);
        FOLDER_PROPERTY_SET.add(PropertyIds.VERSION_SERIES_CHECKED_OUT_ID);
    }

    private static final Set<String> VERSION_PROPERTY_SET = new HashSet<String>();

    static {
        VERSION_PROPERTY_SET.add(PropertyIds.OBJECT_ID);
        VERSION_PROPERTY_SET.add(PropertyIds.OBJECT_TYPE_ID);
        VERSION_PROPERTY_SET.add(PropertyIds.NAME);
        VERSION_PROPERTY_SET.add(PropertyIds.VERSION_LABEL);
        VERSION_PROPERTY_SET.add(PropertyIds.IS_LATEST_VERSION);
        VERSION_PROPERTY_SET.add(PropertyIds.IS_MAJOR_VERSION);
        VERSION_PROPERTY_SET.add(PropertyIds.IS_LATEST_MAJOR_VERSION);
        VERSION_PROPERTY_SET.add(PropertyIds.CONTENT_STREAM_MIME_TYPE);
        VERSION_PROPERTY_SET.add(PropertyIds.CONTENT_STREAM_LENGTH);
        VERSION_PROPERTY_SET.add(PropertyIds.CONTENT_STREAM_FILE_NAME);
    }

    private Map<String, String> sessionParameters;
    private List<Repository> repositories;
    private Session session;
    private OperationContext objectOperationContext;
    private OperationContext folderOperationContext;
    private OperationContext versionOperationContext;

    public ClientSession(Map<String, String> sessionParameters, ObjectFactory objectFactory,
                         AuthenticationProvider authenticationProvider, Cache cache) {
        if (sessionParameters == null) {
            throw new IllegalArgumentException("Parameters must not be null!");
        }

        connect(sessionParameters, objectFactory, authenticationProvider, cache);
    }

    public static Map<String, String> createSessionParameters(String url, BindingType binding, String username,
                                                              String password, Authentication authentication, boolean compression, boolean clientCompression,
                                                              boolean cookies) {
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put(BasicLoginTab.SYSPROP_AUTHENTICATION, authentication.toString().toLowerCase());
        parameters.put(SessionParameter.BROWSER_URL, url);

        switch (binding) {
            case WEBSERVICES:
                parameters.put(SessionParameter.BINDING_TYPE, BindingType.WEBSERVICES.value());
                parameters.put(SessionParameter.WEBSERVICES_REPOSITORY_SERVICE, url);
                parameters.put(SessionParameter.WEBSERVICES_NAVIGATION_SERVICE, url);
                parameters.put(SessionParameter.WEBSERVICES_OBJECT_SERVICE, url);
                parameters.put(SessionParameter.WEBSERVICES_VERSIONING_SERVICE, url);
                parameters.put(SessionParameter.WEBSERVICES_DISCOVERY_SERVICE, url);
                parameters.put(SessionParameter.WEBSERVICES_MULTIFILING_SERVICE, url);
                parameters.put(SessionParameter.WEBSERVICES_RELATIONSHIP_SERVICE, url);
                parameters.put(SessionParameter.WEBSERVICES_ACL_SERVICE, url);
                parameters.put(SessionParameter.WEBSERVICES_POLICY_SERVICE, url);
                break;
            case ATOMPUB:
                parameters.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
                parameters.put(SessionParameter.ATOMPUB_URL, url);
                break;
            case BROWSER:
                parameters.put(SessionParameter.BINDING_TYPE, BindingType.BROWSER.value());
                parameters.put(SessionParameter.BROWSER_URL, url);
                break;
            default:
                parameters.put(SessionParameter.BINDING_TYPE, BindingType.CUSTOM.value());
        }

        switch (authentication) {
            case STANDARD:
                parameters.put(SessionParameter.USER, username);
                parameters.put(SessionParameter.PASSWORD, password);
                break;
            case NTLM:
                parameters.put(SessionParameter.USER, username);
                parameters.put(SessionParameter.PASSWORD, password);
                parameters.put(SessionParameter.AUTHENTICATION_PROVIDER_CLASS,
                        CmisBindingFactory.NTLM_AUTHENTICATION_PROVIDER);
                break;
            case SSO:
                parameters.put(SessionParameter.USER, username);
                parameters.put(SessionParameter.PASSWORD, password);
                parameters.put(SessionParameter.AUTHENTICATION_PROVIDER_CLASS,
                        CmisBindingFactory.SSO_AUTHENTICATION_PROVIDER);
                parameters.put(SessionParameter.AUTH_SOAP_USERNAMETOKEN,
                        Boolean.FALSE.toString());
                parameters.put(SessionParameter.AUTH_SSO_AUTH_SOAP_SAMLTOKEN,
                        Boolean.TRUE.toString());
                parameters.put(SessionParameter.AUTH_SSO_HOK_SIGNATURE,
                        Boolean.FALSE.toString());
                setCatcherParameters(url, parameters);
                parameters.put(SessionParameter.SECURITY_CRYPTO_FILE, DEFAULT_KEYSTORE_FILE);
                parameters.put(SessionParameter.SECURITY_CRYPTO_KEYSTORE_TYPE, DEAULT_KEYSTORE_TYPE);
                parameters.put(SessionParameter.SECURITY_CRYPTO_KEYSTORE_ALIAS, "");
                parameters.put(SessionParameter.SECURITY_CRYPTO_KEYSTORE_PASSWORD, "");
                parameters.put(SessionParameter.SECURITY_CRYPTO_KEYSTORE_PRIVATE_PASSWORD, "");
            default:
        }

        parameters.put(SessionParameter.COMPRESSION, String.valueOf(compression));
        parameters.put(SessionParameter.CLIENT_COMPRESSION, String.valueOf(clientCompression));
        parameters.put(SessionParameter.COOKIES, String.valueOf(cookies));


        // get additional workbench properties from system properties
        Properties sysProps = System.getProperties();
        for (String key : sysProps.stringPropertyNames()) {
            if (key.startsWith(WORKBENCH_PREFIX)) {
                parameters.put(key, sysProps.getProperty(key));
            }
        }

        return parameters;
    }

    private static void setCatcherParameters(String url, Map<String, String> parameters) {
        String catcherHost = extractCatcherHost(url);
        String tokenIssuerUrl = getCatcherLocation(catcherHost, DEFAULT_TOKEN_ISSUER_PATH);
        String catcherUrl = getCatcherLocation(catcherHost, DEFAULT_CATCHER_PATH) ;
        parameters.put(SessionParameter.AUTH_SSO_TOKEN_ISSUER_URL, tokenIssuerUrl);
        parameters.put(SessionParameter.AUTH_SSO_CATCHER_URL, catcherUrl);
    }

    private static String getCatcherLocation(String catcherHost, String path) {
        return catcherHost == null ? "" : HTTPS + catcherHost + ":"
                            + DEFAULT_CATCHER_PORT + path;
    }

    private static String extractCatcherHost(String url)  {
        try {
            return new URI(url).getHost();
        } catch (URISyntaxException e) {
            return null;
        }

    }

    private void connect(Map<String, String> sessionParameters, ObjectFactory objectFactory,
                         AuthenticationProvider authenticationProvider, Cache cache) {
        this.sessionParameters = sessionParameters;

        // set a new dummy authenticator
        // don't send previous credentials to another server
        Authenticator.setDefault(new Authenticator() {
        });

        if (Boolean.parseBoolean(sessionParameters.get(ACCEPT_SELF_SIGNED_CERTIFICATES))) {
            acceptSelfSignedCertificates();
        }

        repositories = SessionFactoryImpl.newInstance().getRepositories(sessionParameters, objectFactory,
                authenticationProvider, cache);
    }

    public List<Repository> getRepositories() {
        return repositories;
    }

    public Session createSession(int index) {
        session = repositories.get(index).createSession();
        objectOperationContext = null;
        folderOperationContext = null;
        versionOperationContext = null;
        return getSession();
    }

    public Session getSession() {
        return session;
    }

    public Map<String, String> getSessionParameters() {
        return Collections.unmodifiableMap(sessionParameters);
    }

    public synchronized OperationContext getObjectOperationContext() {
        if (objectOperationContext == null) {
            createOperationContexts();
        }
        return objectOperationContext;
    }

    public synchronized OperationContext getFolderOperationContext() {
        if (folderOperationContext == null) {
            createOperationContexts();
        }
        return folderOperationContext;
    }

    public synchronized OperationContext getVersionOperationContext() {
        if (versionOperationContext == null) {
            createOperationContexts();
        }
        return versionOperationContext;
    }

    private void createOperationContexts() {

        RepositoryInfo repositoryInfo = getSession().getRepositoryInfo();

        String supportsAcl = "true";
        if (repositoryInfo != null && repositoryInfo.getCapabilities() != null
                && repositoryInfo.getCapabilities().getAclCapability() == CapabilityAcl.NONE) {
            supportsAcl = "false";
        }

        // object operation context
        setDefault(OBJECT_PREFIX, sessionParameters, ClientOperationContext.FILTER, "*");
        setDefault(OBJECT_PREFIX, sessionParameters, ClientOperationContext.INCLUDE_ACLS, supportsAcl);
        setDefault(OBJECT_PREFIX, sessionParameters, ClientOperationContext.INCLUDE_ALLOWABLE_ACTIONS, "true");
        setDefault(OBJECT_PREFIX, sessionParameters, ClientOperationContext.INCLUDE_POLICIES, "true");
        setDefault(OBJECT_PREFIX, sessionParameters, ClientOperationContext.INCLUDE_RELATIONSHIPS,
                IncludeRelationships.BOTH.value());
        setDefault(OBJECT_PREFIX, sessionParameters, ClientOperationContext.RENDITION_FILTER, "*");
        setDefault(OBJECT_PREFIX, sessionParameters, ClientOperationContext.ORDER_BY, null);
        setDefault(OBJECT_PREFIX, sessionParameters, ClientOperationContext.MAX_ITEMS_PER_PAGE, "1000");

        objectOperationContext = new ClientOperationContext(OBJECT_PREFIX, sessionParameters);

        // folder operation context
        if (!sessionParameters.containsKey(FOLDER_PREFIX + ClientOperationContext.FILTER)) {
            ObjectType type = session.getTypeDefinition(BaseTypeId.CMIS_DOCUMENT.value());

            StringBuilder filter = new StringBuilder();
            for (String propId : FOLDER_PROPERTY_SET) {
                PropertyDefinition<?> propDef = type.getPropertyDefinitions().get(propId);
                if (propDef != null) {
                    if (filter.length() > 0) {
                        filter.append(",");
                    }
                    filter.append(propDef.getQueryName());
                }
            }

            sessionParameters.put(FOLDER_PREFIX + ClientOperationContext.FILTER, filter.toString());
        }

        setDefault(FOLDER_PREFIX, sessionParameters, ClientOperationContext.INCLUDE_ACLS, "false");
        setDefault(FOLDER_PREFIX, sessionParameters, ClientOperationContext.INCLUDE_ALLOWABLE_ACTIONS, "false");
        setDefault(FOLDER_PREFIX, sessionParameters, ClientOperationContext.INCLUDE_POLICIES, "false");
        setDefault(FOLDER_PREFIX, sessionParameters, ClientOperationContext.INCLUDE_RELATIONSHIPS,
                IncludeRelationships.NONE.value());
        setDefault(FOLDER_PREFIX, sessionParameters, ClientOperationContext.RENDITION_FILTER, "cmis:none");
        setDefault(FOLDER_PREFIX, sessionParameters, ClientOperationContext.ORDER_BY, null);
        setDefault(FOLDER_PREFIX, sessionParameters, ClientOperationContext.MAX_ITEMS_PER_PAGE, "10000");

        folderOperationContext = new ClientOperationContext(FOLDER_PREFIX, sessionParameters);

        if (!sessionParameters.containsKey(VERSION_PREFIX + ClientOperationContext.FILTER)) {
            ObjectType type = session.getTypeDefinition(BaseTypeId.CMIS_DOCUMENT.value());

            StringBuilder filter = new StringBuilder();
            for (String propId : VERSION_PROPERTY_SET) {
                PropertyDefinition<?> propDef = type.getPropertyDefinitions().get(propId);
                if (propDef != null) {
                    if (filter.length() > 0) {
                        filter.append(",");
                    }
                    filter.append(propDef.getQueryName());
                }
            }

            sessionParameters.put(VERSION_PREFIX + ClientOperationContext.FILTER, filter.toString());
        }

        setDefault(VERSION_PREFIX, sessionParameters, ClientOperationContext.INCLUDE_ACLS, "false");
        setDefault(VERSION_PREFIX, sessionParameters, ClientOperationContext.INCLUDE_ALLOWABLE_ACTIONS, "false");
        setDefault(VERSION_PREFIX, sessionParameters, ClientOperationContext.INCLUDE_POLICIES, "false");
        setDefault(VERSION_PREFIX, sessionParameters, ClientOperationContext.INCLUDE_RELATIONSHIPS,
                IncludeRelationships.NONE.value());
        setDefault(VERSION_PREFIX, sessionParameters, ClientOperationContext.RENDITION_FILTER, "cmis:none");
        setDefault(VERSION_PREFIX, sessionParameters, ClientOperationContext.MAX_ITEMS_PER_PAGE, "10000");

        versionOperationContext = new ClientOperationContext(VERSION_PREFIX, sessionParameters);
    }

    private void setDefault(String prefix, Map<String, String> map, String key, String value) {
        if (!map.containsKey(prefix + key)) {
            map.put(prefix + key, value);
        }
    }

    private void acceptSelfSignedCertificates() {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};

        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
        }
    }
}
