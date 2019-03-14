import java.util.*;
import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.gui.Roi;
import ij.gui.Overlay;
import ij.plugin.frame.RoiManager;
import java.awt.*;

/**
	This plugin determines simple region matching for areas in a left image to a right one.
    It takes as input two images in an ImageStack (a left and right one - where the right one is rectififed).
    ROI(s) must be selected from the left image and added to the ROI Manager before calling this plugin. Otherwise, a NullPointerException will occur.
    The matching is implemented using the SSD (sum of squared differences algorithm).
*/

public class Matching implements PlugInFilter {

	public int setup(String arg, ImagePlus imp) {
		return DOES_ALL+SUPPORTS_MASKING;
	}

	public void run(ImageProcessor ip) {
		
        //Get access to the current ImagePlus instance
        ImagePlus imp = IJ.getImage();
        
        //retrieve the current stack
        ImageStack stack = imp.getStack();
        
        //Get the left and right images from the stack respectively
        ImageProcessor leftImage = stack.getProcessor(1);
        ImageProcessor rightImage = stack.getProcessor(2);
       
        //Get the current instance of the ROI Manager
        RoiManager roiMng = RoiManager.getInstance();
        
        //Get all selected ROIs in the current instance
        Roi[] rois = roiMng.getRoisAsArray();
        
        //Find matches
        findMatches(leftImage, rightImage, rois);
	}

    /*  Description: This function finds the best match between points in the left image and right, and then displays them.
        Input: An instance of the left and right images and an array of all the selected ROIs from the left image
    */
    public void findMatches(ImageProcessor leftImage, ImageProcessor rightImage, Roi[] rois) {

        //This imageplus instance will be recycled in the following calculations
        ImagePlus rightImageRoisImp = new ImagePlus();
        
        //An ImagePlus object is required from the right image's imageprocessor
        ImagePlus rightImageImp = new ImagePlus("rightImageImp", rightImage);
        
        //This Overlay object will be used for displaying the final matches
        Overlay overlay = new Overlay();
        
        //For every selected ROI
        for (int r=0; r<rois.length; r++) {
            
            //Get the current image and set this roi on it
            ImagePlus imp = IJ.getImage();
            imp.setRoi(rois[r]);
            
            //Duplicate the part of the image defined by the roi and get its processor object
            ImagePlus leftImageRoiImp = imp.duplicate();
            ImageProcessor leftSelectedRoiProcessor = leftImageRoiImp.getProcessor();

            //Get a 2d array of pixels for this roi
            float[][] leftImageRoiPixels = leftSelectedRoiProcessor.getFloatArray();
            
            //Get the y coordinate base value, height, and width of this roi
            int leftImageYBase = (int) rois[r].getYBase();
            int leftImageRoiHeight = (int) rois[r].getFloatHeight();
            int leftImageRoiWidth = (int) rois[r].getFloatWidth();
            
            //Obtain the width of the right image
            int rightImageWidth = rightImage.getWidth();
            
            //Initialize variables to be used for the minimum SSD value and the final best match ROI
            int minSsd = 0;
            Roi match = new Roi(0,0,0,0);
            
            int[] ssds = new int[rightImageWidth];

            //These images are rectified and thus we can match along epipolar (horizontal) lines
            //Loop through the whole width of the right image for the row that the selected ROI (from the left image) was one
            for (int s=0; s<rightImageWidth; s++) {
                
                //Construct ROIs from the right image that are along the same horizontal line as the selected ROI from the left image
                Roi roi = new Roi(s, leftImageYBase, leftImageRoiWidth, leftImageRoiHeight, rightImageImp);
                
                //Then obtain the pixel array of this roi
                imp.setRoi(roi);
                rightImageRoisImp = imp.duplicate();
                ImageProcessor rightImageRoiIp = rightImageRoisImp.getProcessor();
                float[][] rightImageRoiPixels = rightImageRoiIp.getFloatArray();
                
                //reset this sum to 0
                int ssd = 0;
                
                //Ensure left and right regions are of same size so we don't get an out of bounds exception
                if ((leftImageRoiPixels.length == rightImageRoiPixels.length) && (leftImageRoiPixels[0].length == rightImageRoiPixels[0].length)) {

                    //compute ssd (cumulative sum of the square of differences between all the left and right values)
                    for (int i=0; i<leftImageRoiPixels.length; i++) {
                        
                        //for every pixel in the selected roi of the left image and the new computed roi for the right image
                        for (int j=0; j<leftImageRoiPixels[i].length; j++) {
                            ssd += (leftImageRoiPixels[i][j] - rightImageRoiPixels[i][j]) * (leftImageRoiPixels[i][j] - rightImageRoiPixels[i][j]);
                        }
                    }
                    
                }
                
                //A match is the lowest SSD value
                //Keep updating the match
                if ((minSsd>ssd || s==1) && ssd != 0) {
                    minSsd = ssd;
                    match = roi;
                }
                
            }
            
            //Set the color of the matches' outlines to green to differentiate them between the selections (yellow)
            match.setStrokeColor(java.awt.Color.green);
            
            //Add the matches to the final overlay
            overlay.add(match);
            
        }
        
        //Output the overlay with all the new ROI matches onto the image
        ImagePlus output = IJ.getImage();
        output.setOverlay(overlay);
    
    }
    
}
