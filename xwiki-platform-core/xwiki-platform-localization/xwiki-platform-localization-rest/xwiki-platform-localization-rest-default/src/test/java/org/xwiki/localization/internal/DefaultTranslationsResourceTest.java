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
package org.xwiki.localization.internal;

import java.io.StringWriter;
import java.util.Locale;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.localization.rest.model.jaxb.ObjectFactory;
import org.xwiki.localization.rest.model.jaxb.Translations;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import ch.qos.logback.classic.Level;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link DefaultTranslationsResource}.
 *
 * @version $Id$
 * @since 13.3RC1
 */
@ComponentTest
class DefaultTranslationsResourceTest
{
    private static final DocumentReference CURRENT_ENTITY =
        new DocumentReference("currentwiki", "XWiki", "CurrentEntity");

    @InjectMockComponents
    private DefaultTranslationsResource defaultTranslationResource;

    @MockComponent
    private LocalizationContext localizationContext;

    @MockComponent
    private LocalizationManager localizationManager;

    @MockComponent
    private ModelContext modelContext;

    private final Locale defaultLocale = Locale.forLanguageTag("es");

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Mock
    private Translation translationKey1;

    @Mock
    private Translation translationKey2;

    @BeforeEach
    void setUp()
    {
        when(this.localizationContext.getCurrentLocale()).thenReturn(this.defaultLocale);
        when(this.modelContext.getCurrentEntityReference()).thenReturn(CURRENT_ENTITY);
    }

    @Test
    void getTranslations() throws Exception
    {
        ObjectFactory objectFactory = new ObjectFactory();
        Translations expected = objectFactory.createTranslations();
        expected.getTranslations().addAll(asList(
            createTranslation(objectFactory, "key1", "value1"),
            createTranslation(objectFactory, "key2", "value2")
        ));

        when(this.localizationManager.getTranslation("key1", this.defaultLocale)).thenReturn(this.translationKey1);
        when(this.localizationManager.getTranslation("key2", this.defaultLocale)).thenReturn(this.translationKey2);
        when(this.translationKey1.getRawSource()).thenReturn("value1");
        when(this.translationKey2.getRawSource()).thenReturn("value2");

        Translations response =
            this.defaultTranslationResource.getTranslations("mywiki", null, null, asList("key1", "key2"));

        assertEquals(marshal(expected), marshal(response));

        verify(this.localizationManager).getTranslation("key1", this.defaultLocale);
        verify(this.localizationManager).getTranslation("key2", this.defaultLocale);
        verify(this.modelContext).setCurrentEntityReference(new WikiReference("mywiki"));
        verify(this.modelContext).setCurrentEntityReference(CURRENT_ENTITY);
    }

    @Test
    void getTranslationsNotFound() throws Exception
    {
        ObjectFactory objectFactory = new ObjectFactory();
        Translations expected = objectFactory.createTranslations();
        expected.getTranslations().addAll(asList(
            createTranslation(objectFactory, "key1", null),
            createTranslation(objectFactory, "key2", "value2")
        ));

        when(this.localizationManager.getTranslation("key1", this.defaultLocale)).thenReturn(null);
        when(this.localizationManager.getTranslation("key2", this.defaultLocale)).thenReturn(this.translationKey2);
        when(this.translationKey2.getRawSource()).thenReturn("value2");

        Translations response =
            this.defaultTranslationResource.getTranslations("mywiki", null, null, asList("key1", "key2"));

        assertEquals(marshal(expected), marshal(response));

        assertEquals(1, this.logCapture.size());
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
        assertEquals("Translation key [key1] not found for locale [es] in wiki [mywiki].",
            this.logCapture.getMessage(0));

        verify(this.localizationManager).getTranslation("key1", this.defaultLocale);
        verify(this.localizationManager).getTranslation("key2", this.defaultLocale);
        verify(this.modelContext).setCurrentEntityReference(new WikiReference("mywiki"));
        verify(this.modelContext).setCurrentEntityReference(CURRENT_ENTITY);
    }

    @Test
    void getTranslationsNotFoundWithPrefix() throws Exception
    {
        ObjectFactory objectFactory = new ObjectFactory();
        Translations expected = objectFactory.createTranslations();
        expected.getTranslations().addAll(singletonList(createTranslation(objectFactory, "prefix.key1", null)));

        Translations response =
            this.defaultTranslationResource.getTranslations("mywiki", null, "prefix.", singletonList("key1"));

        assertEquals(marshal(expected), marshal(response));

        assertEquals(1, this.logCapture.size());
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
        assertEquals("Translation key [prefix.key1] not found for locale [es] in wiki [mywiki].",
            this.logCapture.getMessage(0));

        verify(this.localizationManager).getTranslation("prefix.key1", this.defaultLocale);
        verify(this.modelContext).setCurrentEntityReference(new WikiReference("mywiki"));
        verify(this.modelContext).setCurrentEntityReference(CURRENT_ENTITY);
    }

