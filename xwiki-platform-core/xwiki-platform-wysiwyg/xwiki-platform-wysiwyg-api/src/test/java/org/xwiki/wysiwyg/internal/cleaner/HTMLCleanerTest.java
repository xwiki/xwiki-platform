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
package org.xwiki.wysiwyg.internal.cleaner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.opentest4j.IncompleteExecutionException;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.wysiwyg.cleaner.HTMLCleaner;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Run all tests found in {@code *.test} files located in the classpath.
 * <p>
 * Usage Example
 * </p>
 * 
 * <pre>
 * <code>
 * &#064;AllComponents
 * class MyIntegrationTests extends FilterTest
 * {
 * }
 * </code>
 * </pre>
 * 
 * @version $Id$
 * @since 18.0.0RC1
 */
public class HTMLCleanerTest
{
    private static final String DEFAULT_PATTERN = ".*\\.test";

    /**
     * The line that separates the HTML input from the expected clean HTML.
     */
    private static final String INPUT_EXPECTED_SEPARATOR = "---";

    /**
     * The string that prefixes comment lines.
     */
    private static final String COMMENT_LINE_PREFIX = "#";

    /**
     * The string used to escape the new line character. This is very useful when you want to split the HTML input or
     * the expected clean HTML on multiple lines without inserting new line characters in the content.
     */
    private static final String NEW_LINE_ESCAPE = "\\";

    /**
     * A generic JUnit test used by {@link HTMLCleanerTest} to clean some passed HTML content and verify it matches some
     * passed expectation.
     * 
     * @version $Id$
     */
    class TestRunner
    {
        /**
         * The HTML cleaner being tested.
         */
        private HTMLCleaner cleaner;

        /**
         * The HTML fragment to be cleaned.
         */
        private final String input;

        /**
         * The expected clean HTML.
         */
        private final String expected;

        /**
         * Creates a new test case that checks if the result of cleaning the given HTML input equals the expected HTML.
         * 
         * @param input the HTML fragment to be cleaned
         * @param expected the expected clean HTML
         * @throws ComponentLookupException when failing to lookup the HTML cleaner component
         */
        public TestRunner(String input, String expected) throws ComponentLookupException
        {
            this.input = input;
            this.expected = expected;
            this.cleaner = getComponentManager().getInstance(HTMLCleaner.class);
        }

        /**
         * The actual test.
         */
        void execute()
        {
            assertEquals(xhtmlFragment(expected), cleaner.clean(input));
        }

        /**
         * Adds the XHTML envelope to the given XHTML fragment.
         * 
         * @param fragment the content to be placed inside the {@code body} tag
         * @return the given XHTML fragment wrapped in the XHTML envelope
         */
        private String xhtmlFragment(String fragment)
        {
            return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<!DOCTYPE html>\n" + "<html><head></head><body>"
                + fragment + "</body></html>\n";
        }
    }

    /**
     * Used to perform specific initializations before each test in the suite is executed.
     * 
     * @version $Id$
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Initialized
    {
    }

    /**
     * Annotation to use to indicate the resources directory containing the tests to execute.
     * 
     * @version $Id$
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Scope
    {
        /**
         * @return the classpath prefix to search in
         */
        String value() default "";

