import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;
import javax.imageio.ImageIO;

public class Renderer extends JFrame {
    Settings s;
    private final int TEX_SIZE = 64;
    BufferedImage[] textures;

    public Renderer(Settings s) {
        super("Radium");
        this.s = s;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(s.getScreenWidth(), s.getScreenHeight());
        setLocationRelativeTo(null);
        setVisible(true);

        try {
            textures = new BufferedImage[8];
            textures[0] = ImageIO.read(new File("resources/textures/eagle.png"));
            textures[1] = ImageIO.read(new File("resources/textures/redbrick.png"));
            textures[2] = ImageIO.read(new File("resources/textures/purplestone.png"));
            textures[3] = ImageIO.read(new File("resources/textures/greystone.png"));
            textures[4] = ImageIO.read(new File("resources/textures/bluestone.png"));
            textures[5] = ImageIO.read(new File("resources/textures/mossy.png"));
            textures[6] = ImageIO.read(new File("resources/textures/wood.png"));
            textures[7] = ImageIO.read(new File("resources/textures/colorstone.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void drawFrame(Player p, byte[][] map) {
        BufferStrategy bs = this.getBufferStrategy();
        if (bs == null) {
            createBufferStrategy(2);
            return;
        }
        Graphics g = bs.getDrawGraphics();
        BufferedImage bi = new BufferedImage(s.getScreenWidth(), s.getScreenHeight(), BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < s.getScreenHeight(); y++) {
            double rayDirX0 = p.getDirX() - p.getPlaneX();
            double rayDirY0 = p.getDirY() - p.getPlaneY();
            double rayDirX1 = p.getDirX() + p.getPlaneX();
            double rayDirY1 = p.getDirY() + p.getPlaneY();

            int yPos = y - s.getScreenHeight() / 2;
            double zPos = 0.5 * s.getScreenHeight();
            double rowDistance = zPos / yPos;

            double floorStepX = rowDistance * (rayDirX1 - rayDirX0) / s.getScreenWidth();
            double floorStepY = rowDistance * (rayDirY1 - rayDirY0) / s.getScreenWidth();
            double floorX = p.getPosX() + rowDistance * rayDirX0;
            double floorY = p.getPosY() + rowDistance * rayDirY0;

            for (int x = 0; x < s.getScreenWidth(); ++x) {
                int cellX = (int) (floorX);
                int cellY = (int) (floorY);

                int tx = (int) (TEX_SIZE * (floorX - cellX)) & (TEX_SIZE - 1);
                int ty = (int) (TEX_SIZE * (floorY - cellY)) & (TEX_SIZE - 1);

                floorX += floorStepX;
                floorY += floorStepY;

                int floorTexture = 3;
                int ceilingTexture = 6;
                Color color;

                color = new Color(textures[floorTexture].getRGB(tx, ty));
                color = color.darker();
                color = color.darker();
                color = color.darker();
                bi.setRGB(x, y, color.getRGB());

                color = new Color(textures[ceilingTexture].getRGB(tx, ty));
                color = color.darker();
                color = color.darker();
                bi.setRGB(x, s.getScreenHeight() - y - 1, color.getRGB());
            }
        }

        for (int x = 0; x < bi.getWidth(); x++) {

            // for (int y = 0; y < bi.getHeight(); y++)
            // bi.setRGB(x, y, Color.BLACK.getRGB());

            double cameraX = 2 * x / (double) bi.getWidth() - 1;
            double rayDirX = p.getDirX() + p.getPlaneX() * cameraX;
            double rayDirY = p.getDirY() + p.getPlaneY() * cameraX;

            int mapX = (int) p.getPosX();
            int mapY = (int) p.getPosY();

            double sideDistX, sideDistY;

            double deltaDistX = Math.abs(1 / rayDirX);
            double deltaDistY = Math.abs(1 / rayDirY);
            double perpWallDist;

            int stepX, stepY;

            boolean hit = false;
            int side = -1;

            if (rayDirX < 0) {
                stepX = -1;
                sideDistX = (p.getPosX() - mapX) * deltaDistX;
            } else {
                stepX = 1;
                sideDistX = (mapX + 1.0 - p.getPosX()) * deltaDistX;
            }
            if (rayDirY < 0) {
                stepY = -1;
                sideDistY = (p.getPosY() - mapY) * deltaDistY;
            } else {
                stepY = 1;
                sideDistY = (mapY + 1.0 - p.getPosY()) * deltaDistY;
            }

            while (!hit) {
                if (sideDistX < sideDistY) {
                    sideDistX += deltaDistX;
                    mapX += stepX;
                    side = 0;
                } else {
                    sideDistY += deltaDistY;
                    mapY += stepY;
                    side = 1;
                }
                if (map[mapX][mapY] > 0)
                    hit = true;
            }

            if (side == 0) {
                perpWallDist = (mapX - p.getPosX() + (1 - stepX) / 2) / rayDirX;
            } else {
                perpWallDist = (mapY - p.getPosY() + (1 - stepY) / 2) / rayDirY;
            }

            int lineHeight = (int) (s.getScreenHeight() / perpWallDist);

            int drawStart = -lineHeight / 2 + s.getScreenHeight() / 2;
            if (drawStart < 0)
                drawStart = 0;
            int drawEnd = lineHeight / 2 + s.getScreenHeight() / 2;
            if (drawEnd >= s.getScreenHeight())
                drawEnd = s.getScreenHeight() - 1;

            int texNum = map[mapX][mapY] - 1;

            double wallX;
            if (side == 0)
                wallX = p.getPosY() + perpWallDist * rayDirY;
            else
                wallX = p.getPosX() + perpWallDist * rayDirX;
            wallX -= Math.floor(wallX);

            int texX = (int) (wallX * (double) TEX_SIZE);
            if ((side == 0 && rayDirX > 0) || (side == 1 && rayDirY < 0))
                texX = TEX_SIZE - texX - 1;

            double step = 1.0 * TEX_SIZE / lineHeight;
            double texPos = (drawStart - s.getScreenHeight() / 2 + lineHeight / 2) * step;

            for (int y = drawStart; y < drawEnd; y++) {
                int texY = (int) texPos & (TEX_SIZE - 1);
                texPos += step;
                Color color = new Color(textures[texNum].getRGB(texX, texY));
                if (side == 1)
                    color = color.darker();
                bi.setRGB(x, y, color.getRGB());
            }
        }

        g.drawImage(bi, 0, 0, null);
        g.dispose();
        bs.show();

    }
}
