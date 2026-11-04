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
package org.xwiki.wysiwyg.internal.cleaner;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.wysiwyg.cleaner.HTMLCleaner;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link DefaultHTMLCleaner}.
 * 
 * @version $Id$
 */
@AllComponents
public class DefaultHTMLCleanerTest extends HTMLCleanerTest
{
    @Test
    void cleanDoesNotModifyTheSpecificFilters() throws Exception
    {
        HTMLCleaner cleaner = getComponentManager().getInstance(HTMLCleaner.class);
        // The cleaner is a singleton that can be called concurrently, so it must not modify the list of specific
        // filters, which is shared by all the calls. We use an unmodifiable list to detect modifications.
        List<HTMLFilter> specificFilters = getComponentManager().getInstanceList(HTMLFilter.class);
        ReflectionUtils.setFieldValue(cleaner, "specificFilters", List.copyOf(specificFilters));

        assertEquals("""
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE html>
            <html><head></head><body><p>text</p></body></html>
            """, cleaner.clean("<p>text</p>"));
    }
}
