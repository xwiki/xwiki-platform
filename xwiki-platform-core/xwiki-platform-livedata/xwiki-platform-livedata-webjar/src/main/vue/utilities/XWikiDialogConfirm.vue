<!--
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
 -->

<template>
  <XWikiDialog
    :id="id"
    :title="title"
    :text="text"
    :buttons="[
      {
        id: undefined,
        text: cancelText,
      },
      {
        id: true,
        text: confirmText,
        variant: 'primary',
      },
    ]"
    :callback="callback"
  ><XWikiDialog>
</template>


<script>
import XWikiDialog, { dialogFactory } from "./XWikiDialog";

const XWikiDialogConfirm = {

  name: "XWikiDialogConfirm",

  components: {
    XWikiDialog,
  },

  props: {
    id: String,
    title: String,
    text: String,
    confirmText: {
      type: String,
      default: "Confirm",
    },
    cancelText: {
      type: String,
      default: "Cancel",
    },
    callback: Function,
  },

};
export default XWikiDialogConfirm;


/**
 * Display a confirm box using the XWikiDialogConfirm component
 * Act the same way than window.confirm
 * but is asyncronous and return a promise
 * @param {paramObject} paramObject An object containing the modal configuration
 * (that is the props to be passed to the created XWikiDialogConfirm component)
 * @returns {Promise}
 */
export const confirm = dialogFactory(XWikiDialogConfirm);

</script>


<style>

</style>
