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
package org.xwiki.security.authorization.internal;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.DefaultSecurityReferenceFactory;
import org.xwiki.security.SecurityReference;
import org.xwiki.security.SecurityReferenceFactory;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.RightSet;
import org.xwiki.security.authorization.RuleState;
import org.xwiki.security.authorization.SecurityRuleEntry;
import org.xwiki.security.internal.DefaultXWikiBridge;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultSecurityEntryReader}.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList({
    DefaultSecurityReferenceFactory.class,
    DefaultXWikiBridge.class
})
@ReferenceComponentList
class DefaultSecurityEntryReaderTest
{
    private static final WikiReference SUB_WIKI = new WikiReference("sub");

    @InjectMockComponents
    private DefaultSecurityEntryReader defaultSecurityEntryReader;

    @Inject
    private SecurityReferenceFactory securityReferenceFactory;

    @MockComponent
    private XWikiContext context;

    private XWiki wiki;

    @BeforeComponent
    void setup(MockitoComponentManager componentManager) throws Exception
    {
        componentManager.registerComponent(ComponentManager.class, "context", componentManager);
    }

    @BeforeEach
    void setup()
    {
        this.wiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(this.wiki);
    }

    @Test
    void readUnexistingWikiReference() throws AuthorizationException, XWikiException
    {
        when(this.context.getWikiReference()).thenReturn(new WikiReference("xwiki"));
        when(this.context.getMainXWiki()).thenReturn("xwiki");
        SecurityReference securityReference = this.securityReferenceFactory.newEntityReference(new WikiReference("foo"));
        when(this.wiki.getWikiOwner("foo", this.context)).thenThrow(
            new XWikiException(XWikiException.MODULE_XWIKI, XWikiException.ERROR_XWIKI_DOES_NOT_EXIST,
                "Does not exist"));
        SecurityRuleEntry ruleEntry = this.defaultSecurityEntryReader.read(securityReference);

        assertTrue(ruleEntry.getRules().isEmpty());
        verify(this.wiki).getWikiOwner("foo", this.context);
    }

    @Test
    void ownerRuleOnlyOnWiki() throws Exception
    {
        when(this.context.getWikiReference()).thenReturn(new WikiReference("xwiki"));
        when(this.context.getMainXWiki()).thenReturn("xwiki");
        when(this.context.isMainWiki("xwiki")).thenReturn(true);
        when(this.wiki.getWikiOwner("sub", this.context)).thenReturn("XWiki.SubOwner");
        when(this.wiki.getWikiOwner("xwiki", this.context)).thenReturn("XWiki.MainOwner");

        SecurityRuleEntry subWikiEntry =
            this.defaultSecurityEntryReader.read(this.securityReferenceFactory.newEntityReference(SUB_WIKI));
        assertEquals(List.of(new XWikiSecurityRule(new RightSet(Right.ADMIN), RuleState.ALLOW,
            List.of(new DocumentReference("sub", "XWiki", "SubOwner")), null)),
            List.copyOf(subWikiEntry.getRules()));

        SecurityRuleEntry mainWikiEntry = this.defaultSecurityEntryReader
            .read(this.securityReferenceFactory.newEntityReference(new WikiReference("xwiki")));
        assertEquals(List.of(new XWikiSecurityRule(new RightSet(Right.PROGRAM), RuleState.ALLOW,
            List.of(new DocumentReference("xwiki", "XWiki", "MainOwner")), null)),
            List.copyOf(mainWikiEntry.getRules()));

        // The rules of the wiki already allow the owner to administrate every space.
        for (SpaceReference space : List.of(new SpaceReference("Space", SUB_WIKI),
            new SpaceReference("XWiki", SUB_WIKI))) {
            assertEquals(List.of(), List.copyOf(
                this.defaultSecurityEntryReader.read(this.securityReferenceFactory.newEntityReference(space))
                    .getRules()));
        }
        verify(this.wiki).getWikiOwner("sub", this.context);
    }
}
