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
package org.xwiki.extension.xar.internal.doc;

import java.io.IOException;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.xar.internal.handler.XarExtensionHandler;
import org.xwiki.extension.xar.internal.handler.packager.Packager;
import org.xwiki.extension.xar.internal.job.diff.DocumentUnifiedDiffBuilder;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtension;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtensionRepository;
import org.xwiki.extension.xar.job.diff.DocumentUnifiedDiff;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.xar.XarException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Detects if a document that belongs to an installed extension has customizations.
 * 
 * @version $Id$
 */
@Component(roles = InstalledExtensionDocumentCustomizationDetector.class)
@Singleton
public class InstalledExtensionDocumentCustomizationDetector
{
    @Inject
    private Logger logger;

    @Inject
    private DocumentUnifiedDiffBuilder documentDiffBuilder;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private Packager packager;

    @Inject
    @Named(XarExtensionHandler.TYPE)
    private InstalledExtensionRepository installedXARs;

    /**
     * @param documentReferenceWithLocale a document reference with locale
     * @return {@code true} if the specified document belongs to an installed extension and has customizations
     */
    public boolean isCustomized(DocumentReference documentReferenceWithLocale)
    {
        Collection<XarInstalledExtension> installedExtensions = ((XarInstalledExtensionRepository) this.installedXARs)
            .getXarInstalledExtensions(documentReferenceWithLocale);
        // If the specified document is not part of a XAR then we can't determine if it was customized or not. This
        // is the case with documents created automatically by mandatory document initializers. We assume these
        // documents are customized (in order to not loose the customizations that we can't detect).
        //
        // If the document belongs to multiple extensions we consider it to be customized if it doesn't match any of
        // those extensions.
        return installedExtensions.stream()
            .allMatch(installedExtension -> isCustomized(documentReferenceWithLocale, installedExtension));
    }

    private boolean isCustomized(DocumentReference documentReferenceWithLocale,
        XarInstalledExtension installedExtension)
    {
        XWikiDocument docFromXAR = getDocumentFromXAR(documentReferenceWithLocale, installedExtension);
        XWikiDocument docFromDB = getDocumentFromDatabase(documentReferenceWithLocale);

        DocumentUnifiedDiff documentDiff = this.documentDiffBuilder.diff(docFromXAR, docFromDB);
        return !documentDiff.isEmpty() || !documentDiff.getAttachmentDiffs().isEmpty()
            || !documentDiff.getObjectDiffs().isEmpty() || !documentDiff.getClassPropertyDiffs().isEmpty();

    }

    private XWikiDocument getDocumentFromXAR(DocumentReference documentReferenceWithLocale,
        XarInstalledExtension installedExtension)
    {
        try {
            return this.packager.getXWikiDocument(documentReferenceWithLocale, installedExtension);
        } catch (XarException | IOException e) {
            this.logger.error("Failed to get document [{}] from XAR [{}].", documentReferenceWithLocale,
                installedExtension.getId(), e);
            return null;
        }
    }

    private XWikiDocument getDocumentFromDatabase(DocumentReference documentReferenceWithLocale)
    {
        try {
            XWikiContext xcontext = this.xcontextProvider.get();
            XWikiDocument document = xcontext.getWiki().getDocument(documentReferenceWithLocale, xcontext);
            if (!document.isNew()) {
                return document;
            }
        } catch (XWikiException e) {
            this.logger.error("Failed to get document [{}] from database.", documentReferenceWithLocale, e);
        }

        return null;
    }
}
