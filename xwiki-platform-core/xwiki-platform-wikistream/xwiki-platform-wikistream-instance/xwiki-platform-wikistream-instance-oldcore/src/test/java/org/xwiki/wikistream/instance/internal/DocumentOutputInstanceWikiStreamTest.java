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
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentManagerRule;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.input.BeanInputWikiStreamFactory;
import org.xwiki.wikistream.input.InputWikiStream;
import org.xwiki.wikistream.input.InputWikiStreamFactory;
import org.xwiki.wikistream.instance.internal.output.DocumentOutputInstanceWikiStream;
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
import com.xpn.xwiki.objects.classes.BaseClass;
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
    }

    private void importFromXML(String resource) throws WikiStreamException
    {
        InstanceOutputProperties outputProperties = new InstanceOutputProperties();

        OutputWikiStream outputWikiStream = this.outputWikiStreamFactory.creaOutputWikiStream(outputProperties);

        URL url = getClass().getResource("/" + resource + ".xml");

        WikiXMLInputProperties properties = new WikiXMLInputProperties();
        properties.setSource(new DefaultURLInputSource(url));

        InputWikiStream inputWikiStream = this.inputWikiStreamFactory.createInputWikiStream(properties);

        inputWikiStream.read(outputWikiStream.getFilter());
    }

    // Tests

    @Test
    public void testDocument() throws WikiStreamException
    {
        importFromXML("document1");
    }
}
