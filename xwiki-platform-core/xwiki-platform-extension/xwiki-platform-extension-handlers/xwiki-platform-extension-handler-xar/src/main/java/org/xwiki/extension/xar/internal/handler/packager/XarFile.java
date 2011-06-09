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

    private Map<EntityReference, XarEntry> entries = new HashMap<EntityReference, XarEntry>();

    public XarFile(File file, Collection<XarEntry> entries) throws ZipException, IOException
    {
        this.zipFile = new ZipFile(file);

        for (XarEntry xarEntry : entries) {
            this.entries.put(xarEntry.getDocumentReference(), xarEntry);
        }
    }

    public void close() throws IOException
    {
        this.zipFile.close();
    }

    public InputStream getInputStream(XarEntry entry) throws IOException
    {
        return this.zipFile.getInputStream(entry.getZipEntry());
    }

    public Collection<XarEntry> getEntries()
    {
        return this.entries.values();
    }

    public XarEntry getEntry(EntityReference reference)
    {
        return this.entries.get(reference);
    }
}
