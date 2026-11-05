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
package org.xwiki.test.page.accessibility;

import java.util.List;

/**
 * The outcome of an axe-core rule on the validated page: violated, or incomplete when jsdom cannot resolve it.
 *
 * @param id the id of the rule, e.g. {@code image-alt}
 * @param impact the seriousness of the outcome, e.g. {@code critical}
 * @param help a short human readable description of the rule
 * @param description a longer human readable description of the rule
 * @param helpUrl the URL of the documentation of the rule
 * @param tags the tags of the rule, e.g. {@code wcag2a}
 * @param nodes the elements concerned by the rule
 * @version $Id$
 * @since 18.10.0RC1
 */
record AxeRuleResult(String id, String impact, String help, String description, String helpUrl,
    List<String> tags, List<AxeNodeResult> nodes)
{
}
