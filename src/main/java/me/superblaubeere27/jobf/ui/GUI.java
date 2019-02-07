/*
 * Copyright (c) 2017-2019 superblaubeere27, Sam Sun, MarcoMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package me.superblaubeere27.jobf.ui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import me.superblaubeere27.jobf.JObf;
import me.superblaubeere27.jobf.JObfImpl;
import me.superblaubeere27.jobf.utils.*;
import me.superblaubeere27.jobf.utils.values.*;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;

public class GUI extends JFrame {
    public JTextArea logArea;
    private JTabbedPane tabbedPane1;
    private JPanel panel1;
    private JTextField inputTextField;
    private JButton inputBrowseButton;
    private JTextField outputTextField;
    private JButton outputBrowseButton;
    private JButton obfuscateButton;
    private JButton buildButton;
    private JButton loadButton;
    private JButton saveButton;
    private JCheckBox prettyPrintCheckBox;
    private JTextArea configPanel;
    private JTabbedPane processorOptions;
    private RSyntaxTextArea scriptArea;
    private JList<Template> templates;
    private JButton loadTemplateButton;
    private JCheckBox autoScroll;
    private JList<String> libraries;
    private JButton addButton;
    private JButton removeButton;
    private JSlider threadsSlider;
    private JLabel threadsLabel;
    private JCheckBox verbose;
    private List<String> libraryList = new ArrayList<>();

    private void updateLibraries() {
        Object[] libraries = libraryList.toArray();
        this.libraries.setListData(Arrays.copyOf(libraries, libraries.length, String[].class));
    }

    public void scrollDown() {
        if (this.autoScroll.isSelected()) {
            this.logArea.setCaretPosition(this.logArea.getDocument().getLength());
        }

    }

    public GUI(String updateCheckResult) {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setContentPane(panel1);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setSize((int) (screenSize.getWidth() / 2), (int) (screenSize.getHeight() / 2));
        setLocationRelativeTo(null);
        setTitle(JObf.VERSION);

        inputBrowseButton.addActionListener(e -> {
            String file = Utils.chooseFile(null, GUI.this, new JarFileFilter());
            if (file != null) {
                inputTextField.setText(file);
            }
        });
        outputBrowseButton.addActionListener(e -> {
            String file = Utils.chooseFileToSave(null, GUI.this, new JarFileFilter());
            if (file != null) {
                outputTextField.setText(file);
            }
        });
        obfuscateButton.addActionListener(e -> startObfuscator());
        buildButton.addActionListener(e -> buildConfig());
        saveButton.addActionListener(e -> {
            String name = Utils.chooseFileToSave(null, GUI.this, new JObfFileFilter());
            if (name != null) {
                buildConfig();
                try {
                    Files.write(new File(name).toPath(), configPanel.getText().getBytes(Charset.forName("UTF-8")));
                } catch (IOException e1) {
                    e1.printStackTrace();
                    JOptionPane.showMessageDialog(GUI.this, e1.toString(), "ERROR", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        loadButton.addActionListener(e -> {
            String name = Utils.chooseFile(null, GUI.this, new JObfFileFilter());
            if (name != null) {
                buildConfig();
                try {
                    Configuration configuration = ConfigManager.loadConfig(new String(Files.readAllBytes(Paths.get(name)), StandardCharsets.UTF_8));
                    inputTextField.setText(configuration.getInput());
                    outputTextField.setText(configuration.getOutput());
                    scriptArea.setText(configuration.getScript());
                    libraryList = new ArrayList<>(configuration.getLibraries());
                    updateLibraries();
                    initValues();
                    System.gc();
                } catch (IOException e1) {
                    e1.printStackTrace();
                    JOptionPane.showMessageDialog(GUI.this, e1.toString(), "ERROR", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        Object[] tmplts = Templates.getTemplates().toArray();
        Template[] templatesArray = Arrays.copyOf(tmplts, tmplts.length, Template[].class);
        templates.setListData(templatesArray);
        try {
            Theme.load(GUI.class.getResourceAsStream("/theme.xml")).apply(scriptArea);
        } catch (IOException e) {
            e.printStackTrace();
        }
        scriptArea.setText("function isRemappingEnabledForClass(node) {\n" +
                "    return false;\n" +
                "}\n" +
                "function isObfuscatorEnabledForClass(node) {\n" +
                "    return true;\n" +
                "}");
        initValues();
        loadTemplateButton.addActionListener(e -> {
            if (templates.getSelectedIndex() != -1) {
                Configuration config = ConfigManager.loadConfig(templates.getSelectedValue().getJson());
                initValues();
                if (config.getScript() != null && !config.getScript().isEmpty()) {
                    scriptArea.setText(config.getScript());
                }
            } else {
                JOptionPane.showMessageDialog(GUI.this, "Maybe you should select a template before applying it :thinking:", "Hmmmm...", JOptionPane.ERROR_MESSAGE);
            }
        });

        addButton.addActionListener(e -> {
            String file = Utils.chooseDirectoryOrFile(new File(System.getProperty("java.home")), GUI.this);

            if (file != null) {
                if (new File(file).isDirectory() || Utils.checkZip(file)) {
                    libraryList.add(file);
                    updateLibraries();
                } else {
                    JOptionPane.showMessageDialog(GUI.this, "This file isn't a valid file. Allowed: ZIP-Files, Directories", "ERROR", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        removeButton.addActionListener(e -> {
            if (libraries.getSelectedIndex() != -1) {
                libraryList.remove(libraries.getSelectedIndex());
                updateLibraries();
            } else {
                JOptionPane.showMessageDialog(GUI.this, "Maybe you should select a library before removing it :thinking:", "Hmmmm...", JOptionPane.ERROR_MESSAGE);
            }
        });

        int cores = Runtime.getRuntime().availableProcessors();


        threadsSlider.addChangeListener(e -> threadsLabel.setText(Integer.toString(threadsSlider.getValue())));

        threadsSlider.setMinimum(1);
        threadsSlider.setMaximum(cores);
        threadsSlider.setValue(cores);


        setVisible(true);

        if (updateCheckResult != null) {
            JLabel label = new JLabel("<html>You are using an outdated version of obfuscator. Latest: " + updateCheckResult + "<br/> You can download the latest version at: <a>https://github.com/superblaubeere27/obfuscator/releases/latest</a> <br/>(Click on the link to open it)</html>");

            label.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    try {
                        Desktop.getDesktop().browse(new URI("https://github.com/superblaubeere27/obfuscator/releases/latest"));
                    } catch (IOException | URISyntaxException e1) {
                        e1.printStackTrace();
                    }
                }
            });

            JOptionPane.showMessageDialog(this, label, "Update available", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void buildConfig() {
        configPanel.setText(ConfigManager.generateConfig(new Configuration(inputTextField.getText(), outputTextField.getText(), scriptArea.getText(), libraryList), prettyPrintCheckBox.isSelected()));
    }

    private void initValues() {
        processorOptions.removeAll();
        HashMap<String, ArrayList<Value<?>>> ownerValueMap = new HashMap<>();

        for (Value<?> value : ValueManager.getValues()) {
            if (!ownerValueMap.containsKey(value.getOwner())) {
                ownerValueMap.put(value.getOwner(), new ArrayList<>());
            }
            ownerValueMap.get(value.getOwner()).add(value);
        }

//        for (Map.Entry<String, ArrayList<Value<?>>> stringArrayListEntry : ownerValueMap.entrySet()) {
        ownerValueMap.entrySet().stream().sorted(Comparator.comparingInt(entry -> -entry.getValue().size())).forEach(entry -> {
            JPanel panel = new JPanel();
            int rows = 0;

            for (Value<?> value : entry.getValue()) {
                if (value instanceof BooleanValue) {
                    BooleanValue booleanValue = (BooleanValue) value;

                    JCheckBox checkBox = new JCheckBox(value.getName(), booleanValue.getObject());
                    checkBox.addActionListener(event -> booleanValue.setObject(checkBox.isSelected()));
                    panel.add(checkBox);

                    panel.add(new JLabel(booleanValue.getDescription() == null ? "" : booleanValue.getDescription()));

                    Color c = Utils.getColor(booleanValue.getDeprecation());

                    if (c != null) {
                        checkBox.setForeground(c);
                    }

                    rows++;
                }
                if (value instanceof StringValue) {
                    StringValue stringValue = (StringValue) value;

                    JTextField textBox = new JTextField(stringValue.getObject());
                    textBox.addActionListener(event -> stringValue.setObject(textBox.getText()));


                    Color c = Utils.getColor(stringValue.getDeprecation());

                    if (c != null) {
                        textBox.setForeground(c);
                    }
                    panel.add(new JLabel(stringValue.getName() + ":"));
                    panel.add(new JLabel(stringValue.getDescription() == null ? "" : stringValue.getDescription()));
                    panel.add(textBox);
                    panel.add(new JLabel(""));

                    rows += 2;
                }
            }
            panel.setLayout(new GridLayout(rows, 2));
            JPanel p1 = new JPanel();
            p1.setLayout(new FlowLayout(FlowLayout.LEFT));
            p1.add(panel);

            processorOptions.addTab(entry.getKey(), p1);
        });

//            }

//        }
    }

    private void startObfuscator() {
//        impl.loadConfig(config);

        File in = new File(inputTextField.getText());

        if (!in.exists()) {
            JOptionPane.showMessageDialog(this, "Input file doesn't exist!", "ERROR", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            new Thread(() -> {
                obfuscateButton.setEnabled(false);

                try {
                    Configuration config = new Configuration(inputTextField.getText(), outputTextField.getText(), scriptArea.getText(), libraryList);

                    JObf.VERBOSE = verbose.isSelected();

                    JObfImpl.INSTANCE.setThreadCount(threadsSlider.getValue());

                    JObfImpl.INSTANCE.processJar(config);
                } catch (Throwable e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, e.toString(), "ERROR", JOptionPane.ERROR_MESSAGE);
                }
                obfuscateButton.setEnabled(true);
            }, "Obfuscator thread").start();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, e.toString(), "ERROR", JOptionPane.ERROR_MESSAGE);
        }
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1 = new JTabbedPane();
        panel1.add(tabbedPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(6, 3, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab(ResourceBundle.getBundle("strings").getString("input.output"), panel2);
        inputTextField = new JTextField();
        panel2.add(inputTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        inputBrowseButton = new JButton();
        this.$$$loadButtonText$$$(inputBrowseButton, ResourceBundle.getBundle("strings").getString("browse"));
        panel2.add(inputBrowseButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        outputTextField = new JTextField();
        panel2.add(outputTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        outputBrowseButton = new JButton();
        this.$$$loadButtonText$$$(outputBrowseButton, ResourceBundle.getBundle("strings").getString("browse"));
        panel2.add(outputBrowseButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, ResourceBundle.getBundle("strings").getString("input"));
        panel2.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, ResourceBundle.getBundle("strings").getString("output"));
        panel2.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        libraries = new JList();
        panel2.add(libraries, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Libraries:");
        panel2.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel3, new GridConstraints(4, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        addButton = new JButton();
        addButton.setText("Add");
        panel3.add(addButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel3.add(spacer1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        removeButton = new JButton();
        removeButton.setText("Remove");
        panel3.add(removeButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        threadsSlider = new JSlider();
        threadsSlider.setMinimum(1);
        threadsSlider.setPaintLabels(true);
        threadsSlider.setPaintTicks(true);
        threadsSlider.setPaintTrack(true);
        threadsSlider.setSnapToTicks(true);
        panel2.add(threadsSlider, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Threads");
        panel2.add(label4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        threadsLabel = new JLabel();
        threadsLabel.setText("");
        panel2.add(threadsLabel, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Processors", panel4);
        processorOptions = new JTabbedPane();
        panel4.add(processorOptions, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        processorOptions.addTab("Untitled", panel5);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Config", panel6);
        buildButton = new JButton();
        buildButton.setText("Build");
        panel6.add(buildButton, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        loadButton = new JButton();
        loadButton.setText("Load");
        panel6.add(loadButton, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        saveButton = new JButton();
        saveButton.setText("Save");
        panel6.add(saveButton, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        prettyPrintCheckBox = new JCheckBox();
        prettyPrintCheckBox.setText("PrettyPrint");
        panel6.add(prettyPrintCheckBox, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel6.add(scrollPane1, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        configPanel = new JTextArea();
        scrollPane1.setViewportView(configPanel);
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Script", panel7);
        final RTextScrollPane rTextScrollPane1 = new RTextScrollPane();
        panel7.add(rTextScrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        scriptArea = new RSyntaxTextArea();
        scriptArea.setCodeFoldingEnabled(true);
        scriptArea.setColumns(150);
        scriptArea.setRows(27);
        scriptArea.setShowMatchedBracketPopup(false);
        scriptArea.setSyntaxEditingStyle("text/javascript");
        rTextScrollPane1.setViewportView(scriptArea);
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Templates", panel8);
        templates = new JList();
        panel8.add(templates, new GridConstraints(0, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        loadTemplateButton = new JButton();
        loadTemplateButton.setText("Apply");
        panel8.add(loadTemplateButton, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab(ResourceBundle.getBundle("strings").getString("log"), panel9);
        final JScrollPane scrollPane2 = new JScrollPane();
        panel9.add(scrollPane2, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        logArea = new JTextArea();
        logArea.setEditable(false);
        scrollPane2.setViewportView(logArea);
        autoScroll = new JCheckBox();
        autoScroll.setSelected(true);
        autoScroll.setText("AutoScroll");
        panel9.add(autoScroll, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        verbose = new JCheckBox();
        verbose.setSelected(false);
        verbose.setText("Verbose");
        panel9.add(verbose, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        obfuscateButton = new JButton();
        this.$$$loadButtonText$$$(obfuscateButton, ResourceBundle.getBundle("strings").getString("obfuscate"));
        panel1.add(obfuscateButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        label1.setLabelFor(inputTextField);
        label2.setLabelFor(outputTextField);
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadLabelText$$$(JLabel component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) break;
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setDisplayedMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadButtonText$$$(AbstractButton component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) break;
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }

}
