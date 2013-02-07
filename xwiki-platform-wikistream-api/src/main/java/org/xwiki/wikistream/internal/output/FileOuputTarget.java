package org.xwiki.wikistream.internal.output;

import java.io.File;

public class FileOuputTarget implements OuputTarget
{
    private final File file;

    public FileOuputTarget(File file)
    {
        this.file = file;
    }

    public File getFile()
    {
        return file;
    }
}
