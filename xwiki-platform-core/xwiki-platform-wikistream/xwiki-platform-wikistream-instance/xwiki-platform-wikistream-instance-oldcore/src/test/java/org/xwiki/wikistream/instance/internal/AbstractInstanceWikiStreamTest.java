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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.input.BeanInputWikiStreamFactory;
import org.xwiki.wikistream.input.InputWikiStream;
import org.xwiki.wikistream.input.InputWikiStreamFactory;
import org.xwiki.wikistream.instance.internal.output.InstanceOutputProperties;
import org.xwiki.wikistream.internal.input.DefaultURLInputSource;
import org.xwiki.wikistream.output.BeanOutputWikiStreamFactory;
import org.xwiki.wikistream.output.OutputWikiStream;
import org.xwiki.wikistream.output.OutputWikiStreamFactory;
import org.xwiki.wikistream.type.WikiStreamType;
import org.xwiki.wikistream.wikixml.internal.input.WikiXMLInputProperties;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.test.MockitoOldcoreRule;

/**
 * Base class to validate an instance sub {@link OutputInstanceWikiStream}.
 * 
 * @version $Id$
 */
@AllComponents
public class AbstractInstanceWikiStreamTest
{
    private static final SimpleDateFormat DATE_PARSER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S z", Locale.ENGLISH);

    protected static final LocalDocumentReference USER_CLASS = new LocalDocumentReference("XWiki", "XWikiUsers");

    protected static final LocalDocumentReference GROUP_CLASS = new LocalDocumentReference("XWiki", "XWikiGroups");

    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule();

    protected BeanInputWikiStreamFactory<WikiXMLInputProperties> xmlInputWikiStreamFactory;

    protected BeanOutputWikiStreamFactory<InstanceOutputProperties> outputWikiStreamFactory;

    protected Map<DocumentReference, XWikiDocument> documents = new HashMap<DocumentReference, XWikiDocument>();

