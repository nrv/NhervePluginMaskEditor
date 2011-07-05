/*
 * Copyright 2010, 2011 Institut Pasteur.
 * 
 * This file is part of Mask Editor, which is an ICY plugin.
 * 
 * Mask Editor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Mask Editor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Mask Editor. If not, see <http://www.gnu.org/licenses/>.
 */
package plugins.nherve.maskeditor;

import icy.canvas.IcyCanvas;
import icy.file.Saver;
import icy.gui.component.ComponentUtil;
import icy.gui.component.ImageComponent;
import icy.gui.frame.IcyFrame;
import icy.gui.main.MainEvent;
import icy.gui.util.GuiUtil;
import icy.gui.util.WindowPositionSaver;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.painter.Painter;
import icy.plugin.PluginDescriptor;
import icy.plugin.PluginLauncher;
import icy.plugin.PluginLoader;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.roi.ROI2DArea;
import icy.sequence.Sequence;
import icy.swimmingPool.SwimmingObject;
import icy.swimmingPool.SwimmingPool;
import icy.swimmingPool.SwimmingPoolEvent;
import icy.swimmingPool.SwimmingPoolListener;
import icy.system.thread.ThreadUtil;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.prefs.Preferences;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import loci.formats.FormatException;
import plugins.nherve.toolbox.NherveToolbox;
import plugins.nherve.toolbox.PersistenceException;
import plugins.nherve.toolbox.image.DifferentColorsMap;
import plugins.nherve.toolbox.image.mask.Mask;
import plugins.nherve.toolbox.image.mask.MaskException;
import plugins.nherve.toolbox.image.mask.MaskListener;
import plugins.nherve.toolbox.image.mask.MaskPersistence;
import plugins.nherve.toolbox.image.mask.MaskStack;
import plugins.nherve.toolbox.image.mask.OptimizedMaskPersistenceImpl;
import plugins.nherve.toolbox.image.segmentation.SegmentationComparison;
import plugins.nherve.toolbox.image.toolboxes.ColorSpaceTools;
import plugins.nherve.toolbox.image.toolboxes.SomeImageTools;
import plugins.nherve.toolbox.plugin.BackupAndPainterManagerSingletonPlugin;
import plugins.nherve.toolbox.plugin.HelpWindow;

/**
 * The Class MaskEditor.
 * 
 * @author Nicolas HERVE - nicolas.herve@pasteur.fr
 */
public class MaskEditor extends BackupAndPainterManagerSingletonPlugin<MaskStack, MaskEditor.MaskEditorPainter> implements ItemListener, ActionListener, ChangeListener, SwimmingPoolListener, MaskListener {

