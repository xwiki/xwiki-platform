package org.xwiki.observation.remote.converter;

import org.xwiki.component.logging.AbstractLogEnabled;

/**
 * Base class for events converters. Provide a default priority.
 * 
 * @version $Id$
 * @since 2.0M3
 */
public abstract class AbstractEventConverter extends AbstractLogEnabled implements LocalEventConverter,
    RemoteEventConverter
{
    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.remote.converter.LocalEventConverter#getPriority()
     */
    public int getPriority()
    {
        return 1000;
    }
}
