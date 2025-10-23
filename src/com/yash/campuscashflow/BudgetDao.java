package com.yash.campuscashflow;

import java.sql.*;
import java.time.YearMonth;

public class BudgetDao {

    public static Long findCapCents(YearMonth month) {
        String sql = "SELECT cap_cents FROM budget WHERE month=?";
        try (var ps = Db.get().prepareStatement(sql)) {
            ps.setString(1, month.toString()); // "YYYY-MM"
            try (var rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void upsertCap(YearMonth month, long capCents) {
        String sql = "INSERT INTO budget(month, cap_cents) VALUES(?, ?) " +
                     "ON CONFLICT(month) DO UPDATE SET cap_cents=excluded.cap_cents";
        try (var ps = Db.get().prepareStatement(sql)) {
            ps.setString(1, month.toString());
            ps.setLong(2, capCents);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

