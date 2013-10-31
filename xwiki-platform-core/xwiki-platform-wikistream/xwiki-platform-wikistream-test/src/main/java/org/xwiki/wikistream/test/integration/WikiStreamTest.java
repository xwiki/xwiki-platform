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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.internal.MockConfigurationSource;
import org.xwiki.wikistream.input.InputSource;
import org.xwiki.wikistream.input.InputStreamInputSource;
import org.xwiki.wikistream.input.InputWikiStream;
import org.xwiki.wikistream.input.InputWikiStreamFactory;
import org.xwiki.wikistream.internal.output.ByteArrayOutputTarget;
import org.xwiki.wikistream.internal.output.StringWriterOutputTarget;
import org.xwiki.wikistream.output.OutputTarget;
import org.xwiki.wikistream.output.OutputWikiStream;
import org.xwiki.wikistream.output.OutputWikiStreamFactory;

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

                for (Map.Entry<String, ? > entry : this.configuration.configuration.entrySet()) {
                    originalConfiguration.put(entry.getKey(),
                        mockConfigurationSource.<String> getProperty(entry.getKey()));
                    mockConfigurationSource.setProperty(entry.getKey(), entry.getValue());
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

    private void runTestInternal() throws Throwable
    {
        InputWikiStreamFactory inputFactory =
            getComponentManager().getInstance(InputWikiStreamFactory.class,
                this.configuration.inputConfiguration.typeId);
        OutputWikiStreamFactory outputFactory =
            getComponentManager().getInstance(OutputWikiStreamFactory.class,
                this.configuration.expectConfiguration.output.typeId);

        InputWikiStream inputWikiStream = inputFactory.createInputWikiStream(this.configuration.inputConfiguration);
        OutputWikiStream outputWikiStream =
            outputFactory.createOutputWikiStream(this.configuration.expectConfiguration.output);

        try {
            // Convert
            inputWikiStream.read(outputWikiStream.getFilter());

            // Compare

            // ////////////////////////
            // ////////////////::::///::///:::

            // Verify the expected result against the result we got.
            assertExpectedResult(this.configuration.expectConfiguration.output.typeId,
                this.configuration.expectConfiguration.expect,
                this.configuration.expectConfiguration.output.getTarget());
        } finally {
            inputWikiStream.close();
            outputWikiStream.close();
        }
    }

    private void assertExpectedResult(String typeId, InputSource expect, OutputTarget actual) throws IOException
    {
        if (actual instanceof StringWriterOutputTarget) {
            Assert.assertEquals(expect.toString(), actual.toString());
        } else if (actual instanceof ByteArrayOutputTarget) {
            byte[] actualBytes = ((ByteArrayOutputTarget) actual).toByteArray();
            byte[] expectButes = IOUtils.toByteArray(((InputStreamInputSource) expect).getInputStream());
        } else {
            // No idea how to compare that
            Assert.fail("Ouput target type [" + actual.getClass() + "] is not supported");
        }
    }

    public ComponentManager getComponentManager() throws Exception
    {
        return this.componentManager;
    }
}
