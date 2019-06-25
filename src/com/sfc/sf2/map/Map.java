/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sfc.sf2.map;

import com.sfc.sf2.graphics.Tile;
import com.sfc.sf2.map.block.MapBlock;
import com.sfc.sf2.map.layout.MapLayout;

/**
 *
 * @author wiz
 */
public class Map {
    private MapBlock[] blocks;
    private MapBlock[] optimizedBlockset;
    private MapLayout layout;
    private Tile[] tiles;
    private Tile[][] tilesets;
    private Tile[][] newTilesets;
    private Tile[] orphanTiles;
    
    public MapBlock[] getBlocks() {
        return blocks;
    }

    public void setBlocks(MapBlock[] blocks) {
        this.blocks = blocks;
    }

    public MapLayout getLayout() {
        return layout;
    }

    public void setLayout(MapLayout layout) {
        this.layout = layout;
    }

    public Tile[] getTiles() {
        return tiles;
    }

    public void setTiles(Tile[] tiles) {
        this.tiles = tiles;
    }

    public Tile[][] getTilesets() {
        return tilesets;
    }

    public void setTilesets(Tile[][] tilesets) {
        this.tilesets = tilesets;
    }

    public Tile[] getOrphanTiles() {
        return orphanTiles;
    }

    public void setOrphanTiles(Tile[] orphanTiles) {
        this.orphanTiles = orphanTiles;
    }

    public MapBlock[] getOptimizedBlockset() {
        return optimizedBlockset;
    }

    public void setOptimizedBlockset(MapBlock[] optimizedBlockset) {
        this.optimizedBlockset = optimizedBlockset;
    }

    public Tile[][] getNewTilesets() {
        return newTilesets;
    }

    public void setNewTilesets(Tile[][] newTilesets) {
        this.newTilesets = newTilesets;
    }
    
    
    
}
