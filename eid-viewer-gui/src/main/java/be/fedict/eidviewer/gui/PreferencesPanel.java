/*
 * eID Middleware Project.
 * Copyright (C) 2010-2011 FedICT.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version
 * 3.0 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, see
 * http://www.gnu.org/licenses/.
 */

package be.fedict.eidviewer.gui;

import be.fedict.eidviewer.gui.helper.PositiveIntegerDocument;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 *
 * @author Frank Marien
 */
public class PreferencesPanel extends JPanel
{
    private ResourceBundle bundle;
    
    private JTextField  httpProxyHost;
    private JTextField  httpProxyPort;
    private JLabel      httpProxyPortLabel;
    private JPanel      multiplePrefsSectionsPanel;
    private JPanel      proxyPrefsPanel;
    private JLabel      spacer;
    private JCheckBox   useProxyCheckbox;

    public PreferencesPanel()
    {
        bundle = ResourceBundle.getBundle("be/fedict/eidviewer/gui/resources/PreferencesPanel");
        initComponents();
        initProxyPrefsPanel();
        fillProxyPrefs();
    }

    private void fillProxyPrefs()
    {
        boolean proxyEnabled = ViewerPrefs.getUseHTTPProxy();

        useProxyCheckbox.setSelected(proxyEnabled);
        httpProxyHost.setText(ViewerPrefs.getHTTPProxyHost());
        httpProxyPort.setText(String.valueOf(ViewerPrefs.getHTTPProxyPort()));
        updateProxyComponentsEnabled();
    }

    public String getHttpProxyHost()
    {
        return httpProxyHost.getText();
    }

    public int getHttpProxyPort()
    {
        return Integer.parseInt(httpProxyPort.getText());
    }

    public boolean getUseProxy()
    {
        return useProxyCheckbox.isSelected();
    }
   
    private void initProxyPrefsPanel()
    {  
        useProxyCheckbox.addActionListener(new ActionListener()
        {

            public void actionPerformed(ActionEvent ae)
            {
                updateProxyComponentsEnabled();
            }
        });

        httpProxyPort.setDocument(new PositiveIntegerDocument(5));
    }

    private void updateProxyComponentsEnabled()
    {
        boolean enabled = useProxyCheckbox.isSelected();
        httpProxyHost.setEnabled(enabled);
        httpProxyPortLabel.setEnabled(enabled);
        httpProxyPort.setEnabled(enabled);
    }

    private void initComponents()
	{
        java.awt.GridBagConstraints gridBagConstraints;

        multiplePrefsSectionsPanel = new JPanel();
        proxyPrefsPanel = new JPanel();
        httpProxyPortLabel = new JLabel();
        useProxyCheckbox = new JCheckBox();
        httpProxyHost = new JTextField();
        spacer = new JLabel();
        httpProxyPort = new JTextField();
       
        setLayout(new java.awt.BorderLayout());

        // increment first argument and add panels to add preferences sections
        multiplePrefsSectionsPanel.setLayout(new java.awt.GridLayout(1, 1));

        proxyPrefsPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        proxyPrefsPanel.setLayout(new java.awt.GridBagLayout());

        httpProxyPortLabel.setText(bundle.getString("portLabel")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 4;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        proxyPrefsPanel.add(httpProxyPortLabel, gridBagConstraints);

        useProxyCheckbox.setText(bundle.getString("proxyCheckbox")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 4;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        proxyPrefsPanel.add(useProxyCheckbox, gridBagConstraints);

        httpProxyHost.setMinimumSize(new java.awt.Dimension(128, 18));
        httpProxyHost.setPreferredSize(new java.awt.Dimension(256, 18));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 4;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        proxyPrefsPanel.add(httpProxyHost, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 8);
        proxyPrefsPanel.add(spacer, gridBagConstraints);

        httpProxyPort.setText("8080");
        httpProxyPort.setMinimumSize(new java.awt.Dimension(48, 18));
        httpProxyPort.setPreferredSize(new java.awt.Dimension(48, 18));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        proxyPrefsPanel.add(httpProxyPort, gridBagConstraints);

        multiplePrefsSectionsPanel.add(proxyPrefsPanel);

        add(multiplePrefsSectionsPanel, java.awt.BorderLayout.CENTER);
    }
}
