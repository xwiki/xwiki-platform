package org.xwiki.extension.wrap;

import java.net.URL;

import org.xwiki.extension.CoreExtension;

/**
 * Wrap a Core extension.
 * 
 * @param <T> the extension type
 * @version $Id$
 */
public class WrappingCoreExtension<T extends CoreExtension> extends WrappingExtension<T> implements CoreExtension
{
    /**
     * @param extension the wrapped core extension
     */
    public WrappingCoreExtension(T extension)
    {
        super(extension);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.CoreExtension#getURL()
     */
    public URL getURL()
    {
        return getExtension().getURL();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.CoreExtension#isGuessed()
     */
    public boolean isGuessed()
    {
        return getExtension().isGuessed();
    }
}
