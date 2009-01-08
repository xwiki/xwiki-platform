package org.xwiki.rendering.parser.xwiki10;

import org.xwiki.component.logging.AbstractLogEnabled;

public abstract class AbstractFilter extends AbstractLogEnabled implements Filter
{
    int priority;

    public int getPriority()
    {
        return this.priority;
    }
}
