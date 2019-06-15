/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sfc.sf2.map.io;

import com.sfc.sf2.graphics.Tile;
import com.sfc.sf2.graphics.layout.DefaultLayout;
import com.sfc.sf2.map.Map;
import com.sfc.sf2.map.block.MapBlock;
import com.sfc.sf2.map.gui.MapPanel;
import com.sfc.sf2.map.layout.MapLayout;
import com.sfc.sf2.map.layout.layout.MapLayoutLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author wiz
 */
public class PngManager {
    
    public static final int MAP_PIXEL_WIDTH = 64*3*8;
    public static final int MAP_PIXEL_HEIGHT = 64*3*8;
    
    public static Map importPngMap(String filepath, String hptilesPath){
        System.out.println("com.sfc.sf2.map.io.PngManager.importPng() - Importing PNG files ...");
        Map map = new Map();
        try{
            Tile[] tiles = loadPngFile(filepath, hptilesPath);
            map.setTiles(tiles);
            if(tiles!=null){
                if(tiles.length==9*64*64){  
                   System.out.println(new Date()+" - Created tileset with " + tiles.length + " tiles."); 
                   MapBlock[] blockset = loadMapBlocks(tiles);          
                   System.out.println(new Date()+" - Created blockset with " + blockset.length + " bocks.");        
                   map.setBlocks(blockset);       
                   MapLayout ml = new MapLayout();
                   ml.setBlocks(blockset);
                   map.setLayout(ml);
                }else{
                    System.out.println("Could not create map because of wrong length : tiles=" + tiles.length);
                }
            }
        }catch(Exception e){
             System.err.println("com.sfc.sf2.map.io.PngManager.importPng() - Error while parsing graphics data : "+e);
        }        
        System.out.println("com.sfc.sf2.map.io.PngManager.importPng() - PNG files imported.");
        return map;                
    }
    
    public static MapBlock[] loadMapBlocks(Tile[] tiles){
        MapBlock[] blocks = new MapBlock[64*64];
        
        for(int y=0;y<64;y++){
            for(int x=0;x<64;x++){
                MapBlock mb = new MapBlock();
                mb.setIndex(y*64+x);
                Tile[] mbTiles = new Tile[9];
                mbTiles[0] = tiles[(y*3+0)*3*64+(x*3+0)];
                mbTiles[1] = tiles[(y*3+0)*3*64+(x*3+1)];
                mbTiles[2] = tiles[(y*3+0)*3*64+(x*3+2)];
                mbTiles[3] = tiles[(y*3+1)*3*64+(x*3+0)];
                mbTiles[4] = tiles[(y*3+1)*3*64+(x*3+1)];
                mbTiles[5] = tiles[(y*3+1)*3*64+(x*3+2)];
                mbTiles[6] = tiles[(y*3+2)*3*64+(x*3+0)];
                mbTiles[7] = tiles[(y*3+2)*3*64+(x*3+1)];
                mbTiles[8] = tiles[(y*3+2)*3*64+(x*3+2)];
                mb.setTiles(mbTiles);
                blocks[y*64+x] = mb;
            }
        }
        
        return blocks;
    }
    
    
    public static Tile[] loadPngFile(String filepath, String hptilespath) throws IOException{
        Tile[] tiles = null;
        boolean cFound = false;
        try{
            Path path = Paths.get(filepath);
            if(path.toFile().exists()){
                BufferedImage img = ImageIO.read(path.toFile());
                ColorModel cm = img.getColorModel();
                if(!(cm instanceof IndexColorModel)){
                    System.out.println("PNG FORMAT ERROR : COLORS ARE NOT INDEXED AS EXPECTED.");
                }else{
                    IndexColorModel icm = (IndexColorModel) cm;
                    Color[] palette = buildColors(icm);

                    int imageWidth = img.getWidth();
                    int imageHeight = img.getHeight();
                    if(imageWidth%8!=0 || imageHeight%8!=0){
                        System.out.println("PNG FORMAT WARNING : DIMENSIONS ARE NOT MULTIPLES OF 8. (8 pixels per tile)");
                    }else if(imageWidth!=MAP_PIXEL_WIDTH || imageHeight!=MAP_PIXEL_HEIGHT){
                        System.out.println("PNG FORMAT WARNING : DIMENSIONS ARE NOT "+MAP_PIXEL_WIDTH+"px*"+MAP_PIXEL_HEIGHT+"px AS EXPECTED");
                    }else {
                        tiles = new Tile[(imageWidth/8)*(imageHeight/8)];
                        int tileId = 0;
                                    for(int tileLine=0;tileLine<64*3;tileLine++){
                                        for(int tileColumn=0;tileColumn<64*3;tileColumn++){
                                            int x = (tileColumn)*8;
                                            int y = (tileLine)*8;
                                            //System.out.println("Building tile from coordinates "+x+":"+y);
                                            Tile tile = new Tile();
                                            tile.setId(tileId);
                                            tile.setPalette(palette);
                                            for(int j=0;j<8;j++){
                                                for(int i=0;i<8;i++){
                                                    Color color = new Color(img.getRGB(x+i,y+j));
                                                    cFound = false;
                                                    for(int c=0;c<16;c++){
                                                        if(color.equals(palette[c])){
                                                            tile.setPixel(i, j, c);
                                                            cFound = true;
                                                            break;
                                                        }
                                                    }
                                                    if(!cFound){
                                                        System.out.println("PNG FORMAT WARNING : UNRECOGNIZED COLOR AT "+i+":*"+j+" : "+color.toString());
                                                        tile.setPixel(i, j, 0);
                                                    }
                                                }
                                            }
                                            //System.out.println(tile);
                                            tiles[tileId] = tile;   
                                            tileId++;
                                        }
                                    }        

                    }
                }                
            }
            Path hptpath = Paths.get(hptilespath);
            if(hptpath.toFile().exists() && !hptpath.toFile().isDirectory()){
                
                int lineIndex = 0;
                int cursor = 0;
                Scanner scan = new Scanner(hptpath);
                while(scan.hasNext()){
                    String line = scan.nextLine();
                    while(cursor<line.length()){
                        if(line.charAt(cursor)=='H'){
                            tiles[lineIndex*192+cursor].setHighPriority(true);
                        }
                        cursor++;
                    }
                    cursor=0;
                    lineIndex++;
                }
            }
        }catch(Exception e){
             System.err.println("com.sfc.sf2.map.io.PngManager.importPng() - Error while parsing PNG data : "+e);
             throw e;
        }                
        return tiles;                
    }
    
