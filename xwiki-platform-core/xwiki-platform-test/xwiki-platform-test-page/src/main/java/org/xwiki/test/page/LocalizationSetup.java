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
package org.xwiki.test.page;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.stream.Collectors;

import org.mockito.invocation.InvocationOnMock;
import org.xwiki.localization.Translation;
import org.xwiki.localization.TranslationBundle;
import org.xwiki.localization.TranslationBundleContext;
import org.xwiki.localization.script.LocalizationScriptService;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.TestComponentManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Set up Localization Services for Page Tests.
 *
 * @version $Id$
 * @since 8.3M2
 */
public final class LocalizationSetup
{
    private LocalizationSetup()
    {
        // Utility class and thus no public constructor.
    }

    /**
     * Sets up localization so that all translation return their key as translation values. When the translation has
     * parameters, the parameter are appended comma separated and  between square brackets after the key.
     * <p>
     * For instance, calling {@code $services.localization.render('my.key')} returns {@code "my.key"}, and calling
     * {@code $services.localization.render('my.key2', 'a', 1)} returns {@code "my.key2 [a, 1]"}.
     *
     * @param tcm the stubbed Component Manager for the test
     * @throws Exception when a setup error occurs
     */
    public static void setUp(TestComponentManager tcm) throws Exception
    {
        LocalizationScriptService lss = mock(LocalizationScriptService.class);
        tcm.registerComponent(ScriptService.class, "localization", lss);

        // The translations are mocked by returning the translation, suffixed with the list of the String.valueOf
        // values of the translation parameters if they exist.
        // We mock the translations instead of using their actual values because they are subject to change from
        // Weblate, possibly making the build fail unexpectedly.
        when(lss.render(anyString())).thenAnswer(invocationOnMock -> {
                // Return the translation key as the value
                return renderString(invocationOnMock.getArgument(0), new Object[] {});
            }
        );
        when(lss.render(anyString(), anyCollection())).thenAnswer(invocationOnMock -> {
            // Displays the comma-separated list of parameters between squared brackets after the translation key as
            // the value, so that they can be verified in tests.
            // For instance: my.key [paramA, paramB]
            Collection<?> parameters = invocationOnMock.getArgument(1);
            return renderString(invocationOnMock.getArgument(0), parameters.toArray());
        });

        TranslationBundleContext translationBundleContext = tcm.getInstance(TranslationBundleContext.class);
        TranslationBundle translationBundle = mock(TranslationBundle.class);
        when(translationBundle.getTranslation(any(), any()))
            .thenAnswer(invocationOnMockTranslation -> {
                Translation translation = mock(Translation.class);
                when(translation.getLocale()).thenReturn(Locale.ENGLISH);
                String translationKey = invocationOnMockTranslation.getArgument(0);
                when(translation.getKey()).thenReturn(translationKey);
                when(translation.render(any())).thenAnswer(invocationOnMockRender -> {
                    Object[] parameters = getVarArgs(invocationOnMockRender, 0);
                    return renderBlock(translationKey, parameters);
                });
                when(translation.render(any(), any())).thenAnswer(invocationOnMockRender -> {
                    Object[] parameters = getVarArgs(invocationOnMockRender, 1);
                    return renderBlock(translationKey, parameters);
                });
                return translation;
            });
        translationBundleContext.addBundle(translationBundle);
    }

    private static WordBlock renderBlock(String translationKey, Object[] parameters)
    {
        return new WordBlock(renderString(translationKey, parameters));
    }

    private static String renderString(String translationKey, Object[] parameters)
    {
        String word;
        if (parameters.length == 0) {
            word = translationKey;
        } else {
            String parametersString = Arrays.stream(parameters)
                .map(String::valueOf)
                .collect(Collectors.joining(", ", "[", "]"));
            word = String.format("%s %s", translationKey, parametersString);
        }
        return word;
    }

    private static Object[] getVarArgs(InvocationOnMock invocationOnMockRender, int i)
    {
        Object[] parameters;
        if (invocationOnMockRender.getArguments().length > i) {
            Object argument = invocationOnMockRender.getArgument(i);
            if (argument instanceof String) {
                parameters = new Object[] { argument };
            } else {
                parameters = (Object[]) argument;
            }
        } else {
            parameters = new Object[] {};
        }
        return parameters;
    }
}
