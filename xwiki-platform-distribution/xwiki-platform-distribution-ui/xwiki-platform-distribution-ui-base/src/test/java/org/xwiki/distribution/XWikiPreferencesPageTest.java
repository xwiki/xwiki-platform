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
package org.xwiki.distribution;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.suigeneris.jrcs.diff.delta.Delta;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.PageTest;

import com.xpn.xwiki.doc.MandatoryDocumentInitializer;
import com.xpn.xwiki.doc.MetaDataDiff;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.XWikiPreferencesDocumentInitializer;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.ObjectDiff;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Page test for {@code XWiki.XWikiPreferences} verifying that the XAR XML and the mandatory document initializer are
 * kept in sync.
 *
 * @version $Id$
 */
@ComponentList({
    XWikiPreferencesDocumentInitializer.class
})
class XWikiPreferencesPageTest extends PageTest
{
    private static final DocumentReference XWIKI_PREFERENCES =
        new DocumentReference("xwiki", "XWiki", "XWikiPreferences");

    /**
     * Verifies that the mandatory document initializer does not make any changes to
     * {@code XWiki.XWikiPreferences} when loaded from the XAR. This ensures that the XAR is at least as
     * up-to-date as the mandatory initializer.
     */
    @Test
    void xmlIsUpToDateWithInitializer() throws Exception
    {
        XWikiDocument xmlDoc = loadPage(XWIKI_PREFERENCES);

        MandatoryDocumentInitializer initializer =
            this.componentManager.getInstance(MandatoryDocumentInitializer.class,
                XWikiPreferencesDocumentInitializer.LOCAL_REFERENCE_STRING);

        // Clone to avoid modifying the stored document, then run the initializer on the clone.
        XWikiDocument updatedDoc = xmlDoc.clone();
        initializer.updateDocument(updatedDoc);

        StringBuilder message = new StringBuilder();

        // Metadata diff (title, parent, hidden, author, etc.)
        List<MetaDataDiff> metaDiffs = xmlDoc.getMetaDataDiff(xmlDoc, updatedDoc, this.context);
        if (!metaDiffs.isEmpty()) {
            message.append("Metadata changes:\n");
            for (MetaDataDiff diff : metaDiffs) {
                message.append(String.format("  - %s: [%s] -> [%s]%n",
                    diff.getField(), diff.getPrevValue(), diff.getNewValue()));
            }
        }

        // Content diff
        List<Delta> contentDiffs = xmlDoc.getContentDiff(xmlDoc, updatedDoc, this.context);
        if (!contentDiffs.isEmpty()) {
            message.append("Content changes:\n");
            for (Delta delta : contentDiffs) {
                message.append(String.format("  %s%n", delta));
            }
        }

        // Object diff
        List<List<ObjectDiff>> objectDiffs = xmlDoc.getObjectDiff(xmlDoc, updatedDoc, this.context);
        if (!objectDiffs.isEmpty()) {
            message.append("Object changes:\n");
            for (List<ObjectDiff> objDiffList : objectDiffs) {
                for (ObjectDiff diff : objDiffList) {
                    message.append(String.format("  - [%s] property [%s]: %s (was: %s, now: %s)%n",
                        diff.getClassName(), diff.getPropName(), diff.getAction(),
                        diff.getPrevValue(), diff.getNewValue()));
                }
            }
        }

        // XClass diff
        List<List<ObjectDiff>> classDiffs = xmlDoc.getClassDiff(xmlDoc, updatedDoc, this.context);
        if (!classDiffs.isEmpty()) {
            message.append("XClass changes:\n");
            BaseClass xmlClass = xmlDoc.getXClass();
            BaseClass updatedClass = updatedDoc.getXClass();
            for (List<ObjectDiff> propDiffs : classDiffs) {
                for (ObjectDiff diff : propDiffs) {
                    String propName = diff.getPropName();
                    message.append(String.format("  - Property [%s]: %s%n", propName, diff.getAction()));
                    if (ObjectDiff.ACTION_PROPERTYCHANGED.equals(diff.getAction())) {
                        appendPropertyFieldDiffs(message, (PropertyClass) xmlClass.get(propName),
                            (PropertyClass) updatedClass.get(propName));
                    }
                }
            }
        }

        if (!message.isEmpty()) {
            fail("The mandatory document initializer modified XWikiPreferences loaded from the XAR. "
                + "This means the XAR is out of date. "
                + "Please update XWiki/XWikiPreferences.xml to match XWikiPreferencesDocumentInitializer.\n"
                + message);
        }
    }

