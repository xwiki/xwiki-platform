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

import java.io.ByteArrayInputStream;
import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.export.pdf.test.po.PDFDocument;
import org.xwiki.test.docker.internal.junit5.DockerTestUtils;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.servletengine.ServletEngine;

/**
 * Prevent the execution of the PDF export tests from inside a Docker container (such as {@code xwiki-build}) when
 * {@link ServletEngine#JETTY_STANDALONE} is used.
 * 
 * @version $Id$
 * @since 14.9
 * @see <a href="https://jira.xwiki.org/browse/XWIKI-20085">XWIKI-20085: PDFExportIT#exportAsPDF is failing when
 *      executed from a container</a>
 */
public class PDFExportExecutionCondition implements ExecutionCondition, BeforeEachCallback, AfterEachCallback
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PDFExportExecutionCondition.class);

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context)
    {
        if (DockerTestUtils.isInAContainer()) {
            TestConfiguration configuration = DockerTestUtils.getTestConfiguration(context);
            if (ServletEngine.JETTY_STANDALONE.equals(configuration.getServletEngine())) {
                return ConditionEvaluationResult.disabled(String.format(
                    "Servlet engine [%s] is forbidden "
                        + "when the PDF export tests are executed inside a Docker container, skipping.",
                    configuration.getServletEngine()));
            }
        }

        return ConditionEvaluationResult.enabled("The configured servlet engine is supported, continuing.");
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception
    {
        // Clear any previously created PDF documents.
        PDFDocument.BLOBS.clear();
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception
    {
        // Save the created PDF documents for debugging purposes.
        TestConfiguration configuration = DockerTestUtils.getTestConfiguration(context);
        for (int i = 0; i < PDFDocument.BLOBS.size(); i++) {
            savePDF(PDFDocument.BLOBS.get(i), i + ".pdf", context, configuration);
        }
    }

    private void savePDF(byte[] pdfBlob, String suffix, ExtensionContext context, TestConfiguration configuration)
    {
        String testName = context.getTestMethod().get().getName();

        try {
            File pdfFile = DockerTestUtils.getResultFileLocation(suffix, configuration, context);
            pdfFile.getParentFile().mkdirs();
            FileUtils.copyInputStreamToFile(new ByteArrayInputStream(pdfBlob), pdfFile);
            LOGGER.info("PDF for test [{}] saved at [{}].", testName, pdfFile.getAbsolutePath());
        } catch (Exception e) {
            LOGGER.error("Failed to save PDF for test [{}].", testName, e);
        }
    }
}
