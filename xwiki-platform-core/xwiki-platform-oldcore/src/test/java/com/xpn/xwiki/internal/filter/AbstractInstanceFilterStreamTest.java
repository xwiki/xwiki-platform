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
package com.xpn.xwiki.internal.filter;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.filterxml.input.FilterXMLInputProperties;
import org.xwiki.filter.input.BeanInputFilterStreamFactory;
import org.xwiki.filter.input.DefaultURLInputSource;
import org.xwiki.filter.input.InputFilterStream;
import org.xwiki.filter.input.InputFilterStreamFactory;
import org.xwiki.filter.instance.output.InstanceOutputProperties;
import org.xwiki.filter.output.BeanOutputFilterStreamFactory;
import org.xwiki.filter.output.OutputFilterStream;
import org.xwiki.filter.output.OutputFilterStreamFactory;
import org.xwiki.filter.type.FilterStreamType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static com.xpn.xwiki.test.mockito.OldcoreMatchers.anyXWikiContext;
import static org.mockito.Mockito.doReturn;

/**
 * Base class to validate an instance sub {@link OutputInstanceFilterStream}.
 * 
 * @version $Id$
 */
@OldcoreTest
@AllComponents
public abstract class AbstractInstanceFilterStreamTest
{
    private static final SimpleDateFormat DATE_PARSER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S z", Locale.ENGLISH);

    protected UserReferenceResolver<DocumentReference> userReferenceResolver;

    @InjectComponentManager
    protected MockitoComponentManager componentManager;

    @InjectMockitoOldcore
    protected MockitoOldcore oldcore;

    protected BeanInputFilterStreamFactory<FilterXMLInputProperties> xmlInputFilterStreamFactory;

    protected BeanOutputFilterStreamFactory<InstanceOutputProperties> outputFilterStreamFactory;

    @BeforeEach
    public void beforeEach() throws Exception
    {
        this.xmlInputFilterStreamFactory = this.oldcore.getMocker().getInstance(InputFilterStreamFactory.class,
            FilterStreamType.FILTER_XML.serialize());
        this.outputFilterStreamFactory = this.oldcore.getMocker().getInstance(OutputFilterStreamFactory.class,
            FilterStreamType.XWIKI_INSTANCE.serialize());

        this.oldcore.getXWikiContext().setWikiId("wiki");

        // XWiki

        doReturn(true).when(this.oldcore.getSpyXWiki()).hasAttachmentRecycleBin(anyXWikiContext());

        // Users

        this.userReferenceResolver = this.componentManager.getInstance(new DefaultParameterizedType(null, UserReferenceResolver.class, DocumentReference.class), "document");
        //DocumentReference contextUser = new DocumentReference("wiki", "XWiki", "contextuser");
        //UserReference contextUserReference = mockUserReference(contextUser);
    }

    protected void importFromXML(String resource) throws FilterException
    {
        importFromXML(resource, null);
    }

    protected void importFromXML(String resource, InstanceOutputProperties instanceProperties) throws FilterException
    {
        if (instanceProperties == null) {
            instanceProperties = new InstanceOutputProperties();
            instanceProperties.setVerbose(false);
        }

        OutputFilterStream outputFilterStream =
            this.outputFilterStreamFactory.createOutputFilterStream(instanceProperties);

        URL url = getClass().getResource("/filter/" + resource + ".xml");

        FilterXMLInputProperties properties = new FilterXMLInputProperties();
        properties.setSource(new DefaultURLInputSource(url));

        InputFilterStream inputFilterStream = this.xmlInputFilterStreamFactory.createInputFilterStream(properties);

        inputFilterStream.read(outputFilterStream.getFilter());

        try {
            inputFilterStream.close();
        } catch (IOException e) {
            throw new FilterException("Failed to close input wiki stream", e);
        }
        try {
            outputFilterStream.close();
        } catch (IOException e) {
            throw new FilterException("Failed to close output wiki stream", e);
        }
    }

    protected Date toDate(String date) throws ParseException
    {
        return DATE_PARSER.parse(date);
    }

    protected UserReference mockUserReference(DocumentReference documentReference)
    {
        return this.userReferenceResolver.resolve(documentReference);
    }
}
