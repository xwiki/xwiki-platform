package org.xwiki.wikistream.internal.output;

import java.io.OutputStream;

public class OutputStreamOuputTarget implements OuputTarget
{
    private final OutputStream outputStream;

    public OutputStreamOuputTarget(OutputStream outputStream)
    {
        this.outputStream = outputStream;
    }

    public OutputStream getOutputStream()
    {
        return outputStream;
    }
}
