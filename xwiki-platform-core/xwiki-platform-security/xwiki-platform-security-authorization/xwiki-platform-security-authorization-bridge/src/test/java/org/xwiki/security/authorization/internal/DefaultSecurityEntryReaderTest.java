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

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.DefaultSecurityReferenceFactory;
import org.xwiki.security.SecurityReference;
import org.xwiki.security.SecurityReferenceFactory;
import org.xwiki.security.authorization.AuthorizationException;
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
        when(context.getWikiReference()).thenReturn(new WikiReference("xwiki"));
        when(context.getMainXWiki()).thenReturn("xwiki");
        SecurityReference securityReference = securityReferenceFactory.newEntityReference(new WikiReference("foo"));
        when(wiki.getWikiOwner("foo", context)).thenThrow(
            new XWikiException(XWikiException.MODULE_XWIKI, XWikiException.ERROR_XWIKI_DOES_NOT_EXIST,
                "Does not exist"));
        SecurityRuleEntry ruleEntry = this.defaultSecurityEntryReader.read(securityReference);

        assertTrue(ruleEntry.getRules().isEmpty());
        verify(wiki).getWikiOwner("foo", this.context);
    }
}
