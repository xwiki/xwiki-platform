package org.xwiki.wikistream.internal.input;

import java.io.InputStream;

public class InputStreamInputSource implements InputSource
{
    private InputStream inputStream;

    public InputStream getInputStream()
    {
        return inputStream;
    }
}
