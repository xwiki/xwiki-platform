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
package com.xpn.xwiki.internal.template;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.Test;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.environment.Environment;
import org.xwiki.template.Template;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
@ComponentTest
class InternalTemplateManagerTest
{
    private static final String FS_TEMPLATE_PATH = "/templates/template";

    private static final String FS_TEMPLATE_ID = TemplateSkinResource.createId(FS_TEMPLATE_PATH);

    @MockComponent
    private CacheManager cacheManager;

    @MockComponent
    private Environment environment;

    @InjectMockComponents
    private InternalTemplateManager templates;

    private Cache<Template> cache;

    @AfterComponent
    private void afterComponent() throws CacheException
    {
        this.cache = mock();
        when(this.cacheManager.<Template>createNewCache(any())).thenReturn(this.cache);
    }

    @Test
    void getFilesystemTemplateWhenTemplateDoesNotExist()
    {
        assertNull(this.templates.getTemplate("template"));

        verify(this.cache, never()).remove(any());
        verify(this.cache, times(1)).get(FS_TEMPLATE_ID);
    }

    @Test
    void getFilesystemTemplateWhenTemplateDoesNotExistAnymore()
    {
        when(this.cache.get(FS_TEMPLATE_ID)).thenReturn(mock());

        assertNull(this.templates.getTemplate("template"));

        verify(this.cache, times(1)).get(FS_TEMPLATE_ID);
        verify(this.cache, times(1)).remove(any());
    }

    @Test
    void getFilesystemTemplateWhenTemplateExist() throws MalformedURLException
    {
        when(this.environment.getResource(FS_TEMPLATE_PATH)).thenReturn(new URL("file://templates/template"));

        Template template = this.templates.getTemplate("template");

        assertNotNull(template);
        assertEquals(FS_TEMPLATE_ID, template.getId());
        assertEquals(FS_TEMPLATE_PATH, template.getPath());

        verify(this.cache, times(2)).get(FS_TEMPLATE_ID);
        verify(this.cache, never()).remove(any());
    }
}
