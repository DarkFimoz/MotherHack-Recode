package fun.motherhack.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Icons;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Manages the window icon for the Minecraft client.
 * Allows switching between custom and default icons.
 */
public class WindowIconManager {
    
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static boolean customIconSet = false;
    
    /**
     * Sets a custom icon for the window.
     * Attempts to load from motherhack assets, falls back to default if not found.
     */
    public static void setCustomIcon() {
        if (mc.getWindow() == null) {
            return;
        }
        
        try {
            InputStream iconStream = WindowIconManager.class.getResourceAsStream("/assets/motherhack/icon.png");
            if (iconStream != null) {
                BufferedImage image = ImageIO.read(iconStream);
                setWindowIcon(image);
                customIconSet = true;
                iconStream.close();
            } else {
                restoreDefaultIcon();
            }
        } catch (IOException e) {
            restoreDefaultIcon();
        }
    }
    
    /**
     * Restores the default Minecraft icon.
     */
    public static void restoreDefaultIcon() {
        if (mc.getWindow() == null) {
            return;
        }
        
        try {
            // Simply reset to null/empty to restore default
            GLFW.glfwSetWindowIcon(mc.getWindow().getHandle(), null);
            customIconSet = false;
        } catch (Exception e) {
            // Silently fail if we can't restore the icon
        }
    }
    
    /**
     * Sets the window icon from a BufferedImage.
     */
    private static void setWindowIcon(BufferedImage image) {
        if (mc.getWindow() == null || image == null) {
            return;
        }
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int width = image.getWidth();
            int height = image.getHeight();
            int[] pixels = new int[width * height];
            image.getRGB(0, 0, width, height, pixels, 0, width);
            
            ByteBuffer buffer = MemoryUtil.memAlloc(width * height * 4);
            
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = pixels[y * width + x];
                    buffer.put((byte) ((pixel >> 16) & 0xFF)); // Red
                    buffer.put((byte) ((pixel >> 8) & 0xFF));  // Green
                    buffer.put((byte) (pixel & 0xFF));         // Blue
                    buffer.put((byte) ((pixel >> 24) & 0xFF)); // Alpha
                }
            }
            
            buffer.flip();
            
            GLFWImage.Buffer iconBuffer = GLFWImage.malloc(1, stack);
            iconBuffer.position(0)
                .width(width)
                .height(height)
                .pixels(buffer);
            
            GLFW.glfwSetWindowIcon(mc.getWindow().getHandle(), iconBuffer);
            
            MemoryUtil.memFree(buffer);
        } catch (Exception e) {
            // Silently fail
        }
    }
    
    /**
     * Checks if a custom icon is currently set.
     */
    public static boolean isCustomIconSet() {
        return customIconSet;
    }
}
