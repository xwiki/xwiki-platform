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
package org.xwiki.extension.xar.internal.handler;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.extension.ExtensionException;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.job.plan.ExtensionPlan;
import org.xwiki.extension.job.plan.ExtensionPlanAction;
import org.xwiki.extension.job.plan.ExtensionPlanAction.Action;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.extension.xar.internal.handler.packager.Packager;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtension;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.xar.XarEntry;
import org.xwiki.xar.XarException;
import org.xwiki.xar.XarPackage;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 * @since 5.4M1
 */
public class XarExtensionPlan implements Closeable
{
    public static final String SESSIONTKEY_XARINSTALLPLAN = "extension.xar.installplan";

    /**
     * Logging helper object.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(XarExtensionPlan.class);

    public final Map<String, Map<XarEntry, XarExtensionPlanEntry>> previousXAREntries;

    /**
     * Map<namespace, >
     */
    public final Map<String, Map<XarEntry, LocalExtension>> nextXAREntries;

    public XarExtensionPlan(ExtensionPlan plan, InstalledExtensionRepository xarRepository,
        LocalExtensionRepository localReposirory) throws ExtensionException, XarException, IOException
    {
        this.previousXAREntries = new HashMap<>();
        this.nextXAREntries = new HashMap<>();

        Map<ExtensionId, XarExtensionPlanEntry> planEntry = new HashMap<>();

        for (ExtensionPlanAction action : plan.getActions()) {
            if (XarExtensionHandler.TYPE.equals(action.getExtension().getType())) {
                // Get previous entries
                Collection<InstalledExtension> previousExtensions = action.getPreviousExtensions();
                for (InstalledExtension previousExtension : previousExtensions) {
                    if (previousExtension != null && previousExtension.getType().equals(XarExtensionHandler.TYPE)) {
                        XarInstalledExtension previousXARExtension =
                            (XarInstalledExtension) xarRepository.getInstalledExtension(previousExtension.getId());

                        if (previousXARExtension == null) {
                            // Very weird situation but let's be safe
                            LOGGER.error("Installed extension [{}] is not properly registered in"
                                + " the virtual XAR installed extensions repository", previousExtension);

                            continue;
                        }

                        XarExtensionPlanEntry xarPlanEntry = planEntry.get(previousXARExtension.getId());
                        if (xarPlanEntry == null) {
                            xarPlanEntry =
                                new XarExtensionPlanEntry(previousXARExtension, previousXARExtension.getXarPackage());
                            planEntry.put(previousXARExtension.getId(), xarPlanEntry);
                        }

                        for (XarEntry entry : previousXARExtension.getXarPackage().getEntries()) {
                            String wiki;
                            try {
                                wiki = XarHandlerUtils.getWikiFromNamespace(action.getNamespace());
                            } catch (UnsupportedNamespaceException e) {
                                throw new ExtensionException("Failed to extract wiki id from namespace", e);
                            }
                            Map<XarEntry, XarExtensionPlanEntry> pages = previousXAREntries.get(wiki);
                            if (pages == null) {
                                pages = new HashMap<>();
                                this.previousXAREntries.put(wiki, pages);
                            }
                            // We want to replace the key too because the type might be different but HashMap keep the old one
                            pages.remove(entry);
                            pages.put(entry, xarPlanEntry);
                        }
                    }
                }

                // Get new entries
                LocalExtension nextExtension = action.getAction() != Action.UNINSTALL && action.getExtension() != null
                    ? localReposirory.getLocalExtension(action.getExtension().getId()) : null;

                if (nextExtension != null) {
                    try {
                        Collection<XarEntry> entries =
                            XarPackage.getEntries(new File(nextExtension.getFile().getAbsolutePath()));

                        for (XarEntry entry : entries) {
                            String wiki;
                            try {
                                wiki = XarHandlerUtils.getWikiFromNamespace(action.getNamespace());
                            } catch (UnsupportedNamespaceException e) {
                                throw new ExtensionException("Failed to extract wiki id from namespace", e);
                            }
                            Map<XarEntry, LocalExtension> pages = this.nextXAREntries.get(wiki);
                            if (pages == null) {
                                pages = new HashMap<>();
                                this.nextXAREntries.put(wiki, pages);
                            }
                            // We want to replace the key too because the type might be different but HashMap keep the old one
                            pages.remove(entry);
                            pages.put(entry, nextExtension);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to parse extension file [{}]", nextExtension.getFile().getAbsolutePath(),
                            e);
                    }
                }
            }
        }
    }

