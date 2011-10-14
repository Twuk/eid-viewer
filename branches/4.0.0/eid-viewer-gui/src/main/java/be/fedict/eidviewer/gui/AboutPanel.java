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

import java.util.ResourceBundle;
import javax.swing.JDialog;
import org.jdesktop.application.Action;

/**
 *
 * @author Frank Marien
 */
public class AboutPanel extends javax.swing.JPanel
{
    private JDialog dialog;
    
    /** Creates new form AboutPanel */
    public AboutPanel()
    {
        initComponents();
        String aboutHTML=ResourceBundle.getBundle("be/fedict/eidviewer/gui/resources/AboutPanel").getString("about_html");
        aboutCopyrightText.setText(aboutHTML.replace("__FULLVERSION__", ViewerPrefs.getFullVersion()));
    }

    private void initComponents()
	{
        java.awt.GridBagConstraints gridBagConstraints;

        jPanel3 = new javax.swing.JPanel();
        okButton = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        aboutCopyrightText = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();

        setLayout(new java.awt.BorderLayout());

        jPanel3.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16));
        jPanel3.setName("jPanel3"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance().getContext().getActionMap(AboutPanel.class, this);
        okButton.setAction(actionMap.get("close")); // NOI18N
        okButton.setText("OK");
        okButton.setName("okButton"); // NOI18N
        jPanel3.add(okButton);

        add(jPanel3, java.awt.BorderLayout.PAGE_END);

        jPanel2.setBorder(javax.swing.BorderFactory.createEmptyBorder(24, 24, 24, 24));
        jPanel2.setName("jPanel2"); // NOI18N
        jPanel2.setLayout(new java.awt.GridBagLayout());

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/be/fedict/eidviewer/gui/resources/icons/state_eidpresent.png"))); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel2.add(jLabel2, gridBagConstraints);

        aboutCopyrightText.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        aboutCopyrightText.setText("Copyright (C) 2010 - 2011 Fedict");
        aboutCopyrightText.setName("aboutCopyrightText"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 8;
        gridBagConstraints.ipady = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel2.add(aboutCopyrightText, gridBagConstraints);

        jLabel4.setMaximumSize(new java.awt.Dimension(16, 16));
        jLabel4.setMinimumSize(new java.awt.Dimension(16, 16));
        jLabel4.setName("jLabel4"); // NOI18N
        jLabel4.setPreferredSize(new java.awt.Dimension(16, 16));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        jPanel2.add(jLabel4, gridBagConstraints);

        add(jPanel2, java.awt.BorderLayout.CENTER);
    }

    @Action
    public void close()
    {
        if(dialog!=null)
            dialog.dispose();
    }
    
    public AboutPanel setDialog(JDialog frame)
    {
        this.dialog=frame;
        return this;
    }


    private javax.swing.JLabel aboutCopyrightText;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JButton okButton;

}
