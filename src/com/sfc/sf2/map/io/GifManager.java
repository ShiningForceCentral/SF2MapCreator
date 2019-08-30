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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
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
public class GifManager {
    
    public static final int MAP_PIXEL_WIDTH = 64*3*8;
    public static final int MAP_PIXEL_HEIGHT = 64*3*8;
    
    public static Map importGifMap(String filepath, String flagsPath, String hptilesPath, String targetPaletteFilepath){
        System.out.println("com.sfc.sf2.map.io.GifManager.importGif() - Importing GIF files ...");
        Map map = new Map();
        try{
            Tile[] tiles = loadGifFile(filepath, hptilesPath, targetPaletteFilepath);
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
             System.err.println("com.sfc.sf2.map.io.GifManager.importGif() - Error while parsing graphics data : "+e);
        }        
        System.out.println("com.sfc.sf2.map.io.GifgManager.importGif() - GIF files imported.");
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
    
    
    public static Tile[] loadGifFile(String filepath, String hptilespath, String targetPaletteFilepath) throws IOException{
        Tile[] tiles = null;
        boolean cFound = false;
        try{
            Path path = Paths.get(filepath);
            if(path.toFile().exists()){
                BufferedImage img = ImageIO.read(path.toFile());
                ColorModel cm = img.getColorModel();
                Color[] palette = null;   
                Path palettepath = Paths.get(targetPaletteFilepath);
                if(!(cm instanceof IndexColorModel)){
                    if(palettepath.toFile().exists() && palettepath.toFile().isFile()){
                        System.out.println("GIF FORMAT WARNING : COLORS ARE NOT INDEXED AS EXPECTED. Using external palette "+palettepath.toString());
                        byte[] paletteData = Files.readAllBytes(palettepath);
                        palette = PaletteDecoder.parsePalette(paletteData);
                    }else{
                    System.out.println("GIF FORMAT WARNING : COLORS ARE NOT INDEXED AS EXPECTED. Using original game default map palette 0");
                    try {
                        InputStream is = GifManager.class.getResourceAsStream("basemappalette0.bin");
                        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                        int nRead;
                        byte[] data = new byte[32];
                        while ((nRead = is.read(data, 0, data.length)) != -1) {
                          buffer.write(data, 0, nRead);
                        }
                        byte[] paletteData = buffer.toByteArray();
                        palette = PaletteDecoder.parsePalette(paletteData);
                    } catch (IOException ex) {
                        Logger.getLogger(GifManager.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    }                    
                }else if(palettepath.toFile().exists()){
                    System.out.println("Using external palette "+palettepath.toString());
                    byte[] paletteData = Files.readAllBytes(palettepath);
                    palette = PaletteDecoder.parsePalette(paletteData);
                }else{
                    System.out.println("Using image's indexed palette");
                    IndexColorModel icm = (IndexColorModel) cm;
                    palette = PaletteDecoder.parsePalette(PaletteEncoder.producePalette(buildColors(icm)));
                }

                int imageWidth = img.getWidth();
                int imageHeight = img.getHeight();
                if(imageWidth%8!=0 || imageHeight%8!=0){
                    System.out.println("GIF FORMAT WARNING : DIMENSIONS ARE NOT MULTIPLES OF 8. (8 pixels per tile)");
                }else if(imageWidth!=MAP_PIXEL_WIDTH || imageHeight!=MAP_PIXEL_HEIGHT){
                    System.out.println("GIF FORMAT WARNING : DIMENSIONS ARE NOT "+MAP_PIXEL_WIDTH+"px*"+MAP_PIXEL_HEIGHT+"px AS EXPECTED");
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
                                    int a = PaletteDecoder.VALUE_MAP.get(PaletteEncoder.VALUE_ARRAY[color.getAlpha()]&0xE);;
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
                                        if(a>=128){
                                            index=0;
                                        }else{
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
             System.err.println("com.sfc.sf2.map.io.GifManager.importGif() - Error while parsing GIF data : "+e);
             e.printStackTrace();
             throw e;
        }                
        return tiles;                
    }    
    
    private static Color[] buildColors(IndexColorModel icm){
        Color[] colors = new Color[16];
        if(icm.getMapSize()>16){
            System.out.println("com.sfc.sf2.map.io.GifManager.buildColors() - GIF FORMAT HAS MORE THAN 16 INDEXED COLORS : "+icm.getMapSize());
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
    
    
    
    
    
    
    
    
    
    public static void exportGif(MapPanel mapPanel, String filepath){
        try {
            System.out.println("com.sfc.sf2.map.io.GifManager.exportGif() - Exporting GIF files ...");
            writeGifFile(mapPanel,filepath);    
            System.out.println("com.sfc.sf2.map.io.GifManager.exportGif() - GIF files exported.");
        } catch (Exception ex) {
            Logger.getLogger(GifManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
                
    }    
    
    public static void writeGifFile(MapPanel mapPanel, String filepath){
        try {
            BufferedImage image = mapPanel.buildImage();
            File outputfile = new File(filepath);
            ImageIO.write(image, "gif", outputfile);
            System.out.println("GIF file exported : " + outputfile.getAbsolutePath());
        } catch (Exception ex) {
            Logger.getLogger(GifManager.class.getName()).log(Level.SEVERE, null, ex);
        }       
    }
       
    
    public static void exportHPTiles(Map map, String hpTilesPath){
        try {
            System.out.println("com.sfc.sf2.map.io.GifManager.exportHPTiles() - Exporting HP Tiles file ...");
            writeMapHpTilesFile(map,hpTilesPath);    
            System.out.println("com.sfc.sf2.map.io.GifManager.exportHPTiles() - HP Tiles file exported.");
        } catch (Exception ex) {
            Logger.getLogger(GifManager.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(GifManager.class.getName()).log(Level.SEVERE, null, ex);
        }       
    }        
    
}
