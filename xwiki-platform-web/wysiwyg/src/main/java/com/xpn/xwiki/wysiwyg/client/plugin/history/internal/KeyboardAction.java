package com.xpn.xwiki.wysiwyg.client.plugin.history.internal;

import com.google.gwt.user.client.ui.KeyboardListener;

/**
 * Types of action the user can do with its keyboard.
 * 
 * @version $Id$
 */
public enum KeyboardAction
{
    /**
     * The user writes text.
     */
    INSERT_WORD(new char[] {}),
    /**
     * The user deletes text.
     */
    DELETE(new char[] {KeyboardListener.KEY_BACKSPACE, KeyboardListener.KEY_DELETE}),
    /**
     * The user inserts in-line white space.
     */
    INSERT_SPACE(new char[] {' ', KeyboardListener.KEY_TAB}),
    /**
     * The user inserts a new line.
     */
    INSERT_NEW_LINE(new char[] {KeyboardListener.KEY_ENTER}),
    /**
     * The user navigates through the text.
     */
    MOVE_CARET(new char[] {KeyboardListener.KEY_DOWN, KeyboardListener.KEY_END, KeyboardListener.KEY_HOME,
        KeyboardListener.KEY_LEFT, KeyboardListener.KEY_PAGEDOWN, KeyboardListener.KEY_PAGEUP,
        KeyboardListener.KEY_RIGHT, KeyboardListener.KEY_UP});

    /**
     * The key codes associated with this keyboard action.
     */
    private final char[] keyCodes;

    /**
     * @param keyCodes The key codes associated with the keyboard action being created.
     */
    private KeyboardAction(char[] keyCodes)
    {
        this.keyCodes = keyCodes;
    }

    /**
     * @param keyCode the code of the key pressed.
     * @param modifiers specify what other keys were pressed.
     * @return true if this keyboard action matches the pressed keys.
     */
    private boolean matches(int keyCode, int modifiers)
    {
        for (char attachedKeyCode : keyCodes) {
            if (keyCode == attachedKeyCode) {
                return true;
            }
        }
        return false;
    }

    /**
     * Iterates through all the keyboard actions in search for one matching the given key code and modifiers. if none is
     * found then {@link #INSERT_WORD} is returned.
     * 
     * @param keyCode the code of the key pressed.
     * @param modifiers specify what other keys were pressed.
     * @return the keyboard action associated with the specified key code and modifiers.
     */
    public static KeyboardAction valueOf(int keyCode, int modifiers)
    {
        for (KeyboardAction action : KeyboardAction.values()) {
            if (action.matches(keyCode, modifiers)) {
                return action;
            }
        }
        return KeyboardAction.INSERT_WORD;
    }
}
