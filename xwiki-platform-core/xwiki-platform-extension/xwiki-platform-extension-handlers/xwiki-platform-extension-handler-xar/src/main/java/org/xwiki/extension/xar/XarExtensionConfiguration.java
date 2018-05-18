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
package org.xwiki.extension.xar;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Various configuration related to XAR extensions support.
 * 
 * @version $Id$
 * @since 10.5RC1
 */
@Role
@Unstable
public interface XarExtensionConfiguration
{
    /**
     * The type of protection to apply.
     * 
     * @version $Id$
     */
    enum DocumentProtection
    {
        /**
         * No protection at all.
         */
        NONE(false, false, false, false),

        /**
         * Everyone get a warning when trying to edit a protected document.
         */
        WARNING(true, false, false, false),

        /**
         * EDIT/DELETE right is denied for everyone except for admins who just get a warning.
         */
        DENY(true, true, false, false),

        /**
         * EDIT/DELETE right is denied for everyone including admins.
         */
        FORCEDDENY(true, true, true, false),

        /**
         * EDIT/DELETE right is denied for simple users except for simple admins who just get a warning.
         */
        DENYSIMPLE(true, true, false, true),

        /**
         * EDIT/DELETE right is denied for all simple users including simple admins.
         */
        FORCEDDENYSIMPLE(true, true, true, true);

        private final boolean warning;

        private final boolean deny;

        private final boolean forced;

        private final boolean simple;

        /**
         * @param warning true if a warning should be showed when editing a protected document
         * @param deny true if EDIT/DELETE rights should be denied
         * @param forced true if the deny should apply to admins
         * @param simple true if the deny should apply only to simple user and not advanced users
         */
        DocumentProtection(boolean warning, boolean deny, boolean forced, boolean simple)
        {
            this.warning = warning;
            this.deny = deny;
            this.forced = this.deny && forced;
            this.simple = simple;
        }

        /**
         * @return true if a warning should be showed when editing a protected document
         */
        public boolean isWarning()
        {
            return this.warning;
        }

        /**
         * @return true if EDIT/DELETE rights should be denied
         */
        public boolean isDeny()
        {
            return this.deny;
        }

        /**
         * @return true if the deny should apply to admins
         */
        public boolean isForced()
        {
            return this.forced;
        }

        /**
         * @return true if the deny should apply only to simple user and not advanced users
         */
        public boolean isSimple()
        {
            return this.simple;
        }
    }

    /**
     * @return the protection to apply
     */
    DocumentProtection getDocumentProtection();
}
