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

import java.awt.Container;
import java.awt.Dimension;
import java.util.Locale;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.workbench.model.ClientSession;

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
    private JRadioButton authenticationNTLMButton;
    private JRadioButton compressionOnButton;
    private JRadioButton compressionOffButton;
    private JRadioButton clientCompressionOnButton;
    private JRadioButton clientCompressionOffButton;
    private JRadioButton cookiesOnButton;
    private JRadioButton cookiesOffButton;

    public BasicLoginTab() {
        super();
        createGUI();
    }

    private void createGUI() {
        setLayout(new SpringLayout());

        urlField = createTextField(this, "URL:");
        urlField.setText(System.getProperty(SYSPROP_URL, ""));

        createBindingButtons(this);

        usernameField = createTextField(this, "Username:");
        usernameField.setText(System.getProperty(SYSPROP_USER, ""));

        passwordField = createPasswordField(this, "Password:");
        passwordField.setText(System.getProperty(SYSPROP_PASSWORD, ""));

        createAuthenticationButtons(this);

        createCompressionButtons(this);

        createClientCompressionButtons(this);

        createCookieButtons(this);

        makeCompactGrid(this, 8, 2, 5, 10, 5, 5);
    }

    protected void createBindingButtons(Container pane) {
        JPanel bindingContainer = new JPanel();
        bindingContainer.setLayout(new BoxLayout(bindingContainer, BoxLayout.LINE_AXIS));
        char bc = System.getProperty(SYSPROP_BINDING, "atom").toLowerCase(Locale.ENGLISH).charAt(0);
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

    protected void createAuthenticationButtons(Container pane) {
        JPanel authenticationContainer = new JPanel();
        authenticationContainer.setLayout(new BoxLayout(authenticationContainer, BoxLayout.LINE_AXIS));
        boolean standard = (System.getProperty(SYSPROP_AUTHENTICATION, "standard").toLowerCase(Locale.ENGLISH)
                .equals("standard"));
        boolean ntlm = (System.getProperty(SYSPROP_AUTHENTICATION, "").toLowerCase(Locale.ENGLISH).equals("ntlm"));
        boolean none = !standard && !ntlm;
        authenticationNoneButton = new JRadioButton("None", none);
        authenticationStandardButton = new JRadioButton("Standard", standard);
        authenticationNTLMButton = new JRadioButton("NTLM", ntlm);
        ButtonGroup authenticationGroup = new ButtonGroup();
        authenticationGroup.add(authenticationNoneButton);
        authenticationGroup.add(authenticationStandardButton);
        authenticationGroup.add(authenticationNTLMButton);
        authenticationContainer.add(authenticationNoneButton);
        authenticationContainer.add(Box.createRigidArea(new Dimension(10, 0)));
        authenticationContainer.add(authenticationStandardButton);
        authenticationContainer.add(Box.createRigidArea(new Dimension(10, 0)));
        authenticationContainer.add(authenticationNTLMButton);
        JLabel authenticatioLabel = new JLabel("Authentication:", JLabel.TRAILING);

        pane.add(authenticatioLabel);
        pane.add(authenticationContainer);
    }

    protected void createCompressionButtons(Container pane) {
        JPanel compressionContainer = new JPanel();
        compressionContainer.setLayout(new BoxLayout(compressionContainer, BoxLayout.LINE_AXIS));
        boolean compression = !(System.getProperty(SYSPROP_COMPRESSION, "on").equalsIgnoreCase("off"));
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

    protected void createClientCompressionButtons(Container pane) {
        JPanel clientCompressionContainer = new JPanel();
        clientCompressionContainer.setLayout(new BoxLayout(clientCompressionContainer, BoxLayout.LINE_AXIS));
        boolean clientCompression = (System.getProperty(SYSPROP_CLIENTCOMPRESSION, "off").equalsIgnoreCase("on"));
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

    protected void createCookieButtons(Container pane) {
        JPanel cookiesContainer = new JPanel();
        cookiesContainer.setLayout(new BoxLayout(cookiesContainer, BoxLayout.LINE_AXIS));
        boolean cookies = (System.getProperty(SYSPROP_COOKIES, "on").equalsIgnoreCase("on"));
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

        ClientSession.Authentication authentication = ClientSession.Authentication.NONE;
        if (authenticationStandardButton.isSelected()) {
            authentication = ClientSession.Authentication.STANDARD;
        } else if (authenticationNTLMButton.isSelected()) {
            authentication = ClientSession.Authentication.NTLM;
        }

        return ClientSession.createSessionParameters(url, binding, username, password, authentication,
                compressionOnButton.isSelected(), clientCompressionOnButton.isSelected(), cookiesOnButton.isSelected());
    }

    @Override
    public boolean transferSessionParametersToExpertTab() {
        return true;
    }
}
