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

    @Override
    public URL getURL()
    {
        return getExtension().getURL();
    }

    @Override
    public boolean isGuessed()
    {
        return getExtension().isGuessed();
    }
}
