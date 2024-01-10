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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.extension.CoreExtension;
import org.xwiki.extension.Extension;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.index.security.ExtensionSecurityAnalysisResult;
import org.xwiki.extension.index.security.review.ReviewsMap;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.security.ExtensionSecurityIndexationEndEvent;
import org.xwiki.extension.security.analyzer.ExtensionSecurityAnalyzer;
import org.xwiki.extension.security.analyzer.ReviewsFetcher;
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
    private CoreExtensionRepository coreExtensionRepository;

    @Inject
    @Named(OsvExtensionSecurityAnalyzer.ID)
    private ExtensionSecurityAnalyzer extensionSecurityAnalyzer;

    @Inject
    private VulnerabilityIndexer vulnerabilityIndexer;

    @Inject
    private ExecutionContextManager executionContextManager;

    @Inject
    private ReviewsFetcher reviewsFetcher;

    @Override
    public String getType()
    {
        return JOBTYPE;
    }

    @Override
    protected void runInternal()
    {
        Collection<InstalledExtension> installedExtensions = this.installedExtensionRepository.getInstalledExtensions();
        Collection<CoreExtension> coreExtensions = this.coreExtensionRepository.getCoreExtensions();
        this.progressManager.pushLevelProgress(installedExtensions.size() + coreExtensions.size(), this);

        ReviewsMap reviewsMap = fetchReviewsMap();
        try {
            ExecutorService executorService = Executors.newFixedThreadPool(10);

            List<Future<Boolean>> tasks = new ArrayList<>();
            for (InstalledExtension extension : installedExtensions) {
                tasks.add(executorService.submit(() -> handleExtension(extension, reviewsMap)));
            }

            for (CoreExtension extension : coreExtensions) {
                tasks.add(executorService.submit(() -> handleExtension(extension, reviewsMap)));
            }

            long newVulnerabilityCount = consumeTasks(tasks);
            this.observationManager.notify(new ExtensionSecurityIndexationEndEvent(), null, newVulnerabilityCount);
        } catch (InterruptedException e) {
            this.logger.warn("The job has been interrupted. Cause: [{}]", getRootCauseMessage(e));
            Thread.currentThread().interrupt();
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }

    private ReviewsMap fetchReviewsMap()
    {
        ReviewsMap reviewsMap;
        try {
            reviewsMap = this.reviewsFetcher.fetch();
        } catch (ExtensionSecurityException e) {
            this.logger.warn("Vulnerabilities reviews fetch failed. All the security issues are going to be displayed "
                + "without reviews. Cause: [{}]", getRootCauseMessage(e));
            reviewsMap = new ReviewsMap();
        }
        return reviewsMap;
    }

    private long consumeTasks(List<Future<Boolean>> tasks) throws InterruptedException
    {
        long newVulnerabilityCount = 0;
        for (Future<Boolean> future : tasks) {
            try {
                Boolean updated = future.get();
                this.progressManager.startStep(this);
                if (Objects.equals(Boolean.TRUE, updated)) {
                    newVulnerabilityCount++;
                }
            } catch (ExecutionException e) {
                this.logger.error("Failed to execute an extension analysis.", e);
            } finally {
                this.progressManager.endStep(this);
            }
        }
        return newVulnerabilityCount;
    }

    private boolean handleExtension(Extension extension, ReviewsMap reviewsMap)
    {
        boolean hasNew = false;
        try {
            this.executionContextManager.initialize(new ExecutionContext());
            ExtensionSecurityAnalysisResult analysis = this.extensionSecurityAnalyzer.analyze(extension);
            if (analysis != null) {
                boolean update = this.vulnerabilityIndexer.update(extension, analysis, reviewsMap);
                if (update) {
                    hasNew = true;
                }
            }
        } catch (ExtensionSecurityException e) {
            this.logger.warn("Failed to analyse [{}]. Cause: [{}]", extension.getId(), getRootCauseMessage(e));
        } catch (ExecutionContextException e) {
            this.logger.warn("Failed to initialize the execution context for [{}]. Cause: [{}]", extension,
                getRootCauseMessage(e));
        } catch (Exception e) {
            this.logger.warn("Unexpected error [{}]", getRootCauseMessage(e));
        }
        return hasNew;
    }
}