    @Test
    void getTranslationsWithLocale() throws Exception
    {
        ObjectFactory objectFactory = new ObjectFactory();
        Translations expected = objectFactory.createTranslations();
        expected.getTranslations().addAll(asList(
            createTranslation(objectFactory, "key1", "value1"),
            createTranslation(objectFactory, "key2", "value2")
        ));

        Locale localeFr = Locale.forLanguageTag("fr");
        when(this.localizationManager.getTranslation("key1", localeFr)).thenReturn(this.translationKey1);
        when(this.localizationManager.getTranslation("key2", localeFr)).thenReturn(this.translationKey2);
        when(this.translationKey1.getRawSource()).thenReturn("value1");
        when(this.translationKey2.getRawSource()).thenReturn("value2");

        Translations response =
            this.defaultTranslationResource.getTranslations("mywiki", "fr", null, asList("key1", "key2"));

        assertEquals(marshal(expected), marshal(response));

        verify(this.localizationManager).getTranslation("key1", localeFr);
        verify(this.localizationManager).getTranslation("key2", localeFr);
        verify(this.modelContext).setCurrentEntityReference(new WikiReference("mywiki"));
        verify(this.modelContext).setCurrentEntityReference(CURRENT_ENTITY);
    }

    @Test
    void getTranslationsWithPrefix() throws Exception
    {
        ObjectFactory objectFactory = new ObjectFactory();
        Translations expected = objectFactory.createTranslations();
        expected.getTranslations().addAll(asList(
            createTranslation(objectFactory, "key1", "value1"),
            createTranslation(objectFactory, "key2", "value2")
        ));

        when(this.localizationManager.getTranslation("key1", this.defaultLocale)).thenReturn(this.translationKey1);
        when(this.localizationManager.getTranslation("key2", this.defaultLocale)).thenReturn(this.translationKey2);
        when(this.translationKey1.getRawSource()).thenReturn("value1");
        when(this.translationKey2.getRawSource()).thenReturn("value2");

        Translations response =
            this.defaultTranslationResource.getTranslations("mywiki", null, "key", asList("1", "2"));

        assertEquals(marshal(expected), marshal(response));

        verify(this.localizationManager).getTranslation("key1", this.defaultLocale);
        verify(this.localizationManager).getTranslation("key2", this.defaultLocale);
        verify(this.modelContext).setCurrentEntityReference(new WikiReference("mywiki"));
        verify(this.modelContext).setCurrentEntityReference(CURRENT_ENTITY);
    }

    @Test
    void getTranslationsMissingKey() throws Exception
    {
        when(this.localizationManager.getTranslation("key1", this.defaultLocale)).thenReturn(this.translationKey1);
        when(this.localizationManager.getTranslation("key2", this.defaultLocale)).thenReturn(this.translationKey2);
        when(this.translationKey1.getRawSource()).thenReturn("value1");
        when(this.translationKey2.getRawSource()).thenReturn("value2");

        Translations response =
            this.defaultTranslationResource.getTranslations("mywiki", null, "key", asList((String) null));

        assertEquals(marshal(new ObjectFactory().createTranslations()), marshal(response));

        verify(this.modelContext).setCurrentEntityReference(new WikiReference("mywiki"));
        verify(this.modelContext).setCurrentEntityReference(CURRENT_ENTITY);
    }

    private org.xwiki.localization.rest.model.jaxb.Translation createTranslation(ObjectFactory objectFactory,
        String key, String rawSource)
    {
        org.xwiki.localization.rest.model.jaxb.Translation mapEntry = objectFactory.createTranslation();
        mapEntry.setKey(key);
        mapEntry.setRawSource(rawSource);
        return mapEntry;
    }

    private String marshal(Translations translations) throws JAXBException
    {
        // We need to marshal because jaxb generated objects does not have equalily operations.
        JAXBContext jaxbContext = JAXBContext.newInstance(Translations.class);
        StringWriter writer = new StringWriter();
        jaxbContext.createMarshaller().marshal(translations, writer);
        return writer.toString();
    }
}
