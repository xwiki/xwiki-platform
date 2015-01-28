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
package com.xpn.xwiki.api;

import java.util.Locale;

import org.apache.velocity.VelocityContext;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.jmock.Expectations;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.jmock.AbstractComponentTestCase;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.CoreConfiguration;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.web.Utils;

/**
 * Unit tests for {@link Context}.
 *
 * @version $Id$
 * @since 4.4RC1
 */
public class ContextTest extends AbstractComponentTestCase
{
    public static class XWikiDocumentMatcher extends TypeSafeMatcher<XWikiDocument>
    {
        private DocumentReference documentReference;

        public XWikiDocumentMatcher(DocumentReference documentReference)
        {
            this.documentReference = documentReference;
        }

        @Override
        protected boolean matchesSafely(XWikiDocument document)
        {
            return documentReference.equals(document.getDocumentReference());
        }

        @Override
        public void describeTo(Description description)
        {
            description.appendText("a document with a reference equal to [")
                .appendValue(this.documentReference)
                .appendText("]");
        }
    }

    @Factory
    public static Matcher<XWikiDocument> anXWikiDocumentWithReference(DocumentReference documentReference)
    {
        return new XWikiDocumentMatcher(documentReference);
    }

    @Before
    public void configure() throws Exception
    {
        getMockery().setImposteriser(ClassImposteriser.INSTANCE);
        Utils.setComponentManager(getComponentManager());
    }

    /**
     * Tests that pages can override the default property display mode using {@code $xcontext.setDisplayMode}.
     *
     * @see "XWIKI-2436."
     */
    @Test
    public void setDisplayMode() throws Exception
    {
        // Setup Context and XWiki objects
        final XWikiContext xcontext = new XWikiContext();
        xcontext.setMainXWiki("testwiki");
        xcontext.setWikiId("testwiki");

        final com.xpn.xwiki.XWiki xwiki = getMockery().mock(com.xpn.xwiki.XWiki.class);
        xcontext.setWiki(xwiki);

        final CoreConfiguration coreConfiguration = registerMockComponent(CoreConfiguration.class);
        final VelocityManager velocityManager = registerMockComponent(VelocityManager.class);

        final DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        final XWikiDocument document = new XWikiDocument(documentReference);
        final BaseClass baseClass = document.getXClass();

        getMockery().checking(new Expectations()
        {{
            allowing(xwiki).getCurrentContentSyntaxId("xwiki/2.1", xcontext);
            will(returnValue("xwiki/2.1"));
            allowing(coreConfiguration).getDefaultDocumentSyntax();
            will(returnValue(Syntax.XWIKI_2_1));
            allowing(velocityManager).getVelocityContext();
            will(returnValue(new VelocityContext()));
            allowing(xwiki).getLanguagePreference(xcontext);
            will(returnValue("en"));
            // Translated document
            allowing(xwiki).getDocument(with(equal(new DocumentReference(documentReference, Locale.ENGLISH))), with(same(xcontext)));
            will(returnValue(document));
            allowing(xwiki).getXClass(documentReference, xcontext);
            will(returnValue(baseClass));
            // Decide that there's no custom Displayer for the String field
            allowing(xwiki).exists(new DocumentReference("testwiki", "XWiki", "StringDisplayer"), xcontext);
            will(returnValue(false));
            allowing(xwiki).evaluateTemplate("displayer_string.vm", xcontext);
            will(returnValue(""));
        }});

        baseClass.addTextField("prop", "prop", 5);
        BaseObject obj = (BaseObject) document.getXClass().newObject(xcontext);
        obj.setStringValue("prop", "value");
        document.addXObject(obj);

        // Tie together Execution Context and old XWiki Context
        Execution execution = getComponentManager().getInstance(Execution.class);
        execution.getContext().setProperty("xwikicontext", xcontext);

        Context context = new Context(xcontext);
        context.setDisplayMode("edit");

        // We verify that the result contains a form input
        Assert.assertEquals("<input size='5' id='space.page_0_prop' value='value' name='space.page_0_prop' "
            + "type='text'/>", document.display("prop", xcontext));
    }
}
