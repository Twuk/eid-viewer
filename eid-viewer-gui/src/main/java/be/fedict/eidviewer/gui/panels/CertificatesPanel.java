/*
 * eID Middleware Project.
 * Copyright (C) 2010-2013 FedICT.
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
package be.fedict.eidviewer.gui.panels;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import be.fedict.eidviewer.gui.DynamicLocale;
import be.fedict.eidviewer.gui.DynamicLocaleAbstractAction;
import be.fedict.eidviewer.gui.ViewerPrefs;
import be.fedict.eidviewer.gui.helper.ImageUtilities;
import be.fedict.eidviewer.lib.PCSCEidController;
import be.fedict.eidviewer.lib.X509CertificateAndTrust;
import be.fedict.eidviewer.lib.X509CertificateChainAndTrust;
import be.fedict.eidviewer.lib.X509Utilities;
import be.fedict.eidviewer.lib.file.gui.CertFileFilter;
import be.fedict.eidviewer.lib.file.helper.TextFormatHelper;

/**
 * 
 * @author Frank Marien
 */
public class CertificatesPanel extends JPanel implements Observer,
	TreeSelectionListener, DynamicLocale, ActionListener {
    private static final long serialVersionUID = 3873882498274610172L;
    private static final Logger logger = Logger
	    .getLogger(CertificatesPanel.class.getName());
    private static final String ICONS = "/be/fedict/eidviewer/gui/resources/icons/";
    private ResourceBundle bundle;

    private JCheckBox alwaysValidateCheckbox;
    private JPanel authCertsPanel, certDetailsPanel, trustPrefspanel;
    private JLabel certificateIcon, certsBusyIcon;
    private JSplitPane certsDetailsSplitPane;
    private JTree certsTree;
    private JLabel dn, keyUsage, keyUsageLabel;
    private JSeparator keyUsageTrustSeparator;
    private JLabel spacer, trustErrors;
    private JSeparator trustServiceTrustErrorsSeparator;
    private JLabel trustStatus, trustStatusLabel, validFrom, validFromLabel,
	    validUntil, validUntilLabel;
    private JButton validateNowButton;
    private JSeparator validdUntilKeyUsageSeparator;

    private ExportPEMAction exportPEMAction;
    private ExportDERAction exportDERAction;
    private ExportChainPEMAction exportChainPEMAction;
    private CertificateDetailAction certificateDetailAction;
    private X509CertificateAndTrust exportingCertificate;

    private DateFormat dateFormat;
    private Map<Principal, DefaultMutableTreeNode> certificatesInTree;
    private DefaultMutableTreeNode rootNode;
    private DefaultTreeModel treeModel;
    private Color defaultLabelForeground, defaultLabelBackground;
    private PCSCEidController eidController;

    public CertificatesPanel() {
	bundle = ResourceBundle
		.getBundle("be/fedict/eidviewer/gui/resources/CertificatesPanel");
	dateFormat = DateFormat.getDateInstance(DateFormat.LONG,
		Locale.getDefault());
	initActions();
	initComponents();
	initI18N();
	trustErrors.setVisible(false);
	defaultLabelForeground = UIManager.getColor("Label.foreground");
	defaultLabelBackground = UIManager.getColor("Label.background");
	initCertsTree();
    }

    private void initActions() {
	exportPEMAction = new ExportPEMAction("Export to PEM..");
	exportDERAction = new ExportDERAction("Export to DER..");
	exportChainPEMAction = new ExportChainPEMAction("Export Chain to PEM..");
	certificateDetailAction = new CertificateDetailAction(
		"Show All Information..");
    }

    public CertificatesPanel setEidController(PCSCEidController eidController) {
	this.eidController = eidController;
	return this;
    }

    public CertificatesPanel start() {
	initTrustPrefsPanel();
	certsBusyIcon.setVisible(false);
	certsTree.addTreeSelectionListener(this);
	clearCertsTree();
	return this;
    }

    public void update(Observable o, Object o1) {
	if (eidController == null)
	    return;

	logger.finest("Updating..");
	updateVisibleState();

	if (eidController.getState() == PCSCEidController.STATE.EID_PRESENT
		|| eidController.getState() == PCSCEidController.STATE.EID_YIELDED
		|| eidController.getState() == PCSCEidController.STATE.FILE_LOADED) {
	    logger.finest("Filling Out Certificate Data..");

	    if (eidController.hasRRNCertChain())
		addCerts(eidController.getRRNCertChain());

	    if (eidController.hasAuthCertChain())
		addCerts(eidController.getAuthCertChain());

	    if (eidController.hasSignCertChain())
		addCerts(eidController.getSignCertChain());
	} else {
	    logger.finest("Clearing Certificate Data..");
	    clearCertsTree();
	    rootNode = null;
	}
    }

    private void addCerts(X509CertificateChainAndTrust chain) {
	if (chain != null) {
	    List<X509CertificateAndTrust> certificates = chain
		    .getCertificatesAndTrusts();
	    for (ListIterator<X509CertificateAndTrust> i = certificates
		    .listIterator(certificates.size()); i.hasPrevious();) {
		X509CertificateAndTrust certificate = i.previous();
		DefaultMutableTreeNode existingNode = certificatesInTree
			.get(certificate.getSubjectDN());

		if (existingNode == null) // new information to add
		{
		    DefaultMutableTreeNode newkid = new DefaultMutableTreeNode(
			    certificate);
		    DefaultMutableTreeNode parent = certificatesInTree
			    .get(certificate.getIssuerDN());
		    addTreeNode(parent, newkid);
		    certificatesInTree.put(certificate.getSubjectDN(), newkid);
		    updateCertificateDetail();
		} else // existing information to update
		{
		    updateTreeNode(existingNode);
		    updateCertificateDetail();
		    logger.log(Level.FINEST, "UPDATE [{0}]",
			    (((X509CertificateAndTrust) (existingNode
				    .getUserObject())).getSubjectDN()
				    .toString()));
		}
	    }
	}
    }

    private void updateVisibleState() {
	java.awt.EventQueue.invokeLater(new Runnable() {
	    public void run() {
		validateNowButton.setEnabled(eidController.isReadyForCommand());

		if (eidController.getState() == PCSCEidController.STATE.EID_PRESENT)
		    certsBusyIcon.setVisible(eidController.getActivity() == PCSCEidController.ACTIVITY.READING_AUTH_CHAIN
			    || eidController.getActivity() == PCSCEidController.ACTIVITY.READING_SIGN_CHAIN
			    || eidController.getActivity() == PCSCEidController.ACTIVITY.READING_RRN_CHAIN
			    || eidController.isValidatingTrust());
		else
		    certsBusyIcon.setVisible(false);
	    }
	});
    }

    private void updateTreeNode(final DefaultMutableTreeNode changedNode) {
	java.awt.EventQueue.invokeLater(new Runnable() {
	    public void run() {
		treeModel.nodeChanged(changedNode);
	    }
	});
    }

    private void addTreeNode(final DefaultMutableTreeNode parent,
	    final DefaultMutableTreeNode child) {
	java.awt.EventQueue.invokeLater(new Runnable() {
	    public void run() {
		if (parent == null) // Belgian Root CA. When this arrives,
				    // activate the tree
		{
		    rootNode = child;
		    treeModel = new DefaultTreeModel(rootNode);
		    certsTree.setModel(treeModel);
		    certsTree.setVisible(true);
		} else // all other certs are attached to their parent node
		{
		    treeModel.insertNodeInto(child, parent,
			    parent.getChildCount());
		    for (int row = 0; row < certsTree.getRowCount(); row++)
			// and auto-expand to display it
			certsTree.expandRow(row);
		    X509CertificateAndTrust certAndTrust = (X509CertificateAndTrust) child
			    .getUserObject();

		    // auto-select the authentication certificate
		    if (certAndTrust != null
			    && X509Utilities
				    .hasDigitalSignatureConstraint(certAndTrust
					    .getCertificate()))
			certsTree.setSelectionPath(new TreePath(child.getPath()));
		}
	    }
	});
    }

    // user (de)selected a certificate
    public void valueChanged(TreeSelectionEvent treeSelectionEvent) {
	updateCertificateDetail();
    }

    private void updateCertificateDetail() {
	DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) certsTree
		.getLastSelectedPathComponent();

	if (treeNode != null) {
	    X509CertificateAndTrust certAndTrust = (X509CertificateAndTrust) treeNode
		    .getUserObject();
	    certificateSelected(certAndTrust);
	} else {
	    certificateSelected(null);
	}
    }

    private void certificateSelected(final X509CertificateAndTrust certAndTrust) {
	java.awt.EventQueue.invokeLater(new Runnable() {
	    public void run() {
		if (certAndTrust != null) {
		    X509Certificate certificate = certAndTrust.getCertificate();

		    if (certAndTrust.isValidated()) {
			if (certAndTrust.isTrusted()) {
			    trustStatus.setText(bundle
				    .getString("trustStatus_trusted"));
			    trustErrors.setVisible(false);
			    trustStatus
				    .setForeground(greener(defaultLabelForeground));
			    trustStatusLabel
				    .setForeground(greener(defaultLabelForeground));
			    trustStatus
				    .setBackground(greener(defaultLabelBackground));
			    trustStatusLabel
				    .setBackground(greener(defaultLabelBackground));
			} else {
			    trustStatus.setText(bundle
				    .getString("trustStatus_untrusted"));
			    trustErrors.setText(getMultilineLabelBundleText(
				    bundle, "trustError_",
				    certAndTrust.getInvalidReasons()));
			    trustErrors.setVisible(true);
			    trustStatus
				    .setForeground(redder(defaultLabelForeground));
			    trustStatusLabel
				    .setForeground(redder(defaultLabelForeground));
			    trustStatus
				    .setBackground(redder(defaultLabelBackground));
			    trustStatusLabel
				    .setBackground(redder(defaultLabelBackground));
			}
		    } else {
			trustStatus.setForeground(defaultLabelForeground);
			trustStatusLabel.setForeground(defaultLabelForeground);
			trustStatus.setBackground(defaultLabelBackground);
			trustStatusLabel.setBackground(defaultLabelBackground);

			if (certAndTrust.getValidationException() != null) {
			    trustStatus.setText(getMultilinelabelText(bundle
				    .getString("trustStatus_unobtainable")));
			    trustErrors.setText(certAndTrust
				    .getValidationException()
				    .getLocalizedMessage());
			    trustErrors.setVisible(true);
			} else {
			    trustStatus.setText(bundle.getString(certAndTrust
				    .isValidating() ? "trustStatus_validating"
				    : "trustStatus_unknown"));
			    trustStatus.setForeground(defaultLabelForeground);
			    trustErrors.setVisible(false);
			}
		    }

		    dn.setText(getMultilineDN(certificate.getSubjectDN()
			    .getName()));
		    validFrom.setText(dateFormat.format(certificate
			    .getNotBefore().getTime()));
		    validUntil.setText(dateFormat.format(certificate
			    .getNotAfter().getTime()));
		    keyUsage.setText(getMultilineLabelText(X509Utilities
			    .getKeyUsageStrings(bundle,
				    certificate.getKeyUsage())));

		    try {
			certificate.checkValidity();
			validFromLabel.setForeground(defaultLabelForeground);
			validFrom.setForeground(defaultLabelForeground);
			validFrom.setText(dateFormat.format(certificate
				.getNotBefore().getTime()));
			validUntilLabel.setForeground(defaultLabelForeground);
			validUntil.setForeground(defaultLabelForeground);
			validUntil.setText(dateFormat.format(certificate
				.getNotAfter().getTime()));
		    } catch (CertificateExpiredException ex) {
			validFromLabel.setForeground(defaultLabelForeground);
			validFrom.setForeground(defaultLabelForeground);
			validFrom.setText(dateFormat.format(certificate
				.getNotBefore().getTime()));
			validUntilLabel
				.setForeground(redder(defaultLabelForeground));
			validUntil
				.setForeground(redder(defaultLabelForeground));
			validUntil.setText(dateFormat.format(certificate
				.getNotAfter().getTime())
				+ " "
				+ bundle.getString("notAfterWarning"));
		    } catch (CertificateNotYetValidException ex) {
			validFromLabel
				.setForeground(redder(defaultLabelForeground));
			validFrom.setForeground(redder(defaultLabelForeground));
			validFrom.setText(dateFormat.format(certificate
				.getNotBefore().getTime())
				+ " "
				+ bundle.getString("notBeforeWarning"));
			validUntilLabel.setForeground(defaultLabelForeground);
			validUntil.setForeground(defaultLabelForeground);
			validUntil.setText(dateFormat.format(certificate
				.getNotAfter().getTime()));
		    }

		    dn.setEnabled(true);
		    keyUsage.setEnabled(true);
		    validFromLabel.setEnabled(true);
		    validUntilLabel.setEnabled(true);
		    validFrom.setEnabled(true);
		    validUntil.setEnabled(true);
		    certificateIcon.setEnabled(true);
		    trustStatus.setEnabled(true);
		    trustStatusLabel.setEnabled(true);
		    keyUsageLabel.setEnabled(true);
		} else {
		    dn.setText("");
		    keyUsage.setText("");
		    validFrom.setText(TextFormatHelper.UNKNOWN_VALUE_TEXT);
		    validUntil.setText(TextFormatHelper.UNKNOWN_VALUE_TEXT);
		    trustStatus.setText(TextFormatHelper.UNKNOWN_VALUE_TEXT);

		    trustStatus.setForeground(defaultLabelForeground);
		    trustStatusLabel.setForeground(defaultLabelForeground);
		    trustStatus.setBackground(defaultLabelBackground);
		    trustStatusLabel.setBackground(defaultLabelBackground);

		    dn.setEnabled(false);
		    keyUsage.setEnabled(false);
		    validFromLabel.setEnabled(false);
		    validUntilLabel.setEnabled(false);
		    validFrom.setEnabled(false);
		    validUntil.setEnabled(false);
		    certificateIcon.setEnabled(false);
		    trustErrors.setVisible(false);
		    trustStatus.setEnabled(false);
		    trustStatusLabel.setEnabled(false);
		    keyUsageLabel.setEnabled(false);
		}
	    }
	});
    }

    private void initComponents() {
	java.awt.GridBagConstraints gridBagConstraints;

	certsDetailsSplitPane = new JSplitPane();
	authCertsPanel = new JPanel();
	certsTree = new JTree();
	certsBusyIcon = new JLabel();
	certDetailsPanel = new JPanel();
	certificateIcon = new JLabel();
	dn = new JLabel();
	validFromLabel = new JLabel();
	validUntilLabel = new JLabel();
	validFrom = new JLabel();
	validUntil = new JLabel();
	keyUsage = new JLabel();
	trustStatusLabel = new JLabel();
	trustStatus = new JLabel();
	trustErrors = new JLabel();
	keyUsageLabel = new JLabel();
	trustServiceTrustErrorsSeparator = new JSeparator();
	spacer = new JLabel();
	validdUntilKeyUsageSeparator = new JSeparator();
	keyUsageTrustSeparator = new JSeparator();
	trustPrefspanel = new JPanel();
	alwaysValidateCheckbox = new JCheckBox();
	validateNowButton = new JButton();

	MouseAdapter certContextMenuListener = new MouseAdapter() {
	    private void maybeShowPopup(MouseEvent e) {
		if (e.isPopupTrigger()) {
		    Point p = e.getPoint();
		    TreePath path = certsTree.getPathForLocation(p.x, p.y);

		    if (path != null) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) path
				.getLastPathComponent();
			Object nodeInfo = node.getUserObject();
			exportingCertificate = (X509CertificateAndTrust) nodeInfo;

			JPopupMenu certContextMenu = new JPopupMenu();
			certContextMenu.add(new JMenuItem(certificateDetailAction));
			certContextMenu.addSeparator();
			certContextMenu.add(new JMenuItem(exportPEMAction));
			certContextMenu.add(new JMenuItem(exportDERAction));
			if (!X509Utilities.isSelfSigned(exportingCertificate
				.getCertificate())) {
			    certContextMenu.addSeparator();
			    certContextMenu.add(new JMenuItem(
				    exportChainPEMAction));
			}

			certContextMenu.show(e.getComponent(), e.getX(),
				e.getY());
		    }
		}
	    }

	    public void mousePressed(MouseEvent e) {
		maybeShowPopup(e);
	    }

	    public void mouseReleased(MouseEvent e) {
		maybeShowPopup(e);
	    }
	};
	certsTree.addMouseListener(certContextMenuListener);

	setBorder(ImageUtilities.getEIDBorder());
	setLayout(new java.awt.BorderLayout());

	certsDetailsSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);

	authCertsPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
	authCertsPanel.setMinimumSize(new java.awt.Dimension(24, 148));
	authCertsPanel.setOpaque(false);
	authCertsPanel.setPreferredSize(new java.awt.Dimension(600, 124));
	authCertsPanel.setLayout(new java.awt.BorderLayout());

	certsTree.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
	authCertsPanel.add(certsTree, java.awt.BorderLayout.CENTER);

	certsBusyIcon
		.setIcon(new ImageIcon(
			getClass()
				.getResource(
					"/be/fedict/eidviewer/gui/resources/busyicons/busy_anim_small.gif"))); // NOI18N
	authCertsPanel.add(certsBusyIcon, java.awt.BorderLayout.WEST);

	certsDetailsSplitPane.setLeftComponent(authCertsPanel);

	certDetailsPanel.setLayout(new java.awt.GridBagLayout());

	certificateIcon
		.setIcon(new ImageIcon(
			getClass()
				.getResource(
					"/be/fedict/eidviewer/gui/resources/icons/certificate_large.png"))); // NOI18N
	gridBagConstraints = new java.awt.GridBagConstraints();
	gridBagConstraints.gridheight = 5;
	gridBagConstraints.ipadx = 8;
	gridBagConstraints.ipady = 8;
	gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
	certDetailsPanel.add(certificateIcon, gridBagConstraints);

	dn.setBackground(new java.awt.Color(204, 204, 204));
	dn.setOpaque(true);
	gridBagConstraints = new java.awt.GridBagConstraints();
	gridBagConstraints.gridwidth = 3;
	gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
	gridBagConstraints.ipadx = 12;
	gridBagConstraints.ipady = 12;
	gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
	gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
	certDetailsPanel.add(dn, gridBagConstraints);

	gridBagConstraints = new java.awt.GridBagConstraints();
	gridBagConstraints.gridx = 1;
	gridBagConstraints.gridy = 2;
	gridBagConstraints.ipadx = 12;
	gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
	gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
	certDetailsPanel.add(validFromLabel, gridBagConstraints);

	gridBagConstraints = new java.awt.GridBagConstraints();
	gridBagConstraints.gridx = 1;
	gridBagConstraints.gridy = 3;
	gridBagConstraints.ipadx = 12;
	gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
	gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
	certDetailsPanel.add(validUntilLabel, gridBagConstraints);

	gridBagConstraints = new java.awt.GridBagConstraints();
	gridBagConstraints.gridx = 3;
	gridBagConstraints.gridy = 2;
	gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
	gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
	gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 4);
	certDetailsPanel.add(validFrom, gridBagConstraints);

	gridBagConstraints = new java.awt.GridBagConstraints();
	gridBagConstraints.gridx = 3;
	gridBagConstraints.gridy = 3;
	gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
	gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
	gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 4);
	certDetailsPanel.add(validUntil, gridBagConstraints);

	keyUsage.setOpaque(true);
	gridBagConstraints = new java.awt.GridBagConstraints();
	gridBagConstraints.gridx = 3;
	gridBagConstraints.gridy = 5;
	gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
	gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
	gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 4);
	certDetailsPanel.add(keyUsage, gridBagConstraints);

	gridBagConstraints = new java.awt.GridBagConstraints();
	gridBagConstraints.gridx = 1;
	gridBagConstraints.gridy = 7;
	gridBagConstraints.ipadx = 12;
	gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
	gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
	certDetailsPanel.add(trustStatusLabel, gridBagConstraints);

	trustStatus.setOpaque(true);
	gridBagConstraints = new java.awt.GridBagConstraints();
	gridBagConstraints.gridx = 3;
	gridBagConstraints.gridy = 7;
	gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
	gridBagConstraints.ipadx = 2;
	gridBagConstraints.ipady = 2;
	gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
	gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 4);
	certDetailsPanel.add(trustStatus, gridBagConstraints);

	trustErrors.setBackground(new java.awt.Color(255, 153, 102));
	trustErrors.setHorizontalAlignment(SwingConstants.CENTER);
	trustErrors.setOpaque(true);
	gridBagConstraints = new java.awt.GridBagConstraints();
	gridBagConstraints.gridx = 1;
	gridBagConstraints.gridy = 10;
	gridBagConstraints.gridwidth = 3;
	gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
	gridBagConstraints.ipadx = 12;
	gridBagConstraints.ipady = 12;
	gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
	certDetailsPanel.add(trustErrors, gridBagConstraints);

	gridBagConstraints = new java.awt.GridBagConstraints();
	gridBagConstraints.gridx = 1;
	gridBagConstraints.gridy = 5;
	gridBagConstraints.ipadx = 12;
	gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
	gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
	certDetailsPanel.add(keyUsageLabel, gridBagConstraints);

	gridBagConstraints = new java.awt.GridBagConstraints();
	gridBagConstraints.gridx = 1;
	gridBagConstraints.gridy = 9;
	gridBagConstraints.gridwidth = 3;
	gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
	gridBagConstraints.insets = new java.awt.Insets(8, 0, 4, 0);
	certDetailsPanel.add(trustServiceTrustErrorsSeparator,
		gridBagConstraints);

	spacer.setEnabled(false);
	spacer.setMaximumSize(new java.awt.Dimension(16, 16));
	spacer.setMinimumSize(new java.awt.Dimension(16, 16));
	spacer.setPreferredSize(new java.awt.Dimension(16, 16));
	gridBagConstraints = new java.awt.GridBagConstraints();
	gridBagConstraints.gridx = 2;
	gridBagConstraints.gridy = 3;
	gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
	certDetailsPanel.add(spacer, gridBagConstraints);

	gridBagConstraints = new java.awt.GridBagConstraints();
	gridBagConstraints.gridx = 1;
	gridBagConstraints.gridy = 4;
	gridBagConstraints.gridwidth = 3;
	gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
	gridBagConstraints.insets = new java.awt.Insets(8, 0, 4, 0);
	certDetailsPanel.add(validdUntilKeyUsageSeparator, gridBagConstraints);

	gridBagConstraints = new java.awt.GridBagConstraints();
	gridBagConstraints.gridx = 1;
	gridBagConstraints.gridy = 6;
	gridBagConstraints.gridwidth = 3;
	gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
	gridBagConstraints.insets = new java.awt.Insets(8, 0, 4, 0);
	certDetailsPanel.add(keyUsageTrustSeparator, gridBagConstraints);

	certsDetailsSplitPane.setRightComponent(certDetailsPanel);

	add(certsDetailsSplitPane, java.awt.BorderLayout.CENTER);
	trustPrefspanel.setLayout(new java.awt.FlowLayout(
		java.awt.FlowLayout.CENTER, 16, 5));
	trustPrefspanel.add(alwaysValidateCheckbox);
	trustPrefspanel.add(validateNowButton);
	add(trustPrefspanel, java.awt.BorderLayout.PAGE_END);
    }

    private void initI18N() {
	Locale.setDefault(ViewerPrefs.getLocale());
	bundle = ResourceBundle
		.getBundle("be/fedict/eidviewer/gui/resources/CertificatesPanel");
	dateFormat = DateFormat.getDateInstance(DateFormat.LONG,
		Locale.getDefault());
	dn.setText("(cn)");
	validFromLabel.setText(bundle.getString("validFromLabel")); // NOI18N
	validUntilLabel.setText(bundle.getString("validUntilLabel")); // NOI18N
	validFrom.setText("-");
	validUntil.setText("-");
	keyUsage.setText(bundle.getString("keyUsageLabel")); // NOI18N
	trustStatusLabel.setText(bundle.getString("trustStatusLabel")); // NOI18N
	trustStatus.setText("-"); // NOI18N
	trustErrors.setText("(trusterrors)");
	keyUsageLabel.setText(bundle.getString("keyUsageLabel")); // NOI18N
	alwaysValidateCheckbox.setText(bundle
		.getString("alwaysValidateCheckbox")); // NOI18N
	validateNowButton.setText(bundle.getString("validateNowButton")); // NOI18N

	exportPEMAction.setName(bundle.getString("exportToPEM"));
	exportDERAction.setName(bundle.getString("exportToDER"));
	exportChainPEMAction.setName(bundle.getString("exportChainToPEM"));
	certificateDetailAction.setName(bundle.getString("showDetails"));

	if (certificatesInTree == null)
	    return;

	// update cert names, to adapt to possibly longer name for
	// "RRN (translation)"
	for (DefaultMutableTreeNode nodeToUpdate : certificatesInTree.values())
	    updateTreeNode(nodeToUpdate);

	updateCertificateDetail();
    }

    private void initCertsTree() {
	certificatesInTree = new HashMap<Principal, DefaultMutableTreeNode>(5);
	certsTree.setCellRenderer(new CertAndTrustCellRenderer());
	certsTree.getSelectionModel().setSelectionMode(
		TreeSelectionModel.SINGLE_TREE_SELECTION);
	certsTree.setRootVisible(true);
    }

    private void initTrustPrefsPanel() {
	alwaysValidateCheckbox.setSelected(eidController
		.isAutoValidatingTrust());

	alwaysValidateCheckbox.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent ae) {
		eidController.setAutoValidateTrust(alwaysValidateCheckbox
			.isSelected());
		ViewerPrefs.setAutoValidating(alwaysValidateCheckbox
			.isSelected());
		if (alwaysValidateCheckbox.isSelected()
			&& eidController.isReadyForCommand())
		    eidController.validateTrust();

	    }
	});

	validateNowButton.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent ae) {
		if (eidController.isReadyForCommand())
		    eidController.validateTrust();
	    }
	});
    }

    public void setDynamicLocale(Locale locale) {
	initI18N();
	updateCertificateDetail();
    }

    private class CertAndTrustCellRenderer extends DefaultTreeCellRenderer {
	private static final long serialVersionUID = 7097352650872290815L;
	private Icon certIcon, certTrustedIcon, certInvalidIcon;
	private Color redSelectedForeground, redForeground;
	private Color greenSelectedForeground, greenForeground;
	private Color defaultSelectedForeground, defaultForeground;
	private Font defaultFont, boldFont;

	public CertAndTrustCellRenderer() {
	    certIcon = ImageUtilities.getIcon(CertificatesPanel.class, ICONS
		    + "certificate_small.png");
	    certTrustedIcon = ImageUtilities.getIcon(CertificatesPanel.class,
		    ICONS + "certificate_trusted_small.png");
	    certInvalidIcon = ImageUtilities.getIcon(CertificatesPanel.class,
		    ICONS + "certificate_invalid_small.png");
	    defaultSelectedForeground = UIManager
		    .getColor("Tree.selectionForeground");
	    defaultForeground = UIManager.getColor("Tree.textForeground");
	    redForeground = redder(defaultForeground);
	    redSelectedForeground = redder(defaultSelectedForeground);
	    greenForeground = greener(defaultForeground);
	    greenSelectedForeground = greener(defaultSelectedForeground);
	}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value,
		boolean sel, boolean expanded, boolean leaf, int row,
		boolean hasFocus) {
	    super.getTreeCellRendererComponent(tree, value, sel, expanded,
		    leaf, row, hasFocus);
	    DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) value;

	    if (!(treeNode.getUserObject() instanceof X509CertificateAndTrust))
		return this;

	    if (defaultFont == null)
		defaultFont = getFont();

	    if (boldFont == null)
		boldFont = getFont().deriveFont(Font.BOLD);

	    X509CertificateAndTrust certAndTrust = (X509CertificateAndTrust) treeNode
		    .getUserObject();

	    if (certAndTrust.isValidated()) {
		if (certAndTrust.isTrusted()) {
		    setTextNonSelectionColor(greenForeground);
		    setTextSelectionColor(greenSelectedForeground);
		    setIcon(certTrustedIcon);
		    setFont(boldFont);
		} else {
		    setTextNonSelectionColor(redForeground);
		    setTextSelectionColor(redSelectedForeground);
		    setIcon(certInvalidIcon);
		    setFont(boldFont);
		}
	    } else {
		setTextNonSelectionColor(defaultForeground);
		setTextSelectionColor(defaultSelectedForeground);
		setIcon(certIcon);
		setFont(defaultFont);
	    }

	    return this;
	}
    }

    private Color greener(Color originalColor) {
	Color less = originalColor.darker().darker();
	Color more = originalColor.brighter().brighter();
	return new Color(less.getRed(), more.getGreen(), less.getBlue());
    }

    private Color redder(Color originalColor) {
	Color less = originalColor.darker().darker();
	Color more = originalColor.brighter().brighter();
	return new Color(more.getRed(), less.getGreen(), less.getBlue());
    }

    private void clearCertsTree() {
	certsTree.setVisible(false);
	certificateSelected(null);
	certificatesInTree.clear();
	treeModel = new DefaultTreeModel(new DefaultMutableTreeNode("-"));
	certsTree.setModel(treeModel);
    }

    private String getMultilineDN(String dnStr) {
	StringBuilder html = new StringBuilder("<html>");
	String[] dnParts = dnStr.split("\\s*,\\s*");
	for (String dnPart : dnParts) {
	    html.append(dnPart);
	    html.append("<br/>");
	}
	html.append("</html>");
	return html.toString();
    }

    private String getMultilinelabelText(String string) {
	StringBuilder html = new StringBuilder("<html>");
	String[] parts = string.split(";");
	for (String part : parts) {
	    html.append(part);
	    html.append("<br/>");
	}
	html.append("</html>");
	return html.toString();
    }

    private String getMultilineLabelText(List<String> strings) {
	StringBuilder html = new StringBuilder("<html>");
	for (String string : strings) {
	    html.append(string);
	    html.append("<br/>");
	}
	html.append("</html>");
	return html.toString();
    }

    private String getMultilineLabelBundleText(ResourceBundle bundle,
	    String prefix, List<String> strings) {
	StringBuilder html = new StringBuilder("<html>");
	for (String key : strings) {
	    html.append(bundle.getString(prefix + key));
	    html.append("<br/>");
	}
	html.append("</html>");
	return html.toString();
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
	logger.fine(actionEvent.getActionCommand());
    }

    private File suggestedExportFile(X509Certificate certificate,
	    String extension, boolean isChain) throws IOException {
	String commonName = X509Utilities.getCN(certificate).toLowerCase()
		.replace(' ', '_').replaceAll("[^a-z0-9_]", "");
	File suggestedFile = new File(new File(commonName
		+ (isChain ? "_chain." : ".") + extension).getCanonicalPath());
	logger.log(Level.FINE, "Suggesting \"{0}\"",
		suggestedFile.getCanonicalPath());
	return suggestedFile;
    }

    private File ensureFilenameExtension(File targetFile, String extension)
	    throws IOException {
	if (targetFile.getCanonicalPath().toLowerCase()
		.endsWith("." + extension))
	    return targetFile;
	logger.fine("File would not have correct extension, appending extension \"."
		+ extension + "\"");
	return new File(targetFile.getCanonicalPath() + "." + extension);
    }

    private class ExportPEMAction extends DynamicLocaleAbstractAction {
	private static final long serialVersionUID = 2751644027503348460L;

	public ExportPEMAction(String text) {
	    super(text);
	}

	public void actionPerformed(ActionEvent ae) {
	    logger.fine("Export PEM action chosen..");

	    final JFileChooser fileChooser = new JFileChooser();
	    fileChooser.setAcceptAllFileFilterUsed(false);
	    fileChooser.addChoosableFileFilter(new CertFileFilter(true, false,
		    bundle.getString("pemFiles")));
	    fileChooser.addChoosableFileFilter(new CertFileFilter(false, true,
		    bundle.getString("allFiles")));

	    try {
		fileChooser.setSelectedFile(suggestedExportFile(
			exportingCertificate.getCertificate(), "pem", false));
	    } catch (IOException ex) {
		// suggested file likely doesn't exist yet but that's OK here.
	    }

	    if (fileChooser.showSaveDialog(CertificatesPanel.this) == JFileChooser.APPROVE_OPTION) {
		try {
		    File targetFile = ensureFilenameExtension(
			    fileChooser.getSelectedFile(), "pem");
		    X509Utilities.certificateToPEMFile(
			    exportingCertificate.getCertificate(), targetFile);
		} catch (IOException ioex) {
		    logger.log(
			    Level.SEVERE,
			    "Problem getting Canonical Name For Filename Extension Correction",
			    ioex);
		} catch (CertificateEncodingException cex) {
		    logger.log(Level.SEVERE,
			    "Can't Export Certificate to PEM Format", cex);
		}
	    }
	}
    }

    private class ExportDERAction extends DynamicLocaleAbstractAction {
	private static final long serialVersionUID = 6701186462084833299L;

	public ExportDERAction(String text) {
	    super(text);
	}

	public void actionPerformed(ActionEvent ae) {
	    logger.fine("Export DER action chosen..");

	    final JFileChooser fileChooser = new JFileChooser();
	    fileChooser.setAcceptAllFileFilterUsed(false);
	    fileChooser.addChoosableFileFilter(new CertFileFilter(true, false,
		    bundle.getString("derFiles")));
	    fileChooser.addChoosableFileFilter(new CertFileFilter(false, true,
		    bundle.getString("allFiles")));

	    try {
		fileChooser.setSelectedFile(suggestedExportFile(
			exportingCertificate.getCertificate(), "der", false));
	    } catch (IOException ex) {
		// suggested file likely doesn't exist yet but that's OK here.
	    }

	    if (fileChooser.showSaveDialog(CertificatesPanel.this) == JFileChooser.APPROVE_OPTION) {
		try {
		    File targetFile = ensureFilenameExtension(
			    fileChooser.getSelectedFile(), "der");
		    X509Utilities.certificateToDERFile(
			    exportingCertificate.getCertificate(), targetFile);
		} catch (IOException ioex) {
		    logger.log(
			    Level.SEVERE,
			    "Problem getting Canonical Name For Filename Extension Correction",
			    ioex);
		} catch (CertificateEncodingException cex) {
		    logger.log(Level.SEVERE,
			    "Can't Export Certificate to DER Format", cex);
		}
	    }
	}
    }

    private class ExportChainPEMAction extends DynamicLocaleAbstractAction {
	private static final long serialVersionUID = 2751644027503348460L;

	public ExportChainPEMAction(String text) {
	    super(text);
	}

	public void actionPerformed(ActionEvent ae) {
	    logger.fine("Export PEM Chain action chosen..");

	    final JFileChooser fileChooser = new JFileChooser();
	    fileChooser.setAcceptAllFileFilterUsed(false);
	    fileChooser.addChoosableFileFilter(new CertFileFilter(true, false,
		    bundle.getString("pemFiles")));
	    fileChooser.addChoosableFileFilter(new CertFileFilter(false, true,
		    bundle.getString("allFiles")));

	    try {
		fileChooser.setSelectedFile(suggestedExportFile(
			exportingCertificate.getCertificate(), "pem", true));
	    } catch (IOException ex) {
		// suggested file likely doesn't exist yet but that's OK here.
	    }

	    if (fileChooser.showSaveDialog(CertificatesPanel.this) == JFileChooser.APPROVE_OPTION) {
		try {
		    File targetFile = ensureFilenameExtension(
			    fileChooser.getSelectedFile(), "pem");
		    String commonName = X509Utilities
			    .getCN(exportingCertificate.getCertificate());

		    if (commonName.endsWith("(Authentication)")) {
			X509Utilities.certificateChainToPEMFile(eidController
				.getAuthCertChain().getCertificates(),
				targetFile);
		    } else if (commonName.endsWith("(Signature)")) {
			X509Utilities.certificateChainToPEMFile(eidController
				.getSignCertChain().getCertificates(),
				targetFile);
		    } else if (commonName.startsWith("RRN")) {
			X509Utilities.certificateChainToPEMFile(eidController
				.getRRNCertChain().getCertificates(),
				targetFile);
		    } else if (commonName.startsWith("Citizen CA")) {
			X509Utilities.certificateChainToPEMFile(
				eidController.getCitizenCACertChain(),
				targetFile);
		    }
		} catch (IOException ioex) {
		    logger.log(
			    Level.SEVERE,
			    "Problem getting Canonical Name For Filename Extension Correction",
			    ioex);
		}
	    }
	}
    }

    private class CertificateDetailAction extends DynamicLocaleAbstractAction {
	private static final long serialVersionUID = -3054897441714476522L;

	public CertificateDetailAction(String text) {
	    super(text);
	}

	public void actionPerformed(ActionEvent ae) {
	    JOptionPane.showMessageDialog(null, new CertificateDetailPanel(
		    exportingCertificate.getCertificate().toString()), bundle
		    .getString("showDetails"), JOptionPane.PLAIN_MESSAGE);
	}
    }
}
