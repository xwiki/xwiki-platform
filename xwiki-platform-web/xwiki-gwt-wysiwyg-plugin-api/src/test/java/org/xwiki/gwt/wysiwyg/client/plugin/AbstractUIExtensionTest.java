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
package org.xwiki.gwt.wysiwyg.client.plugin;

import com.google.gwt.user.client.Random;

/**
 * Unit tests for any concrete implementation of the {@link UIExtension} interface.
 * 
 * @version $Id$
 */
public abstract class AbstractUIExtensionTest extends PluginTestCase
{
    /**
     * @return A new instance of the concrete {@link UIExtension} being tested.
     */
    protected abstract UIExtension newUIExtension();

    /**
     * Tests the enabling and disabling of a feature.
     */
    public void testEnabled()
    {
        UIExtension uie = newUIExtension();

        String[] features = uie.getFeatures();
        assertTrue(features.length > 0);

        String feature = features[Random.nextInt(features.length)];

        uie.setEnabled(feature, true);
        assertTrue(uie.isEnabled(feature));

        uie.setEnabled(feature, false);
        assertFalse(uie.isEnabled(feature));
    }
}
