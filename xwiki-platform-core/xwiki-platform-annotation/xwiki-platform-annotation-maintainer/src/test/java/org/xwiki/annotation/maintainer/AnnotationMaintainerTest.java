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
package org.xwiki.annotation.maintainer;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import org.xwiki.annotation.Annotation;
import org.xwiki.annotation.internal.content.DefaultTextExtractor;
import org.xwiki.annotation.internal.content.SpaceNormalizerContentAlterer;
import org.xwiki.annotation.internal.content.WhiteSpaceContentAlterer;
import org.xwiki.annotation.internal.content.filter.WhiteSpaceFilter;
import org.xwiki.annotation.internal.renderer.PlainTextNormalizingRenderer;
import org.xwiki.annotation.io.IOService;
import org.xwiki.annotation.io.IOServiceException;
import org.xwiki.annotation.io.IOTargetService;
import org.xwiki.annotation.maintainer.internal.CharacterDiffService;
import org.xwiki.annotation.maintainer.internal.DefaultAnnotationMaintainer;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.internal.DefaultExecution;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfiguration;
import org.xwiki.rendering.internal.parser.plain.PlainTextStreamParser;
import org.xwiki.rendering.internal.parser.reference.type.URLResourceReferenceTypeParser;
import org.xwiki.rendering.internal.renderer.DefaultLinkLabelGenerator;
import org.xwiki.rendering.internal.renderer.plain.PlainTextRendererFactory;
import org.xwiki.rendering.internal.transformation.DefaultRenderingContext;
import org.xwiki.rendering.internal.transformation.DefaultTransformationManager;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxRegistry;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.test.page.XWikiSyntax20ComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * Tests the annotation maintainer that updates annotations when documents change.
 *
 * @version $Id$
 * @since 2.3M1
 */
@XWikiSyntax20ComponentList
@ComponentList({
    PlainTextNormalizingRenderer.class,
    SpaceNormalizerContentAlterer.class,
    WhiteSpaceFilter.class,
    PlainTextStreamParser.class,
    DefaultLinkLabelGenerator.class,
    DefaultTextExtractor.class,
    URLResourceReferenceTypeParser.class,
    PlainTextRendererFactory.class,
    DefaultTransformationManager.class,
    DefaultRenderingConfiguration.class,
    DefaultRenderingContext.class,
    DefaultExecution.class,
    CharacterDiffService.class,
    WhiteSpaceContentAlterer.class
})
@ComponentTest
class AnnotationMaintainerTest
{
    /**
     * Document description files to run this test for.
     */
    private static final List<String> files = List.of(
        "maintainer/correction/Correction1",
        "maintainer/correction/Correction2",
        "maintainer/correction/Correction3",
        "maintainer/correction/Correction4",

        "maintainer/correction/multiple/CorrectionMultiple1",
        "maintainer/correction/multiple/CorrectionMultiple2",
        "maintainer/correction/multiple/CorrectionMultiple3",
        "maintainer/correction/multiple/CorrectionMultiple4",
        "maintainer/correction/multiple/CorrectionMultiple5",

        "maintainer/endpoint/Beginning1",
        "maintainer/endpoint/Beginning2",
        "maintainer/endpoint/Beginning3",
        "maintainer/endpoint/Beginning4",
        "maintainer/endpoint/Beginning5",

        "maintainer/endpoint/End1",
        "maintainer/endpoint/End2",
        "maintainer/endpoint/End3",
        "maintainer/endpoint/End4",
        "maintainer/endpoint/End5",

        "maintainer/endpoint/Endpoints1",
        "maintainer/endpoint/Endpoints2",
        "maintainer/endpoint/Endpoints3",
        "maintainer/endpoint/Endpoints4",

        // TODO: the following tests, paragraph and altered, need to be reviewed since there are multiple issues with
        // character based diff that cause them to yield different results for a difference of a single letter which can
        // cause a matching point.
        "maintainer/paragraph/Paragraph1",
        "maintainer/alter/Altered1",
        // TODO: enable when/if fixed: see comment in the file
        // "maintainer/alter/Altered2",
        "maintainer/alter/Altered3",
        "maintainer/alter/Altered4",

        // tests for the uniqueness maintenance
        "maintainer/uniqueness/Unique1",
        "maintainer/uniqueness/Unique2",
        "maintainer/uniqueness/Unique3",

        "maintainer/uniqueness/Duplicate1",
        "maintainer/uniqueness/Duplicate2",
        "maintainer/uniqueness/Duplicate3",
        "maintainer/uniqueness/Duplicate4",
        "maintainer/uniqueness/Duplicate5",
        "maintainer/uniqueness/Duplicate6",
        "maintainer/uniqueness/Duplicate7",
        "maintainer/uniqueness/Duplicate8",
        "maintainer/uniqueness/Duplicate9",

        // tests for the cases when annotation stays unchanged, regardless of the changes on the content, or the
        // selection of the annotation. Should check that the updater doesn't even run on these annotations
        "maintainer/unchanged/Unchanged1",
        "maintainer/unchanged/Unchanged2",
        "maintainer/unchanged/Unchanged3",
        "maintainer/unchanged/Unchanged4",
        "maintainer/unchanged/Unchanged5",

        "maintainer/spaces/Spaces1",
        "maintainer/spaces/Spaces2",
        "maintainer/spaces/Spaces3",
        "maintainer/spaces/Spaces4",
        "maintainer/spaces/Spaces5"
        // TODO: add test cases here for the case when an annotation becomes non-unique on update, but the 2 differ by
        // some spaces (spaceless they are the same, but with normalized spaces they are different. Maintainer will fail
        // in this case)

        // TODO: add tests for the case of multiple annotations which are being changed
    );

