package hentrope.runeframe.ui;

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.lang.reflect.Method;

import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;

import hentrope.runeframe.util.OperatingSystem;

public class ExitConfirmDialog {
	private static Method createDialog = null;

	/**
	 * Prompts the user to confirm that he/she intends to exit.
	 * 
	 * This is a close replica of showOptionDialog that customizes the buttons to
	 * show that confirming the dialog will exit the client. It also includes a
	 * fix that prevents the client from becoming partially locked if the dialog is
	 * minimized in a KDE desktop environment.
	 * 
	 * @param parentComponent the <code>Frame</code> in which the dialog is displayed
	 * @param message the <code>Object</code> (usually string) to display
	 * @param title the prompt's title
	 * @param icon      the icon to display in the dialog
	 * @return
	 * @see JOptionPane#showConfirmDialog(java.awt.Component, Object, String, int, int, Icon)
	 * @see JOptionPane#showOptionDialog(java.awt.Component, Object, String, int, int, Icon, Object[], Object)
	 */
	public static int showConfirmDialog(Frame parentComponent,
			Object message, String title, Icon icon) {
		try {
			if (createDialog == null) {
				Class<JOptionPane> clazz = JOptionPane.class;
				createDialog = clazz.getDeclaredMethod("createDialog", Component.class, String.class, int.class);
				createDialog.setAccessible(true);
			}

			Object[] options = new Object[] {"Exit", "Cancel"};
			JOptionPane pane = new JOptionPane(message,
					JOptionPane.WARNING_MESSAGE,
					JOptionPane.OK_CANCEL_OPTION,
					icon,
					options,
					options[1]);

			pane.setInitialValue(options[1]);
			pane.setComponentOrientation(parentComponent.getComponentOrientation());

			JDialog dialog = (JDialog) createDialog.invoke(pane, parentComponent, title, JRootPane.WARNING_DIALOG);

			/*
			 * This fixes an issue caused by being able to minimize the prompt in a
			 * KDE desktop environment. If the prompt loses focus, dispose of it.
			 */
			if (OperatingSystem.getCurrent() == OperatingSystem.LINUX
					&& "kde".equalsIgnoreCase(System.getenv("XDG_CURRENT_DESKTOP")))
				dialog.addWindowFocusListener(new WindowFocusListener() {
					@Override
					public void windowLostFocus(WindowEvent we) {
						if (we.getOppositeWindow() == null)
							dialog.dispose();
					}

					@Override
					public void windowGainedFocus(WindowEvent e) {}
				});

			pane.selectInitialValue();
			dialog.setVisible(true);
			dialog.dispose();

			if (options[0].equals(pane.getValue()))
				return JOptionPane.YES_OPTION;
			else
				return JOptionPane.CLOSED_OPTION;
		} catch (ReflectiveOperationException | SecurityException e) {
			System.err.println("Unable to access JOptionPane.createDialog");
			throw new RuntimeException(e);
		}
	}
}
