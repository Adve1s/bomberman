package com.bomberman.bomberman.shared.network;

/**
 * Common marker for everything that travels over the wire.
 * Concrete messages should implement either ClientToServerMessage or
 * ServerToClientMessage, not this directly.
 */
public interface NetworkMessage {}