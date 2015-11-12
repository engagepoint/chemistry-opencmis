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
package org.apache.chemistry.opencmis.workbench;

import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.workbench.model.ClientSession;

import javax.swing.*;
import java.awt.*;
import java.util.Locale;
import java.util.Map;

public class BasicLoginTab extends AbstractSpringLoginTab {

    private static final long serialVersionUID = 1L;

    public static final String SYSPROP_URL = ClientSession.WORKBENCH_PREFIX + "url";
    public static final String SYSPROP_BINDING = ClientSession.WORKBENCH_PREFIX + "binding";
    public static final String SYSPROP_AUTHENTICATION = ClientSession.WORKBENCH_PREFIX + "authentication";
    public static final String SYSPROP_COMPRESSION = ClientSession.WORKBENCH_PREFIX + "compression";
    public static final String SYSPROP_CLIENTCOMPRESSION = ClientSession.WORKBENCH_PREFIX + "clientcompression";
    public static final String SYSPROP_COOKIES = ClientSession.WORKBENCH_PREFIX + "cookies";
    public static final String SYSPROP_USER = ClientSession.WORKBENCH_PREFIX + "user";
    public static final String SYSPROP_PASSWORD = ClientSession.WORKBENCH_PREFIX + "password";

    private JTextField urlField;
    private JRadioButton bindingAtomButton;
    private JRadioButton bindingWebServicesButton;
    private JRadioButton bindingBrowserButton;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JRadioButton authenticationNoneButton;
    private JRadioButton authenticationStandardButton;
    private JRadioButton authenticationSsoButton;
    private JRadioButton authenticationNTLMButton;
    private JRadioButton compressionOnButton;
    private JRadioButton compressionOffButton;
    private JRadioButton clientCompressionOnButton;
    private JRadioButton clientCompressionOffButton;
    private JRadioButton cookiesOnButton;
    private JRadioButton cookiesOffButton;
    private JRadioButton sslCheckOnButton;
    private JRadioButton sslCheckOffButton;

    public BasicLoginTab(Map<String, String> parameters) {
        super();
        createGUI(parameters);
    }

    private void createGUI(Map<String, String> parameters) {
        setLayout(new SpringLayout());

        urlField = createTextField(this, "URL:");
        String url = parameters.get(SessionParameter.BROWSER_URL);
        urlField.setText(url != null ? url : System.getProperty(SYSPROP_URL, ""));

        createBindingButtons(this, parameters);

        usernameField = createTextField(this, "Username:");
        usernameField.setText(System.getProperty(SYSPROP_USER, ""));

        passwordField = createPasswordField(this, "Password:");
        passwordField.setText(System.getProperty(SYSPROP_PASSWORD, ""));

        createAuthenticationButtons(this, parameters);

        createCompressionButtons(this, parameters);

        createClientCompressionButtons(this, parameters);

        createCookieButtons(this, parameters);

        createSslCheckButtons(this, parameters);

        makeCompactGrid(this, 9, 2, 5, 10, 5, 5);
    }

    protected void createBindingButtons(Container pane, Map<String, String> parameters) {
        JPanel bindingContainer = new JPanel();
        bindingContainer.setLayout(new BoxLayout(bindingContainer, BoxLayout.LINE_AXIS));
        String predefinedBinding = parameters.get(SessionParameter.BINDING_TYPE);
        char bc = predefinedBinding != null ? predefinedBinding.charAt(0) :
                System.getProperty(SYSPROP_BINDING, "atom").toLowerCase(Locale.ENGLISH).charAt(0);
        boolean atom = (bc == 'a');
        boolean ws = (bc == 'w');
        boolean browser = (bc == 'b');
        bindingAtomButton = new JRadioButton("AtomPub", atom);
        bindingWebServicesButton = new JRadioButton("Web Services", ws);
        bindingBrowserButton = new JRadioButton("Browser", browser);
        ButtonGroup bindingGroup = new ButtonGroup();
        bindingGroup.add(bindingAtomButton);
        bindingGroup.add(bindingWebServicesButton);
        bindingGroup.add(bindingBrowserButton);
        bindingContainer.add(bindingAtomButton);
        bindingContainer.add(Box.createRigidArea(new Dimension(10, 0)));
        bindingContainer.add(bindingWebServicesButton);
        bindingContainer.add(Box.createRigidArea(new Dimension(10, 0)));
        bindingContainer.add(bindingBrowserButton);
        JLabel bindingLabel = new JLabel("Binding:", JLabel.TRAILING);

        pane.add(bindingLabel);
        pane.add(bindingContainer);
    }

