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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.CoreExtension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.distribution.internal.DistributionManager.DistributionState;
import org.xwiki.extension.distribution.internal.job.DistributionJob;
import org.xwiki.extension.distribution.internal.job.DistributionJobStatus;
import org.xwiki.extension.distribution.internal.job.step.UpgradeModeDistributionStep.UpgradeMode;
import org.xwiki.extension.internal.safe.ScriptSafeProvider;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.job.event.status.JobStatus.State;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
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

        return xcontext.isMainWiki() ? this.distributionManager.getFarmDistributionState() : this.distributionManager
            .getWikiDistributionState(xcontext.getDatabase());
    }

    /**
     * @return the extension that defines the current distribution
     */
    public CoreExtension getDistributionExtension()
    {
        return this.distributionManager.getDistributionExtension();
    }

    /**
     * @return the recommended user interface for {@link #getDistributionExtension()}
     */
    public ExtensionId getUIExtensionId()
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        return xcontext.isMainWiki() ? this.distributionManager.getMainUIExtensionId() : this.distributionManager
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
        DistributionJob job = this.distributionManager.getCurrentDistributionJob();

        if (job != null) {
            JobStatus jobStatus = job.getStatus();

            if (jobStatus != null) {
                State jobState = jobStatus.getState();

                if (jobState == State.RUNNING || jobState == State.WAITING) {
                    Block block = job.getCurrentStep().render();

                    WikiPrinter printer = new DefaultWikiPrinter();

                    this.xhtmlRenderer.render(block, printer);

                    return printer.toString();
                }
            }
        }

        return null;
    }

    /**
     * @return the upgrade mode
     * @since 5.0RC1
     */
    public UpgradeMode getUpgradeMode()
    {
        return this.distributionManager.getUpgradeMode();
    }
}
