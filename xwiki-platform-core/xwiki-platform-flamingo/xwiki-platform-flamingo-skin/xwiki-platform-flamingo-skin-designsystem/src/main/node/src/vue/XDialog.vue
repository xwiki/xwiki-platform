<!--
  See the NOTICE file distributed with this work for additional
  information regarding copyright ownership.

  This is free software; you can redistribute it and/or modify it
  under the terms of the GNU Lesser General Public License as
  published by the Free Software Foundation; either version 2.1 of
  the License, or (at your option) any later version.

  This software is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with this software; if not, write to the Free
  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  02110-1301 USA, or see the FSF site: http://www.fsf.org.
-->
<script setup lang="ts"></script>

<template>
  
    <div class="modal-dialog" role="document">
      <div class="modal-content">
        <div class="modal-header">
          <button type="button" class="close close-modal">
            <span aria-hidden="true">
              <span class="fa fa-times" aria-hidden="true"></span>
            </span></button>
          <div class="modal-title">
            <span class="fa fa-circle"></span> Modal Title
          </div>
        </div>
        <div class="modal-body">

modal content
        </div><!-- end modal body -->
        <div class="modal-footer">
          <button type="button" class="btn btn-default close-modal">
            Close 
          </button>
          <button type="button" class="btn btn-primary">
            Save changes
          </button>
        </div>
      </div><!-- /.modal-content -->
    </div><!-- /.modal-dialog -->
  
</template>

<style scoped></style>
