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
package org.xwiki.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.Object;
import com.xpn.xwiki.api.XWiki;
import com.xpn.xwiki.util.XWikiStubContextProvider;

/**
 * Script service for performing a stress test on the security instance.
 *
 * @version $Id$
 */
@Component
@Singleton
@Named("securityCachePerformanceTest")
public class SecurityCachePerformanceTestScriptService implements ScriptService
{
    protected static final String END_OF_MESSAGE = ".\n";

    @Inject
    private Logger logger;

    @Inject
    private Execution execution;

    @Inject
    private ExecutionContextManager executionContextManager;

    @Inject
    private XWikiStubContextProvider stubContextProvider;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private AuthorizationManager authorizationManager;

    /**
     * Perform a stress test on the security cache. The results are logged.
     *
     * @param users the names of the users to use for the test
     * @return an empty string if everything is okay, a list of errors otherwise
     * @throws InterruptedException if the test is interrupted.
     */
    public String perform(List<String> users) throws InterruptedException
    {
        XWikiContext context = this.contextProvider.get();

        // Create a bit more than 8k document references, but avoid creating 20 top level spaces.
        SpaceReference parentSpace = new SpaceReference("Security Cache Performance", context.getWikiReference());
        List<DocumentReference> allPages = createSpaces(parentSpace, 2);

        // Define user references
        SpaceReference userSpaceReference = new SpaceReference("xwiki", "XWiki");

        List<DocumentReference> userReferences = users.stream()
            .map(name -> new DocumentReference(name, userSpaceReference))
            .toList();

        StringBuilder errorOutput = new StringBuilder();

        // Perform two runs to have some warm-up for the caches.
        for (Integer i : List.of(100, 200)) {
            this.logger.warn("Test run with {} pages.", i);
            logResults(performTest(i, allPages, userReferences, errorOutput));
        }

        return errorOutput.toString();
    }

    private Map<String, Double> performTest(int numSaves, List<DocumentReference> allPages,
        List<DocumentReference> userReferences, StringBuilder errorOutput) throws InterruptedException
    {
        // Calculate top 1k pages
        List<DocumentReference> top1kPages = allPages.subList(0, 1000);
        // Create random orders of document references
        List<DocumentReference> randomOrder1 = new ArrayList<>(allPages);
        Collections.shuffle(randomOrder1);

        List<DocumentReference> randomOrder2 = new ArrayList<>(allPages);
        Collections.shuffle(randomOrder2);

        // Thread coordination
        AtomicBoolean stop = new AtomicBoolean(false);

        Map<Integer, List<Double>> top1kPagesTimes = new ConcurrentHashMap<>();
        List<Double> randomOrderExecutionTimes = new ArrayList<>();
        List<Double> randomOrderAllUsersExecutionTimes = new ArrayList<>();

        this.logger.warn("Finished preparing the test, starting rights checks.");

        // Create thread pool
        ExecutorService executorService = Executors.newFixedThreadPool(12);

        // Launch 10 threads to query top 1k pages for guest
        for (int i = 0; i < 10; ++i) {
            int finalI = i;
            executorService.execute(() -> {
                initializeContext();
                top1kPagesTimes.put(finalI, checkAllAccess(top1kPages, Collections.singletonList(null), stop));
            });
        }

        // Launch thread to query random order pages for guest
        executorService.execute(() -> {
            initializeContext();
            randomOrderExecutionTimes.addAll(checkAllAccess(randomOrder1, Collections.singletonList(null), stop));
        });

        // Launch thread to query random order pages for all users
        executorService.execute(() -> {
            initializeContext();
            randomOrderAllUsersExecutionTimes.addAll(checkAllAccess(randomOrder2, userReferences, stop));
        });

        List<Double> averageSaveTime = new ArrayList<>();

        try {
            averageSaveTime.addAll(updateRights(allPages.subList(0, numSaves), errorOutput));

            this.logger.warn("Finishing test...");
        } catch (Exception e) {
            errorOutput.append("Error while performing the test: ");
            errorOutput.append(ExceptionUtils.getStackTrace(e));
        } finally {
            stop.set(true);
            executorService.shutdown();
        }

        if (!executorService.awaitTermination(5, TimeUnit.MINUTES)) {
            errorOutput.append("Timeout while waiting for the rights checks threads to finish.\n");
        }

        List<Double> top1kAverages = top1kPagesTimes.values().stream().flatMap(List::stream).toList();

        // Print results
        return Map.of("Top 1k average", getAverage(top1kAverages),
            "Random guest average", getAverage(randomOrderExecutionTimes),
            "Random all users average", getAverage(randomOrderAllUsersExecutionTimes),
            "Average saving time", getAverage(averageSaveTime));
    }