    protected void createAuthenticationButtons(Container pane, Map<String, String> parameters) {
        JPanel authenticationContainer = new JPanel();
        authenticationContainer.setLayout(new BoxLayout(authenticationContainer, BoxLayout.LINE_AXIS));
        String predefinedBinding = parameters.get(BasicLoginTab.SYSPROP_AUTHENTICATION);
        boolean standard = ("standard".equals(predefinedBinding != null ? predefinedBinding :
                System.getProperty(SYSPROP_AUTHENTICATION, "standard").toLowerCase(Locale.ENGLISH)));
        boolean ntlm = ("ntlm".equals(predefinedBinding != null ? predefinedBinding :
                System.getProperty(SYSPROP_AUTHENTICATION, "").toLowerCase(Locale.ENGLISH)));
        boolean sso = ("sso".equals(predefinedBinding != null ? predefinedBinding :
                System.getProperty(SYSPROP_AUTHENTICATION, "").toLowerCase(Locale.ENGLISH)));
        boolean none = !standard && !ntlm && !sso;
        authenticationNoneButton = new JRadioButton("None", none);
        authenticationStandardButton = new JRadioButton("Standard", standard);
        authenticationNTLMButton = new JRadioButton("NTLM", ntlm);
        authenticationSsoButton = new JRadioButton("SSO", sso);
        ButtonGroup authenticationGroup = new ButtonGroup();
        authenticationGroup.add(authenticationNoneButton);
        authenticationGroup.add(authenticationStandardButton);
        authenticationGroup.add(authenticationNTLMButton);
        authenticationGroup.add(authenticationSsoButton);
        authenticationContainer.add(authenticationNoneButton);
        authenticationContainer.add(Box.createRigidArea(new Dimension(10, 0)));
        authenticationContainer.add(authenticationStandardButton);
        authenticationContainer.add(Box.createRigidArea(new Dimension(10, 0)));
        authenticationContainer.add(authenticationNTLMButton);
        authenticationContainer.add(Box.createRigidArea(new Dimension(10, 0)));
        authenticationContainer.add(authenticationSsoButton);
        JLabel authenticatioLabel = new JLabel("Authentication:", JLabel.TRAILING);

        pane.add(authenticatioLabel);
        pane.add(authenticationContainer);
    }

    protected void createCompressionButtons(Container pane, Map<String, String> parameters) {
        JPanel compressionContainer = new JPanel();
        compressionContainer.setLayout(new BoxLayout(compressionContainer, BoxLayout.LINE_AXIS));
        String predefinedBindingParam = parameters.get(SessionParameter.COMPRESSION);
        String predefinedBinding = "true".equals(predefinedBindingParam) ? "on" : "off";
        boolean compression = !("off".equalsIgnoreCase(predefinedBindingParam != null ? predefinedBinding :
                System.getProperty(SYSPROP_COMPRESSION, "on")));
        compressionOnButton = new JRadioButton("On", compression);
        compressionOffButton = new JRadioButton("Off", !compression);
        ButtonGroup compressionGroup = new ButtonGroup();
        compressionGroup.add(compressionOnButton);
        compressionGroup.add(compressionOffButton);
        compressionContainer.add(compressionOnButton);
        compressionContainer.add(Box.createRigidArea(new Dimension(10, 0)));
        compressionContainer.add(compressionOffButton);
        JLabel compressionLabel = new JLabel("Compression:", JLabel.TRAILING);

        pane.add(compressionLabel);
        pane.add(compressionContainer);
    }

    protected void createClientCompressionButtons(Container pane, Map<String, String> parameters) {
        JPanel clientCompressionContainer = new JPanel();
        clientCompressionContainer.setLayout(new BoxLayout(clientCompressionContainer, BoxLayout.LINE_AXIS));
        String predefinedBindingParam = parameters.get(SessionParameter.CLIENT_COMPRESSION);
        String predefinedBinding = "true".equals(predefinedBindingParam) ? "on" : "off";
        boolean clientCompression = ("on".equalsIgnoreCase(predefinedBindingParam != null ? predefinedBinding :
                System.getProperty(SYSPROP_CLIENTCOMPRESSION, "off")));
        clientCompressionOnButton = new JRadioButton("On", clientCompression);
        clientCompressionOffButton = new JRadioButton("Off", !clientCompression);
        ButtonGroup clientCompressionGroup = new ButtonGroup();
        clientCompressionGroup.add(clientCompressionOnButton);
        clientCompressionGroup.add(clientCompressionOffButton);
        clientCompressionContainer.add(clientCompressionOnButton);
        clientCompressionContainer.add(Box.createRigidArea(new Dimension(10, 0)));
        clientCompressionContainer.add(clientCompressionOffButton);
        JLabel clientCompressionLabel = new JLabel("Client Compression:", JLabel.TRAILING);

        pane.add(clientCompressionLabel);
        pane.add(clientCompressionContainer);
    }

