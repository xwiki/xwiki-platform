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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.annotation.Annotation;
import org.xwiki.annotation.content.AlteredContent;
import org.xwiki.annotation.content.ContentAlterer;
import org.xwiki.annotation.io.IOService;
import org.xwiki.annotation.io.IOTargetService;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.PrintRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxRegistry;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationManager;

/**
 * Default implementation for the annotation maintainer.
 *
 * @version $Id$
 * @since 2.3M1
 */
public abstract class AbstractAnnotationMaintainer implements AnnotationMaintainer
{
    /**
     * Annotations storage service.
     */
    @Inject
    protected IOService ioService;

    /**
     * Content storage and manipulation service.
     */
    @Inject
    protected IOTargetService ioContentService;

    /**
     * Space stripper content alterer, to be able to map annotations on the content in the same way the rendering
     * mapping does it.
     */
    @Inject
    @Named("whitespace")
    protected ContentAlterer spaceStripperContentAlterer;

    /**
     * The component manager, used to grab the plain text renderer.
     */
    @Inject
    protected ComponentManager componentManager;

    @Inject
    private SyntaxRegistry syntaxRegistry;

    @Override
    public void updateAnnotations(String target, String previousContent, String currentContent)
        throws MaintainerServiceException
    {
        Collection<Annotation> annotations;
        try {
            annotations = ioService.getAnnotations(target);

            if (annotations.size() == 0) {
                // no annotations, nothing to do
                return;
            }

            // store the annotations to save after update
            List<Annotation> toUpdate = new ArrayList<>();

            // produce the ptr of the previous and current, wrt to syntax
            String syntaxId = ioContentService.getSourceSyntax(target);
            String renderedPreviousContent = renderPlainText(target, previousContent, syntaxId);
            String renderedCurrentContent = renderPlainText(target, currentContent, syntaxId);

            // create the diffs
            Collection<XDelta> differences =
                getDiffService().getDifferences(renderedPreviousContent, renderedCurrentContent);
            // if any differences: note that there can be updates on the content that have no influence on the plain
            // text space normalized version
            if (differences.size() > 0) {
                // compute the spaceless version of the renderedPreviousContent to be able to map the annotation on it
                // (so that matching is done in the same way as for rendering), and then go back to the normalized
                // version
                AlteredContent spacelessRenderedPreviousContent =
                    spaceStripperContentAlterer.alter(renderedPreviousContent);
                // recompute properties for all annotations and store the ones to update
                for (Annotation annotation : annotations) {
                    boolean wasUpdated = recomputeProperties(annotation, differences, renderedPreviousContent,
                        spacelessRenderedPreviousContent, renderedCurrentContent);
                    if (wasUpdated) {
                        toUpdate.add(annotation);
                    }
                }
            }

            // finally store the updates
            ioService.updateAnnotations(target, toUpdate);
        } catch (Exception e) {
            throw new MaintainerServiceException(
                "An exception occurred while updating annotations for content at " + target, e);
        }
    }

    /**
     * Helper method to render the plain text version of the passed content.
     *
     * @param content the content to render in plain text
     * @param syntaxId the source syntax of the content to render
     * @throws Exception if anything goes wrong while rendering the content
     * @return the normalized plain text rendered content
     */
    private String renderPlainText(String target, String content, String syntaxId) throws Exception
    {
        PrintRenderer renderer = componentManager.getInstance(PrintRenderer.class, "normalizer-plain/1.0");

        // Parse
        Parser parser = componentManager.getInstance(Parser.class, syntaxId);
        XDOM xdom = parser.parse(new StringReader(content));

        // Run transformations -> although it's going to be at least strange to handle rendered content since there
        // is no context
        Syntax sourceSyntax = this.syntaxRegistry.getSyntax(syntaxId).orElseThrow(() ->
            new MaintainerServiceException(String.format(
                "Failed to render content for target [%s] since Syntax [%s] is not available in the Syntax Registry",
                target, syntaxId)));
        TransformationManager transformationManager = componentManager.getInstance(TransformationManager.class);
        TransformationContext txContext = new TransformationContext(xdom, sourceSyntax);
        txContext.setId(target);
        transformationManager.performTransformations(xdom, txContext);

        // Render
        WikiPrinter printer = new DefaultWikiPrinter();
        renderer.setPrinter(printer);

        xdom.traverse(renderer);

        return printer.toString();
    }

