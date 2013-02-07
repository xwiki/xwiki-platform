package org.xwiki.wikistream.internal.input;

import java.io.InputStream;

public class InputStreamInputSource implements InputSource
{
    private final InputStream inputStream;

    public InputStreamInputSource(InputStream inputStream)
    {
        this.inputStream = inputStream;
    }

    public InputStream getInputStream()
    {
        return this.inputStream;
    }
}
