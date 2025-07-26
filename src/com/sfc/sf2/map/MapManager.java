/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sfc.sf2.map;

import com.sfc.sf2.graphics.GraphicsManager;
import com.sfc.sf2.graphics.Tile;
import com.sfc.sf2.graphics.compressed.StackGraphicsDecoder;
import com.sfc.sf2.map.block.MapBlock;
import com.sfc.sf2.map.block.io.MetaManager;
import com.sfc.sf2.map.io.RawImageManager;
import com.sfc.sf2.palette.Palette;
import com.sfc.sf2.palette.PaletteManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author wiz
 */
public class MapManager {
    
    com.sfc.sf2.map.layout.io.DisassemblyManager layoutDisasm = new com.sfc.sf2.map.layout.io.DisassemblyManager();
    
    private Map map;
    
    public void importPng(String imagePath, String flagsPath, String hptilesPath){
        System.out.println("com.sfc.sf2.map.MapManager.importPng() - Importing Image ...");
        map = RawImageManager.importMapFromRawImage(imagePath,flagsPath,hptilesPath);
        System.out.println("com.sfc.sf2.map.MapManager.importPng() - Image imported.");
    }
    
    public void importGif(String imagePath, String flagsPath, String hptilesPath){
        System.out.println("com.sfc.sf2.map.MapManager.importGif() - Importing Image ...");
        map = RawImageManager.importMapFromRawImage(imagePath,flagsPath,hptilesPath);
        System.out.println("com.sfc.sf2.map.MapManager.importGif() - Image imported.");
    }
    
    public void importMapPaletteAndTilesets(String palettesPath, String tilesetsPath, String tilesetsFilePath){
        System.out.println("com.sfc.sf2.map.MapManager.importMapPaletteAndTilesets() - Importing tilets data ...");
        try {
            String[] paths = layoutDisasm.importTilesetsFile(palettesPath, tilesetsPath, tilesetsFilePath);
            Palette palette = com.sfc.sf2.palette.io.DisassemblyManager.importDisassembly(paths[0]);
            Tile[][] tilesets = new Tile[paths.length-1][];
            for (int i = 0; i < tilesets.length; i++) {
                tilesets[i] = com.sfc.sf2.graphics.io.DisassemblyManager.importDisassembly(paths[i+1], palette, GraphicsManager.COMPRESSION_STACK);
            }
            map.setPalette(palette);
            map.setTilesets(tilesets);
        }
        catch (Exception e) {
             System.err.println("com.sfc.sf2.map.MapManager.importMapPaletteAndTilesets() - Error while parsing tileset data : "+e);
        }
        System.out.println("com.sfc.sf2.map.MapManager.importMapPaletteAndTilesets() - Tilets data imported.");
    }
    
