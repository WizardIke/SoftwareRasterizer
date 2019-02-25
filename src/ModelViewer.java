import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

/**
 * Allows users to load .dat model files which can then by displayed in different ways
 */
public class ModelViewer {
    private JFrame frame;
    private Canvas canvas;

    private int oldRotateX = 0;
    private int oldRotateY = 0;
    private int oldRotateZ = 0;
    private JSlider sliderRotateX;
    private JSlider sliderRotateY;
    private JSlider sliderRotateZ;

    private JButton btnScaleUp;
    private JButton btnScaleDown;
    private JButton btnIncrX;
    private JButton btnDecrX;
    private JButton btnIncrY;
    private JButton btnDecrY;
    private JButton btnIncrZ;
    private JButton btnDecrZ;

    private JCheckBox chkRenderWireframe;
    private JCheckBox chkRenderSolid;
    private JCheckBox chkCullBackFaces;

    private JMenuItem menuOpenModelFile;

    private Model currentModel;

    //////////////////////////////////////////////////////////////////////////////
    /**
     * Handles input for rotating the model
     */
    private ChangeListener sliderChangeListener = new ChangeListener() {

        @Override
        public void stateChanged(ChangeEvent e) {
            final JSlider source = (JSlider) e.getSource();
            if (currentModel != null) {
                if (source == sliderRotateX) {
                    canvas.setWorldMatrix(canvas.getWorldMatrix().
                            mul(Matrix4x4.getRotationX((float)Math.toRadians(sliderRotateX.getValue() - oldRotateX))));
                    oldRotateX = sliderRotateX.getValue();
                } else if (source == sliderRotateY) {
                    canvas.setWorldMatrix(canvas.getWorldMatrix().
                            mul(Matrix4x4.getRotationY((float)Math.toRadians(sliderRotateY.getValue() - oldRotateY))));
                    oldRotateY = sliderRotateY.getValue();
                } else if (source == sliderRotateZ) {
                    canvas.setWorldMatrix(canvas.getWorldMatrix().
                            mul(Matrix4x4.getRotationZ((float)Math.toRadians(sliderRotateZ.getValue() - oldRotateZ))));
                    oldRotateZ = sliderRotateZ.getValue();
                }

                canvas.updateTransform();
                canvas.repaint();
            }
        }
    };

