package byow.Core;

import byow.Core.TileEngine.TETile;
import byow.Core.TileEngine.Tileset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A more intelligent Hallway that finds optimal places to generate Hallways.
 * @author Jonathan Atkins, Jake Webster 11/18/20.
 */
public class Hallway  implements java.io.Serializable {
    /**
     * @param world: The grid that everything is generated on.
     * @param rooms: The List of RoomAdjs that exist in @param world.
     * @param halls: The list of HallwayObjs that exist in @param world.
     * @param obj: The UnionFind that is used to ensure a connected structure.
     * @param random: The random used for generation.
     */
    private TETile[][] world;
    private List<Room> rooms;
    private List<HallwayObj> halls;
    private UnionFind obj;
    private Random random;

    /**
     * @param w sets the world array of this class.
     * @param rms sets the rooms list of this class.
     * @param uf sets the UnionFind obj of this class.
     * @param rand sets the Random of this class.
     * Also initializes @param halls: There are no Hallways before we start generation.
     */
    public Hallway(TETile[][] w, List<Room> rms, UnionFind uf, Random rand) {
        this.world = w;
        this.rooms = rms;
        this.obj = uf;
        this.halls = new ArrayList<>();
        this.random = rand;
    }

    /**
     * Makes function calls to connect all the rooms.
     */
    public void connectAllRooms() {
        for (Room i: rooms) {
            for (Room j: rooms) {
                // If not already connected, go to connect.
                if (!obj.isConnected(i, j)) {
                    connectRoom(i, j);
                }
            }
        }
    }

    /**
     * Begins the process of connecting rooms @param i and @param j.
     * Attempts connecting 20 times; if it does not, then they cannot connect.
     */
    private void connectRoom(Room i, Room j) {
        for (int fails = 0; fails < 20; fails += 1) {
            Position a = findLoc(i);
            Position b = findLoc(j);
            if (connectPoint(a, b)) {
                break;
            }
        }
    }

    /**
     * @Returns the Position of @param i, if it is not a corner.
     */
    private Position findLoc(Room i) {
        Position returnV = null;
        while (returnV == null) {
            int x = RandomUtils.uniform(random, 0, i.getWallLocation().size() - 1);
            Position temp = i.getWallLocation().get(x);

            if (!i.getCornerLocation().contains(temp)) {
                returnV = temp;
            }
        }
        return returnV;
    }

    /**
     * Figure out the distance between @param a and @param b starting from @param a.
     * Then, use the width and length to work on generating a Hallway that connects them.
     * @Return whether a Hallway can be generated between the two.
     */
    private boolean connectPoint(Position a, Position b) {
        int widthOrg = Math.abs(b.getX() - a.getX());
        int width = Math.abs(widthOrg) + 1;

        int lengthOrg = Math.abs(b.getY() - a.getY());
        int length = Math.abs(lengthOrg) + 1;

        int a1;
        int a2;
        // Try vertical and then horizontal generation from a.
        HallwayObj firstPart1;
        if (a.getY() > b.getY()) {
            firstPart1 = makeVerticalHall(a, -length);
            a1 = -lengthOrg;
        } else {
            firstPart1 = makeVerticalHall(a, length);
            a1 = lengthOrg;
        }
        HallwayObj secondPart1;
        if (a.getX() > b.getX()) {
            secondPart1 = makeHorizontalHall(b, width);
        } else {
            secondPart1 = makeHorizontalHall(b, -width);
        }

        // Then, try horizontal and then vertical generation from a.
        HallwayObj firstPart2;
        if (a.getX() > b.getX()) {
            firstPart2 = makeHorizontalHall(a, -width);
            a2 = -widthOrg;
        } else {
            firstPart2 = makeHorizontalHall(a, width);
            a2 = widthOrg;
        }
        HallwayObj secondPart2;
        if (a.getY() > b.getY()) {
            secondPart2 = makeVerticalHall(b, length);

        } else {
            secondPart2 = makeVerticalHall(b, -length);
        }

        if ((firstPart1 == null || secondPart1 == null)
                && (firstPart2 == null || secondPart2 == null)) {
            return false;
        } else if (firstPart1 == null || secondPart1 == null) {
            addHall(firstPart2, world);
            addHall(secondPart2, world);
            Position intersection = new Position(a, a2, 0);
            addCorner(intersection);
            return true;
        } else if (firstPart2 == null || secondPart2 == null) {
            addHall(firstPart1, world);
            addHall(secondPart1, world);
            Position intersection = new Position(a, 0, a1);
            addCorner(intersection);
            return true;
        }
        return false;
    }

