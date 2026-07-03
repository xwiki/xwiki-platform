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
package org.xwiki.test.docker.internal.junit5;

import java.util.regex.Pattern;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.XWikiWebDriver;
import org.xwiki.test.ui.browser.IgnoreBrowser;

/**
 * Implements the {@link ExecutionCondition} interface to decide if a test should be executed or not.
 *
 * @version $Id$
 * @since 15.10.12
 * @since 16.4.1
 * @since 16.6.0RC1
 */
public abstract class AbstractExecutionConditionExtension extends AbstractExtension
{
    private ExtensionContextTestConfigurationResolver testConfigurationMerger =
        new ExtensionContextTestConfigurationResolver();

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext extensionContext)
    {
        // Verify that the servlet engine used is not in the list of the forbidden ones.
        ConditionEvaluationResult result = evaluateConditionForServletEngine(extensionContext);

        if (!result.isDisabled()) {
            // Verify that there's no annotation to disable a test method for a given browser.
            result = evaluateConditionForBrowserIgnores(extensionContext);
        }

        return result;
    }

    private ConditionEvaluationResult evaluateConditionForBrowserIgnores(ExtensionContext extensionContext)
    {
        if (extensionContext.getTestMethod().isPresent()) {
            if (extensionContext.getRequiredTestMethod().isAnnotationPresent(IgnoreBrowser.class)) {
                IgnoreBrowser ignoreBrowser =
                    extensionContext.getRequiredTestMethod().getAnnotation(IgnoreBrowser.class);
                // Is it the current browser?
                XWikiWebDriver webDriver = loadXWikiWebDriver(extensionContext);
                Capabilities capability = ((RemoteWebDriver) webDriver).getCapabilities();
                String currentBrowserName = capability.getBrowserName();
                String currentBrowserVersion = capability.getBrowserVersion();
                Pattern browserNamePattern = Pattern.compile(ignoreBrowser.value());
                Pattern browserVersionPattern = Pattern.compile(ignoreBrowser.version());
                if (browserNamePattern.matcher(currentBrowserName).matches()
                    && (ignoreBrowser.version().isEmpty()
                    || browserVersionPattern.matcher(currentBrowserVersion).matches()))
                {
                    return ConditionEvaluationResult.disabled(String.format("Matching browser ignore [%s][%s] for "
                        + "[%s][%s]. Reason: [%s]. Disabling the test", ignoreBrowser.value(), ignoreBrowser.version(),
                        currentBrowserName, currentBrowserVersion, ignoreBrowser.reason()));
                } else {
                    return ConditionEvaluationResult.enabled(String.format("Browser ignore specified [%s][%s] but not"
                        + " matching [%s][%s], continuing", ignoreBrowser.value(), ignoreBrowser.version(),
                        currentBrowserName, currentBrowserVersion));
                }
            } else {
                return ConditionEvaluationResult.enabled("No browser ignore specified, continuing");
            }
        } else {
            return ConditionEvaluationResult.enabled("Not in a test method, continuing");
        }
    }

    private ConditionEvaluationResult evaluateConditionForServletEngine(ExtensionContext extensionContext)
    {
        // This method is the first one called in the test lifecycle. It's called for the top level test class but
        // also for nested test classes. So if the test class has parent tests and one of them has the @UITest
        // annotation then it means all containers have already been started and the servlet engine is supported.
        if (!hasParentTestContainingUITestAnnotation(extensionContext)) {
            // Create & save the test configuration so that we can access it in afterAll()
            TestConfiguration testConfiguration = this.testConfigurationMerger.resolve(extensionContext);
            saveTestConfiguration(extensionContext, testConfiguration);
            // Skip the test if the Servlet Engine selected is in the forbidden list
            if (isServletEngineForbidden(testConfiguration)) {
                return ConditionEvaluationResult.disabled(String.format("Servlet Engine [%s] is forbidden, skipping",
                    testConfiguration.getServletEngine()));
            } else {
                return ConditionEvaluationResult.enabled(String.format("Servlet Engine [%s] is supported, continuing",
                    testConfiguration.getServletEngine()));
            }
        } else {
            return ConditionEvaluationResult.enabled("Servlet Engine is supported by parent Test class, continuing");
        }
    }

    protected boolean hasParentTestContainingUITestAnnotation(ExtensionContext extensionContext)
    {
        boolean hasUITest = false;
        ExtensionContext current = extensionContext;
        // Note: the top level context is the JUnitJupiterExtensionContext one and it doesn't contain any test and
        // thus calling getRequiredTestClass() throws an exception on it, which is why we skip it.
        while (current.getParent().get().getParent().isPresent() && !hasUITest) {
            current = current.getParent().get();
            hasUITest = current.getRequiredTestClass().isAnnotationPresent(UITest.class);
        }
        return hasUITest;
    }

    private boolean isServletEngineForbidden(TestConfiguration testConfiguration)
    {
        return testConfiguration.getForbiddenServletEngines().contains(testConfiguration.getServletEngine());
    }
}
