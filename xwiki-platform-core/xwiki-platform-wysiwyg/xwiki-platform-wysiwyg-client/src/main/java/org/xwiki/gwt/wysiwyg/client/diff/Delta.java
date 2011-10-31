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
package org.xwiki.gwt.wysiwyg.client.diff;

import java.util.List;

/**
 * Holds a "delta" difference between to revisions of a text.
 * 
 * @version $Revision: 1.1 $ $Date: 2006/03/12 00:24:21 $
 * 
 * @author <a href="mailto:juanco@suigeneris.org">Juanco Anez</a>
 * @author <a href="mailto:bwm@hplb.hpl.hp.com">Brian McBride</a>
 * @see Diff
 * @see Chunk
 * @see Revision
 * 
 * modifications
 * 
 * 27 Apr 2003 bwm
 * 
 * Added getOriginal() and getRevised() accessor methods Added visitor pattern
 * accept() method
 */

public abstract class Delta extends ToString
{

    protected Chunk original;

    protected Chunk revised;

   
    /**
     * Returns a Delta that corresponds to the given chunks in the original and
     * revised text respectively.
     * 
     * @param orig
     *            the chunk in the original text.
     * @param rev
     *            the chunk in the revised text.
     */
    public static Delta newDelta(Chunk orig, Chunk rev)
    {
        Delta result;
        if ((orig.size()>0)&&(rev.size()>0))
            result = new ChangeDelta();
        else if ((orig.size()==0)&&(rev.size()==0))
            result = new ChangeDelta();
        else if ((orig.size()>0)&&(rev.size()==0))
            result = new DeleteDelta();
        else if ((orig.size()==0)&&(rev.size()>0))
                 result = new AddDelta();
        else
             return null;

        result.init(orig, rev);
        return result;
    }

    /**
     * Creates an uninitialized delta.
     */
    public Delta()
    {
    }

    /**
     * Creates a delta object with the given chunks from the original and
     * revised texts.
     */
    public  Delta(Chunk orig, Chunk rev)
    {
        init(orig, rev);
    }

    /**
     * Initializaes the delta with the given chunks from the original and
     * revised texts.
     */
    public void init(Chunk orig, Chunk rev)
    {
        original = orig;
        revised = rev;
    }

    /**
     * Verifies that this delta can be used to patch the given text.
     * 
     * @param target
     *            the text to patch.
     * @throws PatchFailedException
     *             if the patch cannot be applied.
     */
    public abstract void verify(List target) throws PatchFailedException;

    /**
     * Applies this delta as a patch to the given text.
     * 
     * @param target
     *            the text to patch.
     * @throws PatchFailedException
     *             if the patch cannot be applied.
     */
    public final void patch(List target) throws PatchFailedException
    {
        verify(target);
        try
        {
            applyTo(target);
        }
        catch (Exception e)
        {
            throw new PatchFailedException(e.getMessage());
        }
    }

    /**
     * Applies this delta as a patch to the given text.
     * 
     * @param target
     *            the text to patch.
     * @throws PatchFailedException
     *             if the patch cannot be applied.
     */
    public abstract void applyTo(List target);

    /**
     * Converts this delta into its Unix diff style string representation.
     * 
     * @param s
     *            a {@link StringBuffer StringBuffer} to which the string
     *            representation will be appended.
     */
    public void toString(StringBuffer s)
    {
        original.rangeString(s);
        s.append("x");
        revised.rangeString(s);
        s.append(Diff.NL);
        original.toString(s, "> ", "\n");
        s.append("---");
        s.append(Diff.NL);
        revised.toString(s, "< ", "\n");
    }

    /**
     * Converts this delta into its RCS style string representation.
     * 
     * @param s
     *            a {@link StringBuffer StringBuffer} to which the string
     *            representation will be appended.
     * @param EOL
     *            the string to use as line separator.
     */
    public abstract void toRCSString(StringBuffer s, String EOL);

    /**
     * Converts this delta into its RCS style string representation.
     * 
     * @param EOL
     *            the string to use as line separator.
     */
    public String toRCSString(String EOL)
    {
        StringBuffer s = new StringBuffer();
        toRCSString(s, EOL);
        return s.toString();
    }

    /**
     * Accessor method to return the chunk representing the original sequence of
     * items
     * 
     * @return the original sequence
     */
    public Chunk getOriginal()
    {
        return original;
    }

    /**
     * Accessor method to return the chunk representing the updated sequence of
     * items.
     * 
     * @return the updated sequence
     */
    public Chunk getRevised()
    {
        return revised;
    }

    /**
     * Accepts a visitor.
     * <p>
     * See the Visitor pattern in "Design Patterns" by the GOF4.
     * 
     * @param visitor
     *            The visitor.
     */
    public abstract void accept(RevisionVisitor visitor);
}
