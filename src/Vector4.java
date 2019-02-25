/**
 * Maths vector with 4 float dimensions
 */
public class Vector4 {
    public float x, y, z, w;

    public Vector4() {}
    public Vector4(float x, float y, float z, float w) {
        this.x = x; this.y = y; this.z = z; this.w = w;
    }

    public void normalize() {
        float oneOverLength = x * x + y * y + z * z + w * w;
        if(oneOverLength == 0f) {return;}
        oneOverLength = 1f / (float)Math.sqrt(oneOverLength);
        x *= oneOverLength;
        y *= oneOverLength;
        z *= oneOverLength;
        w *= oneOverLength;
    }

    public Vector3 xyz() {
        return new Vector3(x, y, z);
    }
}
