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
 *
 * <h2>For teammate A</h2>
 *
 * <h3>Goal</h3>
 * Build a lobby where connected players see each other, toggle their ready state,
 * and the host clicks Start to begin the game.
 *
 * <h3>Steps</h3>
 * <ol>
 *   <li>Extend {@link #create} to take the data and callbacks the lobby needs.
 *       Suggested signature:
 *       <pre>
 *       public static Parent create(
 *               LobbyState state,
 *               int myPlayerId,
 *               Runnable onReadyToggled,    // sends {@code new ReadyCommand(true/false)}
 *               Runnable onStartClicked,    // sends {@code new StartGameCommand()}, host only
 *               Runnable onLeaveClicked)    // disconnects and returns to MainMenu
 *       </pre>
 *       Use {@link LobbyState#getPlayerNames()} for the player list and
 *       {@link LobbyState#getHostPlayerId()} to decide whether to show the
 *       Start button (only the host sees it).</li>
 *
 *   <li>Add callbacks on {@link com.bomberman.bomberman.client.net.GameClient}
 *       for the messages this screen needs. Follow the
 *       {@code setOnGameStarted} template that's already there:
 *       <ul>
 *         <li>{@code setOnLobbyState(Consumer<LobbyState>)} — fires on every
 *             LobbyState broadcast (someone joined, left, toggled ready).</li>
 *         <li>{@code setOnJoinRejected(Consumer<String>)} — fires when the
 *             server rejects the join. Wire to {@code returnToMenu} in
 *             {@link com.bomberman.bomberman.client.app.GameApp} with the
 *             rejection reason as the error message.</li>
 *       </ul>
 *       Each one needs a {@code volatile} field, a setter, and a fire site in
 *       {@link com.bomberman.bomberman.client.net.GameClient#handleReceived}.
 *       The pattern is identical to the existing {@code onGameStartedCallback}.
 *       <br><br>
 *       <b>Threading:</b> callbacks fire on the network thread. Wrap UI work
 *       in {@code Platform.runLater}, same as the GameStarted handler in
 *       {@code GameApp.joinGame}.</li>
 *
 *   <li>In {@link com.bomberman.bomberman.client.app.GameApp#joinGame}, wire
 *       the new callbacks alongside the existing {@code setOnGameStarted}:
 *       <pre>
 *       client.setOnLobbyState(state -> Platform.runLater(() -> showLobby(state)));
 *       client.setOnJoinRejected(reason -> Platform.runLater(() ->
 *               returnToMenu(host, name, "Rejected: " + reason)));
 *       </pre>
 *       Where {@code showLobby(LobbyState)} swaps the scene root to
 *       {@code LobbyScreen.create(state, ...)}. The transition out of the
 *       lobby is already wired — {@code onGameStarted} swaps to the game
 *       scene when the server sends {@code GameStarted}.</li>
 *
 *   <li>On the server, remove the auto-start logic from
 *       {@link com.bomberman.bomberman.server.net.GameServer#handleConnected}.
 *       Replace it with proper handling for {@code StartGameCommand} that
 *       validates the sender is the host before broadcasting {@code GameStarted}.
 *       Also implement {@code ReadyCommand} handling and {@code LobbyState}
 *       broadcasting whenever lobby membership or ready states change.</li>
 * </ol>
 *
 * <h3>Worth thinking about</h3>
 * <ul>
 *   <li><b>Re-rendering on every LobbyState:</b> simplest approach is to call
 *       {@code scene.setRoot(LobbyScreen.create(newState, ...))} each time
 *       state changes. Cheap for ~4 players and a handful of controls. No
 *       need for reactive bindings.</li>
 *   <li><b>Timeouts on this screen:</b> if {@code JoinAccepted} or the first
 *       {@code LobbyState} never arrives (server bug, network drop), the user
 *       sits on ConnectingScreen forever. Worth a ~3-second timeout that
 *       returns to menu with a "Server didn't respond" message.</li>
 *   <li><b>Cancel button on ConnectingScreen:</b> if the user wants to bail
 *       during the TCP connect (e.g. they typed a host that resolves but
 *       isn't running a server, so the 5-second timeout drags), a Cancel
 *       button that calls {@code client.disconnect()} and returns to menu
 *       is a nice touch.</li>
 * </ul>
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