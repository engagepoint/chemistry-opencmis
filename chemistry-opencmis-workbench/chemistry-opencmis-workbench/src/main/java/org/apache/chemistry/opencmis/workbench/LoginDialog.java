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

import org.apache.chemistry.opencmis.client.api.Repository;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.workbench.model.ClientSession;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class LoginDialog extends JDialog {

    private static final String PREDEFINED_PROPERTIES_FILE = "predefined.properties";
    private static final String WORKBENCH_USER_HOME_FOLDER = ".workbench";

    private static final long serialVersionUID = 1L;

    public static final String SYSPROP_LOGIN_TAB = ClientSession.WORKBENCH_PREFIX + "logintab";

    private static ServiceLoader<AbstractLoginTab> TAB_SERVICE_LOADER = ServiceLoader.load(AbstractLoginTab.class);


    private JTabbedPane loginTabs;
    private BasicLoginTab basicLoginTab;
    private ExpertLoginTab expertLoginTab;
    private JButton loadRepositoryButton;
    private JButton loginButton;
    private JComboBox repositoryBox;
    private AbstractLoginTab currentTab;

    private boolean canceled = true;

    private PredefinedParameters predefinedParameters;
    private ClientSession clientSession;

    public LoginDialog(Frame owner) {
        super(owner, "Login", true);
        loadLastPropertiesSnapshot();
        createGUI();
    }

    private void createGUI() {
        setMinimumSize(new Dimension(700, 500));
        setPreferredSize(new Dimension(700, 500));

        Container pane = getContentPane();
        pane.setLayout(new BorderLayout());

        loginTabs = new JTabbedPane();
        add(loginTabs, BorderLayout.CENTER);

        // add tabs
        addLoginTabs(loginTabs);

        // repository
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.PAGE_AXIS));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
        add(buttonPanel, BorderLayout.PAGE_END);

        loadRepositoryButton = createButton("Load Repositories");
        buttonPanel.add(loadRepositoryButton);
        getRootPane().setDefaultButton(loadRepositoryButton);

        createRepositoryBox(buttonPanel);

        loginButton = createButton("Login");
        buttonPanel.add(loginButton);
        loginButton.setEnabled(false);

        // listeners
        loadRepositoryButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                repositoryBox.removeAllItems();

                try {
                    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                    currentTab.beforeLoadRepositories();

                    createClientSession();

                    List<Repository> repositories = clientSession.getRepositories();

                    Collections.sort(repositories, new Comparator<Repository>() {
                        @Override
                        public int compare(Repository r1, Repository r2) {
                            if (r1 == null || r1.getName() == null) {
                                return 1;
                            }

                            if (r2 == null || r2.getName() == null) {
                                return -1;
                            }

                            return r1.getName().compareTo(r2.getName());
                        }
                    });

                    if (repositories.size() > 0) {

                        for (Repository repository : repositories) {
                            repositoryBox.addItem(repository);
                        }

                        repositoryBox.setEnabled(true);
                        loginButton.setEnabled(true);
                        getRootPane().setDefaultButton(loginButton);
                    } else {
                        repositoryBox.setEnabled(false);
                        loginButton.setEnabled(false);
                        getRootPane().setDefaultButton(loadRepositoryButton);
                    }

                    currentTab.afterLoadRepositories(repositories);
                } catch (Exception ex) {
                    repositoryBox.setEnabled(false);
                    loginButton.setEnabled(false);
                    getRootPane().setDefaultButton(loadRepositoryButton);

                    ClientHelper.showError(getOwner(), ex);
                } finally {
                    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            }
        });

        loginButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                    currentTab.beforeLogin(clientSession.getRepositories().get(repositoryBox.getSelectedIndex()));

                    clientSession.createSession(repositoryBox.getSelectedIndex());

                    currentTab.afterLogin(clientSession.getSession());

                    canceled = false;
                    hideDialog();

                    storeLastPropertiesSnapshot();
                } catch (Exception ex) {
                    repositoryBox.setEnabled(false);
                    loginButton.setEnabled(false);
                    getRootPane().setDefaultButton(loadRepositoryButton);

                    ClientHelper.showError(getOwner(), ex);

                } finally {
                    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    getRootPane().setDefaultButton(loadRepositoryButton);
                }
            }
        });

        ClientHelper.installEscapeBinding(this, getRootPane(), false);

        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }


    protected void addLoginTabs(final JTabbedPane loginTabs) {

        for (AbstractLoginTab tab : TAB_SERVICE_LOADER) {
            loginTabs.add(tab.getTabTitle(), tab);
        }

        Map<String, String> predefinedSessionParams = getPredefinedSessionParams();

        basicLoginTab = new BasicLoginTab(predefinedSessionParams);
        loginTabs.addTab(basicLoginTab.getTabTitle(), basicLoginTab);

        expertLoginTab = new ExpertLoginTab();
        loginTabs.addTab(expertLoginTab.getTabTitle(), expertLoginTab);

        setLoginTabsSelectIndexProperties(loginTabs);

        setLoginTabsSelectIndexFromSysProperties(loginTabs);

        expertLoginTab.setSessionParameters(predefinedSessionParams);

        currentTab = (AbstractLoginTab) loginTabs.getSelectedComponent();

        loginTabs.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (loginTabs.getSelectedComponent() == expertLoginTab) {
                    if (currentTab.transferSessionParametersToExpertTab()) {
                        Map<String, String> sessionParameters = currentTab.getSessionParameters();
                        expertLoginTab.setSessionParameters(sessionParameters);
                    }
                }

                currentTab = (AbstractLoginTab) loginTabs.getSelectedComponent();
            }
        });
    }

    private Map<String, String> getPredefinedSessionParams() {
        Map<String, String> predefinedSessionParams = new LinkedHashMap<String, String>();
        for (Map.Entry<String, String> entry : predefinedParameters.propertiesSet()) {
            if (getLoadFilterExpression(entry.getKey())) {
                predefinedSessionParams.put(entry.getKey(), entry.getValue());
            }
        }
        return predefinedSessionParams;
    }


    private void setLoginTabsSelectIndexProperties(JTabbedPane loginTabs) {
        String selectedIndexParam = predefinedParameters.get(SYSPROP_LOGIN_TAB);
        int selectedIndex = selectedIndexParam != null ? Integer.parseInt(selectedIndexParam) : 0;
        loginTabs.setSelectedIndex(selectedIndex);
    }

    private void setLoginTabsSelectIndexFromSysProperties(JTabbedPane loginTabs) {
        String startTab = System.getProperty(SYSPROP_LOGIN_TAB);
        if (startTab != null) {
            try {
                int tab = Integer.parseInt(startTab);
                if (tab >= 0 && tab < loginTabs.getTabCount()) {
                    loginTabs.setSelectedIndex(tab);
                }
            } catch (NumberFormatException nfe) {
                // do nothing
            }
        }
    }

    protected JTextField createTextField(Container pane, String label) {
        JTextField textField = new JTextField(60);
        JLabel textLabel = new JLabel(label, JLabel.TRAILING);
        textLabel.setLabelFor(textField);

        pane.add(textLabel);
        pane.add(textField);

        return textField;
    }

    protected JPasswordField createPasswordField(Container pane, String label) {
        JPasswordField textField = new JPasswordField(60);
        JLabel textLabel = new JLabel(label, JLabel.TRAILING);
        textLabel.setLabelFor(textField);

        pane.add(textLabel);
        pane.add(textField);

        return textField;
    }

    protected JButton createButton(String title) {
        JButton button = new JButton(title);
        button.setPreferredSize(new Dimension(Short.MAX_VALUE, 30));
        button.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);

        return button;
    }

    protected void createRepositoryBox(Container pane) {
        repositoryBox = new JComboBox();
        repositoryBox.setEnabled(false);
        repositoryBox.setRenderer(new RepositoryRenderer());
        repositoryBox.setPreferredSize(new Dimension(Short.MAX_VALUE, 60));
        repositoryBox.setAlignmentX(Component.CENTER_ALIGNMENT);

        pane.add(repositoryBox);
    }

    protected void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    public void createClientSession() {
        setClientSession(new ClientSession(currentTab.getSessionParameters(), currentTab.getObjectFactory(),
                currentTab.getAuthenticationProvider(), currentTab.getCache()));
    }

    public void showDialog() {
        clientSession = null;
        canceled = true;

        repositoryBox.removeAllItems();
        repositoryBox.setEnabled(false);
        loginButton.setEnabled(false);
        getRootPane().setDefaultButton(loadRepositoryButton);

        setLocationRelativeTo(getOwner());
        setVisible(true);
    }

    public void hideDialog() {
        setVisible(false);
    }

    public ClientSession getClientSession() {
        return clientSession;
    }

    public boolean isCanceled() {
        return canceled;
    }

    static class RepositoryRenderer extends JPanel implements ListCellRenderer {
        private static final long serialVersionUID = 1L;

        private final JLabel nameLabel;
        private final JLabel idLabel;
        private final JLabel descriptionLabel;

        public RepositoryRenderer() {
            super();
            setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
            setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

            Font labelFont = UIManager.getFont("Label.font");

            nameLabel = new JLabel();
            nameLabel.setFont(labelFont.deriveFont(Font.BOLD));
            add(nameLabel);

            idLabel = new JLabel();
            add(idLabel);

            descriptionLabel = new JLabel();
            add(descriptionLabel);
        }

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            Repository repository = (Repository) value;

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            if (repository == null) {
                nameLabel.setText("");
                idLabel.setText("");
                descriptionLabel.setText("");
            } else {
                nameLabel.setText(repository.getName());
                idLabel.setText(repository.getId());
                descriptionLabel.setText(repository.getDescription());
            }

            return this;
        }
    }

    private void loadLastPropertiesSnapshot() {
        try {
            predefinedParameters = new PredefinedParameters();
            predefinedParameters.load(getPredefinedParametersFilePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void storeLastPropertiesSnapshot() {
        boolean created = true;
        File file = new File(getPredefinedParametersFilePath());
        if (!file.exists()) {
            created = file.getParentFile().mkdirs();
        }
        if (created) {
            PredefinedParameters predefineParameters = buildPredefineParameters(currentTab.getSessionParameters());
            try {
                predefineParameters.store(getPredefinedParametersFilePath());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private PredefinedParameters buildPredefineParameters(Map<String, String> sessionParameters) {
        PredefinedParameters predefineParams = new PredefinedParameters();
        for (Map.Entry<String, String> entry : sessionParameters.entrySet()) {
            String value = getStoredFilterExpression(entry) ? entry.getValue() : "";
            predefineParams.put(entry.getKey(), value);
        }
        predefineParams.put(SYSPROP_LOGIN_TAB, String.valueOf(loginTabs.getSelectedIndex()));
        return predefineParams;
    }

    private String getPredefinedParametersFilePath() {
        String separator = File.separator;
        return System.getProperty("user.home")
                + separator + WORKBENCH_USER_HOME_FOLDER
                + separator + PREDEFINED_PROPERTIES_FILE;
    }

    private boolean getStoredFilterExpression(Map.Entry<String, String> entry) {
        return !entry.getKey().equals(SessionParameter.USER) &&
                !entry.getKey().equals(SessionParameter.PASSWORD);
    }

    private boolean getLoadFilterExpression(String name) {
        return !name.contains(ClientSession.WORKBENCH_PREFIX);
    }

}
