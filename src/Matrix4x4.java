/**
 * 4 by 4 row-major matrix of floats
 */
public class Matrix4x4 {
    private float m00, m01, m02, m03,
            m10, m11, m12, m13,
            m20, m21, m22, m23,
            m30, m31, m32, m33;

    public Matrix4x4(float m00, float m01, float m02, float m03, float m10, float m11, float m12, float m13, float m20,
                     float m21, float m22, float m23, float m30, float m31, float m32, float m33) {
        this.m00 = m00; this.m01 = m01; this.m02 = m02; this.m03 = m03;
        this.m10 = m10; this.m11 = m11; this.m12 = m12; this.m13 = m13;
        this.m20 = m20; this.m21 = m21; this.m22 = m22; this.m23 = m23;
        this.m30 = m30; this.m31 = m31; this.m32 = m32; this.m33 = m33;
    }

    /**
     * Creates a new scale matrix
     */
    static Matrix4x4 getScale(final float x, final float y, final float z) {
        return new Matrix4x4(x, 0f, 0f, 0f,
                0f, y, 0f, 0f,
                0f, 0f, z, 0f,
                0f, 0f, 0f, 1f);
    }

    /**
     * @param x The amount to translate in the x-axis
     * @param y The amount to translate in the y-axis
     * @param z The amount to translate in the z-axis
     * @return A new translation matrix
     */
    static Matrix4x4 getTranslation(final float x, final float y, final float z) {
        return new Matrix4x4(1, 0, 0, x,
                0, 1, 0, y,
                0, 0, 1, z,
                0, 0, 0,1);
    }

    static Matrix4x4 getProjection(float top, float bottum, float left, float right, float near, float far) {
        return new Matrix4x4((2 * near) / (right - left), 0, (right + left) / (right - left), 0,
                0, (2 * near) / (top - bottum), (top + bottum) / (top - bottum), 0,
                0, 0, -far / (far - near), (-far * near) / (far - near),
                0, 0, -1, 0);
    }

    /**
     * @return A new identity matrix
     */
    static Matrix4x4 getIdentity() {
        return new Matrix4x4(1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0,0,0,1);
    }

    /**
     * Returns a rotation matrix about the x-axis
     * @param angle The angle to rotate by
     */
    static Matrix4x4 getRotationX(float angle){
        float cosAngle = (float)Math.cos(angle);
        float sinAngle = (float)Math.sin(angle);

        return new Matrix4x4(1, 0 ,0, 0,
                0, cosAngle, -sinAngle, 0,
                0 ,sinAngle, cosAngle, 0,
                0, 0, 0, 1);
    }

    /**
     * Returns a rotation matrix about the y-axis
     * @param angle The angle to rotate by
     */
    static Matrix4x4 getRotationY(float angle){
        float cosAngle = (float)Math.cos(angle);
        float sinAngle = (float)Math.sin(angle);

        return new Matrix4x4(cosAngle, 0, sinAngle, 0,
                0, 1, 0, 0,
                -sinAngle , 0, cosAngle, 0,
                0, 0, 0, 1);
    }

    /**
     * Returns a rotation matrix about the z-axis
     * @param angle The angle to rotate by
     */
    static Matrix4x4 getRotationZ(float angle){
        float cosAngle = (float)Math.cos(angle);
        float sinAngle = (float)Math.sin(angle);

        return new Matrix4x4(cosAngle, -sinAngle, 0f, 0f,
                sinAngle, cosAngle, 0f, 0f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f);
    }

    void translate(final float x, final float y, final float z) {
        m00 += x * m30; m01 += x * m31; m02 += x * m32; m03 += x * m33;
        m10 += y * m30; m11 += y * m31; m12 += y * m32; m13 += y * m33;
        m20 += z * m30; m21 += z * m31; m22 += z * m32; m23 += z * m33;
    }

    void scale(final float x, final float y, final float z) {
        m00 *= x; m01 *= x; m02 *= x; m03 *= x;
        m10 *= y; m11 *= y; m12 *= y; m13 *= y;
        m20 *= z; m21 *= z; m22 *= z; m23 *= z;
    }

    void rotateX(float amount) {
        float cosAngle = (float)Math.cos(amount);
        float sinAngle = (float)Math.sin(amount);

        float newM10 = cosAngle * m10 - sinAngle * m20;
        float newM11 = cosAngle * m11 - sinAngle * m21;
        float newM12 = cosAngle * m12 - sinAngle * m22;
        float newM13 = cosAngle * m13 - sinAngle * m23;

        m20 = sinAngle * m10 + cosAngle * m20;
        m21 = sinAngle * m11 + cosAngle * m21;
        m22 = sinAngle * m12 + cosAngle * m22;
        m23 = sinAngle * m13 + cosAngle * m23;

        m10 = newM10;
        m11 = newM11;
        m12 = newM12;
        m13 = newM13;
    }

