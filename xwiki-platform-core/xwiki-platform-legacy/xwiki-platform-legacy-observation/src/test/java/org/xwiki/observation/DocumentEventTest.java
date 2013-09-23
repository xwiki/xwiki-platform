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
package org.xwiki.observation;

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.observation.event.DocumentSaveEvent;
import org.xwiki.observation.event.filter.RegexEventFilter;

/**
 * Unit tests for {@link org.xwiki.observation.event.AbstractDocumentEvent}.
 * 
 * @version $Id$
 */
@Deprecated
public class DocumentEventTest
{
    @Test
    public void testMatchesWithEmptyConstructor()
    {
        DocumentSaveEvent event = new DocumentSaveEvent();
        Assert.assertTrue(event.matches(new DocumentSaveEvent("whatever")));
    }
    
    @Test
    public void testMatchesWithDocumentNameConstructor()
    {
        DocumentSaveEvent event = new DocumentSaveEvent("document");
        Assert.assertTrue(event.matches(new DocumentSaveEvent("document")));
        Assert.assertFalse(event.matches(new DocumentSaveEvent("wrong")));
    }
    
    @Test
    public void testMatchesWithRegexFilter()
    {
        DocumentSaveEvent event = new DocumentSaveEvent(new RegexEventFilter(".*Doc.*"));
        Assert.assertTrue(event.matches(new DocumentSaveEvent("some.Document")));
        Assert.assertFalse(event.matches(new DocumentSaveEvent("some.document")));
    }
}