    /**
     * For each annotation, recompute its properties wrt the differences in the document. The annotation mapping will be
     * done using the spaceless version of the rendered previous content, in order to have synchronization with the
     * rendering, whereas the annotation diff and update will be done wrt to the normalized spaces version, to produce
     * human readable versions of the annotation selection and contexts.
     *
     * @param annotation the annotation to update properties for
     * @param differences the differences between {@code renderedPreviousContent} and {@code renderedCurrentContent}
     * @param renderedPreviousContent the plain text space normalized rendered previous content
     * @param spacelessPreviousContent the spaceless version of the rendered previous content, to be used to map
     *            annotations on the content in the same way they are done on rendering, that is, spaceless.
     * @param renderedCurrentContent the plain text space normalized rendered current content
     * @return the updated state of this annotation, {@code true} if the annotation was updated during property
     *         recompute, {@code false} otherwise
     */
    protected boolean recomputeProperties(Annotation annotation, Collection<XDelta> differences,
        String renderedPreviousContent, AlteredContent spacelessPreviousContent, String renderedCurrentContent)
    {
        boolean updated = false;

        // TODO: do we still want this here? Do we want to try to recover altered annotations?
        if (annotation.getState().equals(AnnotationState.ALTERED)) {
            return updated;
        }

        String spacelessLeftContext = StringUtils.isEmpty(annotation.getSelectionLeftContext()) ? ""
            : spaceStripperContentAlterer.alter(annotation.getSelectionLeftContext()).getContent().toString();
        String spacelessRightContext = StringUtils.isEmpty(annotation.getSelectionRightContext()) ? ""
            : spaceStripperContentAlterer.alter(annotation.getSelectionRightContext()).getContent().toString();
        String spacelessSelection = StringUtils.isEmpty(annotation.getSelection()) ? ""
            : spaceStripperContentAlterer.alter(annotation.getSelection()).getContent().toString();
        String spacelessContext = spacelessLeftContext + spacelessSelection + spacelessRightContext;
        // get the positions for the first character in selection and last character in selection (instead of first out)
        // to protect selection boundaries (spaces are grouped to the left when altered and we don't want extra spaces
        // in the selection by using first index outside the selection)
        int selectionIndex = spacelessLeftContext.length();
        int lastSelectionIndex = selectionIndex + spacelessSelection.length() - 1;

        // map spaceless annotation (in context) on the spaceless version of the content
        int cStart = spacelessPreviousContent.getContent().toString().indexOf(spacelessContext);

        if (spacelessContext.length() == 0 || cStart < 0) {
            // annotation context does not exist or could not be found in the previous rendered content, it must be
            // somewhere in the generated content or something like that, skip it
            return updated;
        }

        int cEnd = cStart + spacelessContext.length();
        int sStart = cStart + selectionIndex;
        int sEnd = cStart + lastSelectionIndex;

        // translate all back to the spaces version
        cStart = spacelessPreviousContent.getInitialOffset(cStart);
        // -1 +1 here because we're interested in the first character outside the context. To get that, we get last
        // significant character and we advance one char further
        cEnd = spacelessPreviousContent.getInitialOffset(cEnd - 1) + 1;
        sStart = spacelessPreviousContent.getInitialOffset(sStart);
        // add one char here so that selection end is outside the selection
        sEnd = spacelessPreviousContent.getInitialOffset(sEnd) + 1;

        // save initial annotation state, to check how it needs to be updated afterwards
        AnnotationState initialState = annotation.getState();

        // the context start & selection length after the modification of the content has took place
        int alteredCStart = cStart;
        int alteredSLength = sEnd - sStart;

        for (XDelta diff : differences) {
            int dStart = diff.getOffset();
            int dEnd = diff.getOffset() + diff.getOriginal().length();
            // 1/ if the diff is before the selection, or ends exactly where selection starts, update the position of
            // the context, to preserve the selection offset
            if (dEnd <= sStart) {
                alteredCStart += diff.getSignedDelta();
            }
            // 2/ diff is inside the selection (and not the previous condition)
            if (dEnd > sStart && dStart >= sStart && dStart < sEnd && dEnd <= sEnd) {
                // update the selection length
                alteredSLength += diff.getSignedDelta();
                annotation.setState(AnnotationState.UPDATED);
                updated = true;
            }

            // 3/ the edit overlaps the annotation selection completely
            if (dStart <= sStart && dEnd >= sEnd) {
                // mark annotation as altered and drop it
                annotation.setState(AnnotationState.ALTERED);
                updated = true;
                break;
            }

            // 4/ the edit overlaps the start of the annotation
            if (dStart < sStart && dEnd > sStart && dEnd <= sEnd) {
                // shift with the signed delta to the right, assume that the edit took place before the annotation and
                // keep its size. This way it will be mapped at the position as if the edit would have taken place
                // before it and will contain the new content at the start of the annotation
                alteredCStart += diff.getSignedDelta();
                annotation.setState(AnnotationState.UPDATED);
                updated = true;
            }

            // 5/ the edit overlaps the end of the annotation
            if (dStart < sEnd && dEnd > sEnd) {
                // nothing, behave as if the edit would have taken place after the annotation
                annotation.setState(AnnotationState.UPDATED);
                updated = true;
            }
        }

        if (annotation.getState() != AnnotationState.ALTERED) {
            // compute the sizes of the contexts to be able to build the annotation contexts
            int cLeftSize = sStart - cStart;
            int cRightSize = cEnd - sEnd;

            // recompute the annotation context and all
            // if this annotation was updated first time during this update, set its original selection
            if (annotation.getState() == AnnotationState.UPDATED && initialState == AnnotationState.SAFE) {
                annotation.setOriginalSelection(annotation.getSelection());
                // FIXME: redundant, but anyway
                updated = true;
            }

            String originalLeftContext = annotation.getSelectionLeftContext();
            String originalSelection = annotation.getSelection();
            String originalRightContext = annotation.getSelectionRightContext();

            String contextLeft = renderedCurrentContent.substring(alteredCStart, alteredCStart + cLeftSize);
            String selection =
                renderedCurrentContent.substring(alteredCStart + cLeftSize, alteredCStart + cLeftSize + alteredSLength);
            String contextRight = renderedCurrentContent.substring(alteredCStart + cLeftSize + alteredSLength,
                alteredCStart + cLeftSize + alteredSLength + cRightSize);
            // and finally update the context & selection
            annotation.setSelection(selection, contextLeft, contextRight);

            // make sure annotation stays unique
            ensureUnique(annotation, renderedCurrentContent, alteredCStart, cLeftSize, alteredSLength, cRightSize);

            // if the annotations selection and/or context have changed during the recompute, set the update flag
            updated = updated || !(selection.equals(originalSelection) && contextLeft.equals(originalLeftContext)
                && contextRight.equals(originalRightContext));
        }

        return updated;
    }

