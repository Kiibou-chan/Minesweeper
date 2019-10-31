package com.kiibou;

import com.kiibou.server.GameService;
import processing.data.JSONArray;
import processing.data.JSONObject;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.PJOGL;
import space.kiibou.GApplet;
import space.kiibou.net.NetUtils;
import space.kiibou.net.client.Client;
import space.kiibou.net.common.ActionDispatcher;
import space.kiibou.net.server.Server;

import java.util.Objects;

public class Minesweeper extends GApplet {
    private Map map;
    private Client client;
    private ActionDispatcher<JSONObject> dispatcher;

    public static void main(String[] args) {
        GApplet.main(Minesweeper.class);

        if (!NetUtils.checkServerListening("localhost", 8454, 200)) {
            Server.start(GameService.class).ifPresent((server) -> {
                System.out.println("Starting Server");

                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    server.destroy();
                    System.out.println("Stopped Server");
                }));
            });/**/

            /*Server.main(new String[]{
                    "--port=8454",
                    "--services=" + GameService.class.getCanonicalName()
            });/**/
        }
    }

    @Override
    public void settings() {
        size(800, 800, P2D);
//        fullScreen(P2D);
        PJOGL.setIcon("pictures/icon.png");
    }

    @Override
    public void setup() {
        surface.setTitle("Minesweeper");

        ((PGraphicsOpenGL) g).textureSampling(2);
        frameRate(60);

        map = new Map(this, 0, 0, 9, 9, 4, 10);
        registerGraphicsElement(map);

        dispatcher = new ActionDispatcher<JSONObject>() {
            @Override
            public void messageReceived(JSONObject obj) {
                Objects.requireNonNull(obj);

                if (obj.hasKey("action")) {
                    final String action = obj.getString("action");
                    dispatchAction(action, obj);
                }
            }
        };

        dispatcher.addActionCallback("set-time", this::setTime);
        dispatcher.addActionCallback("reveal-tiles", this::revealTiles);
        dispatcher.addActionCallback("win", o -> map.win());
        dispatcher.addActionCallback("loose", o -> map.loose());
        dispatcher.addActionCallback("restart", o -> map.restart());
        dispatcher.addActionCallback("toggle-flag", this::toggleFlag);

        client = new Client(
                this::onServerConnect,
                dispatcher::messageReceived,
                this::onServerDisconnect
        ).connect("localhost", 8454);
    }

    private void onServerConnect() {
        System.out.println("Connected to Server!");
    }

    private void onServerDisconnect() {
        System.out.println("Disconnected from Server!");
        exit();
    }

    @Override
    public void draw() {
        if (width != map.getWidth() || height != map.getHeight()) {
            surface.setSize(map.getWidth(), map.getHeight());
        }

        background(204);
    }

    public Client getClient() {
        return client;
    }

    private void revealTiles(JSONObject o) {
        final JSONArray revealedTiles = o.getJSONArray("revealed-tiles");
        for (int i = 0; i < revealedTiles.size(); i++) {
            final JSONObject data = revealedTiles.getJSONObject(i);
            final int x = data.getInt("x");
            final int y = data.getInt("y");
            final int type = data.getInt("type");
            map.revealTile(x, y, TileType.getTypeFromValue(type));
        }
    }

    private void setTime(JSONObject o) {
        map.getControlBar().getTimerDisplay().setValue(o.getInt("time"));
    }

    private void toggleFlag(JSONObject o) {
        final int x = o.getInt("x");
        final int y = o.getInt("y");
        final boolean flag = o.getBoolean("toggle");
        map.tileFlag(x, y, flag);
    }
}
