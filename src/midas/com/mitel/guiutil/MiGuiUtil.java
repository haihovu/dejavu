/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.mitel.guiutil;

import com.mitel.miutil.MiExceptionUtil;
import com.mitel.miutil.MiLogMsg.Category;
import com.mitel.miutil.MiSystem;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import javax.swing.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Class utility that provides a number of convenient, GUI-related functionality.
 * @author haiv
 */
public class MiGuiUtil
{

	/**
	 * <p>Determines whether a tree <i>should</i> be in auto scroll mode. The logic is as follows:
	 * Auto scroll is <i>desirable</i> if:</p>
	 * <ul>
	 * <li>No selection is visible</li>
	 * AND
	 * <li>The bottom of the tree is visible</li>
	 * </ul>
	 * <p>MUST be invoked from EDT.</p>
	 * @param tree The target of this query.
	 * @return True if the tree <i>should</i> be in auto scroll mode, false otherwise.
	 */
	public static boolean autoScrollIsDesirable(JTree tree)
	{
		boolean ret = false;
		if(tree.getRowCount() < 1)
		{
			// Empty tree
			return ret;
		}
		Rectangle viewRect = tree.getVisibleRect();
		int[] selected = tree.getSelectionRows();
		boolean selectedIsVisible = false;
		if(selected == null)
		{
			selected = new int[0];
		}
		for(int row : selected)
		{
			if(viewRect.intersects(tree.getRowBounds(row)))
			{
				selectedIsVisible = true;
				break;
			}
		}
		if(!selectedIsVisible)
		{
			Rectangle bottomBound = tree.getRowBounds(tree.getRowCount() - 1);
			ret = viewRect.intersects(bottomBound);
		}
		return ret;
	}

	/**
	 * Centers and makes visible a component (target) against another (anchor).
	 * Thread-safe, may be invoked from any thread.
	 * @param theTarget The target component to be centered.
	 * @param theAnchor The anchor component against which the target is to be centered.
	 * Null means centering the target component on screen.
	 */
	public static void centerAndMakeVisible(Component theTarget, Component theAnchor)
	{
		class SwingTaskMakeVisible implements Runnable
		{
			private final Component target;

			SwingTaskMakeVisible(Component theComponent)
			{
				target = theComponent;
			}

			@Override
			public void run()
			{
				target.setVisible(true);
			}
		}
		centerComponent(theTarget, theAnchor);
		SwingUtilities.invokeLater(new SwingTaskMakeVisible(theTarget));
	}

	/**
	 * Centers a component (target) against another (anchor).
	 * Thread-safe, may be invoked from any thread.
	 * @param theTarget The target component to be centered.
	 * @param theAnchor The anchor component against which the target is to be centered.
	 * Null means centering the target component on screen.
	 */
	public static void centerComponent(Component theTarget, Component theAnchor)
	{
		SwingUtilities.invokeLater(new SwingTaskCenterComponent(theTarget, theAnchor));
	}

	/**
	 * Positions one component (target) next to another one (anchor).
	 * Thread-safe, may be invoked from any thread.
	 * @param theTarget The target component to be positioned and made visible.
	 * @param theAnchor The anchor component against which the target component is positioned.
	 * Null means simply plopping the target at the screen's origin (0,0)
	 */
	public static void makeVisibleNextTo(Component theTarget, Component theAnchor)
	{
		SwingUtilities.invokeLater(new SwingTaskPositionNextTo(theTarget, theAnchor));
	}

	/**
	 * Registers an action against a key stroke for a particular component.
	 * Must be invoked from EDT.
	 * @param component The component against which the key action is registered.
	 * @param when The condition when this action is active (see JComponent.WHEN...)
	 * @param key The key stroke to register.
	 * @param action The action to perform when the key stroke is received.
	 */
	public static void registerKeyAction(JComponent component, int when, KeyStroke key, Action action)
	{
		component.getInputMap(when).put(key, key);
		component.getActionMap().put(key, action);
	}