    /**
     * Helper function to adjust passed annotation to make sure it is unique in the content.
     *
     * @param annotation the annotation to ensure uniqueness for
     * @param content the content in which the annotation must be unique
     * @param cStart precomputed position where the annotation starts, passed here for cache reasons
     * @param cLeftSize precomputed length of the context to the left side of the selection inside the annotation
     *            context, passed here for cache reasons
     * @param sLength precomputed length of the annotation selection, passed here for cache reasons
     * @param cRightSize precomputed length of the context to the right side of the selection inside the annotation,
     *            passed here for cache reasons
     */
    private void ensureUnique(Annotation annotation, String content, int cStart, int cLeftSize, int sLength,
        int cRightSize)
    {
        // find out if there is another encounter of the selection text & context than the one at cStart
        List<Integer> occurrences = getOccurrences(content, annotation.getSelectionInContext(), cStart);
        if (occurrences.size() == 0) {
            // it appears only once, it's done
            return;
        }

        // enlarge the context to the left and right with one character, until it is unique
        boolean isUnique = false;
        int cLength = cLeftSize + sLength + cRightSize;
        // size expansion of the context of the annotation such as it becomes unique
        int expansionLeft = 0;
        int expansionRight = 0;
        // the characters corresponding to the ends of the expanded context, to compare with all other occurrences and
        // check if they're unique
        // TODO: an odd situation can happen by comparing characters: at each expansion position there's another
        // occurrence that matches, therefore an unique context is never found although it exists
        // TODO: maybe expansion should be considered by words?
        char charLeft = content.charAt(cStart - expansionLeft);
        char charRight = content.charAt(cStart + cLength + expansionRight - 1);
        while (!isUnique) {
            boolean updated = false;
            // get the characters at left and right and expand, but only if the positions are valid. If one stops being
            // valid, only the other direction will be expanded in search of a new context
            if (cStart - expansionLeft - 1 > 0) {
                expansionLeft++;
                charLeft = content.charAt(cStart - expansionLeft);
                updated = true;
            }
            if (cStart + cLength + expansionRight + 1 <= content.length()) {
                expansionRight++;
                charRight = content.charAt(cStart + cLength + expansionRight - 1);
                updated = true;
            }
            if (!updated) {
                // couldn't update the context to the left nor to the right
                break;
            }
            if (charLeft == ' ' || charRight == ' ') {
                // don't consider uniqueness from space chars
                continue;
            }
            // assume it's unique
            isUnique = true;
            // and check again all occurrences
            for (int occurence : occurrences) {
                // get the chars relative to the current occurrence at the respective expansion positions to the right
                // and left
                Character occurenceCharLeft = getSafeCharacter(content, occurence - expansionLeft);
                Character occurenceCharRight = getSafeCharacter(content, occurence + cLength + expansionRight - 1);
                if ((occurenceCharLeft != null && occurenceCharLeft.charValue() == charLeft)
                    && (occurenceCharRight != null && occurenceCharRight.charValue() == charRight)) {
                    isUnique = false;
                    break;
                }
            }
        }
        if (isUnique) {
            // update the context with the new indexes
            // expand the context to the entire word that it touches (just to make more sense and not depend with only
            // one letter)
            expansionLeft = expansionLeft + toNextWord(content, cStart - expansionLeft, true);
            expansionRight = expansionRight + toNextWord(content, cStart + cLength + expansionRight, false);
            // normally selection is not updated here, only the context therefore we don't set original selection
            String contextLeft = content.substring(cStart - expansionLeft, cStart + cLeftSize);
            String selection = content.substring(cStart + cLeftSize, cStart + cLeftSize + sLength);
            String contextRight = content.substring(cStart + cLeftSize + sLength, cStart + cLength + expansionRight);

            annotation.setSelection(selection, contextLeft, contextRight);
        } else {
            // left the loop for other reasons: for example couldn't expand context
            // leave it unchanged there's not much we could do anyway
        }
    }

