package plugins.nherve.maskeditor;

import icy.common.exception.UnsupportedFormatException;
import icy.file.Loader;
import icy.gui.component.ComponentUtil;
import icy.gui.component.ImageComponent;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.roi.ROI2D;
import icy.roi.ROI2DArea;
import icy.sequence.Sequence;
import icy.swimmingPool.SwimmingObject;
import icy.type.TypeUtil;

import java.awt.Dimension;
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
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.TransferHandler;

import loci.formats.FormatException;
import plugins.nherve.toolbox.Algorithm;
import plugins.nherve.toolbox.NherveToolbox;
import plugins.nherve.toolbox.image.BinaryIcyBufferedImage;
import plugins.nherve.toolbox.image.mask.Mask;
import plugins.nherve.toolbox.image.mask.MaskException;
import plugins.nherve.toolbox.image.toolboxes.ColorSpaceTools;
import plugins.nherve.toolbox.image.toolboxes.SomeImageTools;

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
			editor.refreshMLLInterface();
		}

		@Override
		public boolean importData(TransferSupport support) {
			if (!canImport(support)) {
				return false;
			}

			try {
				Transferable tf = support.getTransferable();
				String s = (String) tf.getTransferData(editor.getLocalFlavor());
				MaskLayer toMove = editor.getLayerById(Integer.parseInt(s));
				MaskLayer moveAbove = (MaskLayer) support.getComponent();
				editor.getStack().moveAt(toMove.getMask(), editor.getStack().indexOf(moveAbove.getMask()));
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
			if (!support.isDataFlavorSupported(editor.getLocalFlavor())) {
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
			return new DataFlavor[] { editor.getLocalFlavor() };
		}

		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return flavor.equals(editor.getLocalFlavor());
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
	
	private JButton btLoad;

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
	
	private MaskEditor editor;

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
				btColor.setBackground(JColorChooser.showDialog(editor.getFrame().getFrame(), "Choose current mask color", btColor.getBackground()));
				mask.setColor(btColor.getBackground());
				editor.getCurrentSequence().painterChanged(null);
			}
			if (b == btLocalRealColors) {
				mask.setColor(mask.getAverageColor(editor.getCurrentSequence().getFirstImage()));
				btColor.setBackground(mask.getColor());
				editor.getCurrentSequence().painterChanged(null);
				btPop.getComponentPopupMenu().setVisible(false);
			}
			if (b == btExportToBlack) {
				IcyBufferedImage toSave = mask.getBinaryData().asIcyBufferedImage(1, false);
				Sequence s = new Sequence(toSave);
				editor.addSequence(s);
				btPop.getComponentPopupMenu().setVisible(false);
			}
			if (b == btExportToWhite) {
				IcyBufferedImage toSave = mask.getBinaryData().asIcyBufferedImage(1, true);
				Sequence s = new Sequence(toSave);
				editor.addSequence(s);
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
				Algorithm.out("Some info on " + mask + " : surface = " + nbon + " (" + df.format(pct) + " %)");
				btPop.getComponentPopupMenu().setVisible(false);
			}
			if (b == btSave) {
				File f = editor.displayTiffChooser();
				if (f != null) {
					try {
						SomeImageTools.save(mask.getBinaryData(), f, ColorSpaceTools.NB_COLOR_CHANNELS);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
				btPop.getComponentPopupMenu().setVisible(false);
			}
			if (b == btLoad) {
				File f = editor.displayTiffChooser();
				if (f != null) {
					try {
						IcyBufferedImage im = Loader.loadImage(f);
						im = im.convertToType(TypeUtil.TYPE_DOUBLE, false, false);
						BinaryIcyBufferedImage bin = SomeImageTools.toMask(im);
						mask.setBinaryData(bin);
					} catch (IOException e1) {
						e1.printStackTrace();
					} catch (UnsupportedFormatException e1) {
						e1.printStackTrace();
					}
					editor.getCurrentSequence().painterChanged(null);
				}
				btPop.getComponentPopupMenu().setVisible(false);
			}
			if (b == btAsROI) {
				ROI2DArea a = mask.asROI2DArea(editor.getCurrentSequence());
				if (a != null) {
					a.setName("From mask " + mask.getLabel());
				}
				btPop.getComponentPopupMenu().setVisible(false);
			}
			if (b == btROIPlus) {
				for (ROI2D roi : editor.getCurrentSequence().getROI2Ds()) {
					if (roi.isSelected()) {
						try {
							mask.add(roi);
						} catch (MaskException e1) {
							editor.logError(e1.getClass().getName() + " : " + e1.getMessage());
						}
					}
				}
				editor.refreshInterface();
				btPop.getComponentPopupMenu().setVisible(false);
			}
			if (b == btROIMinus) {
				for (ROI2D roi : editor.getCurrentSequence().getROI2Ds()) {
					if (roi.isSelected()) {
						try {
							mask.remove(roi);
						} catch (MaskException e1) {
							editor.logError(e1.getClass().getName() + " : " + e1.getMessage());
						}
					}
				}
				editor.refreshInterface();
				btPop.getComponentPopupMenu().setVisible(false);
			}
			if (b == btDelete) {
				editor.getStack().remove(mask);
				editor.refreshInterface();
			}
			if (b == btUp) {
				// reverse display
				editor.getStack().moveDown(mask);
				editor.refreshInterface();
			}
			if (b == btDown) {
				// reverse display
				editor.getStack().moveUp(mask);
				editor.refreshInterface();
			}
			if (b == btAdd) {
				try {
					editor.getStack().addPreviousInStack(mask);
				} catch (MaskException e1) {
					editor.logError(e1.getClass().getName() + " : " + e1.getMessage());
				}
				editor.refreshInterface();
			}
			if (b == btIntersect) {
				try {
					editor.getStack().intersectPreviousInStack(mask);
				} catch (MaskException e1) {
					editor.logError(e1.getClass().getName() + " : " + e1.getMessage());
				}
				editor.refreshInterface();
			}
		}

		if (o instanceof JRadioButton) {
			JRadioButton b = (JRadioButton) e.getSource();

			if (b == btActive) {
				editor.getStack().setActiveMask(mask);
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
				editor.getCurrentSequence().painterChanged(null);
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
		
		btLoad = new JButton(NherveToolbox.loadIcon);
		btLoad.setToolTipText("Import mask from TIFF");
		btLoad.addActionListener(this);
		pop.add(btLoad);

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

	MaskEditor getEditor() {
		return editor;
	}

	void setEditor(MaskEditor editor) {
		this.editor = editor;
	}

	JCheckBox getCbView() {
		return cbView;
	}

}
