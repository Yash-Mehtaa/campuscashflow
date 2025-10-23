#!/bin/bash
set -e
echo "🧹 cleaning..."
rm -rf bin campus.db
mkdir bin
sqlite3 campus.db < src/com/yash/campuscashflow/schema.sql

echo "⚙️ compiling..."
javac --module-path /Users/work/Desktop/javafx-sdk-25.0.1/lib \
--add-modules javafx.controls,javafx.fxml \
-d bin $(find src -name "*.java")

echo "🚀 running..."
java --module-path /Users/work/Desktop/javafx-sdk-25.0.1/lib \
--add-modules javafx.controls,javafx.fxml \
-cp "bin:sqlite-jdbc.jar:slf4j-api.jar:slf4j-simple.jar" \
com.yash.campuscashflow.Main

