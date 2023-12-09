package com.mijibox.ppworkshop;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;

public class AboutPPWorkshop extends JDialog {

	public final static String CONTENT = "<html>"
			+ PPWorkshop.APP_NAME + "<br/><br/>"
			+ "Version: " + PPWorkshop.getAppVersion() + "<br/>"
			+ PPWorkshop.COPYRIGHT_STATEMENT + "<br/><br/>"
			+ "Visit: <a href=\"http://www.sleepingdumpling.com\">http://www.sleepingdumpling.com</a><br/>"
			+ "E-mail: <a href=\"mailto:support@sleepingdumpling.com\">support@sleepingdumpling.com</a>"
			+ "</html>";
	
	private PPWorkshop ppworkshop;

	public AboutPPWorkshop(PPWorkshop ppworkshop) {
		super(ppworkshop.getMainWindow(), "About " + PPWorkshop.APP_NAME, false);
		this.ppworkshop = ppworkshop;
		this.init();
	}

	private void init() {
		this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

		//bottom panel
		JPanel pnlBottom = new JPanel();
		pnlBottom.setLayout(new BoxLayout(pnlBottom, BoxLayout.X_AXIS));
		
		JButton btnClose = new JButton("Close");
		btnClose.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				AboutPPWorkshop.this.setVisible(false);
			}

		});
		
		pnlBottom.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
		pnlBottom.add(Box.createHorizontalGlue());
		pnlBottom.add(btnClose);

		this.getContentPane().add(this.getContentPanel(), BorderLayout.CENTER);
		this.getContentPane().add(pnlBottom, BorderLayout.SOUTH);

		this.pack();
	}
	
	private JPanel getContentPanel() {
		JPanel pnl = new JPanel();
		pnl.setLayout(new BorderLayout());
		
		JTabbedPane tabbedPane = new JTabbedPane();

		
		JPanel licensePnl = PPWorkshop.getLicenseContentPanel();
		licensePnl.setPreferredSize(new Dimension(400, 360));

		tabbedPane.addTab("About", this.createAboutPanel());
		tabbedPane.addTab("License", licensePnl);
		
		pnl.add(tabbedPane, BorderLayout.CENTER);
		
		return pnl;
	}
	
	public JPanel createAboutPanel() {
		JPanel pnl = new JPanel(new BorderLayout());
		pnl.setBorder(BorderFactory.createTitledBorder("About " + PPWorkshop.APP_NAME));
		JEditorPane textPane = new JEditorPane();
		textPane.setEditable(false);
//		textPane.setOpaque(false);
		HTMLEditorKit editorKit = (HTMLEditorKit) JEditorPane.createEditorKitForContentType("text/html");
		editorKit.getStyleSheet().addRule("body {color:white; font-family:\"" + textPane.getFont().getName() + "\" }");
////		editorKit.getStyleSheet().addRule("body {color:white; font-family:\"tahoma\" }");
//		editorKit.getStyleSheet().addRule("a {color:#bbbbbb}");
		textPane.setEditorKit(editorKit);
		textPane.setText(CONTENT);
		textPane.addHyperlinkListener(new HyperlinkListener() {
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					if (Desktop.isDesktopSupported()) {
						try {
							Desktop.getDesktop().browse(e.getURL().toURI());
						}
						catch (IOException e1) {
							e1.printStackTrace();
						}
						catch (URISyntaxException e1) {
							e1.printStackTrace();
						}
					}
				}
			}
		});
		
		
		
		pnl.add(new JScrollPane(textPane), BorderLayout.CENTER);
		return pnl;
	}


}
