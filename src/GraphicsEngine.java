import java.awt.image.BufferedImage;

/**
 * Class for drawing shaded objects
 */
public class GraphicsEngine {
    /**
     * Transforms vertices into screen space
     */
    interface VertexShader {
         void run(Object vertex, Object[] rootSignature, Vector4[] out);
    }

    /**
     * calculates the color of a pixel fragment
     */
    interface PixelShader {
        int run(Object[] rootSignature, Vector4[] interpolates);
    }

    public enum FillMode {
        wireFrame, solid
    }

    /**
     * description of how to draw the polygons
     */
    static class PipelineState {
        public VertexShader vertexShader;
        public int vertexShaderInterpolateCount;
        public PixelShader pixelShader;
        public FillMode fillMode;
        public boolean cullBackFace;
    }

    /**
     * @param pipelineState The description of how to draw the polygons.
     * @param rootArgument The values to pass to the vertex and pixel shaders.
     * @param vertexBuffer An array of vertices to draw.
     * @param indexBuffer Contains information on which order to draw the vertices. Also allows reuse of vertices.
     * @param renderTarget The image to draw onto.
     */
    public static void draw(PipelineState pipelineState, Object[] rootArgument,  Object[] vertexBuffer, int[] indexBuffer,
                     BufferedImage renderTarget) {
        Vector4[] pixelShaderInput = null;
        if(pipelineState.vertexShaderInterpolateCount - 1 > 0) {
            pixelShaderInput = new Vector4[pipelineState.vertexShaderInterpolateCount - 1];
            for(int i = 0; i < pixelShaderInput.length; ++i) {
                pixelShaderInput[i] = new Vector4();
            }
        }
        Vector4[] interpolates1 = new Vector4[pipelineState.vertexShaderInterpolateCount];
        Vector4[] interpolates2 = new Vector4[pipelineState.vertexShaderInterpolateCount];
        Vector4[] interpolates3 = new Vector4[pipelineState.vertexShaderInterpolateCount];
        Vector4[] interpolates4 = new Vector4[pipelineState.vertexShaderInterpolateCount];

        for(int i = 0; i < pipelineState.vertexShaderInterpolateCount; ++i) {
            interpolates1[i] = new Vector4();
            interpolates2[i] = new Vector4();
            interpolates3[i] = new Vector4();
            interpolates4[i] = new Vector4();
        }
        for(int i = 0; i < indexBuffer.length; ++i) {
            pipelineState.vertexShader.run(vertexBuffer[indexBuffer[i]], rootArgument, interpolates1);
            ++i;
            pipelineState.vertexShader.run(vertexBuffer[indexBuffer[i]], rootArgument, interpolates2);
            ++i;
            pipelineState.vertexShader.run(vertexBuffer[indexBuffer[i]], rootArgument, interpolates3);

            cullTriangle(interpolates1, interpolates2, interpolates3, interpolates4, pipelineState, rootArgument,
                    renderTarget, pixelShaderInput);
        }
    }

    /**
     * draws the first polygon using pipelineState1 and pipelineState2 and then the second etc.
     */
    public static void drawInterleaved(PipelineState pipelineState1, PipelineState pipelineState2, Object[] rootSignature,
                                       Object[] vertexBuffer, int[] indexBuffer, BufferedImage renderTarget) {
        Vector4[] pixelShaderInput = null;
        int pixelShaderInputCount = Math.max(pipelineState1.vertexShaderInterpolateCount - 1,
                pipelineState2.vertexShaderInterpolateCount - 1);
        if(pixelShaderInputCount > 0) {
            pixelShaderInput = new Vector4[pixelShaderInputCount];
            for(int i = 0; i < pixelShaderInput.length; ++i) {
                pixelShaderInput[i] = new Vector4();
            }
        }
        Vector4[] interpolates1 = new Vector4[pipelineState1.vertexShaderInterpolateCount];
        Vector4[] interpolates2 = new Vector4[pipelineState1.vertexShaderInterpolateCount];
        Vector4[] interpolates3 = new Vector4[pipelineState1.vertexShaderInterpolateCount];
        Vector4[] interpolates4 = new Vector4[pipelineState1.vertexShaderInterpolateCount];

        for(int i = 0; i < pipelineState1.vertexShaderInterpolateCount; ++i) {
            interpolates1[i] = new Vector4();
            interpolates2[i] = new Vector4();
            interpolates3[i] = new Vector4();
            interpolates4[i] = new Vector4();
        }

        Vector4[] interpolates12 = new Vector4[pipelineState2.vertexShaderInterpolateCount];
        Vector4[] interpolates22 = new Vector4[pipelineState2.vertexShaderInterpolateCount];
        Vector4[] interpolates32 = new Vector4[pipelineState2.vertexShaderInterpolateCount];
        Vector4[] interpolates42 = new Vector4[pipelineState2.vertexShaderInterpolateCount];

        for(int i = 0; i < pipelineState2.vertexShaderInterpolateCount; ++i) {
            interpolates12[i] = new Vector4();
            interpolates22[i] = new Vector4();
            interpolates32[i] = new Vector4();
            interpolates42[i] = new Vector4();
        }

        for(int i = 0; i < indexBuffer.length; ++i) {
            pipelineState1.vertexShader.run(vertexBuffer[indexBuffer[i]], rootSignature, interpolates1);
            pipelineState2.vertexShader.run(vertexBuffer[indexBuffer[i]], rootSignature, interpolates12);
            ++i;
            pipelineState1.vertexShader.run(vertexBuffer[indexBuffer[i]], rootSignature, interpolates2);
            pipelineState2.vertexShader.run(vertexBuffer[indexBuffer[i]], rootSignature, interpolates22);
            ++i;
            pipelineState1.vertexShader.run(vertexBuffer[indexBuffer[i]], rootSignature, interpolates3);
            pipelineState2.vertexShader.run(vertexBuffer[indexBuffer[i]], rootSignature, interpolates32);

            cullTriangle(interpolates1, interpolates2, interpolates3, interpolates4, pipelineState1, rootSignature,
                    renderTarget, pixelShaderInput);

            cullTriangle(interpolates12, interpolates22, interpolates32, interpolates42, pipelineState2, rootSignature,
                    renderTarget, pixelShaderInput);
        }
    }

    /**
     * Removes offscreen triangles and draws the parts of triangles that are on screen
     */
    private static void cullTriangle(Vector4[] interpolates1, Vector4[] interpolates2, Vector4[] interpolates3,
                                     Vector4[] interpolates4, PipelineState pipelineState,
                                     Object[] rootSignature, BufferedImage renderTarget, Vector4[] pixelShaderInput) {

        Vector4 pos1 = interpolates1[0];
        Vector4 pos2 = interpolates2[0];
        Vector4 pos3 = interpolates3[0];
        final int widthMinusOne = renderTarget.getWidth() - 1;
        final int heightMinusOne = renderTarget.getHeight() - 1;

        if(pipelineState.fillMode == FillMode.solid) {
            //TODO fix culling triangles that are to close
            if(pos1.z > 0f && pos2.z > 0f && pos3.z > 0f) {
                pos1.x /= pos1.w;
                pos1.y /= pos1.w;
                pos2.x /= pos2.w;
                pos2.y /= pos2.w;
                pos3.x /= pos3.w;
                pos3.y /= pos3.w;

                if(pipelineState.cullBackFace) {
                    final float x1 = pos1.x - pos2.x;
                    final float x2 = pos3.x - pos2.x;
                    final float y1 = pos1.y - pos2.y;
                    final float y2 = pos3.y - pos2.y;
                    if((x1 * y2 - y1 * x2) < 0) return;
                }
                int pos1Zone = findZone(pos1.x, pos1.y, widthMinusOne, heightMinusOne);
                int pos2Zone = findZone(pos2.x, pos2.y, widthMinusOne, heightMinusOne);
                int pos3Zone = findZone(pos3.x, pos3.y, widthMinusOne, heightMinusOne);

                if((pos1Zone & pos2Zone & pos3Zone) == 0) {
                    if(pos1Zone == 0 && pos2Zone == 0 && pos3Zone == 0) {
                        drawTriangle(pos1, pos2, pos3, interpolates1, interpolates2, interpolates3, interpolates4, rootSignature,
                                pixelShaderInput, pipelineState.pixelShader, renderTarget);
                    } else {
                        cullTriangleTop(pos1, pos2, pos3, interpolates1, interpolates2, interpolates3, interpolates4, rootSignature,
                                pixelShaderInput, pipelineState.pixelShader, renderTarget);
                    }
                }
            }
        } else {
            if(pos1.z > 0f && pos2.z > 0f && pos3.z > 0f) {
                pos1.x /= pos1.w;
                pos1.y /= pos1.w;
                pos2.x /= pos2.w;
                pos2.y /= pos2.w;
                pos3.x /= pos3.w;
                pos3.y /= pos3.w;

                if(pipelineState.cullBackFace) {
                    final float x1 = pos1.x - pos2.x;
                    final float x2 = pos3.x - pos2.x;
                    final float y1 = pos1.y - pos2.y;
                    final float y2 = pos3.y - pos2.y;
                    if((x1 * y2 - y1 * x2) < 0) return;
                }
                int pos1Zone = findZone(pos1.x, pos1.y, widthMinusOne, heightMinusOne);
                int pos2Zone = findZone(pos2.x, pos2.y, widthMinusOne, heightMinusOne);
                int pos3Zone = findZone(pos3.x, pos3.y, widthMinusOne, heightMinusOne);

                cullLine(pos1.x, pos1.y, pos2.x, pos2.y, pos1Zone, pos2Zone, pipelineState.pixelShader, rootSignature, renderTarget,
                        interpolates1, interpolates2, pixelShaderInput, widthMinusOne, heightMinusOne);
                cullLine(pos1.x, pos1.y, pos3.x, pos3.y, pos1Zone, pos3Zone, pipelineState.pixelShader, rootSignature, renderTarget,
                        interpolates1, interpolates3, pixelShaderInput, widthMinusOne, heightMinusOne);
                cullLine(pos3.x, pos3.y, pos2.x, pos2.y, pos3Zone, pos2Zone, pipelineState.pixelShader, rootSignature, renderTarget,
                        interpolates3, interpolates2, pixelShaderInput, widthMinusOne, heightMinusOne);
            } else {
                cullZLine(pos1, pos2, interpolates1, interpolates2, interpolates4, pipelineState.pixelShader,
                        rootSignature, renderTarget, pixelShaderInput, widthMinusOne, heightMinusOne);
                cullZLine(pos1, pos3, interpolates1, interpolates3, interpolates4, pipelineState.pixelShader,
                        rootSignature, renderTarget, pixelShaderInput, widthMinusOne, heightMinusOne);
                cullZLine(pos3, pos2, interpolates3, interpolates2, interpolates4, pipelineState.pixelShader,
                        rootSignature, renderTarget, pixelShaderInput, widthMinusOne, heightMinusOne);
            }
        }
    }

