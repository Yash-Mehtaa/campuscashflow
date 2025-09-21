package com.yash.campuscashflow;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class TransactionsView extends BorderPane {

    private final TableView<TxDao.Tx> table = new TableView<>();
    private final Label startingAfterRecurringLbl = new Label("—");
    private final Label monthNetTxLbl = new Label("$0.00");
    private final Label remainingVsCapLbl = new Label("—");

    private YearMonth currentMonth = YearMonth.now();

    public TransactionsView() {
        setPadding(new Insets(16));
        setTop(makeHeader());
        setCenter(makeTable());
        setBottom(makeAddForm());
        refresh();
    }

    private HBox makeHeader() {
        var title = new Label("Transactions — " + currentMonth);
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        var grid = new GridPane();
        grid.setHgap(16); grid.setVgap(6);
        grid.add(new Label("Starting balance after recurring:"), 0, 0);
        grid.add(startingAfterRecurringLbl, 1, 0);
        grid.add(new Label("This month net (income − expenses):"), 0, 1);
        grid.add(monthNetTxLbl, 1, 1);
        grid.add(new Label("Remaining vs budget cap:"), 0, 2);
        grid.add(remainingVsCapLbl, 1, 2);

        var box = new HBox(24, title, new Separator(), grid);
        box.setSpacing(24);
        return box;
    }

    /** FULL version with Delete column */
    private TableView<TxDao.Tx> makeTable() {
        var dateCol = new TableColumn<TxDao.Tx, String>("Date");
        dateCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().date.toString()));

        var descCol = new TableColumn<TxDao.Tx, String>("Description");
        descCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().description));

        var amtCol = new TableColumn<TxDao.Tx, String>("Amount");
        amtCol.setCellValueFactory(c -> new SimpleStringProperty(Money.format(c.getValue().amountCents)));

        var delCol = new TableColumn<TxDao.Tx, String>("Delete");
        delCol.setCellFactory(col -> new TableCell<>() {
            final Button btn = new Button("✕");
            {
                btn.setOnAction(e -> {
                    var item = getTableView().getItems().get(getIndex());
                    if (item != null && item.id != null) {
                        TxDao.delete(item.id);
                        AppBus.notifyDataChanged(); // update Dashboard big number
                        refresh();                   // reload table and totals
                    }
                });
            }
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                setGraphic(empty ? null : btn);
            }
        });

        table.getColumns().addAll(dateCol, descCol, amtCol, delCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        return table;
    }

    private GridPane makeAddForm() {
        var title = new Label("Add transaction");
        title.setStyle("-fx-font-weight: bold;");

        var date = new DatePicker(LocalDate.now());
        var desc = new TextField(); desc.setPromptText("e.g., Groceries, Movie tickets, Book sale");
        var amt  = new TextField(); amt.setPromptText("Use negative for expense (e.g., -45.50)");

        var addBtn = new Button("Add");
        addBtn.setOnAction(e -> {
            try {
                var d = date.getValue();
                var description = desc.getText().trim();
                if (description.isEmpty()) { error("Enter a description"); return; }
                long cents = Math.round(Double.parseDouble(amt.getText().trim()) * 100.0);
                TxDao.insert(d, description, cents);
                desc.clear(); amt.clear();
                AppBus.notifyDataChanged(); // tell Dashboard to update big number
                refresh();
            } catch (Exception ex) {
                error("Enter a valid amount (e.g., -12.99 for expense, 50 for income)");
            }
        });

        var grid = new GridPane();
        grid.setHgap(8); grid.setVgap(8);
        grid.setPadding(new Insets(12, 0, 0, 0));
        grid.addRow(0, title);
        grid.addRow(1, new Label("Date"), date);
        grid.addRow(2, new Label("Description"), desc);
        grid.addRow(3, new Label("Amount ($)"), amt);
        grid.add(addBtn, 1, 4);
        return grid;
    }

    private void refresh() {
        // 1) Load month transactions into table
        List<TxDao.Tx> txs = TxDao.listByMonth(currentMonth);
        table.setItems(FXCollections.observableArrayList(txs));

        // 2) Compute recurring totals (monthly)
        long recurringExp = RecurringDao.monthlyTotal("expense");
        long recurringInc = RecurringDao.monthlyTotal("income");

        // 3) Starting balance after recurring = cap - recurringExp + recurringInc
        Long cap = BudgetDao.findCapCents(currentMonth);
        if (cap == null) {
            startingAfterRecurringLbl.setText("Set a budget cap in Dashboard");
            remainingVsCapLbl.setText("—");
        } else {
            long startAfterRecurring = cap - recurringExp + recurringInc;
            startingAfterRecurringLbl.setText(Money.format(startAfterRecurring));

            // 4) Net of variable transactions this month
            long monthIncome = TxDao.monthlyIncome(currentMonth);
            long monthExpense = TxDao.monthlyExpense(currentMonth);
            long monthNet = monthIncome - monthExpense; // positive if net income
            monthNetTxLbl.setText(Money.format(monthNet));

            // 5) Remaining vs cap after all
            long remaining = startAfterRecurring + monthNet;
            remainingVsCapLbl.setText(Money.format(remaining));
        }
    }

    private void error(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}
