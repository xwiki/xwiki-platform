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
package org.xwiki.watchlist.internal;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.mail.MailSender;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.watchlist.internal.api.WatchListNotifier;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.Utils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.watchlist.internal.DefaultWatchListNotifier}.
 *
 * @version $Id$
 * @since 5.0M2
 */
public class DefaultWatchListNotifierTest
{
    private XWikiContext xcontext;

    private XWiki xwiki;

    private BaseObject userObject;

    private MailSender mockMailSender;

    @Rule
    public MockitoComponentMockingRule<WatchListNotifier> mocker = new MockitoComponentMockingRule<WatchListNotifier>(
        DefaultWatchListNotifier.class);

    @Before
    public void setUp() throws Exception
    {
        this.xcontext = mock(XWikiContext.class);

        Provider<XWikiContext> contextProvider =
            mocker.getInstance(new DefaultParameterizedType(null, Provider.class, XWikiContext.class));
        when(contextProvider.get()).thenReturn(this.xcontext);

        this.xwiki = mock(com.xpn.xwiki.XWiki.class);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);

        DocumentReferenceResolver<String> defaultDocumentReferenceResolver =
            mocker.registerMockComponent(DocumentReferenceResolver.TYPE_STRING);

        DocumentReferenceResolver<String> currentmixedResolver =
            mocker.registerMockComponent(DocumentReferenceResolver.TYPE_STRING, "currentmixed");

        EntityReferenceSerializer<String> defaultStringEntityReferenceSerializer =
            mocker.registerMockComponent(EntityReferenceSerializer.TYPE_STRING);

        Utils.setComponentManager(mocker);

        mockMailSender = mocker.getInstance(MailSender.class);
    }

    // TODO: Add tests since we changed the logic of DefaultWatchListNotifier and it now only delegates.
}