    private static void cullZLine(Vector4 pos1, Vector4 pos2, Vector4[] interpolates1,
                                  Vector4[] interpolates2, Vector4[] interpolates4, PixelShader pixelShader,
                                  Object[] rootSignature, BufferedImage renderTarget, Vector4[] pixelShaderInput,
                                  int widthMinusOne, int heightMinusOne) {
        if(pos1.z > 0f) {
            if(pos2.z > 0f) {
                float x1 = pos1.x /= pos1.w;
                float y1 = pos1.y /= pos1.w;
                float x2 = pos2.x /= pos2.w;
                float y2 = pos2.y /= pos2.w;
                int pos1Zone = findZone(x1, y1, widthMinusOne, heightMinusOne);
                int pos2Zone = findZone(x2, y2, widthMinusOne, heightMinusOne);
                cullLine(x1, y1, x2, y2, pos1Zone, pos2Zone,
                        pixelShader, rootSignature, renderTarget, interpolates1, interpolates2, pixelShaderInput,
                        widthMinusOne, heightMinusOne);
            } else {
                float amount = (-pos2.z) / (pos1.z - pos2.z);
                createNewInterpolate(interpolates4, interpolates1, interpolates2, amount);
                float x4 = pos2.x - amount * (pos2.x - pos1.x);
                float y4 = pos2.y - amount * (pos2.y - pos1.y);
                float w4 = pos2.w - amount * (pos2.w - pos1.w);
                x4 /= w4;
                y4 /= w4;
                float x1 = pos1.x / pos1.w;
                float y1 = pos1.y / pos1.w;
                int pos1Zone = findZone(x1, y1, widthMinusOne, heightMinusOne);
                int pos2Zone = findZone(x4, y4, widthMinusOne, heightMinusOne);
                cullLine(x1, y1, x4, y4, pos1Zone, pos2Zone,
                        pixelShader, rootSignature, renderTarget, interpolates1, interpolates4, pixelShaderInput,
                        widthMinusOne, heightMinusOne);
            }
        } else {
            if(pos2.z > 0f) {
                float amount = (-pos1.z) / (pos2.z - pos1.z);
                createNewInterpolate(interpolates4, interpolates2, interpolates1, amount);
                float x4 = pos1.x - amount * (pos1.x - pos2.x);
                float y4 = pos1.y - amount * (pos1.y - pos2.y);
                float w4 = pos1.w - amount * (pos1.w - pos2.w);
                x4 /= w4;
                y4 /= w4;
                float x2 = pos2.x / pos2.w;
                float y2 = pos2.y / pos2.w;
                int pos4Zone = findZone(x4, y4, widthMinusOne, heightMinusOne);
                int pos2Zone = findZone(x2, y2, widthMinusOne, heightMinusOne);
                cullLine(x4, y4, x2, y2, pos4Zone, pos2Zone,
                        pixelShader, rootSignature, renderTarget, interpolates4, interpolates2, pixelShaderInput,
                        widthMinusOne, heightMinusOne);
            }
        }
    }

    private static void cullLine(float x1, float y1, float x2, float y2, int pos1Zone, int pos2Zone, PixelShader pixelShader,
                          Object[] rootSignature, BufferedImage renderTarget, Vector4[] interpolates1,
                          Vector4[] interpolates2, Vector4[] pixelShaderInput, int widthMinusOne, int heightMinusOne) {
        if((pos1Zone & pos2Zone) == 0) {
            if(pos1Zone == 0) {
                if(pos2Zone == 0) {
                    rasterizeLine((int)x1, (int)y1, (int)x2, (int)y2, pixelShader,
                            rootSignature, renderTarget, interpolates1, interpolates2, pixelShaderInput);
                } else {
                    Vector2 p2 = clipSecond(new Vector4(x1, y1, 0f, 1f), new Vector4(x2, y2, 0f, 1f),
                            widthMinusOne, heightMinusOne);
                    rasterizeLine((int)x1, (int)y1, (int)p2.x, (int)p2.y, pixelShader,
                            rootSignature, renderTarget, interpolates1, interpolates2, pixelShaderInput);
                }
            } else {
                if(pos2Zone == 0) {
                    Vector2 p1 = clipSecond(new Vector4(x2, y2, 0f, 1f), new Vector4(x1, y1, 0f, 1f),
                            widthMinusOne, heightMinusOne);
                    rasterizeLine((int)p1.x, (int)p1.y, (int)x2, (int)y2, pixelShader,
                            rootSignature, renderTarget, interpolates1, interpolates2, pixelShaderInput);
                } else {
                    clipBoth(x2, y2, x1, y1, pos1Zone, widthMinusOne, heightMinusOne, pixelShader,
                            rootSignature, renderTarget, interpolates2, interpolates1, pixelShaderInput);
                }
            }
        }
    }

    private static void clipBoth(float x1, float y1, float x2, float y2, int pos1Zone, int widthMinusOne, int heightMinusOne,
                                 PixelShader pixelShader, Object[] rootSignature, BufferedImage renderTarget, Vector4[] interpolates1,
                                 Vector4[] interpolates2, Vector4[] pixelShaderInput) {
        if(x2 < 0) {
            y2 += (y1 - y2) * (-x2) / (x1 - x2);
            x2 = 0;
        } else if(x2 > widthMinusOne) {
            y2 += (y1 - y2) * (x2 - widthMinusOne) / (x2 - x1);
            x2 = widthMinusOne;
        } else if(y2 < 0) {
            x2 += (x1 - x2) * (-y2) / (y1 - y2);
            y2 = 0;
        } else if(y2 > heightMinusOne) {
            x2 += (x1 - x2) * (y2 - heightMinusOne) / (y2 - y1);
            y2 = heightMinusOne;
        }
        int pos2Zone = findZone(x2, y2, widthMinusOne, heightMinusOne);
        if((pos1Zone & pos2Zone) != 0) {
            return;
        } else if(pos2Zone == 0) {
            Vector2 p1 = clipSecond(new Vector4(x2, y2, 0f, 1f), new Vector4(x1, y1, 0f, 1f), widthMinusOne, heightMinusOne);
            rasterizeLine((int)p1.x, (int)p1.y, (int)x2, (int)y2, pixelShader,
                    rootSignature, renderTarget, interpolates2, interpolates1, pixelShaderInput);
            return;
        }
        clipBoth(x2, y2, x1, y1, pos2Zone, widthMinusOne, heightMinusOne, pixelShader, rootSignature, renderTarget,
                interpolates2, interpolates1, pixelShaderInput);
    }

    private static int findZone(float x, float y, int widthMinusOne, int heightMinusOne) {
        int posZone = 0;
        if(x < 0f) {
            posZone = 1;
        }
        if(x > widthMinusOne) {
            posZone |= 2;
        }
        if(y < 0f) {
            posZone |= 4;
        }
        if(y > heightMinusOne) {
            posZone |= 8;
        }
        return posZone;
    }

    private static void cullThirdTop(Vector4 pos1, Vector4 pos2, Vector4 pos3, Vector4 pos4, Vector4[] interpolates1,
                                  Vector4[] interpolates2, Vector4[] interpolates3, Vector4[] interpolates4,
                                  Vector4[] interpolates5, Object[] rootSignature, Vector4[] pixelShaderInput,
                                  PixelShader solidPixelShader, BufferedImage renderTarget) {
        pos4.y = 0f;
        float amount3 = pos3.y / (pos3.y - pos1.y);
        pos4.x = pos3.x - amount3 * (pos3.x - pos1.x);
        createNewInterpolate(interpolates5, interpolates1, interpolates3, amount3);
        cullTriangleBottom(pos1, pos2, pos4, interpolates1, interpolates2, interpolates5, interpolates4,
                rootSignature, pixelShaderInput, solidPixelShader, renderTarget);

        amount3 = pos3.y / (pos3.y - pos2.y);
        pos1.x = pos3.x + amount3 * (pos2.x - pos3.x);
        pos1.y = 0f;
        createNewInterpolate(interpolates1, interpolates2, interpolates3, amount3);
        cullTriangleBottom(pos2, pos1, pos4, interpolates2, interpolates1, interpolates5, interpolates4,
                rootSignature, pixelShaderInput, solidPixelShader, renderTarget);
    }

    private static void cullThirdBottom(Vector4 pos1, Vector4 pos2, Vector4 pos3, Vector4 pos4, Vector4[] interpolates1,
                                     Vector4[] interpolates2, Vector4[] interpolates3, Vector4[] interpolates4,
                                     Vector4[] interpolates5, Object[] rootSignature, Vector4[] pixelShaderInput,
                                     PixelShader solidPixelShader, BufferedImage renderTarget) {
        final int heightMinusOne = renderTarget.getHeight() - 1;
        pos4.y = heightMinusOne;
        float amount3 = (heightMinusOne - pos3.y) / (pos1.y - pos3.y);
        pos4.x = pos3.x - amount3 * (pos3.x - pos1.x);
        createNewInterpolate(interpolates5, interpolates1, interpolates3, amount3);
        cullTriangleLeft(pos1, pos2, pos4, interpolates1, interpolates2, interpolates5, interpolates4,
                rootSignature, pixelShaderInput, solidPixelShader, renderTarget);

        amount3 = (heightMinusOne - pos3.y) / (pos2.y - pos3.y);
        pos1.x = pos3.x + amount3 * (pos2.x - pos3.x);
        pos1.y = heightMinusOne;
        createNewInterpolate(interpolates1, interpolates2, interpolates3, amount3);
        cullTriangleLeft(pos2, pos1, pos4, interpolates2, interpolates1, interpolates5, interpolates4,
                rootSignature, pixelShaderInput, solidPixelShader, renderTarget);
    }

    private static void cullThirdLeft(Vector4 pos1, Vector4 pos2, Vector4 pos3, Vector4 pos4, Vector4[] interpolates1,
                                        Vector4[] interpolates2, Vector4[] interpolates3, Vector4[] interpolates4,
                                        Vector4[] interpolates5, Object[] rootSignature, Vector4[] pixelShaderInput,
                                        PixelShader solidPixelShader, BufferedImage renderTarget) {
        pos4.x = 0f;
        float amount3 = pos3.x / (pos3.x - pos1.x);
        pos4.y = pos3.y - amount3 * (pos3.y - pos1.y);
        createNewInterpolate(interpolates4, interpolates1, interpolates3, amount3);
        cullTriangleRight(pos1, pos2, pos4, interpolates1, interpolates2, interpolates4, interpolates5,
                rootSignature, pixelShaderInput, solidPixelShader, renderTarget);

        amount3 = pos3.x / (pos3.x - pos2.x);
        pos1.y = pos3.y + amount3 * (pos2.y - pos3.y);
        pos1.x = 0f;
        createNewInterpolate(interpolates1, interpolates2, interpolates3, amount3);
        cullTriangleRight(pos2, pos1, pos4, interpolates2, interpolates1, interpolates4, interpolates5,
                rootSignature, pixelShaderInput, solidPixelShader, renderTarget);
    }

