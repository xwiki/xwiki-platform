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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.ircbot.IRCBotException;
import org.xwiki.ircbot.internal.BotData;
import org.xwiki.ircbot.internal.BotListenerData;
import org.xwiki.ircbot.wiki.WikiIRCBotConstants;
import org.xwiki.ircbot.wiki.WikiIRCModel;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.EntityReferenceValueProvider;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.Utils;

import junit.framework.Assert;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultWikiIRCModel}.
 *
 * @version $Id$
 * @since 4.0M2
 */
@AllComponents
public class DefaultWikiIRCModelTest implements WikiIRCBotConstants
{
    @Rule
    public MockitoComponentMockingRule<DefaultWikiIRCModel> componentManager =
        new MockitoComponentMockingRule<DefaultWikiIRCModel>(DefaultWikiIRCModel.class,
                Arrays.asList(EntityReferenceSerializer.class, EntityReferenceValueProvider.class));

    private XWiki xwiki;

    private XWikiContext xwikiContext;

    private XWikiDocument configDoc;

    @Before
    public void setUp() throws Exception
    {
        Utils.setComponentManager(this.componentManager);

        Execution execution = this.componentManager.getInstance(Execution.class);
        ExecutionContext context = new ExecutionContext();

        this.xwiki = mock(XWiki.class);

        this.xwikiContext = new XWikiContext();
        this.xwikiContext.setDatabase("somewiki");
        this.xwikiContext.setWiki(this.xwiki);

        context.setProperty("xwikicontext", this.xwikiContext);

        final DocumentReference configDocReference = new DocumentReference("somewiki", "IRC", "IRCConfiguration");
        this.configDoc = mock(XWikiDocument.class);

        when(execution.getContext()).thenReturn(context);
        when(xwiki.getDocument(configDocReference, xwikiContext)).thenReturn(configDoc);
        when(configDoc.getDocumentReference()).thenReturn(configDocReference);
    }

    @Test
    public void loadBotDataWhenNoConfigDataInConfigDocument() throws Exception
    {
        try {
            this.componentManager.getMockedComponent().loadBotData();
            Assert.fail("Should have thrown an exception");
        } catch (IRCBotException expected) {
            Assert.assertEquals("Cannot find the IRC Configuration object in the [somewiki:IRC.IRCConfiguration] "
                + "document", expected.getMessage());
        }
    }

    @Test
    public void loadBotData() throws Exception
    {
        BaseObject botDataObject = mock(BaseObject.class);

        when(configDoc.getXObject(WIKI_BOT_CONFIGURATION_CLASS)).thenReturn(botDataObject);
        when(botDataObject.getStringValue("botname")).thenReturn("mybotname");
        when(botDataObject.getStringValue("server")).thenReturn("myserver");
        when(botDataObject.getStringValue("channel")).thenReturn("mychannel");
        when(botDataObject.getStringValue("password")).thenReturn("mypassword");
        when(botDataObject.getIntValue("inactive")).thenReturn(0);

        BotData botData = this.componentManager.getMockedComponent().loadBotData();
        Assert.assertEquals("mybotname", botData.getName());
        Assert.assertEquals("myserver", botData.getServer());
        Assert.assertEquals("mychannel", botData.getChannel());
        Assert.assertEquals("mypassword", botData.getPassword());
        Assert.assertTrue(botData.isActive());
    }

    @Test
    public void getWikiBotListenerData() throws Exception
    {
        QueryManager queryManager = this.componentManager.getInstance(QueryManager.class);
        Query query = mock(Query.class);

        List results = Collections.singletonList(new Object[]{
            "space", "name", "listenername", "listenerdescription" });

        when(queryManager.createQuery(anyString(), anyString())).thenReturn(query);
        when(query.execute()).thenReturn(results);

        List<BotListenerData> data = this.componentManager.getMockedComponent().getWikiBotListenerData();
        Assert.assertEquals(1, data.size());
        Assert.assertEquals("space.name", data.get(0).getId());
        Assert.assertEquals("listenername", data.get(0).getName());
        Assert.assertEquals("listenerdescription", data.get(0).getDescription());
    }

    /**
     * Verify that if there's no configuration document in the current wiki then we'll look into the main wiki.
     */
    @Test
    public void getConfigurationDocumentWhenLocatedInMainWiki() throws Exception
    {
        when(this.configDoc.isNew()).thenReturn(true);
        XWikiDocument mainConfigDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(new DocumentReference("xwiki", "IRC", "IRCConfiguration"), this.xwikiContext))
            .thenReturn(mainConfigDoc);

        Assert.assertSame(mainConfigDoc, this.componentManager.getMockedComponent().getConfigurationDocument());
    }
}