    @Before
    public void before() throws Exception
    {
        this.xmlInputWikiStreamFactory =
            this.oldcore.getMocker().getInstance(InputWikiStreamFactory.class, WikiStreamType.WIKI_XML.serialize());
        this.outputWikiStreamFactory =
            this.oldcore.getMocker().getInstance(OutputWikiStreamFactory.class,
                WikiStreamType.XWIKI_INSTANCE.serialize());

        this.oldcore.getXWikiContext().setDatabase("wiki");

        // XWiki

        Mockito.when(
            this.oldcore.getMockXWiki().getDocument(Mockito.any(DocumentReference.class),
                Mockito.any(XWikiContext.class))).then(new Answer<XWikiDocument>()
        {
            @Override
            public XWikiDocument answer(InvocationOnMock invocation) throws Throwable
            {
                DocumentReference target = (DocumentReference) invocation.getArguments()[0];

                if (target.getLocale() == null) {
                    target = new DocumentReference(target, Locale.ROOT);
                }

                XWikiDocument document = documents.get(target);

                if (document == null) {
                    document = new XWikiDocument(target);
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
                    String comment = (String) invocation.getArguments()[1];
                    boolean minorEdit = (Boolean) invocation.getArguments()[2];

                    document.setComment(StringUtils.defaultString(comment));
                    document.setMinorEdit(minorEdit);

                    if (document.isContentDirty() || document.isMetaDataDirty()) {
                        document.setDate(new Date());
                        if (document.isContentDirty()) {
                            document.setContentUpdateDate(new Date());
                            document.setContentAuthorReference(document.getAuthorReference());
                        }
                        document.incrementVersion();

                        document.setContentDirty(false);
                        document.setMetaDataDirty(false);
                    }
                    document.setNew(false);
                    document.setStore(oldcore.getMockStore());

                    XWikiDocument previousDocument = documents.get(document.getDocumentReferenceWithLocale());

                    if (previousDocument != document) {
                        for (XWikiAttachment attachment : document.getAttachmentList()) {
                            if (!attachment.isContentDirty()) {
                                attachment.setAttachment_content(previousDocument.getAttachment(
                                    attachment.getFilename()).getAttachment_content());
                            }
                        }
                    }

                    documents.put(document.getDocumentReferenceWithLocale(), document.clone());

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

                documents.remove(document.getDocumentReferenceWithLocale());

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
                    return oldcore
                        .getMockXWiki()
                        .getDocument((DocumentReference) invocation.getArguments()[0],
                            (XWikiContext) invocation.getArguments()[1]).getXClass();
                }
            });
        Mockito.when(this.oldcore.getMockXWiki().hasAttachmentRecycleBin(Mockito.any(XWikiContext.class))).thenReturn(
            true);

        // XWikiStoreInterface

        Mockito.when(
            this.oldcore.getMockStore().getTranslationList(Mockito.any(XWikiDocument.class),
                Mockito.any(XWikiContext.class))).then(new Answer<List<String>>()
        {
            @Override
            public List<String> answer(InvocationOnMock invocation) throws Throwable
            {
                XWikiDocument document = (XWikiDocument) invocation.getArguments()[0];

                List<String> translations = new ArrayList<String>();

                for (XWikiDocument storedDocument : documents.values()) {
                    Locale storedLocale = storedDocument.getLocale();
                    if (!storedLocale.equals(Locale.ROOT)
                        && storedDocument.getDocumentReference().equals(document.getDocumentReference())) {
                        translations.add(storedLocale.toString());
                    }
                }

                return translations;
            }
        });

        // Users

        Mockito.when(this.oldcore.getMockXWiki().getUserClass(Mockito.any(XWikiContext.class))).then(
            new Answer<BaseClass>()
            {
                @Override
                public BaseClass answer(InvocationOnMock invocation) throws Throwable
                {
                    XWikiContext xcontext = (XWikiContext) invocation.getArguments()[0];

                    XWikiDocument userDocument =
                        oldcore.getMockXWiki().getDocument(
                            new DocumentReference(USER_CLASS, new WikiReference(xcontext.getDatabase())), xcontext);

                    final BaseClass userClass = userDocument.getXClass();

                    if (userDocument.isNew()) {
                        userClass.addTextField("first_name", "First Name", 30);
                        userClass.addTextField("last_name", "Last Name", 30);
                        userClass.addEmailField("email", "e-Mail", 30);
                        userClass.addPasswordField("password", "Password", 10);
                        userClass.addBooleanField("active", "Active", "active");
                        userClass.addTextAreaField("comment", "Comment", 40, 5);
                        userClass.addTextField("avatar", "Avatar", 30);
                        userClass.addTextField("phone", "Phone", 30);
                        userClass.addTextAreaField("address", "Address", 40, 3);

                        oldcore.getMockXWiki().saveDocument(userDocument, xcontext);
                    }

                    return userClass;
                }
            });
        Mockito.when(this.oldcore.getMockXWiki().getGroupClass(Mockito.any(XWikiContext.class))).then(
            new Answer<BaseClass>()
            {
                @Override
                public BaseClass answer(InvocationOnMock invocation) throws Throwable
                {
                    XWikiContext xcontext = (XWikiContext) invocation.getArguments()[0];

                    XWikiDocument groupDocument =
                        oldcore.getMockXWiki().getDocument(
                            new DocumentReference(GROUP_CLASS, new WikiReference(xcontext.getDatabase())), xcontext);

                    final BaseClass groupClass = groupDocument.getXClass();

                    if (groupDocument.isNew()) {
                        groupClass.addTextField("member", "Member", 30);

                        oldcore.getMockXWiki().saveDocument(groupDocument, xcontext);
                    }

                    return groupClass;
                }
            });
    }

    protected void importFromXML(String resource) throws WikiStreamException
    {
        importFromXML(resource, new InstanceOutputProperties());
    }

    protected void importFromXML(String resource, InstanceOutputProperties instanceProperties)
        throws WikiStreamException
    {
        OutputWikiStream outputWikiStream = this.outputWikiStreamFactory.createOutputWikiStream(instanceProperties);

        URL url = getClass().getResource("/" + resource + ".xml");

        WikiXMLInputProperties properties = new WikiXMLInputProperties();
        properties.setSource(new DefaultURLInputSource(url));

        InputWikiStream inputWikiStream = this.xmlInputWikiStreamFactory.createInputWikiStream(properties);

        inputWikiStream.read(outputWikiStream.getFilter());
    }

    protected Date toDate(String date) throws ParseException
    {
        return DATE_PARSER.parse(date);
    }
}
