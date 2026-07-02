/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.filter.xar.internal.output;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Locale;

import javax.inject.Inject;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CloseShieldOutputStream;
import org.apache.commons.io.output.DeferredFileOutputStream;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.environment.Environment;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.output.FileOutputTarget;
import org.xwiki.filter.output.OutputStreamOutputTarget;
import org.xwiki.filter.output.OutputTarget;
import org.xwiki.filter.xar.output.XAROutputProperties;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.xar.XarPackage;
import org.xwiki.xar.internal.model.XarModel;

/**
 * @version $Id$
 * @since 6.2M1
 */
@Component(roles = XARWikiWriter.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class XARWikiWriter implements Closeable
{
    /**
     * Threshold in bytes below which entry content is kept in memory. Above this, a temporary file is used.
     */
    private static final int DEFERRED_THRESHOLD = 10_000;

    private String name;

    private XAROutputProperties xarProperties;

    private ZipArchiveOutputStream zipStream;

    private XarPackage xarPackage = new XarPackage();

    private DeferredFileOutputStream currentEntryBuffer;

    private String currentEntryName;

    @Inject
    private Environment environment;

    @Inject
    private Logger logger;

    /**
     * Initialize the writer. Must be called before any other method.
     *
     * @param name the name of the package.
     * @param xarProperties the properties to be used for writing the XAR.
     * @throws FilterException in case of problem for creating the output target.
     */
    public void initialize(String name, XAROutputProperties xarProperties) throws FilterException
    {
        this.name = name;
        this.xarProperties = xarProperties;

        this.xarPackage = new XarPackage();

        this.xarPackage.setPackageName(this.name);
        this.xarPackage.setPackageDescription(xarProperties.getPackageDescription());
        this.xarPackage.setPackageLicense(xarProperties.getPackageLicense());
        this.xarPackage.setPackageAuthor(xarProperties.getPackageAuthor());
        this.xarPackage.setPackageVersion(xarProperties.getPackageVersion());
        this.xarPackage.setPackageBackupPack(xarProperties.isPackageBackupPack());
        this.xarPackage.setPackagePreserveVersion(xarProperties.isPreserveVersion());
        this.xarPackage.setPackageExtensionId(xarProperties.getPackageExtensionId());

        OutputTarget target = this.xarProperties.getTarget();

        try {
            switch (target) {
                case FileOutputTarget fileOutputTarget when fileOutputTarget.getFile().isDirectory() ->
                    this.zipStream =
                        new ZipArchiveOutputStream(new File(fileOutputTarget.getFile(), name + ".xar"));
                case FileOutputTarget fileOutputTarget ->
                    this.zipStream = new ZipArchiveOutputStream(fileOutputTarget.getFile());
                case OutputStreamOutputTarget outputStreamOutputTarget ->
                    this.zipStream = new ZipArchiveOutputStream(
                        CloseShieldOutputStream.wrap(outputStreamOutputTarget.getOutputStream()));
                default ->
                    throw new FilterException(String.format("Unsupported output target [%s]. Only [%s] is supported",
                        target, OutputStreamOutputTarget.class));
            }
        } catch (IOException e) {
            throw new FilterException("Failed to create zip output stream", e);
        }

        this.zipStream.setEncoding("UTF8");

        // By including the unicode extra fields, it is possible to extract XAR-files containing documents with
        // non-ascii characters in the document name using InfoZIP, and the filenames will be correctly
        // converted to the character set of the local file system.
        this.zipStream.setCreateUnicodeExtraFields(ZipArchiveOutputStream.UnicodeExtraFieldPolicy.ALWAYS);
    }

    /**
     * @return the name of the package
     */
    public String getName()
    {
        return this.name;
    }

    private String escapeXARPath(String pathElement)
    {
        return pathElement.replace("%", "%25").replace("/", "%2F").replace("\\", "%5C");
    }

    private void addSpacePath(StringBuilder path, EntityReference spaceReference)
    {
        EntityReference parent = spaceReference.getParent();
        if (parent != null && parent.getType() == EntityType.SPACE) {
            addSpacePath(path, parent);
        }

        path.append(escapeXARPath(spaceReference.getName())).append('/');
    }

    /**
     * Creates a new entry based on the given reference and returns the output stream for writing it.
     *
     * @param reference the reference of the document for creating the entry in the XAR.
     * @return the output stream for writing it.
     * @throws FilterException in case of problem for creating the entry.
     */
    public OutputStream newEntry(LocalDocumentReference reference) throws FilterException
    {
        StringBuilder path = new StringBuilder();

        // Add space path
        addSpacePath(path, reference.getParent());

        // Add document name
        path.append(escapeXARPath(reference.getName()));

        // Add language
        if (reference.getLocale() != null && !reference.getLocale().equals(Locale.ROOT)) {
            path.append('.');
            path.append(reference.getLocale());
        }

        // Add extension
        path.append(".xml");

        String entryName = path.toString();

        this.xarPackage.addEntry(reference, entryName);
        return initializeEntryBuffer(entryName);
    }

    private DeferredFileOutputStream initializeEntryBuffer(String entryName)
    {
        // Buffer entry content in memory (up to DEFERRED_THRESHOLD bytes) or on disk.
        // This is required for Zip64 support on non-seekable output streams as we need to know the uncompressed size
        // before adding it to the ZIP archive.
        this.currentEntryName = entryName;

        File tempDir = this.environment.getTemporaryDirectory();
        this.currentEntryBuffer = DeferredFileOutputStream.builder()
            .setThreshold(DEFERRED_THRESHOLD)
            .setDirectory(tempDir)
            .setPrefix("xar-entry-")
            .get();

        return this.currentEntryBuffer;
    }

    /**
     * Close the archive: no write should be performed after calling this.
     * @throws FilterException in case of problem when closing.
     */
    public void closeEntry() throws FilterException
    {
        try {
            // Flush the entry content to the ZIP archive with a known uncompressed size.
            this.currentEntryBuffer.close();

            long size = this.currentEntryBuffer.isInMemory()
                ? this.currentEntryBuffer.getData().length
                : this.currentEntryBuffer.getFile().length();

            ZipArchiveEntry entry = new ZipArchiveEntry(this.currentEntryName);
            entry.setSize(size);
            this.zipStream.putArchiveEntry(entry);

            if (this.currentEntryBuffer.isInMemory()) {
                this.zipStream.write(this.currentEntryBuffer.getData());
            } else {
                try (InputStream in = this.currentEntryBuffer.toInputStream()) {
                    IOUtils.copyLarge(in, this.zipStream);
                }
            }

            this.zipStream.closeArchiveEntry();
        } catch (IOException e) {
            throw new FilterException("Failed to close zip archive entry", e);
        } finally {
            cleanupCurrentEntryBuffer();
        }
    }

    private void writePackage() throws FilterException
    {
        DeferredFileOutputStream outputStream = initializeEntryBuffer(XarModel.PATH_PACKAGE);
        try {
            // To be safe, use buffering to avoid Zip64 issues.
            this.xarPackage.write(outputStream, this.xarProperties.getEncoding());
            closeEntry();
        } catch (Exception e) {
            throw new FilterException("Failed to write package.xml entry", e);
        } finally {
            // Clean up the entry buffer for error cases. Calling this twice is safe as it does nothing if the buffer
            // has already been closed.
            cleanupCurrentEntryBuffer();
        }
    }

    @Override
    public void close() throws IOException
    {
        // Add package.xml descriptor
        try {
            writePackage();
        } catch (FilterException e) {
            throw new IOException("Failed to write package", e);
        }

        // Close zip stream
        this.zipStream.close();
    }

    private void cleanupCurrentEntryBuffer()
    {
        if (this.currentEntryBuffer != null && !this.currentEntryBuffer.isInMemory()) {
            // Try to close the buffer before deleting the temporary file for error cases where the buffer might
            // still be open.
            IOUtils.closeQuietly(this.currentEntryBuffer);
            File file = this.currentEntryBuffer.getFile();
            if (file != null && file.exists()) {
                try {
                    Files.delete(file.toPath());
                } catch (IOException e) {
                    this.logger.warn("Failed to delete temporary file [{}], root cause: {}", file,
                        ExceptionUtils.getRootCauseMessage(e));
                }
            }
        }
        this.currentEntryBuffer = null;
        this.currentEntryName = null;
    }
}
