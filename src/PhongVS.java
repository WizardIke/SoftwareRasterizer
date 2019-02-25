/**
 * A basic vertex shader that outputs normal and world position
 */

public class PhongVS implements GraphicsEngine.VertexShader {
    @Override
    public void run(Object v, Object[] rootSignature, Vector4[] out) {
        Model.Vertex vertex = (Model.Vertex)v;
        Matrix4x4 wvpMatrix = (Matrix4x4)rootSignature[0];
        Vector4 pos = out[0];
        pos.x = vertex.x;
        pos.y = vertex.y;
        pos.z = vertex.z;
        pos.w = 1.0f;
        wvpMatrix.transform(pos);
        Matrix4x4 worldMatrix = (Matrix4x4)rootSignature[1];
        Vector4 worldPos = out[1];
        worldPos.x = vertex.x;
        worldPos.y = vertex.y;
        worldPos.z = vertex.z;
        worldPos.w = 1.0f;
        worldMatrix.transform(worldPos);
        Vector4 normal = out[2];
        normal.x = vertex.nx;
        normal.y = vertex.ny;
        normal.z = vertex.nz;
        worldMatrix.transformAsMatrix3x3(normal);
    }

    public static final int interpolateCount = 3;
}
