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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.distribution.internal.job.step.UpgradeModeDistributionStep.UpgradeMode;
import org.xwiki.extension.repository.InstalledExtensionRepository;

import com.xpn.xwiki.XWikiContext;

@Component
@Named(DefaultUIDistributionStep.ID)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DefaultUIDistributionStep extends AbstractDistributionStep
{
    public static final String ID = "extension.defaultui";

    @Inject
    private transient InstalledExtensionRepository installedRepository;

    @Inject
    private transient Provider<XWikiContext> xcontextProvider;

    public DefaultUIDistributionStep()
    {
        super(ID);
    }

    @Override
    public void prepare()
    {
        if (getState() == null) {
            setState(State.COMPLETED);

            String namespace = getNamespace();

            ExtensionId extensionUI = this.distributionJob.getStatus().getDistributionExtensionUI();

            // Only if the UI is not already installed
            if (extensionUI != null) {
                InstalledExtension installedExtension =
                    this.installedRepository.getInstalledExtension(extensionUI.getId(), namespace);
                if (installedExtension == null
                    || !installedExtension.getId().getVersion().equals(extensionUI.getVersion())) {
                    XWikiContext xcontext = this.xcontextProvider.get();

                    if (!xcontext.isMainWiki(getWiki())) {
                        // Enable the step for empty wikis only
                        if (this.distributionManager.getUpgradeMode() != UpgradeMode.ALLINONE
                            || this.installedRepository.getInstalledExtensions(namespace).isEmpty()) {
                            setState(null);
                        }
                    } else {
                        setState(null);
                    }
                }
            }
        }
    }
}