    private List<Double> updateRights(List<DocumentReference> pagesToUpdate, StringBuilder errorOutput)
        throws XWikiException
    {
        List<Double> saveTimes = new ArrayList<>();
        this.logger.warn("Starting {} updates of rights.", pagesToUpdate.size());
        XWikiContext context = this.contextProvider.get();
        XWiki apiWiki = new XWiki(context.getWiki(), context);

        // Set and remove rights on the top pages to put maximum stress on the security cache.
        for (DocumentReference page : pagesToUpdate) {
            long startTime = System.currentTimeMillis();
            testSettingRight(apiWiki, page, errorOutput);
            saveTimes.add(((double) System.currentTimeMillis() - startTime) / 2);
        }

        return saveTimes;
    }

    private List<Double> checkAllAccess(List<DocumentReference> pages, List<DocumentReference> users,
        AtomicBoolean stop)
    {
        List<Double> averageTime = new ArrayList<>();
        int i = 0;
        long startTime = System.currentTimeMillis();
        while (!stop.get()) {
            for (DocumentReference documentReference : pages) {
                for (DocumentReference user : users) {
                    this.authorizationManager.hasAccess(Right.VIEW, user, documentReference);
                    ++i;

                    if (i == 100) {
                        long executionTime = System.currentTimeMillis() - startTime;
                        averageTime.add((double) executionTime / i);
                        startTime = System.currentTimeMillis();
                        i = 0;
                    }
                }
            }
        }

        if (i > 0) {
            long executionTime = System.currentTimeMillis() - startTime;
            averageTime.add((double) executionTime / i);
        }

        return averageTime;
    }

    private void testSettingRight(XWiki apiWiki, DocumentReference documentReference, StringBuilder errorOutput)
        throws XWikiException
    {
        // Remove view right from the space by granting it to all users. When performed on top spaces, this should cause
        // invalidation of a larger part of the security cache.
        DocumentReference preferencesReference =
            new DocumentReference("WebPreferences", documentReference.getLastSpaceReference());
        Document preferencesDocument = apiWiki.getDocument(preferencesReference);

        Object rightObject = preferencesDocument.newObject("XWiki.XWikiGlobalRights");
        rightObject.set("levels", List.of("view"));
        rightObject.set("groups", List.of("XWiki.XWikiAllGroup"));
        rightObject.set("allow", 1);
        preferencesDocument.saveAsAuthor();
        if (this.authorizationManager.hasAccess(Right.VIEW, null, documentReference)) {
            errorOutput.append("Error setting right on ");
            errorOutput.append(preferencesReference);
            errorOutput.append(". Guest has still view right on document ");
            errorOutput.append(documentReference);
            errorOutput.append(END_OF_MESSAGE);
        }

        preferencesDocument.removeObject(rightObject);
        preferencesDocument.saveAsAuthor();
        if (!this.authorizationManager.hasAccess(Right.VIEW, null, documentReference)) {
            errorOutput.append("Error removing right on ");
            errorOutput.append(preferencesReference);
            errorOutput.append(". Guest has still no view right on document ");
            errorOutput.append(documentReference);
            errorOutput.append(END_OF_MESSAGE);
        }
    }

    private List<DocumentReference> createSpaces(EntityReference parentReference, int remainingDepth)
    {
        List<DocumentReference> documentReferences = new ArrayList<>();

        for (int i = 0; i < 20; ++i) {
            SpaceReference space =
                new SpaceReference(String.format("Page %d-%d", remainingDepth, i), parentReference);
            documentReferences.add(new DocumentReference("WebHome", space));
            if (remainingDepth > 0) {
                documentReferences.addAll(createSpaces(space, remainingDepth - 1));
            }
        }

        return documentReferences;
    }

    private void initializeContext()
    {
        ExecutionContext context = this.execution.getContext();
        if (context == null) {
            context = new ExecutionContext();

            try {
                this.executionContextManager.initialize(context);
            } catch (Exception e) {
                this.logger.error("Error while initializing execution context", e);
            }

            XWikiContext xwikiContext = this.stubContextProvider.createStubContext();
            xwikiContext.declareInExecutionContext(context);
            this.execution.pushContext(context);
        }
    }

    private double getAverage(List<Double> numbers)
    {
        if (numbers.isEmpty()) {
            return -1.0;
        }
        double sum = 0.0;
        for (double number : numbers) {
            sum += number;
        }
        return sum / numbers.size();
    }

    private void logResults(Map<String, Double> results)
    {
        for (Map.Entry<String, Double> result : results.entrySet()) {
            this.logger.warn("\t{}: {}", result.getKey(), result.getValue());
        }
    }
}
