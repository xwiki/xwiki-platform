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
package org.xwiki.rendering.scaffolding;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.jmock.cglib.MockObjectTestCase;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.SyntaxFactory;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationManager;
import org.xwiki.test.MockConfigurationSource;

/**
 * @version $Id$
 * @since 1.6M1
 */
public class RenderingTestCase extends MockObjectTestCase
{
    private ComponentManager componentManager;

    private String input;

    private String expected;

    private String parserId;

    private String targetSyntaxId;

    private boolean runTransformations;

    private Map<String, ? > configuration;

    /**
     * @since 2.4M1
     */
    public RenderingTestCase(String testName, String input, String expected, String parserId, String targetSyntaxId,
        boolean runTransformations, Map<String, ? > configuration)
    {
        super(testName);

        this.input = input;
        this.expected = expected;
        this.parserId = parserId;
        this.targetSyntaxId = targetSyntaxId;
        this.runTransformations = runTransformations;
        this.configuration = configuration;
    }

    /**
     * {@inheritDoc}
     * 
     * @see junit.framework.TestCase#runTest()
     */
    @Override
    protected void runTest() throws Throwable
    {
        Map<String, String> originalConfiguration = new HashMap<String, String>();
        if (this.configuration != null) {
            ConfigurationSource configurationSource = getComponentManager().lookup(ConfigurationSource.class);

            if (configurationSource instanceof MockConfigurationSource) {
                MockConfigurationSource mockConfigurationSource = (MockConfigurationSource) configurationSource;

                for (Map.Entry<String, ? > entry : this.configuration.entrySet()) {
                    originalConfiguration.put(entry.getKey(),
                        mockConfigurationSource.<String>getProperty(entry.getKey()));
                    mockConfigurationSource.setProperty(entry.getKey(), entry.getValue());
                }
            }
        }

        try {
            runTestInternal();
        } finally {
            // Revert Configuration that have been set
            if (this.configuration != null) {
                ConfigurationSource configurationSource = getComponentManager().lookup(ConfigurationSource.class);

                if (configurationSource instanceof MockConfigurationSource) {
                    MockConfigurationSource mockConfigurationSource = (MockConfigurationSource) configurationSource;

                    for (Map.Entry<String, String> entry : originalConfiguration.entrySet()) {
                        mockConfigurationSource.setProperty(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
    }

    private void runTestInternal() throws Throwable
    {
        Parser parser = getComponentManager().lookup(Parser.class, this.parserId);
        XDOM xdom = parser.parse(new StringReader(this.input));

        if (this.runTransformations) {
            SyntaxFactory syntaxFactory = getComponentManager().lookup(SyntaxFactory.class);
            TransformationManager transformationManager = getComponentManager().lookup(TransformationManager.class);
            TransformationContext txContext = new TransformationContext(xdom,
                syntaxFactory.createSyntaxFromIdString(this.parserId));
            transformationManager.performTransformations(xdom, txContext);
        }

        BlockRenderer renderer = getComponentManager().lookup(BlockRenderer.class, this.targetSyntaxId);
        WikiPrinter printer = new DefaultWikiPrinter();
        renderer.render(xdom, printer);

        assertEquals(this.expected, printer.toString());
    }

    public void setComponentManager(ComponentManager componentManager)
    {
        this.componentManager = componentManager;
    }

    public ComponentManager getComponentManager()
    {
        return this.componentManager;
    }
}
