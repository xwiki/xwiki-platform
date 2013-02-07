package org.xwiki.wikistream.internal.input;

import java.io.File;

public class FileInputSource implements InputSource
{
    private final File file;

    public FileInputSource(File file)
    {
        this.file = file;
    }

    public File getFile()
    {
        return this.file;
    }
}