    /**
     * The annotation maintainer under test.
     */
    @InjectMockComponents
    private DefaultAnnotationMaintainer annotationMaintainer;

    @MockComponent
    private IOTargetService ioTargetService;

    @MockComponent
    private IOService ioService;

    @MockComponent
    private SyntaxRegistry syntaxRegistry;

    @BeforeComponent
    void registerComponents(MockitoComponentManager componentManager) throws Exception
    {
        componentManager.registerComponent(ComponentManager.class, "context", componentManager);
    }

    MockDocument setup(String docName) throws IOServiceException, IOException, ParseException
    {
        TestDocumentFactory docFactory = new TestDocumentFactory();
        MockDocument mDoc = docFactory.getDocument(docName);
        when(ioService.getAnnotations(docName)).thenReturn(mDoc.getAnnotations());
        doAnswer(invocationOnMock -> {
            String documentName = invocationOnMock.getArgument(0);
            org.xwiki.annotation.MockDocument document = docFactory.getDocument(documentName);
            Collection<Annotation> annList = invocationOnMock.getArgument(1);
            for (Annotation ann : annList) {
                Annotation toUpdate = getAnnotation(ann.getId(), document.getAnnotations());
                // remove toUpdate and add ann
                if (toUpdate != null) {
                    document.getAnnotations().remove(toUpdate);
                }
                document.getAnnotations().add(ann);
            }
            return null;
        }).when(ioService).updateAnnotations(eq(docName), any(Collection.class));

        when(ioTargetService.getSource(docName)).thenReturn(mDoc.getSource());
        when(ioTargetService.getSourceSyntax(docName)).thenReturn(mDoc.getSyntax());
        when(syntaxRegistry.getSyntax("xwiki/2.0")).thenReturn(Optional.of(Syntax.XWIKI_2_0));
        when(syntaxRegistry.resolveSyntax("xwiki/2.0")).thenReturn(Syntax.XWIKI_2_0);
        return mDoc;
    }

    /**
     * Tests the update of a document.
     *
     * @throws IOException if anything goes wrong mocking the documents
     * @throws MaintainerServiceException if anything goes wrong maintaining the the document annotations
     */
    @ParameterizedTest
    @FieldSource("files")
    void testUpdate(String docName) throws IOException, MaintainerServiceException, IOServiceException, ParseException
    {

        // ignore the docName ftm, just test the marvelous setup
        MockDocument doc = setup(docName);
        // TODO: this is not the place to put this code, but it's the most comfortable
        copyOriginalSelections(doc);

        annotationMaintainer.updateAnnotations(docName, doc.getSource(), doc.getModifiedSource());

        // test the result
        assertSameAnnotations(doc.getUpdatedAnnotations(), doc.getAnnotations());
    }

    /**
     * Helper method to test if the two passed annotation lists contain the same annotations.
     *
     * @param expected the expected list of annotations
     * @param actual the actual list of annotations
     */
    private void assertSameAnnotations(List<Annotation> expected, List<Annotation> actual)
    {
        assertEquals(expected.size(), actual.size());

        for (Annotation actualAnn : actual) {
            Annotation expectedAnn = getAnnotation(actualAnn.getId(), expected);
            assertNotNull(expectedAnn);
            assertEquals(expectedAnn.getSelection(), actualAnn.getSelection());
            assertEquals(expectedAnn.getSelectionLeftContext(), actualAnn.getSelectionLeftContext());
            assertEquals(expectedAnn.getSelectionRightContext(), actualAnn.getSelectionRightContext());
            assertEquals(expectedAnn.getState(), actualAnn.getState());
            assertEquals(expectedAnn.getOriginalSelection(), actualAnn.getOriginalSelection());
        }
    }

    /**
     * Helper function to get the annotation with the passed id from the passed list.
     *
     * @param annId the id of the searched annotation
     * @param list the list of annotations where to search for the passed annotation
     * @return the found annotation
     */
    private static Annotation getAnnotation(String annId, Collection<Annotation> list)
    {
        for (Annotation ann : list) {
            if (ann.getId().equals(annId)) {
                return ann;
            }
        }

        return null;
    }

    /**
     * Helper function to set the original selected contents in the updated annotations of this document from the
     * original list of annotations.
     *
     * @param doc the document for which to set the updated annotations original selected contents
     */
    private void copyOriginalSelections(MockDocument doc)
    {
        // set the original selections of updated annotations from the original annotations of the document
        for (Annotation updatedAnn : doc.getUpdatedAnnotations()) {
            if (updatedAnn.getState() != AnnotationState.UPDATED) {
                continue;
            }
            Annotation originalAnn = getAnnotation(updatedAnn.getId(), doc.getAnnotations());
            updatedAnn.setOriginalSelection(originalAnn.getSelection());
        }
    }
}
