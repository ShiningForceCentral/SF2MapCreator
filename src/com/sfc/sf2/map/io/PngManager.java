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
import com.sfc.sf2.palette.graphics.PaletteDecoder;
import com.sfc.sf2.palette.graphics.PaletteEncoder;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
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
    
    public static Map importPngMap(String filepath, String flagsPath, String hptilesPath, String targetPaletteFilepath){
        System.out.println("com.sfc.sf2.map.io.PngManager.importPng() - Importing PNG files ...");
        Map map = new Map();
        try{
            Tile[] tiles = loadPngFile(filepath, hptilesPath, targetPaletteFilepath);
            map.setTiles(tiles);
            if(tiles!=null){
                if(tiles.length==9*64*64){  
                   System.out.println(new Date()+" - Created tileset with " + tiles.length + " tiles."); 
                   MapBlock[] blockset = loadMapBlocks(tiles);          
                   System.out.println(new Date()+" - Created blockset with " + blockset.length + " bocks.");        
                   map.setBlocks(blockset);       
                   MapLayout ml = new MapLayout();
                   ml.setBlocks(blockset);
                   loadFlags(ml, flagsPath);
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
    
    public static void loadFlags(MapLayout layout, String flagsPath){
            Path fpath = Paths.get(flagsPath);
            if(fpath.toFile().exists() && !fpath.toFile().isDirectory()){
                try {
                    int blockIndex = 0;
                    int cursor = 0;
                    Scanner scan = new Scanner(fpath);
                    while(scan.hasNext()){
                        String line = scan.nextLine();
                        while(cursor<line.length()-1){
                            String flags = (cursor<line.length()-2)?line.substring(cursor, cursor+2):line.substring(cursor);
                            int flagsValue = Integer.parseInt(flags, 16) << 8;
                            layout.getBlocks()[blockIndex].setFlags(flagsValue);
                            cursor+=2;
                            blockIndex++;
                        }
                        cursor=0;
                    }
                } catch (IOException ex) {
                    Logger.getLogger(PngManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
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
    
    
    public static Tile[] loadPngFile(String filepath, String hptilespath, String targetPaletteFilepath) throws IOException{
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
                    Color[] palette = null;   
                    Path palettepath = Paths.get(targetPaletteFilepath);
                    IndexColorModel icm = (IndexColorModel) cm;
                    if(palettepath.toFile().exists()){
                        byte[] paletteData = Files.readAllBytes(palettepath);
                        palette = PaletteDecoder.parsePalette(paletteData);
                    }else{
                        palette = PaletteEncoder.producePalette(buildColors(icm));
                    }

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
                                                    int b = PaletteDecoder.VALUE_MAP.get(PaletteEncoder.VALUE_ARRAY[color.getBlue()]&0xE);;
                                                    int g = PaletteDecoder.VALUE_MAP.get(PaletteEncoder.VALUE_ARRAY[color.getGreen()]&0xE);;
                                                    int r = PaletteDecoder.VALUE_MAP.get(PaletteEncoder.VALUE_ARRAY[color.getRed()]&0xE);;
                                                    Color standardizedColor = new Color(r,g,b);
                                                    cFound = false;
                                                    for(int c=0;c<16;c++){
                                                        if(standardizedColor.equals(palette[c])){
                                                            tile.setPixel(i, j, c);
                                                            cFound = true;
                                                            break;
                                                        }
                                                    }
                                                    if(!cFound){
                                                        //TODO find nearest color with lowest r*g*b diff
                                                        int diff = Integer.MAX_VALUE;
                                                        int index = 0;
                                                        for(int c=0;c<16;c++){
                                                            int bDiff = Math.abs(palette[c].getBlue()-color.getBlue())+1;
                                                            int gDiff = Math.abs(palette[c].getGreen()-color.getGreen())+1;
                                                            int rDiff = Math.abs(palette[c].getRed()-color.getRed())+1;
                                                            int candidateDiff = bDiff * gDiff * rDiff;
                                                            if(candidateDiff<=diff){
                                                                diff = candidateDiff;
                                                                index = c;
                                                            }
                                                        }
                                                        
                                                        tile.setPixel(i, j, index);
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
