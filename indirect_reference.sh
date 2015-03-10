#!/bin/sh

# Advanced shell scripting from http://tldp.org/LDP/abs/html/abs-guide.html

# Indirect variable referencing.
# This has a few of the attributes of references in C++.


a=letter_of_alphabet
letter_of_alphabet=z

echo "a = $a"           # Direct reference.

echo "Now a = ${!a}"    # Indirect reference.
