package org.xwiki.gwt.wysiwyg.client.plugin.internal;

import org.xwiki.gwt.user.client.ui.MenuItem;

import com.google.gwt.user.client.ui.UIObject;

/**
 * Concrete implementation of the {@link AbstractUIExtension}. Each feature must have associated a menu item.
 * 
 * @version $Id$
 */
public class MenuItemUIExtension extends AbstractUIExtension
{
    /**
     * Creates a new user interface extension that will extend the specified extension point, a menu, with menu items.
     * 
     * @param role The role of the newly created user interface extension.
     */
    public MenuItemUIExtension(String role)
    {
        super(role);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractUIExtension#isEnabled(String)
     */
    public boolean isEnabled(String feature)
    {
        UIObject uiObject = getUIObject(feature);
        if (uiObject instanceof MenuItem) {
            return ((MenuItem) uiObject).isEnabled();
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractUIExtension#setEnabled(String, boolean)
     */
    public void setEnabled(String feature, boolean enabled)
    {
        UIObject uiObject = getUIObject(feature);
        if (uiObject instanceof MenuItem) {
            ((MenuItem) uiObject).setEnabled(enabled);
        }
    }
}
