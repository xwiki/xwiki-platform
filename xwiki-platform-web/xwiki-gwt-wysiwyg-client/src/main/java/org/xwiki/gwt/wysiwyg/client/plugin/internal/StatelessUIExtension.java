package org.xwiki.gwt.wysiwyg.client.plugin.internal;

/**
 * An user interface extension that cannot be enabled or disabled.
 * 
 * @version $Id$
 */
public class StatelessUIExtension extends AbstractUIExtension
{
    /**
     * Creates a new state less UI extension.
     * 
     * @param role the name of the extension point where the newly created UI extension fits
     */
    public StatelessUIExtension(String role)
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
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractUIExtension#setEnabled(String, boolean)
     */
    public void setEnabled(String feature, boolean enabled)
    {
        // ignore
    }
}
