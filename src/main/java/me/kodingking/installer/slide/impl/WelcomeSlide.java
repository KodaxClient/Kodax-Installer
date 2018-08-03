package me.kodingking.installer.slide.impl;

import me.kodingking.installer.InstallerMain;
import me.kodingking.installer.slide.AbstractSlide;
import me.kodingking.installer.utils.SwingUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class WelcomeSlide extends AbstractSlide {

    @Override
    public void initialize(JFrame frame) {
        try {
            JLabel icon = new JLabel();
            icon.setIcon(new ImageIcon(ImageIO.read(getClass().getResourceAsStream("/icon/xenon_logo_cropped.png")).getScaledInstance(242, 89, Image.SCALE_DEFAULT)));
            icon.setBounds((InstallerMain.width - 242) / 2, 40, 242, 89);
            frame.add(icon);

            JButton continueButton = new JButton("Continue");
            continueButton.setBounds((InstallerMain.width - 120) / 2, 160, 130, 50);
            continueButton.addActionListener(e -> InstallerMain.advance());
            SwingUtil.decorate(continueButton);

            frame.add(continueButton);
        } catch (IOException ignored) {
        }
    }

}
