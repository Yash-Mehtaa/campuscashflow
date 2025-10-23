package com.yash.campuscashflow;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class TxDao {

    public static class Tx {
        public Long id;
        public LocalDate date;
        public String description;
        public long amountCents; // positive = income, negative = expense

        public Tx(Long id, LocalDate date, String description, long amountCents) {
            this.id = id;
            this.date = date;
            this.description = description;
            this.amountCents = amountCents;
        }
    }

    private static Tx map(ResultSet rs) throws SQLException {
        return new Tx(
            rs.getLong("id"),
            LocalDate.parse(rs.getString("date")),
            rs.getString("description"),
            rs.getLong("amount_cents")
        );
    }

    /** Insert a single transaction on a specific date. */
    public static void insert(LocalDate date, String description, long amountCents) {
        String sql = "INSERT INTO tx(date, description, amount_cents) VALUES(?,?,?)";
        try (var ps = Db.get().prepareStatement(sql)) {
            ps.setString(1, date.toString());
            ps.setString(2, description);
            ps.setLong(3, amountCents);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** List all transactions for a given month (YYYY-MM). */
    public static List<Tx> listByMonth(YearMonth ym) {
        String sql = "SELECT id,date,description,amount_cents FROM tx WHERE substr(date,1,7)=? ORDER BY date";
        try (var ps = Db.get().prepareStatement(sql)) {
            ps.setString(1, ym.toString());
            try (var rs = ps.executeQuery()) {
                var out = new ArrayList<Tx>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** Sum of positive amounts (income) for a month. */
    public static long monthlyIncome(YearMonth ym) {
        String sql = "SELECT COALESCE(SUM(amount_cents),0) FROM tx WHERE substr(date,1,7)=? AND amount_cents>0";
        try (var ps = Db.get().prepareStatement(sql)) {
            ps.setString(1, ym.toString());
            try (var rs = ps.executeQuery()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** Sum of expenses (as positive number) for a month. */
    public static long monthlyExpense(YearMonth ym) {
        String sql = "SELECT COALESCE(SUM(-amount_cents),0) FROM tx WHERE substr(date,1,7)=? AND amount_cents<0";
        try (var ps = Db.get().prepareStatement(sql)) {
            ps.setString(1, ym.toString());
            try (var rs = ps.executeQuery()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** Delete a transaction by its ID. */
    public static void delete(long id) {
        String sql = "DELETE FROM tx WHERE id=?";
        try (var ps = Db.get().prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
