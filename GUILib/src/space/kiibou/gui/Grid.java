package space.kiibou.gui;

import space.kiibou.GApplet;

import java.util.Arrays;
import java.util.Iterator;

public class Grid<T extends GraphicsElement> extends GraphicsElement implements Iterable<T> {
    private final int cellsX;
    private final int cellsY;
    private final T[][] cells;

    public Grid(GApplet app, int x, int y, int cellsX, int cellsY, int scale) {
        super(app, x, y, 0, 0, scale);

        this.cellsX = cellsX;
        this.cellsY = cellsY;
        //noinspection unchecked
        cells = (T[][]) new GraphicsElement[cellsX][cellsY];
    }

    @Override
    protected void preInitImpl() {

    }

    @Override
    public void initImpl() {
        int[] colWidth = new int[cellsX];
        int[] rowHeight = new int[cellsY];

        for (int x = 0; x < cellsX; x++) {
            for (int y = 0; y < cellsY; y++) {
                T element = cells[x][y];

                if (element != null) {
                    colWidth[x] = Math.max(colWidth[x], element.getWidth());
                    rowHeight[y] = Math.max(rowHeight[y], element.getHeight());
                }
            }
        }

        int width = 0;
        int height = 0;
        for (int x = 0; x < cellsX; x++) {
            height = 0;
            for (int y = 0; y < cellsY; y++) {
                T element = cells[x][y];
                element.moveTo(getX() + width, getY() + height);
                height += rowHeight[y];
            }
            width += colWidth[x];
        }

        setWidth(width);
        setHeight(height);
    }

    @Override
    protected void postInitImpl() {

    }

    @Override
    protected void drawImpl() {

    }

    public void put(int x, int y, T element) {
        if (isValidCell(x, y)) {
            if (cells[x][y] != null) {
                int index = getChildren().indexOf(cells[x][y]);
                removeChild(index);
            }

            cells[x][y] = element;
            addChild(element);
        } else {
            throw new IndexOutOfBoundsException(String.format("passed x:%d, y:%d must be in range x:0-%d, y:0-%d", x, y, cellsX, cellsY));
        }
    }

    public T get(int x, int y) {
        if (isValidCell(x, y)) {
            return cells[x][y];
        } else {
            throw new IndexOutOfBoundsException(String.format("passed x:%d, y:%d must be in range x:0-%d, y:0-%d", x, y, cellsX, cellsY));
        }
    }

    public T remove(int x, int y) {
        if (isValidCell(x, y)) {
            if (cells[x][y] != null) {
                int index = getChildren().indexOf(cells[x][y]);
                cells[x][y] = null;
                //noinspection unchecked
                return (T) removeChild(index);
            } else {
                return null;
            }
        } else {
            throw new IndexOutOfBoundsException(String.format("passed x:%d, y:%d must be in range x:0-%d, y:0-%d", x, y, cellsX, cellsY));
        }
    }

    private boolean isValidCell(int x, int y) {
        return x >= 0 && x < cellsX && y >= 0 && y < cellsY;
    }

    @Override
    public Iterator<T> iterator() {
        return Arrays.stream(cells).flatMap(Arrays::stream).iterator();
    }
}
