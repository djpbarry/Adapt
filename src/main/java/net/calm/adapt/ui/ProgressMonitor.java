/*
 * Copyright (C) 2014 David Barry <david.barry at cancer.org.uk>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.calm.adapt.ui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

/**
 * Non-modal progress dialog with a Cancel button. It is safe to construct and
 * drive from a background analysis thread: UI mutations are marshalled onto the
 * event dispatch thread, and {@link #isCancelled()} can be polled from worker
 * threads.
 */
public class ProgressMonitor {

    private final JDialog dialog;
    private final JLabel label;
    private final JProgressBar bar;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AtomicInteger completed = new AtomicInteger(0);
    private volatile int total = 1;

    public ProgressMonitor(String title, String initialLabel, Runnable onCancel) {
        dialog = new JDialog((Frame) null, title, false);
        dialog.setLayout(new BorderLayout());
        label = new JLabel(initialLabel);
        label.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        bar = new JProgressBar(0, 100);
        bar.setStringPainted(true);
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            cancelled.set(true);
            dialog.dispose();
            if (onCancel != null) {
                onCancel.run();
            }
        });
        dialog.add(label, BorderLayout.NORTH);
        dialog.add(bar, BorderLayout.CENTER);
        dialog.add(cancelButton, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
    }

    public void show() {
        SwingUtilities.invokeLater(() -> dialog.setVisible(true));
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    public void setTotal(int total) {
        this.total = Math.max(1, total);
    }

    public void step(String status) {
        int done = completed.incrementAndGet();
        int percent = (int) Math.round(done * 100.0 / total);
        SwingUtilities.invokeLater(() -> {
            if (status != null) {
                label.setText(status);
            }
            bar.setValue(percent);
        });
    }

    public void close() {
        SwingUtilities.invokeLater(dialog::dispose);
    }
}
