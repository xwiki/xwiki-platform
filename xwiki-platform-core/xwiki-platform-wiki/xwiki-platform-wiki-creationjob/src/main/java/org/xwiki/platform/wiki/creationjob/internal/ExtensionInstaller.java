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
package org.xwiki.platform.wiki.creationjob.internal;

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.job.internal.InstallJob;
import org.xwiki.job.Job;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.platform.wiki.creationjob.WikiCreationException;

/**
 * Component that installs an extension on a wiki.
 *
 * @version $Id$
 * @since 7.0M2
 */
@Component(roles = ExtensionInstaller.class)
@Singleton
public class ExtensionInstaller
{
    private static final String PROPERTY_USERREFERENCE = "user.reference";

    private static final DocumentReference SUPERADMIN_REFERENCE = new DocumentReference("xwiki", "XWiki", "superadmin");

    @Inject
    private ComponentManager componentManager;

    /**
     * Install an extension on a wiki.
     *
     * @param wikiId id of the wiki
     * @param extensionId id of the extension to install
     * @throws org.xwiki.platform.wiki.creationjob.WikiCreationException if problem occurs
     */
    public void installExtension(String wikiId, ExtensionId extensionId) throws WikiCreationException
    {
        try {
            // Create the install request
            InstallRequest installRequest = new InstallRequest();
            installRequest.setId(Arrays.asList(WikiCreationJob.JOB_ID_PREFIX, "install", wikiId));
            installRequest.addExtension(extensionId);
            installRequest.addNamespace("wiki:" + wikiId);
            // To avoid problem with Programming Rights, we install everything with superadmin
            installRequest.setProperty(PROPERTY_USERREFERENCE, SUPERADMIN_REFERENCE);
            InstallJob job = componentManager.getInstance(Job.class, InstallJob.JOBTYPE);
            job.initialize(installRequest);
            job.run();
        } catch (ComponentLookupException e) {
            throw new WikiCreationException(String.format("Failed to install the extension [%s] on the wiki [%s].",
                extensionId.toString(), wikiId), e);
        }
    }
}
