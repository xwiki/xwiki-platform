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
package org.xwiki.test.page.accessibility;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Tests of the automatic WCAG validation behind the {@link WCAG} annotation. The validation happens at test
 * teardown, so these tests run their fixtures through a real JUnit run: the annotation, the collection during
 * {@code renderPage} and the aggregated failure report are all exercised end to end.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
class WCAGAutomaticPageTest
{
    /**
     * The fixtures run through the JUnit launcher, so they exercise the real bridge: fail when it cannot be
     * found, for the same reason as {@link AssertWCAGPageTest#assertBridgeAvailable()}.
     */
    private static final String UNAVAILABILITY_REASON = AxeBridge.getUnavailabilityReason();

    @BeforeAll
    static void assertBridgeAvailable()
    {
        assertNull(UNAVAILABILITY_REASON, "The axe-core bridge is expected to be available here: "
            + UNAVAILABILITY_REASON);
    }

    /**
     * Renders a good page, then the same bad page twice: the collected pages must be deduplicated and the
     * validation must fail at the end of the test with all the violations of the bad page only.
     */
    @WCAG
    @HTML50ComponentList
    @XWikiSyntax21ComponentList
    static class AnnotatedClassPageTest extends PageTest
    {
        private static final DocumentReference GOOD_PAGE = new DocumentReference("xwiki", "WCAG", "GoodPage");

        private static final DocumentReference BAD_PAGE = new DocumentReference("xwiki", "WCAG", "BadPage");

        @Test
        void renderPages() throws Exception
        {
            // The fixtures must really render what the WCAG validation is expected to catch.
            assertTrue(renderPage(GOOD_PAGE).contains("The XWiki logo"));
            assertTrue(renderPage(BAD_PAGE).contains("<button"));
            renderPage(BAD_PAGE);
        }
    }

    /**
     * A class level {@code @WCAG} the method opts out of.
     */
    @WCAG
    @HTML50ComponentList
    @XWikiSyntax21ComponentList
    static class OptedOutMethodPageTest extends PageTest
    {
        private static final DocumentReference BAD_PAGE = new DocumentReference("xwiki", "WCAG", "BadPage");

        @Test
        @WCAG(enabled = false)
        void renderBadPage() throws Exception
        {
            assertTrue(renderPage(BAD_PAGE).contains("<button"));
        }
    }

    /**
     * A method level {@code @WCAG} without any class level annotation.
     */
    @HTML50ComponentList
    @XWikiSyntax21ComponentList
    static class AnnotatedMethodPageTest extends PageTest
    {
        private static final DocumentReference BAD_PAGE = new DocumentReference("xwiki", "WCAG", "BadPage");

        @Test
        @WCAG
        void renderBadPage() throws Exception
        {
            assertTrue(renderPage(BAD_PAGE).contains("<button"));
        }
    }

    /**
     * No annotation at all: the rendered pages must not even be collected.
     */
    @HTML50ComponentList
    @XWikiSyntax21ComponentList
    static class NotAnnotatedPageTest extends PageTest
    {
        private static final DocumentReference BAD_PAGE = new DocumentReference("xwiki", "WCAG", "BadPage");

        @Test
        void renderBadPage() throws Exception
        {
            assertTrue(renderPage(BAD_PAGE).contains("<button"));
        }
    }

    @Test
    void validatePagesRenderedByAnnotatedClass()
    {
        TestExecutionSummary summary = executeJUnit(AnnotatedClassPageTest.class);

        assertEquals(1, summary.getTestsFailedCount());
        String failureMessage = summary.getFailures().getFirst().getException().getMessage();
        assertTrue(failureMessage.contains(
            "WCAG validation of the pages rendered during the test method [renderPages] failed."),
            failureMessage);
        assertTrue(failureMessage.contains("Check for page [xwiki:WCAG.BadPage]."), failureMessage);
        assertTrue(failureMessage.contains("Found [3] failing rule(s)."), failureMessage);
        assertTrue(failureMessage.contains("Rule ID: image-alt"), failureMessage);
        // The good page has no violation, and the bad page rendered twice is validated once.
        assertFalse(failureMessage.contains("xwiki:WCAG.GoodPage"), failureMessage);
    }

    @Test
    void optOutMethodOverridesAnnotatedClass()
    {
        TestExecutionSummary summary = executeJUnit(OptedOutMethodPageTest.class);

        assertEquals(1, summary.getTestsSucceededCount());
        assertEquals(0, summary.getTestsFailedCount());
    }

    @Test
    void validatePagesRenderedByAnnotatedMethod()
    {
        TestExecutionSummary summary = executeJUnit(AnnotatedMethodPageTest.class);

        assertEquals(1, summary.getTestsFailedCount());
        assertTrue(summary.getFailures().getFirst().getException().getMessage()
            .contains("Rule ID: button-name"));
    }

    @Test
    void doNotValidatePagesWithoutAnnotation()
    {
        TestExecutionSummary summary = executeJUnit(NotAnnotatedPageTest.class);

        assertEquals(1, summary.getTestsSucceededCount());
        assertEquals(0, summary.getTestsFailedCount());
    }

    private TestExecutionSummary executeJUnit(Class<?> testClass)
    {
        LauncherDiscoveryRequestBuilder request = LauncherDiscoveryRequestBuilder.request()
            .selectors(selectClass(testClass));
        // Do not autoload TestExecutionListener since that would load "FailingTestDebuggingTestExecutionListener",
        // which prints failures in the console and would fail these tests verifying what is printed.
        LauncherConfig config = LauncherConfig.builder().enableTestExecutionListenerAutoRegistration(false).build();
        Launcher launcher = LauncherFactory.create(config);
        SummaryGeneratingListener summaryListener = new SummaryGeneratingListener();
        launcher.execute(request.build(), summaryListener);

        return summaryListener.getSummary();
    }
}
