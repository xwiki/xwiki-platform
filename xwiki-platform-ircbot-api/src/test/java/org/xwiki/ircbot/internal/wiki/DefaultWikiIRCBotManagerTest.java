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
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.ircbot.IRCBotListener;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;

import junit.framework.Assert;

/**
 * Unit tests for {@link DefaultWikiIRCBotManager}.
 *
 * @version $Id$
 * @since 4.0M1
 */
public class DefaultWikiIRCBotManagerTest extends AbstractMockingComponentTestCase
{
    @MockingRequirement(exceptions = {EntityReferenceSerializer.class})
    DefaultWikiIRCBotManager manager;

    @Test
    public void getBotListenerData() throws Exception
    {
        final QueryManager queryManager = getComponentManager().lookup(QueryManager.class);
        final Query query = getMockery().mock(Query.class);
        final List<Object[]> results =
            Collections.singletonList(new Object[]{"docspace", "docname", "name", "description"});
        final ComponentManager componentManager = getComponentManager().lookup(ComponentManager.class, "wiki");
        final IRCBotListener botListener = getMockery().mock(IRCBotListener.class, "test");

        getMockery().checking(new Expectations()
        {{
            oneOf(queryManager).createQuery("select distinct doc.space, doc.name, listener.name, listener.description "
                + "from Document doc, doc.object(IRC.IRCBotListenerClass) as listener", "xwql");
                will(returnValue(query));
            oneOf(query).execute();
                will(returnValue(results));
            oneOf(componentManager).lookupMap(IRCBotListener.class);
                will(returnValue(Collections.singletonMap("docspace.docname", botListener)));
            oneOf(botListener).getName();
                will(returnValue("name"));
            oneOf(botListener).getDescription();
                will(returnValue("description"));
        }});

        List<BotListenerData> data = this.manager.getBotListenerData();
        Assert.assertEquals(1, data.size());
        Assert.assertEquals("docspace.docname", data.get(0).getId());
        Assert.assertEquals("name", data.get(0).getName());
        Assert.assertEquals("description", data.get(0).getDescription());
    }
}
