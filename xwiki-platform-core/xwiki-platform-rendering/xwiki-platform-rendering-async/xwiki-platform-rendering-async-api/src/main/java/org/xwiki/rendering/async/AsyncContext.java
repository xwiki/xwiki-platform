package org.xwiki.rendering.async;

import org.xwiki.component.annotation.Role;

/**
 * Contextual information related to asynchronous rendering.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
@Role
public interface AsyncContext
{
    /**
     * @return true if it's allowed to render content asynchronously
     */
    boolean isEnabled();

    /**
     * @param enabled true if it's allowed to render content asynchronously
     */
    void setEnabled(boolean enabled);
}
