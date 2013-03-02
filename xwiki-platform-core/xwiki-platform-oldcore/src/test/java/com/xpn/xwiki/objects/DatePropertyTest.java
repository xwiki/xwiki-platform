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
package com.xpn.xwiki.objects;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.internal.reference.LocalStringEntityReferenceSerializer;
import org.xwiki.test.ComponentManagerRule;
import org.xwiki.test.annotation.ComponentList;

import com.xpn.xwiki.web.Utils;

import junit.framework.Assert;

/**
 * Unit tests for {@link DateProperty}.
 *
 * @version $Id$
 * @since 5.0M1
 */
@ComponentList({
    LocalStringEntityReferenceSerializer.class
})
public class DatePropertyTest
{
    @Rule
    public ComponentManagerRule componentManager = new ComponentManagerRule();

    /**
     * Verify that we can set a date that is null (<a href="http://jira.xwiki.org/browse/XWIKI-8837">XWIKI-8837</a>).
     */
    @Test
    public void setValueWithNullDate()
    {
        Utils.setComponentManager(this.componentManager);
        DateProperty property = new DateProperty();
        property.setValue(null);
        Assert.assertNull(property.getValue());
    }
}
