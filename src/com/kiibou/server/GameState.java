package com.kiibou.server;

import com.kiibou.TileType;
import space.kiibou.data.Vec2;
import space.kiibou.data.Vec3;

import java.util.*;
import java.util.stream.Collectors;

import static com.kiibou.TileType.*;

public class GameState {
    private final GameService gameService;
    private long handle;
    private int width;
    private int height;
    private int bombs;
    private boolean[][] revealed;
    private boolean[][] flagged;
    private TileType[][] tiles;
    private Vec2[] bombTiles;
    private boolean gameRunning;
    private int revealedTiles;
    private Timer timer;
    private TimerTask timerTask;
    private int time;

    public GameState(final long handle, final int width, final int height, final int bombs, final GameService gameService) {
        this.handle = handle;
        setupVariables(width, height, bombs);
        timer = new Timer("Timer", true);
        this.gameService = Objects.requireNonNull(gameService);
    }

    void setupVariables() {
        setupVariables(width, height, bombs);
    }

    private void setupVariables(final int width, final int height, final int bombs) {
        this.width = width;
        this.height = height;
        this.bombs = bombs;

        if (gameRunning) {
            stopTimer();
        }

        revealed = new boolean[width][height];
        for (boolean[] value : revealed) {
            Arrays.fill(value, false);
        }

        flagged = new boolean[width][height];
        for (boolean[] booleans : flagged) {
            Arrays.fill(booleans, false);
        }

        tiles = new TileType[width][height];
        for (TileType[] tile : tiles) {
            Arrays.fill(tile, EMPTY);
        }

        // Find out where to put bombs
        final List<Vec2> emptyTiles = new ArrayList<>();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                emptyTiles.add(new Vec2(x, y));
            }
        }
        Collections.shuffle(emptyTiles);
        bombTiles = emptyTiles.subList(0, bombs).toArray(new Vec2[0]);
        for (Vec2 coordinate : bombTiles) {
            tiles[coordinate.x][coordinate.y] = BOMB;
        }

        // Set tiles around bombs to the correct number
        for (int x = 0; x < tiles.length; x++) {
            for (int y = 0; y < tiles[x].length; y++) {
                if (isBomb(x, y)) continue;
                int count = 0;

                for (int px = -1; px <= 1; px++) {
                    for (int py = -1; py <= 1; py++) {
                        if (isValidTile(x + px, y + py) && isBomb(x + px, y + py)) {
                            count++;
                        }
                    }
                }

                setTile(x, y, getTypeFromValue(count));
            }
        }

        gameRunning = false;
        revealedTiles = 0;
    }

    List<Vec3> reveal(final int x, final int y) {
        if (!gameRunning) setGameRunning(true);

        final List<Vec3> revealed = new ArrayList<>();

        if (isValidTile(x, y)) {
            if (getTile(x, y) == EMPTY) {
                for (int px = -1; px <= 1; px++) {
                    for (int py = -1; py <= 1; py++) {
                        if (revealTile(x + px, y + py, revealed)) {
                            revealed.addAll(reveal(x + px, y + py));
                        }
                    }
                }
            } else {
                if (isBomb(x, y)) {
                    setTile(x, y, RED_BOMB);
                    setGameRunning(false);
                    revealed.addAll(
                            Arrays.stream(bombTiles)
                                    .filter(vec -> !(vec.x == x && vec.y == y))
                                    .map(vec -> new Vec3(vec.x, vec.y, BOMB.getLookup()))
                                    .collect(Collectors.toList())
                    );

                    gameService.sendLoose(handle);
                    setGameRunning(false);
                }

                revealTile(x, y, revealed);
            }
        }

        if (revealedTiles == width * height - bombs && gameRunning) {
            gameService.sendWin(handle);
            Vec2[] notFlagged = Arrays.stream(bombTiles)
                    .filter(tile -> !isFlagged(tile.x, tile.y))
                    .toArray(Vec2[]::new);

            for (Vec2 tile : notFlagged) {
                gameService.sendFlagToggle(handle, tile.x, tile.y);
            }

            setGameRunning(false);
        }

        return revealed;
    }

    private boolean revealTile(final int x, final int y, final List<Vec3> revealed) {
        boolean val = false;

        if (isValidTile(x, y)) {
            if (isNotRevealed(x, y)) {
                if (getTile(x, y) == EMPTY) val = true;
                revealed.add(new Vec3(x, y, getTile(x, y).getLookup()));
                setRevealed(x, y, true);
                revealedTiles++;
            }
        }

        return val;
    }

    private TileType getTile(final int x, final int y) {
        return tiles[x][y];
    }

    private void setTile(final int x, final int y, final TileType tileType) {
        Objects.requireNonNull(tileType);

        tiles[x][y] = tileType;
    }

    private boolean isNotRevealed(final int x, final int y) {
        return !revealed[x][y];
    }

    private void setRevealed(final int x, final int y, final boolean r) {
        if (isFlagged(x, y)) {
            gameService.sendFlagToggle(handle, x, y);
        }

        revealed[x][y] = r;
    }

    private boolean isFlagged(final int x, final int y) {
        return flagged[x][y];
    }

    private boolean isBomb(final int x, final int y) {
        return getTile(x, y) == BOMB;
    }

    private boolean isValidTile(final int x, final int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    private void setGameRunning(final boolean running) {
        if (!gameRunning && running) {
            resetTimer();
            startTimer();
        } else if (gameRunning && !running) {
            stopTimer();
        }

        gameRunning = running;
    }

    boolean flagToggle(final int x, final int y) {
        if (!gameRunning) setGameRunning(true);
        if (isNotRevealed(x, y)) {
            flagged[x][y] = !flagged[x][y];
        }
        return flagged[x][y];
    }

    private void startTimer() {
        timerTask = new TimerTask() {
            @Override
            public void run() {
                sendTimeToClients();
                time++;
            }
        };
        timer.schedule(timerTask, 0, 1000);
    }

    private void stopTimer() {
        if (timerTask != null)
            timerTask.cancel();
    }

    private void resetTimer() {
        time = 0;
        sendTimeToClients();
    }

    private void sendTimeToClients() {
        gameService.sendTime(handle, time);
    }

}