    private static void cullThirdRight(Vector4 pos1, Vector4 pos2, Vector4 pos3, Vector4 pos4, Vector4[] interpolates1,
                                        Vector4[] interpolates2, Vector4[] interpolates3, Vector4[] interpolates4,
                                        Vector4[] interpolates5, Object[] rootSignature, Vector4[] pixelShaderInput,
                                        PixelShader solidPixelShader, BufferedImage renderTarget) {
        final int widthMinusOne = renderTarget.getWidth() - 1;
        pos4.x = widthMinusOne;
        float amount3 = (widthMinusOne - pos3.x) / (pos1.x - pos3.x);
        pos4.y = pos3.y - amount3 * (pos3.y - pos1.y);
        createNewInterpolate(interpolates5, interpolates1, interpolates3, amount3);
        drawTriangle(pos1, pos2, pos4, interpolates1, interpolates2, interpolates5, interpolates4,
                rootSignature, pixelShaderInput, solidPixelShader, renderTarget);

        amount3 = (widthMinusOne - pos3.x) / (pos2.x - pos3.x);
        pos1.y = pos3.y + amount3 * (pos2.y - pos3.y);
        pos1.x = widthMinusOne;
        createNewInterpolate(interpolates1, interpolates2, interpolates3, amount3);
        drawTriangle(pos2, pos1, pos4, interpolates2, interpolates1, interpolates5, interpolates4,
                rootSignature, pixelShaderInput, solidPixelShader, renderTarget);
    }

    private static void cullSecondAndThirdTop(Vector4 pos1, Vector4 pos2, Vector4 pos3, Vector4[] interpolates1,
                                              Vector4[] interpolates2, Vector4[] interpolates3,
                                              Vector4[] interpolates4, Object[] rootSignature, Vector4[] pixelShaderInput,
                                              PixelShader solidPixelShader, BufferedImage renderTarget) {
        float amount3 = pos3.y / (pos3.y - pos1.y);
        createNewInterpolate(interpolates3, interpolates1, interpolates3, amount3);
        pos3.y = 0;
        pos3.x += amount3 * (pos1.x - pos3.x);

        float amount2 = pos2.y / (pos2.y - pos1.y);
        createNewInterpolate(interpolates2, interpolates1, interpolates2, amount2);
        pos2.y = 0;
        pos2.x += amount2 * (pos1.x - pos2.x);

        cullTriangleBottom(pos1, pos2, pos3, interpolates1, interpolates2, interpolates3, interpolates4, rootSignature,
                pixelShaderInput, solidPixelShader, renderTarget);
    }

    private static void cullSecondAndThirdBottom(Vector4 pos1, Vector4 pos2, Vector4 pos3, Vector4[] interpolates1,
                                              Vector4[] interpolates2, Vector4[] interpolates3,
                                              Vector4[] interpolates4, Object[] rootSignature, Vector4[] pixelShaderInput,
                                              PixelShader solidPixelShader, BufferedImage renderTarget) {
        final int heightMinusOne = renderTarget.getHeight() - 1;
        float amount3 = (heightMinusOne - pos3.y) / (pos1.y - pos3.y);
        createNewInterpolate(interpolates3, interpolates1, interpolates3, amount3);
        pos3.y = heightMinusOne;
        pos3.x += amount3 * (pos1.x - pos3.x);

        float amount2 = (heightMinusOne - pos2.y) / (pos1.y - pos2.y);
        createNewInterpolate(interpolates2, interpolates1, interpolates2, amount2);
        pos2.y = heightMinusOne;
        pos2.x += amount2 * (pos1.x - pos2.x);

        cullTriangleLeft(pos1, pos2, pos3, interpolates1, interpolates2, interpolates3, interpolates4, rootSignature,
                pixelShaderInput, solidPixelShader, renderTarget);
    }

    private static void cullSecondAndThirdLeft(Vector4 pos1, Vector4 pos2, Vector4 pos3, Vector4[] interpolates1,
                                              Vector4[] interpolates2, Vector4[] interpolates3,
                                              Vector4[] interpolates4, Object[] rootSignature, Vector4[] pixelShaderInput,
                                              PixelShader solidPixelShader, BufferedImage renderTarget) {
        float amount3 = (-pos3.x) / (pos1.x - pos3.x);
        createNewInterpolate(interpolates3, interpolates1, interpolates3, amount3);
        pos3.x = 0;
        pos3.y += amount3 * (pos1.y - pos3.y);

        float amount2 = (-pos2.x) / (pos1.x - pos2.x);
        createNewInterpolate(interpolates2, interpolates1, interpolates2, amount2);
        pos2.x = 0;
        pos2.y += amount2 * (pos1.y - pos2.y);

        cullTriangleRight(pos1, pos2, pos3, interpolates1, interpolates2, interpolates3, interpolates4, rootSignature,
                pixelShaderInput, solidPixelShader, renderTarget);
    }

    private static void cullSecondAndThirdRight(Vector4 pos1, Vector4 pos2, Vector4 pos3, Vector4[] interpolates1,
                                                 Vector4[] interpolates2, Vector4[] interpolates3,
                                                 Vector4[] interpolates4, Object[] rootSignature, Vector4[] pixelShaderInput,
                                                 PixelShader solidPixelShader, BufferedImage renderTarget) {
        final int widthMinusOne = renderTarget.getWidth() - 1;
        float amount3 = (widthMinusOne - pos3.x) / (pos1.x - pos3.x);
        createNewInterpolate(interpolates3, interpolates1, interpolates3, amount3);
        pos3.x = widthMinusOne;
        pos3.y += amount3 * (pos1.y - pos3.y);

        float amount2 = (widthMinusOne - pos2.x) / (pos1.x - pos2.x);
        createNewInterpolate(interpolates2, interpolates1, interpolates2, amount2);
        pos2.x = widthMinusOne;
        pos2.y += amount2 * (pos1.y - pos2.y);

        drawTriangle(pos1, pos2, pos3, interpolates1, interpolates2, interpolates3, interpolates4, rootSignature,
                pixelShaderInput, solidPixelShader, renderTarget);
    }

    private static void cullTriangleTop(Vector4 pos1, Vector4 pos2, Vector4 pos3, Vector4[] interpolates1,
                                        Vector4[] interpolates2, Vector4[] interpolates3, Vector4[] interpolates4,
                                        Object[] rootSignature, Vector4[] pixelShaderInput,
                                        PixelShader solidPixelShader, BufferedImage renderTarget) {
        Vector4[] interpolates5 = new Vector4[interpolates1.length];
        for(int i = 0; i < interpolates5.length; ++i) {
            interpolates5[i] = new Vector4();
        }
        Vector4 pos4 = new Vector4();
        if(pos1.y >= 0) {
            if(pos2.y >= 0) {
                if(pos3.y >= 0) {
                    cullTriangleBottom(pos1, pos2, pos3, interpolates1, interpolates2, interpolates3, interpolates4,
                            rootSignature, pixelShaderInput, solidPixelShader, renderTarget);
                } else {
                    cullThirdTop(pos1, pos2, pos3, pos4, interpolates1, interpolates2, interpolates3, interpolates4,
                            interpolates5, rootSignature, pixelShaderInput, solidPixelShader, renderTarget);
                }
            } else {
                if(pos3.y >= 0) {
                    cullThirdTop(pos1, pos3, pos2, pos4, interpolates1, interpolates3, interpolates2, interpolates4,
                            interpolates5, rootSignature, pixelShaderInput, solidPixelShader, renderTarget);
                } else {
                    cullSecondAndThirdTop(pos1, pos2, pos3, interpolates1, interpolates2, interpolates3,
                            interpolates4, rootSignature, pixelShaderInput, solidPixelShader, renderTarget);
                }
            }
        } else {
            if(pos2.y >= 0) {
                if(pos3.y >= 0) {
                    cullThirdTop(pos2, pos3, pos1, pos4, interpolates2, interpolates3, interpolates1, interpolates4,
                            interpolates5, rootSignature, pixelShaderInput, solidPixelShader, renderTarget);
                } else {
                    cullSecondAndThirdTop(pos2, pos1, pos3, interpolates2, interpolates1, interpolates3,
                            interpolates4, rootSignature, pixelShaderInput, solidPixelShader, renderTarget);
                }
            } else {
                if(pos3.y >= 0) {
                    cullSecondAndThirdTop(pos3, pos1, pos2, interpolates3, interpolates1, interpolates2,
                            interpolates4, rootSignature, pixelShaderInput, solidPixelShader, renderTarget);
                }
            }
        }
    }

    private static void cullTriangleBottom(Vector4 pos1, Vector4 pos2, Vector4 pos3, Vector4[] interpolates1,
                                           Vector4[] interpolates2, Vector4[] interpolates3, Vector4[] interpolates4,
                                           Object[] rootSignature, Vector4[] pixelShaderInput, PixelShader solidPixelShader,
                                           BufferedImage renderTarget) {
        Vector4[] interpolates5 = new Vector4[interpolates1.length];
        for(int i = 0; i < interpolates5.length; ++i) {
            interpolates5[i] = new Vector4();
        }
        Vector4 pos4 = new Vector4();
        final int heightMinusOne = renderTarget.getHeight() - 1;
        if(pos1.y <= heightMinusOne) {
            if(pos2.y <= heightMinusOne) {
                if(pos3.y <= heightMinusOne) {
                    cullTriangleLeft(pos1, pos2, pos3, interpolates1, interpolates2, interpolates3, interpolates4,
                            rootSignature, pixelShaderInput, solidPixelShader, renderTarget);
                } else {
                    cullThirdBottom(pos1, pos2, pos3, pos4, interpolates1, interpolates2, interpolates3, interpolates4,
                            interpolates5, rootSignature, pixelShaderInput, solidPixelShader, renderTarget);
                }
            } else {
                if(pos3.y <= heightMinusOne) {
                    cullThirdBottom(pos1, pos3, pos2, pos4, interpolates1, interpolates3, interpolates2, interpolates4,
                            interpolates5, rootSignature, pixelShaderInput, solidPixelShader, renderTarget);
                } else {
                    cullSecondAndThirdBottom(pos1, pos2, pos3, interpolates1, interpolates2, interpolates3,
                            interpolates4, rootSignature, pixelShaderInput, solidPixelShader, renderTarget);
                }
            }
        } else {
            if(pos2.y <= heightMinusOne) {
                if(pos3.y <= heightMinusOne) {
                    cullThirdBottom(pos2, pos3, pos1, pos4, interpolates2, interpolates3, interpolates1, interpolates4,
                            interpolates5, rootSignature, pixelShaderInput, solidPixelShader, renderTarget);
                } else {
                    cullSecondAndThirdBottom(pos2, pos1, pos3, interpolates2, interpolates1, interpolates3,
                            interpolates4, rootSignature, pixelShaderInput, solidPixelShader, renderTarget);
                }
            } else {
                if(pos3.y <= heightMinusOne) {
                    cullSecondAndThirdBottom(pos3, pos1, pos2, interpolates3, interpolates1, interpolates2,
                            interpolates4, rootSignature, pixelShaderInput, solidPixelShader, renderTarget);
                }
            }
        }
    }

