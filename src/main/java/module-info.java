module focusbuddy {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.sql;
    requires java.desktop;
    
    opens com.focusbuddy to javafx.fxml;
    opens com.focusbuddy.controllers to javafx.fxml;
    opens com.focusbuddy.models to javafx.base;
    opens com.focusbuddy.models.notes to javafx.base;
    opens com.focusbuddy.observers to javafx.base;
    
    exports com.focusbuddy;
    exports com.focusbuddy.controllers;
    exports com.focusbuddy.models;
    exports com.focusbuddy.models.notes;
    exports com.focusbuddy.services;
    exports com.focusbuddy.utils;
    exports com.focusbuddy.observers;
}
