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
package org.xwiki.localization.macro;

import java.util.Arrays;
import java.util.Locale;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.runner.RunWith;
import org.xwiki.localization.TranslationBundle;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.test.integration.RenderingTestSuite;
import org.xwiki.test.jmock.MockingComponentManager;

/**
 * Run all tests found in {@code *.test} files located in the classpath. These {@code *.test} files must follow the
 * conventions described in {@link org.xwiki.rendering.test.integration.TestDataParser}.
 * 
 * @version $Id$
 * @since 3.4M2
 */
@RunWith(RenderingTestSuite.class)
public class IntegrationTests
{
    @RenderingTestSuite.Initialized
    public void initialize(MockingComponentManager componentManager) throws Exception
    {
        Mockery mockery = new JUnit4Mockery();

        final LocalizationManager localizationManager =
            componentManager.registerMockComponent(mockery, LocalizationManager.class);
        final LocalizationContext localizationContext =
            componentManager.registerMockComponent(mockery, LocalizationContext.class);

        mockery.checking(new Expectations()
        {
            {
                allowing(localizationManager).getTranslation("some.translation", Locale.ENGLISH);
                will(returnValue(new Translation()
                {
                    @Override
                    public Block render(Locale locale, Object... parameters)
                    {
                        return parameters.length > 0 ? new WordBlock("entranslationmessage"
                            + Arrays.toString(parameters)) : new WordBlock("entranslationmessage");
                    }

                    @Override
                    public Block render(Object... parameters)
                    {
                        return render(null, parameters);
                    }

                    @Override
                    public String getRawSource()
                    {
                        return "entranslationmessagesource";
                    }

                    @Override
                    public Locale getLocale()
                    {
                        return Locale.ENGLISH;
                    }

                    @Override
                    public String getKey()
                    {
                        return "some.translation";
                    }

                    @Override
                    public TranslationBundle getBundle()
                    {
                        return null;
                    }
                }));

                allowing(localizationManager).getTranslation("some.translation", Locale.FRENCH);
                will(returnValue(new Translation()
                {
                    @Override
                    public Block render(Locale locale, Object... parameters)
                    {
                        return parameters.length > 0 ? new WordBlock("frtranslationmessage"
                            + Arrays.toString(parameters)) : new WordBlock("frtranslationmessage");
                    }

                    @Override
                    public Block render(Object... parameters)
                    {
                        return render(null, parameters);
                    }

                    @Override
                    public String getRawSource()
                    {
                        return "frtranslationmessagesource";
                    }

                    @Override
                    public Locale getLocale()
                    {
                        return Locale.FRENCH;
                    }

                    @Override
                    public String getKey()
                    {
                        return "some.translation";
                    }

                    @Override
                    public TranslationBundle getBundle()
                    {
                        return null;
                    }
                }));

                allowing(localizationManager).getTranslation("unexisting.translation", Locale.ENGLISH);
                will(returnValue(null));

                allowing(localizationContext).getCurrentLocale();
                will(returnValue(Locale.ENGLISH));
            }
        });
    }
}
