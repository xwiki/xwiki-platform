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

import org.junit.Assert;
import org.junit.Test;

/**
 * Validate {@link LocaleUtils}.
 * 
 * @version $Id$
 */
public class LocaleUtilsTest
{
    @Test
    public void getParentLocale()
    {
        Assert.assertEquals(Locale.ENGLISH, LocaleUtils.getParentLocale(Locale.US));
        Assert.assertEquals(Locale.ROOT, LocaleUtils.getParentLocale(Locale.ENGLISH));
        Assert.assertEquals(null, LocaleUtils.getParentLocale(Locale.ROOT));
    }

    @Test
    public void toLocale()
    {
        Assert.assertEquals(null, LocaleUtils.toLocale(null));
        Assert.assertEquals(Locale.ROOT, LocaleUtils.toLocale(""));
        Assert.assertEquals(Locale.ENGLISH, LocaleUtils.toLocale("en"));
        Assert.assertEquals(Locale.US, LocaleUtils.toLocale("en_US"));

        Assert.assertEquals(Locale.FRENCH, LocaleUtils.toLocale("badLocale", Locale.FRENCH));
    }
}