    public void importBaseTilesets(String[] tilesetPaths, boolean chestGraphics, String targetPaletteFilepath){
        System.out.println("com.sfc.sf2.map.MapManager.importDisassembly() - Importing disassembly ...");
        Tile[][] tilesets = new Tile[5][];
        map.setTilesets(tilesets);
        Palette palette = null;
        Path palettepath = Paths.get(targetPaletteFilepath);
        if(palettepath.toFile().exists() && palettepath.toFile().isFile()){
            palette = com.sfc.sf2.palette.io.DisassemblyManager.importDisassembly(targetPaletteFilepath);
        }else{
            palette = map.getTiles()[0].getPalette();
        }
        map.setPalette(palette);
        
        Tile emptyTile = createEmptyTile();
        emptyTile.setPalette(palette);
        for(int i=0;i<tilesets.length;i++){
            String tpath = tilesetPaths[i];
            Path path = Paths.get(tpath);
            if(path.toFile().exists()&&!path.toFile().isDirectory()){
                tilesets[i] = com.sfc.sf2.graphics.io.DisassemblyManager.importDisassembly(tpath, palette, GraphicsManager.COMPRESSION_STACK);
            }else{
                if(i!=4||!chestGraphics){
                    Tile[] emptyTileset = new Tile[128];
                    tilesets[i] = emptyTileset;
                    for(int j=0;j<emptyTileset.length;j++){
                        emptyTileset[j] = createEmptyTile();
                    }
                }else{
                    try {
                        InputStream is = ClassLoader.class.getResourceAsStream("basemaptileset5.bin");
                        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                        int nRead;
                        byte[] data = new byte[424];
                        while ((nRead = is.read(data, 0, data.length)) != -1) {
                          buffer.write(data, 0, nRead);
                        }
                        byte[] bm5 = buffer.toByteArray();
                        tilesets[i] =  new StackGraphicsDecoder().decodeStackGraphics(bm5, palette);
                    } catch (IOException ex) {
                        Logger.getLogger(MapManager.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        System.out.println("com.sfc.sf2.map.MapManager.importDisassembly() - Disassembly imported.");
    }
    
    private Tile createEmptyTile(){
        Tile tile = new Tile();
        tile.setPalette(map.getTiles()[0].getPalette());
        tile.setPixels(new int[8][8]);
        return tile;
    }
    
    public void generateBlockset(){
        MapBlock[] blocks = map.getBlocks();
        MapBlock[] optimizedBlockset = null;
        List<MapBlock> blockList = new ArrayList();
        blockList.add(createVoidMapBlock());
        blockList.add(createClosedChestMapBlock());
        blockList.add(createOpenChestMapBlock());
        for(int i=0;i<blocks.length;i++){
            boolean found = false;
            for(int j=0;j<blockList.size();j++){
                if(blocks[i].equals(blockList.get(j))){
                    found = true;
                    break;
                }
            }
            if(!found){
                blocks[i].setIndex(blockList.size());
                blockList.add(blocks[i]);
            }
        }
        optimizedBlockset = new MapBlock[blockList.size()];
        blockList.toArray(optimizedBlockset);
        map.setOptimizedBlockset(optimizedBlockset);
        /* Re-assign layout block indexes */
        MapBlock[] layout = map.getLayout().getBlocks();
        MapBlock[] blockset = map.getOptimizedBlockset();
        for(int b=0;b<layout.length;b++){
            for(int i=0;i<blockset.length;i++){
                if(layout[b].equals(blockset[i])){
                    layout[b].setIndex(i);
                    break;
                }
            }
        }
    }
    
    private MapBlock createVoidMapBlock(){
        MapBlock block = new MapBlock();
        Tile[] tiles = new Tile[9];
        for(int i=0;i<tiles.length;i++){
            tiles[i] = map.getTilesets()[0][0];
        }
        block.setTiles(tiles);
        block.setIndex(0);
        return block;
    }
    
    private MapBlock createClosedChestMapBlock(){
        MapBlock block = new MapBlock();
        Tile[] tiles = new Tile[9];
        tiles[0] = map.getTilesets()[4][46];
        tiles[1] = map.getTilesets()[4][47];
        tiles[2] = Tile.hFlip(map.getTilesets()[4][46]);
        tiles[3] = map.getTilesets()[4][62];
        tiles[4] = map.getTilesets()[4][63];
        tiles[5] = Tile.hFlip(map.getTilesets()[4][62]);
        tiles[6] = map.getTilesets()[4][78];
        tiles[7] = map.getTilesets()[4][79];
        tiles[8] = Tile.hFlip(map.getTilesets()[4][78]);
        block.setTiles(tiles);
        block.setIndex(1);
        return block;
    }
    
    private MapBlock createOpenChestMapBlock(){
        MapBlock block = new MapBlock();
        Tile[] tiles = new Tile[9];
        tiles[0] = map.getTilesets()[4][46-2];
        tiles[1] = map.getTilesets()[4][47-2];
        tiles[2] = Tile.hFlip(map.getTilesets()[4][46-2]);
        tiles[3] = map.getTilesets()[4][62-2];
        tiles[4] = map.getTilesets()[4][63-2];
        tiles[5] = Tile.hFlip(map.getTilesets()[4][62-2]);
        tiles[6] = map.getTilesets()[4][78];
        tiles[7] = map.getTilesets()[4][79];
        tiles[8] = Tile.hFlip(map.getTilesets()[4][78]);
        block.setTiles(tiles);
        block.setIndex(2);
        return block;
    }
    
    public void generateTilesets(){
        Tile[][] tilesets = map.getTilesets();
        Tile emptyTile = createEmptyTile();
        for(int i=0;i<tilesets.length;i++){
            int availableTileSlots = 0;
            for(int j=0;j<tilesets[i].length;j++){
                if(tilesets[i][j].equals(emptyTile)){
                    availableTileSlots++;
                }
            }
            System.out.println("Available tile slots in tileset "+i+" : "+availableTileSlots);
        }
        Date d = new Date();
        System.out.println(d);
        Tile[] orphanTiles = null;
        List<Tile> orphanTileList = new ArrayList();
        MapBlock[] blockset = map.getOptimizedBlockset();
        for(int b=0;b<blockset.length;b++){
            MapBlock block = blockset[b];
            for(int t=0;t<block.getTiles().length;t++){
                int targetId = -1;
                for(int i=0;i<tilesets.length;i++){
                     if(targetId>=0){
                         break;
                     }
                     for(int j=0;j<tilesets[i].length;j++){
                         Tile testTile = block.getTiles()[t];
                         if(tilesets[i][j].equals(testTile)){
                             targetId = i*128+j;
                             System.out.println("Found blockset block "+b+" tile "+t+" in tileset "+i+" : "+j+" (no flips)");
                             break;
                         }
                         testTile = Tile.vFlip(block.getTiles()[t]);
                         if(tilesets[i][j].equals(testTile)){
                             targetId = i*128+j;
                             System.out.println("Found blockset block "+b+" tile "+t+" in tileset "+i+" : "+j+" (V flip)");
                             break;
                         }
                         testTile = Tile.hFlip(block.getTiles()[t]);
                         if(tilesets[i][j].equals(testTile)){
                             targetId = i*128+j;
                             System.out.println("Found blockset block "+b+" tile "+t+" in tileset "+i+" : "+j+" (H flip)");
                             break;
                         }
                         testTile = Tile.vFlip(testTile);
                         if(tilesets[i][j].equals(testTile)){
                             targetId = i*128+j;
                             System.out.println("Found blockset block "+b+" tile "+t+" in tileset "+i+" : "+j+" (V+H flips)");
                             break;
                         }
                     }
                 }
                 if(targetId<0){
                     System.out.println("  BLOCKSET BLOCK "+b+" TILE "+t+" NOT FOUND IN AVAILABLE TILESETS");
                 }
                if(targetId<0){
                    orphanTileList.add(block.getTiles()[t]);
                }
            }
        }
        Date d2 = new Date();
        List<Tile> optimTileList = new ArrayList();
        for(int i=0;i<orphanTileList.size();i++){
            boolean found = false;
            for(int j=0;j<optimTileList.size();j++){
                if(optimTileList.get(j).equals(orphanTileList.get(i))
                        || optimTileList.get(j).equals(Tile.hFlip(orphanTileList.get(i)))
                        || optimTileList.get(j).equals(Tile.vFlip(orphanTileList.get(i)))
                        || optimTileList.get(j).equals(Tile.hFlip(Tile.vFlip(orphanTileList.get(i))))){
                    found = true;
                    optimTileList.get(j).setOccurrences(optimTileList.get(j).getOccurrences()+1);
                    break;
                }
            }
            if(!found){
                orphanTileList.get(i).setOccurrences(0);
                optimTileList.add(orphanTileList.get(i));
            }
        }
        /*Collections.sort(optimTileList, new Comparator<Tile>() {
            @Override
            public int compare(Tile o1, Tile o2) {
                return o1.getOccurrences()-o2.getOccurrences();
            }
        });*/
        orphanTiles = new Tile[optimTileList.size()];
        optimTileList.toArray(orphanTiles);
        map.setOrphanTiles(orphanTiles);
        Tile[][] newTilesets = map.getTilesets().clone();
        map.setNewTilesets(newTilesets);
        Tile[][] emptyTilesets = new Tile[5][];
        Tile[][] nonEmptyTilesets = new Tile[5][];
        for(int i=0;i<map.getNewTilesets().length;i++){
            boolean empty = true;
            for(int j=0;j<map.getNewTilesets()[i].length;j++){
                if(!map.getNewTilesets()[i][j].equals(emptyTile)){
                    empty = false;
                    nonEmptyTilesets[i] = map.getNewTilesets()[i];
                    break;
                }
            }
            if(empty){
                emptyTilesets[i] = map.getNewTilesets()[i];
            }
        }
        
        for(int t=0;t<map.getOrphanTiles().length;t++){
            Tile tile = map.getOrphanTiles()[t];
            boolean assigned = false;
            for(int i=0;i<emptyTilesets.length;i++){
                if(emptyTilesets[i]!=null){
                    Tile[] tileset = emptyTilesets[i];
                    for(int j=0;j<tileset.length;j++){
                        if(!(i==0&&j==0)&&tileset[j].equals(emptyTile)){
                            tileset[j] = tile;
                            assigned = true;
                            break;
                        }
                    }
                    if(assigned){
                        break;
                    }else{
                        emptyTilesets[i] = null;
                    }
                }
            }
            if(!assigned){
                for(int i=0;i<nonEmptyTilesets.length;i++){
                    if(nonEmptyTilesets[nonEmptyTilesets.length-1-i]!=null){
                        Tile[] tileset = nonEmptyTilesets[nonEmptyTilesets.length-1-i];
                        for(int j=0;j<tileset.length;j++){
                            if(!(i==0&&j==0)&&tileset[j].equals(emptyTile)){
                                tileset[j] = tile;
                                assigned = true;
                                break;
                            }
                        }
                        if(assigned){
                            break;
                        }else{
                            nonEmptyTilesets[nonEmptyTilesets.length-1-i] = null;
                        }
                    }
                }
            }
        }
        /* Re-assign tilesets tile indexes */
        tilesets = map.getNewTilesets();
        for(int i=0;i<tilesets.length;i++){
            for(int j=0;j<tilesets[i].length;j++){
                tilesets[i][j].setId(i*128+j);
            }
        }
        /* Re-assign block tile indexes */
        blockset = map.getOptimizedBlockset();
        tilesets = map.getNewTilesets();
        System.out.println(blockset[3].getTiles()[0].getId());
        for(int b=0;b<blockset.length;b++){
            MapBlock block = blockset[b];
            for(int t=0;t<block.getTiles().length;t++){
                Tile tile = block.getTiles()[t];
                boolean found = false;
                for(int i=0;i<tilesets.length;i++){
                    Tile[] tileset = tilesets[i];
                    for(int j=0;j<tileset.length;j++){
                        if(tile.equals(tileset[j])){
                            tile.setId(i*128+j);
                            tile.sethFlip(false);
                            tile.setvFlip(false);
                            found = true;
                            break;
                        }
                        if(tile.equals(Tile.hFlip(tileset[j]))){
                            tile.setId(i*128+j);
                            tile.sethFlip(true);
                            tile.setvFlip(false);
                            found = true;
                            break;
                        }
                        if(tile.equals(Tile.vFlip(tileset[j]))){
                            tile.setId(i*128+j);
                            tile.sethFlip(false);
                            tile.setvFlip(true);
                            found = true;
                            break;
                        }
                        if(tile.equals(Tile.hFlip(Tile.vFlip(tileset[j])))){
                            tile.setId(i*128+j);
                            tile.sethFlip(true);
                            tile.setvFlip(true);
                            found = true;
                            break;
                        }
                    }
                    if(found){
                        break;
                    }
                }
            }
        }
        System.out.println(blockset[3].getTiles()[0].getId());
        System.out.println(d+" -> "+d2+" -> "+new Date());
        
    }
    
    public void exportDisassembly(String palettePath, String tileset1Path, String tileset2Path, String tileset3Path, String tileset4Path, String tileset5Path, String blocksPath, String layoutPath){
        System.out.println("com.sfc.sf2.map.MapManager.importDisassembly() - Exporting disassembly ...");
        PaletteManager pm = new PaletteManager();
        pm.exportDisassembly(palettePath, map.getLayout().getBlocks()[0].getTiles()[0].getPalette());
        GraphicsManager gm = new GraphicsManager();
        gm.setTiles(map.getNewTilesets()[0]);
        gm.exportDisassembly(tileset1Path, GraphicsManager.COMPRESSION_STACK);
        gm.setTiles(map.getNewTilesets()[1]);
        gm.exportDisassembly(tileset2Path, GraphicsManager.COMPRESSION_STACK);
        gm.setTiles(map.getNewTilesets()[2]);
        gm.exportDisassembly(tileset3Path, GraphicsManager.COMPRESSION_STACK);
        gm.setTiles(map.getNewTilesets()[3]);
        gm.exportDisassembly(tileset4Path, GraphicsManager.COMPRESSION_STACK);
        gm.setTiles(map.getNewTilesets()[4]);
        gm.exportDisassembly(tileset5Path, GraphicsManager.COMPRESSION_STACK);
        com.sfc.sf2.map.layout.io.DisassemblyManager mldm = new com.sfc.sf2.map.layout.io.DisassemblyManager();
        mldm.setBlockset(map.getOptimizedBlockset());
        Tile[] tileset = new Tile[128*5];
        System.arraycopy(map.getNewTilesets()[0], 0, tileset, 0*128, map.getNewTilesets()[0].length);
        System.arraycopy(map.getNewTilesets()[1], 0, tileset, 1*128, map.getNewTilesets()[1].length);
        System.arraycopy(map.getNewTilesets()[2], 0, tileset, 2*128, map.getNewTilesets()[2].length);
        System.arraycopy(map.getNewTilesets()[3], 0, tileset, 3*128, map.getNewTilesets()[3].length);
        System.arraycopy(map.getNewTilesets()[4], 0, tileset, 4*128, map.getNewTilesets()[4].length);
        mldm.setTileset(tileset);
        mldm.exportDisassembly(map.getOptimizedBlockset(), blocksPath, map.getLayout(), layoutPath);
        System.out.println("com.sfc.sf2.map.MapManager.importDisassembly() - Disassembly exported.");        
    }

    public void exportHPTiles(String hpTilesPath){
        System.out.println("com.sfc.sf2.maplayout.MapEditor.exportPng() - Exporting PNG ...");
        MetaManager.exportBlockHpTilesFile(map.getLayout().getBlocks(), 64, hpTilesPath);
        System.out.println("com.sfc.sf2.maplayout.MapEditor.exportPng() - PNG exported.");       
    }
    
    public void exportPng(String filepath){
        System.out.println("com.sfc.sf2.maplayout.MapEditor.exportPng() - Exporting PNG ...");
        com.sfc.sf2.map.block.io.RawImageManager.exportRawImage(map.getLayout().getBlocks(), filepath, 64, com.sfc.sf2.graphics.io.RawImageManager.FILE_FORMAT_PNG);
        System.out.println("com.sfc.sf2.maplayout.MapEditor.exportPng() - PNG exported.");       
    }

    public Map getMap() {
        return map;
    }

    public void setMap(Map map) {
        this.map = map;
    }
}
