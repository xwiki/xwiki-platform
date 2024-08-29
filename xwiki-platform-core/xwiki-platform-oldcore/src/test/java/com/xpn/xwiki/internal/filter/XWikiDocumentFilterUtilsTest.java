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
package com.xpn.xwiki.internal.filter;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.internal.ContextComponentManagerProvider;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.input.DefaultURLInputSource;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.test.MockitoOldcoreRule;
import com.xpn.xwiki.test.component.XWikiDocumentFilterUtilsComponentList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Validate {@link XWikiDocumentFilterUtils}.
 * 
 * @version $Id$
 */
@XWikiDocumentFilterUtilsComponentList
@ComponentList(ContextComponentManagerProvider.class)
public class XWikiDocumentFilterUtilsTest
{
    public final MockitoComponentMockingRule<XWikiDocumentFilterUtils> mocker =
        new MockitoComponentMockingRule<>(XWikiDocumentFilterUtils.class);

    @Rule
    public final MockitoOldcoreRule oldcore = new MockitoOldcoreRule(this.mocker);

    private void assertXML(Object entity, String resource) throws FilterException, IOException, ComponentLookupException
    {
        URL url = getClass().getResource("/filter/xar/" + resource + ".xml");

        assertNotNull(url);

        String expected = IOUtils.toString(url, "UTF-8");
        expected = StringUtils.removeStart(expected, "<?xml version=\"1.1\" encoding=\"UTF-8\"?>\n\n");

        // Import

        Object importedEntity =
            this.mocker.getComponentUnderTest().importEntity(entity, new DefaultURLInputSource(url));

        // Export

        String actual = this.mocker.getComponentUnderTest().exportEntity(importedEntity);

        // Validate

        assertEquals(expected, actual);
    }

    // Tests

    @Test
    public void class1() throws FilterException, IOException, ComponentLookupException
    {
        assertXML(BaseClass.class, "class1");
    }
}
