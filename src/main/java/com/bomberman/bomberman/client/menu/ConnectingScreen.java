package com.bomberman.bomberman.client.menu;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Transitional screen shown between clicking Join (or Host) and the lobby
 * appearing. Typically visible for under 200ms on LAN — long enough that the
 * user sees "something is happening" but short enough to feel instant.
 *
 * The TCP connect, JoinRequest/JoinAccepted handshake, and first LobbyState
 * all happen during this screen. Once LobbyState arrives, GameApp swaps
 * the scene root to {@link LobbyScreen}.
 */
public class ConnectingScreen {

    public static Parent create(String host) {
        Label label = new Label("Connecting to " + host + "...");
        label.setStyle("-fx-font-size: 20px;");

        VBox layout = new VBox(label);
        layout.setAlignment(Pos.CENTER);
        return layout;
    }
}