    private static void cullTriangleLeft(Vector4 pos1, Vector4 pos2, Vector4 pos3, Vector4[] interpolates1,
                                           Vector4[] interpolates2, Vector4[] interpolates3, Vector4[] interpolates4,
                                           Object[] rootSignature, Vector4[] pixelShaderInput, PixelShader solidPixelShader,
                                           BufferedImage renderTarget) {
        Vector4[] interpolates5 = new Vector4[interpolates1.length];
        for(int i = 0; i < interpolates5.length; ++i) {
            interpolates5[i] = new Vector4();
        }
        Vector4 pos4 = new Vector4();

        if(pos1.x >= 0) {
            if(pos2.x >= 0) {
                if(pos3.x >= 0) {
                    cullTriangleRight(pos1, pos2, pos3, interpolates1, interpolates2, interpolates3, interpolates4,
                            rootSignature, pixelShaderInput, solidPixelShader, renderTarget);
                } else {
                    cullThirdLeft(pos1, pos2, pos3, pos4, interpolates1, interpolates2, interpolates3, interpolates4,
                            interpolates5, rootSignature, pixelShaderInput, solidPixelShader, renderTarget);
                }
            } else {
                if(pos3.x >= 0) {
                    cullThirdLeft(pos1, pos3, pos2, pos4, interpolates1, interpolates3, interpolates2, interpolates4,
                            interpolates5, rootSignature, pixelShaderInput, solidPixelShader, renderTarget);
                } else {
                    cullSecondAndThirdLeft(pos1, pos2, pos3, interpolates1, interpolates2, interpolates3,
                            interpolates4, rootSignature, pixelShaderInput, solidPixelShader, renderTarget);
                }
            }
        } else {
            if(pos2.x >= 0) {
                if(pos3.x >= 0) {
                    cullThirdLeft(pos2, pos3, pos1, pos4, interpolates2, interpolates3, interpolates1, interpolates4,
                            interpolates5, rootSignature, pixelShaderInput, solidPixelShader, renderTarget);
                } else {
                    cullSecondAndThirdLeft(pos2, pos1, pos3, interpolates2, interpolates1, interpolates3,
                            interpolates4, rootSignature, pixelShaderInput, solidPixelShader, renderTarget);
                }
            } else {
                if(pos3.x >= 0) {
                    cullSecondAndThirdLeft(pos3, pos1, pos2, interpolates3, interpolates1, interpolates2,
                            interpolates4, rootSignature, pixelShaderInput, solidPixelShader, renderTarget);
                }
            }
        }
    }

    private static void cullTriangleRight(Vector4 pos1, Vector4 pos2, Vector4 pos3, Vector4[] interpolates1,
                                         Vector4[] interpolates2, Vector4[] interpolates3, Vector4[] interpolates4,
                                         Object[] rootSignature, Vector4[] pixelShaderInput, PixelShader solidPixelShader,
                                         BufferedImage renderTarget) {
        final int widthMinusOne = renderTarget.getWidth() - 1;
        Vector4[] interpolates5 = new Vector4[interpolates1.length];
        for(int i = 0; i < interpolates5.length; ++i) {
            interpolates5[i] = new Vector4();
        }
        Vector4 pos4 = new Vector4();

        if(pos1.x <= widthMinusOne) {
            if(pos2.x <= widthMinusOne) {
                if(pos3.x <= widthMinusOne) {
                    drawTriangle(pos1, pos2, pos3, interpolates1, interpolates2, interpolates3, interpolates4,
                            rootSignature, pixelShaderInput, solidPixelShader, renderTarget);
                } else {
                    cullThirdRight(pos1, pos2, pos3, pos4, interpolates1, interpolates2, interpolates3, interpolates4,
                            interpolates5, rootSignature, pixelShaderInput, solidPixelShader, renderTarget);
                }
            } else {
                if(pos3.x <= widthMinusOne) {
                    cullThirdRight(pos1, pos3, pos2, pos4, interpolates1, interpolates3, interpolates2, interpolates4,
                            interpolates5, rootSignature, pixelShaderInput, solidPixelShader, renderTarget);
                } else {
                    cullSecondAndThirdRight(pos1, pos2, pos3, interpolates1, interpolates2, interpolates3,
                            interpolates4, rootSignature, pixelShaderInput, solidPixelShader, renderTarget);
                }
            }
        } else {
            if(pos2.x <= widthMinusOne) {
                if(pos3.x <= widthMinusOne) {
                    cullThirdRight(pos2, pos3, pos1, pos4, interpolates2, interpolates3, interpolates1, interpolates4,
                            interpolates5, rootSignature, pixelShaderInput, solidPixelShader, renderTarget);
                } else {
                    cullSecondAndThirdRight(pos2, pos1, pos3, interpolates2, interpolates1, interpolates3,
                            interpolates4, rootSignature, pixelShaderInput, solidPixelShader, renderTarget);
                }
            } else {
                if(pos3.x <= widthMinusOne) {
                    cullSecondAndThirdRight(pos3, pos1, pos2, interpolates3, interpolates1, interpolates2,
                            interpolates4, rootSignature, pixelShaderInput, solidPixelShader, renderTarget);
                }
            }
        }
    }

    /**
     * draws a triangle that is fully an screen
     */
    private static void drawTriangle(Vector4 pos1, Vector4 pos2, Vector4 pos3, Vector4[] interpolates1,
                                     Vector4[] interpolates2, Vector4[] interpolates3, Vector4[] interpolates4,
                                     Object[] rootSignature, Vector4[] pixelShaderInput, PixelShader solidPixelShader,
                                     BufferedImage renderTarget) {
        int maxX, maxY, midX, midY, minX, minY;
        Vector4[] maxInterpolate, midInterpolate, minInterpolate;
        if (pos1.y >= pos2.y) {
            if (pos1.y >= pos3.y) {
                maxX = (int) pos1.x;
                maxY = (int) pos1.y;
                maxInterpolate = interpolates1;
                if (pos2.y >= pos3.y) {
                    midX = (int) pos2.x;
                    midY = (int) pos2.y;
                    midInterpolate = interpolates2;

                    minX = (int) pos3.x;
                    minY = (int) pos3.y;
                    minInterpolate = interpolates3;
                } else {
                    midX = (int) pos3.x;
                    midY = (int) pos3.y;
                    midInterpolate = interpolates3;

                    minX = (int) pos2.x;
                    minY = (int) pos2.y;
                    minInterpolate = interpolates2;
                }
            } else {
                maxX = (int) pos3.x;
                maxY = (int) pos3.y;
                maxInterpolate = interpolates3;

                midX = (int) pos1.x;
                midY = (int) pos1.y;
                midInterpolate = interpolates1;

                minX = (int) pos2.x;
                minY = (int) pos2.y;
                minInterpolate = interpolates2;
            }
        } else {
            if (pos2.y >= pos3.y) {
                maxX = (int) pos2.x;
                maxY = (int) pos2.y;
                maxInterpolate = interpolates2;
                if (pos1.y >= pos3.y) {
                    midX = (int) pos1.x;
                    midY = (int) pos1.y;
                    midInterpolate = interpolates1;

                    minX = (int) pos3.x;
                    minY = (int) pos3.y;
                    minInterpolate = interpolates3;
                } else {
                    midX = (int) pos3.x;
                    midY = (int) pos3.y;
                    midInterpolate = interpolates3;

                    minX = (int) pos1.x;
                    minY = (int) pos1.y;
                    minInterpolate = interpolates1;
                }
            } else {
                maxX = (int) pos3.x;
                maxY = (int) pos3.y;
                maxInterpolate = interpolates3;

                midX = (int) pos2.x;
                midY = (int) pos2.y;
                midInterpolate = interpolates2;

                minX = (int) pos1.x;
                minY = (int) pos1.y;
                minInterpolate = interpolates1;
            }
        }

        if (maxY == minY) {
            if (midX < minX) {
                int temp = minX;
                minX = midX;
                midX = temp;

                Vector4[] temp2 = minInterpolate;
                minInterpolate = midInterpolate;
                midInterpolate = temp2;
            }
            if (maxX < midX) {
                int temp = midX;
                midX = maxX;
                maxX = temp;

                Vector4[] temp2 = midInterpolate;
                midInterpolate = maxInterpolate;
                maxInterpolate = temp2;
            }
            if (midX < minX) {
                int temp = minX;
                minX = midX;
                midX = temp;

                Vector4[] temp2 = minInterpolate;
                minInterpolate = midInterpolate;
                midInterpolate = temp2;
            }
            final float dx = 1f / (midX - minX);
            for (int lineStart = minX; lineStart < midX; ++lineStart) {
                lerp(pixelShaderInput, minInterpolate, midInterpolate, (float) (lineStart - minX) * dx);
                renderTarget.setRGB(lineStart, maxY, solidPixelShader.run(rootSignature, pixelShaderInput));
            }
            final float dx2 = 1f / (maxX - midX);
            for (int lineStart = midX; lineStart <= maxX; ++lineStart) {
                lerp(pixelShaderInput, midInterpolate, maxInterpolate, (float) (lineStart - midX) * dx2);
                renderTarget.setRGB(lineStart, maxY, solidPixelShader.run(rootSignature, pixelShaderInput));
            }
        } else {

            int newX = maxX + ((minX - maxX) * (maxY - midY)) / (maxY - minY);
            if (midX == newX) {
                //0 width triangles divide 0 by 0 when interpolating
                rasterizeLine(minX, minY, midX, midY, solidPixelShader, rootSignature, renderTarget, minInterpolate,
                        midInterpolate, pixelShaderInput);
                rasterizeLine(midX, midY, maxX, maxY, solidPixelShader, rootSignature, renderTarget, midInterpolate,
                        maxInterpolate, pixelShaderInput);
            } else if (maxY == midY) {
                if (maxX < midX) {
                    int temp = midX;
                    midX = maxX;
                    maxX = temp;

                    Vector4[] temp2 = midInterpolate;
                    midInterpolate = maxInterpolate;
                    maxInterpolate = temp2;
                }
                final float dx = 1f / (maxX - midX);
                for (int lineStart = midX; lineStart <= maxX; ++lineStart) {
                    lerp(pixelShaderInput, midInterpolate, maxInterpolate, (float) (lineStart - midX) * dx);
                    renderTarget.setRGB(lineStart, midY, solidPixelShader.run(rootSignature, pixelShaderInput));
                }
                rasterizeTopFlatTriangle(midX, midY, minX, minY, maxX, solidPixelShader, rootSignature,
                        renderTarget, midInterpolate, maxInterpolate, minInterpolate, pixelShaderInput);
            } else if (minY == midY) {
                if (minX >= midX) {
                    rasterizeBottomFlatTriangle(midX, midY, maxX, maxY, minX, solidPixelShader, rootSignature,
                            renderTarget, midInterpolate, minInterpolate, maxInterpolate, pixelShaderInput);
                } else {
                    rasterizeBottomFlatTriangle(minX, midY, maxX, maxY, midX, solidPixelShader, rootSignature,
                            renderTarget, minInterpolate, midInterpolate, maxInterpolate, pixelShaderInput);
                }
            } else {
                createNewInterpolate(interpolates4, maxInterpolate, minInterpolate, (float) (maxY - midY) / (float) (maxY - minY));

                if (midX >= newX) {
                    int temp = newX;
                    newX = midX;
                    midX = temp;

                    Vector4[] temp2 = interpolates4;
                    interpolates4 = midInterpolate;
                    midInterpolate = temp2;
                }

                rasterizeBottomFlatTriangle(midX, midY, maxX, maxY, newX, solidPixelShader, rootSignature,
                        renderTarget, midInterpolate, interpolates4, maxInterpolate, pixelShaderInput);
                rasterizeTopFlatTriangle(midX, midY, minX, minY, newX, solidPixelShader, rootSignature,
                        renderTarget, midInterpolate, interpolates4, minInterpolate, pixelShaderInput);
            }
        }
    }

