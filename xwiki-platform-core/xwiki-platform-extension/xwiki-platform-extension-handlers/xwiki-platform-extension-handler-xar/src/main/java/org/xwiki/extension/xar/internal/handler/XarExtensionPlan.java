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
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.job.plan.ExtensionPlan;
import org.xwiki.extension.job.plan.ExtensionPlanAction;
import org.xwiki.extension.job.plan.ExtensionPlanAction.Action;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtension;
import org.xwiki.wikistream.xar.internal.XarEntry;
import org.xwiki.wikistream.xar.internal.XarException;
import org.xwiki.wikistream.xar.internal.XarPackage;

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
                                previousXAREntries.put(wiki, pages);
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
                            Map<XarEntry, LocalExtension> pages = nextXAREntries.get(wiki);
                            if (pages == null) {
                                pages = new HashMap<XarEntry, LocalExtension>();
                                nextXAREntries.put(wiki, pages);
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

    @Override
    public void close() throws IOException
    {
        for (Map<XarEntry, XarExtensionPlanEntry> wikiEntry : previousXAREntries.values()) {
            for (XarExtensionPlanEntry entry : wikiEntry.values()) {
                entry.close();
            }
        }
    }
}