    /**
     * Verifies that the XClass defined in the XAR matches what the mandatory document initializer produces.
     * This ensures that any XClass changes made in the XAR are also reflected in the initializer.
     */
    @Test
    void initializerMatchesXMLXClass() throws Exception
    {
        // Run the mandatory document initializer on a fresh document to obtain the expected XClass.
        XWikiDocument initializedDoc = new XWikiDocument(XWIKI_PREFERENCES);
        MandatoryDocumentInitializer initializer =
            this.componentManager.getInstance(MandatoryDocumentInitializer.class,
                XWikiPreferencesDocumentInitializer.LOCAL_REFERENCE_STRING);
        initializer.updateDocument(initializedDoc);

        // Take a copy of the XClass produced by the initializer.
        BaseClass initializedClass = initializedDoc.getXClass().clone();

        // Delete the document so that loadPage loads only from the XAR, not from the mock store.
        this.xwiki.deleteDocument(initializedDoc, true, this.context);

        // Load the document from the XAR.
        XWikiDocument xmlDoc = loadPage(XWIKI_PREFERENCES);
        BaseClass xmlClass = xmlDoc.getXClass();

        // Compare property names first to give a clear error message about any discrepancy.
        Set<String> initializedProps = toSortedSet(initializedClass.getPropertyNames());
        Set<String> xmlProps = toSortedSet(xmlClass.getPropertyNames());

        Set<String> inInitializerOnly = new TreeSet<>(initializedProps);
        inInitializerOnly.removeAll(xmlProps);
        assertEquals(Set.of(), inInitializerOnly,
            "The following XClass properties are defined in the mandatory initializer but missing from the XAR. "
                + "Please add them to XWiki/XWikiPreferences.xml");

        Set<String> inXMLOnly = new TreeSet<>(xmlProps);
        inXMLOnly.removeAll(initializedProps);
        assertEquals(Set.of(), inXMLOnly,
            "The following XClass properties are present in the XAR but missing from the mandatory initializer. "
                + "Please add them to XWikiPreferencesDocumentInitializer");

        // Compare individual properties for any differences in their definitions.
        // Empty string values in the XAR (e.g. <customDisplay/>, <validationMessage/>) are treated as absent since
        // they are noise from historical saves and do not affect behavior.
        StringBuilder propDiffs = new StringBuilder();
        for (String propName : initializedProps) {
            PropertyClass initializedProp = (PropertyClass) initializedClass.get(propName);
            PropertyClass xmlProp = (PropertyClass) xmlClass.get(propName);
            appendNormalizedPropertyFieldDiffs(propDiffs, propName, initializedProp, xmlProp);
        }
        if (!propDiffs.isEmpty()) {
            fail("XClass property definitions differ between the mandatory initializer and the XAR. "
                + "Please synchronize XWikiPreferencesDocumentInitializer and XWiki/XWikiPreferences.xml\n"
                + "Field differences (initializer -> XAR):\n"
                + propDiffs);
        }
    }

    /**
     * Appends a human-readable field-level diff between two PropertyClass instances to the given StringBuilder,
     * using the normalized field value maps. Only fields with meaningful (non-empty) differences are reported.
     */
    private void appendNormalizedPropertyFieldDiffs(StringBuilder sb, String propName, PropertyClass initializedProp,
        PropertyClass xmlProp)
    {
        Map<String, String> initFields = toNormalizedFieldValueMap(initializedProp);
        Map<String, String> xmlFields = toNormalizedFieldValueMap(xmlProp);
        Set<String> allKeys = new TreeSet<>();
        allKeys.addAll(initFields.keySet());
        allKeys.addAll(xmlFields.keySet());
        for (String key : allKeys) {
            String initVal = initFields.getOrDefault(key, "<absent>");
            String xmlVal = xmlFields.getOrDefault(key, "<absent>");
            if (!initVal.equals(xmlVal)) {
                sb.append(String.format("  - %s.%s: [%s] -> [%s]%n", propName, key, initVal, xmlVal));
            }
        }
    }

    /**
     * Appends a human-readable field-level diff between two PropertyClass instances to the given StringBuilder.
     * Each field stored inside the PropertyClass (e.g. {@code rows}, {@code size}, {@code editor}) is compared
     * and only differing or exclusive fields are listed.
     */
    private void appendPropertyFieldDiffs(StringBuilder sb, PropertyClass before, PropertyClass after)
    {
        Map<String, String> beforeFields = toNormalizedFieldValueMap(before);
        Map<String, String> afterFields = toNormalizedFieldValueMap(after);
        Set<String> allKeys = new TreeSet<>();
        allKeys.addAll(beforeFields.keySet());
        allKeys.addAll(afterFields.keySet());
        for (String key : allKeys) {
            String beforeVal = beforeFields.getOrDefault(key, "<absent>");
            String afterVal = afterFields.getOrDefault(key, "<absent>");
            if (!beforeVal.equals(afterVal)) {
                sb.append(String.format("    - %s: [%s] -> [%s]%n", key, beforeVal, afterVal));
            }
        }
    }

    /**
     * Returns a map of field name to text value for the given PropertyClass, omitting fields whose text value is
     * empty, {@code "0"}, or {@code "none"}. Empty-string values stored in XAR (e.g. {@code <customDisplay/>})
     * carry no semantic meaning. Numeric zero ({@code "0"}) and {@code "none"} are stored defaults that are
     * semantically equivalent to absent (e.g. {@code picker=0} means the picker is off, same as unset).
     */
    private Map<String, String> toNormalizedFieldValueMap(PropertyClass propertyClass)
    {
        Map<String, String> result = new TreeMap<>();
        for (String key : propertyClass.getPropertyNames()) {
            Object field = propertyClass.safeget(key);
            String value = (field instanceof BaseProperty<?> baseProperty)
                ? baseProperty.toText() : String.valueOf(field);
            if (!value.isEmpty() && !"0".equals(value) && !"none".equals(value)) {
                result.put(key, value);
            }
        }
        return result;
    }

    private Set<String> toSortedSet(Object[] propertyNames)
    {
        return Arrays.stream(propertyNames)
            .map(Object::toString)
            .collect(Collectors.toCollection(TreeSet::new));
    }
}
