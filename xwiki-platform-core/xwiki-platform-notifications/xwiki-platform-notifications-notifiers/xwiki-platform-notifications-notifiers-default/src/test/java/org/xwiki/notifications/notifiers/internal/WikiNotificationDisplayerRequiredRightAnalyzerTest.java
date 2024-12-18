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
package org.xwiki.notifications.notifiers.internal;

import java.util.List;
import java.util.function.Supplier;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.display.BlockSupplierProvider;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.velocity.internal.util.VelocityDetector;

import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Component test for {@link WikiNotificationDisplayerRequiredRightAnalyzer}.
 *
 * @version $Id$
 */
@ComponentTest
class WikiNotificationDisplayerRequiredRightAnalyzerTest
{
    protected static final String PRETTY_NAME = "Pretty Name";

    protected static final BaseObjectReference OBJECT_REFERENCE = mock();

    @InjectMockComponents
    private WikiNotificationDisplayerRequiredRightAnalyzer analyzer;

    @MockComponent
    @Named("translation")
    private BlockSupplierProvider<String> translationMessageSupplierProvider;

    @MockComponent
    private BlockSupplierProvider<BaseObject> baseObjectBlockSupplierProvider;

    @MockComponent
    private VelocityDetector velocityDetector;

    @Mock
    private BaseObject baseObject;

    @BeforeEach
    void setup()
    {
        when(this.translationMessageSupplierProvider.get(anyString())).then(invocation -> {
            String message = invocation.getArgument(0);
            return (Supplier<Block>) () -> new WordBlock(message);
        });
        when(this.baseObjectBlockSupplierProvider.get(this.baseObject)).then(invocation -> {
            BaseObject object = invocation.getArgument(0);
            return (Supplier<Block>) () -> new WordBlock(object.getPrettyName());
        });

        when(this.baseObject.getReference()).thenReturn(OBJECT_REFERENCE);
        when(this.baseObject.getPrettyName()).thenReturn(PRETTY_NAME);
    }

    @Test
    void analyzeWithoutScript()
    {
        List<RequiredRightAnalysisResult> results = this.analyzer.analyze(this.baseObject);

        assertEquals(1, results.size());
        RequiredRightAnalysisResult result = results.get(0);
        assertEquals(OBJECT_REFERENCE, result.getEntityReference());
        assertEquals(new WordBlock("notifications.notifiers.wikiNotificationDisplayerRequiredRight"),
            result.getSummaryMessage());
        assertEquals(new WordBlock(PRETTY_NAME), result.getDetailedMessage());
        assertEquals(List.of(RequiredRight.WIKI_ADMIN), result.getRequiredRights());
    }

    @Test
    void analyzeWithScript()
    {
        String template = "template";
        when(this.baseObject.getStringValue(WikiNotificationDisplayerDocumentInitializer.NOTIFICATION_TEMPLATE))
            .thenReturn(template);
        when(this.velocityDetector.containsVelocityScript(template)).thenReturn(true);

        List<RequiredRightAnalysisResult> results = this.analyzer.analyze(this.baseObject);

        assertEquals(1, results.size());
        RequiredRightAnalysisResult result = results.get(0);
        assertEquals(OBJECT_REFERENCE, result.getEntityReference());
        assertEquals(new WordBlock("notifications.notifiers.wikiNotificationDisplayerRequiredRightWithScript"),
            result.getSummaryMessage());
        assertEquals(new WordBlock(PRETTY_NAME), result.getDetailedMessage());
        assertEquals(List.of(RequiredRight.WIKI_ADMIN, RequiredRight.MAYBE_PROGRAM), result.getRequiredRights());
    }

    @Test
    void analyzeWithNullBaseObject()
    {
        List<RequiredRightAnalysisResult> results = this.analyzer.analyze(null);

        assertTrue(results.isEmpty());
    }
}