    protected void createCookieButtons(Container pane, Map<String, String> parameters) {
        JPanel cookiesContainer = new JPanel();
        cookiesContainer.setLayout(new BoxLayout(cookiesContainer, BoxLayout.LINE_AXIS));
        String predefinedCookieParam = parameters.get(SessionParameter.COOKIES);
        String predefinedCookie = "true".equals(predefinedCookieParam) ? "on" : "off";
        boolean cookies = "on".equalsIgnoreCase(predefinedCookieParam != null ? predefinedCookie :
                System.getProperty(SYSPROP_COOKIES, "on"));
        cookiesOnButton = new JRadioButton("On", cookies);
        cookiesOffButton = new JRadioButton("Off", !cookies);
        ButtonGroup cookiesGroup = new ButtonGroup();
        cookiesGroup.add(cookiesOnButton);
        cookiesGroup.add(cookiesOffButton);
        cookiesContainer.add(cookiesOnButton);
        cookiesContainer.add(Box.createRigidArea(new Dimension(10, 0)));
        cookiesContainer.add(cookiesOffButton);
        JLabel cookiesLabel = new JLabel("Cookies:", JLabel.TRAILING);

        pane.add(cookiesLabel);
        pane.add(cookiesContainer);
    }

    protected void createSslCheckButtons(Container pane, Map<String, String> parameters) {
        JPanel sslCheckContainer = new JPanel();
        sslCheckContainer.setLayout(new BoxLayout(sslCheckContainer, BoxLayout.LINE_AXIS));
        String predefinedAcceptSelfSignedCertParam = parameters.get(ClientSession.ACCEPT_SELF_SIGNED_CERTIFICATES);
        String predefinedAcceptSelfSignedCert = "true".equals(predefinedAcceptSelfSignedCertParam) ? "on" : "off";
        boolean sslCheck = !"on".equalsIgnoreCase(predefinedAcceptSelfSignedCertParam != null ? predefinedAcceptSelfSignedCert :
                System.getProperty(ClientSession.ACCEPT_SELF_SIGNED_CERTIFICATES, "on"));
        sslCheckOnButton = new JRadioButton("On", sslCheck);
        sslCheckOffButton = new JRadioButton("Off", !sslCheck);
        ButtonGroup sslCheckGroup = new ButtonGroup();
        sslCheckGroup.add(sslCheckOnButton);
        sslCheckGroup.add(sslCheckOffButton);
        sslCheckContainer.add(sslCheckOnButton);
        sslCheckContainer.add(Box.createRigidArea(new Dimension(10, 0)));
        sslCheckContainer.add(sslCheckOffButton);
        JLabel sslCheckLabel = new JLabel("SSL certificate check:", JLabel.TRAILING);

        pane.add(sslCheckLabel);
        pane.add(sslCheckContainer);
    }

    @Override
    public String getTabTitle() {
        return "Basic";
    }

    @Override
    public Map<String, String> getSessionParameters() {
        String url = urlField.getText();

        BindingType binding = BindingType.ATOMPUB;
        if (bindingWebServicesButton.isSelected()) {
            binding = BindingType.WEBSERVICES;
        } else if (bindingBrowserButton.isSelected()) {
            binding = BindingType.BROWSER;
        }

        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        ClientSession.Authentication authentication = getAuthentication();

        return ClientSession.createSessionParameters(url, binding, username, password, authentication,
                compressionOnButton.isSelected(), clientCompressionOnButton.isSelected(), cookiesOnButton.isSelected(),
                sslCheckOnButton.isSelected());
    }

    private ClientSession.Authentication getAuthentication() {
        ClientSession.Authentication authentication = ClientSession.Authentication.NONE;
        if (authenticationStandardButton.isSelected()) {
            authentication = ClientSession.Authentication.STANDARD;
        } else if (authenticationNTLMButton.isSelected()) {
            authentication = ClientSession.Authentication.NTLM;
        } else if (authenticationSsoButton.isSelected()) {
            authentication = ClientSession.Authentication.SSO;
        }
        return authentication;
    }

    @Override
    public boolean transferSessionParametersToExpertTab() {
        return true;
    }
}