    /**
     * draws a fully on screen triangle that has a bottom that is parallel to the x-axis
     */
    private static void rasterizeBottomFlatTriangle(int firstX, int minY, int maxX, int maxY, int secondX,
                                                    PixelShader pixelShader, Object[] rootSignature, BufferedImage renderTarget,
                                                    Vector4[] interpolates1, Vector4[] interpolates2, Vector4[] interpolates3,
                                                    Vector4[] pixelShaderInput) {
        int dx1 = maxX - firstX;
        int dy = maxY - minY;
        int dx2 = maxX - secondX;

        if(dy == 0) {
            final float dx = 1f / (secondX - firstX);
            for(int lineStart = firstX; lineStart <= secondX; ++lineStart) {
                lerp(pixelShaderInput, interpolates1, interpolates2, (float)(lineStart - firstX) * dx);
                renderTarget.setRGB(lineStart, minY, pixelShader.run(rootSignature, pixelShaderInput));
            }
        } else {
            int x1 = firstX;
            int x2 = secondX;
            int y = minY;
            int eps1 = 0;
            int eps2 = 0;
            if(dx1 >= 0) {
                if(dx2 >= 0) {
                    //both leaning right
                    if(dx1 >= dy) {
                        if(dx2 >= dy) {
                            while (y < maxY) {
                                while((eps1 << 1) < dx1) {
                                    lerp(firstX, minY, interpolates1, secondX, minY, interpolates2, maxX, maxY, interpolates3,
                                            x1, y, pixelShaderInput);
                                    renderTarget.setRGB(x1, y, pixelShader.run(rootSignature, pixelShaderInput));
                                    eps1 += dy;
                                    ++x1;
                                }

                                int oldx2 = x2;
                                while((eps2 << 1) < dx2) {
                                    lerp(firstX, minY, interpolates1, secondX, minY, interpolates2, maxX, maxY, interpolates3,
                                            x2, y, pixelShaderInput);
                                    renderTarget.setRGB(x2, y, pixelShader.run(rootSignature, pixelShaderInput));
                                    eps2 += dy;
                                    ++x2;
                                }

                                for(int lineStart = x1; lineStart < oldx2; ++lineStart) {
                                    lerp(firstX, minY, interpolates1, secondX, minY, interpolates2, maxX, maxY, interpolates3,
                                            lineStart, y, pixelShaderInput);
                                    renderTarget.setRGB(lineStart, y, pixelShader.run(rootSignature, pixelShaderInput));
                                }

                                ++y;
                                eps1 -= dx1;
                                eps2 -= dx2;
                            }
                            copyInterpolateToPixelShaderInput(interpolates3, pixelShaderInput);
                            renderTarget.setRGB(maxX, maxY, pixelShader.run(rootSignature, pixelShaderInput));
                        } else {
                            while (y < maxY) {
                                while((eps1 << 1) < dx1) {
                                    lerp(firstX, minY, interpolates1, secondX, minY, interpolates2, maxX, maxY, interpolates3,
                                            x1, y, pixelShaderInput);
                                    renderTarget.setRGB(x1, y, pixelShader.run(rootSignature, pixelShaderInput));
                                    eps1 += dy;
                                    ++x1;
                                }

                                int oldx2 = x2;
                                lerp(firstX, minY, interpolates1, secondX, minY, interpolates2, maxX, maxY, interpolates3,
                                        x2, y, pixelShaderInput);
                                renderTarget.setRGB(x2, y, pixelShader.run(rootSignature, pixelShaderInput));
                                eps2 += dx2;
                                if ((eps2 << 1) >= dy) {
                                    ++x2;
                                    eps2 -= dy;
                                }

                                for(int lineStart = x1; lineStart < oldx2; ++lineStart) {
                                    lerp(firstX, minY, interpolates1, secondX, minY, interpolates2, maxX, maxY, interpolates3,
                                            lineStart, y, pixelShaderInput);
                                    renderTarget.setRGB(lineStart, y, pixelShader.run(rootSignature, pixelShaderInput));
                                }

                                ++y;
                                eps1 -= dx1;
                            }
                        }
                        copyInterpolateToPixelShaderInput(interpolates3, pixelShaderInput);
                        renderTarget.setRGB(maxX, maxY, pixelShader.run(rootSignature, pixelShaderInput));
                    } else {
                        if (dx2 >= dy) {
                            while (y < maxY) {
                                lerp(firstX, minY, interpolates1, secondX, minY, interpolates2, maxX, maxY, interpolates3,
                                        x1, y, pixelShaderInput);
                                renderTarget.setRGB(x1, y, pixelShader.run(rootSignature, pixelShaderInput));
                                eps1 += dx1;
                                if ((eps1 << 1) >= dy) {
                                    ++x1;
                                    eps1 -= dy;
                                }

                                int oldx2 = x2;
                                while((eps2 << 1) < dx2) {
                                    lerp(firstX, minY, interpolates1, secondX, minY, interpolates2, maxX, maxY, interpolates3,
                                            x2, y, pixelShaderInput);
                                    renderTarget.setRGB(x2, y, pixelShader.run(rootSignature, pixelShaderInput));
                                    eps2 += dy;
                                    ++x2;
                                }

                                for (int lineStart = x1; lineStart < oldx2; ++lineStart) {
                                    lerp(firstX, minY, interpolates1, secondX, minY, interpolates2, maxX, maxY, interpolates3,
                                            lineStart, y, pixelShaderInput);
                                    renderTarget.setRGB(lineStart, y, pixelShader.run(rootSignature, pixelShaderInput));
                                }

                                ++y;
                                eps2 -= dx2;
                            }
                            copyInterpolateToPixelShaderInput(interpolates3, pixelShaderInput);
                            renderTarget.setRGB(maxX, maxY, pixelShader.run(rootSignature, pixelShaderInput));
                        } else {
                            while (y < maxY) {
                                lerp(firstX, minY, interpolates1, secondX, minY, interpolates2, maxX, maxY, interpolates3,
                                        x1, y, pixelShaderInput);
                                renderTarget.setRGB(x1, y, pixelShader.run(rootSignature, pixelShaderInput));
                                eps1 += dx1;
                                if ((eps1 << 1) >= dy) {
                                    ++x1;
                                    eps1 -= dy;
                                }

                                int oldx2 = x2;
                                lerp(firstX, minY, interpolates1, secondX, minY, interpolates2, maxX, maxY, interpolates3,
                                        x2, y, pixelShaderInput);
                                renderTarget.setRGB(x2, y, pixelShader.run(rootSignature, pixelShaderInput));
                                eps2 += dx2;
                                if ((eps2 << 1) >= dy) {
                                    ++x2;
                                    eps2 -= dy;
                                }

                                for (int lineStart = x1; lineStart < oldx2; ++lineStart) {
                                    lerp(firstX, minY, interpolates1, secondX, minY, interpolates2, maxX, maxY, interpolates3,
                                            lineStart, y, pixelShaderInput);
                                    renderTarget.setRGB(lineStart, y, pixelShader.run(rootSignature, pixelShaderInput));
                                }

                                ++y;
                            }
                            copyInterpolateToPixelShaderInput(interpolates3, pixelShaderInput);
                            renderTarget.setRGB(maxX, maxY, pixelShader.run(rootSignature, pixelShaderInput));
                        }
                    }
                } else {
                    //both leaning in
                    if (dx1 >= dy) {
                        if (-dx2 >= dy) {
                            while (y < maxY) {
                                while ((eps1 << 1) < dx1) {
                                    lerp(firstX, minY, interpolates1, secondX, minY, interpolates2, maxX, maxY, interpolates3,
                                            x1, y, pixelShaderInput);
                                    renderTarget.setRGB(x1, y, pixelShader.run(rootSignature, pixelShaderInput));
                                    eps1 += dy;
                                    ++x1;
                                }

                                while ((eps2 << 1) > dx2) {
                                    lerp(firstX, minY, interpolates1, secondX, minY, interpolates2, maxX, maxY, interpolates3,
                                            x2, y, pixelShaderInput);
                                    renderTarget.setRGB(x2, y, pixelShader.run(rootSignature, pixelShaderInput));
                                    eps2 -= dy;
                                    --x2;
                                }

                                for (int lineStart = x1; lineStart <= x2; ++lineStart) {
                                    lerp(firstX, minY, interpolates1, secondX, minY, interpolates2, maxX, maxY, interpolates3,
                                            lineStart, y, pixelShaderInput);
                                    renderTarget.setRGB(lineStart, y, pixelShader.run(rootSignature, pixelShaderInput));
                                }

                                ++y;
                                eps1 -= dx1;
                                eps2 -= dx2;
                            }
                            copyInterpolateToPixelShaderInput(interpolates3, pixelShaderInput);
                            renderTarget.setRGB(maxX, maxY, pixelShader.run(rootSignature, pixelShaderInput));
                        } else {
                            while (y < maxY) {
                                while ((eps1 << 1) < dx1) {
                                    lerp(firstX, minY, interpolates1, secondX, minY, interpolates2, maxX, maxY, interpolates3,
                                            x1, y, pixelShaderInput);
                                    renderTarget.setRGB(x1, y, pixelShader.run(rootSignature, pixelShaderInput));
                                    eps1 += dy;
                                    ++x1;
                                }

                                lerp(firstX, minY, interpolates1, secondX, minY, interpolates2, maxX, maxY, interpolates3,
                                        x2, y, pixelShaderInput);
                                renderTarget.setRGB(x2, y, pixelShader.run(rootSignature, pixelShaderInput));
                                eps2 -= dx2;
                                if ((eps2 << 1) >= dy) {
                                    --x2;
                                    eps2 -= dy;
                                }

                                for (int lineStart = x1; lineStart <= x2; ++lineStart) {
                                    lerp(firstX, minY, interpolates1, secondX, minY, interpolates2, maxX, maxY, interpolates3,
                                            lineStart, y, pixelShaderInput);
                                    renderTarget.setRGB(lineStart, y, pixelShader.run(rootSignature, pixelShaderInput));
                                }

                                ++y;
                                eps1 -= dx1;
                            }
                            copyInterpolateToPixelShaderInput(interpolates3, pixelShaderInput);
                            renderTarget.setRGB(maxX, maxY, pixelShader.run(rootSignature, pixelShaderInput));
                        }
                    } else {
                        if (-dx2 >= dy) {
                            while (y < maxY) {
                                lerp(firstX, minY, interpolates1, secondX, minY, interpolates2, maxX, maxY, interpolates3,
                                        x1, y, pixelShaderInput);
                                renderTarget.setRGB(x1, y, pixelShader.run(rootSignature, pixelShaderInput));
                                eps1 += dx1;
                                if ((eps1 << 1) >= dy) {
                                    ++x1;
                                    eps1 -= dy;
                                }

                                while ((eps2 << 1) > dx2) {
                                    lerp(firstX, minY, interpolates1, secondX, minY, interpolates2, maxX, maxY, interpolates3,
                                            x2, y, pixelShaderInput);
                                    renderTarget.setRGB(x2, y, pixelShader.run(rootSignature, pixelShaderInput));
                                    eps2 -= dy;
                                    --x2;
                                }

                                for (int lineStart = x1; lineStart <= x2; ++lineStart) {
                                    lerp(firstX, minY, interpolates1, secondX, minY, interpolates2, maxX, maxY, interpolates3,
                                            lineStart, y, pixelShaderInput);
                                    renderTarget.setRGB(lineStart, y, pixelShader.run(rootSignature, pixelShaderInput));
                                }

                                ++y;
                                eps2 -= dx2;
                            }
                            copyInterpolateToPixelShaderInput(interpolates3, pixelShaderInput);
                            renderTarget.setRGB(maxX, maxY, pixelShader.run(rootSignature, pixelShaderInput));
                        } else {
                            while (y < maxY) {
                                lerp(firstX, minY, interpolates1, secondX, minY, interpolates2, maxX, maxY, interpolates3,
                                        x1, y, pixelShaderInput);
                                renderTarget.setRGB(x1, y, pixelShader.run(rootSignature, pixelShaderInput));
                                eps1 += dx1;
                                if ((eps1 << 1) >= dy) {
                                    ++x1;
                                    eps1 -= dy;
                                }

                                lerp(firstX, minY, interpolates1, secondX, minY, interpolates2, maxX, maxY, interpolates3,
                                        x2, y, pixelShaderInput);
                                renderTarget.setRGB(x2, y, pixelShader.run(rootSignature, pixelShaderInput));
                                eps2 -= dx2;
                                if ((eps2 << 1) >= dy) {
                                    --x2;
                                    eps2 -= dy;
                                }

                                for (int lineStart = x1; lineStart <= x2; ++lineStart) {
                                    lerp(firstX, minY, interpolates1, secondX, minY, interpolates2, maxX, maxY, interpolates3,
                                            lineStart, y, pixelShaderInput);
                                    renderTarget.setRGB(lineStart, y, pixelShader.run(rootSignature, pixelShaderInput));
                                }

                                ++y;
                            }
                            copyInterpolateToPixelShaderInput(interpolates3, pixelShaderInput);
                            renderTarget.setRGB(maxX, maxY, pixelShader.run(rootSignature, pixelShaderInput));
                        }
                    }
                }
            } else {
                    //both leaning left
                if(-dx1 >= dy) {
                    if(-dx2 >= dy) {
                        while (y < maxY) {
                            int oldx1 = x1;
                            while((eps1 << 1) > dx1) {
                                lerp(firstX, minY, interpolates1, secondX, minY, interpolates2, maxX, maxY, interpolates3,
                                        x1, y, pixelShaderInput);
                                renderTarget.setRGB(x1, y, pixelShader.run(rootSignature, pixelShaderInput));
                                eps1 -= dy;
                                --x1;
                            }

                            while((eps2 << 1) > dx2) {
                                lerp(firstX, minY, interpolates1, secondX, minY, interpolates2, maxX, maxY, interpolates3,
                                        x2, y, pixelShaderInput);
                                renderTarget.setRGB(x2, y, pixelShader.run(rootSignature, pixelShaderInput));
                                eps2 -= dy;
                                --x2;
                            }

                            for(int lineStart = oldx1 + 1; lineStart <= x2; ++lineStart) {
                                lerp(firstX, minY, interpolates1, secondX, minY, interpolates2, maxX, maxY, interpolates3,
                                        lineStart, y, pixelShaderInput);
                                renderTarget.setRGB(lineStart, y, pixelShader.run(rootSignature, pixelShaderInput));
                            }

                            ++y;
                            eps1 -= dx1;
                            eps2 -= dx2;
                        }
                        copyInterpolateToPixelShaderInput(interpolates3, pixelShaderInput);
                        renderTarget.setRGB(maxX, maxY, pixelShader.run(rootSignature, pixelShaderInput));
                    } else {
                        while (y < maxY) {
                            int oldx1 = x1;
                            while((eps1 << 1) > dx1) {
                                lerp(firstX, minY, interpolates1, secondX, minY, interpolates2, maxX, maxY, interpolates3,
                                        x1, y, pixelShaderInput);
                                renderTarget.setRGB(x1, y, pixelShader.run(rootSignature, pixelShaderInput));
                                eps1 -= dy;
                                --x1;
                            }

                            lerp(firstX, minY, interpolates1, secondX, minY, interpolates2, maxX, maxY, interpolates3,
                                    x2, y, pixelShaderInput);
                            renderTarget.setRGB(x2, y, pixelShader.run(rootSignature, pixelShaderInput));
                            eps2 -= dx2;
                            if ((eps2 << 1) >= dy) {
                                --x2;
                                eps2 -= dy;
                            }

                            for(int lineStart = oldx1 + 1; lineStart <= x2; ++lineStart) {
                                lerp(firstX, minY, interpolates1, secondX, minY, interpolates2, maxX, maxY, interpolates3,
                                        lineStart, y, pixelShaderInput);
                                renderTarget.setRGB(lineStart, y, pixelShader.run(rootSignature, pixelShaderInput));
                            }

                            ++y;
                            eps1 -= dx1;
                        }
                        copyInterpolateToPixelShaderInput(interpolates3, pixelShaderInput);
                        renderTarget.setRGB(maxX, maxY, pixelShader.run(rootSignature, pixelShaderInput));
                    }
                } else {
                    if(-dx2 >= dy) {
                        while (y < maxY) {
                            int oldx1 = x1;
                            lerp(firstX, minY, interpolates1, secondX, minY, interpolates2, maxX, maxY, interpolates3,
                                    x1, y, pixelShaderInput);
                            renderTarget.setRGB(x1, y, pixelShader.run(rootSignature, pixelShaderInput));
                            eps1 -= dx1;
                            if ((eps1 << 1) >= dy) {
                                --x1;
                                eps1 -= dy;
                            }

                            while((eps2 << 1) > dx2) {
                                lerp(firstX, minY, interpolates1, secondX, minY, interpolates2, maxX, maxY, interpolates3,
                                        x2, y, pixelShaderInput);
                                renderTarget.setRGB(x2, y, pixelShader.run(rootSignature, pixelShaderInput));
                                eps2 -= dy;
                                --x2;
                            }

                            for(int lineStart = oldx1 + 1; lineStart <= x2; ++lineStart) {
                                lerp(firstX, minY, interpolates1, secondX, minY, interpolates2, maxX, maxY, interpolates3,
                                        lineStart, y, pixelShaderInput);
                                renderTarget.setRGB(lineStart, y, pixelShader.run(rootSignature, pixelShaderInput));
                            }

                            ++y;
                            eps2 -= dx2;
                        }
                        copyInterpolateToPixelShaderInput(interpolates3, pixelShaderInput);
                        renderTarget.setRGB(maxX, maxY, pixelShader.run(rootSignature, pixelShaderInput));
                    } else {
                        while (y < maxY) {
                            int oldx1 = x1;
                            lerp(firstX, minY, interpolates1, secondX, minY, interpolates2, maxX, maxY, interpolates3,
                                    x1, y, pixelShaderInput);
                            renderTarget.setRGB(x1, y, pixelShader.run(rootSignature, pixelShaderInput));
                            eps1 -= dx1;
                            if ((eps1 << 1) >= dy) {
                                --x1;
                                eps1 -= dy;
                            }

                            lerp(firstX, minY, interpolates1, secondX, minY, interpolates2, maxX, maxY, interpolates3,
                                    x2, y, pixelShaderInput);
                            renderTarget.setRGB(x2, y, pixelShader.run(rootSignature, pixelShaderInput));
                            eps2 -= dx2;
                            if ((eps2 << 1) >= dy) {
                                --x2;
                                eps2 -= dy;
                            }

                            for(int lineStart = oldx1 + 1; lineStart <= x2; ++lineStart) {
                                lerp(firstX, minY, interpolates1, secondX, minY, interpolates2, maxX, maxY, interpolates3,
                                        lineStart, y, pixelShaderInput);
                                renderTarget.setRGB(lineStart, y, pixelShader.run(rootSignature, pixelShaderInput));
                            }

                            ++y;
                        }
                        copyInterpolateToPixelShaderInput(interpolates3, pixelShaderInput);
                        renderTarget.setRGB(maxX, maxY, pixelShader.run(rootSignature, pixelShaderInput));
                    }
                }
            }
        }
    }

