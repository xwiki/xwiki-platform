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
package org.xwiki.wikistream.instance.internal;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxType;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentManagerRule;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.input.BeanInputWikiStreamFactory;
import org.xwiki.wikistream.input.InputWikiStream;
import org.xwiki.wikistream.input.InputWikiStreamFactory;
import org.xwiki.wikistream.instance.internal.output.DocumentOutputInstanceWikiStream;
import org.xwiki.wikistream.instance.internal.output.DocumentOutputProperties;
import org.xwiki.wikistream.instance.internal.output.InstanceOutputProperties;
import org.xwiki.wikistream.internal.input.DefaultURLInputSource;
import org.xwiki.wikistream.output.BeanOutputWikiStreamFactory;
import org.xwiki.wikistream.output.OutputWikiStream;
import org.xwiki.wikistream.output.OutputWikiStreamFactory;
import org.xwiki.wikistream.type.WikiStreamType;
import org.xwiki.wikistream.wikixml.internal.input.WikiXMLInputProperties;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.test.MockitoOldcoreRule;

/**
 * Validate {@link DocumentOutputInstanceWikiStream}.
 * 
 * @version $Id$
 */
@AllComponents
public class DocumentOutputInstanceWikiStreamTest
{
    private MockitoComponentManagerRule mocker = new MockitoComponentManagerRule();

    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule(this.mocker);

    private BeanInputWikiStreamFactory<WikiXMLInputProperties> inputWikiStreamFactory;

    private BeanOutputWikiStreamFactory<InstanceOutputProperties> outputWikiStreamFactory;

    private Map<DocumentReference, Map<String, XWikiDocument>> documents =
        new HashMap<DocumentReference, Map<String, XWikiDocument>>();

    private Map<String, BaseClass> classes = new HashMap<String, BaseClass>();

