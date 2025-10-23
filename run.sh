#!/bin/bash
java \
--module-path /Users/work/Desktop/javafx-sdk-25.0.1/lib \
--add-modules javafx.controls,javafx.fxml \
-cp ".:sqlite-jdbc.jar:slf4j-api.jar:slf4j-simple.jar:CampusCashflow.jar" \
com.yash.campuscashflow.Main

