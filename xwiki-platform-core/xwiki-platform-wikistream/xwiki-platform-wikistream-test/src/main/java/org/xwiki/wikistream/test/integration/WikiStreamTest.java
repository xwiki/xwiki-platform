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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.internal.MockConfigurationSource;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.input.FileInputSource;
import org.xwiki.wikistream.input.InputSource;
import org.xwiki.wikistream.input.InputStreamInputSource;
import org.xwiki.wikistream.input.InputWikiStream;
import org.xwiki.wikistream.input.InputWikiStreamFactory;
import org.xwiki.wikistream.input.ReaderInputSource;
import org.xwiki.wikistream.internal.input.DefaultFileInputSource;
import org.xwiki.wikistream.internal.input.DefaultURLInputSource;
import org.xwiki.wikistream.internal.input.StringInputSource;
import org.xwiki.wikistream.internal.output.ByteArrayOutputTarget;
import org.xwiki.wikistream.internal.output.StringWriterOutputTarget;
import org.xwiki.wikistream.output.OutputTarget;
import org.xwiki.wikistream.output.OutputWikiStream;
import org.xwiki.wikistream.output.OutputWikiStreamFactory;
import org.xwiki.wikistream.test.internal.FileAssert;
import org.xwiki.wikistream.utils.WikiStreamConstants;

/**
 * A generic JUnit Test used by {@link WikiStreamTestSuite} to parse some passed content and verify it matches some
 * passed expectation. The format of the input/expectation is specified in {@link TestDataParser}.
 * 
 * @version $Id$
 * @since 5.2M2
 */
public class WikiStreamTest
{
    private TestConfiguration configuration;

    private ComponentManager componentManager;

    public WikiStreamTest(TestConfiguration configuration, ComponentManager componentManager)
    {
        this.configuration = configuration;
        this.componentManager = componentManager;
    }

    @Test
    public void execute() throws Throwable
    {
        Map<String, String> originalConfiguration = new HashMap<String, String>();
        if (this.configuration.configuration != null) {
            ConfigurationSource configurationSource = getComponentManager().getInstance(ConfigurationSource.class);

            if (configurationSource instanceof MockConfigurationSource) {
                MockConfigurationSource mockConfigurationSource = (MockConfigurationSource) configurationSource;

                for (Map.Entry<String, String> entry : this.configuration.configuration.entrySet()) {
                    originalConfiguration.put(entry.getKey(),
                        mockConfigurationSource.<String> getProperty(entry.getKey()));
                    mockConfigurationSource.setProperty(entry.getKey(), TestDataParser.interpret(entry.getValue()));
                }
            }
        }

        try {
            runTestInternal();
        } finally {
            // Revert Configuration that have been set
            if (this.configuration.configuration != null) {
                ConfigurationSource configurationSource = getComponentManager().getInstance(ConfigurationSource.class);

                if (configurationSource instanceof MockConfigurationSource) {
                    MockConfigurationSource mockConfigurationSource = (MockConfigurationSource) configurationSource;

                    for (Map.Entry<String, String> entry : originalConfiguration.entrySet()) {
                        if (entry.getValue() == null) {
                            mockConfigurationSource.removeProperty(entry.getKey());
                        } else {
                            mockConfigurationSource.setProperty(entry.getKey(), entry.getValue());
                        }
                    }
                }
            }
        }
    }

    private InputSource getInputSource(TestConfiguration testConfiguration, String value) throws WikiStreamException
    {
        InputSource source;

        String sourceString = TestDataParser.interpret(value);

        File file = new File(sourceString);

        if (file.exists()) {
            // It's a file

            source = new DefaultFileInputSource(file);
        } else {
            // If not a file it's probably a resource

            if (!sourceString.startsWith("/")) {
                sourceString =
                    StringUtils.substringBeforeLast(testConfiguration.resourceName, "/") + '/' + sourceString;
            }

            URL url = getClass().getResource(sourceString);

            if (url == null) {
                throw new WikiStreamException("Resource [" + sourceString + "] does not exist");
            }

            source = new DefaultURLInputSource(url);
        }

        return source;
    }

    private Map<String, Object> toInputConfiguration(TestConfiguration testConfiguration,
        InputTestConfiguration inputTestConfiguration) throws WikiStreamException
    {
        Map<String, Object> inputConfiguration = new HashMap<>();
        for (Map.Entry<String, String> entry : inputTestConfiguration.entrySet()) {
            if (entry.getKey().equals(WikiStreamConstants.PROPERTY_SOURCE)) {
                inputConfiguration.put(WikiStreamConstants.PROPERTY_SOURCE,
                    getInputSource(testConfiguration, entry.getValue()));
            } else {
                inputConfiguration.put(entry.getKey(), TestDataParser.interpret(entry.getValue()));
            }
        }

        // Generate a source f it does not exist
        if (!inputConfiguration.containsKey(WikiStreamConstants.PROPERTY_SOURCE)) {
            inputConfiguration.put(WikiStreamConstants.PROPERTY_SOURCE, new StringInputSource(
                inputTestConfiguration.buffer));
        }

        return inputConfiguration;
    }

