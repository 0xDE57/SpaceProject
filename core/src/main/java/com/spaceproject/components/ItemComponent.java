package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

public class ItemComponent implements Component {

    public enum Resource {
        RED(1, Color.RED, 50, 1.5f, 0.5f, 0.5f, 1.5f, "break0.wav"),
        GREEN(2, Color.GREEN, 60, 3f, 0.5f, 0.5f, 1.7f, "break1.wav"),
        BLUE(3, new Color(0.1f, 0.3f, 1f, 1), 50, 1f, 0.4f, 0.5f, 1.3f, "break2.wav"),
        SILVER(4, new Color(0.97f, 0.97f, 1, 1), 100, 0.3f, 0.3f, 0.8f, 1f, "break3.wav"),
        GOLD(5, Color.GOLD, 400, 8f, 0.8f, 0.8f, 3, "break4.wav"),
        GLASS(6, new Color(0.5f, 0.5f, 0.5f, 0.5f), 10, 0.1f, 0.05f, 1, 1.5f, "glass.wav");//
        //BLACK(7, Color.BLACK, 99999, -1, 99, 0, 0, "heavy?"); // bullets bounce (no damage) and laser absorb (no reflect) but does lots of damage (maybe 1.5x - 2x ?)
        //glass soft, bullets break easy, but laser refracts and passes through and does no damage
        //this forces you to get a laser to mine black!
        //this would allow a multi-material asteroid to have a black outer polygon, and gold inned polygon

        //glass could pre-fracture multiple levels!

        //consume resource for effect (either directly from cargo bay, or perhaps with a certain upgrade level for component):
        // green -> HP
        // blue -> shield or speed boost?
        // red -> extra damage?
        // this functionally allows resources to have more utility and depth than simply converting to credits
        // this would also reduce dependency on space stations, allowing for easier exploration of deep space
        // provided the player keeps discovering and mining resources along the way

        //todo : currently drops are squares. this should be an equilateral triangle.
        // 1. self-consistency with the triangle shattering. why does a square resource pop out of triangle?
        // 2. minor optimization. a quad requires 2 triangles. 1 triangle is less than 2.
        // 3. another optimization each resource should using single triangle sprite references between them (and recolor it)
        //      TriangleTexture {} as empty marker component. wait no need -> this component serves as marker flag. sprite2DRendersystem?
        //      SharedTextureComponent { enum {triangle, bullet, circle} }
        // 3.5 the new triangle sprite should also glow, either another radial background spite or better yet find a way to add glow to generated triangle sprite
        //     this completely negates optimization #2 as we are back to a quad. that is ok. #3 is most important.
        // 4. unrelated and maybe overkill potential optimization, could move resource into custom physics engine and remove from box2d and entities list altogether and into a local array managed by a system...
        //    but 2 and 3 should be sufficient as there are usually not too many resources in world anyways as the player prefers to collect

        /*
        ROCK()

        NICKEL(0.1f, new Color(192f/255f, 192f/255f, 192f/255f, 1)),
        IRON(0.2f, new Color(128f/255f, 128f/255f, 128f/255f, 1)),
        GOLD(0.15f, new Color(1f, 128f/255f, 128f/255f, 1)),
        PLATINUM(0.05f, new Color(214f/255f, 214f/255f, 214f/255f, 1)),

        RUBY(0.08f, new Color(220f/255f, 20f/255f, 60f/255f, 1)),
        SAPPHIRE(0.08f, new Color(0f, 0f, 186f/255f, 1)),
        EMERALD(0.07f, new Color(0, 128f/255f, 0, 1)),
        DIAMOND(0.02f, new Color(1, 1, 1, 1)),

        WATER(0.25f, new Color(0, 0, 1, 1));
        */

        /*
        ice asteroids
        collect water from ice asteroids
        ship has water tank (upgradable capacity, cooling efficiency)
        water for cooling
        ship hull heat
        ship touch sun -> consumes water for cooling
        ship overheat -> ship explode
        */

        private final int id;
        private final Color color;
        private final int value;
        private final float hardness;
        private final float density;
        private final float albedo;
        private final float refractiveIndex;
        private final String sound; // ID for which sound to play when interacted with (asteroid hit, resource collected)

        Resource(int id, Color color, int value, float hardness, float density, float albeto, float refractiveIndex, String sound) {
            this.id = id;
            this.color = color;
            this.value = value;
            this.hardness = hardness;
            this.density = density;
            this.albedo = albeto;
            this.refractiveIndex = refractiveIndex;
            this.sound = sound;
        }

        public int getId() {
            return id;
        }

        public Color getColor() {
            return color;
        }

        public int getValue() {
            return value;
        }

        public float getHardness() {
            return hardness;
        }

        public float getDensity() {
            return density;
        }

        public float getAlbedo() {
            return albedo;
        }

        public float getRefractiveIndex() {
            return refractiveIndex;
        }

        public String getSound() {
            return sound;
        }

        public static Resource getById(int id) {
            for (Resource resource : Resource.values()) {
                if (resource.getId() == id) {
                    return resource;
                }
            }
            throw new IllegalArgumentException("No enum constant with id " + id);
        }

        final static Resource[] VALUES = Resource.values();
        
        public static Resource random() {
            return VALUES[MathUtils.random(VALUES.length - 1)];
        }

    }

    public Resource resource;

}
