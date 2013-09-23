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
package org.xwiki.extension.distribution.internal.job.step;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.distribution.internal.job.step.UpgradeModeDistributionStep.UpgradeMode;
import org.xwiki.extension.repository.InstalledExtensionRepository;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

@Component
@Named(WikisDefaultUIDistributionStep.ID)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class WikisDefaultUIDistributionStep extends AbstractDistributionStep
{
    public static final String ID = "extension.defaultui.wikis";

    @Inject
    private transient InstalledExtensionRepository installedRepository;

    @Inject
    private transient Provider<XWikiContext> xcontextProvider;

    @Inject
    private Logger logger;

    public WikisDefaultUIDistributionStep()
    {
        super(ID);
    }

    @Override
    public void prepare()
    {
        if (getState() == null) {
            setState(State.COMPLETED);

            if (this.distributionManager.getUpgradeMode() == UpgradeMode.ALLINONE) {
                XWikiContext xcontext = this.xcontextProvider.get();

                List<String> wikis;
                try {
                    wikis = xcontext.getWiki().getVirtualWikisDatabaseNames(xcontext);
                } catch (XWikiException e) {
                    this.logger.error("Failed to get the list of wikis", e);
                    setState(null);
                    return;
                }

                ExtensionId wikiExtensionUI = this.distributionManager.getWikiUIExtensionId();

                for (String wiki : wikis) {
                    if (!xcontext.isMainWiki(wiki)) {
                        String namespace = "wiki:" + wiki;

                        // Only if the UI is not already installed
                        if (wikiExtensionUI != null) {
                            InstalledExtension installedExtension =
                                this.installedRepository.getInstalledExtension(wikiExtensionUI.getId(), namespace);
                            if (installedExtension == null
                                || !installedExtension.getId().getVersion().equals(wikiExtensionUI.getVersion())) {
                                setState(null);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }
}
