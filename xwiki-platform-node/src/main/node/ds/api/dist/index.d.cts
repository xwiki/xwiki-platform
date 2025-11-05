import { navigationTreePropsDefaults, NavigationTreeProps } from './XNavigationTree';
import { navigationTreeSelectPropsDefaults, NavigationTreeSelectProps } from './XNavigationTreeSelect';
import { AlertActions, AlertProps, AlterAction } from './XAlert';
import { AvatarProps } from './XAvatar';
import { BreadcrumbItem, BreadcrumbItems, BreadcrumbProps } from './XBreadcrumb';
import { BtnProps } from './XBtn';
import { CardProps } from './XCard';
import { CheckboxProps } from './XCheckbox';
import { DialogProps } from './XDialog';
import { DividerProps } from './XDivider';
import { FileInputModel, FileInputProps } from './XFileInput';
import { FormProps } from './XForm';
import { ImgProps } from './XImg';
import { LoadProps } from './XLoad';
import { MenuProps } from './XMenu';
import { MenuItemProps } from './XMenuItem';
import { MenuLabelProps } from './XMenuLabel';
import { SelectProps } from './XSelect';
import { TabProps } from './XTab';
import { TabGroupProps } from './XTabGroup';
import { TabPanelProps } from './XTabPanel';
import { TextFieldProps } from './XTextField';
import { DisplayableTreeNode, TreeProps } from './XTree';
import { ButtonHTMLAttributes, ComponentOptionsMixin, ComputedOptions, DefineComponent, FormHTMLAttributes, HTMLAttributes, ImgHTMLAttributes, InputHTMLAttributes, MethodOptions } from 'vue';
/**
 * {@link HTMLAttributes} must be a type for all components. Possibly a sub-type of the abstract component is expected
 * to mick an existing HTML element (e.g., {@link ButtonHTMLAttributes} from {@link AbstractElements.XBtn}).
 *
 * @since 0.14
 * @beta
 */
type AbstractElements = {
    XAlert: DefineComponent<AlertProps & HTMLAttributes, object, object, ComputedOptions, MethodOptions, ComponentOptionsMixin, ComponentOptionsMixin, {
        "update:modelValue": (open: boolean) => never;
    }>;
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
    XNavigationTreeSelect: DefineComponent<NavigationTreeSelectProps & HTMLAttributes>;
    XSelect: DefineComponent<SelectProps & HTMLAttributes>;
    XTab: DefineComponent<TabProps & HTMLAttributes>;
    XTabGroup: DefineComponent<HTMLAttributes & TabGroupProps>;
    XTabPanel: DefineComponent<TabPanelProps & HTMLAttributes>;
    XTextField: DefineComponent<TextFieldProps & ImgHTMLAttributes>;
    XTree: DefineComponent<TreeProps & HTMLAttributes>;
};
export type { AbstractElements, AlertActions, AlertProps, AlterAction, AvatarProps, BreadcrumbItem, BreadcrumbItems, BreadcrumbProps, BtnProps, CardProps, CheckboxProps, DialogProps, DisplayableTreeNode, DividerProps, FileInputModel, FileInputProps, FormProps, ImgProps, LoadProps, MenuItemProps, MenuLabelProps, MenuProps, NavigationTreeProps, NavigationTreeSelectProps, SelectProps, TabGroupProps, TabPanelProps, TabProps, TextFieldProps, TreeProps, };
export { navigationTreePropsDefaults, navigationTreeSelectPropsDefaults };
declare module "vue" {
    interface GlobalComponents extends AbstractElements {
    }
}