    private static Color[] buildColors(IndexColorModel icm){
        Color[] colors = new Color[16];
        if(icm.getMapSize()>16){
            System.out.println("com.sfc.sf2.map.io.PngManager.buildColors() - PNG FORMAT HAS MORE THAN 16 INDEXED COLORS : "+icm.getMapSize());
        }
        byte[] reds = new byte[icm.getMapSize()];
        byte[] greens = new byte[icm.getMapSize()];
        byte[] blues = new byte[icm.getMapSize()];
        icm.getReds(reds);
        icm.getGreens(greens);
        icm.getBlues(blues);
        for(int i=0;i<16;i++){
            colors[i] = new Color((int)(reds[i]&0xff),(int)(greens[i]&0xff),(int)(blues[i]&0xff));
        }
        return colors;
    }
    
    
    
    
    
    
    
    
    
    public static void exportPng(MapPanel mapPanel, String filepath){
        try {
            System.out.println("com.sfc.sf2.map.io.PngManager.exportPng() - Exporting PNG files ...");
            writePngFile(mapPanel,filepath);    
            System.out.println("com.sfc.sf2.map.io.PngManager.exportPng() - PNG files exported.");
        } catch (Exception ex) {
            Logger.getLogger(PngManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
                
    }    
    
    public static void writePngFile(MapPanel mapPanel, String filepath){
        try {
            BufferedImage image = mapPanel.buildImage();
            File outputfile = new File(filepath);
            ImageIO.write(image, "png", outputfile);
            System.out.println("PNG file exported : " + outputfile.getAbsolutePath());
        } catch (Exception ex) {
            Logger.getLogger(PngManager.class.getName()).log(Level.SEVERE, null, ex);
        }       
    }
       
    
    public static void exportHPTiles(Map map, String hpTilesPath){
        try {
            System.out.println("com.sfc.sf2.map.io.PngManager.exportPng() - Exporting HP Tiles file ...");
            writeMapHpTilesFile(map,hpTilesPath);    
            System.out.println("com.sfc.sf2.map.io.PngManager.exportPng() - HP Tiles file exported.");
        } catch (Exception ex) {
            Logger.getLogger(PngManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
                
    }    
    
    public static void writeMapHpTilesFile(Map map, String filepath){
        try {
            File outputfile = new File(filepath);
            BufferedWriter bw = new BufferedWriter(new FileWriter(outputfile));
            StringBuilder sb = new StringBuilder();
            for(int y=0;y<64*3;y++){
                for(int x=0;x<64;x++){
                    int blockIndex = (y/3)*64+x;
                    //System.out.println(y+":"+x+"->"+blockIndex+"->"+(int)((y%3)*3+0)+","+(int)((y%3)*3+1)+","+(int)((y%3)*3+2));
                    sb.append((map.getLayout().getBlocks()[blockIndex].getTiles()[(y%3)*3+0].isHighPriority())?"H":"L");
                    sb.append((map.getLayout().getBlocks()[blockIndex].getTiles()[(y%3)*3+1].isHighPriority())?"H":"L");
                    sb.append((map.getLayout().getBlocks()[blockIndex].getTiles()[(y%3)*3+2].isHighPriority())?"H":"L");
                }
                sb.append("\n");
            }
            bw.write(sb.toString());
            bw.close();
            System.out.println("HP Tiles file exported : " + outputfile.getAbsolutePath());
        } catch (Exception ex) {
            Logger.getLogger(PngManager.class.getName()).log(Level.SEVERE, null, ex);
        }       
    }        
    
}
