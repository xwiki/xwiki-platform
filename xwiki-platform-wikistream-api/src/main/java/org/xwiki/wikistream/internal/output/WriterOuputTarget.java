package org.xwiki.wikistream.internal.output;

import java.io.Writer;

public class WriterOuputTarget implements OuputTarget
{
    private Writer writer;

    public Writer getWriter()
    {
        return writer;
    }
}
