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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.ArrayUtils;
import org.suigeneris.jrcs.diff.Diff;
import org.suigeneris.jrcs.diff.DifferentiationFailedException;
import org.suigeneris.jrcs.diff.myers.MyersDiff;
import org.suigeneris.jrcs.util.ToString;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.script.service.ScriptService;

/**
 * Generate diffs from server-side scripts.
 * 
 * @version $Id$
 * @since 4.1M2
 */
@Component
@Named("diff")
@Singleton
public class DiffScriptService implements ScriptService
{
    /**
     * The key under which the last encountered error is stored in the current execution context.
     */
    public static final String DIFF_ERROR_KEY = "scriptservice.diff.error";

    /**
     * The component used to access the execution context.
     */
    @Inject
    private Execution execution;

    /**
     * Builds a unified diff between two versions of a text.
     * 
     * @param original the original version
     * @param revised the revised version
     * @return the list of unified diff blocks
     */
    public List<UnifiedDiffBlock> unified(String original, String revised)
    {
        setError(null);

        try {
            String[] originalLines = ToString.stringToArray(original);
            String[] revisedLines = ToString.stringToArray(revised);
            UnifiedDiffBuilder builder = new UnifiedDiffBuilder(originalLines, revisedLines);
            Diff.diff(originalLines, revisedLines).accept(builder);
            return builder.getResult();
        } catch (DifferentiationFailedException e) {
            setError(e);
            return null;
        }
    }

    /**
     * Builds an in-line diff between two versions of a text.
     * 
     * @param original the original version
     * @param revised the revised version
     * @return the list of in-line diff words
     */
    public List<InlineDiffWord> inline(String original, String revised)
    {
        setError(null);

        try {
            Character[] originalChars = ArrayUtils.toObject(original.toCharArray());
            Character[] revisedChars = ArrayUtils.toObject(revised.toCharArray());
            InlineDiffBuilder builder = new InlineDiffBuilder(originalChars);
            Diff.diff(originalChars, revisedChars).accept(builder);
            return builder.getResult();
        } catch (DifferentiationFailedException e) {
            setError(e);
            return null;
        }
    }

    /**
     * Builds an extended diff between two versions of a text. The extended diff is a mix between a unified diff and an
     * in-line diff: it provides information about both line-level and character-level changes (the later only when a
     * line is modified).
     * 
     * @param original the original version
     * @param revised the revised version
     * @return the list of extended diff blocks
     */
    public List<UnifiedDiffBlock> extended(String original, String revised)
    {
        setError(null);

        try {
            String[] originalLines = ToString.stringToArray(original);
            String[] revisedLines = ToString.stringToArray(revised);
            ExtendedDiffBuilder builder = new ExtendedDiffBuilder(originalLines, revisedLines, new MyersDiff());
            Diff.diff(originalLines, revisedLines).accept(builder);
            return builder.getResult();
        } catch (DifferentiationFailedException e) {
            setError(e);
            return null;
        }
    }

    /**
     * Get the error generated while performing the previously called action.
     * 
     * @return an eventual exception or {@code null} if no exception was thrown
     */
    public Exception getLastError()
    {
        return (Exception) this.execution.getContext().getProperty(DIFF_ERROR_KEY);
    }

    /**
     * Store a caught exception in the context, so that it can be later retrieved using {@link #getLastError()}.
     * 
     * @param e the exception to store, can be {@code null} to clear the previously stored exception
     * @see #getLastError()
     */
    private void setError(Exception e)
    {
        this.execution.getContext().setProperty(DIFF_ERROR_KEY, e);
    }
}
