package com.mijibox.ppworkshop;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;

import org.jdesktop.jxlayer.JXLayer;

public class PPWorkshop {

	public final static String COMPANY_NAME = "MIJI Technology LLC";
	public final static String COPYRIGHT_STATEMENT = "\u00a9 2019 " + COMPANY_NAME + ".  All Rights Reserved.";
	public final static String APP_NAME = "Passport Photo Workshop";
	public final static String PREF_LICENSE_AGREEMENT = "LicenseAgreement";
	public final static String PREF_LICENSE_AGREED_TIME = "LicenseAgreedTime";

	private JFrame mainWindow;
	private JButton btnLoad;
	private JButton btnSave;
	private AboutPPWorkshop aboutDialog;

	public PPWorkshop() {

		ClassLoader classLoader = PPWorkshop.class.getClassLoader();
		ImageIcon imageIcon = new ImageIcon(classLoader.getResource("PPWorkshopIcon.png"));

		this.mainWindow = new JFrame(APP_NAME);
		this.mainWindow.setIconImage(imageIcon.getImage());
		this.mainWindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.mainWindow.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				// show warning...
				System.exit(0);
			}
		});

		final PPViewer ppViewer = new PPViewer();

		JPanel toolbar = new JPanel();
		toolbar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));

		this.btnLoad = new JButton("Load...");
		btnLoad.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (ppViewer.loadPicture()) {
					ppViewer.getZoomSliderPanel().setEnabled(true);
					ppViewer.getRotateSliderPanel().setEnabled(true);
					btnSave.setEnabled(true);
				}
			}
		});

		final JPopupMenu savePopup = new JPopupMenu();
		JMenuItem saveSingle = new JMenuItem("Save single photo...");
		saveSingle.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ppViewer.savePicture(null, false);
			}
		});

		JMenuItem save6x4 = new JMenuItem("Save 6x4 photo sheet...");
		save6x4.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ppViewer.savePicture(null, true);
			}
		});

		savePopup.add(saveSingle);
		savePopup.add(save6x4);

		this.btnSave = new JButton("Save...");
		btnSave.setEnabled(false);
		btnSave.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				savePopup.show(btnSave, 0, btnSave.getHeight() + 1);
			}
		});

		final JCheckBox cbGuideLines = new JCheckBox("Guide Lines");
		cbGuideLines.setSelected(true);
		cbGuideLines.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ppViewer.showGuideLines(cbGuideLines.isSelected());
			}
		});

		toolbar.add(btnLoad);
		toolbar.add(Box.createHorizontalStrut(5));
		toolbar.add(btnSave);
		toolbar.add(Box.createHorizontalStrut(10));
		toolbar.add(cbGuideLines);
		toolbar.add(Box.createHorizontalGlue());

		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));

		bottomPanel.add(new JLabel(COPYRIGHT_STATEMENT));
		bottomPanel.add(Box.createHorizontalGlue());
//		bottomPanel.add(new UpdateIndicatorVersionLabel(APP_NAME, this.appUpdater, this.getShowAboutDialogAction()));

		JXLayer centerPanel = new JXLayer(ppViewer);
		centerPanel.setGlassPane(ppViewer.getControlOverlay());

		this.mainWindow.getContentPane().add(toolbar, BorderLayout.NORTH);
		this.mainWindow.getContentPane().add(centerPanel, BorderLayout.CENTER);
		this.mainWindow.getContentPane().add(bottomPanel, BorderLayout.SOUTH);

		this.mainWindow.pack();
		this.mainWindow.setLocationRelativeTo(null);
		this.mainWindow.setVisible(true);

	}

	public JFrame getMainWindow() {
		return this.mainWindow;
	}

	private AbstractAction getShowAboutDialogAction() {
		AbstractAction action = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (aboutDialog == null) {
					aboutDialog = new AboutPPWorkshop(PPWorkshop.this);
				}
				aboutDialog.setLocationRelativeTo(PPWorkshop.this.getMainWindow());
				aboutDialog.setVisible(true);
			}
		};

		return action;
	}

	public static String getAppVersion() {
		String version = "Development";
		try {
			InputStream is = PPWorkshop.class.getResourceAsStream("/version.properties");
			Properties props = new Properties();
			props.load(is);

			String strVersion = props.getProperty("app.version");
			if (strVersion != null && !strVersion.equals("${project.version}")) {
				version = strVersion;
			}
		} catch (Exception ex) {

		}
		return version;
	}

	public static JPanel getLicenseContentPanel() {
		JPanel pnl = new JPanel(new BorderLayout());
		pnl.setBorder(BorderFactory.createTitledBorder(APP_NAME + " - License Agreement"));
		JTextPane textPane = new JTextPane();
		textPane.setEditable(false);
		try {
			InputStream licenseFileInputStream = PPWorkshop.class.getClassLoader().getResourceAsStream("license.txt");
			textPane.read(licenseFileInputStream, null);
		} catch (NullPointerException npe) {

		} catch (IOException e) {
			e.printStackTrace();
		}
		pnl.add(new JScrollPane(textPane), BorderLayout.CENTER);
		return pnl;
	}

	public void saveAutoUpdateCheckPreference(boolean enabled) {
		Preferences appRootNode = Preferences.userNodeForPackage(PPWorkshop.class);
		try {
			appRootNode.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
//					UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
//					JFrame.setDefaultLookAndFeelDecorated(true);
//					JDialog.setDefaultLookAndFeelDecorated(true);
					new PPWorkshop();
				} catch (Exception e1) {
					e1.printStackTrace();
				}

			}
		});
	}
}
