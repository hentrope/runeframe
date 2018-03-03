package hentrope.runeframe.ui;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowStateListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

import hentrope.runeframe.util.OperatingSystem;

public class ExitConfirmationDialog {
	public static final String MESSAGE = "Are you sure you want to quit?",
			TITLE = "Exit Confirmation";
	public static final int OPTION_TYPE = JOptionPane.OK_CANCEL_OPTION,
			MESSAGE_TYPE = JOptionPane.WARNING_MESSAGE;

	private static Dialog dialog;

	public synchronized static void showExitConfirmationDialog(Frame parentComponent) {
		if (dialog == null) {
			dialog = new JDialog(parentComponent, TITLE, true);
			dialog.setResizable(false);
			dialog.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent we) {
					closeDialog();
				}

				@Override
				public void windowIconified(WindowEvent we) {
					closeDialog();
				}
			});

			if (OperatingSystem.getCurrent() == OperatingSystem.LINUX
					&& "kde".equalsIgnoreCase(System.getenv("XDG_CURRENT_DESKTOP")))
				dialog.addWindowFocusListener(new WindowFocusListener() {
					@Override
					public void windowLostFocus(WindowEvent we) {
						if (we.getOppositeWindow() == null)
							closeDialog();
					}
	
					@Override
					public void windowGainedFocus(WindowEvent e) {}
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
		
		dialog.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent arg0) {
				fixie();
			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
				fixie();
			}

			@Override
			public void mouseExited(MouseEvent arg0) {
				fixie();
			}

			@Override
			public void mousePressed(MouseEvent arg0) {
				fixie();
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
				fixie();
			}
		});

		dialog.setLocationRelativeTo(parentComponent);
		(new Thread(new Runnable() {

			@Override
			public void run() {
				dialog.setVisible(true);
			}
			
		})).start();
	}
	
	private static void fixie() {
		dialog.invalidate();
		dialog.revalidate();
		dialog.repaint();
		System.out.println(dialog.getIgnoreRepaint());
	}

	private synchronized static void closeDialog() {
		dialog.setVisible(false);
		dialog.dispose();
		dialog = null;
	}
}