    /**
     * Add a corner at @param p in the most reasonable location.
     */
    private void addCorner(Position p) {
        Position upperRight = new Position(p, 1, 1);
        Position upperLeft = new Position(p, -1, 1);
        Position lowerRight = new Position(p, 1, -1);
        Position lowerLeft = new Position(p, -1, -1);
        if (getWorldTile(upperRight) == Tileset.NOTHING) {
            world[upperRight.getX()][upperRight.getY()] = Tileset.WALL;
        } else if (getWorldTile(upperLeft) == Tileset.NOTHING) {
            world[upperLeft.getX()][upperLeft.getY()] = Tileset.WALL;
        } else if (getWorldTile(lowerRight) == Tileset.NOTHING) {
            world[lowerRight.getX()][lowerRight.getY()] = Tileset.WALL;
        } else if (getWorldTile(lowerLeft) == Tileset.NOTHING) {
            world[lowerLeft.getX()][lowerLeft.getY()] = Tileset.WALL;
        }
    }


    /**
     * Makes a vertical hallway.
     * If it hits a wall before it reaches its desired length, still create the wall,
     * thus merging the Hallway with another Hallway or a Room, as long as the wall
     * is not a corner. If the hallway is built to its desired length, then it is a
     * dead end hallway.
     *
     * Starts the Hallway of @param length from Position @param p.
     * Orientation is determined by @param up: true = up, false = down.
     * @Return the HallwayObj generated in the process.
     */
    public HallwayObj makeVerticalHall(Position p, int length) {
        List<Position> floor = new ArrayList<>();
        List<Position> wall = new ArrayList<>();


        int absLength = Math.abs(length);
        for (int j = 0; j < absLength; j++) {
            int i;
            if (length > 0) {
                i = j;
            } else {
                i = -j;
            }

            Position pos = new Position(p, 0, i);
            Position wall1 = new Position(pos, -1, 0);
            Position wall2 = new Position(pos, 1, 0);

            // If the position is not in bounds, a Hallway cannot be generated.
            if (!Engine.inBounds(pos) || !Engine.inBounds(wall1) || !Engine.inBounds(wall2)) {
                return null;
            }

            // Generation should also fail if the Hallway is going to hit a corner.
            if (isCorner(pos, length, true)) {
                return null;
            }

            // If the second thing you put down for floor hits floor, do not make the first floor
            // a floor; it should become a wall in that case.
            if (j == 0 && secondPlacedFloorIsFloor(pos, length, true)) {
                wall.add(pos);
            } else {
                floor.add(pos);
            }

            wall.add(wall1);
            wall.add(wall2);

        }
        HallwayObj hall = new HallwayObj(floor, wall, absLength, 3);
        return hall;
    }

    /**
     * @param p: The Position that is being considered.
     * @param distance: The desired distance to place. It should be > 0.
     * @param vertical: Whether a vertical Hallway is being generated or not.
     * @return whether the next Position the Hallway is going to is already a floor.
     */
    private boolean secondPlacedFloorIsFloor(Position p, int distance, boolean vertical) {
        Position nextPos;
        if (vertical) {
            if (distance > 0) {
                nextPos = new Position(p, 0, 1);
            } else {
                nextPos = new Position(p, 0, -1);
            }
        } else {
            if (distance > 0) {
                nextPos = new Position(p, 1, 0);
            } else {
                nextPos = new Position(p, -1, 0);
            }
        }
        if (Engine.inBounds(nextPos)) {
            return getWorldTile(nextPos) == Tileset.FLOOR;
        }
        return false;
    }


