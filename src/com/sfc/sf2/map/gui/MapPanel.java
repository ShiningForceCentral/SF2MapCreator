/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sfc.sf2.map.gui;

import com.sfc.sf2.graphics.Tile;
import com.sfc.sf2.map.Map;
import com.sfc.sf2.map.block.MapBlock;
import com.sfc.sf2.map.layout.MapLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

/**
 *
 * @author wiz
 */
public class MapPanel extends JPanel  implements MouseListener, MouseMotionListener {
    
    private static final int DEFAULT_TILES_PER_ROW = 64*3;
    
    int lastMapX = 0;
    int lastMapY = 0;
    
    public static final int MODE_VIEW = 0;
    public static final int MODE_TILEPRIORITY = 1;
    
    private int currentMode = MODE_VIEW;
    
    private int tilesPerRow = DEFAULT_TILES_PER_ROW;
    private Map map;
    private MapLayout layout;
    private MapBlock[] blockset;
    private int currentDisplaySize = 1;
    
    private BufferedImage currentImage;
    private boolean redraw = true;
    private int renderCounter = 0;
    private boolean drawGrid = false;
    private boolean drawLPTiles = false;
    private boolean drawOrphanedTiles = false;
    
    private BufferedImage blocksImage;
    private BufferedImage gridImage;
    private BufferedImage lpTilesImage;
    private BufferedImage orphanedTilesImage;

