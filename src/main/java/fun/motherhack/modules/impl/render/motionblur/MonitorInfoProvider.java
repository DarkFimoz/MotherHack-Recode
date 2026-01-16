package fun.motherhack.modules.impl.render.motionblur;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;

public class MonitorInfoProvider {
    private static long lastMonitorHandle = 0;
    private static int lastRefreshRate = 60;
    private static long lastCheckTime = 0;
    private static final long CHECK_INTERVAL_NS = 1_000_000_000L;

    public static void updateDisplayInfo() {
        long now = System.nanoTime();
        if (now - lastCheckTime < CHECK_INTERVAL_NS) {
            return;
        }
        lastCheckTime = now;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) return;

        long window = client.getWindow().getHandle();
        long monitor = GLFW.glfwGetWindowMonitor(window);

        if (monitor == 0) {
            monitor = getMonitorFromWindowPosition(window, client.getWindow().getWidth(), client.getWindow().getHeight());
        }

        if (monitor != lastMonitorHandle) {
            lastRefreshRate = detectRefreshRateFromMonitor(monitor);
            lastMonitorHandle = monitor;
        }
    }

    public static int getRefreshRate() {
        updateDisplayInfo();
        return lastRefreshRate;
    }

    private static long getMonitorFromWindowPosition(long window, int windowWidth, int windowHeight) {
        int[] winX = new int[1];
        int[] winY = new int[1];
        GLFW.glfwGetWindowPos(window, winX, winY);

        int windowCenterX = winX[0] + windowWidth / 2;
        int windowCenterY = winY[0] + windowHeight / 2;

        long monitorResult = GLFW.glfwGetPrimaryMonitor();
        PointerBuffer monitors = GLFW.glfwGetMonitors();

        if (monitors != null) {
            for (int i = 0; i < monitors.limit(); i++) {
                long m = monitors.get(i);
                int[] mx = new int[1];
                int[] my = new int[1];
                GLFW.glfwGetMonitorPos(m, mx, my);

                GLFWVidMode mode = GLFW.glfwGetVideoMode(m);
                if (mode == null) continue;

                int mw = mode.width();
                int mh = mode.height();

                if (windowCenterX >= mx[0] && windowCenterX < mx[0] + mw &&
                    windowCenterY >= my[0] && windowCenterY < my[0] + mh) {
                    monitorResult = m;
                    break;
                }
            }
        }
        return monitorResult;
    }

    private static int detectRefreshRateFromMonitor(long monitor) {
        GLFWVidMode vidMode = GLFW.glfwGetVideoMode(monitor);
        return (vidMode != null) ? vidMode.refreshRate() : 60;
    }
}
