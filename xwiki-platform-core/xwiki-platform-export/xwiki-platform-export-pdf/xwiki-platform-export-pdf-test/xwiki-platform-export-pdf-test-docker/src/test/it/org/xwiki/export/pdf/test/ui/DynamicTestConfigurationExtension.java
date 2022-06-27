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
package org.xwiki.export.pdf.test.ui;

import java.util.Properties;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.Network;
import org.xwiki.test.docker.internal.junit5.DockerTestUtils;
import org.xwiki.test.docker.junit5.DockerTestException;
import org.xwiki.test.docker.junit5.TestConfiguration;

/**
 * Change settings from {@code xwiki.properties} dynamically before XWiki starts. We're not checking any execution
 * condition here. The reasons we're extending {@link ExecutionCondition} are:
 * <ul>
 * <li>the test configuration is initialized in the same way (in the same phase of the test life cycle)</li>
 * <li>we want to modify the text configuration as early as possible, before the servlet engine starts.</li>
 * </ul>
 * 
 * @version $Id$
 * @since 14.4.2
 * @since 14.5
 */
public class DynamicTestConfigurationExtension implements ExecutionCondition
{
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context)
    {
        TestConfiguration configuration = DockerTestUtils.getTestConfiguration(context);
        if (!configuration.getServletEngine().isOutsideDocker()) {
            // The servlet engine runs inside a Docker container so in order for the headless Chrome web browser (used
            // for PDF export) to access XWiki its own Docker container has to be in the same network and we also need
            // to pass the internal host name or IP address used by XWiki.
            Properties properties = new Properties();
            properties.put("xwikiPropertiesAdditionalProperties",
                String.format("export.pdf.dockerNetwork=%s\nexport.pdf.xwikiHost=%s\n", Network.SHARED.getId(),
                    configuration.getServletEngine().getInternalIP()));

            TestConfiguration pdfExportConfig = new TestConfiguration();
            pdfExportConfig.setProperties(properties);

            try {
                configuration.merge(pdfExportConfig);
            } catch (DockerTestException e) {
                throw new RuntimeException("Failed to merge PDF Export configuration.", e);
            }
        }
        return ConditionEvaluationResult.enabled("PDF export configuration merged.");
    }
}
