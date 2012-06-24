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
package org.xwiki.ircbot.internal.wiki;

import java.util.Collections;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.ircbot.IRCBotException;
import org.xwiki.ircbot.internal.BotData;
import org.xwiki.ircbot.internal.BotListenerData;
import org.xwiki.ircbot.wiki.WikiIRCBotConstants;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.Utils;

import junit.framework.Assert;

/**
 * Unit tests for {@link DefaultWikiIRCModel}.
 *
 * @version $Id$
 * @since 4.0M2
 */
public class DefaultWikiIRCModelTest extends AbstractMockingComponentTestCase implements WikiIRCBotConstants
{
    @MockingRequirement(exceptions = {EntityReferenceSerializer.class})
    DefaultWikiIRCModel model;

    private XWiki xwiki;
    private XWikiContext xwikiContext;
    private XWikiDocument configDoc;

    @Override
    public void setUp() throws Exception
    {
        getMockery().setImposteriser(ClassImposteriser.INSTANCE);
        super.setUp();

        Utils.setComponentManager(getComponentManager());

        final Execution execution = getComponentManager().getInstance(Execution.class);
        final ExecutionContext context = new ExecutionContext();

        this.xwiki = getMockery().mock(XWiki.class);

        this.xwikiContext = new XWikiContext();
        this.xwikiContext.setDatabase("xwiki");
        this.xwikiContext.setWiki(this.xwiki);

        context.setProperty("xwikicontext", this.xwikiContext);

        final DocumentReference configDocReference = new DocumentReference("xwiki", "IRC", "IRCConfiguration");
        this.configDoc = getMockery().mock(XWikiDocument.class);

        getMockery().checking(new Expectations()
        {
            {
                allowing(execution).getContext();
                will(returnValue(context));
                allowing(xwiki).getDocument(configDocReference, xwikiContext);
                will(returnValue(configDoc));
                allowing(configDoc).getDocumentReference();
                will(returnValue(configDocReference));
            }
        });
    }

    @Test
    public void loadBotDataWhenNoConfigDataInConfigDocument() throws Exception
    {
        getMockery().checking(new Expectations()
        {
            {
                oneOf(configDoc).getXObject(WIKI_BOT_CONFIGURATION_CLASS);
                will(returnValue(null));
            }
        });

        try {
            this.model.loadBotData();
            Assert.fail("Should have thrown an exception");
        } catch (IRCBotException expected) {
            Assert.assertEquals("Cannot find the IRC Configuration object in the [xwiki:IRC.IRCConfiguration] "
                + "document", expected.getMessage());
        }
    }

    @Test
    public void loadBotData() throws Exception
    {
        final BaseObject botDataObject = getMockery().mock(BaseObject.class);

        getMockery().checking(new Expectations()
        {
            {
                oneOf(configDoc).getXObject(WIKI_BOT_CONFIGURATION_CLASS);
                will(returnValue(botDataObject));
                oneOf(botDataObject).getStringValue("botname");
                will(returnValue("mybotname"));
                oneOf(botDataObject).getStringValue("server");
                will(returnValue("myserver"));
                oneOf(botDataObject).getStringValue("channel");
                will(returnValue("mychannel"));
                oneOf(botDataObject).getStringValue("password");
                will(returnValue("mypassword"));
                oneOf(botDataObject).getIntValue("inactive");
                will(returnValue(0));
            }
        });

        BotData botData = this.model.loadBotData();
        Assert.assertEquals("mybotname", botData.getName());
        Assert.assertEquals("myserver", botData.getServer());
        Assert.assertEquals("mychannel", botData.getChannel());
        Assert.assertEquals("mypassword", botData.getPassword());
        Assert.assertTrue(botData.isActive());
    }

    @Test
    public void getWikiBotListenerData() throws Exception
    {
        final QueryManager queryManager = getComponentManager().getInstance(QueryManager.class);
        final Query query = getMockery().mock(Query.class);

        final List<Object[]> results = Collections.singletonList(new Object[] {
            "space", "name", "listenername", "listenerdescription"});

        getMockery().checking(new Expectations()
        {
            {
                oneOf(queryManager).createQuery(with(any(String.class)), with(any(String.class)));
                will(returnValue(query));
                oneOf(query).execute();
                will(returnValue(results));
            }
        });

        List<BotListenerData> data = this.model.getWikiBotListenerData();
        Assert.assertEquals(1, data.size());
        Assert.assertEquals("space.name", data.get(0).getId());
        Assert.assertEquals("listenername", data.get(0).getName());
        Assert.assertEquals("listenerdescription", data.get(0).getDescription());
    }
}