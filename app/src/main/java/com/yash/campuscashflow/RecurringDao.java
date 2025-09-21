package com.yash.campuscashflow;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RecurringDao {

    public static class Recurring {
        public Long id;
        public String name;       // Tuition, Rent, Scholarship, Campus Job, etc.
        public String kind;       // "expense" or "income"
        public long amountCents;  // monthly amount (always positive)

        public Recurring(Long id, String name, String kind, long amountCents) {
            this.id = id;
            this.name = name;
            this.kind = kind;
            this.amountCents = amountCents;
        }
    }

    private static Recurring map(ResultSet rs) throws SQLException {
        return new Recurring(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("kind"),
            rs.getLong("amount_cents")
        );
    }

    /** Insert a monthly recurring item. Pass kind = "expense" or "income". */
    public static void insertMonthly(String name, String kind, long monthlyCents) {
        String sql = "INSERT INTO recurring(name, kind, amount_cents) VALUES(?,?,?)";
        try (var ps = Db.get().prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, kind);
            ps.setLong(3, monthlyCents);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** Return all recurring items (both income and expense). */
    public static List<Recurring> listAll() {
        String sql = "SELECT id,name,kind,amount_cents FROM recurring ORDER BY name";
        try (var st = Db.get().createStatement();
             var rs = st.executeQuery(sql)) {
            var out = new ArrayList<Recurring>();
            while (rs.next()) out.add(map(rs));
            return out;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** Sum of monthly amounts by kind ("expense" or "income"). */
    public static long monthlyTotal(String kind) {
        String sql = "SELECT COALESCE(SUM(amount_cents),0) FROM recurring WHERE kind=?";
        try (var ps = Db.get().prepareStatement(sql)) {
            ps.setString(1, kind);
            try (var rs = ps.executeQuery()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void delete(long id) {
        String sql = "DELETE FROM recurring WHERE id=?";
        try (var ps = Db.get().prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
