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
package com.xpn.xwiki.web;

import java.io.File;
import java.io.IOException;

import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.environment.Environment;
import org.xwiki.environment.internal.ServletEnvironment;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.test.AbstractBridgedComponentTestCase;

/**
 * Unit tests for {@link TempResourceAction}.
 * 
 * @version $Id$
 */
public class TempResourceActionTest extends AbstractBridgedComponentTestCase
{
    /**
     * The action being tested.
     */
    private TempResourceAction action;

    /**
     * The base directory.
     */
    private File base;

    private ExecutionContext executionContext;

    @Override
    public void setUp() throws Exception
    {
        base = new File(getClass().getResource("/").toURI());

        super.setUp();

        action = new TempResourceAction();
    }

    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        this.executionContext = new ExecutionContext();

        // Configure Servlet Environment defined in AbstractBridgedComponentTestCase so that it returns a good
        // temporary directory
        ServletEnvironment environment = (ServletEnvironment) getComponentManager().getInstance(Environment.class);
        environment.setTemporaryDirectory(base);

        final ExecutionContextManager mockExecutionContextManager =
            registerMockComponent(ExecutionContextManager.class);
        final Execution mockExecution = registerMockComponent(Execution.class);
        
        getMockery().checking(new Expectations()
        {
            {
                allowing(mockExecutionContextManager).initialize(with(any(ExecutionContext.class)));

                allowing(mockExecution).getContext(); will(returnValue(executionContext));
                allowing(mockExecution).removeContext();
            }
        });
    }

    /**
     * Creates an empty file at the specified path.
     * 
     * @param path the file path
     * @throws IOException if creating the empty file fails
     */
    private void createEmptyFile(String path) throws IOException
    {
        File emptyFile = new File(base, path);
        emptyFile.getParentFile().mkdirs();
        emptyFile.createNewFile();
        emptyFile.deleteOnExit();
    }

    /**
     * {@link TempResourceAction#getTemporaryFile(String, XWikiContext)} should return {@code null} if the given URI
     * doesn't match the known pattern.
     */
    @Test
    public void testGetTemporaryFileForBadURI() throws Exception
    {
        createEmptyFile("temp/secret.txt");
        Assert.assertNull(action.getTemporaryFile("/xwiki/bin/temp/secret.txt", getContext()));
    }

    /**
     * {@link TempResourceAction#getTemporaryFile(String, XWikiContext)} should prevent access to files outside the
     * temporary directory by ignoring relative URIs (i.e. which use ".." to move to the parent folder).
     */
    @Test
    public void testGetTemporaryFileForRelativeURI() throws Exception
    {
        createEmptyFile("temp/secret.txt");
        Assert.assertNull(action.getTemporaryFile("/xwiki/bin/temp/../../module/secret.txt", getContext()));
    }

    /**
     * Tests {@link TempResourceAction#getTemporaryFile(String, XWikiContext)} when the file is missing.
     */
    @Test
    public void testGetTemporaryFileMissing() throws Exception
    {
        Assert.assertFalse(new File(base, "temp/module/xwiki/Space/Page/file.txt").exists());
        Assert.assertNull(action.getTemporaryFile("/xwiki/bin/temp/Space/Page/module/file.txt", getContext()));
    }

    /**
     * Tests {@link TempResourceAction#getTemporaryFile(String, XWikiContext)} when the file is present.
     */
    @Test
    public void testGetTemporaryFile() throws Exception
    {
        getContext().setWikiId("wiki");
        createEmptyFile("temp/module/wiki/Space/Page/file.txt");
        Assert.assertNotNull(action.getTemporaryFile("/xwiki/bin/temp/Space/Page/module/file.txt", getContext()));
    }

    /**
     * Tests {@link TempResourceAction#getTemporaryFile(String, XWikiContext)} when the URL is over encoded.
     */
    @Test
    public void testGetTemporaryFileForOverEncodedURL() throws Exception
    {
        createEmptyFile("temp/officeviewer/xwiki/Sp*ace/Pa-ge/presentation.odp/presentation-slide0.jpg");
        Assert.assertNotNull(action.getTemporaryFile(
            "/xwiki/bin/temp/Sp%2Aace/Pa%2Dge/officeviewer/presentation.odp/presentation-slide0.jpg", getContext()));
    }

    /**
     * Tests {@link TempResourceAction#getTemporaryFile(String, XWikiContext)} when the URL is partially decoded. This
     * can happen for instance when XWiki is behind Apache's {@code mode_proxy} with {@code nocanon} option disabled.
     */
    @Test
    public void testGetTemporaryFileForPartiallyDecodedURL() throws Exception
    {
        createEmptyFile("temp/officeviewer/xwiki/Space/Page/"
            + "attach%3Axwiki%3ASpace.Page%40pres%2Fentation.odp/13/presentation-slide0.jpg");
        Assert.assertNotNull(action.getTemporaryFile("/xwiki/bin/temp/Space/Page/officeviewer/"
            + "attach:xwiki:Space.Page@pres%2Fentation.odp/13/presentation-slide0.jpg", getContext()));
    }
}
