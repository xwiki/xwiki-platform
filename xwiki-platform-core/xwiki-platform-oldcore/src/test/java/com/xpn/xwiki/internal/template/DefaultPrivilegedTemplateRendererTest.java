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
package com.xpn.xwiki.internal.template;

import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import com.xpn.xwiki.internal.model.reference.CurrentStringDocumentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentStringEntityReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentReferenceDocumentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentReferenceEntityReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentEntityReferenceValueProvider;
import com.xpn.xwiki.internal.model.reference.XClassRelativeStringEntityReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentMixedStringDocumentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentMixedEntityReferenceValueProvider;
import com.xpn.xwiki.internal.model.reference.CompactStringEntityReferenceSerializer;
import com.xpn.xwiki.internal.model.reference.CompactWikiStringEntityReferenceSerializer;
import com.xpn.xwiki.internal.model.reference.CurrentReferenceObjectReferenceResolver;
import org.xwiki.model.internal.reference.ExplicitStringDocumentReferenceResolver;
import org.xwiki.model.internal.reference.ExplicitStringEntityReferenceResolver;
import org.xwiki.model.internal.reference.ExplicitReferenceDocumentReferenceResolver;
import org.xwiki.model.internal.reference.ExplicitReferenceEntityReferenceResolver;
import org.xwiki.model.internal.reference.DefaultEntityReferenceValueProvider;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceResolver;
import org.xwiki.model.internal.reference.RelativeStringEntityReferenceResolver;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.LocalStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.LocalUidStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.UidStringEntityReferenceSerializer;
import org.xwiki.model.internal.DefaultModelContext;
import org.xwiki.model.internal.DefaultModelConfiguration;
import org.xwiki.rendering.internal.syntax.DefaultSyntaxFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.Utils;

import org.junit.Test;
import org.junit.Assert;
import org.junit.Rule;


import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test that delegation of programming rights is enabled and disabled only when executing "privileged" templates.
 *
 * @version $Id$
 * @since 4.5M1
 */
// This is the component list required for instantiating an XWikiDocument.
@ComponentList({CurrentStringDocumentReferenceResolver.class, CurrentStringEntityReferenceResolver.class,
        CurrentEntityReferenceValueProvider.class, DefaultModelContext.class, DefaultModelConfiguration.class,
        ExplicitStringDocumentReferenceResolver.class, ExplicitStringEntityReferenceResolver.class,
        ExplicitReferenceDocumentReferenceResolver.class, ExplicitReferenceEntityReferenceResolver.class,
        XClassRelativeStringEntityReferenceResolver.class, CurrentMixedStringDocumentReferenceResolver.class,
        CurrentMixedEntityReferenceValueProvider.class, DefaultEntityReferenceValueProvider.class,
        CurrentReferenceDocumentReferenceResolver.class, CurrentReferenceEntityReferenceResolver.class,
        RelativeStringEntityReferenceResolver.class, CompactStringEntityReferenceSerializer.class,
        DefaultStringEntityReferenceSerializer.class, CompactWikiStringEntityReferenceSerializer.class,
        LocalStringEntityReferenceSerializer.class, LocalUidStringEntityReferenceSerializer.class,
        UidStringEntityReferenceSerializer.class, CurrentReferenceObjectReferenceResolver.class,
        DefaultSyntaxFactory.class
        })
public class DefaultPrivilegedTemplateRendererTest
{

    @Rule
    public final MockitoComponentMockingRule<PrivilegedTemplateRenderer> mocker =
        new MockitoComponentMockingRule<PrivilegedTemplateRenderer>(DefaultPrivilegedTemplateRenderer.class);

    @Test
    public void ordinaryTemplate() throws Exception
    {
        PrivilegedTemplateRenderer privilegedTemplateRenderer = mocker.getMockedComponent();

        final XWikiContext context = mock(XWikiContext.class);

        Execution execution = mocker.getInstance(Execution.class);

        final ExecutionContext executionContext = new ExecutionContext();

        executionContext.newProperty(XWikiContext.EXECUTIONCONTEXT_KEY).initial(context).declare();

        when(execution.getContext()).thenReturn(executionContext);

        // XWikiVelocityRenderer.evaluate is going to fail and print a stack trace to stderr.
        privilegedTemplateRenderer.evaluateTemplate("content", "template.vm");

        verify(context, never()).setDoc(any(XWikiDocument.class));
    }

    @Test
    public void privilegedTemplate() throws Exception
    {
        // Required in order to instantiate an XWikiDocument
        Utils.setComponentManager(mocker);

        PrivilegedTemplateRenderer privilegedTemplateRenderer = mocker.getMockedComponent();

        final XWikiContext context = mock(XWikiContext.class);

        Execution execution = mocker.getInstance(Execution.class);

        final ExecutionContext executionContext = new ExecutionContext();

        executionContext.newProperty(XWikiContext.EXECUTIONCONTEXT_KEY).initial(context).declare();

        when(execution.getContext()).thenReturn(executionContext);
        when(context.getDatabase()).thenReturn("xwiki");

        // XWikiVelocityRenderer.evaluate is going to fail and print a stack trace to stderr.
        privilegedTemplateRenderer.evaluateTemplate("content", "/templates/suggest.vm");

        verify(context, times(2)).setDoc(any(XWikiDocument.class));
    }

}
