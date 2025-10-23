package com.yash.campuscashflow;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.io.FileInputStream;
import java.io.InputStream;

public final class Db {
    private static Connection conn;

    public static synchronized Connection get() {
        if (conn == null) {
            try {
                conn = DriverManager.getConnection("jdbc:sqlite:campus.db");
                try (var st = conn.createStatement()) { st.execute("PRAGMA foreign_keys = ON"); }
            } catch (SQLException e) {
                throw new RuntimeException("DB connect failed", e);
            }
        }
        return conn;
    }

    public static void init() {
        try {
InputStream schemaStream = new FileInputStream("schema.sql");
if (schemaStream == null) throw new RuntimeException("schema.sql not found");
String sql = new String(schemaStream.readAllBytes(), StandardCharsets.UTF_8);
            for (String stmt : sql.split(";\\s*\\n")) {
                String s = stmt.trim();
                if (!s.isEmpty()) try (var st = get().createStatement()) { st.execute(s); }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to init schema", e);
        }
    }
}
