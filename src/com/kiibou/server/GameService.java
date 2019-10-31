package com.kiibou.server;

import processing.data.JSONArray;
import processing.data.JSONObject;
import space.kiibou.data.Vec3;
import space.kiibou.net.server.JSONMessage;
import space.kiibou.net.server.Server;
import space.kiibou.net.server.Service;
import space.kiibou.net.server.service.ActionService;
import space.kiibou.reflect.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class GameService extends Service {
    @Inject
    public ActionService actionService;

    private HashMap<Long, GameState> gameStates;

    public GameService(Server server) {
        super(server);
        gameStates = new HashMap<>();
    }

    @Override
    public void initialize() {
        actionService.addActionCallback("reveal-tiles", this::revealTiles);
        actionService.addActionCallback("restart", this::restart);
        actionService.addActionCallback("flag-toggle", this::flagToggle);
    }

    void sendFlagToggle(final long handle, final int x, final int y) {
        final GameState gameState = getGameState(handle);

        actionService.sendActionToClient(handle, "toggle-flag", new JSONObject()
                .setInt("x", x)
                .setInt("y", y)
                .setBoolean("toggle", gameState.flagToggle(x, y))
        );
    }

    private void revealTiles(final JSONMessage message) {
        Objects.requireNonNull(message);

        final GameState gameState = getGameState(message.getConnectionHandle());

        final int x = message.getMessage().getInt("x");
        final int y = message.getMessage().getInt("y");

        final List<Vec3> revealed = gameState.reveal(x, y);
        sendRevealTiles(message.getConnectionHandle(), revealed);
    }

    private void restart(final JSONMessage message) {
        Objects.requireNonNull(message);

        final GameState gameState = getGameState(message.getConnectionHandle());

        gameState.setupVariables();
        actionService.sendActionToClient(message.getConnectionHandle(), "restart");
    }

    private void flagToggle(final JSONMessage message) {
        Objects.requireNonNull(message);

        final int x = message.getMessage().getInt("x");
        final int y = message.getMessage().getInt("y");

        sendFlagToggle(message.getConnectionHandle(), x, y);
    }

    private void sendRevealTiles(final long handle, final List<Vec3> revealed) {
        JSONObject message = new JSONObject();

        JSONArray revealedTiles = new JSONArray();

        for (Vec3 pos : revealed) {
            int x = pos.x;
            int y = pos.y;
            int type = pos.z;
            revealedTiles.append(new JSONObject().setInt("x", x).setInt("y", y).setInt("type", type));
        }

        message.setJSONArray("revealed-tiles", revealedTiles);

        actionService.sendActionToClient(handle, "reveal-tiles", message);
    }

    void sendWin(final long handle) {
        actionService.sendActionToClient(handle, "win");
    }

    void sendLoose(final long handle) {
        actionService.sendActionToClient(handle, "loose");
    }

    void sendTime(final long handle, final int time) {
        actionService.sendActionToClient(handle, "set-time", new JSONObject().setInt("time", time));
    }

    private GameState getGameState(final long handle) {
        if (!gameStates.containsKey(handle)) {
            final GameState gameState = new GameState(handle, 9, 9, 10, this);
            gameStates.put(handle, gameState);
        }

        return gameStates.get(handle);
    }

}
