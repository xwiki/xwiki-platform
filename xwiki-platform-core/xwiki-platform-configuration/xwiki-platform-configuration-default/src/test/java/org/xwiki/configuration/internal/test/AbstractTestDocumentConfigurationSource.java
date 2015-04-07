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
package org.xwiki.configuration.internal.test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Type;

import org.junit.Before;
import org.junit.Rule;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.internal.AbstractDocumentConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.properties.ConverterManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcoreRule;

/**
 * Utility to test all extensions of {@link AbstractDocumentConfigurationSource}.
 * 
 * @version $Id$
 */
public abstract class AbstractTestDocumentConfigurationSource
{
    @Rule
    public MockitoOldcoreRule oldcore;

    public MockitoComponentMockingRule<ConfigurationSource> componentManager;

    protected static final String CURRENT_WIKI = "currentwiki";

    protected Cache<Object> mockCache;

    protected ConverterManager mockConverter;

    public AbstractTestDocumentConfigurationSource(Class<? extends ConfigurationSource> clazz)
    {
        this.componentManager = new MockitoComponentMockingRule<ConfigurationSource>(clazz);
        this.oldcore = new MockitoOldcoreRule(this.componentManager);
    }

    protected abstract LocalDocumentReference getClassReference();

    @Before
    public void before() throws Exception
    {
        this.mockCache = mock(Cache.class);
        this.mockConverter = this.componentManager.getInstance(ConverterManager.class);

        when(this.mockConverter.convert(any(Type.class), anyObject())).then(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                return invocation.getArguments()[1];
            }
        });

        CacheManager cacheManager = this.componentManager.getInstance(CacheManager.class);

        when(cacheManager.createNewCache(any(CacheConfiguration.class))).thenReturn(this.mockCache);

        WikiDescriptorManager wikiManager = this.componentManager.getInstance(WikiDescriptorManager.class);
        when(wikiManager.getCurrentWikiId()).thenReturn(CURRENT_WIKI);

        DocumentReferenceResolver<EntityReference> mockCurrentEntityResolver =
            this.componentManager.registerMockComponent(DocumentReferenceResolver.TYPE_REFERENCE, "current");
        when(mockCurrentEntityResolver.resolve(eq(getClassReference()), any(DocumentReference.class))).thenReturn(
            new DocumentReference(getClassReference(), new WikiReference(CURRENT_WIKI)));
    }

    protected void setStringProperty(DocumentReference documentReference, String propertyName, String propertyValue)
        throws XWikiException
    {
        XWikiContext xcontext = this.oldcore.getXWikiContext();

        XWikiDocument document = this.oldcore.getXWikiContext().getWiki().getDocument(documentReference, xcontext);

        LocalDocumentReference classReference = getClassReference();

        BaseObject baseOject = document.getXObject(classReference);
        if (baseOject == null) {
            baseOject = new BaseObject();
            baseOject.setDocumentReference(documentReference);
            baseOject.setXClassReference(classReference);
            document.addXObject(baseOject);
        }

        baseOject.setStringValue(propertyName, propertyValue);

        xcontext.getWiki().saveDocument(document, xcontext);
    }

    protected void resetCache()
    {
        this.mockCache = mock(Cache.class);
    }

    protected void setCache(String property, Object value)
    {
        when(this.mockCache.get(property)).thenReturn(value);
    }
}
