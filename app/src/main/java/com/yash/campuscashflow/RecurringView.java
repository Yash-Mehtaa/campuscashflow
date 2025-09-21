package com.yash.campuscashflow;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class RecurringView extends BorderPane {

    private final TableView<RecurringDao.Recurring> table = new TableView<>();
    private final Label monthlyExpenseLbl = new Label("$0.00");
    private final Label monthlyIncomeLbl  = new Label("$0.00");
    private final Label capAfterRecurring = new Label("—");

    public RecurringView() {
        setPadding(new Insets(16));
        setTop(makeHeader());
        setCenter(makeTable());
        setBottom(makeAddForm());
        refresh();
    }

    private VBox makeHeader() {
        var t = new Label("Recurring (stored as monthly; enter any frequency)");
        t.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        var grid = new GridPane();
        grid.setHgap(16); grid.setVgap(6);
        grid.add(new Label("Monthly Expense Total:"), 0, 0);
        grid.add(monthlyExpenseLbl, 1, 0);
        grid.add(new Label("Monthly Income Total:"), 0, 1);
        grid.add(monthlyIncomeLbl, 1, 1);
        grid.add(new Label("Budget Cap Left After Recurring:"), 0, 2);
        grid.add(capAfterRecurring, 1, 2);

        return new VBox(8, t, new Separator(), grid, new Separator());
    }

    private VBox makeTable() {
        var nameCol = new TableColumn<RecurringDao.Recurring, String>("Name");
        nameCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().name));

        var kindCol = new TableColumn<RecurringDao.Recurring, String>("Kind");
        kindCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().kind));

        var amtCol = new TableColumn<RecurringDao.Recurring, String>("Monthly Amount");
        amtCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(Money.format(c.getValue().amountCents)));

        var delCol = new TableColumn<RecurringDao.Recurring, String>("Delete");
        delCol.setCellFactory(col -> new TableCell<>() {
            final Button btn = new Button("✕");
            { btn.setOnAction(e -> {
                var item = getTableView().getItems().get(getIndex());
                if (item != null && item.id != null) {
                    RecurringDao.delete(item.id);
                    AppBus.notifyDataChanged();
                    refresh();
                }
            });}
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                setGraphic(empty ? null : btn);
            }
        });

        table.getColumns().addAll(nameCol, kindCol, amtCol, delCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        return new VBox(table);
    }

    private VBox makeAddForm() {
        var title = new Label("Add recurring");
        title.setStyle("-fx-font-weight: bold;");

        var name = new TextField(); name.setPromptText("e.g., Tuition, Rent, Scholarship, Campus Job");
        var kind = new ComboBox<String>(FXCollections.observableArrayList("expense", "income"));
        kind.getSelectionModel().selectFirst();
        var freq = new ComboBox<String>(FXCollections.observableArrayList("Yearly", "Monthly", "Weekly", "Biweekly", "Daily"));
        freq.getSelectionModel().select("Yearly");
        var amount = new TextField(); amount.setPromptText("Amount for that frequency");

        var addBtn = new Button("Add");
        addBtn.setOnAction(e -> {
            try {
                var nm = name.getText().trim();
                var kd = kind.getSelectionModel().getSelectedItem();
                var fq = freq.getSelectionModel().getSelectedItem();
                if (nm.isEmpty()) { error("Enter a name"); return; }

                long baseCents = Money.dollarsToCents(amount.getText());
                long monthlyCents = convertToMonthlyCents(baseCents, fq);

                RecurringDao.insertMonthly(nm, kd, monthlyCents);
                name.clear(); amount.clear();
                AppBus.notifyDataChanged();
                refresh();
            } catch (Exception ex) {
                error("Enter a valid amount (e.g., 12000 or 12000.00)");
            }
        });

        var form = new GridPane();
        form.setHgap(8); form.setVgap(8);
        form.addRow(0, new Label("Name"), name);
        form.addRow(1, new Label("Kind"), kind);
        form.addRow(2, new Label("Frequency"), freq);
        form.addRow(3, new Label("Amount ($)"), amount);
        form.add(addBtn, 1, 4);

        var tip = new Label("""
                Tip: Choose the frequency that matches your amount.
                • Yearly → divided by 12
                • Monthly → stored as-is
                • Weekly → × 52 / 12
                • Biweekly → × 26 / 12
                • Daily → × 365 / 12
                """);
        tip.setStyle("-fx-text-fill: #666;");

        var box = new VBox(8, new Separator(), title, form, tip);
        box.setPadding(new Insets(8,0,0,0));
        return box;
    }

    private long convertToMonthlyCents(long baseCents, String freq) {
        return switch (freq) {
            case "Yearly"   -> Math.round(baseCents / 12.0);
            case "Monthly"  -> baseCents;
            case "Weekly"   -> Math.round(baseCents * (52.0 / 12.0));
            case "Biweekly" -> Math.round(baseCents * (26.0 / 12.0));
            case "Daily"    -> Math.round(baseCents * (365.0 / 12.0));
            default         -> baseCents;
        };
    }

    private void refresh() {
        var items = RecurringDao.listAll();
        table.setItems(FXCollections.observableArrayList(items));

        long exp = RecurringDao.monthlyTotal("expense");
        long inc = RecurringDao.monthlyTotal("income");
        monthlyExpenseLbl.setText(Money.format(exp));
        monthlyIncomeLbl.setText(Money.format(inc));

        var cap = BudgetDao.findCapCents(java.time.YearMonth.now());
        if (cap == null) capAfterRecurring.setText("Set a budget cap in Dashboard");
        else capAfterRecurring.setText(Money.format(cap - exp + inc));
    }

    private void error(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}