	/**
	 * A simplified version of another method of the same name for registering a handler for certain key events.
	 * Must be invoked from EDT.
	 * @param component The component against which the key action is registered.
	 * @param keyCode The key code to register, as specified in the KeyEvent class's VK constants.
	 * @param action The action to perform when the key stroke is received.
	 */
	public static void registerKeyAction(JComponent component, int keyCode, Action action)
	{
		registerKeyAction(component, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, KeyStroke.getKeyStroke(keyCode, 0), action);
	}

	/**
	 * A simplified version of another method of the same name for registering a handler for certain key events.
	 * Must be invoked from EDT.
	 * @param component The component against which the key action is registered.
	 * @param keyCode The key code to register, as specified in the KeyEvent class's VK constants.
	 * @param modifiers Combination of zero or more modifiers from InputEvent.<i>XXX</i>_DOWN_MASK
	 * @param action The action to perform when the key stroke is received.
	 */
	public static void registerKeyAction(JComponent component, int keyCode, int modifiers, Action action)
	{
		registerKeyAction(component, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, KeyStroke.getKeyStroke(keyCode, modifiers), action);
	}

	/**
	 * <p>Activate a confirm dialog displaying some specified text. The argument messageType is one of:
	 * JOptionPane.ERROR_MESSAGE, INFORMATION_MESSAGE, PLAIN_MESSAGE, QUESTION_MESSAGE, or WARNING_MESSAGE</p>
	 * May be invoked from any thread.
	 * @param parent The parent component from which the message dialog is spawned.
	 * @param message The message part of the dialog.
	 * @param title The title of the dialog.
	 * @param messageType The dialog type, one of:
	 * @param optionType Option for the JOptionPane.showConfirmDialog() call.
	 * @return The integer value as returned from JOptionPane.showConfirmDialog()
	 * @throws InterruptedException
	 */
	public static int showConfirmDialog(Component parent, Object message, String title, int optionType, int messageType) throws InterruptedException
	{
		if(SwingUtilities.isEventDispatchThread())
		{
			return JOptionPane.showConfirmDialog(parent, message, title, optionType, messageType);
		}
		SwingTaskShowConfirmDialog dialog = new SwingTaskShowConfirmDialog(parent, message, title, optionType, messageType);
		SwingUtilities.invokeLater(dialog);
		return dialog.waitForReturn();
	}

	/**
	 * <p>Activate a message dialog displaying some specified text.</p>
	 * This may be invoked from any thread.
	 * @param parent The parent component from which the message dialog is spawned.
	 * @param messages The messages part of the dialog. Each string represents one line to be displayed.
	 * @param title The title of the dialog.
	 * @param messageType The dialog type, one of:
	 * JOptionPane.ERROR_MESSAGE, INFORMATION_MESSAGE, PLAIN_MESSAGE, QUESTION_MESSAGE, or WARNING_MESSAGE
	 */
	public static void showMessageDialog(Component parent, Object messages, String title, int messageType)
	{
		class activateDialog implements Runnable
		{
			private final Component lCOmponent;
			private final Object lMsgs;
			private final String lTitle;
			private final int lType;

			activateDialog(Component parent, Object msgs, String title, int messageType)
			{
				lCOmponent = parent;
				lMsgs = msgs;
				lTitle = title;
				lType = messageType;
			}

			@Override
			public void run()
			{
				JOptionPane.showMessageDialog(lCOmponent, lMsgs, lTitle, lType);
			}
		}
		if(SwingUtilities.isEventDispatchThread())
		{
			new activateDialog(parent, messages, title, messageType).run();
			return;
		}
		try
		{
			SwingUtilities.invokeAndWait(new activateDialog(parent, messages, title, messageType));
		}
		catch(InterruptedException ex) {
		}
		catch(RuntimeException | InvocationTargetException ex)
		{
			MiSystem.logError(Category.DESIGN, MiExceptionUtil.simpleTrace(ex));
		}
	}

