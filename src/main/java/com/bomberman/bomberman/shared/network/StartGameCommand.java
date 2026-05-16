package com.bomberman.bomberman.shared.network;

/**
 * Sent by the host clicking "Start Game" in the lobby.
 * Server should validate that the sender is actually the host before honoring.
 */
public class StartGameCommand implements ClientToServerMessage {
}