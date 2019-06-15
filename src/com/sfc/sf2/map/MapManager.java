/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sfc.sf2.map;

import com.sfc.sf2.graphics.GraphicsManager;
import com.sfc.sf2.map.block.MapBlock;
import com.sfc.sf2.map.gui.MapPanel;
import com.sfc.sf2.map.io.DisassemblyManager;
import com.sfc.sf2.map.io.PngManager;
import com.sfc.sf2.map.layout.MapLayout;
import com.sfc.sf2.map.layout.MapLayoutManager;
import com.sfc.sf2.map.layout.layout.MapLayoutLayout;

/**
 *
 * @author wiz
 */
public class MapManager {
       
    private MapLayoutManager mapLayoutManager = new MapLayoutManager();
    private Map map;
    
    public void importDisassembly(String imagePath, String hptilesPath){
        System.out.println("com.sfc.sf2.map.MapManager.importDisassembly() - Importing disassembly ...");
        map = PngManager.importPngMap(imagePath,hptilesPath);
        System.out.println("com.sfc.sf2.map.MapManager.importDisassembly() - Disassembly imported.");
    }
    
    public void exportDisassembly(String blocksPath, String layoutPath){
        System.out.println("com.sfc.sf2.map.MapManager.importDisassembly() - Exporting disassembly ...");
        mapLayoutManager.exportDisassembly(blocksPath, layoutPath);
        System.out.println("com.sfc.sf2.map.MapManager.importDisassembly() - Disassembly exported.");        
    }    
    

    public void exportHPTiles(Map map, String hpTilesPath){
        System.out.println("com.sfc.sf2.maplayout.MapEditor.exportPng() - Exporting PNG ...");
        PngManager.exportHPTiles(map, hpTilesPath);
        System.out.println("com.sfc.sf2.maplayout.MapEditor.exportPng() - PNG exported.");       
    }    
    
    public void exportPng(MapPanel mapPanel, String filepath){
        System.out.println("com.sfc.sf2.maplayout.MapEditor.exportPng() - Exporting PNG ...");
        PngManager.exportPng(mapPanel, filepath);
        System.out.println("com.sfc.sf2.maplayout.MapEditor.exportPng() - PNG exported.");       
    }

    public Map getMap() {
        return map;
    }

    public void setMap(Map map) {
        this.map = map;
    }
    
    
    
}
