package org.xwiki.wikistream.internal;

import org.xwiki.wikistream.WikiStream;
import org.xwiki.wikistream.descriptor.WikiStreamDescriptor;
import org.xwiki.wikistream.type.WikiStreamType;

public abstract class AbstractWikiStream implements WikiStream
{
    protected final WikiStreamType type;

    protected WikiStreamDescriptor descriptor;

    public AbstractWikiStream(WikiStreamType type)
    {
        this.type = type;
    }

    @Override
    public WikiStreamType getType()
    {
        return this.type;
    }

    @Override
    public WikiStreamDescriptor getDescriptor()
    {
        return this.descriptor;
    }

    protected void setDescriptor(WikiStreamDescriptor descriptor)
    {
        this.descriptor = descriptor;
    }
}
