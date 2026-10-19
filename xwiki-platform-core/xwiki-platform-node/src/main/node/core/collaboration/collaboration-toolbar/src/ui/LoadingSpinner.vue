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
<template>
  <span class="realtime-spinner"></span>
</template>

<style>
.realtime-spinner,
.realtime-spinner::before {
  display: inline-block;
  border-radius: 50%;
  border: 2px solid;
  width: 8px;
  height: 8px;
}

.realtime-spinner {
  position: relative;
  border-color: transparent;
  animation: realtime-spinner-rotate 3s linear infinite;
}

.realtime-spinner::before {
  content: "";
  box-sizing: content-box !important;
  position: absolute;
  top: -2px;
  left: -2px;
  clip-path: rect(6px 12px 12px 0);
  animation: realtime-spinner-rotate 1.5s cubic-bezier(0.77, 0, 0.175, 1)
    infinite;
}

@keyframes realtime-spinner-rotate {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
</style>
