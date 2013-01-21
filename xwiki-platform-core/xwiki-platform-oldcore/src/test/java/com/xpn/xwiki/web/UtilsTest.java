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

import java.net.URL;

import junit.framework.Assert;

import org.jmock.Expectations;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.AbstractBridgedComponentTestCase;

/**
 * Unit tests for {@link Utils}.
 * 
 * @version $Id$
 */
public class UtilsTest extends AbstractBridgedComponentTestCase
{
    /**
     * Tests that the returned redirect URL is relative to the servlet container root when it's not taken from a request
     * parameter (in which case the code that sets the request parameter needs to make sure the URL is relative).
     * 
     * @throws Exception if something goes wrong
     */
    @Test
    public void getRedirectReturnsRelativeURL() throws Exception
    {
        final XWikiURLFactory mockURLFactory = getMockery().mock(XWikiURLFactory.class);
        final DocumentReference documentReference = new DocumentReference("Page", "Space", "xwiki");
        final String action = "edit";
        final String queryString = "editor=class";
        final URL redirectURL = new URL("http://localhost:8080/xwiki/bin/edit/Space/Page?editor=class");
        final String redirectPath = "/xwiki/bin/edit/Space/Page?editor=class";
        getContext().setDoc(new XWikiDocument(documentReference));
        getContext().setURLFactory(mockURLFactory);
        getMockery().checking(new Expectations()
        {
            {
                allowing(mockURLFactory).createURL(documentReference.getLastSpaceReference().getName(),
                    documentReference.getName(), action, queryString, null,
                    documentReference.getWikiReference().getName(), getContext());
                will(returnValue(redirectURL));

                // This method is supposed to return a relative URL whenever possible.
                allowing(mockURLFactory).getURL(redirectURL, getContext());
                will(returnValue(redirectPath));
            }
        });
        Assert.assertEquals(redirectPath, Utils.getRedirect(action, queryString));
    }
}
