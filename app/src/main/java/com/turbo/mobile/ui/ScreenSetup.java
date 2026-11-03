package com.turbo.mobile.ui;

import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.ui.auth.AuthViewModel;

public class ScreenSetup {

    private static final SystemBarStyle DARK_ICONS = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT);

    public static void fitSystemBars(AppCompatActivity activity, View root) {
        EdgeToEdge.enable(activity, DARK_ICONS, DARK_ICONS);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets barsAndKeyboard = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            view.setPadding(barsAndKeyboard.left, barsAndKeyboard.top, barsAndKeyboard.right, barsAndKeyboard.bottom);
            return insets;
        });
    }

    public static void showInitials(AppCompatActivity activity, TextView avatar) {
        AppContainer container = ((TurboApp) activity.getApplication()).getContainer();
        AuthViewModel viewModel = new AuthViewModel(container.userRepository, container.sessionManager);
        avatar.setText(viewModel.getInitials());
    }
}
