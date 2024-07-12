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
package org.xwiki.extension.distribution.internal;

import java.util.Map;
import java.util.TreeMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.CoreExtension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.distribution.internal.DistributionManager.DistributionState;
import org.xwiki.extension.distribution.internal.DocumentsModifiedDuringDistributionListener.DocumentStatus;
import org.xwiki.extension.distribution.internal.job.DistributionJob;
import org.xwiki.extension.distribution.internal.job.DistributionJobStatus;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.job.event.status.JobStatus.State;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.script.safe.ScriptSafeProvider;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.text.StringUtils;

import com.xpn.xwiki.XWikiContext;

/**
 * Provide helpers to manage running distribution.
 * <p>
 * Note: this script service is strictly internal and intended to be used only from templates for now.
 * 
 * @version $Id$
 * @since 4.2M3
 */
@Component
@Named("distribution")
@Singleton
public class DistributionInternalScriptService implements ScriptService
{
    /**
     * The key under which the last encountered error is stored in the current execution context.
     */
    public static final String EXTENSIONERROR_KEY = "scriptservice.distribution.error";

    /**
     * The component used to get information about the current distribution.
     */
    @Inject
    protected DistributionManager distributionManager;

    /**
     * Used to access current {@link XWikiContext}.
     */
    @Inject
    protected Provider<XWikiContext> xcontextProvider;

    /**
     * Provides safe objects for scripts.
     */
    @Inject
    @SuppressWarnings("rawtypes")
    private ScriptSafeProvider scriptProvider;

    /**
     * Used to access HTML renderer.
     */
    @Inject
    @Named("xhtml/1.0")
    private BlockRenderer xhtmlRenderer;

    @Inject
    @Named(DocumentsModifiedDuringDistributionListener.NAME)
    private EventListener modifiedDocumentsListener;

    @Inject
    private RenderingContext renderingContext;

    @Inject
    private ContextualAuthorizationManager authorization;

    /**
     * @param <T> the type of the object
     * @param unsafe the unsafe object
     * @return the safe version of the passed object
     */
    @SuppressWarnings("unchecked")
    private <T> T safe(T unsafe)
    {
        return (T) this.scriptProvider.get(unsafe);
    }

    // Distribution

