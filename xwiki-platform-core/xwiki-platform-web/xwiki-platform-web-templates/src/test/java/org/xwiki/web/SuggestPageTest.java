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
package org.xwiki.web;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xwiki.mail.script.GeneralMailScriptService;
import org.xwiki.mail.script.MailScriptService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.internal.ScriptQuery;
import org.xwiki.query.script.QueryManagerScriptService;
import org.xwiki.script.service.ScriptService;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.page.PageTest;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the {@code suggest.vm} template.
 *
 * @version $Id$
 * @since 14.2
 * @since 13.10.4
 */
class SuggestPageTest extends PageTest
{
    private static final String EMAIL_FIELD = "email";

    private static final String PASSWORD_FIELD = "password";

    private static final String VIEW_ACTION = "view";

    private static final String RESULTS_TAG = "rs";

    private TemplateManager templateManager;

    private ScriptQuery query;

    @BeforeEach
    void setUp() throws Exception
    {
        this.templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);

        // suggest.vm needs programming rights and normally has them as a Velocity template.
        when(this.xwiki.getRightService().hasProgrammingRights(any())).thenReturn(true);

        // Set the query manager to basically ignore any parameters set, the individual tests set the results
        // independently of them.
        QueryManagerScriptService qmss = mock(QueryManagerScriptService.class);
        this.oldcore.getMocker().registerComponent(ScriptService.class, "query", qmss);
        this.query = mock(ScriptQuery.class);
        when(qmss.hql(anyString())).thenReturn(this.query);
        when(this.query.setLimit(anyInt())).thenReturn(this.query);
        when(this.query.bindValues(anyMap())).thenReturn(this.query);

        // Create a document with some properties to test. Most of them are inspired by XWiki.XWikiUsers, thus the
        // naming.
        XWikiDocument classDocument =
            new XWikiDocument(new DocumentReference("xwiki", "XWiki", "XWikiUsers"));
        BaseClass xclass = classDocument.getXClass();
        xclass.addTextField("first_name", "First Name", 30);
        xclass.addEmailField(EMAIL_FIELD, "e-Mail", 30);
        xclass.addPasswordField(PASSWORD_FIELD, "Password", 10);
        xclass.addStaticListField("imtype", "IM Type", 30, true, "AIM|Yahoo|Jabber|MSN|Skype|ICQ");
        xclass.addStaticListField("tags", "Tags", 30, true, true, "", "checkbox", ",");
        this.xwiki.saveDocument(classDocument, this.context);

        // Set this document as default class name.
        this.request.put("classname", "XWiki.XWikiUsers");
    }

    @Test
    void hidesPassword() throws Exception
    {
        NodeList resultElements = getResult(PASSWORD_FIELD).getElementsByTagName(RESULTS_TAG);

        verify(this.query, never()).execute();
        assertEquals(0, resultElements.getLength());
    }

    @ParameterizedTest
    @CsvSource({ "first_name, 8", "imtype, 5", "tags, 3", "email, 8" })
    void accessCheck(String fieldName, String resultsType) throws Exception
    {
        String documentU1Reference = "XWiki.U1";
        String documentU2Reference = "XWiki.U2";

        List<Object> values = Arrays.asList("U1", "U2");
        when(this.query.execute()).thenReturn(values)
            .thenReturn(Collections.singletonList(documentU1Reference))
            .thenReturn(Collections.singletonList(documentU2Reference));

        when(this.xwiki.getRightService().hasAccessLevel(eq(VIEW_ACTION), any(), eq(documentU1Reference), any()))
            .thenReturn(true);
        when(this.xwiki.getRightService().hasAccessLevel(eq(VIEW_ACTION), any(), eq(documentU2Reference), any()))
            .thenReturn(false);

        Document result = getResult(fieldName);
        assertEquals(resultsType, result.getDocumentElement().getAttribute("type"));
        NodeList resultElements = result.getElementsByTagName(RESULTS_TAG);

        assertEquals(1, resultElements.getLength());
        assertEquals(values.get(0), resultElements.item(0).getTextContent());
    }

    /**
     * Check that the email is displayed when the script service is available but returns false (vs. does not exist
     * as in the tests where it is not explicitly configured).
     *
     * @throws Exception when the test fails
     */
    @Test
    void emailVisibleWhenNotObfuscated() throws Exception
    {
        mockEmailObfuscation(false);

        String documentReference = "XWiki.Admin";
        String email = "user@example.com";
        when(this.query.execute()).thenReturn(Collections.singletonList(email))
            .thenReturn(Collections.singletonList(documentReference));
        when(this.xwiki.getRightService().hasAccessLevel(eq(VIEW_ACTION), any(), eq(documentReference), any()))
            .thenReturn(true);

        NodeList resultElements = getResult(EMAIL_FIELD).getElementsByTagName(RESULTS_TAG);
        assertEquals(1, resultElements.getLength());
        assertEquals(email, resultElements.item(0).getTextContent());
    }

    @Test
    void emailHiddenWhenObfuscated() throws Exception
    {
        mockEmailObfuscation(true);

        NodeList resultElements = getResult(EMAIL_FIELD).getElementsByTagName(RESULTS_TAG);
        verify(this.query, never()).execute();
        assertEquals(0, resultElements.getLength());
    }

    private void mockEmailObfuscation(boolean shallObfuscate) throws Exception
    {
        GeneralMailScriptService generalMailScriptService = mock(GeneralMailScriptService.class);
        MailScriptService mailScriptService = mock(MailScriptService.class);
        when(mailScriptService.get("general")).thenReturn(generalMailScriptService);
        this.oldcore.getMocker().registerComponent(ScriptService.class, "mail", mailScriptService);
        when(generalMailScriptService.shouldObfuscate()).thenReturn(shallObfuscate);
    }

    private Document getResult(String fieldName) throws Exception
    {
        this.request.put("fieldname", fieldName);

        String xmlResult = this.templateManager.render("suggest.vm").trim();

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        return documentBuilder.parse(new ByteArrayInputStream(xmlResult.getBytes(StandardCharsets.UTF_8)));
    }
}
