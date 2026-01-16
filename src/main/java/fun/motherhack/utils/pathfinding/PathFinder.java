package fun.motherhack.utils.pathfinding;

import fun.motherhack.utils.Wrapper;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class PathFinder implements Wrapper {

    public static List<BlockPos> findPath(BlockPos start, BlockPos goal) {
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fCost));
        Map<BlockPos, Node> allNodes = new HashMap<>();
        Set<BlockPos> closedSet = new HashSet<>();

        Node startNode = new Node(start, null, 0, heuristic(start, goal));
        openSet.add(startNode);
        allNodes.put(start, startNode);

        int maxIterations = 5000;
        int iterations = 0;

        while (!openSet.isEmpty() && iterations < maxIterations) {
            iterations++;
            Node current = openSet.poll();

            // Достигли цели по XZ (Y может отличаться)
            if (Math.abs(current.pos.getX() - goal.getX()) <= 1 && 
                Math.abs(current.pos.getZ() - goal.getZ()) <= 1) {
                return reconstructPath(current);
            }

            closedSet.add(current.pos);

            for (BlockPos neighbor : getNeighbors(current.pos)) {
                if (closedSet.contains(neighbor)) continue;
                if (!isWalkable(neighbor)) continue;

                double tentativeG = current.gCost + 1;
                Node neighborNode = allNodes.get(neighbor);

                if (neighborNode == null) {
                    neighborNode = new Node(neighbor, current, tentativeG, heuristic(neighbor, goal));
                    allNodes.put(neighbor, neighborNode);
                    openSet.add(neighborNode);
                } else if (tentativeG < neighborNode.gCost) {
                    openSet.remove(neighborNode);
                    neighborNode.parent = current;
                    neighborNode.gCost = tentativeG;
                    neighborNode.fCost = tentativeG + neighborNode.hCost;
                    openSet.add(neighborNode);
                }
            }
        }

        return Collections.emptyList();
    }

    private static List<BlockPos> reconstructPath(Node node) {
        List<BlockPos> path = new ArrayList<>();
        while (node != null) {
            path.add(node.pos);
            node = node.parent;
        }
        Collections.reverse(path);
        return path;
    }

    private static double heuristic(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getZ() - b.getZ());
    }

    private static List<BlockPos> getNeighbors(BlockPos pos) {
        List<BlockPos> neighbors = new ArrayList<>();
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        for (int[] dir : directions) {
            // На том же уровне
            neighbors.add(pos.add(dir[0], 0, dir[1]));
            // Вверх на 1 блок (прыжок)
            neighbors.add(pos.add(dir[0], 1, dir[1]));
            // Вниз на 1 блок (спуск)
            neighbors.add(pos.add(dir[0], -1, dir[1]));
        }
        return neighbors;
    }

    public static boolean isWalkable(BlockPos pos) {
        if (mc.world == null) return false;
        
        BlockState ground = mc.world.getBlockState(pos.down());
        BlockState feet = mc.world.getBlockState(pos);
        BlockState head = mc.world.getBlockState(pos.up());

        boolean solidGround = !ground.isAir() && ground.getBlock() != Blocks.LAVA;
        boolean feetClear = feet.isAir() || !feet.blocksMovement();
        boolean headClear = head.isAir() || !head.blocksMovement();

        return solidGround && feetClear && headClear;
    }

    private static class Node {
        BlockPos pos;
        Node parent;
        double gCost, hCost, fCost;

        Node(BlockPos pos, Node parent, double gCost, double hCost) {
            this.pos = pos;
            this.parent = parent;
            this.gCost = gCost;
            this.hCost = hCost;
            this.fCost = gCost + hCost;
        }
    }
}