    /**
     * @return the current distribution state
     */
    public DistributionState getState()
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        return getState(xcontext.getWikiId());
    }

    /**
     * @return the current distribution state
     */
    public DistributionState getState(String wiki)
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        return xcontext.isMainWiki(wiki) ? this.distributionManager.getFarmDistributionState()
            : this.distributionManager.getWikiDistributionState(wiki);
    }

    /**
     * @return the extension that defines the current distribution
     */
    public CoreExtension getDistributionExtension()
    {
        return this.distributionManager.getDistributionExtension();
    }

    /**
     * @return if the main wiki has a default UI configured
     */
    public boolean hasMainDefaultUIExtension()
    {
        ExtensionId extension = this.distributionManager.getMainUIExtensionId();
        return extension != null && StringUtils.isNotBlank(extension.getId());
    }

    /**
     * @return if wikis have a default UI configured
     */
    public boolean hasWikiDefaultUIExtension()
    {
        ExtensionId extension = this.distributionManager.getWikiUIExtensionId();
        return extension != null && StringUtils.isNotBlank(extension.getId());
    }

    /**
     * @return the recommended user interface for current wiki
     */
    public ExtensionId getUIExtensionId()
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        return getUIExtensionId(xcontext.getWikiId());
    }

    /**
     * @param wiki the wiki
     * @return the recommended user interface for passed wiki
     */
    public ExtensionId getUIExtensionId(String wiki)
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        return xcontext.isMainWiki(wiki) ? this.distributionManager.getMainUIExtensionId()
            : this.distributionManager.getWikiUIExtensionId();
    }

    /**
     * @return the previous status of the distribution job for the current wiki (e.g. from last time the distribution
     *         was upgraded)
     */
    public DistributionJobStatus getPreviousJobStatus()
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        return getPreviousJobStatus(xcontext.getWikiId());
    }

    /**
     * @param wiki the wiki for which to retrieve the previous status of the distribution job
     * @return the previous status of the distribution job for the specified wiki (e.g. from last time the distribution
     *         was upgraded)
     */
    public DistributionJobStatus getPreviousJobStatus(String wiki)
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        return xcontext.isMainWiki(wiki) ? this.distributionManager.getPreviousFarmJobStatus()
            : this.distributionManager.getPreviousWikiJobStatus(wiki);
    }

    /**
     * @return indicate of it's allowed to display the Distribution Wizard in the current context
     */
    public boolean canDisplayDistributionWizard()
    {
        return this.distributionManager.canDisplayDistributionWizard();
    }

    /**
     * @return the status of the current distribution job
     */
    public DistributionJobStatus getJobStatus()
    {
        DistributionJob job = this.distributionManager.getCurrentDistributionJob();

        return job != null ? (DistributionJobStatus) job.getStatus() : null;
    }

    /**
     * @return the HTML resulting in the executing of the current step
     */
    public String renderCurrentStepToXHTML()
    {
        return renderCurrentStepToXHTML(this.renderingContext.getTransformationId());
    }

    public String renderCurrentStepToXHTML(String transformationId)
    {
        DistributionJob job = this.distributionManager.getCurrentDistributionJob();

        if (job != null) {
            JobStatus jobStatus = job.getStatus();

            if (jobStatus != null) {
                State jobState = jobStatus.getState();

                if (jobState == State.RUNNING || jobState == State.WAITING) {
                    Block block = job.getCurrentStep().executeInteractive();

                    WikiPrinter printer = new DefaultWikiPrinter();

                    this.xhtmlRenderer.render(block, printer);

                    return printer.toString();
                }
            }
        }

        return null;
    }

    /**
     * @return the document modified during the Distribution Wizard execution
     * @since 5.4RC1
     */
    public Map<DocumentReference, DocumentStatus> getModifiedDocuments()
    {
        return ((DocumentsModifiedDuringDistributionListener) this.modifiedDocumentsListener).getDocuments()
            .get(this.xcontextProvider.get().getWikiId());
    }

    /**
     * @return the document modified during the Distribution Wizard execution
     * @since 5.4RC1
     */
    public Map<String, Map<String, Map<String, Map<String, DocumentStatus>>>> getModifiedDocumentsTree()
    {
        Map<DocumentReference, DocumentStatus> documents =
            ((DocumentsModifiedDuringDistributionListener) this.modifiedDocumentsListener).getDocuments()
                .get(this.xcontextProvider.get().getWikiId());

        Map<String, Map<String, Map<String, Map<String, DocumentStatus>>>> tree = new TreeMap<>();

        if (documents != null) {
            for (Map.Entry<DocumentReference, DocumentStatus> document : documents.entrySet()) {
                DocumentReference reference = document.getKey();
                String wiki = reference.getWikiReference().getName();
                // TODO: add support for subspaces
                String space = reference.getLastSpaceReference().getName();
                String page = reference.getName();
                String locale = reference.getLocale() != null ? reference.getLocale().toString() : "";

                Map<String, Map<String, Map<String, DocumentStatus>>> spaces = tree.get(wiki);
                if (spaces == null) {
                    spaces = new TreeMap<>();
                    tree.put(wiki, spaces);
                }

                Map<String, Map<String, DocumentStatus>> pages = spaces.get(space);
                if (pages == null) {
                    pages = new TreeMap<>();
                    spaces.put(space, pages);
                }

                Map<String, DocumentStatus> locales = pages.get(page);
                if (locales == null) {
                    locales = new TreeMap<>();
                    pages.put(page, locales);
                }

                locales.put(locale, document.getValue());
            }
        }

        return tree;
    }

    /**
     * @since 11.7RC1
     * @since 11.3.3
     * @since 10.11.10
     */
    public void setProperty(String key, Object value)
    {
        if (this.authorization.hasAccess(Right.PROGRAM)) {
            DistributionJob job = this.distributionManager.getCurrentDistributionJob();

            if (job != null) {
                job.setProperty(key, value);
            }
        }
    }

    /**
     * @since 11.7RC1
     * @since 11.3.3
     * @since 10.11.10
     */
    public Object getProperty(String key)
    {
        if (this.authorization.hasAccess(Right.PROGRAM)) {
            DistributionJob job = this.distributionManager.getCurrentDistributionJob();

            if (job != null) {
                return job.getProperty(key);
            }
        }

        return null;
    }

    /**
     * Remove a stored property.
     *
     * @param key the key of the property to be removed.
     * @since 11.10.6
     * @since 12.4
     */
    public void removeProperty(String key)
    {
        if (this.authorization.hasAccess(Right.PROGRAM)) {
            DistributionJob job = this.distributionManager.getCurrentDistributionJob();

            if (job != null) {
                job.removeProperty(key);
            }
        }
    }
}
