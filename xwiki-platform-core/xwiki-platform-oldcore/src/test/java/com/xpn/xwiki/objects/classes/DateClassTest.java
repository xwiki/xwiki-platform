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
package com.xpn.xwiki.objects.classes;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.internal.multi.DelegateComponentManager;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.DateProperty;
import com.xpn.xwiki.web.Utils;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DateClass}.
 * 
 * @version $Id$
 */
public class DateClassTest
{
    @Rule
    public MockitoComponentManagerRule mocker = new MockitoComponentManagerRule();

    private LocalizationContext localizationContext;

    @Before
    public void setUp() throws Exception
    {
        DelegateComponentManager contextCM = new DelegateComponentManager();
        contextCM.setComponentManager(mocker);
        mocker.registerComponent(ComponentManager.class, "context", contextCM);

        Utils.setComponentManager(mocker);

        localizationContext = mocker.registerMockComponent(LocalizationContext.class);
    }

    @Test
    public void fromString()
    {
        DateClass dateClass = new DateClass();
        dateClass.setDateFormat("MMMM yyyy");
        when(localizationContext.getCurrentLocale()).thenReturn(new Locale("ro"));

        BaseProperty property = dateClass.fromString("octombrie 2015");
        Date date = (Date) property.getValue();
        assertEquals("10 2015", new SimpleDateFormat("MM yyyy").format(date));
    }

    @Test
    public void toFormString() throws Exception
    {
        DateClass dateClass = new DateClass();
        dateClass.setDateFormat("MMMM yyyy");
        when(localizationContext.getCurrentLocale()).thenReturn(new Locale("ro"));

        DateProperty property = new DateProperty();
        property.setValue(new SimpleDateFormat("MM/yyyy").parse("10/2015"));

        assertEquals("octombrie 2015", dateClass.toFormString(property));
    }
}
