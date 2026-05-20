package com.bomberman.bomberman.client.menu;

import com.bomberman.bomberman.shared.network.LobbyState;
import javafx.scene.control.Button;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Pre-game lobby UI shown after a successful connection.
 *
 * Displays connected players, their ready states, and available lobby actions.
 * Players can toggle Ready/Unready, while the host can start the match once
 * enough players have joined and everyone is ready.
 *
 * The screen is recreated whenever GameApp receives a new {@link LobbyState}.
 */
public class LobbyScreen {

    public static Parent create(
            LobbyState state,
            int myPlayerId,
            Runnable onReadyToggled,
            Runnable onStartClicked,
            Runnable onLeaveClicked
    ) {
        Label title = new Label("LOBBY");
        title.setStyle("-fx-font-size: 36px; -fx-font-weight: bold;");

        VBox playersBox = new VBox(8);
        playersBox.setAlignment(Pos.CENTER);

        List<String> playerNames = state.getPlayerNames();
        List<Boolean> readyStates = state.getReadyStates();

        long readyCount = 0;
        for (int i = 0; i < playerNames.size(); i++) {
            if (isReadyForDisplay(state, readyStates, i)) {
                readyCount++;
            }
        }

        String readyText = playerNames.size() < 2
                ? "Waiting for players ..."
                : readyCount + "/" + playerNames.size() + " Ready";

        Label readyCounterLabel = new Label(readyText);
        readyCounterLabel.setStyle("-fx-font-size: 16px;");

        for (int i = 0; i < playerNames.size(); i++) {
            String playerName = playerNames.get(i);
            boolean ready = isReadyForDisplay(state, readyStates, i);
            String status = ready ? "Ready" : "Not ready";

            Label playerLabel = new Label(playerName + " (" + status + ")");
            playerLabel.setStyle("-fx-font-size: 16px;");
            playersBox.getChildren().add(playerLabel);
        }

        int myIndex = state.getPlayerIds().indexOf(myPlayerId);
        boolean amReady = myIndex >= 0 && readyStates.get(myIndex);

        Button leaveButton = new Button("Leave");
        leaveButton.setStyle("-fx-font-size: 18px; -fx-padding: 6 24;");
        leaveButton.setOnAction(event -> onLeaveClicked.run());

        HBox buttons;

        if (state.getHostPlayerId() == myPlayerId) {
            buttons = new HBox(20, leaveButton);
        } else {
            Button readyButton = new Button(amReady ? "Unready" : "Ready");
            readyButton.setStyle("-fx-font-size: 18px; -fx-padding: 6 24;");
            readyButton.setOnAction(event -> onReadyToggled.run());

            buttons = new HBox(20, readyButton, leaveButton);
        }

        buttons.setAlignment(Pos.CENTER);

        if (state.getHostPlayerId() == myPlayerId) {
            boolean enoughPlayers = playerNames.size() >= 2;
            boolean everyoneReady = areAllNonHostPlayersReady(state, readyStates);

            Button startButton = new Button("Start");
            startButton.setStyle("-fx-font-size: 18px; -fx-padding: 6 24;");
            startButton.setDisable(!enoughPlayers || !everyoneReady);
            startButton.setOnAction(event -> onStartClicked.run());
            buttons.getChildren().add(startButton);
        }

        VBox layout = new VBox(20, title, readyCounterLabel, playersBox, buttons);
        layout.setAlignment(Pos.CENTER);

        return layout;
    }

    // Helpers

    private static boolean isReadyForDisplay(LobbyState state, List<Boolean> readyStates, int index) {
        boolean isHost = state.getPlayerIds().get(index) == state.getHostPlayerId();
        return isHost || readyStates.get(index);
    }

    private static boolean areAllNonHostPlayersReady(LobbyState state, List<Boolean> readyStates) {
        for (int i = 0; i < readyStates.size(); i++) {
            int playerId = state.getPlayerIds().get(i);

            if (playerId != state.getHostPlayerId() && !readyStates.get(i)) {
                return false;
            }
        }
        return true;
    }
}