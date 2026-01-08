package fun.motherhack.modules.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor @Getter
public enum Category {
    Combat("E"),
    Movement("F"),
    Misc("G"),
    Render("H"),
    Client("I"),
    Hud("LOL");

    private final String icon;
}