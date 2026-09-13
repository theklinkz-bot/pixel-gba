.syntax unified
.arm
.section .header,"ax"
.global _start
b _start
.space 188
_start:
ldr sp, =0x03007f00
bl main
1: b 1b
