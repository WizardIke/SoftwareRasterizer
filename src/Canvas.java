import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;

/**
 * The drawing area.
 */
class Canvas extends JPanel {
    private static final long serialVersionUID = 1L;


    private BufferedImage backBuffer;
    private Model model;
    private GraphicsEngine.PipelineState filledPipelineState;
    private GraphicsEngine.PipelineState wireFramePipelineState;
    private Matrix4x4 worldMatrix = Matrix4x4.getIdentity();
    private Matrix4x4 projectionMatrix;
    private Object[] rootArguments = new Object[3];
    private boolean wireFrame = true;
    private boolean fill = true;

    public Canvas() {
        setOpaque(true);

        GraphicsEngine.VertexShader phongVS = new PhongVS();
        GraphicsEngine.VertexShader wireFrameVS = (Object vert, Object[] rootSignature, Vector4[] out) -> {
            Model.Vertex vertex = (Model.Vertex) vert;
            Matrix4x4 wvpMatrix = (Matrix4x4) rootSignature[0];
            Vector4 pos = out[0];
            pos.x = vertex.x;
            pos.y = vertex.y;
            pos.z = vertex.z;
            pos.w = 1.f;
            wvpMatrix.transform(pos);
        };

        GraphicsEngine.PixelShader phongPS = new PhongPS();
        GraphicsEngine.PixelShader wireFramePS = (Object[] rootSignature, Vector4[] in) -> (255 << 24) + (140 << 16);

        filledPipelineState = new GraphicsEngine.PipelineState();
        filledPipelineState.cullBackFace = true;
        filledPipelineState.fillMode = GraphicsEngine.FillMode.solid;
        filledPipelineState.vertexShaderInterpolateCount = PhongVS.interpolateCount;
        filledPipelineState.vertexShader = phongVS;
        filledPipelineState.pixelShader = phongPS;

        wireFramePipelineState = new GraphicsEngine.PipelineState();
        wireFramePipelineState.cullBackFace = true;
        wireFramePipelineState.fillMode = GraphicsEngine.FillMode.wireFrame;
        wireFramePipelineState.vertexShaderInterpolateCount = 1;
        wireFramePipelineState.vertexShader = wireFrameVS;
        wireFramePipelineState.pixelShader = wireFramePS;

        addComponentListener(new Resizer());
        rootArguments[1] = worldMatrix;
        rootArguments[2] = new PointLight(10f, 5f, -5f, 0.f, 0.f, 6.f);
    }

    /**
     * resizes the backbuffer and recalculates projection matrix
     */
    private class Resizer extends ComponentAdapter {
        @Override
        public void componentResized(ComponentEvent e) {
            final int width = Math.max(Canvas.this.getWidth(), 1);
            final int height = Math.max(Canvas.this.getHeight(), 1);
            backBuffer = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().
                    getDefaultConfiguration().createCompatibleImage(width, height);
            float aspectRatio = (float)width / (float)height;
            projectionMatrix = (Matrix4x4.getScale(width / 2, -height / 2, 1f)).
                    mul(Matrix4x4.getTranslation(1.f, -1.f, 0f).
                            mul(Matrix4x4.getProjection(0.2f, -0.2f, -0.2f * aspectRatio, 0.2f * aspectRatio,
                                    0.2f, 100000f)));
            rootArguments[0] = projectionMatrix.mul(worldMatrix);

            repaint();
        }
    }

    public Matrix4x4 getWorldMatrix() {
        return worldMatrix;
    }

    public void setModel(final Model model) {
        this.model = model;
    }

    public void setWireFrame(boolean value) {
        this.wireFrame = value;
    }

    public void setFill(boolean value){
        this.fill = value;
    }

    public void setCullBackFace(boolean value) {
        filledPipelineState.cullBackFace = value;
        wireFramePipelineState.cullBackFace = value;
    }

    public void setWorldMatrix(Matrix4x4 worldMatrix) {
        this.worldMatrix = worldMatrix;
        rootArguments[1] = worldMatrix;
    }

    /**
     * must be called after setting transforms
     */
    public void updateTransform() {
        Matrix4x4 wvpMatrix = projectionMatrix.mul(worldMatrix);
        rootArguments[0] = wvpMatrix;
        sort(model, wvpMatrix);
    }

