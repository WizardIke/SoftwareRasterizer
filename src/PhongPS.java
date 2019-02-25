/**
 * A basic Phong fragment shader in java
 */

public class PhongPS implements GraphicsEngine.PixelShader {
    @Override
    public int run (Object[] rootSignature, Vector4[] in) {
        PointLight pointLight = (PointLight)rootSignature[2];
        Vector4 worldPos = in[0];
        Vector3 normal = in[1].xyz();
        normal.normalize();
        Vector3 minusLightVector = new Vector3(pointLight.x - worldPos.x, pointLight.y - worldPos.y, pointLight.z - worldPos.z);
        Vector3 light = new Vector3(0.15f, 0.15f, 0.15f);
        float lDotn = minusLightVector.dot(normal);
        if(lDotn > 0) {
            float distSq = minusLightVector.dot(minusLightVector);
            float lightAmount = lDotn / distSq;
            minusLightVector.normalize();
            Vector3 viewDir = new Vector3(worldPos.x - 0f, worldPos.y - 0f, worldPos.z - 0f);
            viewDir.normalize();
            Vector3 h = viewDir.plus(minusLightVector);
            h.normalize();
            lightAmount += Math.pow(Math.max(h.dot(normal), 0f), 4) * 0.15;
            light.x += pointLight.brightnessR * lightAmount;
            light.y += pointLight.brightnessG * lightAmount;
            light.z += pointLight.brightnessB * lightAmount;
        }
        if(light.x > 1f) light.x = 1f;
        if(light.y > 1f) light.y = 1f;
        if(light.z > 1f) light.z = 1f;

        return (255 << 24) + (((int)(light.x * 255)) << 16) + (((int)(light.y * 255)) << 8) + ((int)(light.z * 255));
    }
}
