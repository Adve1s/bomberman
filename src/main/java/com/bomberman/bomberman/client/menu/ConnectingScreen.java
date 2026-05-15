package com.bomberman.bomberman.client.menu;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Shown after Play is clicked, while the network handshake is in flight.
 *
 * TODO - For teammate
 * This is the slot for the lobby UI. The current content is a placeholder.
 * To convert this into a lobby, you'll typically want to:
 *
 *   1. Rename this class to {@code LobbyScreen} (and the file/package if you want).
 *   2. Extend {@link #create} with callbacks for the lobby actions, following
 *      the {@link MainMenu#create} template:
 *      <pre>
 *      create(String host,
 *             List&lt;String&gt; playerNames,
 *             boolean isHost,
 *             Runnable onReadyToggled,
 *             Runnable onStartClicked,
 *             Runnable onLeaveClicked)
 *      </pre>
 *   3. Wire up the {@code GameClient} side: add {@code setOnLobbyState} and
 *      {@code setOnJoinRejected} callbacks alongside the existing
 *      {@code setOnGameStarted}, register them from {@code GameApp}, and on
 *      each update rebuild this screen with the new player list.
 *   4. The transition out of this screen is already wired: when the server
 *      sends {@code GameStarted}, the existing {@code onGameStarted} callback
 *      in {@code GameApp} swaps to the game scene. No change needed there.
 *
 * On the server: remove the current auto-start logic (in {@code GameServer.handleConnected})
 * and only broadcast {@code GameStarted} when the host's {@code StartGameCommand}
 * arrives.
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