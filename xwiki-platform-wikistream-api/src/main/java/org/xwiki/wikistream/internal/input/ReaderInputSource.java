package org.xwiki.wikistream.internal.input;

import java.io.Reader;

public class ReaderInputSource implements InputSource
{
    private final Reader reader;

    public ReaderInputSource(Reader reader)
    {
        this.reader = reader;
    }

    public Reader getReader()
    {
        return this.reader;
    }
}
