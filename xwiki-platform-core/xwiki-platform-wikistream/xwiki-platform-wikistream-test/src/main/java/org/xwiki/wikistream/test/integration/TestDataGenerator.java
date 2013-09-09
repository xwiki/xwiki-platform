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
package org.xwiki.wikistream.test.integration;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.xwiki.wikistream.input.source.ReaderInputSource;
import org.xwiki.wikistream.internal.output.target.ByteArrayOutputTarget;
import org.xwiki.wikistream.internal.output.target.StringWriterOutputTarget;
import org.xwiki.wikistream.utils.WikiStreamConstants;

/**
 * Finds all test files in the current classloader, read them and return test data to represent them.
 * 
 * @version $Id$
 */
public class TestDataGenerator
{
    private TestDataParser parser = new TestDataParser();

    public Collection<TestConfiguration> generateData(String testPackage, String pattern)
    {
        Reflections reflections =
            new Reflections(new ConfigurationBuilder().setScanners(new ResourcesScanner())
                .setUrls(ClasspathHelper.forPackage(""))
                .filterInputsBy(new FilterBuilder.Include(FilterBuilder.prefix(testPackage))));

        Collection<TestConfiguration> data = new ArrayList<TestConfiguration>();
        for (String testResourceName : reflections.getResources(Pattern.compile(pattern))) {
            data.addAll(parseSingleResource(testResourceName));
        }

        return data;
    }

    private Collection<TestConfiguration> parseSingleResource(String testResourceName)
    {
        String resourceName = "/" + testResourceName;
        TestResourceData data;
        try {
            InputStream source = getClass().getResourceAsStream(resourceName);
            if (source == null) {
                throw new RuntimeException("Failed to find test file [" + resourceName + "]");
            }
            data = this.parser.parse(source, resourceName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read test data from [" + resourceName + "]", e);
        }

        Collection<TestConfiguration> result = new ArrayList<TestConfiguration>();
        for (InputTestConfiguration inputConfiguration : data.inputs) {
            for (ExpectTestConfiguration expectConfiguration : data.expects) {
                TestConfiguration testConfiguration = new TestConfiguration();

                testConfiguration.configuration = data.configuration;

                // Input
                // Clone because InputSource cannot be reused for several tests
                testConfiguration.inputConfiguration = new InputTestConfiguration(inputConfiguration);

                // Expect
                // Clone because OutputTarget cannot be reused for several tests
                testConfiguration.expectConfiguration = new ExpectTestConfiguration(expectConfiguration);
                // If there is no custom output target, use the inline string
                OutputTestConfiguration outputConfiguration = testConfiguration.expectConfiguration.output;
                if (outputConfiguration.getTarget() == null) {
                    if (testConfiguration.expectConfiguration.expect instanceof ReaderInputSource) {
                        outputConfiguration.setTarget(new StringWriterOutputTarget());
                    } else {
                        outputConfiguration.setTarget(new ByteArrayOutputTarget());
                    }
                }

                // Set test name
                testConfiguration.name =
                    computeTestName(testResourceName, inputConfiguration.typeId, expectConfiguration.output.typeId);

                // Add the test to the list
                result.add(testConfiguration);
            }
        }

        return result;
    }

    private String computeTestName(String prefix, String parserId, String targetSyntaxId)
    {
        // Note: For some reason the Eclipse JUnit test runner strips the information found in parenthesis. Thus we use
        // square brackets instead.
        return prefix + " [" + parserId + ", " + targetSyntaxId + "]";
    }
}
