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
package org.xwiki.configuration.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.internal.test.AbstractTestDocumentConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.properties.ConverterManager;
import org.xwiki.rendering.async.AsyncContext;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AbstractWikisConfigurationSource}.
 * 
 * @version $Id$
 */
@OldcoreTest
class WikisConfigurationSourceTest extends AbstractTestDocumentConfigurationSource
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private AbstractWikisConfigurationSource wikisConfigSource;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private AsyncContext asyncContext;

    @MockComponent
    private EntityReferenceSerializer<String> referenceSerializer;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<EntityReference> currentRefDocRefResolver;

    @MockComponent
    private ConverterManager converter;

    private LocalDocumentReference configClassRef = new LocalDocumentReference("MyApp", "ConfigClass");

    private LocalDocumentReference configDocRef = new LocalDocumentReference("MyApp", "Config");

    private DocumentReference subWikiConfigDocRef =
        new DocumentReference(this.configDocRef, new WikiReference("design"));

    private DocumentReference mainWikiConfigDocRef =
        new DocumentReference(this.configDocRef, new WikiReference("xwiki"));

    @Override
    protected ConfigurationSource getConfigurationSource()
    {
        return this.wikisConfigSource;
    }

    @Override
    protected LocalDocumentReference getClassReference()
    {
        return this.configClassRef;
    }

    @Override
    @BeforeEach
    public void before() throws Exception
    {
        super.before();

        when(this.wikisConfigSource.getLocalDocumentReference())
            .thenReturn(new LocalDocumentReference("MyApp", "Config"));
        when(this.wikisConfigSource.getClassReference()).thenReturn(this.configClassRef);
        when(this.wikisConfigSource.getCacheId()).thenReturn("configuration.document.myApp");

        this.wikisConfigSource.wikiManager = this.oldcore.getWikiDescriptorManager();
        this.wikisConfigSource.xcontextProvider = this.xcontextProvider;
        this.wikisConfigSource.asyncContext = this.asyncContext;
        this.wikisConfigSource.referenceSerializer = this.referenceSerializer;
        this.wikisConfigSource.cache = this.mockCache;
        this.wikisConfigSource.converter = this.converter;

        when(this.oldcore.getWikiDescriptorManager().getCurrentWikiId()).then(new Answer<String>()
        {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable
            {
                return oldcore.getXWikiContext().getWikiId();
            }
        });
        when(this.oldcore.getWikiDescriptorManager().getMainWikiId()).thenReturn("xwiki");

        when(this.referenceSerializer.serialize(this.subWikiConfigDocRef)).thenReturn("design:MyApp.Config");
        when(this.referenceSerializer.serialize(this.mainWikiConfigDocRef)).thenReturn("xwiki:MyApp.Config");

        when(this.currentRefDocRefResolver.resolve(this.configClassRef, this.subWikiConfigDocRef))
            .thenReturn(new DocumentReference(this.configClassRef, this.subWikiConfigDocRef.getWikiReference()));
        when(this.currentRefDocRefResolver.resolve(this.configClassRef, this.mainWikiConfigDocRef))
            .thenReturn(new DocumentReference(this.configClassRef, this.mainWikiConfigDocRef.getWikiReference()));

        when(this.xcontextProvider.get()).thenReturn(this.oldcore.getXWikiContext());
        this.oldcore.getXWikiContext().setWikiId("design");

        setStringProperty(this.subWikiConfigDocRef, "color", "blue");
        setStringProperty(this.mainWikiConfigDocRef, "color", "red");
        setStringProperty(this.mainWikiConfigDocRef, "enabled", "true");

        when(this.mockConverter.convert(Boolean.class, "true")).thenReturn(true);
    }

    @Test
    void containsKey() throws Exception
    {
        assertFalse(this.wikisConfigSource.containsKey("age"));
        assertTrue(this.wikisConfigSource.containsKey("color"));
        assertTrue(this.wikisConfigSource.containsKey("enabled"));
    }

    @Test
    void getKeys() throws Exception
    {
        assertEquals(Arrays.asList("color", "enabled"), this.wikisConfigSource.getKeys());
    }

    @Test
    void getProperty()
    {
        assertEquals("blue", this.wikisConfigSource.getProperty("color"));
        assertEquals("true", this.wikisConfigSource.getProperty("enabled"));
        assertNull(this.wikisConfigSource.getProperty("age"));

        assertEquals("blue", this.wikisConfigSource.getProperty("color", "green"));
        assertEquals("18", this.wikisConfigSource.getProperty("age", "18"));

        assertEquals(true, this.wikisConfigSource.getProperty("enabled", Boolean.class));
        assertEquals(Collections.emptyList(), this.wikisConfigSource.getProperty("age", List.class));
    }

    @Test
    void isEmpty() throws Exception
    {
        assertFalse(this.wikisConfigSource.isEmpty());

        removeConfigObject(this.subWikiConfigDocRef);
        assertFalse(this.wikisConfigSource.isEmpty());

        removeConfigObject(this.mainWikiConfigDocRef);
        assertTrue(this.wikisConfigSource.isEmpty());
    }
}
