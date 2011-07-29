package plugins.nherve.maskeditor;

import icy.canvas.IcyCanvas;
import icy.painter.Painter;
import icy.sequence.Sequence;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Area;
import java.awt.geom.Point2D;

import plugins.nherve.toolbox.image.mask.Mask;
import plugins.nherve.toolbox.image.mask.MaskException;
import plugins.nherve.toolbox.image.mask.MaskStack;

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

	private MaskEditor editor;

	/**
	 * Instantiates a new mask editor painter.
	 */
	public MaskEditorPainter() {
		super();
		setSequence(null);
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

			editor.switchLayerDisplay(layer);
		}

		if (((e.getModifiersEx() & (KeyEvent.CTRL_DOWN_MASK)) == KeyEvent.CTRL_DOWN_MASK) && (e.getKeyCode() == KeyEvent.VK_B)) {
			editor.getCbViewBgdBox().setSelected(!editor.getCbViewBgdBox().isSelected());
		}

		if (e.getKeyCode() == KeyEvent.VK_W) {
			editor.switchActiveLayerDisplay();
		}

		if (e.getKeyCode() == KeyEvent.VK_D) {
			editor.getCbDrawEnabled().setSelected(!editor.getCbDrawEnabled().isSelected());
		}

		if (e.getKeyCode() == KeyEvent.VK_X) {
			if (editor.getSlOpacity().getValue() != editor.getSlOpacity().getMaximum()) {
				editor.setBackupOpacity(editor.getSlOpacity().getValue());
				editor.getSlOpacity().setValue(editor.getSlOpacity().getMaximum());
			} else {
				editor.getSlOpacity().setValue(editor.getBackupOpacity());
			}
		}

		if (e.getKeyCode() == KeyEvent.VK_C) {
			if ((e.getModifiersEx() & (KeyEvent.CTRL_DOWN_MASK)) == KeyEvent.CTRL_DOWN_MASK) {
				editor.getSlCursorSize().setValue(editor.getSlCursorSize().getValue() - 1);
			} else {
				editor.getSlCursorSize().setValue(editor.getSlCursorSize().getValue() - 5);
			}
		}

		if (e.getKeyCode() == KeyEvent.VK_V) {
			if ((e.getModifiersEx() & (KeyEvent.CTRL_DOWN_MASK)) == KeyEvent.CTRL_DOWN_MASK) {
				editor.getSlCursorSize().setValue(editor.getSlCursorSize().getValue() + 1);
			} else {
				editor.getSlCursorSize().setValue(editor.getSlCursorSize().getValue() + 5);
			}
		}

		editor.getCurrentSequence().painterChanged(null);
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
	 * @see icy.painter.Painter#mouseClick(java.awt.event.MouseEvent,
	 * java.awt.geom.Point2D, icy.canvas.IcyCanvas)
	 */
	@Override
	public void mouseClick(MouseEvent e, Point2D imagePoint, IcyCanvas canvas) {
		boolean ctrl = (e.getModifiersEx() & (MouseEvent.CTRL_DOWN_MASK)) == MouseEvent.CTRL_DOWN_MASK;

		if (ctrl) {
			if (editor.getCbDrawEnabled().isSelected()) {
				MaskStack stack = editor.getBackupObject();
				Mask currentMask = stack.getActiveMask();
				try {
					currentMask.fillHole((int) imagePoint.getX(), (int) imagePoint.getY());
				} catch (MaskException e1) {
					editor.logError(e1.getClass().getName() + " : " + e1.getMessage());
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

		if (editor.getCbDrawEnabled().isSelected()) {
			MaskStack stack = editor.getBackupObject();
			Mask currentMask = stack.getActiveMask();
			if (currentMask != null) {
				boolean shift = (e.getModifiersEx() & (MouseEvent.SHIFT_DOWN_MASK)) == MouseEvent.SHIFT_DOWN_MASK;

				try {
					Area a = new Area(editor.buildCursorShape(imagePoint));

					if (shift) {
						currentMask.remove(a);
					} else {
						currentMask.add(a);
					}
				} catch (MaskException e1) {
					editor.logError(e1.getClass().getName() + " : " + e1.getMessage());
				}

				e.consume();
				
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
	 * @see icy.painter.Painter#mousePressed(java.awt.event.MouseEvent,
	 * java.awt.geom.Point2D, icy.canvas.IcyCanvas)
	 */
	@Override
	public void mousePressed(MouseEvent e, Point2D imagePoint, IcyCanvas canvas) {
		// TODO due to new canvas2D behaviour
		e.consume();
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.MouseWheelListener#mouseWheelMoved(java.awt.event.
	 * MouseWheelEvent)
	 */
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		// System.out.println("mouseWheelMoved !");
		int oldValue = editor.getSlCursorSize().getValue();
		oldValue += e.getWheelRotation();
		if (oldValue < editor.getSlCursorSize().getMinimum()) {
			oldValue = editor.getSlCursorSize().getMinimum();
		}
		if (oldValue > editor.getSlCursorSize().getMaximum()) {
			oldValue = editor.getSlCursorSize().getMaximum();
		}
		editor.getSlCursorSize().setValue(oldValue);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see icy.painter.Painter#paint(java.awt.Graphics2D,
	 * icy.sequence.Sequence, icy.canvas.IcyCanvas)
	 */
	@Override
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {
		MaskStack stack = editor.getBackupObject(sequence);

		if (!editor.getCbViewBgdBox().isSelected()) {
			if (editor.getCbBlackWhite().isSelected()) {
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
		if ((activeMask != null) && (editor.getCbDrawEnabled().isSelected()) && (currentMousePosition != null) && (currentMousePosition.getX() >= 0) && (currentMousePosition.getX() < sequence.getWidth()) && (currentMousePosition.getY() >= 0) && (currentMousePosition.getY() < sequence.getHeight())) {
			g.setColor(activeMask.getColor());
			Shape s = editor.buildCursorShape(currentMousePosition);

			g.fill(s);

		}
	}

	void setEditor(MaskEditor editor) {
		this.editor = editor;
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

}
