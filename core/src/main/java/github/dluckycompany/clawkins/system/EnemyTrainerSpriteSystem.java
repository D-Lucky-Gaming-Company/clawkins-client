package github.dluckycompany.clawkins.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import github.dluckycompany.clawkins.component.Enemy;
import github.dluckycompany.clawkins.component.FieldTrainerWalkSprite;
import github.dluckycompany.clawkins.component.Graphic;
import github.dluckycompany.clawkins.component.Move;

/**
 * For field trainer enemy sheets (three walk rows: south / side / north),
 * updates the trainer {@link com.badlogic.gdx.graphics.g2d.TextureRegion} each frame
 * from {@link Enemy#getFacingDirection()}
 * and walk animation timing, mirroring the side row for west when art faces
 * right (same axis preference as {@link PlayerInputSystem}).
 */
public class EnemyTrainerSpriteSystem extends IteratingSystem {

    public EnemyTrainerSpriteSystem() {
        super(
                Family.all(Enemy.class, Move.class, Graphic.class, FieldTrainerWalkSprite.class).get(),
                /* after MoveSystem + AnimationSystem */ 2);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        Enemy enemy = Enemy.MAPPER.get(entity);
        Move move = Move.MAPPER.get(entity);
        FieldTrainerWalkSprite sheet = FieldTrainerWalkSprite.MAPPER.get(entity);
        if (enemy == null || move == null || sheet == null) {
            return;
        }

        boolean moving = !move.getDirection().isZero() && move.getMaxSpeed() > 0f;
        if (moving) {
            sheet.addAnimTime(deltaTime);
        } else {
            sheet.clearAnimTime();
        }

        int frame = moving
                ? (int) (sheet.getAnimTime() / FieldTrainerWalkSprite.FRAME_DURATION_SEC)
                        % FieldTrainerWalkSprite.WALK_FRAMES
                : 0;

        Vector2 face = enemy.getFacingDirection();
        float fx = face.x;
        float fy = face.y;

        int row;
        boolean flipWest;
        if (Math.abs(fx) >= Math.abs(fy)) {
            row = sheet.getSideTextureRow();
            // Side row art faces right by default (see trainer*_field.tsx); mirror for west.
            flipWest = fx < 0f;
        } else if (fy > 0f) {
            row = sheet.getNorthTextureRow();
            flipWest = false;
        } else {
            row = sheet.getSouthTextureRow();
            flipWest = false;
        }

        int tw = sheet.getTilePixelWidth();
        int th = sheet.getTilePixelHeight();
        TextureRegion r = sheet.getRegion();
        r.setRegion(frame * tw, row * th, tw, th);

        if (flipWest) {
            if (!r.isFlipX()) {
                r.flip(true, false);
            }
        } else {
            if (r.isFlipX()) {
                r.flip(true, false);
            }
        }
    }
}
