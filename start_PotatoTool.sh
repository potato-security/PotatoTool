#!/bin/bash
java  --module-path $JAVAFX --add-modules javafx.fxml,javafx.controls --add-exports java.xml/com.sun.org.apache.bcel.internal.classfile=ALL-UNNAMED -jar PotatoTool.jar