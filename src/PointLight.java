/**
 * Stores the data for a point light for passing to a {@code GraphicsEngine.PixelShader}
 */
public class PointLight {
    public float x, y, z;
    public float brightnessR, brightnessG, brightnessB;

    public PointLight(float x, float y, float z, float brightnessR, float brightnessG, float brightnessB) {
        this.x = x; this.y = y; this.z = z;
        this.brightnessR = brightnessR; this.brightnessG = brightnessG; this.brightnessB = brightnessB;
    }
}