    private Map<String, Object> toOutputConfiguration(TestConfiguration testConfiguration,
        ExpectTestConfiguration expectTestConfiguration, InputSource expect) throws WikiStreamException
    {
        Map<String, Object> outputConfiguration = new HashMap<>();
        for (Map.Entry<String, String> entry : expectTestConfiguration.entrySet()) {
            outputConfiguration.put(entry.getKey(), TestDataParser.interpret(entry.getValue()));
        }

        // Generate a source if it does not exist
        if (!outputConfiguration.containsKey(WikiStreamConstants.PROPERTY_TARGET)) {
            if (expect instanceof ReaderInputSource) {
                outputConfiguration.put(WikiStreamConstants.PROPERTY_TARGET, new StringWriterOutputTarget());
            } else {
                outputConfiguration.put(WikiStreamConstants.PROPERTY_TARGET, new ByteArrayOutputTarget());
            }
        }

        // Format by default
        if (!outputConfiguration.containsKey(WikiStreamConstants.PROPERTY_FORMAT)) {
            outputConfiguration.put(WikiStreamConstants.PROPERTY_FORMAT, true);
        }

        // Encoding by default
        if (!outputConfiguration.containsKey(WikiStreamConstants.PROPERTY_ENCODING)) {
            outputConfiguration.put(WikiStreamConstants.PROPERTY_ENCODING, "UTF-8");
        }

        return outputConfiguration;
    }

    private InputSource getExpectInputSource(TestConfiguration testConfiguration,
        ExpectTestConfiguration expectConfiguration) throws WikiStreamException
    {
        String expectPath = expectConfiguration.get(WikiStreamConstants.PROPERTY_SOURCE);
        if (expectPath == null) {
            return new StringInputSource(expectConfiguration.buffer.toString());
        } else {
            return getInputSource(testConfiguration, expectPath);
        }
    }

    private void runTestInternal() throws Throwable
    {
        // Expect

        InputSource expect = getExpectInputSource(this.configuration, this.configuration.expectConfiguration);

        // Input

        InputWikiStreamFactory inputFactory =
            getComponentManager().getInstance(InputWikiStreamFactory.class,
                this.configuration.inputConfiguration.typeId);
        InputWikiStream inputWikiStream =
            inputFactory.createInputWikiStream(toInputConfiguration(this.configuration,
                this.configuration.inputConfiguration));

        // Output

        Map<String, Object> outputConfiguration =
            toOutputConfiguration(this.configuration, this.configuration.expectConfiguration, expect);
        OutputWikiStreamFactory outputFactory =
            getComponentManager().getInstance(OutputWikiStreamFactory.class,
                this.configuration.expectConfiguration.typeId);
        OutputWikiStream outputWikiStream = outputFactory.createOutputWikiStream(outputConfiguration);

        // Convert

        inputWikiStream.read(outputWikiStream.getFilter());

        inputWikiStream.close();
        outputWikiStream.close();

        // Verify the expected result against the result we got.

        assertExpectedResult(this.configuration.expectConfiguration.typeId, expect,
            (OutputTarget) outputConfiguration.get(WikiStreamConstants.PROPERTY_TARGET));
    }

    private void assertExpectedResult(String typeId, InputSource expected, OutputTarget actual) throws IOException
    {
        if (actual instanceof StringWriterOutputTarget) {
            Assert.assertEquals(expected.toString(), actual.toString());
        } else if (actual instanceof ByteArrayOutputTarget) {
            byte[] actualBytes = ((ByteArrayOutputTarget) actual).toByteArray();

            if (expected instanceof FileInputSource) {
                FileAssert.assertEquals(((FileInputSource) expected).getFile(), actualBytes);
            } else {
                byte[] expectedBytes = IOUtils.toByteArray(((InputStreamInputSource) expected).getInputStream());
                expected.close();

                Assert.assertArrayEquals(expectedBytes, actualBytes);
            }
        } else {
            // No idea how to compare that
            Assert.fail("Output target type [" + actual.getClass() + "] is not supported");
        }
    }

    public ComponentManager getComponentManager() throws Exception
    {
        return this.componentManager;
    }
}
