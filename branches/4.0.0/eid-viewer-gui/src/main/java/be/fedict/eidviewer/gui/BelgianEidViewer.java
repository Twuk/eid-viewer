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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;

import be.fedict.eid.applet.DiagnosticTests;
import be.fedict.eid.applet.Messages;
import be.fedict.eid.applet.Messages.MESSAGE_ID;
import be.fedict.eid.applet.Status;
import be.fedict.eid.applet.View;
import be.fedict.eidviewer.gui.helper.EidFileFilter;
import be.fedict.eidviewer.gui.helper.EidFilePreviewAccessory;
import be.fedict.eidviewer.gui.helper.EidFileView;
import be.fedict.eidviewer.gui.helper.ImageUtilities;
import be.fedict.eidviewer.lib.Eid;
import be.fedict.eidviewer.lib.PCSCEidImpl;
import java.awt.Component;
import java.awt.Event;
import java.awt.event.KeyEvent;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu.Separator;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

/**
 *
 * @author Frank Marien
 */
public class BelgianEidViewer extends javax.swing.JFrame implements View, Observer, DiagnosticsContainer
{

    private static final Logger logger = Logger.getLogger(BelgianEidViewer.class.getName());
    private ResourceBundle bundle;
    private static final String EXTENSION_PNG = ".png";
    private static final String ICONS = "resources/icons/";
    private JMenu fileMenu;
    private JMenuItem fileMenuQuitItem;
    private JMenu helpMenu;
    private JMenuItem printMenuItem;
    private JMenuItem openMenuItem;
    private JMenuItem saveMenuItem;
    private JMenuItem closeMenuItem;
    private JMenuItem aboutMenuItem;
    private JMenuBar menuBar;
    private JButton printButton;
    private JPanel printPanel;
    private JLabel statusIcon;
    private JPanel statusPanel;
    private JLabel statusText;
    private JTabbedPane tabPanel;
    private Messages coreMessages;
    private Eid eid;
    private EidController eidController;
    private TrustServiceController trustServiceController;
    private EnumMap<EidController.STATE, ImageIcon> cardStatusIcons;
    private EnumMap<EidController.STATE, String> cardStatusTexts;
    private EnumMap<EidController.ACTIVITY, String> activityTexts;
    private IdentityPanel identityPanel;
    private CertificatesPanel certificatesPanel;
    private CardPanel cardPanel;
    private PreferencesPanel preferencesPanel;
    private LogPanel logPanel;
    private Action printAction, openAction, saveAction, closeAction, aboutAction, quitAction;

