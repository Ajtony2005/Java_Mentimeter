module hu.ppke.itk.nagyhazi {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires java.sql;
    requires com.google.gson;
    requires jbcrypt;

    opens hu.ppke.itk.nagyhazi to javafx.fxml;
    exports hu.ppke.itk.tonyo.backend;
    exports hu.ppke.itk.tonyo.frontend;
    exports hu.ppke.itk.tonyo.frontend.pages;
}