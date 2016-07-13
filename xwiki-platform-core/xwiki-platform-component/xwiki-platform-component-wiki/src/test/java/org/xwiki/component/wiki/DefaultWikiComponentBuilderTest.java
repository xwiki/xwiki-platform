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
package org.xwiki.component.wiki;

import java.util.List;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.MapUtils;
import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.wiki.internal.DefaultWikiComponentBuilder;
import org.xwiki.component.wiki.internal.WikiComponentConstants;
import org.xwiki.component.wiki.internal.bridge.WikiComponentBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.jmock.AbstractMockingComponentTestCase;
import org.xwiki.test.jmock.annotation.MockingRequirement;

@MockingRequirement(DefaultWikiComponentBuilder.class)
public class DefaultWikiComponentBuilderTest extends AbstractMockingComponentTestCase implements WikiComponentConstants
{
    private static final DocumentReference DOC_REFERENCE = new DocumentReference("xwiki", "XWiki", "MyComponent");

    private static final DocumentReference AUTHOR_REFERENCE = new DocumentReference("xwiki", "XWiki", "Admin");

    private WikiComponentBuilder builder;

    private WikiComponentBridge bridge;

    private ContextualAuthorizationManager authorization;

    @Before
    public void configure() throws Exception
    {
        this.builder = getComponentManager().getInstance(WikiComponentBuilder.class);
        this.bridge = getComponentManager().getInstance(WikiComponentBridge.class);
        this.authorization = getComponentManager().getInstance(ContextualAuthorizationManager.class);
    }

    @Test
    public void buildComponentsWithoutProgrammingRights() throws Exception
    {
        getMockery().checking(new Expectations()
        {
            {
                oneOf(authorization).hasAccess(Right.PROGRAM, DOC_REFERENCE);
                will(returnValue(false));
            }
        });

        try {
            this.builder.buildComponents(DOC_REFERENCE);
            Assert.fail("Should have thrown an exception");
        } catch (WikiComponentException expected) {
            Assert.assertEquals("Registering wiki components requires programming rights", expected.getMessage());
        }
    }

    @Test
    public void buildComponents() throws Exception
    {
        getMockery().checking(new Expectations()
        {
            {
                oneOf(bridge).getAuthorReference(DOC_REFERENCE);
                will(returnValue(AUTHOR_REFERENCE));
                oneOf(bridge).getRoleType(DOC_REFERENCE);
                will(returnValue(TestRole.class));
                oneOf(bridge).getRoleHint(DOC_REFERENCE);
                will(returnValue("test"));
                oneOf(bridge).getScope(DOC_REFERENCE);
                will(returnValue(WikiComponentScope.WIKI));
                oneOf(bridge).getHandledMethods(DOC_REFERENCE);
                will(returnValue(MapUtils.EMPTY_MAP));
                oneOf(bridge).getDependencies(DOC_REFERENCE);
                will(returnValue(MapUtils.EMPTY_MAP));
                oneOf(bridge).getDeclaredInterfaces(DOC_REFERENCE);
                will(returnValue(ListUtils.EMPTY_LIST));
                oneOf(bridge).getSyntax(DOC_REFERENCE);
                will(returnValue(Syntax.XWIKI_2_1));

                oneOf(authorization).hasAccess(Right.PROGRAM, DOC_REFERENCE);
                will(returnValue(true));
            }
        });

        List<WikiComponent> components = this.builder.buildComponents(DOC_REFERENCE);

        Assert.assertEquals(1, components.size());
    }
}
