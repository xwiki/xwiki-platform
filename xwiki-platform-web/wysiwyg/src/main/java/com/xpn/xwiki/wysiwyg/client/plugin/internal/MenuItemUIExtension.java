package com.xpn.xwiki.wysiwyg.client.plugin.internal;

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
        // TODO
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractUIExtension#setEnabled(String, boolean)
     */
    public void setEnabled(String feature, boolean enabled)
    {
        // TODO
    }
}