        /**
         * @return the regex pattern to filter *.test files to execute
         */
        String pattern() default DEFAULT_PATTERN;
    }

    private MockitoComponentManager componentManager;

    @BeforeEach
    void initializeComponentManager()
    {
        // Initialize a component manager used to locate filter components to decide what tests to execute in
        // TestDataGenerator.
        if (this.componentManager == null) {
            MockitoComponentManager mockitoComponentManager = new MockitoComponentManager();
            try {
                mockitoComponentManager.initializeTest(this);
                mockitoComponentManager.registerMemoryConfigurationSource();
            } catch (Exception e) {
                throw new IncompleteExecutionException("Failed to initialize Component Manager", e);
            }
            this.componentManager = mockitoComponentManager;
        }
    }

    @BeforeEach
    void callInitializers()
    {
        callAnnotatedMethods(Initialized.class);
    }

    @AfterEach
    void shutdownComponentManager()
    {
        if (this.componentManager != null) {
            try {
                this.componentManager.shutdownTest();
            } catch (Exception e) {
                throw new IncompleteExecutionException("Failed to shutdown Component Manager", e);
            }
        }
    }

    protected MockitoComponentManager getComponentManager()
    {
        return this.componentManager;
    }

    private void callAnnotatedMethods(Class<? extends Annotation> annotationClass)
    {
        try {
            for (Method klassMethod : getClass().getDeclaredMethods()) {
                Annotation componentManagerAnnotation = klassMethod.getAnnotation(annotationClass);
                if (componentManagerAnnotation != null) {
                    // Call it!
                    klassMethod.invoke(this, this.componentManager);
                }
            }
        } catch (Exception e) {
            throw new IncompleteExecutionException(
                String.format("Failed to call test methods annotated with [%s]", annotationClass.getCanonicalName()),
                e);
        }
    }

    /**
     * @return the dynamic list of tests to execute
     * @throws Exception when failing to generate the tests
     */
    @TestFactory
    Stream<DynamicTest> filterTests() throws Exception
    {
        List<Object[]> testsData = readTestsDataFromResource("/HTMLCleanerTests.txt");

        // Step 2: Generate test names
        Function<Object[], String> displayNameGenerator = input -> (String) input[0];

        // Step 3: Generate tests to execute
        ThrowingConsumer<Object[]> testExecutor =
            input -> new TestRunner((String) input[1], (String) input[2]).execute();

        // Return the dynamically created tests
        return DynamicTest.stream(testsData.iterator(), displayNameGenerator, testExecutor);
    }

    /**
     * Reads the data for the tests listed in the specified resource file.
     * 
     * @param testResourceName the name of a resource file that lists the tests to be run
     * @return the data for all the test to be run
     * @throws IOException if reading the test resource fails
     */
    private List<Object[]> readTestsDataFromResource(String testResourceName) throws IOException
    {
        List<Object[]> testsData = new ArrayList<Object[]>();
        for (String testName : getTestNames(getClass().getResourceAsStream(testResourceName))) {
            testsData.add(readTestDataFromResource(testName));
        }
        return testsData;
    }

    /**
     * Reads the list of test names from the given input stream.
     * 
     * @param source where to read the test names from
     * @return a list of test names
     * @throws IOException if reading the test names fails
     */
    private List<String> getTestNames(InputStream source) throws IOException
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(source));
        List<String> testNames = new ArrayList<String>();
        try {
            String line = reader.readLine();
            while (line != null) {
                if (!line.startsWith(COMMENT_LINE_PREFIX)) {
                    testNames.add(line.trim());
                }
                line = reader.readLine();
            }
        } finally {
            reader.close();
        }
        return testNames;
    }

    /**
     * Reads the test data, the HTML input and the expected clean HTML, from the resource file with the specified name.
     * 
     * @param testResourceName the name of a resource file that contains the test data
     * @return the test data read from the specified resource
     * @throws IOException if reading the resource files fails
     */
    private String[] readTestDataFromResource(String testResourceName) throws IOException
    {
        InputStream source = getClass().getResourceAsStream('/' + testResourceName + ".test");
        BufferedReader reader = new BufferedReader(new InputStreamReader(source));
        try {
            StringBuilder input = new StringBuilder();
            String line = reader.readLine();
            while (line != null && !line.equals(INPUT_EXPECTED_SEPARATOR)) {
                appendLine(input, line);
                line = reader.readLine();
            }
            StringBuilder expected = new StringBuilder();
            // Skip the line that separates the input from the expected HTML.
            line = reader.readLine();
            while (line != null) {
                appendLine(expected, line);
                line = reader.readLine();
            }
            return new String[] {testResourceName, input.toString(), expected.toString()};
        } finally {
            reader.close();
        }
    }

    /**
     * Skips the line if it's a comment and removes the new line character if it is escaped.
     * 
     * @param output where to append the given line
     * @param line the line of text to be appended
     */
    private void appendLine(StringBuilder output, String line)
    {
        if (line.startsWith(COMMENT_LINE_PREFIX)) {
            return;
        } else if (line.endsWith(NEW_LINE_ESCAPE)) {
            output.append(line, 0, line.length() - NEW_LINE_ESCAPE.length());
        } else {
            output.append(line).append('\n');
        }
    }
}
