package org.xwiki.extension.xar.internal.handler.packager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.xwiki.model.reference.EntityReference;

public class XarFile
{
    private ZipFile zipFile;

    private Map<XarEntry, XarEntry> entries = new HashMap<XarEntry, XarEntry>();

    public XarFile(File file, Collection<XarEntry> entries) throws ZipException, IOException
    {
        this.zipFile = new ZipFile(file);

        for (XarEntry xarEntry : entries) {
            this.entries.put(xarEntry, xarEntry);
        }
    }

    public void close() throws IOException
    {
        this.zipFile.close();
    }

    public InputStream getInputStream(XarEntry entry) throws IOException
    {
        XarEntry realEntry = this.entries.get(entry);
        if (realEntry == null) {
            throw new IOException("Failed to find entry [" + entry + "]");
        }

        return this.zipFile.getInputStream(realEntry.getZipEntry());
    }

    public Collection<XarEntry> getEntries()
    {
        return this.entries.values();
    }

    public XarEntry getEntry(EntityReference reference, String language)
    {
        return this.entries.get(new XarEntry(reference, language));
    }
}
