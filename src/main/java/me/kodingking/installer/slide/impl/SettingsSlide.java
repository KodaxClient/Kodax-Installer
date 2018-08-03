package me.kodingking.installer.slide.impl;

import me.kodingking.installer.InstallerMain;
import me.kodingking.installer.Settings;
import me.kodingking.installer.slide.AbstractSlide;
import me.kodingking.installer.utils.SwingUtil;

import javax.swing.*;
import java.awt.*;

public class SettingsSlide extends AbstractSlide {

    @Override
    public void initialize(JFrame frame) {
        JCheckBox optifineCheckbox = new JCheckBox("Use Optifine", Settings.USE_OPTIFINE);
        optifineCheckbox.setBounds(InstallerMain.width / 2 - 150, 50, 300, 20);
        optifineCheckbox.addActionListener(e -> Settings.USE_OPTIFINE = optifineCheckbox.isSelected());
        optifineCheckbox.setHorizontalAlignment(SwingConstants.CENTER);
        frame.add(optifineCheckbox);

        JButton continueButton = new JButton("Install Now");
        continueButton.setBounds((InstallerMain.width - 120) / 2, 160, 130, 50);
        continueButton.addActionListener(e -> InstallerMain.advance());
        SwingUtil.decorate(continueButton);
        frame.add(continueButton);
    }
}