    /**
     * Helper function to get all occurrences of {@code pattern} in {@code subject}.
     *
     * @param subject the subject of the search
     * @param pattern the pattern of the search
     * @param exclude value to exclude from the results set
     * @return the list of all occurrences of {@code pattern} in {@code subject}
     */
    private List<Integer> getOccurrences(String subject, String pattern, int exclude)
    {
        List<Integer> indexes = new ArrayList<>();
        int lastIndex = subject.indexOf(pattern);
        while (lastIndex != -1) {
            if (lastIndex != exclude) {
                indexes.add(lastIndex);
            }
            lastIndex = subject.indexOf(pattern, lastIndex + 1);
        }

        return indexes;
    }

    /**
     * Helper function to advance to the next word in the subject, until the first space is encountered, starting from
     * {@code position} and going to the left or to the right, as {@code toLeft} specifies. The returned value is the
     * length of the offset from position to where the space was found.
     *
     * @param subject the string to search for spaces in
     * @param position the position to start the search from
     * @param toLeft {@code true} if the search should be done to the left of the string, {@code false} otherwise
     * @return the offset starting from position, to the left or to the right, until the next word starts (or the
     *         document ends)
     */
    private int toNextWord(String subject, int position, boolean toLeft)
    {
        int expansion = 1;
        // advance until the next space is encountered in subject, from position, to the right by default and left if
        // it's specified otherwise
        boolean isSpaceOrEnd = toLeft ? position - expansion < 0 || subject.charAt(position - expansion) == ' '
            : position + expansion > subject.length() || subject.charAt(position + expansion - 1) == ' ';
        while (!isSpaceOrEnd) {
            expansion++;
            isSpaceOrEnd = toLeft ? position - expansion < 0 || subject.charAt(position - expansion) == ' '
                : position + expansion > subject.length() || subject.charAt(position + expansion - 1) == ' ';
        }

        return expansion - 1;
    }

    /**
     * Helper function to safely get the character at position {@code position} in the passed content, or null
     * otherwise.
     *
     * @param content the content to get the character from
     * @param position the position to get character at
     * @return the character at position {@code position} or {@code null} otherwise.
     */
    private Character getSafeCharacter(String content, int position)
    {
        if (position >= 0 && position < content.length()) {
            return content.charAt(position);
        } else {
            return null;
        }
    }

    /**
     * @return the diff service to be used by this maintainer to get the content differences
     */
    public abstract DiffService getDiffService();
}
