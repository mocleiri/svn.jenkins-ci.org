package org.jvnet.hudson.ec2.launcher.gui;

import org.jvnet.hudson.ec2.launcher.Booter;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * Boot the instance and perform necessary initialization to start Hudson.
 *
 * @author Kohsuke Kawaguchi
 */
public class BootPage extends Page {
    private JTextArea console;
    private JPanel panel;
    private JLabel status;
    private JProgressBar progressBar;
    private JScrollPane scrollPane;
    /**
     * This thread does the booting asynchronously.
     */
    private Booter thread;

    public BootPage(WizardState state) {
        super(state);
        add(panel);
    }

    @Override
    public void prepare() {
        // if the previous launch operation is in progress, cancel that.
        if (thread != null)
            thread.interrupt();

        setBusy(true);
        console.setText(""); // reset the console
        progressBar.setIndeterminate(true);

        PipedOutputStream out = new PipedOutputStream();
        final PipedInputStream in;
        try {
            in = new PipedInputStream(out);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        new Thread() {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            public void run() {
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        final String text = line;
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                console.append(text + '\n');
                                scrollDown();
                            }
                        });
                    }
                } catch (IOException e) {
                    throw new Error(e);
                }
            }
        }.start();
        // show the summary line
        thread = new Booter(this, out) {
            protected void reportStatus(final String msg) {
                super.reportStatus(msg);
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        status.setText(msg);
                    }
                });
            }

            protected void reportError(final String msg) {
                super.reportError(msg);
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        status.setText(msg);
                    }
                });
            }

            protected void onEnd() {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        progressBar.setIndeterminate(false);
                        progressBar.setValue(100);
                    }
                });
            }
        };
        thread.start();
    }

    /**
     * Forces the scroll of text area.
     */
    private void scrollDown() {
        int pos = console.getDocument().getEndPosition().getOffset();
        console.getCaret().setDot(pos);
        console.requestFocus();
    }

    @Override
    public void abortBusy() {
        thread.interrupt();
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
        panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8), null));
        scrollPane = new JScrollPane();
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(scrollPane, gbc);
        console = new JTextArea();
        console.setColumns(40);
        console.setRows(10);
        scrollPane.setViewportView(console);
        status = new JLabel();
        status.setText("Starting...");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(status, gbc);
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(progressBar, gbc);
        final JPanel spacer1 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 999.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(spacer1, gbc);
        final JPanel spacer2 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.weighty = 999.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        panel.add(spacer2, gbc);
        final JLabel label1 = new JLabel();
        label1.setText(" ");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(label1, gbc);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel;
    }
}
