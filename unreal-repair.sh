#!/bin/bash
java -Xmx8g -Djava.library.path=/usr/local/lib -cp bin/.:lib/*:lib/ejml/* main.Main "$@"