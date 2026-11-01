package com.example.desertvanguard.client;

import com.example.desertvanguard.DesertVanguardMod;
import com.example.desertvanguard.entity.DesertVanguardEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * Renders the skeletal mesh (models/entity/desert_vanguard.mesh): a bone hierarchy with a baked idle pose,
 * plus a walk animation that swings the legs while the upper body (arms, shield, glaive) stays in its pose.
 */
public class DesertVanguardRenderer extends EntityRenderer<DesertVanguardEntity> {

    private static final ResourceLocation MESH =
            new ResourceLocation(DesertVanguardMod.MODID, "models/entity/desert_vanguard.mesh");

    // Same order as the texture list in the PMX: skeleton, armor, glaive, shield
    private static final ResourceLocation[] TEXTURES = {
            tex("skeleton"), tex("desert_armor"), tex("desert_weapon"), tex("desert_shield")
    };

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(DesertVanguardMod.MODID, "textures/entity/desert_vanguard/" + name + ".png");
    }

    /** Max leg swing in radians at full stride. */
    private static final float LEG_SWING = 0.9F;

    private record Bone(String name, int parent, Vector3f pivot, Quaternionf idle) {}
    private record Part(int bone, int texture, int triangles, float[] data) {}

    private final Bone[] bones;
    private final Part[] parts;
    private final Matrix4f[] world;
    private final int rightLeg;
    private final int leftLeg;

    public DesertVanguardRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
        Bone[][] boneOut = new Bone[1][];
        this.parts = loadMesh(context.getResourceManager(), boneOut);
        this.bones = boneOut[0];
        this.world = new Matrix4f[bones.length];
        for (int i = 0; i < world.length; i++) world[i] = new Matrix4f();
        int r = -1, l = -1;
        for (int i = 0; i < bones.length; i++) {
            if (bones[i].name().equals("J_R_Leg")) r = i;
            if (bones[i].name().equals("J_L_Leg")) l = i;
        }
        this.rightLeg = r;
        this.leftLeg = l;
    }

    private static Part[] loadMesh(ResourceManager manager, Bone[][] boneOut) {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(manager.getResourceOrThrow(MESH).open()))) {
            Bone[] bones = new Bone[in.readInt()];
            for (int i = 0; i < bones.length; i++) {
                String name = in.readUTF();
                int parent = in.readInt();
                Vector3f pivot = new Vector3f(in.readFloat(), in.readFloat(), in.readFloat());
                Quaternionf idle = new Quaternionf(in.readFloat(), in.readFloat(), in.readFloat(), in.readFloat());
                bones[i] = new Bone(name, parent, pivot, idle);
            }
            boneOut[0] = bones;
            Part[] result = new Part[in.readInt()];
            for (int p = 0; p < result.length; p++) {
                int bone = in.readInt();
                int texture = in.readInt();
                int tris = in.readInt();
                float[] data = new float[tris * 3 * 8];
                for (int i = 0; i < data.length; i++) data[i] = in.readFloat();
                result[p] = new Part(bone, texture, tris, data);
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Could not load " + MESH, e);
        }
    }

    @Override
    public void render(DesertVanguardEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));

        if (entity.deathTime > 0) {
            float f = ((float) entity.deathTime + partialTick - 1.0F) / 20.0F * 1.6F;
            f = Math.min(Mth.sqrt(f), 1.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(f * 90.0F));
        }

        // walk cycle: legs swing in opposite directions, strength follows how fast the mob moves
        float phase = Mth.lerp(partialTick, entity.walkDistO, entity.walkDist) * 4.4F;
        float amp = Mth.lerp(partialTick, entity.walkAmpO, entity.walkAmp);
        float swing = Mth.cos(phase) * LEG_SWING * amp;
        if (entity.deathTime > 0) swing = 0.0F;

        for (int i = 0; i < bones.length; i++) {
            Bone b = bones[i];
            Quaternionf q = new Quaternionf();
            if (i == rightLeg) q.rotationX(swing).mul(b.idle());
            else if (i == leftLeg) q.rotationX(-swing).mul(b.idle());
            else q.set(b.idle());
            Matrix4f m = world[i];
            if (b.parent() < 0) {
                m.translation(b.pivot());
            } else {
                Vector3f pp = bones[b.parent()].pivot();
                m.set(world[b.parent()]).translate(b.pivot().x - pp.x, b.pivot().y - pp.y, b.pivot().z - pp.z);
            }
            m.rotate(q);
        }

        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0F);
        PoseStack.Pose pose = poseStack.last();

        for (Part part : parts) {
            Matrix4f pm = new Matrix4f(pose.pose()).mul(world[part.bone()]);
            Matrix3f nm = new Matrix3f(pose.normal()).mul(new Matrix3f(world[part.bone()]));
            VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURES[part.texture()]));
            float[] d = part.data();
            for (int t = 0; t < part.triangles(); t++) {
                int base = t * 24;
                // entity render types use QUADS, so the last vertex of the triangle is emitted twice
                emit(vc, pm, nm, d, base, overlay, packedLight);
                emit(vc, pm, nm, d, base + 8, overlay, packedLight);
                emit(vc, pm, nm, d, base + 16, overlay, packedLight);
                emit(vc, pm, nm, d, base + 16, overlay, packedLight);
            }
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void emit(VertexConsumer vc, Matrix4f m, Matrix3f n, float[] d, int o, int overlay, int light) {
        vc.vertex(m, d[o], d[o + 1], d[o + 2])
                .color(255, 255, 255, 255)
                .uv(d[o + 6], d[o + 7])
                .overlayCoords(overlay)
                .uv2(light)
                .normal(n, d[o + 3], d[o + 4], d[o + 5])
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(DesertVanguardEntity entity) {
        return TEXTURES[0];
    }
}
