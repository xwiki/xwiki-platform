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
package org.xwiki.extension.security.internal;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.index.security.ExtensionSecurityAnalysisResult;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.security.ExtensionSecurityIndexationEndEvent;
import org.xwiki.extension.security.analyzer.ExtensionSecurityAnalyzer;
import org.xwiki.extension.security.internal.analyzer.VulnerabilityIndexer;
import org.xwiki.extension.security.internal.analyzer.osv.OsvExtensionSecurityAnalyzer;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.DefaultJobStatus;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Run a security analysis on the current instance's extensions.
 *
 * @version $Id$
 * @since 15.5RC1
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
@Named(ExtensionSecurityJob.JOBTYPE)
public class ExtensionSecurityJob
    extends AbstractJob<ExtensionSecurityRequest, DefaultJobStatus<ExtensionSecurityRequest>>
{
    /**
     * The id of the job.
     */
    public static final String JOBTYPE = "extension_security";

    @Inject
    private InstalledExtensionRepository installedExtensionRepository;

    @Inject
    @Named(OsvExtensionSecurityAnalyzer.ID)
    private ExtensionSecurityAnalyzer extensionSecurityAnalyzer;

    @Inject
    private VulnerabilityIndexer vulnerabilityIndexer;

    @Override
    public String getType()
    {
        return JOBTYPE;
    }

    @Override
    protected void runInternal()
    {
        Collection<InstalledExtension> installedExtensions =
            this.installedExtensionRepository.getInstalledExtensions();
        this.progressManager.pushLevelProgress(installedExtensions.size(), this);

        try {
            // Note: for now, this step is sequential and each extension is analyzed after the previous one.
            long newVulnerabilityCount = 0;
            for (InstalledExtension extension : installedExtensions) {
                this.progressManager.startStep(this);
                try {
                    ExtensionSecurityAnalysisResult analysis = this.extensionSecurityAnalyzer.analyze(extension);
                    if (analysis != null) {
                        boolean update = this.vulnerabilityIndexer.update(extension, analysis);
                        if (update) {
                            newVulnerabilityCount++;
                        }
                    }
                } catch (ExtensionSecurityException e) {
                    this.logger.warn("Failed to analyse [{}]. Cause: [{}]", extension.getId().toString(),
                        getRootCauseMessage(e));
                } catch (Exception e) {
                    this.logger.warn("Unexpected error [{}]", getRootCauseMessage(e));
                }
                this.progressManager.endStep(this);
            }
            
            this.observationManager.notify(new ExtensionSecurityIndexationEndEvent(), null, newVulnerabilityCount);
            
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }
}
