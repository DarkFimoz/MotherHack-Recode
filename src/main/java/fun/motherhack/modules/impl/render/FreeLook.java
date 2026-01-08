package fun.motherhack.modules.impl.render;

import fun.motherhack.api.events.impl.EventCamera;
import fun.motherhack.api.events.impl.EventFov;
import fun.motherhack.api.events.impl.EventKey;
import fun.motherhack.api.events.impl.EventMouseRotation;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.api.Bind;
import fun.motherhack.modules.settings.impl.BindSetting;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

@Getter
public class FreeLook extends Module {

    private final BindSetting freeLookBind = new BindSetting("FreeLook Key", new Bind(GLFW.GLFW_KEY_LEFT_ALT, false));

    private Perspective savedPerspective;
    private float cameraYaw;
    private float cameraPitch;
    private boolean isFreeLooking;

    public FreeLook() {
        super("FreeLook", Category.Render);
    }

    @Override
    public void onEnable() {
        isFreeLooking = false;
        savedPerspective = null;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (savedPerspective != null && mc.options != null) {
            mc.options.setPerspective(savedPerspective);
        }
        isFreeLooking = false;
        savedPerspective = null;
        super.onDisable();
    }

    private boolean isKeyPressed() {
        if (mc.getWindow() == null) return false;
        Bind bind = freeLookBind.getValue();
        if (bind.isEmpty()) return false;
        
        if (bind.isMouse()) {
            return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), bind.getKey()) == GLFW.GLFW_PRESS;
        } else {
            return GLFW.glfwGetKey(mc.getWindow().getHandle(), bind.getKey()) == GLFW.GLFW_PRESS;
        }
    }

    @EventHandler
    public void onKey(EventKey e) {
        if (fullNullCheck()) return;
        
        Bind bind = freeLookBind.getValue();
        if (bind.isEmpty() || bind.isMouse()) return;
        
        if (e.getKey() == bind.getKey() && e.getAction() == GLFW.GLFW_PRESS) {
            if (!isFreeLooking) {
                savedPerspective = mc.options.getPerspective();
                cameraYaw = mc.player.getYaw();
                cameraPitch = mc.player.getPitch();
            }
        }
    }

    @EventHandler
    public void onFov(EventFov e) {
        if (fullNullCheck()) return;

        if (isKeyPressed()) {
            if (!isFreeLooking) {
                savedPerspective = mc.options.getPerspective();
                cameraYaw = mc.player.getYaw();
                cameraPitch = mc.player.getPitch();
                isFreeLooking = true;
            }
            
            if (mc.options.getPerspective().isFirstPerson()) {
                mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
            }
        } else if (isFreeLooking) {
            if (savedPerspective != null) {
                mc.options.setPerspective(savedPerspective);
            }
            savedPerspective = null;
            isFreeLooking = false;
        }
    }

    @EventHandler
    public void onMouseRotation(EventMouseRotation e) {
        if (fullNullCheck()) return;

        if (isKeyPressed()) {
            if (!isFreeLooking) {
                cameraYaw = mc.player.getYaw();
                cameraPitch = mc.player.getPitch();
                isFreeLooking = true;
            }
            
            cameraYaw += e.getCursorDeltaX() * 0.15f;
            cameraPitch = MathHelper.clamp(cameraPitch + e.getCursorDeltaY() * 0.15f, -90f, 90f);
            e.cancel();
        } else {
            isFreeLooking = false;
        }
    }

    @EventHandler
    public void onCamera(EventCamera e) {
        if (fullNullCheck()) return;

        if (isKeyPressed() && isFreeLooking) {
            e.setYaw(cameraYaw);
            e.setPitch(cameraPitch);
            e.cancel();
        }
    }
}
