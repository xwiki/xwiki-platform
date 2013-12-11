package org.xwiki.extension.xar.internal.handler;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.extension.ExtensionException;
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
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.xar.internal.XarEntry;
import org.xwiki.wikistream.xar.internal.XarException;
import org.xwiki.wikistream.xar.internal.XarPackage;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 * @since 5.4M1
 */
public class XarExtensionPlan implements Closeable
{
    public static final String CONTEXTKEY_XARINSTALLPLAN = "extension.xar.installplan";

    /**
     * Logging helper object.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(XarExtensionPlan.class);

    public final Map<String, Map<XarEntry, XarExtensionPlanEntry>> previousXAREntries;

    public final Map<String, Map<XarEntry, LocalExtension>> nextXAREntries;

    public XarExtensionPlan(ExtensionPlan plan, InstalledExtensionRepository xarRepository,
        LocalExtensionRepository localReposirory) throws ExtensionException, XarException, IOException
    {
        this.previousXAREntries = new HashMap<String, Map<XarEntry, XarExtensionPlanEntry>>();
        this.nextXAREntries = new HashMap<String, Map<XarEntry, LocalExtension>>();

        for (ExtensionPlanAction action : plan.getActions()) {
            if (action.getExtension().getType().equals(XarExtensionHandler.TYPE)) {
                // Get previous entries
                Collection<InstalledExtension> previousExtensions = action.getPreviousExtensions();
                for (InstalledExtension previousExtension : previousExtensions) {
                    if (previousExtension != null) {
                        XarInstalledExtension previousXARExtension =
                            (XarInstalledExtension) xarRepository.getInstalledExtension(previousExtension.getId());

                        for (XarEntry entry : previousXARExtension.getXarPackage().getEntries()) {
                            String wiki;
                            try {
                                wiki = XarHandlerUtils.getWikiFromNamespace(action.getNamespace());
                            } catch (UnsupportedNamespaceException e) {
                                throw new ExtensionException("Failed to extract wiki id from namespace", e);
                            }
                            Map<XarEntry, XarExtensionPlanEntry> pages = previousXAREntries.get(wiki);
                            if (pages == null) {
                                pages = new HashMap<XarEntry, XarExtensionPlanEntry>();
                                this.previousXAREntries.put(wiki, pages);
                            }
                            pages.put(entry, new XarExtensionPlanEntry(previousXARExtension));
                        }
                    }
                }

                // Get new entries
                LocalExtension nextExtension =
                    action.getAction() != Action.UNINSTALL && action.getExtension() != null ? localReposirory
                        .getLocalExtension(action.getExtension().getId()) : null;

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
                                pages = new HashMap<XarEntry, LocalExtension>();
                                this.nextXAREntries.put(wiki, pages);
                            }
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

    public XWikiDocument getPreviousXWikiDocument(DocumentReference documentReference, Packager packager)
        throws WikiStreamException, ComponentLookupException, IOException
    {
        WikiReference wikiReference = documentReference.getWikiReference();
        LocalDocumentReference localDocumentReference = new LocalDocumentReference(documentReference);

        return getPreviousXWikiDocument(wikiReference, localDocumentReference, packager);
    }

    public XWikiDocument getPreviousXWikiDocument(WikiReference wikiReference, LocalDocumentReference localReference,
        Packager packager) throws WikiStreamException, ComponentLookupException, IOException
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
}