    public MapPanel() {
        addMouseListener(this);
        addMouseMotionListener(this);
    }
   
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);   
        g.drawImage(buildImage(), 0, 0, this);       
    }
    
    public BufferedImage buildImage(){
        if (redraw) {
            if (map == null) {
                currentImage = null;
            } else {
                currentImage = buildImage(this.map,this.tilesPerRow);
                setSize(currentImage.getWidth(), currentImage.getHeight());
            }
            redraw = false;
        }
        return currentImage;
    }
    
    public BufferedImage buildImage(Map map, int tilesPerRow){
        renderCounter++;
        System.out.println("Map render "+renderCounter);
        this.map = map;
        this.layout = map.getLayout();
        if(redraw){
            int imageHeight = 64*3*8;
            currentImage = new BufferedImage(tilesPerRow*8, imageHeight , BufferedImage.TYPE_INT_ARGB);
            Graphics graphics = currentImage.getGraphics(); 
            graphics.drawImage(getBlocksImage(), 0, 0, null);
            if(drawLPTiles){
                graphics.drawImage(getLPTilesImage(), 0, 0, null);
            }
            if (drawOrphanedTiles){
                graphics.drawImage(getOrphanedTilesImage(), 0, 0, null);
            }
            if(drawGrid){
                graphics.drawImage(getGridImage(), 0, 0, null);
            }
            currentImage = resize(currentImage);
        }
                  
        return currentImage;
    }
    
    
    private BufferedImage getGridImage(){
        if(gridImage==null){
            gridImage = new BufferedImage(3*8*64, 3*8*64, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = (Graphics2D) gridImage.getGraphics(); 
            Color c = null;
            if(drawLPTiles){
                c = new Color(0,0,0,0.5f);
            }else{
                c = Color.BLACK;
            }
            g2.setColor(c);
            for(int i=0;i<64;i++){
                g2.drawLine(3*8+i*3*8, 0, 3*8+i*3*8, 3*8*64-1);
                g2.drawLine(0, 3*8+i*3*8, 3*8*64-1, 3*8+i*3*8);
            }
        }
        return gridImage;
    }
    private BufferedImage getBlocksImage(){
        if(blocksImage==null){
            MapBlock[] blocks = layout.getBlocks();          
            blocksImage = new BufferedImage(3*8*64, 3*8*64, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = (Graphics2D) blocksImage.getGraphics(); 
            for(int y=0;y<64;y++){
                for(int x=0;x<64;x++){
                    MapBlock block = blocks[y*64+x];
                    block.updatePixels();
                    BufferedImage blockImage = block.getIndexedColorImage();
                    g2.drawImage(blockImage, x*3*8, y*3*8, null);              
                }   
            } 
        }
        return blocksImage;
    }
    
    private BufferedImage getLPTilesImage(){
        if(lpTilesImage==null){
            lpTilesImage = new BufferedImage(3*8*64, 3*8*64, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = (Graphics2D) lpTilesImage.getGraphics(); 
            g2.setColor(new Color(1f, 1f, 1f, 0.8f)); 
            for(int y=0;y<64*3;y++){
                for(int x=0;x<64*3;x++){
                    int blockIndex = (y/3)*64+(x/3);
                    if(!this.map.getLayout().getBlocks()[blockIndex].getTiles()[(y%3)*3+x%3].isHighPriority()){
                       g2.fillRect(x*8, y*8, 8, 8);
                    }                     
                }
            }
        }
        return lpTilesImage;
    }
    
    private BufferedImage getOrphanedTilesImage() {
        if (orphanedTilesImage == null && map.getOrphanTiles() != null) {
            orphanedTilesImage = new BufferedImage(3*8*64, 3*8*64, BufferedImage.TYPE_INT_ARGB);
            Tile[] orphanedTiles = map.getOrphanTiles();
            Graphics graphics = orphanedTilesImage.getGraphics(); 
            for (int y = 0; y < 64*3; y++) {
                for (int x = 0; x < 64*3; x++) {
                    int blockIndex = (y/3)*64+(x/3);
                    Tile tile = this.map.getLayout().getBlocks()[blockIndex].getTiles()[(y%3)*3+x%3];
                    for (int orphan = 0; orphan < orphanedTiles.length; orphan++) {
                        if (tile.equals(orphanedTiles[orphan])) {
                            graphics.setColor(new Color(1f, 1f, 0f, 0.5f));
                            graphics.fillRect(x*8, y*8, 8, 8);
                            graphics.setColor(new Color(0f, 0f, 0f, 1f));
                            graphics.drawRect(x*8, y*8, 8, 8);
                        }
                    }
                }
            }
        }
        return orphanedTilesImage;
    }
    
    public void resize(int size){
        this.currentDisplaySize = size;
        currentImage = resize(currentImage);
    }
    
    private BufferedImage resize(BufferedImage image){
        BufferedImage newImage = new BufferedImage(image.getWidth()*currentDisplaySize, image.getHeight()*currentDisplaySize, BufferedImage.TYPE_INT_ARGB);
        Graphics g = newImage.getGraphics();
        g.drawImage(image, 0, 0, image.getWidth()*currentDisplaySize, image.getHeight()*currentDisplaySize, null);
        g.dispose();
        return newImage;
    }
    
    public void clearAllCachedImages() {
        redraw = true;
        blocksImage = null;
        gridImage = null;
        lpTilesImage = null;
        orphanedTilesImage = null;
    }
    
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(getWidth(), getHeight());
    }
    
    public int getTilesPerRow() {
        return tilesPerRow;
    }

    public void setTilesPerRow(int tilesPerRow) {
        this.tilesPerRow = tilesPerRow;
    }

    public int getCurrentDisplaySize() {
        return currentDisplaySize;
    }

    public void setCurrentDisplaySize(int currentDisplaySize) {
        this.currentDisplaySize = currentDisplaySize;
        redraw = true;
    }

    public MapLayout getMapLayout() {
        return layout;
    }

    public void setMapLayout(MapLayout layout) {
        this.layout = layout;
    }


    public MapBlock[] getBlockset() {
        return blockset;
    }

    public void setBlockset(MapBlock[] blockset) {
        this.blockset = blockset;
    }

    public boolean isRedraw() {
        return redraw;
    }

    public void setRedraw(boolean redraw) {
        this.redraw = redraw;
    }

    public boolean isDrawGrid() {
        return drawGrid;
    }

    public void setDrawGrid(boolean drawGrid) {
        this.drawGrid = drawGrid;
        this.redraw = true;
    }

    public Map getMap() {
        return map;
    }

    public void setMap(Map map) {
        this.map = map;
    }

    public boolean isDrawLPTiles() {
        return drawLPTiles;
    }

    public void setDrawLPTiles(boolean drawLPTiles) {
        this.drawLPTiles = drawLPTiles;
        if(drawLPTiles){
            this.currentMode = MODE_TILEPRIORITY;
        }else{
            this.currentMode = MODE_VIEW;
        }
        this.lpTilesImage = null;
        this.redraw = true;
    }

    public boolean isDrawOrphanedTiles() {
        return drawOrphanedTiles;
    }

    public void setDrawOrphanedTiles(boolean drawOrphanedTiles) {
        this.drawOrphanedTiles = drawOrphanedTiles;
        this.orphanedTilesImage = null;
        this.redraw = true;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }
    @Override
    public void mouseEntered(MouseEvent e) {

    }
    @Override
    public void mouseExited(MouseEvent e) {

    }
    @Override
    public void mousePressed(MouseEvent e) {
        int x = e.getX() / (currentDisplaySize * 8);
        int y = e.getY() / (currentDisplaySize * 8);
        //System.out.println(x+":"+y);
        switch (currentMode) {
            case MODE_TILEPRIORITY :
                switch (e.getButton()) {
                    case MouseEvent.BUTTON1:
                        lastMapX = x;
                        lastMapY = y;
                        break;
                    case MouseEvent.BUTTON3:
                        lastMapX = x;
                        lastMapY = y;
                        break;
                    default:
                        break;
                } 
                break;
            default:
                break;
        }

        this.repaint();
        //System.out.println("Map press "+e.getButton()+" "+x+" - "+y);
    }
    @Override
    public void mouseReleased(MouseEvent e) {
        int endX = e.getX() / (currentDisplaySize * 8);
        int endY = e.getY() / (currentDisplaySize * 8);
        boolean priorityVal = false;
        switch (e.getButton()) {
            case MouseEvent.BUTTON1:
                priorityVal = true;
                break;
            case MouseEvent.BUTTON3:
                priorityVal = false;
                break;
            default:
                break;
        }       

        if(currentMode==MODE_TILEPRIORITY){    
            /* Zone change */
            int xStart;
            int xEnd;
            int yStart;
            int yEnd;
            if(endX>lastMapX){
                xStart = lastMapX;
                xEnd = endX;
            }else{
                xStart = endX;
                xEnd = lastMapX;
            }
            if(endY>lastMapY){
                yStart = lastMapY;
                yEnd = endY;
            }else{
                yStart = endY;
                yEnd = lastMapY;
            }           
            System.out.println(xStart+":"+yStart+".."+xEnd+":"+yEnd+":"+priorityVal);
            for(int y=yStart;y<=yEnd;y++){
                for(int x=xStart;x<=xEnd;x++){
                    int blockIndex = (y/3)*64+(x/3);
                    this.map.getLayout().getBlocks()[blockIndex].getTiles()[(y%3)*3+x%3].setHighPriority(priorityVal);  
                }
            }
            this.lpTilesImage = null;
            this.redraw=true;
            this.repaint();
        }
    }
    
    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        
    }
    
    
}