    void rotateY(float amount) {
        float cosAngle = (float)Math.cos(amount);
        float sinAngle = (float)Math.sin(amount);

        float newM00 = cosAngle * m00 + sinAngle * m20;
        float newM01 = cosAngle * m01 + sinAngle * m21;
        float newM02 = cosAngle * m02 + sinAngle * m22;
        float newM03 = cosAngle * m03 + sinAngle * m23;

        m20 = cosAngle * m20 - sinAngle * m00;
        m21 = cosAngle * m21 - sinAngle * m01;
        m22 = cosAngle * m22 - sinAngle * m02;
        m23 = cosAngle * m23 - sinAngle * m03;

        m00 = newM00;
        m01 = newM01;
        m02 = newM02;
        m03 = newM03;
    }

    void rotateZ(float amount) {
        float cosAngle = (float)Math.cos(amount);
        float sinAngle = (float)Math.sin(amount);

        float newM00 = cosAngle * m00 - sinAngle * m10;
        float newM01 = cosAngle * m01 - sinAngle * m11;
        float newM02 = cosAngle * m02 - sinAngle * m12;
        float newM03 = cosAngle * m03 - sinAngle * m13;

        m10 = sinAngle * m00 + cosAngle * m10;
        m11 = sinAngle * m01 + cosAngle * m11;
        m12 = sinAngle * m02 + cosAngle * m12;
        m13 = sinAngle * m03 + cosAngle * m13;

        m00 = newM00;
        m01 = newM01;
        m02 = newM02;
        m03 = newM03;
    }

    /**
     * multiples this with rhs to form a new matrix
     * @return The combined matrix
     */
    Matrix4x4 mul(final Matrix4x4 rhs) {
        return new Matrix4x4(m00 * rhs.m00 + m01 * rhs.m10 + m02 * rhs.m20 + m03 * rhs.m30,
                m00 * rhs.m01 + m01 * rhs.m11 + m02 * rhs.m21 + m03 * rhs.m31,
                m00 * rhs.m02 + m01 * rhs.m12 + m02 * rhs.m22 + m03 * rhs.m32,
                m00 * rhs.m03 + m01 * rhs.m13 + m02 * rhs.m23 + m03 * rhs.m33,

                m10 * rhs.m00 + m11 * rhs.m10 + m12 * rhs.m20 + m13 * rhs.m30,
                m10 * rhs.m01 + m11 * rhs.m11 + m12 * rhs.m21 + m13 * rhs.m31,
                m10 * rhs.m02 + m11 * rhs.m12 + m12 * rhs.m22 + m13 * rhs.m32,
                m10 * rhs.m03 + m11 * rhs.m13 + m12 * rhs.m23 + m13 * rhs.m33,

                m20 * rhs.m00 + m21 * rhs.m10 + m22 * rhs.m20 + m23 * rhs.m30,
                m20 * rhs.m01 + m21 * rhs.m11 + m22 * rhs.m21 + m23 * rhs.m31,
                m20 * rhs.m02 + m21 * rhs.m12 + m22 * rhs.m22 + m23 * rhs.m32,
                m20 * rhs.m03 + m21 * rhs.m13 + m22 * rhs.m23 + m23 * rhs.m33,

                m30 * rhs.m00 + m31 * rhs.m10 + m32 * rhs.m20 + m33 * rhs.m30,
                m30 * rhs.m01 + m31 * rhs.m11 + m32 * rhs.m21 + m33 * rhs.m31,
                m30 * rhs.m02 + m31 * rhs.m12 + m32 * rhs.m22 + m33 * rhs.m32,
                m30 * rhs.m03 + m31 * rhs.m13 + m32 * rhs.m23 + m33 * rhs.m33);
    }

    /**
     * Multiples this with rhs and returns the result
     */
    Vector4 mul(Vector4 rhs) {
        Vector4 vec = new Vector4();
        vec.x = m00 * rhs.x + m01 * rhs.y + m02 * rhs.z + m03 * rhs.w;
        vec.y = m10 * rhs.x + m11 * rhs.y + m12 * rhs.z + m13 * rhs.w;
        vec.z = m20 * rhs.x + m21 * rhs.y + m22 * rhs.z + m23 * rhs.w;
        vec.w = m30 * rhs.x + m31 * rhs.y + m32 * rhs.z + m33 * rhs.w;
        return vec;
    }

    /**
     * multiples this with rhs and stores the result in rhs
     * @param rhs the Vector4 to transform
     */
    void transform(Vector4 rhs) {
        float x = m00 * rhs.x + m01 * rhs.y + m02 * rhs.z + m03 * rhs.w;
        float y = m10 * rhs.x + m11 * rhs.y + m12 * rhs.z + m13 * rhs.w;
        float z = m20 * rhs.x + m21 * rhs.y + m22 * rhs.z + m23 * rhs.w;
        float w = m30 * rhs.x + m31 * rhs.y + m32 * rhs.z + m33 * rhs.w;

        rhs.x = x;
        rhs.y = y;
        rhs.z = z;
        rhs.w = w;
    }

    /**
     * transforms a vector ignoring the 4th dimension
     * @param rhs The vector to transform
     */
    void transformAsMatrix3x3(Vector4 rhs) {
        float x = m00 * rhs.x + m01 * rhs.y + m02 * rhs.z;
        float y = m10 * rhs.x + m11 * rhs.y + m12 * rhs.z;
        float z = m20 * rhs.x + m21 * rhs.y + m22 * rhs.z;

        rhs.x = x;
        rhs.y = y;
        rhs.z = z;
    }
}
