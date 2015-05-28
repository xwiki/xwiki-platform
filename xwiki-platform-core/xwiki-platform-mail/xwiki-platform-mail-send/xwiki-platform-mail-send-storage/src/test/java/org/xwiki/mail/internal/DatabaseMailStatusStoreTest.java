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
package org.xwiki.mail.internal;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link DatabaseMailStatusStore}.
 *
 * @version $Id$
 * @since 6.4RC1
 */
public class DatabaseMailStatusStoreTest
{
    @Rule
    public MockitoComponentMockingRule<DatabaseMailStatusStore> mocker =
        new MockitoComponentMockingRule<>(DatabaseMailStatusStore.class);

    @Test
    public void computeSelectQueryString() throws Exception
    {
        Map<String, Object> filterMap = new LinkedHashMap<>();
        filterMap.put("status", "failed");
        filterMap.put("wiki", "mywiki");

        assertEquals("from org.xwiki.mail.MailStatus where mail_status like :status and mail_wiki like :wiki order by date desc",
            this.mocker.getComponentUnderTest().computeSelectQueryString(filterMap, "date", false));
    }

    @Test
    public void computeCountQueryString() throws Exception
    {
        Map<String, Object> filterMap = new LinkedHashMap<>();
        filterMap.put("status", "failed");
        filterMap.put("wiki", "mywiki");

        assertEquals("select count(*) from org.xwiki.mail.MailStatus where mail_status like :status and "
            + "mail_wiki like :wiki", this.mocker.getComponentUnderTest().computeCountQueryString(filterMap));
    }
}
