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
package com.xpn.xwiki.web;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for the {@link com.xpn.xwiki.web.XWikiMessageTool} class.
 * 
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
public class XWikiMessageToolTest
{
    @InjectMockitoOldcore
    MockitoOldcore oldcore;

    private XWikiMessageTool tool;

    private DocumentReference translationDocumentReference1;

    private DocumentReference translationDocumentReferenceFR1;

    private DocumentReference translationDocumentReference2;

    private DocumentReference translationDocumentReferenceFR2;

    private String translationDocumentName1;

    private String translationDocumentName2;

    @BeforeEach
    protected void beforeEach() throws Exception
    {
        this.tool = new XWikiMessageTool(new TestResources(), this.oldcore.getXWikiContext());

        this.translationDocumentReference1 =
            new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "TranslationSpace1", "TranslationPage1");

        this.translationDocumentReferenceFR1 = new DocumentReference(this.translationDocumentReference1, Locale.FRENCH);

        this.translationDocumentName1 = "TranslationSpace1.TranslationPage1";

        this.translationDocumentReference2 =
            new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "TranslationSpace1", "TranslationPage2");

        this.translationDocumentReferenceFR2 = new DocumentReference(this.translationDocumentReference2, Locale.FRENCH);

        this.translationDocumentName2 = "TranslationSpace2.TranslationPage2";
    }

    public class TestResources extends ListResourceBundle
    {
        private final Object[][] contents = { { "key", "value" } };

        @Override
        public Object[][] getContents()
        {
            return contents;
        }
    }

    private String toBundleString(DocumentReference... documentReferences)
    {
        StringBuilder builder = new StringBuilder();

        for (DocumentReference documentReference : documentReferences) {
            builder.append(documentReference.getLastSpaceReference().getName());
            builder.append('.');
            builder.append(documentReference.getName());

            builder.append(',');
        }

        return builder.toString();
    }

    private void setPreferencesTranslationBundle(DocumentReference... documentReferences)
    {
        this.oldcore.getMockWikiConfigurationSource().setProperty("documentBundles",
            toBundleString(documentReferences));
    }

    private void setCfgTranslationBundle(DocumentReference... documentReferences)
    {
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.documentBundles", toBundleString(documentReferences));
    }

    private void saveTranslations(String content) throws XWikiException
    {
        saveTranslations(content, true);
    }

    private void saveTranslations(String content, boolean preferencesBundle) throws XWikiException
    {
        saveTranslations(content, null, preferencesBundle);
    }

    private void saveTranslations(String content, Date date) throws XWikiException
    {
        saveTranslations(content, date, true);
    }

    private void saveTranslations(String content, Date date, boolean preferencesBundle) throws XWikiException
    {
        saveTranslations(this.translationDocumentReference1, content, date, preferencesBundle);
    }

    private void saveTranslations(DocumentReference documentReference, String content) throws XWikiException
    {
        saveTranslations(documentReference, content, null, true);
    }

    private void saveTranslations(DocumentReference documentReference, String content, Date date,
        boolean preferencesBundle) throws XWikiException
    {
        // Set the document as translation bundle
        if (preferencesBundle) {
            setPreferencesTranslationBundle(documentReference);
        } else {
            setCfgTranslationBundle(documentReference);
        }

        XWikiDocument document =
            this.oldcore.getSpyXWiki().getDocument(documentReference, this.oldcore.getXWikiContext());

        document.setContent(content);

        // Force a specific date
        if (date != null) {
            document.setDate(date);
            document.setMetaDataDirty(false);
            document.setContentDirty(false);
        }

        this.oldcore.getSpyXWiki().saveDocument(document, this.oldcore.getXWikiContext());
    }

    // Tests

    /**
     * When no preference exist the returned value is the value of the key.
     */
    @Test
    public void testGetWhenPreferenceDoesNotExist()
    {
        assertEquals("invalid", this.tool.get("invalid"));
    }

    @Test
    public void testGetWhenNoTranslationAvailable()
    {
        assertEquals("value", this.tool.get("key"));
    }

    /**
     * When the key is null the returned value is null.
     */
    @Test
    public void testGetWhenKeyIsNull()
    {
        assertNull(this.tool.get(null));
    }

    @Test
    public void testGetWhenInXWikiPreferences() throws XWikiException
    {
        saveTranslations(this.translationDocumentReference1, "somekey=somevalue");
        saveTranslations(this.translationDocumentReference2, "someKey=someValue\n" + "keyInXWikiPreferences=eureka");
        setPreferencesTranslationBundle(this.translationDocumentReference1, this.translationDocumentReference2);

        assertEquals("eureka", this.tool.get("keyInXWikiPreferences"));
    }

    @Test
    public void testGetWhenInXWikiConfigurationFile() throws XWikiException
    {
        saveTranslations("keyInXWikiCfg=gotcha", false);

        assertEquals("gotcha", this.tool.get("keyInXWikiCfg"));
    }

    /**
     * Validate usage of parameters in bundles
     * 
     * @throws XWikiException
     */
    @Test
    public void testGetWithParameters() throws XWikiException
    {
        saveTranslations("key=We have {0} new documents with {1} objects. {2}");

        List<String> params = new ArrayList<String>();
        params.add("12");
        params.add("3");

        assertEquals("We have 12 new documents with 3 objects. {2}", this.tool.get("key", params));
    }

    /**
     * Verify that a document listed as a bundle document that doesn't exist is not returned as a bundle document.
     */
    @Test
    public void testGetDocumentBundlesWhenDocumentDoesNotExist()
    {
        setPreferencesTranslationBundle(this.translationDocumentReference1);
        setCfgTranslationBundle(this.translationDocumentReference2);

        List<XWikiDocument> docs = this.tool.getDocumentBundles();
        assertEquals(0, docs.size());
    }

    @Test
    public void testGetWhenDocumentModifiedAfterItIsInCache() throws XWikiException
    {
        saveTranslations("key=value");

        // First time get any key just to put the doc properties in cache
        assertEquals("modifiedKey", this.tool.get("modifiedKey"));

        // Now modify the document content to add a new key and change the document's date. We add
        // one second to ensure the new date is definitely newer than the old one.
        saveTranslations("modifiedKey=found", new Date(System.currentTimeMillis() + 1000L));

        // Even though the document has been cached it's reloaded because its date has changed
        assertEquals("found", this.tool.get("modifiedKey"));
    }

    @Test
    public void testGetWhenWithTranslation() throws XWikiException
    {
        saveTranslations(this.translationDocumentReference1, "somekey=somevalue\nsomekey2=somevalue2");
        saveTranslations(this.translationDocumentReferenceFR1, "somekey=somevaluetrans");

        this.oldcore.getXWikiContext().setLanguage("en");
        assertEquals("somevalue", this.tool.get("somekey"));
        assertEquals("somevalue2", this.tool.get("somekey2"));

        // Switch to french
        this.oldcore.getXWikiContext().setLanguage("fr");
        assertEquals("somevaluetrans", this.tool.get("somekey"));
        assertEquals("somevalue2", this.tool.get("somekey2"));
    }

    @Test
    public void testGetWhenWithUTF8Translation() throws XWikiException
    {
        saveTranslations(this.translationDocumentReference1, "somekey=some\u00E9value\nsomekey2=some\\u00E9value2");
        saveTranslations(this.translationDocumentReferenceFR1, "somekey=somevaluetrans");

        assertEquals("some\u00E9value", this.tool.get("somekey"));
        assertEquals("some\u00E9value2", this.tool.get("somekey2"));
    }
}
