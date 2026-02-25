package com.budgetpilot.ui.pages;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.core.PageId;
import com.budgetpilot.model.UserProfile;
import com.budgetpilot.service.auth.AuthService;
import com.budgetpilot.util.ValidationUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.io.InputStream;

public class LoginPage extends VBox {
    private final AppContext appContext;
    private final AuthService authService;

    private final Label errorBanner = new Label();
    private final TextField emailField = new TextField();
    private final PasswordField passwordField = new PasswordField();

    public LoginPage(AppContext appContext) {
        this.appContext = ValidationUtils.requireNonNull(appContext, "appContext");
        this.authService = new AuthService(ValidationUtils.requireNonNull(appContext.getStore(), "store"));

        setSpacing(16);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(32));
        getStyleClass().addAll("page-root", "page-login");

        Region topSpacer = new Region();
        Region bottomSpacer = new Region();
        VBox.setVgrow(topSpacer, Priority.ALWAYS);
        VBox.setVgrow(bottomSpacer, Priority.ALWAYS);

        VBox card = new VBox(14);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(430);
        card.getStyleClass().addAll("card", "login-card");
        card.setPadding(new Insets(24));

        Node logoNode = buildLogoNode();
        Label title = new Label("BudgetPilot");
        title.getStyleClass().add("login-title");
        Label subtitle = new Label("Sign in to your financial cockpit");
        subtitle.getStyleClass().add("login-subtitle");

        errorBanner.getStyleClass().addAll("error-banner", "login-error-banner");
        errorBanner.setManaged(false);
        errorBanner.setVisible(false);
        errorBanner.setWrapText(true);

        emailField.setPromptText("Email");
        emailField.getStyleClass().addAll("text-input", "form-input");
        UserProfile user = appContext.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            emailField.setText(user.getEmail());
        }

        passwordField.setPromptText("Password");
        passwordField.getStyleClass().addAll("text-input", "form-input");
        passwordField.setOnAction(event -> attemptSignIn());

        Button signInButton = new Button("Sign In");
        signInButton.getStyleClass().addAll("quick-add-button", "btn-primary");
        signInButton.setMaxWidth(Double.MAX_VALUE);
        signInButton.setOnAction(event -> attemptSignIn());

        card.getChildren().addAll(
                logoNode,
                title,
                subtitle,
                errorBanner,
                emailField,
                passwordField,
                signInButton
        );

        getChildren().addAll(topSpacer, card, bottomSpacer);
    }

    private void attemptSignIn() {
        hideError();
        try {
            String email = ValidationUtils.requireValidEmail(emailField.getText(), "email");
            String password = ValidationUtils.requireNonBlank(passwordField.getText(), "password");
            UserProfile profile = authService.authenticate(email, password.toCharArray());
            appContext.setCurrentUser(profile);
            appContext.signIn(profile.getId());
            appContext.navigate(PageId.DASHBOARD);
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private Node buildLogoNode() {
        for (String path : new String[]{"/images/budgetpilot-logo.png", "/images/BudgetPilot.ico"}) {
            try (InputStream stream = LoginPage.class.getResourceAsStream(path)) {
                if (stream == null) {
                    continue;
                }
                Image image = new Image(stream);
                if (image.isError()) {
                    continue;
                }
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(88);
                imageView.setFitHeight(88);
                imageView.setPreserveRatio(true);
                imageView.getStyleClass().add("login-logo");
                return imageView;
            } catch (Exception ignored) {
            }
        }

        Circle circle = new Circle(44);
        circle.getStyleClass().add("login-logo-fallback-circle");
        Label initials = new Label("BP");
        initials.getStyleClass().add("login-logo-fallback-text");
        StackPane fallback = new StackPane(circle, initials);
        fallback.getStyleClass().add("login-logo");
        return fallback;
    }

    private void showError(String message) {
        errorBanner.setText(message == null || message.isBlank() ? "Invalid email or password" : message);
        errorBanner.setVisible(true);
        errorBanner.setManaged(true);
    }

    private void hideError() {
        errorBanner.setText("");
        errorBanner.setVisible(false);
        errorBanner.setManaged(false);
    }
}