    @Before
    public void before() throws ComponentLookupException, XWikiException
    {
        this.inputWikiStreamFactory =
            this.mocker.getInstance(InputWikiStreamFactory.class, WikiStreamType.WIKI_XML.serialize());
        this.outputWikiStreamFactory =
            this.mocker.getInstance(OutputWikiStreamFactory.class, WikiStreamType.XWIKI_INSTANCE.serialize());

        Mockito.when(
            this.oldcore.getMockXWiki().getDocument(Mockito.any(DocumentReference.class),
                Mockito.any(XWikiContext.class))).then(new Answer<XWikiDocument>()
        {
            @Override
            public XWikiDocument answer(InvocationOnMock invocation) throws Throwable
            {
                DocumentReference target = (DocumentReference) invocation.getArguments()[0];

                Map<String, XWikiDocument> documentLanguages = documents.get(target);

                if (documentLanguages == null) {
                    documentLanguages = new HashMap<String, XWikiDocument>();
                    documents.put(target, documentLanguages);
                }

                XWikiDocument document = documentLanguages.get("");

                if (document == null) {
                    document = new XWikiDocument(target);
                }

                return document;
            }
        });
        Mockito
            .when(
                this.oldcore.getMockStore().loadXWikiDoc(Mockito.any(XWikiDocument.class),
                    Mockito.any(XWikiContext.class))).then(new Answer<XWikiDocument>()
            {
                @Override
                public XWikiDocument answer(InvocationOnMock invocation) throws Throwable
                {
                    XWikiDocument providedDocument = (XWikiDocument) invocation.getArguments()[0];
                    Map<String, XWikiDocument> documentLanguages =
                        documents.get(providedDocument.getDocumentReference());

                    if (documentLanguages == null) {
                        documentLanguages = new HashMap<String, XWikiDocument>();
                        documents.put(providedDocument.getDocumentReference(), documentLanguages);
                    }

                    XWikiDocument document = documentLanguages.get(providedDocument.getLanguage());

                    if (document == null) {
                        document = new XWikiDocument(providedDocument.getDocumentReference());
                        document.setLanguage(providedDocument.getLanguage());
                        document.setDefaultLanguage(providedDocument.getDefaultLanguage());
                        document.setTranslation(providedDocument.getTranslation());
                    }

                    return document;
                }
            });
        Mockito
            .doAnswer(new Answer()
            {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable
                {
                    XWikiDocument document = (XWikiDocument) invocation.getArguments()[0];
                    boolean minorEdit = (Boolean) invocation.getArguments()[2];

                    document.setMinorEdit(minorEdit);
                    document.incrementVersion();
                    document.setNew(false);

                    Map<String, XWikiDocument> documentLanguages = documents.get(document.getDocumentReference());

                    XWikiDocument previousDocument;
                    if (documentLanguages == null) {
                        documentLanguages = new HashMap<String, XWikiDocument>();
                        documents.put(document.getDocumentReference(), documentLanguages);
                        previousDocument = null;
                    } else {
                        previousDocument = documentLanguages.get(document.getLanguage());
                    }

                    for (XWikiAttachment attachment : document.getAttachmentList()) {
                        if (!attachment.isContentDirty()) {
                            attachment.setAttachment_content(previousDocument.getAttachment(attachment.getFilename())
                                .getAttachment_content());
                        }
                    }

                    documentLanguages.put(document.getLanguage(), document.clone());

                    return null;
                }
            })
            .when(this.oldcore.getMockXWiki())
            .saveDocument(Mockito.any(XWikiDocument.class), Mockito.any(String.class), Mockito.anyBoolean(),
                Mockito.any(XWikiContext.class));
        Mockito
            .doAnswer(new Answer()
            {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable
                {
                    oldcore.getMockXWiki().saveDocument((XWikiDocument) invocation.getArguments()[0],
                        (String) invocation.getArguments()[1], false, (XWikiContext) invocation.getArguments()[2]);

                    return null;
                }
            }).when(this.oldcore.getMockXWiki())
            .saveDocument(Mockito.any(XWikiDocument.class), Mockito.any(String.class), Mockito.any(XWikiContext.class));
        Mockito.doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                XWikiDocument document = (XWikiDocument) invocation.getArguments()[0];

                Map<String, XWikiDocument> documentLanguages = documents.get(document.getDocumentReference());

                if (documentLanguages != null) {
                    documentLanguages.remove(document.getLanguage());
                }

                return null;
            }
        }).when(this.oldcore.getMockXWiki())
            .deleteDocument(Mockito.any(XWikiDocument.class), Mockito.any(XWikiContext.class));
        Mockito.when(
            this.oldcore.getMockXWiki()
                .getXClass(Mockito.any(DocumentReference.class), Mockito.any(XWikiContext.class))).then(
            new Answer<BaseClass>()
            {
                @Override
                public BaseClass answer(InvocationOnMock invocation) throws Throwable
                {
                    DocumentReference documentReference = (DocumentReference) invocation.getArguments()[0];

                    return classes.get(documentReference.getName());
                }
            });

        Mockito.when(this.oldcore.getMockXWiki().hasAttachmentRecycleBin(Mockito.any(XWikiContext.class))).thenReturn(
            true);

        this.oldcore.getXWikiContext().setDatabase("wiki");
    }

    private void importFromXML(String resource) throws WikiStreamException
    {
        DocumentOutputProperties outputProperties = new DocumentOutputProperties();

        outputProperties.setPreserveVersion(true);
        
        OutputWikiStream outputWikiStream = this.outputWikiStreamFactory.creaOutputWikiStream(outputProperties);

        URL url = getClass().getResource("/" + resource + ".xml");

        WikiXMLInputProperties properties = new WikiXMLInputProperties();
        properties.setSource(new DefaultURLInputSource(url));

        InputWikiStream inputWikiStream = this.inputWikiStreamFactory.createInputWikiStream(properties);

        inputWikiStream.read(outputWikiStream.getFilter());
    }

    // Tests

    @Test
    public void testDocumentFull() throws WikiStreamException, XWikiException
    {
        importFromXML("document1");

        XWikiDocument document =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki", "space", "page"),
                this.oldcore.getXWikiContext());

        Assert.assertFalse(document.isNew());

        Assert.assertEquals(Locale.ENGLISH, document.getDefaultLocale());
        Assert.assertEquals(new DocumentReference("wiki", "space", "parent"), document.getParentReference());
        Assert.assertEquals("customclass", document.getCustomClass());
        Assert.assertEquals("title", document.getTitle());
        Assert.assertEquals("defaultTemplate", document.getDefaultTemplate());
        Assert.assertEquals("validationScript", document.getValidationScript());
        Assert.assertEquals(new Syntax(new SyntaxType("syntax", "syntax"), "1.0"), document.getSyntax());
        Assert.assertEquals(true, document.isHidden());
        Assert.assertEquals("content", document.getContent());

        /*Assert.assertEquals(new DocumentReference("wiki", "XWiki", "creator"), document.getCreatorReference());
        Assert.assertEquals(Locale.ROOT, document.getCreationDate());
        Assert.assertEquals(Locale.ROOT, document.getAuthorReference());
        Assert.assertEquals(Locale.ROOT, document.getDate());
        Assert.assertEquals(Locale.ROOT, document.getContentUpdateDate());
        Assert.assertEquals(new DocumentReference("wiki", "space", "contentAuthor"), document.getContentAuthorReference());
        Assert.assertEquals(Locale.ROOT, document.isMinorEdit());
        Assert.assertEquals(Locale.ROOT, document.getComment());*/

        // Attachment

        Assert.assertEquals(1, document.getAttachmentList().size());
        XWikiAttachment attachment = document.getAttachment("attachment.txt");
        Assert.assertEquals("attachment.txt", attachment.getFilename());
        Assert.assertEquals(10, attachment.getFilesize());
        Assert.assertTrue(Arrays.equals(new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
            attachment.getContent(this.oldcore.getXWikiContext())));

        /*Assert.assertEquals(Locale.ROOT, attachment.getAuthor());
        Assert.assertEquals(Locale.ROOT, attachment.getDate());
        Assert.assertEquals(Locale.ROOT, attachment.getVersion());
        Assert.assertEquals(Locale.ROOT, attachment.getComment());*/

        // XClass

        BaseClass xclass = document.getXClass();
        Assert.assertEquals(1, xclass.getFieldList().size());
        Assert.assertEquals("customClass", xclass.getCustomClass());
        Assert.assertEquals("customMapping", xclass.getCustomMapping());
        Assert.assertEquals("defaultViewSheet", xclass.getDefaultViewSheet());
        Assert.assertEquals("defaultEditSheet", xclass.getDefaultEditSheet());
        Assert.assertEquals("defaultWeb", xclass.getDefaultWeb());
        Assert.assertEquals("nameField", xclass.getNameField());
        Assert.assertEquals("validationScript", xclass.getValidationScript());

        NumberClass numberFiled = (NumberClass) xclass.getField("prop1");
        Assert.assertEquals("prop1", numberFiled.getName());
        Assert.assertEquals(false, numberFiled.isDisabled());
        Assert.assertEquals(1, numberFiled.getNumber());
        Assert.assertEquals("long", numberFiled.getNumberType());
        Assert.assertEquals("Prop1", numberFiled.getPrettyName());
        Assert.assertEquals(30, numberFiled.getSize());
        Assert.assertEquals(false, numberFiled.isUnmodifiable());

        // Objects
        
        Map<DocumentReference, List<BaseObject>> objects = document.getXObjects();
        Assert.assertEquals(2, objects.size());
        
        // Object 1

        List<BaseObject> documentObjects = objects.get(new DocumentReference("wiki", "space", "page"));
        Assert.assertEquals(1, documentObjects.size());
        BaseObject documentObject = documentObjects.get(0);
        Assert.assertEquals(0, documentObject.getNumber());
        Assert.assertEquals(new DocumentReference("wiki", "space", "page"), documentObject.getXClassReference());
        Assert.assertEquals("e2167721-2a64-430c-9520-bac1c0ee68cb", documentObject.getGuid());

        // Object 2

        List<BaseObject> otherObjects = objects.get(new DocumentReference("wiki", "space", "otherclass"));
        Assert.assertEquals(1, otherObjects.size());
        BaseObject otherObject = otherObjects.get(0);
        Assert.assertEquals(0, otherObject.getNumber());
        Assert.assertEquals(new DocumentReference("wiki", "space", "otherclass"), otherObject.getXClassReference());
        Assert.assertEquals("8eaeac52-e2f2-47b2-87e1-bc6909597b39", otherObject.getGuid());
    }
}
