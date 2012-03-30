package jgloss.ui;

import java.awt.AWTKeyStroke;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 * Forward certain keystrokes received by a focused component to other components.
 * To use this class, register an instance of <code>KeystrokeForwarder</code> as
 * <code>KeyListener</code> on a source component. Then use the 
 * {@link #registerKeystroke(AWTKeyStroke,Component) registerKeystroke} and
 * {@link #addTarget(JComponent) addTarget} methods
 * to register keystrokes which are forwarded when the source component receives them.
 * Forwarded keystroke events are always consumed and will not reach the source component.
 */
public class KeystrokeForwarder implements KeyListener {
    private final Map<AWTKeyStroke, Component> forwardedKeystrokes;

    public KeystrokeForwarder() {
        forwardedKeystrokes = new HashMap<AWTKeyStroke, Component>();
    }

    /**
     * Register all keystrokes defined in the component input map for forwarding to the
     * given component. Keystrokes defined in the parent input maps are not registered.
     */
    public void addTarget(JComponent target) {
        KeyStroke[] keys = target.getInputMap().keys();
        for (KeyStroke key : keys) {
	        registerKeystroke(key, target);
        }
    }

    /**
     * Register a keystroke to be forwarded to the target component. If the keystroke is already
     * registered, the old target will be removed.
     */
    public void registerKeystroke(AWTKeyStroke keystroke, Component target) {
        forwardedKeystrokes.put(keystroke, target);
    }

    @Override
	public void keyPressed(KeyEvent event) {
        forwardKeyEvent(event);
    }

    @Override
	public void keyReleased(KeyEvent event) {
        forwardKeyEvent(event);
    }
    
    @Override
	public void keyTyped(KeyEvent event) {
        forwardKeyEvent(event);
    }

    /**
     * Forward a key event, if a target component is registered to the keystroke corresponding
     * to the event. If the event is forwarded, it will always be consumed.
     */
    protected void forwardKeyEvent(KeyEvent event) {
        Component target = forwardedKeystrokes.get
            (AWTKeyStroke.getAWTKeyStrokeForEvent(event));
        if (target != null) {
            target.dispatchEvent(event);
            event.consume();
        }
    }
} // class KeystrokeForwarder
