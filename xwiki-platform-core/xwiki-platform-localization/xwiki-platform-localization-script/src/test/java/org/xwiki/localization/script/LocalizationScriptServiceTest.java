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
package org.xwiki.localization.script;

import java.util.Arrays;
import java.util.Locale;

import javax.inject.Provider;

import org.apache.commons.lang3.ArrayUtils;
import org.jmock.Expectations;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.junit.Assert;
import org.junit.Test;
import org.xwiki.component.internal.ContextComponentManagerProvider;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.jmock.AbstractMockingComponentTestCase;
import org.xwiki.test.jmock.annotation.MockingRequirement;

@MockingRequirement(value = LocalizationScriptService.class, exceptions = {Provider.class})
@ComponentList({ContextComponentManagerProvider.class})
public class LocalizationScriptServiceTest extends AbstractMockingComponentTestCase<LocalizationScriptService>
{
    public void prepareRenderTest() throws Exception
    {
        final BlockRenderer renderer = registerMockComponent(BlockRenderer.class, Syntax.PLAIN_1_0.toIdString());

        final LocalizationManager localizationManager = getComponentManager().getInstance(LocalizationManager.class);
        final LocalizationContext localizationContext = getComponentManager().getInstance(LocalizationContext.class);
        final Translation translation = getMockery().mock(Translation.class);
        getMockery().checking(new Expectations()
        {
            {
                oneOf(renderer).render(with(equal(new WordBlock("message"))), with(any(WikiPrinter.class)));
                will(new CustomAction("render")
                {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable
                    {
                        WikiPrinter printer = (WikiPrinter) invocation.getParameter(1);
                        printer.print("print result");

                        return null;
                    }
                });

                oneOf(translation).render(Locale.ROOT, ArrayUtils.EMPTY_OBJECT_ARRAY);
                will(returnValue(new WordBlock("message")));

                oneOf(localizationManager).getTranslation("key", Locale.ROOT);
                will(returnValue(translation));

                oneOf(localizationContext).getCurrentLocale();
                will(returnValue(Locale.ROOT));
            }
        });
    }

    @Test
    public void render() throws Exception
    {
        prepareRenderTest();

        Assert.assertEquals("print result", getMockedComponent().render("key"));
    }

    @Test
    public void renderWithSyntax() throws Exception
    {
        prepareRenderTest();

        Assert.assertEquals("print result", getMockedComponent().render("key", Syntax.PLAIN_1_0));
    }

    @Test
    public void renderWithSyntaxAndParameters() throws Exception
    {
        prepareRenderTest();

        Assert.assertEquals("print result", getMockedComponent().render("key", Syntax.PLAIN_1_0, Arrays.asList()));
    }

    @Test
    public void renderWithParameters() throws Exception
    {
        prepareRenderTest();

        Assert.assertEquals("print result", getMockedComponent().render("key", Arrays.asList()));
    }

    @Test
    public void getCurrentLocale() throws Exception
    {
        final LocalizationContext localizationContext = getComponentManager().getInstance(LocalizationContext.class);
        getMockery().checking(new Expectations()
        {
            {
                oneOf(localizationContext).getCurrentLocale();
                will(returnValue(Locale.ENGLISH));
            }
        });

        Assert.assertEquals(Locale.ENGLISH, getMockedComponent().getCurrentLocale());
    }
}
