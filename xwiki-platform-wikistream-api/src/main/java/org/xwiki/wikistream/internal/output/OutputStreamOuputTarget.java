package org.xwiki.wikistream.internal.output;

import java.io.OutputStream;

public class OutputStreamOuputTarget implements OuputTarget
{
    private OutputStream outputStream;

    public OutputStream getOutputStream()
    {
        return outputStream;
    }
}