    /**
     * sorts a model to make it ready for drawing with the painter's algorithm
     * @param model the model to sort
     */
    private static void sort(Model model, Matrix4x4 wvpMatrix) {
        Model.Vertex pivotVertex = model.vertexBuffer[model.indexBuffer[(model.indexBuffer.length / 6) * 3]];
        Vector4 pivotVector = wvpMatrix.mul(new Vector4(pivotVertex.x, pivotVertex.y, pivotVertex.z, 1f));
        float pivot = pivotVector.z / pivotVector.w;
        sort(0, pivot, model.indexBuffer.length, model.indexBuffer, model.vertexBuffer, wvpMatrix);
    }

    /**
     * modified version of quicksort
     */
    private static void sort(int start, float pivot, int end, int[] indices, Model.Vertex[] vertices, Matrix4x4 wvpMatrix) {
        if(start == end || start + 3 == end) return;
        int middle = start;
        for(int i = start; i != end; i += 3) {
            float distance = calculateMeanDepth(vertices[indices[i]], vertices[indices[i + 1]],
                    vertices[indices[i + 2]], wvpMatrix);
            if(distance > pivot) {
                int temp = indices[i];
                indices[i] = indices[middle];
                indices[middle] = temp;
                ++middle;

                temp = indices[i + 1];
                indices[i + 1] = indices[middle];
                indices[middle] = temp;
                ++middle;

                temp = indices[i + 2];
                indices[i + 2] = indices[middle];
                indices[middle] = temp;
                ++middle;
            }
        }
        if(middle == start) {
            int pivotIndex = ((end - start) / 6) * 3 + start;
            int temp = indices[pivotIndex];
            indices[pivotIndex] = indices[start];
            indices[start] = temp;
            ++start;
            ++pivotIndex;

            temp = indices[pivotIndex];
            indices[pivotIndex] = indices[start];
            indices[start] = temp;
            ++start;
            ++pivotIndex;

            temp = indices[pivotIndex];
            indices[pivotIndex] = indices[start];
            indices[start] = temp;
            ++start;
            ++pivotIndex;

            int newPivotIndex = ((end - start) / 6) * 3 + start;
            float newPivot = calculateMeanDepth(vertices[indices[newPivotIndex]], vertices[indices[newPivotIndex + 1]],
                    vertices[indices[newPivotIndex + 2]], wvpMatrix);
            sort(start, newPivot, end, indices, vertices, wvpMatrix);
            return;
        }
        int newPivot1Index = ((middle - start) / 6) * 3 + start;
        float newPivot1 = calculateMeanDepth(vertices[indices[newPivot1Index]], vertices[indices[newPivot1Index + 1]],
                vertices[indices[newPivot1Index + 2]], wvpMatrix);
        sort(start, newPivot1, middle, indices, vertices, wvpMatrix);

        int newPivot2Index = ((end - middle) / 6) * 3 + middle;
        float newPivot2 = calculateMeanDepth(vertices[indices[newPivot2Index]], vertices[indices[newPivot2Index + 1]],
                vertices[indices[newPivot2Index + 2]], wvpMatrix);
        sort(middle, newPivot2, end, indices, vertices, wvpMatrix);
    }

    /**
     * Calculates the mean distance of a triangle from the camera
     */
    private static float calculateMeanDepth(Model.Vertex vertex1, Model.Vertex vertex2, Model.Vertex vertex3, Matrix4x4 wvpMatrix) {
        Vector4 depthVector1 = wvpMatrix.mul(new Vector4(vertex1.x, vertex1.y, vertex1.z, 1f));
        float depth1 = depthVector1.z / depthVector1.w;

        Vector4 depthVector2 = wvpMatrix.mul(new Vector4(vertex2.x, vertex2.y, vertex2.z, 1f));
        float depth2 = depthVector2.z / depthVector2.w;

        Vector4 depthVector3 = wvpMatrix.mul(new Vector4(vertex3.x, vertex3.y, vertex3.z, 1f));
        float depth3 = depthVector3.z / depthVector3.w;

        return (depth1 + depth2 + depth3) / 3f;
    }

    /**
     * redraws the model
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if(backBuffer == null) return;
        GraphicsEngine.clear(backBuffer, 0);
        if (model != null) {
            if(fill) {
                if(wireFrame) {
                    GraphicsEngine.drawInterleaved(filledPipelineState, wireFramePipelineState, rootArguments,
                            model.vertexBuffer, model.indexBuffer, backBuffer);
                } else {
                    GraphicsEngine.draw(filledPipelineState, rootArguments, model.vertexBuffer, model.indexBuffer,
                            backBuffer);
                }
            } else if(wireFrame) {
                GraphicsEngine.draw(wireFramePipelineState, rootArguments, model.vertexBuffer, model.indexBuffer,
                        backBuffer);
            }
        }
        g.drawImage(backBuffer, 0, 0,null);
    }
}