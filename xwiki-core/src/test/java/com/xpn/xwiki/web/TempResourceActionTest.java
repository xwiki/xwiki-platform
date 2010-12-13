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

import junit.framework.Assert;

import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.container.ApplicationContext;
import org.xwiki.container.Container;

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
     * {@inheritDoc}
     * 
     * @see AbstractBridgedComponentTestCase#registerComponents()
     */
    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        final Container mockContainer = registerMockComponent(Container.class);
        final ApplicationContext mockAppContext = registerMockComponent(ApplicationContext.class);

        getMockery().checking(new Expectations()
        {
            {
                ignoring(mockContainer).setApplicationContext(with(any(ApplicationContext.class)));
                allowing(mockContainer).getApplicationContext();
                will(returnValue(mockAppContext));
                allowing(mockAppContext).getTemporaryDirectory();
                will(returnValue(new File(getClass().getResource("/").toURI())));
            }
        });
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractBridgedComponentTestCase#setUp()
     */
    @Override
    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        action = new TempResourceAction();
    }

    /**
     * {@link TempResourceAction#getTemporaryFile(String, XWikiContext)} should return {@code null} if the given URI
     * doesn't match the known pattern.
     */
    @Test
    public void testGetTemporaryFileForBadURI()
    {
        Assert.assertNull(action.getTemporaryFile("/xwiki/bin/temp/secret.txt", getContext()));
    }

    /**
     * {@link TempResourceAction#getTemporaryFile(String, XWikiContext)} should prevent access to files outside the
     * temporary directory by ignoring relative URIs (i.e. which use ".." to move to the parent folder).
     */
    @Test
    public void testGetTemporaryFileForRelativeURI()
    {
        Assert.assertNull(action.getTemporaryFile("/xwiki/bin/temp/../../module/secret.txt", getContext()));
    }

    /**
     * Tests {@link TempResourceAction#getTemporaryFile(String, XWikiContext)} when the file is missing.
     */
    @Test
    public void testGetTemporaryFileMissing()
    {
        Assert.assertNull(action.getTemporaryFile("/xwiki/bin/temp/Space/Page/module/file.txt", getContext()));
    }

    /**
     * Tests {@link TempResourceAction#getTemporaryFile(String, XWikiContext)} when the file is present.
     */
    @Test
    public void testGetTemporaryFile()
    {
        getContext().setDatabase("wiki");
        Assert.assertNotNull(action.getTemporaryFile("/xwiki/bin/temp/Space/Page/module/file.txt", getContext()));
    }
}