    public BelgianEidViewer()
    {
        Locale.setDefault(ViewerPrefs.getLocale());
        bundle = ResourceBundle.getBundle("be/fedict/eidviewer/gui/resources/BelgianEidViewer");
        coreMessages = new Messages(Locale.getDefault());
        initActions();
        initComponents();
        initPanels();
        initIcons();
        initTexts();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
    
    private void initActions()
    {
        printAction = new PrintAction("Print", new Integer(KeyEvent.VK_P));
        openAction = new OpenFileAction("Open", new Integer(KeyEvent.VK_O));
        closeAction = new CloseFileAction("Close", new Integer(KeyEvent.VK_W));
        saveAction = new SaveFileAction("Save As", new Integer(KeyEvent.VK_S));
        aboutAction = new AboutAction("About");
        quitAction = new QuitAction("Quit",new Integer(KeyEvent.VK_Q));
    }

    private void start()
    {
        Properties properties = System.getProperties();
        Set<String> labels = properties.stringPropertyNames();
        for (String label : labels)
        {
            logger.log(Level.INFO, "{0}={1}", new Object[]
                    {
                        label, properties.getProperty(label)
                    });
        }
        logger.fine("starting..");

        eid = new PCSCEidImpl(this, coreMessages);
        eidController = new EidController(eid);

        trustServiceController = new TrustServiceController(ViewerPrefs.getTrustServiceURL());
        trustServiceController.start();

        if (ViewerPrefs.getUseHTTPProxy())
        {
            trustServiceController.setProxy(ViewerPrefs.getHTTPProxyHost(), ViewerPrefs.getHTTPProxyPort());
        }
        else
        {
            trustServiceController.setProxy(null, 0);
        }

        eidController.setTrustServiceController(trustServiceController);
        eidController.setAutoValidateTrust(ViewerPrefs.getIsAutoValidating());

        identityPanel.setEidController(eidController);

        cardPanel.setEidController(eidController);

        certificatesPanel.setEidController(eidController);
        certificatesPanel.start();

        preferencesPanel.setTrustServiceController(trustServiceController);
        preferencesPanel.setEidController(eidController);
        preferencesPanel.setDiagnosticsContainer(this);
        preferencesPanel.start();

        eidController.addObserver(identityPanel);
        eidController.addObserver(cardPanel);
        eidController.addObserver(certificatesPanel);
        eidController.addObserver(preferencesPanel);
        eidController.addObserver(this);
        eidController.start();

        setVisible(true);
    }

    private void stop()
    {
        logger.fine("stopping..");
        eidController.stop();
        trustServiceController.stop();
        this.dispose();
    }

    public void update(Observable o, Object o1)
    {
        updateVisibleState();

    }

    private void updateVisibleState()
    {
        java.awt.EventQueue.invokeLater(new Runnable()
        {

            public void run()
            {
                printAction.setEnabled(eidController.hasIdentity() && eidController.hasAddress() && eidController.hasPhoto() && (PrinterJob.lookupPrintServices().length > 0));
                saveAction.setEnabled(eidController.hasIdentity() && eidController.hasAddress() && eidController.hasPhoto() && eidController.hasAuthCertChain());
                openAction.setEnabled(eidController.getState() != EidController.STATE.EID_PRESENT && eidController.getState() != EidController.STATE.EID_YIELDED);
                closeAction.setEnabled(eidController.isLoadedFromFile() && (eidController.hasAddress() || eidController.hasPhoto() || eidController.hasAuthCertChain() || eidController.hasSignCertChain()));

                statusIcon.setIcon(cardStatusIcons.get(eidController.getState()));

                switch (eidController.getState())
                {
                    case EID_PRESENT:
                        statusText.setText(activityTexts.get(eidController.getActivity()));
                        break;

                    case FILE_LOADED:
                    default:
                        statusText.setText(cardStatusTexts.get(eidController.getState()));
                }
            }
        });
    }

    private void initComponents()
    {
        tabPanel = new JTabbedPane();
        statusPanel = new JPanel();
        statusIcon = new JLabel();
        statusText = new JLabel();
        printPanel = new JPanel();
        printButton = new JButton();
        menuBar = new JMenuBar();
        fileMenu = new JMenu();
        openMenuItem = new JMenuItem();
        saveMenuItem = new JMenuItem();
        closeMenuItem = new JMenuItem();
        printMenuItem = new JMenuItem();
        fileMenuQuitItem = new JMenuItem();
        helpMenu = new JMenu();
        aboutMenuItem = new JMenuItem();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setBackground(new Color(255, 255, 255));
        setMinimumSize(new Dimension(800, 600));

        tabPanel.setName("tabPanel"); // NOI18N
        tabPanel.setPreferredSize(new Dimension(600, 512));
        getContentPane().add(tabPanel, BorderLayout.CENTER);

        statusPanel.setName("statusPanel"); // NOI18N
        statusPanel.setLayout(new BorderLayout());

        statusIcon.setHorizontalAlignment(SwingConstants.CENTER);
        statusIcon.setIcon(new ImageIcon(getClass().getResource("/be/fedict/eidviewer/gui/resources/icons/state_noeidpresent.png"))); // NOI18N
        statusIcon.setMaximumSize(new Dimension(72, 72));
        statusIcon.setMinimumSize(new Dimension(72, 72));
        statusIcon.setName("statusIcon"); // NOI18N
        statusIcon.setPreferredSize(new Dimension(90, 72));
        statusPanel.add(statusIcon, BorderLayout.EAST);

        statusText.setHorizontalAlignment(SwingConstants.RIGHT);
        statusText.setName("statusText"); // NOI18N
        statusPanel.add(statusText, BorderLayout.CENTER);

        printPanel.setMinimumSize(new Dimension(72, 72));
        printPanel.setName("printPanel"); // NOI18N
        printPanel.setPreferredSize(new Dimension(72, 72));
        printPanel.setLayout(new GridBagLayout());

        printButton.setAction(printAction); // NOI18N
        printButton.setHideActionText(true);
        printButton.setMaximumSize(new Dimension(200, 50));
        printButton.setMinimumSize(new Dimension(50, 50));
        printButton.setName("printButton"); // NOI18N
        printButton.setPreferredSize(new Dimension(200, 50));
        printButton.setIcon(new ImageIcon(getClass().getResource("/be/fedict/eidviewer/gui/resources/icons/print.png"))); // NOI18N
        printPanel.add(printButton, new GridBagConstraints());

        statusPanel.add(printPanel, BorderLayout.WEST);

        getContentPane().add(statusPanel, BorderLayout.SOUTH);

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(bundle.getString("fileMenuTitle")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        openMenuItem.setAction(openAction); // NOI18N
        openMenuItem.setName("jMenuItem2"); // NOI18N
        openMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Event.CTRL_MASK));
        fileMenu.add(openMenuItem);

        saveMenuItem.setAction(saveAction); // NOI18N
        saveMenuItem.setName("jMenuItem3"); // NOI18N
        saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK));
        fileMenu.add(saveMenuItem);

        closeMenuItem.setAction(closeAction); // NOI18N
        closeMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, Event.CTRL_MASK));
        closeMenuItem.setName("jMenuItem4"); // NOI18N
        fileMenu.add(closeMenuItem);

        fileMenu.addSeparator();

        printMenuItem.setAction(printAction); // NOI18N
        printMenuItem.setText(bundle.getString("fileMenuPrintItem")); // NOI18N
        printMenuItem.setName("jMenuItem1"); // NOI18N
        printMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, Event.CTRL_MASK));
        fileMenu.add(printMenuItem);

        fileMenu.addSeparator();

        quitAction.setEnabled(true);
        fileMenuQuitItem.setAction(quitAction); // NOI18N
        fileMenuQuitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Event.CTRL_MASK));
        fileMenuQuitItem.setText(bundle.getString("fileMenuQuitItem")); // NOI18N
        fileMenuQuitItem.setName("fileMenuQuitItem"); // NOI18N
        fileMenu.add(fileMenuQuitItem);

        menuBar.add(fileMenu);

        helpMenu.setAction(aboutAction); // NOI18N
        helpMenu.setText(bundle.getString("helpMenuTitle")); // NOI18N
        helpMenu.setName("jMenu1"); // NOI18N

        aboutMenuItem.setAction(aboutAction); // NOI18N
        aboutMenuItem.setText(bundle.getString("about.Action.text")); // NOI18N
        aboutMenuItem.setName("jMenuItem5"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        pack();
    }

    private void initPanels()
    {
        identityPanel = new IdentityPanel();
        cardPanel = new CardPanel();
        certificatesPanel = new CertificatesPanel();
        preferencesPanel = new PreferencesPanel();
        tabPanel.add(identityPanel, bundle.getString("IDENTITY"));
        tabPanel.add(cardPanel, bundle.getString("CARD"));
        tabPanel.add(certificatesPanel, bundle.getString("CERTIFICATES"));
        tabPanel.add(preferencesPanel, bundle.getString("PREFERENCES"));

        if (ViewerPrefs.getShowLogTab())
        {
            showLog(true);
        }
    }

    private void initIcons()
    {
        cardStatusIcons = new EnumMap<EidController.STATE, ImageIcon>(EidController.STATE.class);
        cardStatusIcons.put(EidController.STATE.NO_READERS, ImageUtilities.getIcon(this.getClass(), ICONS + EidController.STATE.NO_READERS + EXTENSION_PNG));
        cardStatusIcons.put(EidController.STATE.ERROR, ImageUtilities.getIcon(this.getClass(), ICONS + EidController.STATE.ERROR + EXTENSION_PNG));
        cardStatusIcons.put(EidController.STATE.NO_EID_PRESENT, ImageUtilities.getIcon(this.getClass(), ICONS + EidController.STATE.NO_EID_PRESENT + EXTENSION_PNG));
        cardStatusIcons.put(EidController.STATE.EID_PRESENT, ImageUtilities.getIcon(this.getClass(), ICONS + EidController.STATE.EID_PRESENT + EXTENSION_PNG));
        cardStatusIcons.put(EidController.STATE.FILE_LOADING, ImageUtilities.getIcon(this.getClass(), ICONS + EidController.STATE.FILE_LOADING + EXTENSION_PNG));
        cardStatusIcons.put(EidController.STATE.FILE_LOADED, ImageUtilities.getIcon(this.getClass(), ICONS + EidController.STATE.FILE_LOADED + EXTENSION_PNG));
        cardStatusIcons.put(EidController.STATE.EID_YIELDED, ImageUtilities.getIcon(this.getClass(), ICONS + EidController.STATE.EID_YIELDED + EXTENSION_PNG));
    }

    private void initTexts()
    {
        cardStatusTexts = new EnumMap<EidController.STATE, String>(EidController.STATE.class);
        cardStatusTexts.put(EidController.STATE.NO_READERS, bundle.getString(EidController.STATE.NO_READERS.toString()));
        cardStatusTexts.put(EidController.STATE.ERROR, bundle.getString(EidController.STATE.ERROR.toString()));
        cardStatusTexts.put(EidController.STATE.NO_EID_PRESENT, bundle.getString(EidController.STATE.NO_EID_PRESENT.toString()));
        cardStatusTexts.put(EidController.STATE.FILE_LOADING, bundle.getString(EidController.STATE.FILE_LOADING.toString()));
        cardStatusTexts.put(EidController.STATE.FILE_LOADED, bundle.getString(EidController.STATE.FILE_LOADED.toString()));
        cardStatusTexts.put(EidController.STATE.EID_YIELDED, bundle.getString(EidController.STATE.EID_YIELDED.toString()));
        activityTexts = new EnumMap<EidController.ACTIVITY, String>(EidController.ACTIVITY.class);
        activityTexts.put(EidController.ACTIVITY.IDLE, bundle.getString(EidController.ACTIVITY.IDLE.toString()));
        activityTexts.put(EidController.ACTIVITY.READING_IDENTITY, bundle.getString(EidController.ACTIVITY.READING_IDENTITY.toString()));
        activityTexts.put(EidController.ACTIVITY.READING_ADDRESS, bundle.getString(EidController.ACTIVITY.READING_ADDRESS.toString()));
        activityTexts.put(EidController.ACTIVITY.READING_PHOTO, bundle.getString(EidController.ACTIVITY.READING_PHOTO.toString()));
        activityTexts.put(EidController.ACTIVITY.READING_RRN_CHAIN, bundle.getString(EidController.ACTIVITY.READING_RRN_CHAIN.toString()));
        activityTexts.put(EidController.ACTIVITY.READING_AUTH_CHAIN, bundle.getString(EidController.ACTIVITY.READING_AUTH_CHAIN.toString()));
        activityTexts.put(EidController.ACTIVITY.READING_SIGN_CHAIN, bundle.getString(EidController.ACTIVITY.READING_SIGN_CHAIN.toString()));
        activityTexts.put(EidController.ACTIVITY.VALIDATING_IDENTITY, bundle.getString(EidController.ACTIVITY.VALIDATING_IDENTITY.toString()));
        activityTexts.put(EidController.ACTIVITY.VALIDATING_ADDRESS, bundle.getString(EidController.ACTIVITY.VALIDATING_ADDRESS.toString()));
    }

    private class AboutAction extends AbstractAction
    {
        public AboutAction(String text)
        {
            super(text);
        }
        
        public void actionPerformed(ActionEvent ae)
        {
            final JDialog dialog = new JDialog(BelgianEidViewer.this, bundle.getString("about.Action.text"), true);
            dialog.getContentPane().setLayout(new BorderLayout());
            dialog.getContentPane().add(BorderLayout.CENTER, new AboutPanel().setDialog(dialog));
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setLocationRelativeTo(null);
            dialog.setSize(800, 512);
            dialog.setVisible(true);
        }
    }

    private class PrintAction extends AbstractAction
    {
        public PrintAction(String text, Integer mnemonic)
        {
            super(text);
            putValue(MNEMONIC_KEY, mnemonic);   
        }
        
        public void actionPerformed(ActionEvent ae)
        {
            logger.fine("print action chosen..");
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setJobName(eidController.getIdentity().getNationalNumber());

            IDPrintout printout = new IDPrintout();
            printout.setIdentity(eidController.getIdentity());
            printout.setAddress(eidController.getAddress());

            try
            {
                printout.setPhoto(eidController.getPhotoImage());
            }
            catch (IOException ex)
            {
                logger.log(Level.SEVERE, "Photo conversion from JPEG Failed", ex);
            }

            job.setPrintable(printout);

            boolean ok = job.printDialog();


            if (ok)
            {
                try
                {
                    logger.finest("starting print job..");
                    job.print();
                    logger.finest("print job completed.");
                }
                catch (PrinterException pex)
                {
                    logger.log(Level.SEVERE, "Print Job Failed", pex);
                }
            }
        }
    }

    private class QuitAction extends AbstractAction
    {
        public QuitAction(String text, Integer mnemonic)
        {
            super(text);
            putValue(MNEMONIC_KEY, mnemonic);
            
        }
        
        public void actionPerformed(ActionEvent ae)
        {
            logger.fine("quit action chosen..");
            BelgianEidViewer.this.stop();
        }
    }

    private class OpenFileAction extends AbstractAction
    {
        public OpenFileAction(String text, Integer mnemonic)
        {
            super(text);
            putValue(MNEMONIC_KEY, mnemonic);   
        }
        
        public void actionPerformed(ActionEvent ae)
        {
            logger.fine("Open action chosen..");
            final JFileChooser fileChooser = new JFileChooser();

            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            fileChooser.setAcceptAllFileFilterUsed(false);

            fileChooser.addChoosableFileFilter(new EidFileFilter(true, true, true, bundle.getString("allEIDFiles")));
            fileChooser.addChoosableFileFilter(new EidFileFilter(true, false, false, bundle.getString("xmlEIDFiles")));
            fileChooser.addChoosableFileFilter(new EidFileFilter(false, false, true, bundle.getString("csvEIDFiles")));
            fileChooser.addChoosableFileFilter(new EidFileFilter(false, true, false, bundle.getString("tlvEIDFiles")));

            fileChooser.setFileView(new EidFileView(bundle));

            EidFilePreviewAccessory preview = new EidFilePreviewAccessory(bundle);
            fileChooser.setAccessory(preview);
            fileChooser.addPropertyChangeListener(preview);

            if (fileChooser.showOpenDialog(BelgianEidViewer.this) == JFileChooser.APPROVE_OPTION)
            {
                File file = fileChooser.getSelectedFile();
                if (file.isFile())
                {
                    eidController.loadFromFile(file);
                }
            }
        }
    }

    private class SaveFileAction extends AbstractAction
    {
        public SaveFileAction(String text, Integer mnemonic)
        {
            super(text);
            putValue(MNEMONIC_KEY, mnemonic);   
        }
        
        public void actionPerformed(ActionEvent ae)
        {
            logger.fine("Save action chosen..");
            final JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showSaveDialog(BelgianEidViewer.this) == JFileChooser.APPROVE_OPTION)
            {
                eidController.saveToXMLFile(fileChooser.getSelectedFile());
            }
        }
    }

    private class CloseFileAction extends AbstractAction
    {
        public CloseFileAction(String text, Integer mnemonic)
        {
            super(text);
            putValue(MNEMONIC_KEY, mnemonic);   
        }
        
        public void actionPerformed(ActionEvent ae)
        {
            logger.fine("Close action chosen..");
            eidController.closeFile();
        }
    }

    /* ------------------------ Interaction meant for Applets ---------------------------------------------------------------- */
    public void addDetailMessage(String detailMessage)
    {
        logger.finest(detailMessage);
    }

    public void setStatusMessage(Status status, MESSAGE_ID messageId)
    {
        String message = coreMessages.getMessage(messageId);
        logger.info(message);
    }

    public boolean privacyQuestion(boolean includeAddress, boolean includePhoto, String identityDataUsage)
    {
        // this app's only purpose being to read eID cards.. asking "are you sure" is merely annoying to the user
        // and gives no extra security whatsoever
        // (the privacyQuestion was designed for Applets and Middleware, where it makes a *lot* of sense)
        return true;
    }

    public Component getParentComponent()
    {
        return this;
    }

    /* ------------------------Unused from Applet Core ---------------------------------------------------------------- */
    public void addTestResult(DiagnosticTests diagnosticTest, boolean success, String description)
    {
    }

    public void resetProgress(int max)
    {
    }

    public void increaseProgress()
    {
    }

    private void setProgress(final int progress)
    {
    }

    public void setProgressIndeterminate()
    {
    }

    /* ---------------------------------------------------------------------------------------- */
    public static void main(String args[])
    {
        try
        {
            logger.finest("Setting System Look And Feel");
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception e)
        {
            logger.log(Level.WARNING, "Can't Set SystemLookAndFeel", e);
        }

        new BelgianEidViewer().start();
    }

    public void showLog(boolean show)
    {
        if (show)
        {
            if (logPanel == null)
            {
                logPanel = new LogPanel();
                logPanel.start();
                tabPanel.add(logPanel, bundle.getString("LOG"));
                tabPanel.insertTab(bundle.getString("LOG"), null, logPanel, bundle.getString("LOG"), 5);
            }
        }
        else
        {
            if (logPanel != null)
            {
                logPanel.stop();
                tabPanel.remove(logPanel);
                logPanel = null;
            }
        }
    }
}