    /**
     * Handles input for scaling and translating the model
     */
    private ActionListener btnActionListener = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            final Object source = e.getSource();
            if (currentModel != null) {
                // scale changes
                if (source == btnScaleUp) {
                    canvas.setWorldMatrix(canvas.getWorldMatrix().mul(Matrix4x4.getScale(1.1f, 1.1f, 1.1f)));
                } else if (source == btnScaleDown) {
                    canvas.setWorldMatrix(canvas.getWorldMatrix().mul(Matrix4x4.getScale(0.9f, 0.9f, 0.9f)));
                }
                // translation changes
                else if (source == btnIncrX) {
                    canvas.getWorldMatrix().translate(0.1f * currentModel.getMaxSize(), 0f, 0f);
                } else if (source == btnDecrX) {
                    canvas.getWorldMatrix().translate(-0.1f * currentModel.getMaxSize(), 0f, 0f);
                } else if (source == btnIncrY) {
                    canvas.getWorldMatrix().translate(0f, 0.1f * currentModel.getMaxSize(), 0f);
                } else if (source == btnDecrY) {
                    canvas.getWorldMatrix().translate(0.f, -0.1f * currentModel.getMaxSize(), 0f);
                } else if (source == btnIncrZ) {
                    canvas.getWorldMatrix().translate(0.f, 0f, 0.1f * currentModel.getMaxSize());
                } else if (source == btnDecrZ) {
                    canvas.getWorldMatrix().translate(0.f, 0f, -0.1f * currentModel.getMaxSize());
                }

                canvas.updateTransform();
                canvas.repaint();
            }
        }
    };

    /**
     * Handles input for filling frame mode and culling mode
     */
    private ActionListener chkActionListener = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            final Object source = e.getSource();
            if (source == chkRenderWireframe) {
                canvas.setWireFrame(chkRenderWireframe.isSelected());
            } else if (source == chkRenderSolid) {
                canvas.setFill(chkRenderSolid.isSelected());
            } else if (source == chkCullBackFaces) {
                canvas.setCullBackFace(chkCullBackFaces.isSelected());
            }

            canvas.repaint();
        }
    };

    /**
     * Handles input for loading files
     */
    private ActionListener menuActionListener = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == menuOpenModelFile) {
                final Model model = loadModelFile();
                if (model != null) {
                    currentModel = model;
                    canvas.setModel(model);

                    canvas.updateTransform();
                    canvas.repaint();
                }
            }
        }
    };

    //////////////////////////////////////////////////////////////////////////////
    private ModelViewer() {
        SwingUtilities.invokeLater(this::createGui);
    }

    /**
     * Creates the GUI. Must be called from the EDT.
     */
    private void createGui() {
        frame = new JFrame("Model Viewer");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        // setup the content pane
        final JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());
        frame.setContentPane(contentPane);

        final Border border = BorderFactory.createBevelBorder(BevelBorder.LOWERED);

        // setup the canvas and control panel
        canvas = new Canvas();
        canvas.setBorder(border);
        contentPane.add(canvas, BorderLayout.CENTER);
        final JComponent controlPanel = createControlPanel();
        controlPanel.setBorder(border);
        contentPane.add(controlPanel, BorderLayout.LINE_START);

        // add the menu
        final JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);
        final JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        menuOpenModelFile = new JMenuItem("Open");
        menuOpenModelFile.addActionListener(menuActionListener);
        fileMenu.add(menuOpenModelFile);

        // register a key event dispatcher to get a turn in handling all
        // key events, independent of which component currently has the focus
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(e -> {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_ESCAPE:
                            System.exit(0);
                            return true; // consume the event
                        default:
                            return false; // do not consume the event
                    }
                });

        Dimension screenDiminsions = getScreenDimensions();
        screenDiminsions.width *= 0.6f;
        screenDiminsions.height *= 0.8f;
        frame.setSize(screenDiminsions);
        frame.setVisible(true);
    }

    /**
     * @return the width and height of the virtual screen in pixels
     * Copied from a previous project by Isaac Clancy
     */
    private static Dimension getScreenDimensions() {
        Dimension dimension = new Dimension(0, 0);
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        for (GraphicsDevice curGs : gs)
        {
            DisplayMode mode = curGs.getDisplayMode();
            dimension.width += mode.getWidth();
            dimension.height += mode.getHeight();
        }
        return dimension;
    }

    /**
     * Creates and returns the control panel. Must be called from the EDT.
     */
    private JComponent createControlPanel() {
        final JPanel toolbar = new JPanel(new GridBagLayout());

        final GridBagConstraints gbcDefault = new GridBagConstraints();
        gbcDefault.gridx = 0;
        gbcDefault.gridy = GridBagConstraints.RELATIVE;
        gbcDefault.gridwidth = 2;
        gbcDefault.fill = GridBagConstraints.HORIZONTAL;
        gbcDefault.insets = new Insets(5, 10, 5, 10);
        gbcDefault.anchor = GridBagConstraints.FIRST_LINE_START;
        gbcDefault.weightx = 0.5;

        final GridBagConstraints gbcLabels =
                (GridBagConstraints) gbcDefault.clone();
        gbcLabels.insets = new Insets(5, 10, 0, 10);

        final GridBagConstraints gbcTwoCol =
                (GridBagConstraints) gbcDefault.clone();
        gbcTwoCol.gridwidth = 1;
        gbcTwoCol.gridx = 0;
        gbcTwoCol.insets.right = 5;

        GridBagConstraints gbc;

        // setup the rotation sliders
        sliderRotateX = new JSlider(JSlider.HORIZONTAL, 0, 360, oldRotateX);
        sliderRotateY = new JSlider(JSlider.HORIZONTAL, 0, 360, oldRotateY);
        sliderRotateZ = new JSlider(JSlider.HORIZONTAL, 0, 360, oldRotateZ);
        sliderRotateX.setPaintLabels(true);
        sliderRotateY.setPaintLabels(true);
        sliderRotateZ.setPaintLabels(true);
        sliderRotateX.setPaintTicks(true);
        sliderRotateY.setPaintTicks(true);
        sliderRotateZ.setPaintTicks(true);
        sliderRotateX.setMajorTickSpacing(90);
        sliderRotateY.setMajorTickSpacing(90);
        sliderRotateZ.setMajorTickSpacing(90);
        sliderRotateX.addChangeListener(sliderChangeListener);
        sliderRotateY.addChangeListener(sliderChangeListener);
        sliderRotateZ.addChangeListener(sliderChangeListener);
        gbc = gbcDefault;
        toolbar.add(new JLabel("Rotation X:"), gbcLabels);
        toolbar.add(sliderRotateX, gbc);
        toolbar.add(new JLabel("Rotation Y:"), gbcLabels);
        toolbar.add(sliderRotateY, gbc);
        toolbar.add(new JLabel("Rotation Z:"), gbcLabels);
        toolbar.add(sliderRotateZ, gbc);

        btnScaleDown = new JButton("- size");
        btnScaleUp = new JButton("+ size");
        btnScaleDown.addActionListener(btnActionListener);
        btnScaleUp.addActionListener(btnActionListener);
        gbc = (GridBagConstraints) gbcTwoCol.clone();
        toolbar.add(btnScaleDown, gbc);
        gbc.gridx = 1;
        gbc.insets.left = gbc.insets.right;
        gbc.insets.right = gbcDefault.insets.right;
        toolbar.add(btnScaleUp, gbc);

        btnIncrX = new JButton("+ x");
        btnDecrX = new JButton("- x");
        btnIncrY = new JButton("+ y");
        btnDecrY = new JButton("- y");
        btnIncrZ = new JButton("+ z");
        btnDecrZ = new JButton("- z");
        btnIncrX.addActionListener(btnActionListener);
        btnDecrX.addActionListener(btnActionListener);
        btnIncrY.addActionListener(btnActionListener);
        btnDecrY.addActionListener(btnActionListener);
        btnIncrZ.addActionListener(btnActionListener);
        btnDecrZ.addActionListener(btnActionListener);

        gbc = (GridBagConstraints) gbcTwoCol.clone();
        toolbar.add(btnDecrX, gbc);
        gbc.gridx = 1;
        gbc.insets.left = gbc.insets.right;
        gbc.insets.right = gbcDefault.insets.right;
        toolbar.add(btnIncrX, gbc);

        gbc = (GridBagConstraints) gbcTwoCol.clone();
        toolbar.add(btnDecrY, gbc);
        gbc.gridx = 1;
        gbc.insets.left = gbc.insets.right;
        gbc.insets.right = gbcDefault.insets.right;
        toolbar.add(btnIncrY, gbc);

        gbc = (GridBagConstraints) gbcTwoCol.clone();
        toolbar.add(btnDecrZ, gbc);
        gbc.gridx = 1;
        gbc.insets.left = gbc.insets.right;
        gbc.insets.right = gbcDefault.insets.right;
        toolbar.add(btnIncrZ, gbc);

        // add check boxes
        gbc = gbcDefault;

        chkRenderWireframe = new JCheckBox("Render Wireframe");
        chkRenderWireframe.setSelected(true);
        chkRenderWireframe.addActionListener(chkActionListener);
        toolbar.add(chkRenderWireframe, gbc);

        chkRenderSolid = new JCheckBox("Render Solid");
        chkRenderSolid.setSelected(true);
        chkRenderSolid.addActionListener(chkActionListener);
        toolbar.add(chkRenderSolid, gbc);

        chkCullBackFaces = new JCheckBox("Cull Back Faces");
        chkCullBackFaces.setSelected(true);
        chkCullBackFaces.addActionListener(chkActionListener);
        gbc = (GridBagConstraints) gbcDefault.clone();
        gbc.weighty = 1.;
        gbc.gridheight = GridBagConstraints.REMAINDER;
        toolbar.add(chkCullBackFaces, gbc);

        return toolbar;
    }

    /**
     * Displays a chooser dialog and loads the selected model.
     *
     * @return The model, or null if the user cancels the action or something
     * goes wrong.
     */
    private Model loadModelFile() {
        // show a file chooser for model files
        JFileChooser chooser = new JFileChooser("./");
        chooser.setFileFilter(new FileNameExtensionFilter(
                ".dat model files", "dat"));
        int retVal = chooser.showOpenDialog(frame);
        if (retVal == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();

            // try to load the model from the selected file
            final Model model = Model.loadModel(file);

            if (model != null) {
                float scale = Math.min((float)canvas.getWidth() / (float)canvas.getHeight(), 1f) / model.getMaxSize() * 7f;
                canvas.setWorldMatrix(Matrix4x4.getTranslation(0f, 0f, -10f).mul(Matrix4x4.getScale(scale, scale, scale)));
                //m_canvas.updateTransform(); //can't update transform yet or it will cause a NullPointerException
            }

            return model;
        }

        return null;
    }

    public static void main(String[] args) {
        System.out.println("-------------------------------------");
        System.out.println("159.235 Assignment 3, Semester 2 2016");
        System.out.println("Submitted by: Clancy, Isaac, 16125296");
        System.out.println("-------------------------------------");

        new ModelViewer();
    }
}