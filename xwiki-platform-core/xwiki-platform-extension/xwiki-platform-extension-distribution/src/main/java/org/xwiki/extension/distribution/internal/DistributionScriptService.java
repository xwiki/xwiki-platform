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

import java.util.Arrays;
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
import org.xwiki.extension.internal.safe.ScriptSafeProvider;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.job.event.status.JobStatus.State;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.EventListener;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationManager;
import org.xwiki.script.service.ScriptService;

import com.xpn.xwiki.XWikiContext;

/**
 * Provide helpers to manage running distribution.
 * <p>
 * Note: this script service is strictly internal and intended to be used only from templates for now.
 * 
 * @version $Id$
 * @since 4.2M3
 */
/**
 * @version $Id$
 */
@Component
@Named("distribution")
@Singleton
public class DistributionScriptService implements ScriptService
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

    /**
     * Used to execute transformations.
     */
    @Inject
    private transient TransformationManager transformationManager;

    /**
     * The component used to serialize entity references.
     */
    @Inject
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    @Inject
    @Named(DocumentsModifiedDuringDistributionListener.NAME)
    private EventListener modifiedDocumentsListener;

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

        return getState(xcontext.getDatabase());
    }

    /**
     * @return the current distribution state
     */
    public DistributionState getState(String wiki)
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        return xcontext.isMainWiki(wiki) ? this.distributionManager.getFarmDistributionState() : this.distributionManager
            .getWikiDistributionState(wiki);
    }

    /**
     * @return the extension that defines the current distribution
     */
    public CoreExtension getDistributionExtension()
    {
        return this.distributionManager.getDistributionExtension();
    }

    /**
     * @return the recommended user interface for current wiki
     */
    public ExtensionId getUIExtensionId()
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        return getUIExtensionId(xcontext.getDatabase());
    }

    /**
     * @param wiki the wiki
     * @return the recommended user interface for passed wiki
     */
    public ExtensionId getUIExtensionId(String wiki)
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        return xcontext.isMainWiki(wiki) ? this.distributionManager.getMainUIExtensionId() : this.distributionManager
            .getWikiUIExtensionId();
    }

    /**
     * @return the previous status of the distribution job (e.g. from last time the distribution was upgraded)
     */
    public DistributionJobStatus< ? > getPreviousJobStatus()
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        return xcontext.isMainWiki() ? this.distributionManager.getPreviousFarmJobStatus() : this.distributionManager
            .getPreviousWikiJobStatus(xcontext.getDatabase());
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
    public DistributionJobStatus< ? > getJobStatus()
    {
        DistributionJob job = this.distributionManager.getCurrentDistributionJob();

        return job != null ? (DistributionJobStatus< ? >) job.getStatus() : null;
    }

    /**
     * @return the HTML resulting in the executing of the current step
     */
    public String renderCurrentStepToXHTML()
    {
        String transformationId = null;

        XWikiContext xcontext = xcontextProvider.get();
        if (xcontext != null && xcontext.getDoc() != null) {
            transformationId =
                this.defaultEntityReferenceSerializer.serialize(xcontext.getDoc().getDocumentReference());
        }

        return renderCurrentStepToXHTML(transformationId);
    }

    public String renderCurrentStepToXHTML(String transformationId)
    {
        DistributionJob job = this.distributionManager.getCurrentDistributionJob();

        if (job != null) {
            JobStatus jobStatus = job.getStatus();

            if (jobStatus != null) {
                State jobState = jobStatus.getState();

                if (jobState == State.RUNNING || jobState == State.WAITING) {
                    Block block = job.getCurrentStep().render();

                    transform(block, transformationId);

                    WikiPrinter printer = new DefaultWikiPrinter();

                    this.xhtmlRenderer.render(block, printer);

                    return printer.toString();
                }
            }
        }

        return null;
    }

    private void transform(Block block, String transformationId)
    {
        TransformationContext txContext =
            new TransformationContext(block instanceof XDOM ? (XDOM) block : new XDOM(Arrays.asList(block)), null,
                false);

        txContext.setId(transformationId);

        try {
            this.transformationManager.performTransformations(block, txContext);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the document modified during the Distribution Wizard execution
     * @since 5.4RC1
     */
    public Map<DocumentReference, DocumentStatus> getModifiedDocuments()
    {
        return ((DocumentsModifiedDuringDistributionListener) this.modifiedDocumentsListener).getDocuments().get(
            this.xcontextProvider.get().getDatabase());
    }

    /**
     * @return the document modified during the Distribution Wizard execution
     * @since 5.4RC1
     */
    public Map<String, Map<String, Map<String, Map<String, DocumentStatus>>>> getModifiedDocumentsTree()
    {
        Map<DocumentReference, DocumentStatus> documents =
            ((DocumentsModifiedDuringDistributionListener) this.modifiedDocumentsListener).getDocuments().get(
                this.xcontextProvider.get().getDatabase());

        Map<String, Map<String, Map<String, Map<String, DocumentStatus>>>> tree =
            new TreeMap<String, Map<String, Map<String, Map<String, DocumentStatus>>>>();

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
                    spaces = new TreeMap<String, Map<String, Map<String, DocumentStatus>>>();
                    tree.put(wiki, spaces);
                }

                Map<String, Map<String, DocumentStatus>> pages = spaces.get(space);
                if (pages == null) {
                    pages = new TreeMap<String, Map<String, DocumentStatus>>();
                    spaces.put(space, pages);
                }

                Map<String, DocumentStatus> locales = pages.get(page);
                if (locales == null) {
                    locales = new TreeMap<String, DocumentStatus>();
                    pages.put(page, locales);
                }

                locales.put(locale, document.getValue());
            }
        }

        return tree;
    }
}
