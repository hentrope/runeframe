package hentrope.runeframe.ui;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class ExitConfirmationDialog {
	public static final String MESSAGE = "Are you sure you want to quit?",
			TITLE = "Exit Confirmation";
	public static final int OPTION_TYPE = JOptionPane.OK_CANCEL_OPTION,
			MESSAGE_TYPE = JOptionPane.WARNING_MESSAGE;

	private static Dialog dialog;

	public synchronized static void showExitConfirmationDialog(Frame parentComponent) {
		if (dialog == null) {
			dialog = new JDialog(parentComponent, TITLE, false);
			dialog.setResizable(false);
			dialog.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent we) {
					closeDialog();
				}

				@Override
				public void windowIconified(WindowEvent we) {
					System.out.println("Iconified!");
					closeDialog();
				}
			});
			parentComponent.addWindowStateListener(new WindowStateListener() {
				@Override
				public void windowStateChanged(WindowEvent we) {
					System.out.println("State change!");
					if ((we.getNewState() & JFrame.ICONIFIED) != 0)
						closeDialog();
				}
			});

			JButton exit = new JButton("Exit");
			exit.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					System.exit(0);
				}
			});

			JButton cancel = new JButton("Cancel");
			cancel.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					closeDialog();
				}
			});

			JOptionPane pane = new JOptionPane(MESSAGE,
					MESSAGE_TYPE,
					OPTION_TYPE,
					(Icon)null,
					new Object[] {exit, cancel},
					cancel);

			pane.setInitialValue(cancel);
			pane.setComponentOrientation(parentComponent.getComponentOrientation());

			dialog.add(pane);
			dialog.pack();

			pane.selectInitialValue();

		}

		dialog.setLocationRelativeTo(parentComponent);
		dialog.setVisible(true);
	}

	private synchronized static void closeDialog() {
		dialog.setVisible(false);
		dialog.dispose();
		dialog = null;
	}

	//UIManager.getIcon("OptionPane.warningIcon");
	/*Object[] opt = {"Exit","Cancel"};
	if (JOptionPane.showOptionDialog(frame,
			"Are you sure you want to quit?",
			"Exit Confirmation",
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.WARNING_MESSAGE,
			(Icon)null,
			opt,
			opt[1]) == JOptionPane.YES_OPTION)
		System.exit(0);*/
}
