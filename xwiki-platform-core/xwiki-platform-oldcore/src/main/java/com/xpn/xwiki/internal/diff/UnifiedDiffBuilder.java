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
package com.xpn.xwiki.internal.diff;

import java.util.List;
import java.util.Stack;

import org.suigeneris.jrcs.diff.Revision;
import org.suigeneris.jrcs.diff.RevisionVisitor;
import org.suigeneris.jrcs.diff.delta.AddDelta;
import org.suigeneris.jrcs.diff.delta.ChangeDelta;
import org.suigeneris.jrcs.diff.delta.DeleteDelta;
import org.suigeneris.jrcs.diff.delta.Delta;

import com.xpn.xwiki.internal.diff.UnifiedDiffLine.LineType;

/**
 * Builds a <a href="http://en.wikipedia.org/wiki/Diff#Unified_format">unified diff</a> by visiting a {@link Revision}
 * and its {@link Delta}s.
 * <p>
 * NOTE: This class was greatly inspired by the <a href="http
 * ://cvsgrab.cvs.sourceforge.net/viewvc/cvsgrab/cvsgrab/src/java/org/apache/commons/jrcs/diff/print/UnifiedPrint
 * .java">{@code UnifiedPrint}</a> class written by <a href="mailto:ludovicc@users.sourceforge.net">Ludovic Claude</a>
 * for the <a href="http://cvsgrab.sourceforge.net/">CVSGrab</a> project under the Apache Software License version 1.1.
 * 
 * @version $Id$
 * @since 4.1M2
 */
public class UnifiedDiffBuilder implements RevisionVisitor
{
    /**
     * The collection of unified diff blocks.
     */
    protected final Stack<UnifiedDiffBlock> blocks = new Stack<UnifiedDiffBlock>();

    /**
     * The number of unmodified lines to display before and after a block of modified lines.
     */
    private int contextSize = 3;

    /**
     * The last change processed by this builder.
     */
    private Delta lastDelta;

    /**
     * The original version.
     */
    private final Object[] original;

    /**
     * The revised version.
     */
    private final Object[] revised;

    /**
     * Creates a new instance.
     * 
     * @param original the original version
     * @param revised the revised version
     */
    public UnifiedDiffBuilder(Object[] original, Object[] revised)
    {
        this.original = original;
        this.revised = revised;
    }

    @Override
    public void visit(Revision revision)
    {
        // Do nothing.
    }

    @Override
    public void visit(AddDelta delta)
    {
        maybeStartBlock(delta);
        for (int l = delta.getRevised().first(); l <= delta.getRevised().last(); l++) {
            blocks.peek().add(new UnifiedDiffLine(l, LineType.ADDED, revised[l]));
        }
    }

    @Override
    public void visit(ChangeDelta delta)
    {
        maybeStartBlock(delta);
        for (int l = delta.getOriginal().first(); l <= delta.getOriginal().last(); l++) {
            blocks.peek().add(new UnifiedDiffLine(l, LineType.DELETED, original[l]));
        }
        for (int l = delta.getRevised().first(); l <= delta.getRevised().last(); l++) {
            blocks.peek().add(new UnifiedDiffLine(l, LineType.ADDED, revised[l]));
        }
    }

    @Override
    public void visit(DeleteDelta delta)
    {
        maybeStartBlock(delta);
        for (int l = delta.getOriginal().first(); l <= delta.getOriginal().last(); l++) {
            blocks.peek().add(new UnifiedDiffLine(l, LineType.DELETED, original[l]));
        }
    }

    /**
     * @return the number of unmodified lines to display before and after a block of modified lines
     */
    public int getContextSize()
    {
        return contextSize;
    }

    /**
     * Sets the number of unmodified lines to display before and after a block of modified lines.
     * 
     * @param contextSize the context size
     */
    public void setContextSize(int contextSize)
    {
        this.contextSize = contextSize;
    }

    /**
     * @return the unified diff
     */
    public List<UnifiedDiffBlock> getResult()
    {
        maybeEndBlock();
        return blocks;
    }

    /**
     * Starts a new {@link UnifiedDiffBlock} if the provided change is in a different context.
     * 
     * @param delta the change
     */
    private void maybeStartBlock(Delta delta)
    {
        if (lastDelta == null || lastDelta.getOriginal().last() < delta.getOriginal().anchor() - getContextSize() * 2) {
            maybeEndBlock();
            blocks.push(new UnifiedDiffBlock());
        }

        int anchor = delta.getOriginal().anchor();
        int border = blocks.peek().isEmpty() ? getContextSize() : getContextSize() * 2;
        int lastChangedLineNumber = lastDelta == null ? -1 : lastDelta.getOriginal().last();
        for (int i = border; i > 0; i--) {
            if (anchor - i > lastChangedLineNumber) {
                blocks.peek().add(new UnifiedDiffLine(anchor - i, LineType.CONTEXT, original[anchor - i]));
            }
        }

        lastDelta = delta;
    }

    /**
     * Ends the last {@link UnifiedDiffBlock}.
     */
    private void maybeEndBlock()
    {
        if (!blocks.isEmpty()) {
            int lastLineNumber = lastDelta.getOriginal().last();
            for (int i = lastLineNumber + 1; i <= lastLineNumber + getContextSize() && i < original.length; i++) {
                blocks.peek().add(new UnifiedDiffLine(i, LineType.CONTEXT, original[i]));
            }
        }
    }
}
