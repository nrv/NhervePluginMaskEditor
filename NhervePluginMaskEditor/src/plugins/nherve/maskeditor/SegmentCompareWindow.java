package plugins.nherve.maskeditor;

import icy.gui.frame.IcyFrame;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import plugins.nherve.toolbox.image.mask.Mask;
import plugins.nherve.toolbox.image.mask.MaskStack;
import plugins.nherve.toolbox.image.segmentation.SegmentationComparison;

/**
 * The Class SegmentCompareWindow.
 * 
 * @author Nicolas HERVE - nicolas.herve@pasteur.fr
 */
class SegmentCompareWindow extends IcyFrame implements ActionListener {
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
		super(MaskEditor.getRunningInstance(false).getName(), false, true, false, false);

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




