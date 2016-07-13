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
package org.xwiki.wiki.workspacesmigrator.internal;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Default implementation for {@link DocumentRestorerFromAttachedXAR}.
 *
 * @version $Id$
 * @since 5.3RC1
 */
@Component
@Singleton
public class DefaultDocumentRestorerFromAttachedXAR implements DocumentRestorerFromAttachedXAR
{
    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    private File getTemporaryZipFile(DocumentReference docReference, String attachmentName)
        throws XWikiException, IOException
    {
        XWikiContext xcontext = xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();

        // Get the document
        XWikiDocument document = xwiki.getDocument(docReference, xcontext);
        if (document.isNew()) {
            logger.warn("[{}] does not exist", docReference);
            return null;
        }

        // Get the attachment
        XWikiAttachment xar = document.getAttachment(attachmentName);
        if (xar == null) {
            logger.warn("[{}] has no attachment named [{}].", docReference, attachmentName);
            return null;
        }

        // We need to copy the attachment to a temporary file because we want ti use ZipFile
        // instead of ZipArchiveInputStream (see: http://commons.apache.org/proper/commons-compress/zip.html)
        File tempFile = File.createTempFile(attachmentName, ".tmp");
        // We copy the content of the attachment
        FileUtils.copyInputStreamToFile(xar.getContentInputStream(xcontext), tempFile);

        // Return the temp file
        return tempFile;
    }

    @Override
    public void restoreDocumentFromAttachedXAR(DocumentReference docReference, String attachmentName,
            List<DocumentReference> documentsToRestore) throws XWikiException
    {
        XWikiContext xcontext = xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();
        File tempZipFile = null;
        try {
            tempZipFile = getTemporaryZipFile(docReference, attachmentName);
            if (tempZipFile == null) {
                return;
            }
            ZipFile zipFile = new ZipFile(tempZipFile);
            // We look for each document to restore if there is a corresponding zipEntry.
            Iterator<DocumentReference> itDocumentsToRestore = documentsToRestore.iterator();
            while (itDocumentsToRestore.hasNext()) {
                DocumentReference docRef = itDocumentsToRestore.next();

                // Compute what should be the filename of the document to restore
                String fileNameToRestore = String.format("%s/%s.xml", docRef.getLastSpaceReference().getName(),
                        docRef.getName());

                // Get the corresponding zip Entry
                ZipArchiveEntry zipEntry = zipFile.getEntry(fileNameToRestore);
                if (zipEntry != null) {
                    // Restore the document
                    XWikiDocument docToRestore = xwiki.getDocument(docRef, xcontext);
                    docToRestore.fromXML(zipFile.getInputStream(zipEntry));
                    xwiki.saveDocument(docToRestore, xcontext);
                    // We have restored this document
                    itDocumentsToRestore.remove();
                }
            }
            zipFile.close();
        } catch (IOException e) {
            logger.error("Error during the decompression of [{}].", attachmentName, e);
        } finally {
            // Delete the temporary zip file
            if (tempZipFile != null) {
                tempZipFile.delete();
            }
        }
    }


}
