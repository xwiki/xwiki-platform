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
package org.xwiki.distributionwizard.internal.steps;

import java.io.Serializable;
import java.util.Map;

import org.xwiki.component.annotation.Component;
import org.xwiki.distributionwizard.DistributionWizardException;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.rightsmanager.RightsManager;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

@Component
@Singleton
@Named("FirstAdminUserStep")
@Priority(10)
public class FirstAdminUserStep extends AbstractStep
{
    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Override
    public boolean needsInput()
    {
        return true;
    }

    @Override
    public boolean isStepDone() throws DistributionWizardException
    {
        try {
            return RightsManager.getInstance().countAllGlobalUsersOrGroups(true, null,
                this.contextProvider.get()) > 0;
        } catch (XWikiException e) {
            throw new DistributionWizardException("Error when trying to compute if the step is done", e);
        }
    }

    @Override
    public void processStep(Map<String, Serializable> data) throws DistributionWizardException
    {
        XWikiContext context = this.contextProvider.get();
        String username = String.valueOf(data.get("username"));
        try {
            // create user
            int result = context.getWiki().createUser(username, data, context);
            if (result == 1) {
                // assign the user to own the wiki
                WikiDescriptor wikiDescriptor = this.wikiDescriptorManager.getCurrentWikiDescriptor();
                wikiDescriptor.setOwnerId("XWiki." + username);
                this.wikiDescriptorManager.saveDescriptor(wikiDescriptor);
                // login the user
                context.getWiki().getAuthService()
                    .checkAuth(username, String.valueOf(data.get("password")), "true", context);
                completeJobStep();
            } else {
                throw new DistributionWizardException(String.format("Error while registering first admin. Error code: "
                    + "[%s]", result));
            }
        } catch (XWikiException e) {
            throw new DistributionWizardException("Unhandled error while registering first admin user", e);
        } catch (WikiManagerException e) {
            throw new DistributionWizardException("Error while setting user as owner of the wiki descriptor", e);
        }
    }

    @Override
    public Map<String, Serializable> getStepDoneInformation() throws DistributionWizardException
    {
        try {
            WikiDescriptor wikiDescriptor = this.wikiDescriptorManager.getCurrentWikiDescriptor();
            return Map.of("ownerId", wikiDescriptor.getOwnerId());
        } catch (WikiManagerException e) {
            throw new DistributionWizardException("Error while getting wiki descriptor", e);
        }
    }

    @Override
    protected String getJobStepId()
    {
        return org.xwiki.extension.distribution.internal.job.step.FirstAdminUserStep.ID;
    }
}