    /**
     * draws a fully on screen triangle that has a top that is parallel to the x-axis
     */
    private static void rasterizeTopFlatTriangle(int firstX, int maxY, int minX, int minY, int secondX,
                                                 PixelShader pixelShader, Object[] rootSignature, BufferedImage renderTarget,
                                                 Vector4[] interpolates1, Vector4[] interpolates2, Vector4[] interpolates3,
                                                 Vector4[] pixelShaderInput) {

        int dx1 = minX - firstX;
        int dy = minY - maxY;
        int dx2 = minX - secondX;

        if(dy == 0) {
            final float dx = 1f / (secondX - firstX);
            for(int lineStart = firstX; lineStart <= secondX; ++lineStart) {
                lerp(pixelShaderInput, interpolates1, interpolates2, (float)(lineStart - firstX) * dx);
                renderTarget.setRGB(lineStart, minY, pixelShader.run(rootSignature, pixelShaderInput));
            }
        } else {
            int x1 = minX;
            int x2 = minX;
            int y = minY;
            int eps1 = 0;
            int eps2 = 0;
            if(dx1 >= 0) {
                if(dx2 >= 0) {
                    //both leaning right
                    if(dx1 >= -dy) {
                        if(dx2 >= -dy) {
                            while (y < maxY) {
                                int oldx1 = x1;
                                while((eps1 << 1) < dx1) {
                                    lerp(firstX, maxY, interpolates1, secondX, maxY, interpolates2, minX, minY, interpolates3,
                                            x1, y, pixelShaderInput);
                                    renderTarget.setRGB(x1, y, pixelShader.run(rootSignature, pixelShaderInput));
                                    eps1 -= dy;
                                    --x1;
                                }

                                while((eps2 << 1) < dx2) {
                                    lerp(firstX, maxY, interpolates1, secondX, maxY, interpolates2, minX, minY, interpolates3,
                                            x2, y, pixelShaderInput);
                                    renderTarget.setRGB(x2, y, pixelShader.run(rootSignature, pixelShaderInput));
                                    eps2 -= dy;
                                    --x2;
                                }

                                for(int lineStart = oldx1 + 1; lineStart <= x2; ++lineStart) {
                                    lerp(firstX, maxY, interpolates1, secondX, maxY, interpolates2, minX, minY, interpolates3,
                                            lineStart, y, pixelShaderInput);
                                    renderTarget.setRGB(lineStart, y, pixelShader.run(rootSignature, pixelShaderInput));
                                }

                                ++y;
                                eps1 -= dx1;
                                eps2 -= dx2;
                            }
                        } else {
                            while (y < maxY) {
                                int oldx1 = x1;
                                while((eps1 << 1) < dx1) {
                                    lerp(firstX, maxY, interpolates1, secondX, maxY, interpolates2, minX, minY, interpolates3,
                                            x1, y, pixelShaderInput);
                                    renderTarget.setRGB(x1, y, pixelShader.run(rootSignature, pixelShaderInput));
                                    eps1 -= dy;
                                    --x1;
                                }

                                lerp(firstX, maxY, interpolates1, secondX, maxY, interpolates2, minX, minY, interpolates3,
                                        x2, y, pixelShaderInput);
                                renderTarget.setRGB(x2, y, pixelShader.run(rootSignature, pixelShaderInput));
                                eps2 += dx2;
                                if ((eps2 << 1) >= -dy) {
                                    --x2;
                                    eps2 += dy;
                                }

                                for(int lineStart = oldx1 + 1; lineStart <= x2; ++lineStart) {
                                    lerp(firstX, maxY, interpolates1, secondX, maxY, interpolates2, minX, minY, interpolates3,
                                            lineStart, y, pixelShaderInput);
                                    renderTarget.setRGB(lineStart, y, pixelShader.run(rootSignature, pixelShaderInput));
                                }

                                ++y;
                                eps1 -= dx1;
                            }
                        }
                    } else {
                        if (dx2 >= -dy) {
                            while (y < maxY) {
                                int oldx1 = x1;
                                lerp(firstX, maxY, interpolates1, secondX, maxY, interpolates2, minX, minY, interpolates3,
                                        x1, y, pixelShaderInput);
                                renderTarget.setRGB(x1, y, pixelShader.run(rootSignature, pixelShaderInput));
                                eps1 += dx1;
                                if ((eps1 << 1) >= -dy) {
                                    --x1;
                                    eps1 += dy;
                                }

                                while((eps2 << 1) < dx2) {
                                    lerp(firstX, maxY, interpolates1, secondX, maxY, interpolates2, minX, minY, interpolates3,
                                            x2, y, pixelShaderInput);
                                    renderTarget.setRGB(x2, y, pixelShader.run(rootSignature, pixelShaderInput));
                                    eps2 -= dy;
                                    --x2;
                                }

                                for (int lineStart = oldx1 + 1; lineStart <= x2; ++lineStart) {
                                    lerp(firstX, maxY, interpolates1, secondX, maxY, interpolates2, minX, minY, interpolates3,
                                            lineStart, y, pixelShaderInput);
                                    renderTarget.setRGB(lineStart, y, pixelShader.run(rootSignature, pixelShaderInput));
                                }

                                ++y;
                                eps2 -= dx2;
                            }
                        } else {
                            while (y < maxY) {
                                int oldx1 = x1;
                                lerp(firstX, maxY, interpolates1, secondX, maxY, interpolates2, minX, minY, interpolates3,
                                        x1, y, pixelShaderInput);
                                renderTarget.setRGB(x1, y, pixelShader.run(rootSignature, pixelShaderInput));
                                eps1 += dx1;
                                if ((eps1 << 1) >= -dy) {
                                    --x1;
                                    eps1 += dy;
                                }

                                lerp(firstX, maxY, interpolates1, secondX, maxY, interpolates2, minX, minY, interpolates3,
                                        x2, y, pixelShaderInput);
                                renderTarget.setRGB(x2, y, pixelShader.run(rootSignature, pixelShaderInput));
                                eps2 += dx2;
                                if ((eps2 << 1) >= -dy) {
                                    --x2;
                                    eps2 += dy;
                                }

                                for (int lineStart = oldx1 + 1; lineStart <= x2; ++lineStart) {
                                    lerp(firstX, maxY, interpolates1, secondX, maxY, interpolates2, minX, minY, interpolates3,
                                            lineStart, y, pixelShaderInput);
                                    renderTarget.setRGB(lineStart, y, pixelShader.run(rootSignature, pixelShaderInput));
                                }

                                ++y;
                            }
                        }
                    }
                } else {
                    //both leaning in
                    if (dx1 >= -dy) {
                        if (-dx2 >= -dy) {
                            while (y < maxY) {
                                int oldx1 = x1;
                                while ((eps1 << 1) < dx1) {
                                    lerp(firstX, maxY, interpolates1, secondX, maxY, interpolates2, minX, minY, interpolates3,
                                            x1, y, pixelShaderInput);
                                    renderTarget.setRGB(x1, y, pixelShader.run(rootSignature, pixelShaderInput));
                                    eps1 -= dy;
                                    --x1;
                                }

                                int oldx2 = x2;
                                while ((eps2 << 1) > dx2) {
                                    lerp(firstX, maxY, interpolates1, secondX, maxY, interpolates2, minX, minY, interpolates3,
                                            x2, y, pixelShaderInput);
                                    renderTarget.setRGB(x2, y, pixelShader.run(rootSignature, pixelShaderInput));
                                    eps2 += dy;
                                    ++x2;
                                }

                                for (int lineStart = oldx1 + 1; lineStart < oldx2; ++lineStart) {
                                    lerp(firstX, maxY, interpolates1, secondX, maxY, interpolates2, minX, minY, interpolates3,
                                            lineStart, y, pixelShaderInput);
                                    renderTarget.setRGB(lineStart, y, pixelShader.run(rootSignature, pixelShaderInput));
                                }

                                ++y;
                                eps1 -= dx1;
                                eps2 -= dx2;
                            }
                        } else {
                            while (y < maxY) {
                                int oldx1 = x1;
                                while ((eps1 << 1) < dx1) {
                                    lerp(firstX, maxY, interpolates1, secondX, maxY, interpolates2, minX, minY, interpolates3,
                                            x1, y, pixelShaderInput);
                                    renderTarget.setRGB(x1, y, pixelShader.run(rootSignature, pixelShaderInput));
                                    eps1 -= dy;
                                    --x1;
                                }

                                int oldx2 = x2;
                                lerp(firstX, maxY, interpolates1, secondX, maxY, interpolates2, minX, minY, interpolates3,
                                        x2, y, pixelShaderInput);
                                renderTarget.setRGB(x2, y, pixelShader.run(rootSignature, pixelShaderInput));
                                eps2 -= dx2;
                                if ((eps2 << 1) >= -dy) {
                                    ++x2;
                                    eps2 += dy;
                                }

                                for (int lineStart = oldx1 + 1; lineStart < oldx2; ++lineStart) {
                                    lerp(firstX, maxY, interpolates1, secondX, maxY, interpolates2, minX, minY, interpolates3,
                                            lineStart, y, pixelShaderInput);
                                    renderTarget.setRGB(lineStart, y, pixelShader.run(rootSignature, pixelShaderInput));
                                }

                                ++y;
                                eps1 -= dx1;
                            }
                        }
                    } else {
                        if (-dx2 >= -dy) {
                            while (y < maxY) {
                                int oldx1 = x1;
                                lerp(firstX, maxY, interpolates1, secondX, maxY, interpolates2, minX, minY, interpolates3,
                                        x1, y, pixelShaderInput);
                                renderTarget.setRGB(x1, y, pixelShader.run(rootSignature, pixelShaderInput));
                                eps1 += dx1;
                                if ((eps1 << 1) >= -dy) {
                                    --x1;
                                    eps1 += dy;
                                }

                                int oldx2 = x2;
                                while ((eps2 << 1) > dx2) {
                                    lerp(firstX, maxY, interpolates1, secondX, maxY, interpolates2, minX, minY, interpolates3,
                                            x2, y, pixelShaderInput);
                                    renderTarget.setRGB(x2, y, pixelShader.run(rootSignature, pixelShaderInput));
                                    eps2 += dy;
                                    ++x2;
                                }

                                for (int lineStart = oldx1 + 1; lineStart < oldx2; ++lineStart) {
                                    lerp(firstX, maxY, interpolates1, secondX, maxY, interpolates2, minX, minY, interpolates3,
                                            lineStart, y, pixelShaderInput);
                                    renderTarget.setRGB(lineStart, y, pixelShader.run(rootSignature, pixelShaderInput));
                                }

                                ++y;
                                eps2 -= dx2;
                            }
                        } else {
                            while (y < maxY) {
                                int oldx1 = x1;
                                lerp(firstX, maxY, interpolates1, secondX, maxY, interpolates2, minX, minY, interpolates3,
                                        x1, y, pixelShaderInput);
                                renderTarget.setRGB(x1, y, pixelShader.run(rootSignature, pixelShaderInput));
                                eps1 += dx1;
                                if ((eps1 << 1) >= -dy) {
                                    --x1;
                                    eps1 += dy;
                                }

                                int oldx2 = x2;
                                lerp(firstX, maxY, interpolates1, secondX, maxY, interpolates2, minX, minY, interpolates3,
                                        x2, y, pixelShaderInput);
                                renderTarget.setRGB(x2, y, pixelShader.run(rootSignature, pixelShaderInput));
                                eps2 -= dx2;
                                if ((eps2 << 1) >= -dy) {
                                    ++x2;
                                    eps2 += dy;
                                }

                                for (int lineStart = oldx1 + 1; lineStart < oldx2; ++lineStart) {
                                    lerp(firstX, maxY, interpolates1, secondX, maxY, interpolates2, minX, minY, interpolates3,
                                            lineStart, y, pixelShaderInput);
                                    renderTarget.setRGB(lineStart, y, pixelShader.run(rootSignature, pixelShaderInput));
                                }

                                ++y;
                            }
                        }
                    }
                }
            } else {
                //both leaning left
                if(-dx1 >= -dy) {
                    if(-dx2 >= -dy) {
                        while (y < maxY) {
                            while((eps1 << 1) > dx1) {
                                lerp(firstX, maxY, interpolates1, secondX, maxY, interpolates2, minX, minY, interpolates3,
                                        x1, y, pixelShaderInput);
                                renderTarget.setRGB(x1, y, pixelShader.run(rootSignature, pixelShaderInput));
                                eps1 += dy;
                                ++x1;
                            }

                            int oldx2 = x2;
                            while((eps2 << 1) > dx2) {
                                lerp(firstX, maxY, interpolates1, secondX, maxY, interpolates2, minX, minY, interpolates3,
                                        x2, y, pixelShaderInput);
                                renderTarget.setRGB(x2, y, pixelShader.run(rootSignature, pixelShaderInput));
                                eps2 += dy;
                                ++x2;
                            }

                            for(int lineStart = x1; lineStart < oldx2; ++lineStart) {
                                lerp(firstX, maxY, interpolates1, secondX, maxY, interpolates2, minX, minY, interpolates3,
                                        lineStart, y, pixelShaderInput);
                                renderTarget.setRGB(lineStart, y, pixelShader.run(rootSignature, pixelShaderInput));
                            }

                            ++y;
                            eps1 -= dx1;
                            eps2 -= dx2;
                        }
                    } else {
                        while (y < maxY) {
                            while((eps1 << 1) > dx1) {
                                lerp(firstX, maxY, interpolates1, secondX, maxY, interpolates2, minX, minY, interpolates3,
                                        x1, y, pixelShaderInput);
                                renderTarget.setRGB(x1, y, pixelShader.run(rootSignature, pixelShaderInput));
                                eps1 += dy;
                                ++x1;
                            }

                            int oldx2 = x2;
                            lerp(firstX, maxY, interpolates1, secondX, maxY, interpolates2, minX, minY, interpolates3,
                                    x2, y, pixelShaderInput);
                            renderTarget.setRGB(x2, y, pixelShader.run(rootSignature, pixelShaderInput));
                            eps2 -= dx2;
                            if ((eps2 << 1) >= -dy) {
                                ++x2;
                                eps2 += dy;
                            }

                            for(int lineStart = x1; lineStart < oldx2; ++lineStart) {
                                lerp(firstX, maxY, interpolates1, secondX, maxY, interpolates2, minX, minY, interpolates3,
                                        lineStart, y, pixelShaderInput);
                                renderTarget.setRGB(lineStart, y, pixelShader.run(rootSignature, pixelShaderInput));
                            }

                            ++y;
                            eps1 -= dx1;
                        }
                    }
                } else {
                    if(-dx2 >= -dy) {
                        while (y < maxY) {
                            lerp(firstX, maxY, interpolates1, secondX, maxY, interpolates2, minX, minY, interpolates3,
                                    x1, y, pixelShaderInput);
                            renderTarget.setRGB(x1, y, pixelShader.run(rootSignature, pixelShaderInput));
                            eps1 -= dx1;
                            if ((eps1 << 1) >= -dy) {
                                ++x1;
                                eps1 += dy;
                            }

                            int oldx2 = x2;
                            while((eps2 << 1) > dx2) {
                                lerp(firstX, maxY, interpolates1, secondX, maxY, interpolates2, minX, minY, interpolates3,
                                        x2, y, pixelShaderInput);
                                renderTarget.setRGB(x2, y, pixelShader.run(rootSignature, pixelShaderInput));
                                eps2 += dy;
                                ++x2;
                            }

                            for(int lineStart = x1; lineStart < oldx2; ++lineStart) {
                                lerp(firstX, maxY, interpolates1, secondX, maxY, interpolates2, minX, minY, interpolates3,
                                        lineStart, y, pixelShaderInput);
                                renderTarget.setRGB(lineStart, y, pixelShader.run(rootSignature, pixelShaderInput));
                            }

                            ++y;
                            eps2 -= dx2;
                        }
                    } else {
                        while (y < maxY) {
                            lerp(firstX, maxY, interpolates1, secondX, maxY, interpolates2, minX, minY, interpolates3,
                                    x1, y, pixelShaderInput);
                            renderTarget.setRGB(x1, y, pixelShader.run(rootSignature, pixelShaderInput));
                            eps1 -= dx1;
                            if ((eps1 << 1) >= -dy) {
                                ++x1;
                                eps1 += dy;
                            }

                            int oldx2 = x2;
                            lerp(firstX, maxY, interpolates1, secondX, maxY, interpolates2, minX, minY, interpolates3,
                                    x2, y, pixelShaderInput);
                            renderTarget.setRGB(x2, y, pixelShader.run(rootSignature, pixelShaderInput));
                            eps2 -= dx2;
                            if ((eps2 << 1) >= -dy) {
                                ++x2;
                                eps2 += dy;
                            }

                            for(int lineStart = x1; lineStart < oldx2; ++lineStart) {
                                lerp(firstX, maxY, interpolates1, secondX, maxY, interpolates2, minX, minY, interpolates3,
                                        lineStart, y, pixelShaderInput);
                                renderTarget.setRGB(lineStart, y, pixelShader.run(rootSignature, pixelShaderInput));
                            }

                            ++y;
                        }
                    }
                }
            }
        }
    }