    /**
     * Makes a horizontal hallway.
     * If it hits a wall before it reaches its desired length, still create the wall,
     * thus merging the Hallway with another Hallway or a Room, as long as the wall
     * is not a corner. If the hallway is built to its desired length, then it is a
     * dead end hallway.
     *
     * Starts the Hallway of @param length from Position @param p.
     * Orientation is determined by @param up: true = up, false = down.
     * @Return the HallwayObj generated in the process.
     */
    public HallwayObj makeHorizontalHall(Position p, int width) {
        List<Position> floor = new ArrayList<>();
        List<Position> wall = new ArrayList<>();

        int absWidth = Math.abs(width);
        for (int j = 0; j < absWidth; j++) {
            int i;
            if (width > 0) {
                i = j;
            } else {
                i = -j;
            }

            Position pos = new Position(p, i, 0);
            Position wall1 = new Position(pos, 0, -1);
            Position wall2 = new Position(pos, 0, 1);

            // If the position is not in bounds, a Hallway cannot be generated.
            if (!Engine.inBounds(pos) || !Engine.inBounds(wall1) || !Engine.inBounds(wall2)) {
                return null;
            }

            // Generation should also fail if the Hallway is going to hit a corner.
            if (isCorner(pos, width, false)) {
                return null;
            }

            // If the second thing you put down for floor hits floor, do not make the first floor
            // a floor; it should become a wall in that case.
            if (j == 0 && secondPlacedFloorIsFloor(pos, width, false)) {
                wall.add(pos);
            } else {
                floor.add(pos);
            }
            wall.add(wall1);
            wall.add(wall2);
        }
        HallwayObj hall = new HallwayObj(floor, wall, 3, width);
        return hall;
    }


    /**
     * @Return whether Position @param p is a corner looking within the world.
     * @param distance represents how much more Hallway is left; nothing happens if <=0.
     * @param vertical is true if this is a vertical Hallway, and false if it is horizontal.
     */
    private boolean isCorner(Position p, int distance, boolean vertical) {
        Position nextPos;
        Position nextNextPos;
        if (vertical) {
            if (distance > 0) {
                nextPos = new Position(p, 0, 1);
                nextNextPos = new Position(p, 0, 2);
            } else {
                nextPos = new Position(p, 0, -1);
                nextNextPos = new Position(p, 0, -2);
            }
        } else {
            if (distance > 0) {
                nextPos = new Position(p, 1, 0);
                nextNextPos = new Position(p, 2, 0);
            } else {
                nextPos = new Position(p, -1, 0);
                nextNextPos = new Position(p, -2, 0);
            }
        }
        boolean flag = Engine.inBounds(nextPos) && getWorldTile(p) == Tileset.WALL
                        && getWorldTile(nextPos) == Tileset.WALL;
        if (flag && Engine.inBounds(nextNextPos) && getWorldTile(nextNextPos) == Tileset.FLOOR) {
            return false;
        } else {
            return flag;
        }
    }

    /**
     * Adds HallwayObj @param h to @param wrld.
     */
    public void addHall(HallwayObj h, TETile[][] wrld) {
        if (h != null && h.getWall().size() > 0) {
            obj.addComponent(h);
            halls.add(h);

            List<Position> wallPositions = h.getWall();
            List<Position> floorPositions = h.getFloor();

            for (Position p: wallPositions) {
                if (getWorldTile(p) == Tileset.FLOOR) {
                    wrld[p.getX()][p.getY()] = Tileset.FLOOR;
                } else {
                    wrld[p.getX()][p.getY()] = Tileset.WALL;
                }
            }

            for (Position p: floorPositions) {
                // When you place a floor on a wall that is a connection from the Hallway
                // to the other component.
                if (getWorldTile(p) != Tileset.NOTHING) {
                    Object temp = whichComponent(p);
                    obj.connect(temp, h);
                }
                wrld[p.getX()][p.getY()] = Tileset.FLOOR;
            }
        }
    }

    /**
     * @Return the type that the component that contains @param p is, or
     * null if it is none of them.
     */
    private Object whichComponent(Position p) {
        for (Room i : rooms) {
            for (Position wall : i.getWallLocation()) {
                if (p.equals(wall)) {
                    return i;
                }
            }
        }
        for (Room i : rooms) {
            for (Position floor : i.getFloorLocation()) {
                if (p.equals(floor)) {
                    return i;
                }
            }
        }
        for (HallwayObj i : halls) {
            for (Position wall : i.getWall()) {
                if (p.equals(wall)) {
                    return i;
                }
            }
        }
        for (HallwayObj i : halls) {
            for (Position floor : i.getFloor()) {
                if (p.equals(floor)) {
                    return i;
                }
            }
        }
        return null;
    }

    /**
     * @Return the type of tile at Position @param i in the world.
     */
    private TETile getWorldTile(Position i) {
        return world[i.getX()][i.getY()];
    }
}
