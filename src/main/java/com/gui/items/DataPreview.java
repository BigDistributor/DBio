package main.java.com.gui.items;

import java.util.ArrayList;

import main.java.com.blockmanager.GraphicBlocksManager;
import main.java.com.controllers.items.JDataFile;

public class DataPreview extends Object{
	private final JDataFile file;
	private long[] blocksSizes;
	private long overlap;
	private ArrayList<BlockPreview> blocksPreview;
	private int previewPreferedHeight ;

	private DataPreview(Builder builder) {
		this.file = builder.file;
		this.blocksPreview = builder.blocksPreview;
		this.overlap = builder.overlap;
		this.blocksSizes = builder.blocksSizes;
	}
	
	
	public void setBlocksPreview(ArrayList<BlockPreview> blocksPreview) {
		this.blocksPreview = blocksPreview;
	}
	
	 
	public JDataFile getFile() {
		return file;
	}
	
	public ArrayList<BlockPreview> getBlocksPreview() {
		return blocksPreview;
	}
	
	public long[] getBlocksSizes() {
		return blocksSizes;
	}
	public void setBlockSize(int position,long blockSize) {
		this.blocksSizes[position] = blockSize;
	}
	public void setOverlap(long overlap) {
		this.overlap = overlap;
	}
	
	public long getOverlap() {
		return overlap;
	}


	public int getPreviewPreferedHeight() {
		return previewPreferedHeight;
	}


	public void generateBlocks() {
		this.blocksPreview = GraphicBlocksManager.generateBlocks(file.getDimensions(), blocksSizes, overlap);
	}
	
	
	
	public void setBlockSizes(long[] blocksSizes) {
		this.blocksSizes = blocksSizes;
	}
	

	public static class Builder {
		private JDataFile file;
		private ArrayList<BlockPreview> blocksPreview;
		private final long DEFAULT_BLOCK_SIZE = 200;
		private long[] blocksSizes;
		private long overlap = 10 ;
		private int previewPreferedHeight ;
		
		public Builder BlocksPreview() {
			this.blocksPreview = blocksPreview;
			return this;
		}

		public Builder file(JDataFile file) {
			this.file = file;
			return this;
		}
	
		
		
		public DataPreview build() {
//			previewPreferedHeight = (int) (numberBlocks[1]*blocksSize[1]);
			this.blocksSizes = new long[] {DEFAULT_BLOCK_SIZE,DEFAULT_BLOCK_SIZE};
			return new DataPreview(this);
		}
	}
}
