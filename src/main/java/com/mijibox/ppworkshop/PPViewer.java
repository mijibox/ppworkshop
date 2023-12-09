package com.mijibox.ppworkshop;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.swing.Box;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

public class PPViewer extends JPanel {

	public final static int VIEWPORT_SIZE = 400;

	private final static int ROTATE_STEPS = 720;
	private final static int SCALE_STEPS = 1000;
	private final static String PREF_LOAD_DIR = "lastOpenDir";
	private final static String PREF_SAVE_DIR = "lastSaveDir";

	private BufferedImage paintingImage;
	private Double scale;
	private double rotate;
	private Integer imageCenterX;
	private Integer imageCenterY;
	private int draggedX;
	private int draggedY;

	private int outputDPI = 300;
	private int outputSize = 2; //2 inch

	private boolean showAssistantLines = true;
	private JSlider zoomSlider;
	private JSlider rotateSlider;
	private JPanel zoomPnl;
	private JPanel rotatePnl;
	private boolean dirty;
	private JPanel pnlControlOverlay;

	public PPViewer() {
		this.setBackground(Color.black);

		MouseAdapter mouseAdapter = new MouseAdapter() {
			private int pressedX;
			private int pressedY;

			@Override
			public void mousePressed(MouseEvent e) {
				if (scale != null) {
					this.pressedX = (int) (e.getX() / scale);
					this.pressedY = (int) (e.getY() / scale);
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (paintingImage != null) {

					try {
						AffineTransform xform = getTransformer(VIEWPORT_SIZE, scale);
						Point2D newCenterTranslated;
						newCenterTranslated = xform.inverseTransform(new Point2D.Double(((double)VIEWPORT_SIZE)/2, ((double)VIEWPORT_SIZE)/2), null);
						imageCenterX = (int) Math.round(newCenterTranslated.getX());
						imageCenterY = (int) Math.round(newCenterTranslated.getY());

						draggedX = 0;
						draggedY = 0;
						PPViewer.this.repaint();
					}
					catch (NoninvertibleTransformException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

				}
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				if (imageCenterX != null && imageCenterY != null) {
					draggedX = (int) (e.getX() / scale - pressedX);
					draggedY = (int) (e.getY() / scale - pressedY);

					PPViewer.this.repaint();
					PPViewer.this.dirty = true;
				}
			}

			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
//				int units = e.getUnitsToScroll();
//
//				if (scale != null) {
//					double newScale = scale + 0.001 * e.getWheelRotation();
//
//					PPViewer.this.setScale(newScale);
//
//					PPViewer.this.repaint();
//				}
			}
		};

		this.addMouseListener(mouseAdapter);
		this.addMouseMotionListener(mouseAdapter);
//		this.addMouseWheelListener(mouseAdapter);

		this.setPreferredSize(new Dimension(VIEWPORT_SIZE + 150, VIEWPORT_SIZE + 100));
	}

	public void setScale(double scale) {
		this.setScale(scale, true);
	}

	private void setScale(double scale, boolean updateSlider) {
		this.scale = scale;
		if (updateSlider) {
			int sliderValue = (int) (this.scale * SCALE_STEPS);
			if (this.zoomSlider.getValue() != sliderValue) {
				this.zoomSlider.setValue(sliderValue);
			}
		}
		this.repaint();
		this.dirty = true;
	}

	public void setRotate(double rotate) {
		this.setRotate(rotate, true);
	}

	private void setRotate(double rotate, boolean updateSlider) {
		this.rotate = rotate;

		if (updateSlider) {
			int sliderValue = (int) Math.round(rotate/Math.PI * (ROTATE_STEPS/2) + (ROTATE_STEPS/2));

			if (this.rotateSlider.getValue() != sliderValue) {
				this.rotateSlider.setValue(sliderValue);
			}
		}
		this.repaint();
		this.dirty = true;
	}

	private void write4x6Image(File imageFile) {
		int outputPictureSize = this.outputSize * this.outputDPI;
		int xCount = 3;
		int yCount = 2;
		int imageWidth = outputPictureSize * xCount;
		int imageHeight = outputPictureSize * yCount;

		BufferedImage bi = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g2 = bi.createGraphics();

		//convert viewing scale to saving scale.
		double outputScale = this.scale * outputPictureSize / VIEWPORT_SIZE;

		for (int i=0; i<xCount; i++) {
			for (int j=0; j<yCount; j++) {
				
				int picX = outputPictureSize * i;
				int picY = outputPictureSize * j;
				g2.setClip(null);
				g2.translate(picX, picY);
				if (j % 2 == 0) {
					g2.rotate(Math.PI, outputPictureSize/2, outputPictureSize/2);
				}
				g2.clipRect(0, 0, outputPictureSize, outputPictureSize);
				this.paintImage(g2, outputPictureSize, outputScale, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
				g2.setColor(Color.BLACK);
				if (j % 2 == 0) {
					g2.rotate(-Math.PI, outputPictureSize/2, outputPictureSize/2);
				}
				g2.translate(outputPictureSize * -i, outputPictureSize * -j);
			}
		}

		g2.setClip(null);
		g2.setColor(Color.BLACK);
		//vertical lines
		for (int i=1; i<xCount; i++) {
			g2.drawLine(i*outputPictureSize, 0, i*outputPictureSize, imageHeight);
		}

		//horizontal lines
		for (int i=1; i<yCount; i++) {
			g2.drawLine(0, i*outputPictureSize, imageWidth, i*outputPictureSize);
		}
		
		BufferedImage borderedImage = this.addBorder(bi, 1.03f);

		this.writeBufferedImage(borderedImage, imageFile);
	}
	
	private BufferedImage addBorder(BufferedImage originalImage, float percentage) {
		
		int newImageWidth = Math.round(originalImage.getWidth() * percentage);
		int newImageHeight = Math.round(originalImage.getHeight() * percentage);
		
		int x = (newImageWidth - originalImage.getWidth()) / 2;
		int y = (newImageHeight - originalImage.getHeight()) / 2;
		
		BufferedImage bi = new BufferedImage(newImageWidth, newImageHeight, BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g2 = bi.createGraphics();
		g2.setColor(Color.WHITE);
		g2.fillRect(0, 0, newImageWidth, newImageHeight);
		g2.drawImage(originalImage, null, x, y);
		
		return bi;
	}
	

	private void writeBufferedImage(BufferedImage bi, File imageFile) {
		try {
			Iterator<ImageWriter> i = ImageIO.getImageWritersByFormatName("jpeg");

			// Just get the first JPEG writer available
			ImageWriter jpegWriter = i.next();

			// Set the compression quality to 0.97f
			ImageWriteParam param = jpegWriter.getDefaultWriteParam();
			param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			param.setCompressionQuality(0.97f);

			// Write the image to a file
			FileImageOutputStream out = new FileImageOutputStream(imageFile);
			jpegWriter.setOutput(out);
			jpegWriter.write(null, new IIOImage(bi, null, null), param);
			jpegWriter.dispose();
			out.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeImageFile(File imageFile) {
		int outputPictureSize = this.outputSize * this.outputDPI;
		BufferedImage bi = new BufferedImage(outputPictureSize, outputPictureSize, BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g2 = bi.createGraphics();

		//convert viewing scale to saving scale.
		double outputScale = this.scale * outputPictureSize / VIEWPORT_SIZE;

		this.paintImage(g2, outputPictureSize, outputScale, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

		this.writeBufferedImage(bi, imageFile);
	}

	public void savePicture(File f, boolean save6x4) {
//		System.out.println("imageCenterX=" + this.imageCenterX + ", imageCenterY=" + imageCenterY + ", scale=" + this.zoomSlider.getValue() + ", rotate=" + this.rotateSlider.getValue());
		JFileChooser chooser = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter("JPEG Images", "jpg");
		chooser.setFileFilter(filter);

		try {
			if (f == null) {
				String defaultFileName = save6x4 ? "passport_photo_sheet.jpg" : "passport_photo.jpg";
				f = new File(new File(this.getLastSaveDir()), defaultFileName);
			}
			chooser.setSelectedFile(f);
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}

		int returnVal = chooser.showSaveDialog(this);

		if (JFileChooser.APPROVE_OPTION == returnVal) {
			File selectedFile = chooser.getSelectedFile();
			if (!selectedFile.getName().toLowerCase().endsWith(".jpg")) {
				try {
					selectedFile = new File(selectedFile.getCanonicalPath() + ".jpg");
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (selectedFile.exists()) {
				int rv = JOptionPane.showConfirmDialog(this, "\"" + selectedFile.getName() + "\" already exists,\n Do you want to replace it?",
							"Confirm Save", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

				if (JOptionPane.YES_OPTION == rv) {
					if (save6x4) {
						this.write4x6Image(selectedFile);
					}
					else {
						this.writeImageFile(selectedFile);
					}
				}
				else {
					savePicture(selectedFile, save6x4);
				}
			}
			else {
				if (save6x4) {
					this.write4x6Image(selectedFile);
				}
				else {
					this.writeImageFile(selectedFile);
				}
			}
			try {
				this.saveLastSaveDir(selectedFile.getParentFile().getCanonicalPath());
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void showGuideLines(boolean show) {
		this.showAssistantLines = show;
		this.repaint();
	}

	public JPanel getZoomSliderPanel() {


		if (this.zoomPnl == null) {
			final JTextField zoomLabel = new JTextField("100.00%");
			zoomLabel.setOpaque(false);

			this.zoomSlider = new JSlider(SwingConstants.VERTICAL, 1, SCALE_STEPS, SCALE_STEPS);
			zoomSlider.setOpaque(false);
			zoomSlider.setInverted(true);
			zoomSlider.addChangeListener(new ChangeListener() {

				public void stateChanged(ChangeEvent e) {
					JSlider source = (JSlider) e.getSource();
					int value = source.getValue();

					double scale = (double)value / SCALE_STEPS;

					PPViewer.this.setScale(scale, false);
					zoomLabel.setText(String.format("%1.2f%%", scale * 100));
				}

			});

			this.zoomPnl = new JPanel(new BorderLayout()) {
				@Override
				public void setEnabled(boolean enable) {
					zoomSlider.setEnabled(enable);
				}
			};
			zoomPnl.setOpaque(false);
			zoomPnl.add(new JLabel("Zoom"), BorderLayout.NORTH);
			zoomPnl.add(zoomSlider, BorderLayout.CENTER);
//			zoomPnl.add(zoomLabel, BorderLayout.SOUTH);

			zoomPnl.setEnabled(false);
		}

		return zoomPnl;
	}

	public JPanel getRotateSliderPanel() {


		if (this.rotatePnl == null) {

			this.rotateSlider = new JSlider(SwingConstants.HORIZONTAL, 0, ROTATE_STEPS, ROTATE_STEPS/2);
			rotateSlider.setOpaque(false);
			rotateSlider.addChangeListener(new ChangeListener() {

				public void stateChanged(ChangeEvent e) {
					JSlider source = (JSlider) e.getSource();
					int value = source.getValue();

					double rotate = Math.PI * (value - (ROTATE_STEPS/2))/(ROTATE_STEPS/2);

					PPViewer.this.setRotate(rotate, false);
				}

			});

			this.rotatePnl = new JPanel(new BorderLayout()){
				@Override
				public void setEnabled(boolean enable) {
					rotateSlider.setEnabled(enable);
				}
			};
			rotatePnl.setOpaque(false);
			JLabel rotateLabel = new JLabel("Rotate");
			rotatePnl.add(rotateLabel, BorderLayout.WEST);
			rotatePnl.add(rotateSlider, BorderLayout.CENTER);
			rotatePnl.add(Box.createRigidArea(rotateLabel.getPreferredSize()), BorderLayout.EAST);
			rotatePnl.setEnabled(false);
		}

		return this.rotatePnl;
	}

	private AffineTransform getTransformer(int outputSize, double outputScale) {

		double viewCenterX = ((double)outputSize) / 2;
		double viewCenterY = ((double)outputSize) / 2;

		double widthToCenterOfImageInViewPort = this.imageCenterX * outputScale - viewCenterX - draggedX * outputScale;
		double heightToCenterOfImageInViewPort = this.imageCenterY * outputScale - viewCenterY - draggedY * outputScale;

		AffineTransform xform = AffineTransform.getTranslateInstance(-widthToCenterOfImageInViewPort, -heightToCenterOfImageInViewPort);
		xform.rotate(this.rotate, this.imageCenterX * outputScale, this.imageCenterY * outputScale);
		xform.scale(outputScale, outputScale);
		return xform;
	}

	private void paintImage(Graphics2D g2, int outputSize, double outputScale, Object interpolation) {
		if (this.paintingImage != null) {

/*
			int imgWidth = (int)(paintingImage.getWidth()*outputScale);
			int imgHeight = (int)(paintingImage.getHeight()*outputScale);

			int imgX = (int) (outputSize / 2 - this.imageCenterX * outputScale + this.draggedX * outputScale);
			int imgY = (int) (outputSize / 2 - this.imageCenterY * outputScale + this.draggedY * outputScale);

			AffineTransform xform = AffineTransform.getTranslateInstance(imgX, imgY);
			xform.scale(outputScale, outputScale);

			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation);
			g2.drawImage(this.paintingImage, xform, null);
 */


			AffineTransform xform = this.getTransformer(outputSize, outputScale);

			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation);
			g2.drawImage(this.paintingImage, xform, null);
		}
	}

	public boolean loadPicture() {
		boolean loaded = false;
		JFileChooser chooser = new JFileChooser(this.getLastOpenDir());
		FileNameExtensionFilter filter = new FileNameExtensionFilter("JPEG Images", "jpg");
		chooser.setFileFilter(filter);
		int returnVal = chooser.showOpenDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			try {
				final File selectedFile = chooser.getSelectedFile();
				BufferedImage img = ImageIO.read(selectedFile);
				if (img != null) {
					this.scale = null;
					this.setRotate(0, true);
					this.imageCenterX = null;
					this.imageCenterY = null;
					this.paintingImage = img;
					this.repaint();
					loaded = true;
					this.dirty = false;
					this.saveLastOpenDir(selectedFile.getAbsoluteFile().getParentFile().getCanonicalPath());
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}

		return loaded;
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		int w = this.getWidth();
		int h = this.getHeight();

		int boxX = (w - VIEWPORT_SIZE) / 2;
		int boxY = (h - VIEWPORT_SIZE) / 2;
		int boxCenterX = w / 2;
		int boxCenterY = h / 2;

		Graphics2D g2 = (Graphics2D) g.create();

		if (this.paintingImage != null) {
			if (this.scale == null) { //first time load the image, scale to fit viewport
				double scaleX = 1;
				double scaleY = 1;

				scaleX = (double)VIEWPORT_SIZE / (double)this.paintingImage.getWidth();
				scaleY = (double)VIEWPORT_SIZE / (double)this.paintingImage.getHeight();

				this.setScale(Math.max(scaleX, scaleY), true);
				this.zoomSlider.setMinimum((int) (Math.max(scaleX, scaleY) * SCALE_STEPS));
			}

			if (this.imageCenterX == null || this.imageCenterY == null) {
				this.imageCenterX = this.paintingImage.getWidth() / 2;
				this.imageCenterY = this.paintingImage.getHeight() / 2;
			}

			g2.translate(boxX, boxY);
			this.paintImage(g2, VIEWPORT_SIZE, this.scale, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2.translate(-boxX, -boxY);
		}


		g2.setColor(Color.DARK_GRAY);
		g2.drawRect(boxX, boxY, VIEWPORT_SIZE, VIEWPORT_SIZE);

		Area darkenArea = new Area(new Rectangle2D.Double(0, 0, w, h));
		darkenArea.subtract(new Area(new Rectangle2D.Double(boxX, boxY, VIEWPORT_SIZE, VIEWPORT_SIZE)));

		AlphaComposite  ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f);
		g2.setComposite(ac);

		g2.fill(darkenArea);

		if (this.showAssistantLines) {
			float dash1[] = { 10.0f, 5.0f };
			BasicStroke dashed = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash1, 0.0f);

			g2.setStroke(dashed);


			//vertical center line
			g2.drawLine(boxCenterX, boxY, boxCenterX, boxY + VIEWPORT_SIZE);

			//horizontal center line
//			g2.drawLine(boxX, boxCenterY, boxX + VIEWPORT_SIZE, boxCenterY);

			//eye
			int eyeUpper = (int) (boxY + VIEWPORT_SIZE * 0.3125f);
			int eveLower = (int) (boxY + VIEWPORT_SIZE * 0.4375f);
			g2.drawRect(boxX, eyeUpper, VIEWPORT_SIZE, eveLower - eyeUpper);

			//center

			g2.drawLine(boxCenterX - 5, boxCenterY, boxCenterX + 5, boxCenterY);


			//green ovals
			g2.setStroke(new BasicStroke(4));
			g2.setColor(new Color(107, 255, 99));
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			//outter eclipse
			int outterEclipseX = boxX + (int) (VIEWPORT_SIZE * 0.25f);
			int outterEclipseY = boxY + (int) (VIEWPORT_SIZE * 0.0625f);
			int outterEclipseHeight = (int) (VIEWPORT_SIZE * 0.6875f);
			int outterEclipseWidth = (int) (VIEWPORT_SIZE * 0.5f);

			g2.draw(new Ellipse2D.Double(outterEclipseX, outterEclipseY, outterEclipseWidth, outterEclipseHeight));

			//inner eclipse
			int innerEclipseX = outterEclipseX + (int) (VIEWPORT_SIZE * 0.0625f);
			int innerEclipseY = outterEclipseY + (int) (VIEWPORT_SIZE * 0.09375f);
			int innerEclipseHeight = (int) (VIEWPORT_SIZE * 0.5f); //1 inch
			int innerEclipseWidth = (int) (VIEWPORT_SIZE * 0.375f);

			g2.draw(new Ellipse2D.Double(innerEclipseX, innerEclipseY, innerEclipseWidth, innerEclipseHeight));
		}

		g2.dispose();
	}

	private String getLastOpenDir() {
		String defaultOpenDir = System.getProperty("user.home");
		Preferences appRootNode = Preferences.userNodeForPackage(PPWorkshop.class);
		String lastOpenDir = appRootNode.get(PREF_LOAD_DIR, defaultOpenDir);

		return lastOpenDir;
	}

	private void saveLastOpenDir(String openDir) {
		try {
			Preferences appRootNode = Preferences.userNodeForPackage(PPWorkshop.class);
			appRootNode.put(PREF_LOAD_DIR, openDir);
			appRootNode.flush();
		}
		catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}

	private String getLastSaveDir() {
		Preferences appRootNode = Preferences.userNodeForPackage(PPWorkshop.class);
		String lastSaveDir = appRootNode.get(PREF_SAVE_DIR, this.getLastOpenDir());
		return lastSaveDir;
	}

	private void saveLastSaveDir(String saveDir) {
		try {
			Preferences appRootNode = Preferences.userNodeForPackage(PPWorkshop.class);
			appRootNode.put(PREF_SAVE_DIR, saveDir);
			appRootNode.flush();
		}
		catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}

	public boolean isDirty() {
		return this.dirty;
	}

	@Override
	public Dimension getMinimumSize() {
		return this.getControlOverlay().getMinimumSize();
	}

	public JPanel getControlOverlay() {
		if (this.pnlControlOverlay == null) {
			this.pnlControlOverlay = new JPanel(new GridBagLayout());
			pnlControlOverlay.setOpaque(false);


			JPanel zoomPnl = this.getZoomSliderPanel();

			JPanel rotatePnl = this.getRotateSliderPanel();

			GridBagConstraints gbConst = new GridBagConstraints();
			gbConst.insets = new Insets(5, 5, 5, 5);

			//filler
			gbConst.weightx = 0;
			gbConst.weighty = 0.5;
			gbConst.gridx = 1;
			gbConst.gridy = 0;
			gbConst.fill = GridBagConstraints.BOTH;
			gbConst.anchor = GridBagConstraints.CENTER;
			pnlControlOverlay.add(Box.createRigidArea(rotatePnl.getPreferredSize()), gbConst);

			gbConst.gridy = 2;
			gbConst.fill = GridBagConstraints.HORIZONTAL;
			gbConst.anchor = GridBagConstraints.NORTH;
			pnlControlOverlay.add(rotatePnl, gbConst);


			//filler
			gbConst.weightx = 0.5;
			gbConst.weighty = 0;
			gbConst.gridx = 0;
			gbConst.gridy = 1;
			gbConst.anchor = GridBagConstraints.CENTER;
			gbConst.fill = GridBagConstraints.BOTH;
			pnlControlOverlay.add(Box.createRigidArea(zoomPnl.getPreferredSize()), gbConst);

			gbConst.gridx = 2;
			gbConst.anchor = GridBagConstraints.WEST;
			gbConst.fill = GridBagConstraints.VERTICAL;
			pnlControlOverlay.add(zoomPnl, gbConst);

			//center panel
			JPanel center = new JPanel(new BorderLayout());
			center.setMinimumSize(new Dimension(PPViewer.VIEWPORT_SIZE, PPViewer.VIEWPORT_SIZE));
			center.setPreferredSize(new Dimension(PPViewer.VIEWPORT_SIZE, PPViewer.VIEWPORT_SIZE));
			center.setOpaque(false);
//			center.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));

			gbConst.gridx = 1;
			gbConst.gridy = 1;
			gbConst.weightx = 0;
			gbConst.weighty = 0;
			pnlControlOverlay.add(center, gbConst);
		}

		return pnlControlOverlay;
	}

}
