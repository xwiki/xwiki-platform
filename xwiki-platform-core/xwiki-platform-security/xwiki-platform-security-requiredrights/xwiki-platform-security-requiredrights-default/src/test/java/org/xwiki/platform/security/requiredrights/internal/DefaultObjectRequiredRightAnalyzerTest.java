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
package org.xwiki.platform.security.requiredrights.internal;

import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultObjectRequiredRightAnalyzer}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
class DefaultObjectRequiredRightAnalyzerTest
{
    @InjectMockComponents
    private DefaultObjectRequiredRightAnalyzer analyzer;

    @MockComponent
    @Named("object/XWiki.TestClass")
    private RequiredRightAnalyzer<BaseObject> mockAnalyzer;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @Test
    void analyzeWithCustomAnalyzer() throws XWikiException, RequiredRightsException
    {
        XWikiDocument testDocument = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        BaseObject testObject = testDocument.newXObject(new DocumentReference("wiki", "XWiki", "TestClass"),
            this.oldcore.getXWikiContext());

        RequiredRightAnalysisResult mockResult = mock();
        when(this.mockAnalyzer.analyze(testObject)).thenReturn(List.of(mockResult));
        List<RequiredRightAnalysisResult> results = this.analyzer.analyze(testObject);

        assertEquals(List.of(mockResult), results);
        verify(this.mockAnalyzer).analyze(testObject);
    }
}
