package com.yash.campuscashflow;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        Db.init();

        var tabs = new TabPane();

        var dashTab = new Tab("Dashboard", new DashboardView());
        dashTab.setClosable(false);

        var recTab = new Tab("Recurring", new RecurringView());
        recTab.setClosable(false);

        var txTab = new Tab("Transactions", new TransactionsView());
        txTab.setClosable(false);

        tabs.getTabs().addAll(dashTab, recTab, txTab); 

        stage.setScene(new Scene(tabs, 1000, 650));
        stage.setTitle("CampusCashflow");
        stage.show();
    }

    public static void main(String[] args) { launch(args); }
}