    public XarExtensionPlanEntry getPreviousXarExtensionPlanEntry(DocumentReference documentReference)
    {
        String wiki = documentReference.getWikiReference().getName();
        LocalDocumentReference localDocumentReference = new LocalDocumentReference(documentReference);

        return getPreviousXarExtensionPlanEntry(wiki, localDocumentReference);
    }

    public XarExtensionPlanEntry getPreviousXarExtensionPlanEntry(String wiki,
        LocalDocumentReference localDocumentReference)
    {
        XarEntry xarEntry = new XarEntry(localDocumentReference);

        XarExtensionPlanEntry planEntry = null;

        Map<XarEntry, XarExtensionPlanEntry> wikiEntry = this.previousXAREntries.get(wiki);

        if (wikiEntry != null) {
            planEntry = wikiEntry.get(xarEntry);
        }

        if (planEntry == null) {
            wikiEntry = this.previousXAREntries.get(null);

            if (wikiEntry != null) {
                planEntry = wikiEntry.get(xarEntry);
            }
        }

        return planEntry;
    }

    public XarInstalledExtension getPreviousXarExtension(DocumentReference documentReference)
    {
        String wiki = documentReference.getWikiReference().getName();
        LocalDocumentReference localDocumentReference = new LocalDocumentReference(documentReference);

        return getPreviousXarExtension(wiki, localDocumentReference);
    }

    public XarInstalledExtension getPreviousXarExtension(String wiki, LocalDocumentReference localDocumentReference)
    {
        XarExtensionPlanEntry entry = getPreviousXarExtensionPlanEntry(wiki, localDocumentReference);

        return entry != null ? entry.extension : null;
    }

    public LocalExtension getNextXarExtension(DocumentReference documentReference)
    {
        WikiReference wikiReference = documentReference.getWikiReference();
        LocalDocumentReference localDocumentReference = new LocalDocumentReference(documentReference);

        return getNextXarExtension(wikiReference.getName(), localDocumentReference);
    }

    public LocalExtension getNextXarExtension(String wiki, LocalDocumentReference localDocumentReference)
    {
        XarEntry xarEntry = new XarEntry(localDocumentReference);

        LocalExtension nextExtension = null;

        Map<XarEntry, LocalExtension> wikiEntry = this.nextXAREntries.get(wiki);

        if (wikiEntry != null) {
            nextExtension = wikiEntry.get(xarEntry);
        }

        if (nextExtension == null) {
            wikiEntry = this.nextXAREntries.get(null);

            if (wikiEntry != null) {
                nextExtension = wikiEntry.get(xarEntry);
            }
        }

        return nextExtension;
    }

    public XWikiDocument getPreviousXWikiDocument(DocumentReference documentReference, Packager packager)
        throws XarException, IOException
    {
        WikiReference wikiReference = documentReference.getWikiReference();
        LocalDocumentReference localDocumentReference = new LocalDocumentReference(documentReference);

        return getPreviousXWikiDocument(wikiReference, localDocumentReference, packager);
    }

    public XWikiDocument getPreviousXWikiDocument(WikiReference wikiReference, LocalDocumentReference localReference,
        Packager packager) throws XarException, IOException
    {
        XarExtensionPlanEntry xarPlanEntry = getPreviousXarExtensionPlanEntry(wikiReference.getName(), localReference);

        return xarPlanEntry != null ? packager.getXWikiDocument(wikiReference, localReference, xarPlanEntry.xarFile)
            : null;
    }

    @Override
    public void close() throws IOException
    {
        for (Map<XarEntry, XarExtensionPlanEntry> wikiEntry : this.previousXAREntries.values()) {
            for (XarExtensionPlanEntry entry : wikiEntry.values()) {
                entry.close();
            }
        }
    }

    public boolean containsNewPage(DocumentReference documentReference)
    {
        WikiReference wikiReference = documentReference.getWikiReference();
        LocalDocumentReference localDocumentReference = new LocalDocumentReference(documentReference);

        return containsNewPage(wikiReference, localDocumentReference);
    }

    public boolean containsNewPage(WikiReference wikiReference, LocalDocumentReference localDocumentReference)
    {
        return getNextXarExtension(wikiReference.getName(), localDocumentReference) != null;
    }

    public boolean containsNewPages()
    {
        return !this.nextXAREntries.isEmpty();
    }
}