	/**
	 * Extracts the bounds information from a given Frame and returns the XML representation thereof.
	 * Must be invoked from the EDT.
	 * @param theFame The target frame.
	 * @param doc The XML document from which the XML element is to be constructed.
	 * @return The XML construct representing the bounds information from the given frame.
	 */
	public static Element exportBounds(Frame theFame, Document doc)
	{
		Element ret = doc.createElement("Bounds");
		ret.setAttribute("Maximized", String.valueOf(((theFame.getExtendedState() & Frame.MAXIMIZED_BOTH) != 0)));
		Rectangle bounds = theFame.getBounds();
		ret.setAttribute("X", String.valueOf((int)bounds.getX()));
		ret.setAttribute("Y", String.valueOf((int)bounds.getY()));
		ret.setAttribute("Width", String.valueOf((int)bounds.getWidth()));
		ret.setAttribute("Height", String.valueOf((int)bounds.getHeight()));
		return ret;
	}

	/**
	 * Extracts bound information from an XML configuration data and sets it to the given frame.
	 * May be invoked from any thread.
	 * @param theFrame The target frame.
	 * @param configInfo The XML configuration data from which configInfo information is to be extracted.
	 */
	public static void importBounds(Frame theFrame, Element configInfo)
	{
		SwingUtilities.invokeLater(new SwingTaskImportBound(theFrame, configInfo));
	}

	/**
	 * Retrieves the standard error icon (the same one that JOptionPane uses).
	 * @return The error icon.
	 */
	public static Icon getIconError()
	{
		return UIManager.getIcon("OptionPane.errorIcon");
	}

	/**
	 * Retrieves the standard warning icon (the same one that JOptionPane uses).
	 * @return The warning icon.
	 */
	public static Icon getIconWarning()
	{
		return UIManager.getIcon("OptionPane.warningIcon");
	}

	/**
	 * Retrieves the standard information icon (the same one that JOptionPane uses).
	 * @return The information icon.
	 */
	public static Icon getIconInformation()
	{
		return UIManager.getIcon("OptionPane.informationIcon");
	}

	/**
	 * Retrieves the standard question icon (the same one that JOptionPane uses).
	 * @return The question icon.
	 */
	public static Icon getIconQuestion()
	{
		return UIManager.getIcon("OptionPane.questionIcon");
	}
	
	/**
	 * Swing task for importing the state of a frame with data from an XML configuration element.
	 */
	private static class SwingTaskImportBound implements Runnable
	{
		private final Frame lFrame;
		private final Element lConfig;
		/**
		 * Creates a new Swing task.
		 * @param theFrame The frame whose bounds is to be updated.
		 * @param config The XML element containing configuration information.
		 */
		private SwingTaskImportBound(Frame theFrame, Element config)
		{
			lFrame = theFrame;
			lConfig = config;
		}

		@Override
		public void run()
		{
			NodeList els = lConfig.getElementsByTagName("Bounds");
			if(els.getLength() == 0)
			{
				// Check for older format
				els = lConfig.getElementsByTagName("Bound");
			}
			if(els.getLength() > 0)
			{
				Element bel = (Element)els.item(0);
				try
				{
					boolean maximized = Boolean.parseBoolean(bel.getAttribute("Maximized"));
					if(maximized)
					{
						lFrame.setExtendedState(Frame.MAXIMIZED_BOTH);
					}
					else
					{
						int x = Integer.parseInt(bel.getAttribute("X"));
						int y = Integer.parseInt(bel.getAttribute("Y"));
						int w = Integer.parseInt(bel.getAttribute("Width"));
						int h = Integer.parseInt(bel.getAttribute("Height"));
						lFrame.setBounds(new Rectangle(x, y, w, h));
					}
				}
				catch(NumberFormatException e){}
			}
		}
	}
	
	private static class SwingTaskShowConfirmDialog implements Runnable
	{
		private final Component parent;
		private final Object message;
		private final String title;
		private final int optionType;
		private final int messageType;
		private boolean done;
		private int returnValue;
		private SwingTaskShowConfirmDialog(Component parent, Object message, String title, int optionType, int messageType)
		{
			super();
			this.parent = parent;
			this.message = message;
			this.title = title;
			this.optionType = optionType;
			this.messageType = messageType;
		}

