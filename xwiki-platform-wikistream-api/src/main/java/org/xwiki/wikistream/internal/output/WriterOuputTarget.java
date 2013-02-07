package org.xwiki.wikistream.internal.output;

import java.io.Writer;

public class WriterOuputTarget implements OuputTarget
{
    private final Writer writer;

    public WriterOuputTarget(Writer writer)
    {
        this.writer = writer;
    }

    public Writer getWriter()
    {
        return this.writer;
    }
}
