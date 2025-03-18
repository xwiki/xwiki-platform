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
package org.xwiki.flamingo.test.docker;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.text.StringUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Stress test for the security cache. This test takes about 15 minutes on a relatively powerful system of 2021 and
 * doesn't really test anything, it rather gives some performance numbers. Therefore, it isn't executed by default.
 *
 * @version $Id$
 */
@UITest(properties = {
    "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:Test\\.Execute\\..*"
})
class SecurityCacheStressIT
{
    private static final String STREE_TEST_SCRIPT = """
        {{groovy wiki="false" output="false"}}
        import org.xwiki.security.authorization.Right
        import org.xwiki.context.Execution
        import org.xwiki.context.ExecutionContext
        import org.xwiki.context.ExecutionContextException
        import org.xwiki.context.ExecutionContextManager
        import org.xwiki.model.reference.DocumentReference
        import org.xwiki.model.reference.EntityReference
        import org.xwiki.model.reference.SpaceReference
        import org.xwiki.model.reference.WikiReference
        import java.util.concurrent.*
        import java.util.Collections
        import java.util.ArrayList
        import java.util.concurrent.atomic.AtomicBoolean
        import java.util.concurrent.atomic.AtomicReference
        import java.util.stream.Collectors
        import java.util.stream.IntStream
        
        import com.xpn.xwiki.XWikiContext
        import com.xpn.xwiki.util.XWikiStubContextProvider
        import com.xpn.xwiki.web.Utils
        
        def logger = services.logging.getLogger("SecurityCacheStressTest")
        
        // Create spaces and document references
        def createSpaces(EntityReference parentReference, int remainingDepth) {
            def documentReferences = new ArrayList<DocumentReference>()
        
            for (int i = 0; i < 20; ++i) {
                def space = new SpaceReference(String.format("Page %d-%d", remainingDepth, i), parentReference)
                documentReferences.add(new DocumentReference("WebHome", space))
                if (remainingDepth > 0) {
                    documentReferences.addAll(createSpaces(space, remainingDepth - 1))
                }
            }
        
            return documentReferences
        }
        
        static def initializeContext() {
            def execution = Utils.getComponent(Execution.class)
            def context = execution.getContext()
            if (context == null) {
                context = new ExecutionContext()
        
                def executionContextManager = Utils.getComponent(ExecutionContextManager.class)
                executionContextManager.initialize(context)
        
                def stubContextProvider = Utils.getComponent(XWikiStubContextProvider.class)
                def xwikiContext = stubContextProvider.createStubContext()
                xwikiContext.declareInExecutionContext(context)
                execution.pushContext(context)
            }
        }
        
        // Helper function to calculate average
        static def getAverage(times) {
            if (times.isEmpty()) return -1.0
            return times.sum() / times.size()
        }
        
        // Create a bit more than 8k document references
        def allPages = createSpaces(new WikiReference("xwiki"), 2)
        
        // Create pages
        logger.warn("Creating pages...")
        for (documentReference in allPages) {
            try {
                def myDoc = xwiki.getDocument(documentReference)
                myDoc.setContent("Initial content")
                myDoc.setTitle(documentReference.getLastSpaceReference().getName())
                myDoc.saveAsAuthor("Created")
            } catch (Exception e) {
                println("Error creating page: " + e.getMessage())
            }
        }
        
        // Calculate top 1k pages - make it thread-safe with AtomicReference
        def top1kPages = new AtomicReference<>(allPages.subList(0, 1000))
        
        // Define user references
        List<String> users = IntStream.range(0, 20)
            .mapToObj(i -> "User" + i)
            .toList()
        
        SpaceReference userSpaceReference = new SpaceReference("xwiki", "XWiki")
        List<DocumentReference> userReferences = users.stream()
            .map(name -> new DocumentReference(name, userSpaceReference))
            .toList()
        
        // Create random orders of document references
        def randomOrder1 = new ArrayList<>(allPages)
        Collections.shuffle(randomOrder1)
        def randomOrder2 = new ArrayList<>(allPages)
        Collections.shuffle(randomOrder2)
        
        def performTest = { int numSaves ->
            // Thread coordination
            def stop = new AtomicBoolean(false)
            def top1kPagesTimes = new ConcurrentHashMap<Integer, List<Double>>()
            def randomOrderExecutionTimes = Collections.synchronizedList(new ArrayList<Long>())
            def randomOrderAllUsersExecutionTimes = Collections.synchronizedList(new ArrayList<Double>())
        
            // Store closure-local copies of the shared variables
            def localTop1kPages = top1kPages.get()
            def localRandomOrder1 = randomOrder1
            def localRandomOrder2 = randomOrder2
            def localUserReferences = userReferences
        
            // Create thread pool
            def executorService = Executors.newFixedThreadPool(12)
        
            // Launch 10 threads to query top 1k pages for guest
            for (int i = 0; i < 10; ++i) {
                def finalI = i
                executorService.execute({
                    initializeContext()
                    def authorization = services.security.authorization
                    def executionTimes = new ArrayList<Double>()
                    while (!stop.get()) {
                        try {
                            def startTime = System.currentTimeMillis()
                            for (documentReference in localTop1kPages) {
                                authorization.hasAccess(Right.VIEW, null, documentReference)
                            }
                            def endTime = System.currentTimeMillis()
                            def executionTime = endTime - startTime
                            executionTimes.add((double) executionTime / localTop1kPages.size())
                        } catch (Exception e) {
                            println("Error in top1k thread: " + e.getMessage())
                        }
                    }
                    top1kPagesTimes.put(finalI, executionTimes)
                })
            }
        
            // Launch thread to query random order pages for guest
            executorService.execute({
                initializeContext()
                def authorization = services.security.authorization
                while (!stop.get()) {
                    for (documentReference in localRandomOrder1) {
                        if (stop.get()) break
                        try {
                            def startTime = System.currentTimeMillis()
                            authorization.hasAccess(Right.VIEW, null, documentReference)
                            def endTime = System.currentTimeMillis()
                            def executionTime = endTime - startTime
                            randomOrderExecutionTimes.add(executionTime)
                        } catch (Exception e) {
                            println("Error in random guest thread: " + e.getMessage())
                        }
                    }
                }
            })
        
            // Launch thread to query random order pages for all users
            executorService.execute({
                initializeContext()
                def authorization = services.security.authorization
                while (!stop.get()) {
                    for (documentReference in localRandomOrder2) {
                        if (stop.get()) break
                        try {
                            def startTime = System.currentTimeMillis()
                            for (user in localUserReferences) {
                                authorization.hasAccess(Right.VIEW, user, documentReference)
                            }
                            def endTime = System.currentTimeMillis()
                            def executionTime = endTime - startTime
                            randomOrderAllUsersExecutionTimes.add((double) executionTime / localUserReferences.size())
                        } catch (Exception e) {
                            println("Error in random users thread: " + e.getMessage())
                        }
                    }
                }
            })
        
            // Save some pages to test update performance
            def averageSaveTime = []
            def pagesToUpdate = new ArrayList<>(allPages)
            Collections.shuffle(pagesToUpdate)
            pagesToUpdate = pagesToUpdate.subList(0, numSaves)
        
            logger.warn("Updating pages...")
            for (documentReference in pagesToUpdate) {
                def startTime = System.currentTimeMillis()
                try {
                    def myDoc = xwiki.getDocument(documentReference)
                    myDoc.setContent("Updated content 2")
                    myDoc.saveAsAuthor("Updated")
                    def endTime = System.currentTimeMillis()
                    averageSaveTime.add(endTime - startTime)
                } catch (Exception e) {
                    println("Error updating page: " + e.getMessage())
                }
            }
        
            logger.warn("Finishing test...")
            stop.set(true)
        
            executorService.shutdown()
            executorService.awaitTermination(5, TimeUnit.MINUTES)
        
            // Calculate average times
            def top1kAverages = []
            top1kPagesTimes.values().each { times -> top1kAverages.addAll(times) }
            def top1kAverage = getAverage(top1kAverages)
            def randomGuestAverage = getAverage(randomOrderExecutionTimes)
            def randomAllUsersAverage = getAverage(randomOrderAllUsersExecutionTimes)
            def savingAverage = getAverage(averageSaveTime)
        
            // Print results
            return [
                top1kAverage: top1kAverage,
                randomGuestAverage: randomGuestAverage,
                randomAllUsersAverage: randomAllUsersAverage,
                savingAverage: savingAverage
            ]
        }
        
        def result1 = performTest(100)
        def result2 = performTest(500)
        
        
        logger.warn("Test run 1 results (100 pages):")
        logger.warn("Top1kAverage: ${result1.top1kAverage}ms")
        logger.warn("RandomGuestAverage: ${result1.randomGuestAverage}ms")
        logger.warn("RandomAllUsersAverage: ${result1.randomAllUsersAverage}ms")
        logger.warn("Average saving time: ${result1.savingAverage}ms")
        
        logger.warn("Test run 2 results (500 pages):")
        logger.warn("Top1kAverage: ${result2.top1kAverage}ms")
        logger.warn("RandomGuestAverage: ${result2.randomGuestAverage}ms")
        logger.warn("RandomAllUsersAverage: ${result2.randomAllUsersAverage}ms")
        logger.warn("Average saving time: ${result2.savingAverage}ms")
        {{/groovy}}
        """;

    @Test
    void stressTest(TestUtils testUtils) throws Exception
    {
        testUtils.loginAsSuperAdmin();

        // Create 20 users.
        List<String> users = IntStream.range(0, 20)
            .mapToObj(i -> "User" + i)
            .toList();
        for (String user : users) {
            testUtils.createUser(user, user, null);
        }

        String stressTestName = "securityCacheStressTest";
        testUtils.rest().savePage(new DocumentReference("xwiki", List.of("Test", "Execute"), stressTestName),
            STREE_TEST_SCRIPT, "Security Cache Stress Test");
        String baseURL = StringUtils.removeEnd(testUtils.rest().getBaseURL(), "rest");
        String viewURL = baseURL + "bin/get/Test/Execute/" + stressTestName;

        // Use Java 11 HTTP client to execute the script directly without browser involvement
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMinutes(2))
            .build();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(viewURL))
            .timeout(Duration.ofMinutes(40))
            .GET()
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
    }
}
