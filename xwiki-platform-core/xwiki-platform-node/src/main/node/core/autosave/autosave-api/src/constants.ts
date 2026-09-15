/**
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

/**
 * The interval between two consecutive saves (when the content is modified). Using a slightly different value for
 * each client may help reduce the chances of conflicts.
 */
const SAVE_INTERVAL: number = 60000 + Math.random() * 6000;

/**
 * How long to wait after broadcasting the intention to save, before actually saving the content. This helps reduce
 * the chances of concurrent saves (which often lead to merge conflicts).
 */
const SAVE_DELAY: number = 1000;

export { SAVE_DELAY, SAVE_INTERVAL };
