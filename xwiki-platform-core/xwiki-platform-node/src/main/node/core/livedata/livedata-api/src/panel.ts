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
 * Holds all the information required to describe a panel.
 * @since 18.2.0RC1
 * @beta
 */
export interface Panel {
  /**
   * the id of the panel, must be unique among all panels, also used as suffix of the class on the panel
   */
  id: string;
  /**
   * the name that shall be shown in the menu
   */
  name: string;
  /**
   * the title that shall be displayed in the title bar of the panel
   */
  title: string;
  /**
   * the name of the icon for the menu and the title of the panel
   */
  icon: string;
  /**
   * the Element that shall be attached to the extension panel's body, this should
   *   contain the main UI
   */
  container?: Element;
  /**
   * the component id of the panel, should be "LiveDataAdvancedPanelExtension" for
   *   extension panels
   */
  component: string;
  /**
   * the ordering number, panels are sorted by this number in ascending order
   */
  order: number;
}
