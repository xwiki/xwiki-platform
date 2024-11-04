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
package org.xwiki.localization;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validate {@link LocaleUtils}.
 * 
 * @version $Id$
 */
class LocaleUtilsTest
{
    @Test
    void getParentLocale()
    {
        assertEquals(Locale.ENGLISH, LocaleUtils.getParentLocale(Locale.US));
        assertEquals(Locale.ROOT, LocaleUtils.getParentLocale(Locale.ENGLISH));
        assertEquals(null, LocaleUtils.getParentLocale(Locale.ROOT));
    }

    @Test
    void toLocale()
    {
        assertEquals(null, LocaleUtils.toLocale((String) null));
        assertEquals(Locale.ROOT, LocaleUtils.toLocale(""));
        assertEquals(Locale.ENGLISH, LocaleUtils.toLocale("en"));
        assertEquals(Locale.US, LocaleUtils.toLocale("en_US"));

        assertEquals(Locale.FRENCH, LocaleUtils.toLocale("badLocale", Locale.FRENCH));
    }

    @Test
    void isValid()
    {
        assertTrue(LocaleUtils.isValid(Locale.ROOT));
        assertTrue(LocaleUtils.isValid(Locale.ENGLISH));
        assertTrue(LocaleUtils.isValid(Locale.US));

        assertFalse(LocaleUtils.isValid(null));
        assertFalse(LocaleUtils.isValid(new Locale.Builder().setLanguage("zh").setScript("Hans").build()));
    }
}
