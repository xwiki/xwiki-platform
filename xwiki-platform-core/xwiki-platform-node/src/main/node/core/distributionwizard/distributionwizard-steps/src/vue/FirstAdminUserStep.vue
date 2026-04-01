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
<script>
export function callback() {
  return false;
}
</script>

<template>
  <form id="first-user" method="post" class="xform">
    <dl>
      <dt><label for="register_first_name">First Name</label></dt>
      <dd><input type="text" id="register_first_name" name="first_name" /></dd>
    </dl>
    <dl>
      <dt><label for="register_last_name">Last Name</label></dt>
      <dd><input type="text" id="register_last_name" name="last_name" /></dd>
    </dl>
    <dl>
      <dt><label for="register_username">Username</label></dt>
      <dd>
        <input
          type="text"
          id="register_username"
          name="username"
          class="required"
        />
      </dd>
    </dl>
    <dl>
      <dt><label for="register_email">Email</label></dt>
      <dd><input type="text" id="register_email" name="email" /></dd>
    </dl>
    <dl>
      <dt><label for="register_password">Password</label></dt>
      <dd>
        <input
          type="password"
          id="register_password"
          name="password"
          class="required"
        />
      </dd>
    </dl>
    <dl>
      <dt><label for="register_password2">Password confirmation</label></dt>
      <dd>
        <input type="password" id="register_password2" name="password2" />
      </dd>
    </dl>
  </form>
</template>

<style scoped></style>
