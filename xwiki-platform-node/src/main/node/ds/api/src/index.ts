/**
 * See the LICENSE file distributed with this work for additional
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

import { navigationTreePropsDefaults } from "./XNavigationTree";
import { navigationTreeSelectPropsDefaults } from "./XNavigationTreeSelect";
import type { AlertActions, AlertProps, AlterAction } from "./XAlert";
import type { AvatarProps } from "./XAvatar";
import type {
  BreadcrumbItem,
  BreadcrumbItems,
  BreadcrumbProps,
} from "./XBreadcrumb";
import type { BtnProps } from "./XBtn";
import type { CardProps } from "./XCard";
import type { CheckboxProps } from "./XCheckbox";
import type { DialogProps } from "./XDialog";
import type { DividerProps } from "./XDivider";
import type { FileInputModel, FileInputProps } from "./XFileInput";
import type { FormProps } from "./XForm";
import type { ImgProps } from "./XImg";
import type { LoadProps } from "./XLoad";
import type { MenuProps } from "./XMenu";
import type { MenuItemProps } from "./XMenuItem";
import type { MenuLabelProps } from "./XMenuLabel";
import type { NavigationTreeProps } from "./XNavigationTree";
import type { NavigationTreeSelectProps } from "./XNavigationTreeSelect";
import type { SelectProps } from "./XSelect";
import type { TabProps } from "./XTab";
import type { TabGroupProps } from "./XTabGroup";
import type { TabPanelProps } from "./XTabPanel";
import type { TextFieldProps } from "./XTextField";
import type {
  ButtonHTMLAttributes,
  DefineComponent,
  FormHTMLAttributes,
  HTMLAttributes,
  ImgHTMLAttributes,
  InputHTMLAttributes,
} from "vue";

/**
 * {@link HTMLAttributes} must be a type for all components. Possibly a sub-type of the abstract component is expected
 * to mick an existing HTML element (e.g., {@link ButtonHTMLAttributes} from {@link AbstractElements.XBtn}).
 *
 * @since 0.14
 * @beta
 */
type AbstractElements = {
  XAlert: DefineComponent<AlertProps & HTMLAttributes>;
  XAvatar: DefineComponent<AvatarProps & HTMLAttributes>;
  XBtn: DefineComponent<BtnProps & ButtonHTMLAttributes>;
  XBreadcrumb: DefineComponent<BreadcrumbProps & HTMLAttributes>;
  XCard: DefineComponent<CardProps & HTMLAttributes>;
  XCheckbox: DefineComponent<CheckboxProps & HTMLAttributes>;
  XDialog: DefineComponent<DialogProps & HTMLAttributes>;
  XDivider: DefineComponent<DividerProps & HTMLAttributes>;
  XFileInput: DefineComponent<FileInputProps & InputHTMLAttributes>;
  XForm: DefineComponent<FormProps & FormHTMLAttributes>;
  XImg: DefineComponent<ImgProps & ImgHTMLAttributes>;
  XLoad: DefineComponent<LoadProps & HTMLAttributes>;
  XMenu: DefineComponent<MenuProps & HTMLAttributes>;
  XMenuItem: DefineComponent<MenuItemProps & HTMLAttributes>;
  XMenuLabel: DefineComponent<MenuLabelProps & HTMLAttributes>;
  XNavigationTree: DefineComponent<NavigationTreeProps & HTMLAttributes>;
  XNavigationTreeSelect: DefineComponent<
    NavigationTreeSelectProps & HTMLAttributes
  >;
  XSelect: DefineComponent<SelectProps & HTMLAttributes>;
  XTab: DefineComponent<TabProps & HTMLAttributes>;
  XTabGroup: DefineComponent<HTMLAttributes & TabGroupProps>;
  XTabPanel: DefineComponent<TabPanelProps & HTMLAttributes>;
  XTextField: DefineComponent<TextFieldProps & ImgHTMLAttributes>;
};

// Expose all the abstract components types. For instance: props, actions, model value.
export type {
  AbstractElements,
  AlertActions,
  AlertProps,
  AlterAction,
  AvatarProps,
  BreadcrumbItem,
  BreadcrumbItems,
  BreadcrumbProps,
  BtnProps,
  CardProps,
  CheckboxProps,
  DialogProps,
  DividerProps,
  FileInputModel,
  FileInputProps,
  FormProps,
  ImgProps,
  LoadProps,
  MenuItemProps,
  MenuLabelProps,
  MenuProps,
  NavigationTreeProps,
  NavigationTreeSelectProps,
  SelectProps,
  TabGroupProps,
  TabPanelProps,
  TabProps,
  TextFieldProps,
};

export { navigationTreePropsDefaults, navigationTreeSelectPropsDefaults };

// Inspired from what shoelace is doing to expose the types of their web components in Vue.
declare module "vue" {
  // eslint-disable-next-line @typescript-eslint/no-empty-object-type
  interface GlobalComponents extends AbstractElements {}
}