	/**
	 * Instantiates a new mask editor.
	 */
	public MaskEditor() {
		super();

		if (localFlavor == null) {
			ClassLoader backup = Thread.currentThread().getContextClassLoader();
			Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
			try {
				localFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=" + MaskLayer.class.getName());
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			Thread.currentThread().setContextClassLoader(backup);
		}
	}

	private static DataFlavor localFlavor = null;

	/**
	 * The Class SegmentCompareWindow.
	 * 
	 * @author Nicolas HERVE - nicolas.herve@pasteur.fr
	 */
	class SegmentCompareWindow extends IcyFrame implements ActionListener {

		/** The Constant serialVersionUID. */
		private static final long serialVersionUID = -7316859083701187977L;

		/** The cb1. */
		private JComboBox cb1;

		/** The cb2. */
		private JComboBox cb2;

		/** The bt. */
		private JButton bt;

		/**
		 * Instantiates a new segment compare window.
		 * 
		 * @param stack
		 *            the stack
		 */
		public SegmentCompareWindow(MaskStack stack) {
			super(PLUGIN_NAME, false, true, false, false);

			getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));

			JPanel jp = new JPanel();
			jp.setOpaque(false);
			jp.setLayout(new BoxLayout(jp, BoxLayout.PAGE_AXIS));
			jp.setBorder(new TitledBorder("Compare segmentations"));

			cb1 = new JComboBox();
			cb2 = new JComboBox();
			for (Mask m : stack) {
				cb1.addItem(m);
				cb2.addItem(m);
			}
			jp.add(cb1);
			jp.add(cb2);

			bt = new JButton("compare");
			bt.addActionListener(this);
			jp.add(bt);

			add(jp);

			pack();
			setVisible(true);
			center();
			addToMainDesktopPane();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent
		 * )
		 */
		@Override
		public void actionPerformed(ActionEvent e) {
			Mask m1 = (Mask) cb1.getSelectedItem();
			Mask m2 = (Mask) cb2.getSelectedItem();

			DecimalFormat df = new DecimalFormat("0.0000");

			System.out.println("Score between [" + m1.toString() + "] and [" + m2.toString() + "] : ");
			double sc1 = SegmentationComparison.score1(m1, m2);
			double sc2 = SegmentationComparison.score2(m1, m2);
			double sc3 = SegmentationComparison.score3(m1, m2);
			double sc4 = SegmentationComparison.score4(m1, m2);
			double nhd = SegmentationComparison.nhd(m1, m2);
			System.out.println(" - s1  : " + df.format(sc1) + "(" + df.format(sc1 / m1.getSurface()) + ")");
			System.out.println(" - s2  : " + df.format(sc2));
			System.out.println(" - s3  : " + df.format(sc3));
			System.out.println(" - s4  : " + df.format(sc4) + "(" + df.format(sc4 / m1.getSurface()) + ")");
			System.out.println(" - nhd : " + df.format(nhd));
		}
	}

	/**
	 * The Class MaskEditorPainter.
	 * 
	 * @author Nicolas HERVE - nicolas.herve@pasteur.fr
	 */
	class MaskEditorPainter implements Painter, MouseWheelListener {

		/** The current mouse position. */
		private Point2D currentMousePosition;

		/** The sequence. */
		private Sequence sequence;

		/**
		 * Instantiates a new mask editor painter.
		 */
		public MaskEditorPainter() {
			super();
			setSequence(null);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#finalize()
		 */
		@Override
		protected void finalize() throws Throwable {
			super.finalize();
		}

		/**
		 * Gets the sequence.
		 * 
		 * @return the sequence
		 */
		public Sequence getSequence() {
			return sequence;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see icy.painter.Painter#keyPressed(java.awt.event.KeyEvent,
		 * java.awt.geom.Point2D, icy.canvas.IcyCanvas)
		 */
		@Override
		public void keyPressed(KeyEvent e, Point2D imagePoint, IcyCanvas canvas) {
			if (((e.getModifiersEx() & (KeyEvent.CTRL_DOWN_MASK)) == KeyEvent.CTRL_DOWN_MASK) && (((e.getKeyCode() >= KeyEvent.VK_0) && (e.getKeyCode() <= KeyEvent.VK_9)) || ((e.getKeyCode() >= KeyEvent.VK_NUMPAD0) && (e.getKeyCode() <= KeyEvent.VK_NUMPAD9)))) {
				int layer = -1;
				if ((e.getKeyCode() >= KeyEvent.VK_0) && (e.getKeyCode() <= KeyEvent.VK_9)) {
					layer = e.getKeyCode() - KeyEvent.VK_0;
				} else {
					layer = e.getKeyCode() - KeyEvent.VK_NUMPAD0;
				}

				mll.switchLayerDisplay(layer);
			}

			if (((e.getModifiersEx() & (KeyEvent.CTRL_DOWN_MASK)) == KeyEvent.CTRL_DOWN_MASK) && (e.getKeyCode() == KeyEvent.VK_B)) {
				cbViewBgdBox.setSelected(!cbViewBgdBox.isSelected());
			}

			if (e.getKeyCode() == KeyEvent.VK_W) {
				mll.switchActiveLayerDisplay();
			}

			if (e.getKeyCode() == KeyEvent.VK_D) {
				cbDrawEnabled.setSelected(!cbDrawEnabled.isSelected());
			}

			if (e.getKeyCode() == KeyEvent.VK_X) {
				if (slOpacity.getValue() != slOpacity.getMaximum()) {
					backupOpacity = slOpacity.getValue();
					slOpacity.setValue(slOpacity.getMaximum());
				} else {
					slOpacity.setValue(backupOpacity);
				}
			}

			if (e.getKeyCode() == KeyEvent.VK_C) {
				if ((e.getModifiersEx() & (KeyEvent.CTRL_DOWN_MASK)) == KeyEvent.CTRL_DOWN_MASK) {
					slCursorSize.setValue(slCursorSize.getValue() - 1);
				} else {
					slCursorSize.setValue(slCursorSize.getValue() - 5);
				}
			}

			if (e.getKeyCode() == KeyEvent.VK_V) {
				if ((e.getModifiersEx() & (KeyEvent.CTRL_DOWN_MASK)) == KeyEvent.CTRL_DOWN_MASK) {
					slCursorSize.setValue(slCursorSize.getValue() + 1);
				} else {
					slCursorSize.setValue(slCursorSize.getValue() + 5);
				}
			}

			getCurrentSequence().painterChanged(null);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see icy.painter.Painter#mouseClick(java.awt.event.MouseEvent,
		 * java.awt.geom.Point2D, icy.canvas.IcyCanvas)
		 */
		@Override
		public void mouseClick(MouseEvent e, Point2D imagePoint, IcyCanvas canvas) {
			boolean ctrl = (e.getModifiersEx() & (MouseEvent.CTRL_DOWN_MASK)) == MouseEvent.CTRL_DOWN_MASK;

			if (ctrl) {
				if (cbDrawEnabled.isSelected()) {
					MaskStack stack = getBackupObject();
					Mask currentMask = stack.getActiveMask();
					try {
						currentMask.fillHole((int) imagePoint.getX(), (int) imagePoint.getY());
					} catch (MaskException e1) {
						logError(e1.getClass().getName() + " : " + e1.getMessage());
					}
					getSequence().painterChanged(null);
				}
			} else {
				mouseDrag(e, imagePoint, canvas);
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see icy.painter.Painter#mouseDrag(java.awt.event.MouseEvent,
		 * java.awt.geom.Point2D, icy.canvas.IcyCanvas)
		 */
		@Override
		public void mouseDrag(MouseEvent e, Point2D imagePoint, IcyCanvas canvas) {
			currentMousePosition = imagePoint;

			if (cbDrawEnabled.isSelected()) {
				MaskStack stack = getBackupObject();
				Mask currentMask = stack.getActiveMask();
				if (currentMask != null) {
					boolean shift = (e.getModifiersEx() & (MouseEvent.SHIFT_DOWN_MASK)) == MouseEvent.SHIFT_DOWN_MASK;

					try {
						Area a = new Area(buildCursorShape(imagePoint));

						if (shift) {
							currentMask.remove(a);
						} else {
							currentMask.add(a);
						}
					} catch (MaskException e1) {
						logError(e1.getClass().getName() + " : " + e1.getMessage());
					}

					getSequence().painterChanged(null);
				}
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see icy.painter.Painter#mouseMove(java.awt.event.MouseEvent,
		 * java.awt.geom.Point2D, icy.canvas.IcyCanvas)
		 */
		@Override
		public void mouseMove(MouseEvent e, Point2D imagePoint, IcyCanvas canvas) {
			currentMousePosition = imagePoint;
			getSequence().painterChanged(null);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * java.awt.event.MouseWheelListener#mouseWheelMoved(java.awt.event.
		 * MouseWheelEvent)
		 */
		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			// System.out.println("mouseWheelMoved !");
			int oldValue = slCursorSize.getValue();
			oldValue += e.getWheelRotation();
			if (oldValue < slCursorSize.getMinimum()) {
				oldValue = slCursorSize.getMinimum();
			}
			if (oldValue > slCursorSize.getMaximum()) {
				oldValue = slCursorSize.getMaximum();
			}
			slCursorSize.setValue(oldValue);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see icy.painter.Painter#paint(java.awt.Graphics2D,
		 * icy.sequence.Sequence, icy.canvas.IcyCanvas)
		 */
		@Override
		public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {
			MaskStack stack = getBackupObject(sequence);

			if (!cbViewBgdBox.isSelected()) {
				if (cbBlackWhite.isSelected()) {
					g.setColor(Color.WHITE);
				} else {
					g.setColor(Color.BLACK);
				}
				g.fillRect(0, 0, sequence.getWidth(), sequence.getHeight());
			}

			for (int i = 0; i < stack.size(); i++) {
				Mask m = stack.getByIndex(i);
				if (m.isVisibleLayer()) {
					m.paint(g);
				}
			}

			Mask activeMask = stack.getActiveMask();
			if ((activeMask != null) && (cbDrawEnabled.isSelected()) && (currentMousePosition != null) && (currentMousePosition.getX() >= 0) && (currentMousePosition.getX() < sequence.getWidth()) && (currentMousePosition.getY() >= 0) && (currentMousePosition.getY() < sequence.getHeight())) {
				g.setColor(activeMask.getColor());
				Shape s = buildCursorShape(currentMousePosition);

				g.fill(s);

			}
		}

		/**
		 * Sets the sequence.
		 * 
		 * @param sequence
		 *            the new sequence
		 */
		public void setSequence(Sequence sequence) {
			this.sequence = sequence;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see icy.painter.Painter#keyReleased(java.awt.event.KeyEvent,
		 * java.awt.geom.Point2D, icy.canvas.IcyCanvas)
		 */
		@Override
		public void keyReleased(KeyEvent e, Point2D imagePoint, IcyCanvas canvas) {

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see icy.painter.Painter#mousePressed(java.awt.event.MouseEvent,
		 * java.awt.geom.Point2D, icy.canvas.IcyCanvas)
		 */
		@Override
		public void mousePressed(MouseEvent e, Point2D imagePoint, IcyCanvas canvas) {

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see icy.painter.Painter#mouseReleased(java.awt.event.MouseEvent,
		 * java.awt.geom.Point2D, icy.canvas.IcyCanvas)
		 */
		@Override
		public void mouseReleased(MouseEvent e, Point2D imagePoint, IcyCanvas canvas) {

		}

	}

	/**
	 * The Class MaskLayer.
	 * 
	 * @author Nicolas HERVE - nicolas.herve@pasteur.fr
	 */
	class MaskLayer extends JPanel implements ActionListener, ItemListener, MouseListener, KeyListener {

		private class ComponentPanelTransferHandler extends TransferHandler {
			private static final long serialVersionUID = -307871237679751409L;

			@Override
			public int getSourceActions(JComponent c) {
				return MOVE;
			}

			@Override
			protected Transferable createTransferable(JComponent c) {
				if (c instanceof MaskLayer) {
					MaskLayer cp = (MaskLayer) c;
					Transferable r = new ComponentPanelTransferable(Integer.toString(cp.getMask().getId()));
					return r;
				}
				return null;
			}

			@Override
			protected void exportDone(JComponent source, Transferable data, int action) {
				mll.refreshMLLInterface();
			}

			@Override
			public boolean importData(TransferSupport support) {
				if (!canImport(support)) {
					return false;
				}

				try {
					Transferable tf = support.getTransferable();
					String s = (String) tf.getTransferData(localFlavor);
					MaskLayer toMove = mll.getLayerById(Integer.parseInt(s));
					MaskLayer moveAbove = (MaskLayer) support.getComponent();
					mll.getStack().moveAt(toMove.getMask(), mll.getStack().indexOf(moveAbove.getMask()));
					return true;
				} catch (UnsupportedFlavorException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

				return false;
			}

			@Override
			public boolean canImport(TransferSupport support) {
				if (!support.isDataFlavorSupported(localFlavor)) {
					return false;
				}
				return true;
			}
		}

		private class ComponentPanelMouseAdapter extends MouseAdapter {

			@Override
			public void mousePressed(MouseEvent evt) {
				JComponent comp = (JComponent) evt.getSource();
				TransferHandler th = comp.getTransferHandler();
				th.exportAsDrag(comp, evt, TransferHandler.MOVE);
			}
		}

		private class ComponentPanelTransferable implements Transferable {
			public ComponentPanelTransferable(String id) {
				super();
				this.id = id;
			}

			private String id;

			@Override
			public DataFlavor[] getTransferDataFlavors() {
				return new DataFlavor[] { localFlavor };
			}

			@Override
			public boolean isDataFlavorSupported(DataFlavor flavor) {
				return flavor.equals(localFlavor);
			}

			@Override
			public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
				return id;
			}
		}

		/** The Constant serialVersionUID. */
		private static final long serialVersionUID = 5691992815105835697L;

		/** The cb id. */
		private JLabel cbId;

		/** The cb name. */
		private JLabel cbName;

		/** The tf name. */
		private JTextField tfName;

		/** The cb view. */
		private JCheckBox cbView;

		/** The bt color. */
		private JButton btColor;

		/** The bt sp. */
		private JButton btSP;

		/** The bt info. */
		private JButton btInfo;

		private JButton btAsROI;

		/** The bt save. */
		private JButton btSave;

		/** The bt active. */
		private JRadioButton btActive;

		/** The mask. */
		final private Mask mask;

		/** The bt up. */
		private JButton btUp;

		private ImageComponent imMove;

		/** The bt down. */
		private JButton btDown;

		/** The bt delete. */
		private JButton btDelete;

		/** The bt add. */
		private JButton btAdd;

		/** The bt intersect. */
		private JButton btIntersect;

		private JButton btLocalRealColors;

		private JButton btExportToWhite;
		private JButton btExportToBlack;
		
		private JButton btPop;
		private JPopupMenu pop;

		private JButton btROIPlus;
		private JButton btROIMinus;

		/**
		 * Instantiates a new mask layer.
		 * 
		 * @param mask
		 *            the mask
		 */
		public MaskLayer(Mask maskp) {
			super();
			this.mask = maskp;
			setTransferHandler(new ComponentPanelTransferHandler());
			addMouseListener(new ComponentPanelMouseAdapter());
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent
		 * )
		 */
		@Override
		public void actionPerformed(ActionEvent e) {
			Object o = e.getSource();

			if (o == null) {
				return;
			}

			if (o instanceof JButton) {
				JButton b = (JButton) e.getSource();
				if (b == btPop) {
					if (btPop.getComponentPopupMenu().isShowing()) {
						btPop.getComponentPopupMenu().setVisible(false);
					} else {
						btPop.getComponentPopupMenu().show(btPop, 0, btPop.getHeight());
					}
				}
				if (b == btColor) {
					btColor.setBackground(JColorChooser.showDialog(frame.getFrame(), "Choose current mask color", btColor.getBackground()));
					mask.setColor(btColor.getBackground());
					getCurrentSequence().painterChanged(null);
				}
				if (b == btLocalRealColors) {
					mask.setColor(mask.getAverageColor(getCurrentSequence().getFirstImage()));
					btColor.setBackground(mask.getColor());
					getCurrentSequence().painterChanged(null);
					btPop.getComponentPopupMenu().setVisible(false);
				}
				if (b == btExportToBlack) {
					IcyBufferedImage toSave = mask.getBinaryData().asIcyBufferedImage(1, false);
					Sequence s = new Sequence(toSave);
					addSequence(s);
					btPop.getComponentPopupMenu().setVisible(false);
				}
				if (b == btExportToWhite) {
					IcyBufferedImage toSave = mask.getBinaryData().asIcyBufferedImage(1, true);
					Sequence s = new Sequence(toSave);
					addSequence(s);
					btPop.getComponentPopupMenu().setVisible(false);
				}
				if (b == btSP) {
					SwimmingObject result = new SwimmingObject(mask);
					Icy.getMainInterface().getSwimmingPool().add(result);
					btPop.getComponentPopupMenu().setVisible(false);
				}
				if (b == btInfo) {
					int nbon = mask.getSurface();
					double pct = (double) nbon * 100d / (double) (mask.getHeight() * mask.getWidth());
					DecimalFormat df = new DecimalFormat("0.00");
					System.out.println("Some info on " + mask + " : surface = " + nbon + " (" + df.format(pct) + " %)");
					btPop.getComponentPopupMenu().setVisible(false);
				}
				if (b == btSave) {
					File f = displayTiffExport();
					if (f != null) {
						try {
							SomeImageTools.save(mask.getBinaryData(), f, ColorSpaceTools.NB_COLOR_CHANNELS);
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
					btPop.getComponentPopupMenu().setVisible(false);
				}
				if (b == btAsROI) {
					ROI2DArea a = mask.asROI2DArea(getCurrentSequence());
					if (a != null) {
						a.setName("From mask " + mask.getLabel());
					}
					btPop.getComponentPopupMenu().setVisible(false);
				}
				if (b == btROIPlus) {
					for (ROI2D roi : getCurrentSequence().getROI2Ds()) {
						if (roi.isSelected()) {
							try {
								mask.add(roi);
							} catch (MaskException e1) {
								logError(e1.getClass().getName() + " : " + e1.getMessage());
							}
						}
					}
					refreshInterface();
					btPop.getComponentPopupMenu().setVisible(false);
				}
				if (b == btROIMinus) {
					for (ROI2D roi : getCurrentSequence().getROI2Ds()) {
						if (roi.isSelected()) {
							try {
								mask.remove(roi);
							} catch (MaskException e1) {
								logError(e1.getClass().getName() + " : " + e1.getMessage());
							}
						}
					}
					refreshInterface();
					btPop.getComponentPopupMenu().setVisible(false);
				}
				if (b == btDelete) {
					mll.getStack().remove(mask);
					refreshInterface();
				}
				if (b == btUp) {
					// reverse display
					mll.getStack().moveDown(mask);
					refreshInterface();
				}
				if (b == btDown) {
					// reverse display
					mll.getStack().moveUp(mask);
					refreshInterface();
				}
				if (b == btAdd) {
					try {
						mll.getStack().addPreviousInStack(mask);
					} catch (MaskException e1) {
						logError(e1.getClass().getName() + " : " + e1.getMessage());
					}
					refreshInterface();
				}
				if (b == btIntersect) {
					try {
						mll.getStack().intersectPreviousInStack(mask);
					} catch (MaskException e1) {
						logError(e1.getClass().getName() + " : " + e1.getMessage());
					}
					refreshInterface();
				}
			}

			if (o instanceof JRadioButton) {
				JRadioButton b = (JRadioButton) e.getSource();

				if (b == btActive) {
					mll.getStack().setActiveMask(mask);
					cbView.setSelected(true);
				}
			}
		}

		/**
		 * Gets the mask.
		 * 
		 * @return the mask
		 */
		public Mask getMask() {
			return mask;
		}

		/**
		 * Checks if is a visible layer.
		 * 
		 * @return true, if is a visible layer
		 */
		public boolean isAVisibleLayer() {
			return cbView.isSelected();
		}

		/**
		 * Checks if is currently active.
		 * 
		 * @return true, if is currently active
		 */
		public boolean isCurrentlyActive() {
			return btActive.isSelected();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent
		 * )
		 */
		@Override
		public void itemStateChanged(ItemEvent e) {
			Object o = e.getSource();

			if (o == null) {
				return;
			}

			if (o instanceof JCheckBox) {
				JCheckBox c = (JCheckBox) e.getSource();

				if (c == cbView) {
					mask.setVisibleLayer(isAVisibleLayer());
					getCurrentSequence().painterChanged(null);
				}
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
		 */
		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				cbName.setText(tfName.getText());
				mask.setLabel(tfName.getText());
				cbName.setVisible(true);
				tfName.setVisible(false);
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
		 */
		@Override
		public void keyReleased(KeyEvent e) {
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
		 */
		@Override
		public void keyTyped(KeyEvent e) {
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
		 */
		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 2) {
				cbName.setVisible(false);
				tfName.setVisible(true);
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
		 */
		@Override
		public void mouseEntered(MouseEvent e) {
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
		 */
		@Override
		public void mouseExited(MouseEvent e) {
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
		 */
		@Override
		public void mousePressed(MouseEvent e) {
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
		 */
		@Override
		public void mouseReleased(MouseEvent e) {
		}

		/**
		 * Sets the currently active.
		 */
		public void setCurrentlyActive() {
			btActive.setSelected(true);
		}

		/**
		 * Start interface.
		 * 
		 * @param bg
		 *            the bg
		 */
		public void startInterface(ButtonGroup bg) {
			BoxLayout bl = new BoxLayout(this, BoxLayout.LINE_AXIS);
			setLayout(bl);
			btActive = new JRadioButton();
			btActive.setToolTipText("Set as active mask for drawing");
			btActive.addActionListener(this);
			bg.add(btActive);
			add(btActive);
			cbView = new JCheckBox();
			cbView.setToolTipText("Set visible");
			cbView.setSelected(mask.isVisibleLayer());
			cbView.addItemListener(this);
			add(cbView);

			imMove = new ImageComponent(NherveToolbox.handImage);
			ComponentUtil.setFixedSize(imMove, new Dimension(16, 16));
			add(imMove);
			// btUp = new JButton(NherveToolbox.upIcon);
			// btUp.setToolTipText("Move up in stack");
			// btUp.addActionListener(this);
			// add(btUp);
			// btDown = new JButton(NherveToolbox.downIcon);
			// btDown.setToolTipText("Move down in stack");
			// btDown.addActionListener(this);
			// add(btDown);

			cbId = new JLabel(mask.getId() + " - ");
			add(cbId);
			cbName = new JLabel(mask.getLabel());
			cbName.setToolTipText("Mask name - double click to edit");
			cbName.addMouseListener(this);
			ComponentUtil.setFixedHeight(cbName, 24);
			add(cbName);
			tfName = new JTextField(cbName.getText());
			tfName.setEditable(true);
			tfName.addKeyListener(this);
			tfName.setVisible(false);
			ComponentUtil.setFixedHeight(tfName, 24);
			add(tfName);

			add(Box.createHorizontalGlue());

			btAdd = new JButton(NherveToolbox.addIcon);
			btAdd.setToolTipText("Merge with next mask in stack");
			btAdd.addActionListener(this);
			add(btAdd);
			btIntersect = new JButton(NherveToolbox.intersectIcon);
			btIntersect.setToolTipText("Intersect with next mask in stack");
			btIntersect.addActionListener(this);
			add(btIntersect);

			btColor = new JButton();
			ComponentUtil.setFixedSize(btColor, new Dimension(50, 22));
			btColor.setToolTipText("Change mask drawing color");
			btColor.setBackground(mask.getColor());
			btColor.addActionListener(this);
			add(btColor);

			btDelete = new JButton(NherveToolbox.crossIcon);
			btDelete.setToolTipText("Delete mask");
			btDelete.addActionListener(this);
			add(btDelete);

			btROIPlus = new JButton(NherveToolbox.roiPlusIcon);
			btROIPlus.setToolTipText("Add selected ROIs to this mask");
			btROIPlus.addActionListener(this);
			add(btROIPlus);

			btROIMinus = new JButton(NherveToolbox.roiMinusIcon);
			btROIMinus.setToolTipText("Remove selected ROIs from this mask");
			btROIMinus.addActionListener(this);
			add(btROIMinus);

			pop = new JPopupMenu();

			btLocalRealColors = new JButton(NherveToolbox.colorsIcon);
			btLocalRealColors.setToolTipText("Real color");
			btLocalRealColors.addActionListener(this);
			pop.add(btLocalRealColors);
			
			btExportToBlack = new JButton(NherveToolbox.toBlackIcon);
			btExportToBlack.setToolTipText("As sequence (black)");
			btExportToBlack.addActionListener(this);
			pop.add(btExportToBlack);
			
			btExportToWhite = new JButton(NherveToolbox.toWhiteIcon);
			btExportToWhite.setToolTipText("As sequence (white)");
			btExportToWhite.addActionListener(this);
			pop.add(btExportToWhite);

			btSP = new JButton(NherveToolbox.swimingPoolIcon);
			btSP.setToolTipText("Send to the swimming pool");
			btSP.addActionListener(this);
			pop.add(btSP);

			btInfo = new JButton(NherveToolbox.questionIcon);
			btInfo.setToolTipText("Get some basic informations");
			btInfo.addActionListener(this);
			pop.add(btInfo);

			btSave = new JButton(NherveToolbox.saveIcon);
			btSave.setToolTipText("Export mask as TIFF");
			btSave.addActionListener(this);
			pop.add(btSave);

			btAsROI = new JButton(NherveToolbox.asroiIcon);
			btAsROI.setToolTipText("Export mask as ROI");
			btAsROI.addActionListener(this);
			pop.add(btAsROI);

			btPop = new JButton(NherveToolbox.dotsIcon);
			btPop.setToolTipText("Other actions");
			btPop.addActionListener(this);
			btPop.setComponentPopupMenu(pop);
			add(btPop);

			setVisible(true);
		}

	}

	/**
	 * The Class MaskLayerList.
	 * 
	 * @author Nicolas HERVE - nicolas.herve@pasteur.fr
	 */
	class MaskLayerList extends JPanel implements Iterable<MaskLayer>, ActionListener, SwimmingPoolListener {

		private class BackgroundPanelTransferHandler extends TransferHandler {
			private static final long serialVersionUID = -307871237679751409L;

			@Override
			public boolean importData(TransferSupport support) {
				if (!canImport(support)) {
					return false;
				}

				try {
					Transferable tf = support.getTransferable();
					String s = (String) tf.getTransferData(localFlavor);
					MaskLayer toMove = mll.getLayerById(Integer.parseInt(s));
					mll.getStack().moveBottom(toMove.getMask());

					return true;
				} catch (UnsupportedFlavorException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

				return false;
			}

			@Override
			public boolean canImport(TransferSupport support) {
				if (!support.isDataFlavorSupported(localFlavor)) {
					return false;
				}
				return true;
			}
		}

		/** The Constant serialVersionUID. */
		private static final long serialVersionUID = -6963474868004516282L;

		/** The stack. */
		private MaskStack stack;

		/** The bt duplicate mask. */
		private JButton btDuplicateMask;

		/** The bt add mask. */
		private JButton btAddMask;

		/** The bt add mask sp. */
		private JButton btAddMaskSP;

		/** The bt group. */
		private ButtonGroup btGroup;

		/** The bt erode. */
		private JButton btErode;

		/** The bt dilate. */
		private JButton btDilate;

		/** The bt invert. */
		private JButton btInvert;

		/** The bt fill holes. */
		private JButton btFillHoles;

		/** The bt filter size. */
		private JButton btFilterSize;

		private JButton btFromROI;

		/** The tf filter size. */
		private JTextField tfFilterSize;

		/** The bt compare. */
		private JButton btCompare;

		private JScrollPane scroll;
		private JPanel mlp;

		/** The layers. */
		private ArrayList<MaskLayer> layers;

		/**
		 * Instantiates a new mask layer list.
		 */
		public MaskLayerList() {
			super();
			stack = null;
			layers = new ArrayList<MaskLayer>();

			setOpaque(false);
			setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
			setBorder(new TitledBorder("Masks list"));

			btAddMaskSP = new JButton("From SP");
			btAddMaskSP.addActionListener(this);
			btAddMaskSP.setEnabled(isMaskInPool());

			btFromROI = new JButton("From ROI");
			btFromROI.addActionListener(this);
			btFromROI.setEnabled(false);

			btDuplicateMask = new JButton("Copy mask");
			btDuplicateMask.addActionListener(this);
			btDuplicateMask.setEnabled(false);

			btAddMask = new JButton("New mask");
			btAddMask.addActionListener(this);
			btAddMask.setEnabled(false);

			btCompare = new JButton("Compare");
			btCompare.addActionListener(this);

			JPanel btp = GuiUtil.createLineBoxPanel(new Component[] { Box.createHorizontalGlue(), btAddMaskSP, Box.createHorizontalGlue(), btFromROI, Box.createHorizontalGlue(), btDuplicateMask, Box.createHorizontalGlue(), btAddMask, Box.createHorizontalGlue(), btCompare, Box.createHorizontalGlue() });
			add(btp);

			btErode = new JButton("Erode");
			btErode.addActionListener(this);

			btDilate = new JButton("Dilate");
			btDilate.addActionListener(this);

			btInvert = new JButton("Invert");
			btInvert.addActionListener(this);

			btFillHoles = new JButton("Fill holes");
			btFillHoles.addActionListener(this);

			btFilterSize = new JButton("Filter");
			btFilterSize.addActionListener(this);

			tfFilterSize = new JTextField("1");
			ComponentUtil.setFixedSize(tfFilterSize, new Dimension(50, 25));

			JPanel morpho = GuiUtil.createLineBoxPanel(new Component[] { btErode, Box.createHorizontalGlue(), btDilate, Box.createHorizontalGlue(), btInvert, Box.createHorizontalGlue(), btFillHoles, Box.createHorizontalGlue(), btFilterSize, Box.createHorizontalGlue(), tfFilterSize });
			add(morpho);

			mlp = new JPanel();
			mlp.setLayout(new BoxLayout(mlp, BoxLayout.PAGE_AXIS));
			mlp.setMinimumSize(new Dimension(0, 300));

			scroll = new JScrollPane(mlp);
			add(scroll);

			SwimmingPool sp = Icy.getMainInterface().getSwimmingPool();
			sp.addListener(this);

			setTransferHandler(new BackgroundPanelTransferHandler());
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent
		 * )
		 */
		@Override
		public void actionPerformed(ActionEvent e) {
			Object o = e.getSource();

			if (o == null) {
				return;
			}

			if (o instanceof JButton) {
				JButton b = (JButton) e.getSource();

				if (b == btAddMask) {
					try {
						reInitColorMap();
						stack.createNewMask(MaskStack.MASK_DEFAULT_LABEL, true, colorMap, getGlobalOpacity());
					} catch (MaskException e1) {
						logError(e1.getClass().getName() + " : " + e1.getMessage());
					}
				}

				if (b == btCompare) {
					new SegmentCompareWindow(stack);
				}

				if (b == btDuplicateMask) {
					try {
						reInitColorMap();
						stack.copyCurrentMask(colorMap);
					} catch (MaskException e1) {
						logError(e1.getClass().getName() + " : " + e1.getMessage());
					}
				}

				if (b == btFromROI) {
					try {
						reInitColorMap();
						Mask m = stack.createNewMask(MaskStack.MASK_DEFAULT_LABEL, true, colorMap, getGlobalOpacity());
						for (ROI2D roi : getCurrentSequence().getROI2Ds()) {
							m.add(roi);
						}
					} catch (MaskException e1) {
						logError(e1.getClass().getName() + " : " + e1.getMessage());
					}
				}

				if (b == btAddMaskSP) {
					try {
						Mask m = null;
						do {
							m = getFromPool();
							if (m != null) {
								reInitColorMap();
								stack.addExternalMask(m, colorMap);
								m.setOpacity(getGlobalOpacity());
							}
						} while (m != null);
					} catch (MaskException e1) {
						logError(e1.getClass().getName() + " : " + e1.getMessage());
					}
				}

				if (b == btErode) {
					try {
						Mask m = stack.getActiveMask();
						m.erode();
					} catch (MaskException e1) {
						logError(e1.getClass().getName() + " : " + e1.getMessage());
					}
					getCurrentSequence().painterChanged(null);
				}

				if (b == btDilate) {
					try {
						Mask m = stack.getActiveMask();
						m.dilate();
					} catch (MaskException e1) {
						logError(e1.getClass().getName() + " : " + e1.getMessage());
					}
					getCurrentSequence().painterChanged(null);
				}

				if (b == btInvert) {
					try {
						Mask m = stack.getActiveMask();
						m.invert();
					} catch (MaskException e1) {
						logError(e1.getClass().getName() + " : " + e1.getMessage());
					}
					getCurrentSequence().painterChanged(null);
				}

				if (b == btFillHoles) {
					try {
						Mask m = stack.getActiveMask();
						m.fillHoles();
					} catch (MaskException e1) {
						logError(e1.getClass().getName() + " : " + e1.getMessage());
					}
					getCurrentSequence().painterChanged(null);
				}

				if (b == btFilterSize) {
					try {
						Mask m = stack.getActiveMask();
						m.filterSize(Integer.parseInt(tfFilterSize.getText()));
					} catch (MaskException e1) {
						logError(e1.getClass().getName() + " : " + e1.getMessage());
					} catch (NumberFormatException e2) {
						logError(e2.getClass().getName() + " : " + e2.getMessage());
					}
					getCurrentSequence().painterChanged(null);
				}
			}
		}

		/**
		 * Gets the from pool.
		 * 
		 * @return the from pool
		 */
		private Mask getFromPool() {
			SwimmingPool sp = Icy.getMainInterface().getSwimmingPool();
			SwimmingObject res = null;
			for (SwimmingObject r : sp.getObjects()) {
				if (r.getObject() instanceof Mask) {
					res = r;
					break;
				}
			}

			Mask m = null;
			if (res != null) {
				m = (Mask) res.getObject();
				sp.remove(res);
			}
			return m;
		}

		/**
		 * Gets the layer by id.
		 * 
		 * @param id
		 *            the id
		 * @return the layer by id
		 */
		public MaskLayer getLayerById(int id) {
			for (MaskLayer l : layers) {
				if (l.getMask().getId() == id) {
					return l;
				}
			}
			return null;
		}

		/**
		 * Gets the stack.
		 * 
		 * @return the stack
		 */
		public MaskStack getStack() {
			return stack;
		}

		/**
		 * Checks if is mask in pool.
		 * 
		 * @return true, if is mask in pool
		 */
		private boolean isMaskInPool() {
			SwimmingPool sp = Icy.getMainInterface().getSwimmingPool();
			for (SwimmingObject r : sp.getObjects()) {
				if (r.getObject() instanceof Mask) {
					return true;
				}
			}
			return false;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Iterable#iterator()
		 */
		@Override
		public Iterator<MaskLayer> iterator() {
			return layers.iterator();
		}

		/**
		 * Refresh interface.
		 */
		public void refreshMLLInterface() {
			// for (MaskLayer ml : layers) {
			// mlp.remove(ml);
			// }
			mlp.removeAll();
			layers.clear();
			btGroup = new ButtonGroup();

			if (stack == null) {
				// need stack
				btAddMask.setEnabled(false);
				btLoad.setEnabled(false);
			}

			if ((stack == null) || (stack.size() < 1)) {
				// need 1 mask
				btSave.setEnabled(false);
				btSaveFullImage.setEnabled(false);
				btSendSPGlobal.setEnabled(false);
				btDuplicateMask.setEnabled(false);
				cbxCursorShape.setEnabled(false);
				slCursorSize.setEnabled(false);
				slOpacity.setEnabled(false);
				cbDrawEnabled.setEnabled(false);
				cbOnlyContours.setEnabled(false);
				cbViewBgdBox.setEnabled(false);
				cbBlackWhite.setEnabled(false);
				btRealColors.setEnabled(false);
				btArtificialColors.setEnabled(false);
				mll.btErode.setEnabled(false);
				mll.btDilate.setEnabled(false);
				mll.btInvert.setEnabled(false);
				mll.btFillHoles.setEnabled(false);
				mll.btFilterSize.setEnabled(false);
			}

			if ((stack == null) || (stack.size() < 2)) {
				// need 2 masks
				mll.btCompare.setEnabled(false);
			}

			if (stack != null) {
				btAddMask.setEnabled(true);
				btLoad.setEnabled(true);

				if (stack.size() > 0) {
					btSave.setEnabled(true);
					btSaveFullImage.setEnabled(true);
					btSendSPGlobal.setEnabled(true);
					btDuplicateMask.setEnabled(true);
					cbxCursorShape.setEnabled(true);
					slCursorSize.setEnabled(true);
					slOpacity.setEnabled(true);
					cbDrawEnabled.setEnabled(true);
					cbOnlyContours.setEnabled(true);
					cbViewBgdBox.setEnabled(true);
					cbBlackWhite.setEnabled(true);
					btRealColors.setEnabled(true);
					btArtificialColors.setEnabled(true);
					mll.btErode.setEnabled(true);
					mll.btDilate.setEnabled(true);
					mll.btInvert.setEnabled(true);
					mll.btFillHoles.setEnabled(true);
					mll.btFilterSize.setEnabled(true);
				}

				if (stack.size() > 1) {
					mll.btCompare.setEnabled(true);
				}

				MaskLayer al = null;
				for (int i = stack.size() - 1; i >= 0; i--) {
					Mask m = stack.getByIndex(i);
					MaskLayer ml = new MaskLayer(m);
					ml.startInterface(btGroup);
					mlp.add(ml);
					layers.add(ml);
					if (stack.getActiveMask() == m) {
						al = ml;
					}
				}
				if (al != null) {
					al.setCurrentlyActive();
				}
			}

			mlp.revalidate();
			scroll.revalidate();
			scroll.repaint();

		}

		/**
		 * Sets the stack.
		 * 
		 * @param stack
		 *            the new stack
		 */
		public void setStack(MaskStack stack) {
			this.stack = stack;
			if (stack != null) {
				stack.addListener(getRunningInstance(false));
			}
			refreshInterface();
		}

		/**
		 * Stop.
		 */
		public void stop() {
			SwimmingPool sp = Icy.getMainInterface().getSwimmingPool();
			sp.removeListener(this);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * icy.swimmingPool.SwimmingPoolListener#swimmingPoolChangeEvent(icy
		 * .swimmingPool.SwimmingPoolEvent)
		 */
		@Override
		public void swimmingPoolChangeEvent(SwimmingPoolEvent swimmingPoolEvent) {
			Thread ct = Thread.currentThread();

			if (ct.getClass().getName().equals("java.awt.EventDispatchThread")) {
				btAddMaskSP.setEnabled(isMaskInPool());
			} else {
				Runnable r = new Runnable() {
					@Override
					public void run() {
						btAddMaskSP.setEnabled(isMaskInPool());
					}
				};

				try {
					SwingUtilities.invokeAndWait(r);
				} catch (InterruptedException e) {
					logError(e.getClass().getName() + " : " + e.getMessage());
				} catch (InvocationTargetException e) {
					logError(e.getClass().getName() + " : " + e.getMessage());
				}
			}
		}

		/**
		 * Switch active layer display.
		 */
		public void switchActiveLayerDisplay() {
			for (MaskLayer l : layers) {
				if (l.isCurrentlyActive()) {
					switchLayerDisplay(l);
					break;
				}
			}
		}

		/**
		 * Switch layer display.
		 * 
		 * @param id
		 *            the id
		 */
		public void switchLayerDisplay(int id) {
			MaskLayer l = getLayerById(id);
			switchLayerDisplay(l);
		}

		/**
		 * Switch layer display.
		 * 
		 * @param l
		 *            the l
		 */
		public void switchLayerDisplay(MaskLayer l) {
			if (l != null) {
				if (l.cbView.isSelected()) {
					l.cbView.setSelected(false);
					l.getMask().setVisibleLayer(false);
				} else {
					l.cbView.setSelected(true);
					l.getMask().setVisibleLayer(true);
				}
			}
		}
	}

	/** The Constant PLUGIN_NAME. */
	private final static String PLUGIN_NAME = "Mask Editor";

	/** The Constant PLUGIN_VERSION. */
	private final static String PLUGIN_VERSION = "1.1.0";

	/** The Constant FULL_PLUGIN_NAME. */
	private final static String FULL_PLUGIN_NAME = PLUGIN_NAME + " V" + PLUGIN_VERSION;

	/** The Constant PREFERENCES_NODE. */
	private final static String PREFERENCES_NODE = "icy/plugins/nherve/maskeditor/MaskEditor";

	/** The Constant SHAPE_SQUARE. */
	private final static String SHAPE_SQUARE = "Square";

	/** The Constant SHAPE_CIRCLE. */
	private final static String SHAPE_CIRCLE = "Circle";

	/** The Constant SHAPE_VERTICAL_LINE. */
	private final static String SHAPE_VERTICAL_LINE = "Vertical line";

	/** The Constant SHAPE_HORIZONTAL_LINE. */
	private final static String SHAPE_HORIZONTAL_LINE = "Horizontal line";

	/** The Constant NB_COLOR_CYCLE. */
	public final static int NB_COLOR_CYCLE = 3;

	private static String HELP = "<html>" + "<p align=\"center\"><b>" + FULL_PLUGIN_NAME + "</b></p>" + "<p align=\"center\"><b>" + NherveToolbox.DEV_NAME_HTML + "</b></p>" + "<p align=\"center\"><a href=\"http://www.herve.name/pmwiki.php/Main/MaskEditor\">Online help is available</a></p>" + "<p align=\"center\"><b>" + NherveToolbox.COPYRIGHT_HTML + "</b></p>" + "<hr/>" + "<p>On each opened sequence, you can use the following keys : </p>" + "<table>" + "<tr><td align=\"center\"><b>D</b></td><td>activate / deactivate the drawing tool</td></tr>" + "<tr><td align=\"center\"><b>Click</b></td><td>add to current mask</td></tr>" + "<tr><td align=\"center\"><b>SHIFT + Click</b></td><td>substract from current mask</td></tr>" + "<tr><td align=\"center\"><b>CTRL + Click</b></td><td>fill hole in current mask</td></tr>" + "<tr><td></td></tr>" + "<tr><td align=\"center\"><b>W</b></td><td>show / hide the active mask</td></tr>" + "<tr><td align=\"center\"><b>CTRL + [0..9]</b></td><td>show / hide the corresponding mask</td></tr>" + "<tr><td align=\"center\"><b>X</b></td><td>show / hide all masks</i> tool</td></tr>" + "<tr><td align=\"center\"><b>C</b></td><td>decrease the draw tool size</td></tr>" + "<tr><td align=\"center\"><b>CTRL + C</b></td><td>slightly decrease the draw tool size</td></tr>" + "<tr><td align=\"center\"><b>V</b></td><td>increase the draw tool size</td></tr>" + "<tr><td align=\"center\"><b>CTRL + V</b></td><td>slightly increase the draw tool size</td></tr>" + "</table>" + "<hr/>" + "<p>" + PLUGIN_NAME + NherveToolbox.LICENCE_HTML + "</p>" + "<p>" + NherveToolbox.LICENCE_HTMLLINK + "</p>" + "</html>";

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -1433844770818397476L;

	/** The frame. */
	private IcyFrame frame;

	/** The cbx cursor shape. */
	private JComboBox cbxCursorShape;

	/** The sl cursor size. */
	private JSlider slCursorSize;

	/** The sl opacity. */
	private JSlider slOpacity;

	/** The backup opacity. */
	private int backupOpacity;

	/** The bt load. */
	private JButton btLoad;

	/** The bt save. */
	private JButton btSave;

	/** The bt send sp global. */
	private JButton btSendSPGlobal;

	/** The bt get sp global. */
	private JButton btGetSPGlobal;

	/** The bt real colors. */
	private JButton btRealColors;

	/** The bt artificial colors. */
	private JButton btArtificialColors;

	/** The bt save full image. */
	private JButton btSaveFullImage;

	/** The bt help. */
	private JButton btHelp;

	/** The view bgd box. */
	private JCheckBox cbViewBgdBox;

	/** The black white. */
	private JCheckBox cbBlackWhite;

	/** The cb only contours. */
	private JCheckBox cbOnlyContours;

	/** The cb draw enabled. */
	private JCheckBox cbDrawEnabled;

	/** The mll. */
	private MaskLayerList mll;

	/** The color map. */
	private DifferentColorsMap colorMap;

	private final static int COLORMAP_NBCOLORS = 10;

	/**
	 * Sets the segmentation for sequence.
	 * 
	 * @param seq
	 *            the seq
	 * @param ms
	 *            the ms
	 */
	public void setSegmentationForSequence(Sequence seq, MaskStack ms) {
		if (ms != null) {
			ms.checkAfterLoad((float) slOpacity.getValue() / 100f, seq.getFirstImage());
			removeBackupObject(seq);
			addBackupObject(seq, ms);
			if (getCurrentSequence() == seq) {
				mll.setStack(ms);
			}
		}
	}

	/**
	 * Sets the segmentation.
	 * 
	 * @param seq
	 *            the seq
	 * @param ms
	 *            the ms
	 */
	public static void setSegmentation(final Sequence seq, final MaskStack ms) {
		final MaskEditor me = getRunningInstance(true);
		ThreadUtil.invokeLater(new Runnable() {
			@Override
			public void run() {
				me.setSegmentationForSequence(seq, ms);
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();

		if (o == null) {
			return;
		}

		if (o instanceof JButton) {
			JButton b = (JButton) e.getSource();

			if (b == btHelp) {
				new HelpWindow(PLUGIN_NAME, HELP, 400, 600, frame);
				return;
			}

			if (b == btRealColors) {
				mll.getStack().reInitColors(getCurrentSequence().getFirstImage());
				return;
			}

			if (b == btArtificialColors) {
				mll.getStack().reInitColors(colorMap);
				return;
			}

			if (b == btSaveFullImage) {
				File f = displayTiffExport();
				if (f != null) {
					try {
						Sequence s = getCurrentSequence();
						BufferedImage localCache = new BufferedImage(s.getWidth(), s.getHeight(), BufferedImage.TYPE_INT_ARGB);
						Graphics2D g2 = localCache.createGraphics();
						g2.drawImage(s.getFirstImage(), null, 0, 0);
						getCurrentSequencePainter().paint(g2, s, null);
						Saver.saveImage(IcyBufferedImage.createFrom(localCache), f, true);
					} catch (IOException e1) {
						e1.printStackTrace();
					} catch (FormatException e1) {
						e1.printStackTrace();
					}
				}
			}

			if (b == btGetSPGlobal) {
				MaskStack s = null;
				s = getFromPool();
				setSegmentationForSequence(getCurrentSequence(), s);
				return;
			}

			if (b == btSendSPGlobal) {
				MaskStack s = mll.getStack();
				SwimmingObject result = new SwimmingObject(s);
				Icy.getMainInterface().getSwimmingPool().add(result);
				return;
			}

			if ((b == btSave) || (b == btLoad)) {
				try {
					MaskPersistence rep = new OptimizedMaskPersistenceImpl();
					if (b == btSave) {
						String d = getCurrentSequence().getFilename();
						File df = null;
						if ((d != null) && (d.length() > 0)) {
							int idx = d.lastIndexOf(".");
							if (idx > 0) {
								d = d.substring(0, idx);
							}
							d += rep.getMaskFileExtension();
							df = new File(d);
						}
						File f = displayFileChooser(rep, df);
						if (f != null) {
							rep.save(mll.getStack(), f);
						}
					}
					if (b == btLoad) {
						File f = displayFileChooser(rep, null);
						if (f != null) {
							MaskStack s = rep.loadMaskStack(f);
							s.checkAfterLoad((float) slOpacity.getValue() / 100f, getCurrentSequence().getFirstImage());
							removeBackupObject(getCurrentSequence());
							addBackupObject(s);
							mll.setStack(s);
						}
					}
				} catch (PersistenceException e1) {
					logError(e1.getClass().getName() + " : " + e1.getMessage());
				}
				return;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * plugins.nherve.toolbox.plugin.BackupSingletonPlugin#backupCurrentSequence
	 * ()
	 */
	@Override
	public void backupCurrentSequence() {
		if (!hasBackupObject()) {
			Sequence s = getCurrentSequence();
			MaskStack stack = new MaskStack(s.getWidth(), s.getHeight());
			try {
				reInitColorMap();
				stack.createNewMask(MaskStack.MASK_DEFAULT_LABEL, true, colorMap.get(0), getGlobalOpacity());
			} catch (MaskException e) {
				logError(e.getClass().getName() + " : " + e.getMessage());
			}
			addBackupObject(stack);
		}
	}

	/**
	 * Builds the cursor shape.
	 * 
	 * @param p
	 *            the p
	 * @return the shape
	 */
	public Shape buildCursorShape(Point2D p) {
		return buildCursorShape(p, 1);
	}

	/**
	 * Builds the cursor shape.
	 * 
	 * @param p
	 *            the p
	 * @param mult
	 *            the mult
	 * @return the shape
	 */
	public Shape buildCursorShape(Point2D p, float mult) {
		Shape shape = null;
		int s = (int) (slCursorSize.getValue() * mult);
		int s1 = s / 2;
		if (cbxCursorShape.getSelectedItem().equals(SHAPE_CIRCLE)) {
			shape = new Ellipse2D.Double(p.getX() - s1, p.getY() - s1, s, s);
		} else if (cbxCursorShape.getSelectedItem().equals(SHAPE_SQUARE)) {
			shape = new Rectangle2D.Double(p.getX() - s1, p.getY() - s1, s, s);
		} else if (cbxCursorShape.getSelectedItem().equals(SHAPE_HORIZONTAL_LINE)) {
			shape = new Rectangle2D.Double(p.getX() - s1, p.getY(), s, 1);
		} else if (cbxCursorShape.getSelectedItem().equals(SHAPE_VERTICAL_LINE)) {
			shape = new Rectangle2D.Double(p.getX(), p.getY() - s1, 1, s);
		}

		return shape;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see plugins.nherve.toolbox.plugin.PainterFactory#createNewPainter()
	 */
	@Override
	public MaskEditorPainter createNewPainter() {
		MaskEditorPainter painter = new MaskEditorPainter();
		Sequence currentSequence = getCurrentSequence();
		painter.setSequence(currentSequence);
		return painter;
	}

	/**
	 * Display file chooser.
	 * 
	 * @param repository
	 *            the repository
	 * @return the file
	 */
	private File displayFileChooser(final MaskPersistence repository, File defaultFile) {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setMultiSelectionEnabled(false);
		fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

		Preferences preferences = Preferences.userRoot().node(PREFERENCES_NODE + "/loadsave");

		if (defaultFile != null) {
			fileChooser.setSelectedFile(defaultFile);
		} else {
			String path = preferences.get("path", "");
			fileChooser.setCurrentDirectory(new File(path));
		}

		fileChooser.setAcceptAllFileFilterUsed(true);
		fileChooser.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().toLowerCase().endsWith(repository.getMaskFileExtension());
			}

			@Override
			public String getDescription() {
				return "Segmentation mask (*" + repository.getMaskFileExtension() + ")";
			}
		});
		fileChooser.setDialogTitle("Choose segmentation file");

		int returnValue = fileChooser.showDialog(null, "OK");

		preferences.putInt("x", fileChooser.getX());
		preferences.putInt("y", fileChooser.getY());
		preferences.putInt("w", fileChooser.getWidth());
		preferences.putInt("h", fileChooser.getHeight());

		if (returnValue == JFileChooser.APPROVE_OPTION) {
			preferences.put("path", fileChooser.getSelectedFile().getAbsolutePath());
			File file = fileChooser.getSelectedFile();
			return file;
		} else {
			return null;
		}
	}

	/**
	 * Display tiff export.
	 * 
	 * @return the file
	 */
	private File displayTiffExport() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setMultiSelectionEnabled(false);
		fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

		Preferences preferences = Preferences.userRoot().node(PREFERENCES_NODE + "/tiffexport");
		String path = preferences.get("path", "");
		fileChooser.setCurrentDirectory(new File(path));

		int x = preferences.getInt("x", 0);
		int y = preferences.getInt("y", 0);
		int w = preferences.getInt("w", 400);
		int h = preferences.getInt("h", 400);

		fileChooser.setLocation(x, y);
		fileChooser.setPreferredSize(new Dimension(w, h));
		fileChooser.setAcceptAllFileFilterUsed(true);
		fileChooser.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().toLowerCase().endsWith(".tiff") || f.getName().toLowerCase().endsWith(".tif");
			}

			@Override
			public String getDescription() {
				return "TIFF files (*.tiff, *.tif)";
			}
		});
		fileChooser.setDialogTitle("Choose TIFF file");

		int returnValue = fileChooser.showDialog(null, "OK");

		preferences.putInt("x", fileChooser.getX());
		preferences.putInt("y", fileChooser.getY());
		preferences.putInt("w", fileChooser.getWidth());
		preferences.putInt("h", fileChooser.getHeight());

		if (returnValue == JFileChooser.APPROVE_OPTION) {
			preferences.put("path", fileChooser.getSelectedFile().getAbsolutePath());
			File file = fileChooser.getSelectedFile();
			return file;
		} else {
			return null;
		}
	}

	/**
	 * Gets the from pool.
	 * 
	 * @return the from pool
	 */
	private MaskStack getFromPool() {
		SwimmingPool sp = Icy.getMainInterface().getSwimmingPool();
		SwimmingObject res = null;
		for (SwimmingObject r : sp.getObjects()) {
			if (r.getObject() instanceof MaskStack) {
				res = r;
				break;
			}
		}

		MaskStack m = null;
		if (res != null) {
			m = (MaskStack) res.getObject();
			sp.remove(res);
		}
		return m;
	}

	/**
	 * Checks if is segmentation in pool.
	 * 
	 * @return true, if is segmentation in pool
	 */
	private boolean isSegmentationInPool() {
		SwimmingPool sp = Icy.getMainInterface().getSwimmingPool();
		for (SwimmingObject r : sp.getObjects()) {
			if (r.getObject() instanceof MaskStack) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Switch opacity off.
	 * 
	 * @return the int
	 */
	public int switchOpacityOff() {
		int bck = slOpacity.getValue();
		slOpacity.setValue(slOpacity.getMinimum());
		return bck;
	}

	/**
	 * Switch opacity on.
	 */
	public void switchOpacityOn() {
		switchOpacityOn(slOpacity.getMaximum());
	}

	/**
	 * Switch opacity on.
	 * 
	 * @param bck
	 *            the bck
	 */
	public void switchOpacityOn(int bck) {
		slOpacity.setValue(bck);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
	 */
	@Override
	public void itemStateChanged(ItemEvent e) {
		Object o = e.getSource();

		if (o == null) {
			return;
		}

		if (o instanceof JCheckBox) {
			JCheckBox c = (JCheckBox) e.getSource();

			if (c == cbDrawEnabled) {
				getCurrentSequence().painterChanged(null);
			} else if (c == cbOnlyContours) {
				for (MaskLayer ml : mll) {
					ml.getMask().setDrawOnlyContours(cbOnlyContours.isSelected());
				}
				getCurrentSequence().painterChanged(null);
			} else if (c == cbViewBgdBox) {
				getCurrentSequence().painterChanged(null);
			} else if (c == cbBlackWhite) {
				getCurrentSequence().painterChanged(null);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * plugins.nherve.toolbox.plugin.BackupSingletonPlugin#restoreCurrentSequence
	 * (boolean)
	 */
	@Override
	public void restoreCurrentSequence(boolean refresh) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * plugins.nherve.toolbox.plugin.BackupAndPainterManagerSingletonPlugin#
	 * sequenceHasChangedAfterSettingPainter()
	 */
	@Override
	public void sequenceHasChangedAfterSettingPainter() {
		if (hasCurrentSequence()) {
			Sequence currentSequence = getCurrentSequence();
			frame.setTitle(PLUGIN_NAME + " - " + currentSequence.getName());
			mll.setStack(getBackupObject());
		} else {
			frame.setTitle(PLUGIN_NAME);
			mll.setStack(null);
		}
		refreshInterface();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * plugins.nherve.toolbox.plugin.BackupAndPainterManagerSingletonPlugin#
	 * sequenceHasChangedBeforeSettingPainter()
	 */
	@Override
	public void sequenceHasChangedBeforeSettingPainter() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see plugins.nherve.toolbox.plugin.SingletonPlugin#sequenceWillChange()
	 */
	@Override
	public void sequenceWillChange() {
		colorMap = null;
	}

	private void reInitColorMap() {
		if (hasCurrentSequence() && (mll.getStack() != null) && (colorMap != null)) {
			if (mll.getStack().getMaxId() >= (colorMap.getNbColors() - 1)) {
				int n = 2 + mll.getStack().getMaxId() / COLORMAP_NBCOLORS;
				colorMap = new DifferentColorsMap(n * COLORMAP_NBCOLORS, n);
			}
		}

		if (colorMap == null) {
			colorMap = new DifferentColorsMap(COLORMAP_NBCOLORS, 1);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see plugins.nherve.toolbox.plugin.SingletonPlugin#startInterface()
	 */
	@Override
	public void startInterface() {
		SwimmingPool sp = Icy.getMainInterface().getSwimmingPool();
		sp.addListener(this);

		JPanel mainPanel = GuiUtil.generatePanel();
		frame = GuiUtil.generateTitleFrame(FULL_PLUGIN_NAME, mainPanel, new Dimension(580, 100), true, true, true, true);
		addIcyFrame(frame);
		new WindowPositionSaver(frame, PREFERENCES_NODE, new Point(0, 0), new Dimension(580, 800));

		// mainPanel.setLayout(new BorderLayout());

		// LOAD / SAVE TOOLS
		btGetSPGlobal = new JButton(NherveToolbox.fromSwimingPoolIcon);
		btGetSPGlobal.setEnabled(isSegmentationInPool());
		btGetSPGlobal.setToolTipText("Get the full stack from the swimming pool");
		btGetSPGlobal.addActionListener(this);
		btLoad = new JButton("Load");
		btLoad.addActionListener(this);
		btSave = new JButton("Save");
		btSave.addActionListener(this);
		btLoad.setEnabled(false);
		btSave.setEnabled(false);
		btSaveFullImage = new JButton("Export");
		btSaveFullImage.setToolTipText("Export current display");
		btSaveFullImage.addActionListener(this);
		btSendSPGlobal = new JButton(NherveToolbox.toSwimingPoolIcon);
		btSendSPGlobal.setToolTipText("Send the full stack to the swimming pool");
		btSendSPGlobal.addActionListener(this);
		JPanel lsTool = GuiUtil.createLineBoxPanel(new Component[] { Box.createHorizontalGlue(), btGetSPGlobal, Box.createHorizontalGlue(), btLoad, Box.createHorizontalGlue(), btSave, Box.createHorizontalGlue(), btSaveFullImage, Box.createHorizontalGlue(), btSendSPGlobal, Box.createHorizontalGlue() });
		lsTool.setBorder(new TitledBorder("Load & Save tools"));
		mainPanel.add(lsTool);

		// DRAWING TOOLS
		cbxCursorShape = new JComboBox();
		cbxCursorShape.addItem(SHAPE_CIRCLE);
		cbxCursorShape.addItem(SHAPE_SQUARE);
		cbxCursorShape.addItem(SHAPE_HORIZONTAL_LINE);
		cbxCursorShape.addItem(SHAPE_VERTICAL_LINE);
		ComponentUtil.setFixedSize(cbxCursorShape, new Dimension(125, 25));
		cbDrawEnabled = new JCheckBox("Draw enabled");
		cbDrawEnabled.addItemListener(this);
		btHelp = new JButton(NherveToolbox.questionIcon);
		btHelp.addActionListener(this);
		JPanel p1 = GuiUtil.createLineBoxPanel(new Component[] { new JLabel("Shape  "), cbxCursorShape, Box.createHorizontalGlue(), cbDrawEnabled, Box.createHorizontalGlue(), btHelp });

		slCursorSize = new JSlider(JSlider.HORIZONTAL, 1, 100, 11);
		slCursorSize.addChangeListener(this);
		slCursorSize.setMajorTickSpacing(10);
		slCursorSize.setMinorTickSpacing(2);
		slCursorSize.setPaintTicks(true);
		cbxCursorShape.setEnabled(false);
		slCursorSize.setEnabled(false);
		JPanel p2 = GuiUtil.createLineBoxPanel(new JLabel("Size  "), slCursorSize);

		JPanel tool = GuiUtil.createPageBoxPanel(p1, p2);
		tool.setBorder(new TitledBorder("Drawing tools"));
		mainPanel.add(tool);

		// DISPLAY TOOLS
		cbOnlyContours = new JCheckBox("Contours");
		cbOnlyContours.setSelected(false);
		cbOnlyContours.addItemListener(this);
		cbViewBgdBox = new JCheckBox("Image");
		cbViewBgdBox.setSelected(true);
		cbViewBgdBox.addItemListener(this);
		cbBlackWhite = new JCheckBox("B / W");
		cbBlackWhite.setSelected(true);
		cbBlackWhite.addItemListener(this);
		btRealColors = new JButton("Real colors");
		btRealColors.addActionListener(this);
		btArtificialColors = new JButton("Artificial colors");
		btArtificialColors.addActionListener(this);
		JPanel p3 = GuiUtil.createLineBoxPanel(new Component[] { cbOnlyContours, Box.createHorizontalGlue(), cbViewBgdBox, Box.createHorizontalGlue(), cbBlackWhite, Box.createHorizontalGlue(), btRealColors, Box.createHorizontalGlue(), btArtificialColors });

		backupOpacity = 50;
		slOpacity = new JSlider(JSlider.HORIZONTAL, 0, 100, backupOpacity);
		slOpacity.addChangeListener(this);
		slOpacity.setMajorTickSpacing(10);
		slOpacity.setMinorTickSpacing(2);
		slOpacity.setPaintTicks(true);
		slOpacity.setEnabled(false);
		JPanel p4 = GuiUtil.createLineBoxPanel(new JLabel("Opacity  "), slOpacity);

		JPanel dspTool = GuiUtil.createPageBoxPanel(p3, p4);
		dspTool.setBorder(new TitledBorder("Display tools"));
		mainPanel.add(dspTool);

		// MASKS LIST
		mll = new MaskLayerList();
		mainPanel.add(mll);

		// FRAME STUFF
		frame.addFrameListener(this);
		frame.setVisible(true);
		frame.pack();
		

		frame.requestFocus();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent
	 * )
	 */
	@Override
	public void stateChanged(ChangeEvent e) {
		Object o = e.getSource();

		if (o == null) {
			return;
		}

		if (o instanceof JSlider) {
			JSlider s = (JSlider) e.getSource();

			if (s == slOpacity) {
				float globalOpacity = getGlobalOpacity();
				for (MaskLayer ml : mll) {
					ml.getMask().setOpacity(globalOpacity);
				}
				getCurrentSequence().painterChanged(null);
			}
		}
	}

	public float getGlobalOpacity() {
		return slOpacity.getValue() / 100f;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see plugins.nherve.toolbox.plugin.SingletonPlugin#stopInterface()
	 */
	@Override
	public void stopInterface() {
		mll.stop();
		removePainterFromAllSequences();
	}

	/**
	 * Gets the running instance.
	 * 
	 * @param forceStart
	 *            the force start
	 * @return the running instance
	 */
	public static MaskEditor getRunningInstance(boolean forceStart) {
		MaskEditor singleton = (MaskEditor) getInstance(MaskEditor.class);
		if (forceStart && (singleton == null)) {
			PluginDescriptor pd = PluginLoader.getPlugin(MaskEditor.class.getName());
			PluginLauncher.launch(pd);
			singleton = (MaskEditor) getInstance(MaskEditor.class);
		}
		return singleton;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see icy.swimmingPool.SwimmingPoolListener#swimmingPoolChangeEvent(icy.
	 * swimmingPool.SwimmingPoolEvent)
	 */
	@Override
	public void swimmingPoolChangeEvent(SwimmingPoolEvent swimmingPoolEvent) {
		Thread ct = Thread.currentThread();

		if (ct.getClass().getName().equals("java.awt.EventDispatchThread")) {
			btGetSPGlobal.setEnabled(isSegmentationInPool());
		} else {
			ThreadUtil.invokeLater(new Runnable() {
				@Override
				public void run() {
					btGetSPGlobal.setEnabled(isSegmentationInPool());
				}
			});
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see plugins.nherve.toolbox.plugin.PainterManager#getPainterName()
	 */
	@Override
	public String getPainterName() {
		return MaskEditorPainter.class.getName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * plugins.nherve.toolbox.plugin.SingletonPlugin#pluginClosed(icy.gui.main
	 * .MainEvent)
	 */
	@Override
	public void pluginClosed(MainEvent arg0) {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * plugins.nherve.toolbox.plugin.SingletonPlugin#pluginOpened(icy.gui.main
	 * .MainEvent)
	 */
	@Override
	public void pluginOpened(MainEvent arg0) {

	}

	/**
	 * Refresh interface.
	 */
	public void refreshInterface() {
		reInitColorMap();
		mll.refreshMLLInterface();
		Sequence s = getCurrentSequence();
		if (s != null) {
			s.painterChanged(null);
		}
	}

	@Override
	public void stackChanged(MaskStack s) {
		if (mll.stack == s) {
			ThreadUtil.invokeLater(new Runnable() {

				@Override
				public void run() {
					refreshInterface();
				}
			});
		}
	}

	@Override
	public void roiAdded(MainEvent event) {
		ROI roi = (ROI) event.getSource();
		if (roi.getFirstSequence() == getCurrentSequence()) {
			if (getCurrentSequence().getROI2Ds().size() > 0) {
				mll.btFromROI.setEnabled(true);
			}
		}
	}

	@Override
	public void roiRemoved(MainEvent event) {
		if (getCurrentSequence().getROI2Ds().size() == 0) {
			mll.btFromROI.setEnabled(false);
		}
	}

}
