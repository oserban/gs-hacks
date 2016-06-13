/*
 * Copyright (c) 2016. This is a file, part of the GS Hacks project
 *  Everything is provided as it is, without any licence and guarantee
 */

package org.gs.hacks;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URL;

/**
 * @author Ovidiu Serban, ovidiu@roboslang.org
 * @version 1, 11/18/11
 */
public class ImageHelper {
    public static Icon buildIcon(final String relativePath, final Class loader) {
        URL resource = loader.getResource(relativePath);
        if (resource != null) {
            return new ImageIcon(resource);
        } else {
            System.err.println("[ImageHelper] Invalid resource: " + relativePath + " Loader: " + loader.getName());
            throw new IllegalArgumentException("Invalid resource");
        }
    }

    public static Image buildImage(final String relativePath, final Class loader) {
        URL resource = loader.getResource(relativePath);
        if (resource != null) {
            try {
                return ImageIO.read(resource);
            } catch (IOException e) {
                throw new IllegalArgumentException("Invalid resource", e);
            }
        } else {
            System.err.println("[ImageHelper] Invalid resource: " + relativePath + " Loader: " + loader.getName());
            throw new IllegalArgumentException("Invalid resource");
        }
    }
}
