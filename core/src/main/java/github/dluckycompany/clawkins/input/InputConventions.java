package github.dluckycompany.clawkins.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

/**
 * Shared keyboard/controller conventions for menu/dialogue navigation.
 * Keep these mappings centralized so screens do not re-define them.
 */
public final class InputConventions {
    private InputConventions() {
    }

    public static boolean isInteractJustPressed() {
        return isInteractKeyJustPressed(Input.Keys.ENTER)
                || isInteractKeyJustPressed(Input.Keys.NUMPAD_ENTER)
                || isInteractKeyJustPressed(Input.Keys.Z)
                || isInteractKeyJustPressed(Input.Keys.SPACE)
                || isInteractKeyJustPressed(Input.Keys.BUTTON_A);
    }

    public static boolean isCancelJustPressed() {
        return Gdx.input.isKeyJustPressed(Input.Keys.X)
                || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
                || Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)
                || Gdx.input.isKeyJustPressed(Input.Keys.BUTTON_B);
    }

    public static boolean isMenuUpJustPressed() {
        return Gdx.input.isKeyJustPressed(Input.Keys.W)
                || Gdx.input.isKeyJustPressed(Input.Keys.UP)
                || Gdx.input.isKeyJustPressed(Input.Keys.DPAD_UP);
    }

    public static boolean isMenuDownJustPressed() {
        return Gdx.input.isKeyJustPressed(Input.Keys.S)
                || Gdx.input.isKeyJustPressed(Input.Keys.DOWN)
                || Gdx.input.isKeyJustPressed(Input.Keys.DPAD_DOWN);
    }

    public static boolean isMenuLeftJustPressed() {
        return Gdx.input.isKeyJustPressed(Input.Keys.A)
                || Gdx.input.isKeyJustPressed(Input.Keys.LEFT)
                || Gdx.input.isKeyJustPressed(Input.Keys.DPAD_LEFT);
    }

    public static boolean isMenuRightJustPressed() {
        return Gdx.input.isKeyJustPressed(Input.Keys.D)
                || Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)
                || Gdx.input.isKeyJustPressed(Input.Keys.DPAD_RIGHT);
    }

    public static boolean isInteractKey(int keycode) {
        return keycode == Input.Keys.ENTER
                || keycode == Input.Keys.NUMPAD_ENTER
                || keycode == Input.Keys.Z
                || keycode == Input.Keys.SPACE
                || keycode == Input.Keys.BUTTON_A;
    }

    public static boolean isCancelKey(int keycode) {
        return keycode == Input.Keys.X
                || keycode == Input.Keys.ESCAPE
                || keycode == Input.Keys.BACKSPACE
                || keycode == Input.Keys.BUTTON_B;
    }

    private static boolean isInteractKeyJustPressed(int keycode) {
        return Gdx.input.isKeyJustPressed(keycode);
    }
}
