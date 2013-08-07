package org.xwiki.wikistream.xml.internal.input.source;

import java.io.IOException;

import javax.xml.transform.Source;

import org.xwiki.wikistream.xml.input.source.SourceInputSource;

public class DefaultSourceInputSource implements SourceInputSource
{
    private Source source;

    public DefaultSourceInputSource(Source source)
    {
        this.source = source;
    }

    @Override
    public void close() throws IOException
    {
        // Source is not closable
    }

    @Override
    public Source getSource()
    {
        return this.source;
    }
}
