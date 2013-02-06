package org.xwiki.wikistream.internal.input;

import java.io.Reader;

public class ReaderInputSource implements InputSource
{
    private Reader reader;

    public Reader getReader()
    {
        return reader;
    }
}