    /**
     * Alters the second position to make it on screen
     */
    private static Vector2 clipSecond(Vector4 pos1, Vector4 pos2, int widthMinusOne, int heightMinusOne) {
        float x2 = pos2.x, y2 = pos2.y;
        if(pos2.x < 0) {
            y2 += (pos1.y - y2) * (-x2) / (pos1.x - x2);
            x2 = 0;
        } else if(x2 > widthMinusOne) {
            y2 += (pos1.y - y2) * (x2 - widthMinusOne) / (x2 - pos1.x);
            x2 = widthMinusOne;
        }
        if(y2 < 0) {
            x2 += (pos1.x - x2) * (-y2) / (pos1.y - y2);
            y2 = 0;
        } else if(y2 > heightMinusOne) {
            x2 += (pos1.x - x2) * (y2 - heightMinusOne) / (y2 - pos1.y);
            y2 = heightMinusOne;
        }
        return new Vector2(x2, y2);
    }

    /**
     * Draws a fully on screen line using Bresenham's algorithm
     */
    private static void rasterizeLine(int x1, int y1, int x2, int y2, PixelShader pixelShader, Object[] rootSignature,
                                      BufferedImage renderTarget, Vector4[] interpolates1, Vector4[] interpolates2,
                                      Vector4[] pixelShaderInput) {

        if (x1 > x2) {
            int temp = x1;
            x1 = x2;
            x2 = temp;
            temp = y1;
            y1 = y2;
            y2 = temp;
            Vector4[] temp2 = interpolates1;
            interpolates1 = interpolates2;
            interpolates2 = temp2;
        }
        int dx = x2 - x1;
        int dy = y2 - y1;
        if(dy == 0 && dx == 0) {
            lerp(pixelShaderInput, interpolates1, interpolates2, 0.5f);
            renderTarget.setRGB(x1, y1, pixelShader.run(rootSignature, pixelShaderInput));
            return;
        }
        int eps = 0;
        if(dy >= 0) {
            if(dy <= dx) {
                int y = y1;
                for (int x = x1; x <= x2; x++) {
                    lerp(pixelShaderInput, interpolates1, interpolates2, (float) (x - x1) / dx);
                    renderTarget.setRGB(x, y, pixelShader.run(rootSignature, pixelShaderInput));
                    eps += dy;
                    if ((eps << 1) >= dx) {
                        y++;
                        eps -= dx;
                    }
                }
            } else {
                int x = x1;
                for(int y = y1; y <= y2; ++y) {
                    lerp(pixelShaderInput, interpolates1, interpolates2, (float)(y - y1) / dy);
                    renderTarget.setRGB(x, y, pixelShader.run(rootSignature, pixelShaderInput));
                    eps += dx;
                    if ((eps << 1) >= dy) {
                        ++x;
                        eps -= dy;
                    }
                }
            }
        } else {
            if(-dy <= dx) {
                int y = y2;
                for (int x = x2; x >= x1; --x) {
                    lerp(pixelShaderInput, interpolates1, interpolates2, (float)(x - x1) / dx);
                    renderTarget.setRGB(x, y, pixelShader.run(rootSignature, pixelShaderInput));
                    eps -= dy;
                    if ((eps << 1) >= dx) {
                        ++y;
                        eps -= dx;
                    }
                }
            } else {
                int x = x2;
                for(int y = y2; y <= y1; ++y) {
                    lerp(pixelShaderInput, interpolates1, interpolates2, (float)(y - y1) / dy);
                    renderTarget.setRGB(x, y, pixelShader.run(rootSignature, pixelShaderInput));
                    eps -= dx;
                    if ((eps << 1) <= dy) {
                        --x;
                        eps -= dy;
                    }
                }
            }
        }
    }

