import java.io.File;
import java.io.FileNotFoundException;
import java.util.NoSuchElementException;
import java.util.Scanner;

 /**
 * This class can load model data from files and manage it.
 */
public class Model {
    public Vertex[] vertexBuffer;
    public int[] indexBuffer;

    // the largest absolute coordinate value of the untransformed model data
    private float maxSize;

     /**
      * Store per vertex data
      */
    static class Vertex {
        float x, y, z;
        float nx, ny, nz;
    }

    /**
     * Creates a {@link Model} instance for the data in the specified file.
     *
     * @param file The file to load.
     * @return The {@link Model}, or null if an error occurred.
     */
    public static Model loadModel(final File file) {
        final Model model = new Model();

        // read the data from the file
        if (!model.loadModelFromFile(file)) {
            return null;
        }

        return model;
    }

    /**
     * Reads model data from the specified file.
     *
     * @param file The file to load.
     * @return True on success, false otherwise.
     */
    private boolean loadModelFromFile(final File file) {
        maxSize = 0.f;

        try (final Scanner scanner = new Scanner(file)) {
            // the first line specifies the vertex count
            int vertexCount = scanner.nextInt();
            vertexBuffer = new Vertex[vertexCount];
            for(int i = 0; i < vertexCount; ++i) {
                vertexBuffer[i] = new Vertex();
            }

            // read all vertex coordinates
            for (int i = 0; i < vertexCount; ++i) {
                // advance the position to the beginning of the next line
                scanner.nextLine();

                // read the vertex coordinates
                vertexBuffer[i].x = scanner.nextFloat();
                vertexBuffer[i].y = scanner.nextFloat();
                vertexBuffer[i].z = scanner.nextFloat();
                maxSize = Math.max(maxSize, Math.max(Math.abs(vertexBuffer[i].x),
                        Math.max(Math.abs(vertexBuffer[i].y), Math.abs(vertexBuffer[i].z))));
            }

            // the next line specifies the number of triangles
            scanner.nextLine();
            int triangleCount = scanner.nextInt();

            // read all polygon data (assume triangles); these are indices into
            // the vertex array
            indexBuffer = new int[triangleCount * 3];
            for (int i = 0, j = 0; i < triangleCount; ++i) {
                scanner.nextLine();

                // the model files start with index 1, we start with 0
                indexBuffer[j] = scanner.nextInt() - 1;
                ++j;
                indexBuffer[j] = scanner.nextInt() - 1;
                ++j;
                indexBuffer[j] = scanner.nextInt() - 1;
                ++j;
            }

            //generate smoothed normals
            Vector3 sum = new Vector3();
            for(int i = 0; i < vertexCount; ++i) {
                Vertex vertex = vertexBuffer[i];
                sum.x = 0f;
                sum.y = 0f;
                sum.z = 0f;
                for(int j = 0; j < indexBuffer.length;) {
                    if(indexBuffer[j] == i) {
                        Vertex vertex2 = vertexBuffer[indexBuffer[j + 1]];
                        Vertex vertex3 = vertexBuffer[indexBuffer[j + 2]];
                        sum.plusEquals(calculateNormal(new Vector3(vertex.x, vertex.y, vertex.z),
                                new Vector3(vertex2.x, vertex2.y, vertex2.z), new Vector3(vertex3.x, vertex3.y, vertex3.z)));
                        j += 3;
                        continue;
                    }
                    ++j;
                    if(indexBuffer[j] == i) {
                        Vertex vertex2 = vertexBuffer[indexBuffer[j - 1]];
                        Vertex vertex3 = vertexBuffer[indexBuffer[j + 1]];
                        sum.plusEquals(calculateNormal(new Vector3(vertex2.x, vertex2.y, vertex2.z),
                                new Vector3(vertex.x, vertex.y, vertex.z), new Vector3(vertex3.x, vertex3.y, vertex3.z)));
                        j += 2;
                        continue;
                    }
                    ++j;
                    if(indexBuffer[j] == i) {
                        Vertex vertex2 = vertexBuffer[indexBuffer[j - 2]];
                        Vertex vertex3 = vertexBuffer[indexBuffer[j - 1]];
                        sum.plusEquals(calculateNormal(new Vector3(vertex2.x, vertex2.y, vertex2.z),
                                new Vector3(vertex3.x, vertex3.y, vertex.z), new Vector3(vertex.x, vertex.y, vertex.z)));
                    }
                    ++j;
                }
                sum.normalize();
                vertex.nx = sum.x;
                vertex.ny = sum.y;
                vertex.nz = sum.z;
            }

            System.out.println("Number of vertices in model: " + vertexCount);
            System.out.println("Number of triangles in model: " + triangleCount);
        } catch (FileNotFoundException e) {
            System.err.println("No such file " + file.toString() + ": "
                    + e.getMessage());
            return false;
        } catch (NoSuchElementException e) {
            System.err.println("Invalid file format: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Something went wrong while reading the model file: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Returns the largest absolute coordinate value of the original,
     * untransformed model data.
     */
    public float getMaxSize() {
        return maxSize;
    }

     /**
      * Calculates the normal of a triangle
      */
    private static Vector3 calculateNormal(Vector3 vertex1, Vector3 vertex2, Vector3 vertex3) {
        Vector3 l2 = vertex2.minus(vertex1);
        Vector3 l1 = vertex3.minus(vertex1);
        return l2.cross(l1);
    }
}