		private int waitForReturn() throws InterruptedException
		{
			synchronized(this)
			{
				while(!done)
				{
					wait();
				}
				return returnValue;
			}
		}

		@Override
		public void run()
		{
			int ret = -1;
			try
			{
				ret = JOptionPane.showConfirmDialog(parent, message, title, optionType, messageType);
			}
			catch(RuntimeException e)
			{
				MiSystem.logError(Category.DESIGN, MiExceptionUtil.simpleTrace(e));
			}
			finally
			{
				synchronized(this)
				{
					returnValue = ret;
					done = true;
					notify();
				}
			}
		}
	}

	/**
	 * Swing task for positioning one component (target) next to another one (anchor).
	 */
	private static class SwingTaskPositionNextTo implements Runnable
	{
		private final Component target;
		private final Component anchor;

		/**
		 * Creates a new swing task for positioning one component (target) next to another one (anchor).
		 * @param theTarget The target component to be positioned and made visible.
		 * @param theAnchor The anchor component against which the target component is positioned.
		 * Null means plopping the target at the screen's origin (0,0)
		 */
		private SwingTaskPositionNextTo(Component theTarget, Component theAnchor)
		{
			target = theTarget;
			anchor = theAnchor;
		}

		@Override
		public void run()
		{
			int x;
			int y;
			int parentX = 0;
			int parentY = 0;
			int parentWidth = 0;
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			if(anchor instanceof Frame)
			{
				parentX = anchor.getX();
				parentY = anchor.getY();
				parentWidth = anchor.getWidth();
			}
			else if((anchor != null) && anchor.isVisible())
			{
				Point absoluteOrigin = anchor.getLocationOnScreen();
				parentX = absoluteOrigin.x;
				parentY = absoluteOrigin.y;
				parentWidth = anchor.getWidth();
			}

			y = parentY;
			if(((parentX + parentWidth) + target.getWidth()) > screenSize.width)
			{
				if(parentX > target.getWidth())
				{
					x = parentX - target.getWidth();
				}
				else
				{
					x = screenSize.width - target.getWidth();
				}
			}
			else
			{
				// Enough room to fit on right of parent.
				x = parentX + parentWidth;
			}
			if(x < 0)
			{
				x = 0;
			}
			if(y < 0)
			{
				y = 0;
			}
			target.setLocation(x, y);
			target.setVisible(true);
		}
	}

	/**
	 * Swing task for centering a component (target) against another (anchor).
	 */
	private static class SwingTaskCenterComponent implements Runnable
	{
		private final Component target;
		private final Component anchor;

		/**
		 * Creates a swing task for centering a component (target) against another (anchor).
		 * @param theTarget The target component to be centered.
		 * @param theAnchor The anchor component against which the target is to be centered.
		 * Null means center the target on screen.
		 */
		private SwingTaskCenterComponent(Component theTarget, Component theAnchor)
		{
			target = theTarget;
			anchor = theAnchor;
		}

		@Override
		public void run()
		{
			int x;
			int y;
			if(anchor instanceof Frame)
			{
				x = anchor.getX() + (anchor.getWidth() - target.getWidth()) / 2;
				y = anchor.getY() + (anchor.getHeight() - target.getHeight()) / 2;
			}
			else if((anchor != null) && anchor.isVisible())
			{
				Point absoluteOrigin = anchor.getLocationOnScreen();
				x = absoluteOrigin.x + (anchor.getWidth() - target.getWidth()) / 2;
				y = absoluteOrigin.y + (anchor.getHeight() - target.getHeight()) / 2;
			}
			else
			{
				Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
				x = (screenSize.width - target.getWidth()) / 2;
				y = (screenSize.height - target.getHeight()) / 2;
			}
			if(x < 0)
			{
				x = 0;
			}
			if(y < 0)
			{
				y = 0;
			}
			target.setLocation(x, y);
		}
	}
}