    /**
     * linearly interpolates between two vectors
     */
    private static void lerp(Vector4[] pixelShaderInput, Vector4[] interpolates1, Vector4[] interpolates2, float amount2) {
        if(pixelShaderInput != null) {
            for(int i = 1; i < interpolates1.length; ++i) {
                pixelShaderInput[i - 1].x = interpolates1[i].x - amount2 * (interpolates1[i].x - interpolates2[i].x);
                pixelShaderInput[i - 1].y = interpolates1[i].y - amount2 * (interpolates1[i].y - interpolates2[i].y);
                pixelShaderInput[i - 1].z = interpolates1[i].z - amount2 * (interpolates1[i].z - interpolates2[i].z);
                pixelShaderInput[i - 1].w = interpolates1[i].w - amount2 * (interpolates1[i].w - interpolates2[i].w);
            }
        }
    }

    /**
     * Interpolates between three vectors using barycentric coordinates
     */
    private static void lerp(float x1, float y1, Vector4[] interpolates1, float x2, float y2, Vector4[] interpolates2,
                             float x3, float y3, Vector4[] interpolates3, float x, float y, Vector4[] pixelShaderInput) {
        if(pixelShaderInput != null) {
            final float denominator = (y2 - y3) * (x1 - x3) + (x3 - x2) * (y1 - y3);
            float weight1 = ((y2 - y3) * (x - x3) + (x3 - x2) * (y - y3)) / denominator;
            float weight2 = ((y3 - y1) * (x - x3) + (x1 - x3) * (y - y3)) / denominator;
            float weight3 = 1 - weight1 - weight2;

            for(int i = 1; i < interpolates1.length; ++i) {
                pixelShaderInput[i - 1].x = interpolates1[i].x * weight1 + interpolates2[i].x * weight2 + interpolates3[i].x * weight3;
                pixelShaderInput[i - 1].y = interpolates1[i].y * weight1 + interpolates2[i].y * weight2 + interpolates3[i].y * weight3;
                pixelShaderInput[i - 1].z = interpolates1[i].z * weight1 + interpolates2[i].z * weight2 + interpolates3[i].z * weight3;
                pixelShaderInput[i - 1].w = interpolates1[i].w * weight1 + interpolates2[i].w * weight2 + interpolates3[i].w * weight3;
            }
        }
    }

    /**
     * creates a new interpolate by interpolating between two old ones
     */
    private static void createNewInterpolate(Vector4[] out, Vector4[] interpolates1, Vector4[] interpolates2, float amount2) {
        for(int i = 1; i < out.length; ++i) {
            out[i].x = interpolates1[i].x - amount2 * (interpolates1[i].x - interpolates2[i].x);
            out[i].y = interpolates1[i].y - amount2 * (interpolates1[i].y - interpolates2[i].y);
            out[i].z = interpolates1[i].z - amount2 * (interpolates1[i].z - interpolates2[i].z);
            out[i].w = interpolates1[i].w - amount2 * (interpolates1[i].w - interpolates2[i].w);
        }
    }

    private static void copyInterpolateToPixelShaderInput(Vector4[] interpolates1, Vector4[] pixelShaderInput) {
        for(int i = 1; i < interpolates1.length; ++i) {
            pixelShaderInput[i - 1].x = interpolates1[i].x;
            pixelShaderInput[i - 1].y = interpolates1[i].y;
            pixelShaderInput[i - 1].z = interpolates1[i].z;
            pixelShaderInput[i - 1].w = interpolates1[i].w;
        }
    }

    /**
     * makes every pixel in image equal to color
     * @param image the image to clear
     * @param color The new color that the image should be cleared to
     */
    public static void clear(BufferedImage image, int color) {
        int height = image.getHeight();
        int width = image.getWidth();
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                image.setRGB(x, y, color);
            }
        }
    }
}