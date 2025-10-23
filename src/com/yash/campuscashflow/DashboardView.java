package com.yash.campuscashflow;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.YearMonth;

public class DashboardView extends VBox {

    private final Label capStatus = new Label("No cap set yet");
    private final TextField capField = new TextField();

    // BIG live number:
    private final Label remainingBig = new Label("—");

    // What-if:
    private final TextField priceField = new TextField();
    private final Label impactLbl = new Label();

    // Keep a reference to unsubscribe later (optional here)
    private final Runnable refreshListener = this::refreshAll;

    public DashboardView() {
        setSpacing(14);
        setPadding(new Insets(16));
        setStyle("-fx-font-size: 14;");

        // Title
        var title = new Label("Dashboard");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        // BIG Remaining card
        var bigCard = new VBox(6);
        var bigTitle = new Label("Remaining this month (after recurring + transactions)");
        bigTitle.setStyle("-fx-font-weight: bold;");
        remainingBig.setStyle("-fx-font-size: 42; -fx-font-weight: bold;");
        remainingBig.setMinWidth(400);
        bigCard.getChildren().addAll(bigTitle, remainingBig);
        bigCard.setPadding(new Insets(12));
        bigCard.setStyle("-fx-background-color: #eef6ff; -fx-background-radius: 14;");

        // Budget Cap row
        var capRow = new HBox(8);
        capField.setPromptText("e.g., 800");
        var saveBtn = new Button("Save Cap");
        saveBtn.setOnAction(e -> onSaveCap());
        capRow.getChildren().addAll(new Label("Budget Cap (this month) $"), capField, saveBtn);

        // current cap label
        var cards = new HBox(16);
        var capCard = makeCard("Cap Status", capStatus);
        cards.getChildren().addAll(capCard);

        // What-if
        var whatIfBox = new VBox(8);
        priceField.setPromptText("e.g., 99.99");
        var checkBtn = new Button("Check Impact");
        checkBtn.setOnAction(e -> onCheckImpact());
        whatIfBox.getChildren().addAll(new Label("Should I buy it?"), new HBox(8, new Label("$"), priceField, checkBtn), impactLbl);

        // Pre-fill cap
        var existing = BudgetDao.findCapCents(YearMonth.now());
        if (existing != null) {
            capField.setText(String.format("%.2f", existing / 100.0));
            capStatus.setText("Current cap: " + Money.format(existing));
        }

        getChildren().addAll(title, bigCard, new Separator(), cards, new Separator(), capRow, new Separator(), whatIfBox);

        // Subscribe to global "data changed" events (from Recurring/Transactions tabs)
        AppBus.subscribe(refreshListener);

        // Initial compute
        refreshAll();
    }

    private VBox makeCard(String title, Label value) {
        var box = new VBox(4);
        var t = new Label(title);
        t.setStyle("-fx-font-weight: bold;");
        box.getChildren().addAll(t, value);
        box.setPadding(new Insets(12));
        box.setStyle("-fx-background-color: #f4f4f4; -fx-background-radius: 12;");
        return box;
    }
private void onSaveCap() {
    try {
        String input = capField.getText().trim();
        if (input.isEmpty()) {
            showError("Please enter a number.");
            return;
        }

        // ✅ parse input as plain dollars (not cents)
        double dollars = Money.parseDollars(input);
        if (dollars <= 0) {
            showError("Please enter a valid dollar amount (e.g., 800 or 1200.50)");
            return;
        }

        // if your DB still expects cents, convert before saving
        long cents = Math.round(dollars * 100);
        BudgetDao.upsertCap(java.time.YearMonth.now(), cents);

        capStatus.setText("Current cap: " + String.format("$%,.2f", dollars));
        impactLbl.setText("");
        refreshAll();
        AppBus.notifyDataChanged();

    } catch (Exception e) {
        e.printStackTrace();
        showError("Something went wrong while saving the budget cap.");
    }
}

    private void onCheckImpact() {
        try {
            var ym = YearMonth.now();
            var cap = BudgetDao.findCapCents(ym);
            if (cap == null) { showError("Set a budget cap first."); return; }

            long price = Money.dollarsToCents(priceField.getText());
            long remainingAfterAll = computeRemainingThisMonth();
            long leftIfBuy = remainingAfterAll - price;
            boolean exceeds = leftIfBuy < 0;

            impactLbl.setText((exceeds ? "⚠️ Exceeds by " : "✅ Left after purchase: ") + Money.format(Math.abs(leftIfBuy)));
        } catch (Exception ex) {
            showError("Enter a valid price (e.g., 19.99)");
        }
    }

    private void refreshAll() {
        var ym = YearMonth.now();
        Long cap = BudgetDao.findCapCents(ym);
        if (cap == null) {
            remainingBig.setText("Set a budget cap");
            return;
        }
        // Update cap label if needed
        capStatus.setText("Current cap: " + Money.format(cap));

        long remaining = computeRemainingThisMonth();
        remainingBig.setText(Money.format(remaining));
    }

    /** cap - recurringExpense + recurringIncome + (monthIncome - monthExpense) */
    private long computeRemainingThisMonth() {
        var ym = YearMonth.now();
        Long cap = BudgetDao.findCapCents(ym);
        if (cap == null) return 0;

        long recurringExp = RecurringDao.monthlyTotal("expense");
        long recurringInc = RecurringDao.monthlyTotal("income");
        long monthIncome = TxDao.monthlyIncome(ym);
        long monthExpense = TxDao.monthlyExpense(ym);
        long monthNet = monthIncome - monthExpense;

        return cap - recurringExp + recurringInc + monthNet;
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}
