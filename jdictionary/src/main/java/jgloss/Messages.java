package jgloss;

import java.text.MessageFormat;
import java.util.ResourceBundle;

import jgloss.util.UTF8ResourceBundleControl;

/**
 * Ties together ResourceBundles and MessageFormat to access localizable,
 * customizable messages.
 */
public class Messages {
    private final ResourceBundle messages;

    /**
     * Creates a new Messages object which accesses the given resource bundle.
     * The system default locale will be used.
     *
     * @param bundle Base name of the bundle.
     */
    public Messages( String bundle) {
        messages = ResourceBundle.getBundle(bundle, new UTF8ResourceBundleControl());
    }

    /**
     * Returns the string for the given key.
     *
     * @param key Key for a string in the resource bundle.
     * @return Message for this key.
     */
    public String getString( String key) {
        return messages.getString( key);
    }

    /**
     * Returns the string for the given key, filled in with the given values.
     * MessageFormat is used to parse the string stored in the resource bundle
     * and fill in the Objects. See the documentation for MessageFormat for the
     * used format.
     *
     * @param key Key for a string in the resource bundle.
     * @param data Data to insert in the message.
     * @return Message for this key and data.
     * @see java.text.MessageFormat
     */
    public String getString( String key, Object[] data) {
        return MessageFormat.format( messages.getString( key), data);
    }
} // class messages