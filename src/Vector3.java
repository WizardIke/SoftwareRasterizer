/**
 * Math vector that holds 3 floats
 */
public class Vector3 {
    public float x, y, z;

    public Vector3() {}
    public Vector3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public float length() {
        return (float)Math.sqrt(x * x + y * y + z * z);
    }

    public void divEquals(float value) {
        x /= value;
        y /= value;
        z /= value;
    }

    public void normalize() {
        float oneOverLength = x * x + y * y + z * z;
        if(oneOverLength == 0) {return;}
        oneOverLength = 1f / (float)Math.sqrt(oneOverLength);
        x *= oneOverLength;
        y *= oneOverLength;
        z *= oneOverLength;
    }

    public Vector3 minus(Vector3 rhs) {
        return new Vector3(x - rhs.x, y - rhs.y, z - rhs.z);
    }

    public Vector3 plus(Vector3 rhs) {
        return new Vector3(x + rhs.x, y + rhs.y, z + rhs.z);
    }

    /**
     * @return the cross product of the this and rhs
     */
    public Vector3 cross(Vector3 rhs) {
        return new Vector3(y * rhs.z - z * rhs.y, z * rhs.x - x * rhs.z, x * rhs.y - y * rhs.x);
    }

    public void plusEquals(Vector3 rhs) {
        x += rhs.x;
        y += rhs.y;
        z += rhs.z;
    }

    /**
     * @return the dot product of the two vectors
     */
    public float dot(Vector3 rhs) {
        return rhs.x * x + rhs.y * y + rhs.z * z;
    }
}
