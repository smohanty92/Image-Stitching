import java.util.*;
import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.gui.Roi;
import java.awt.*;

/**
    This plugin creates a panoramic mosiac of pictures by stitching them together
*/

public class Stitch implements PlugInFilter {

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
       
        //Stitch images
        stitch(leftImage, rightImage);
        
	}

    /*  Description: This function stitches together two images by computing the horizontal displacement between two arbitrary points (a matching pair) and stitching them together
        Input: An instance of the left and right images
    */
    public void stitch(ImageProcessor leftImage, ImageProcessor rightImage) {

        ImagePlus imp = IJ.getImage();
        
        /*
         We can fix bounds for the amount of shift because we can generally know the angle of rotation between views and have an idea of the shifts.
         
         Only horizontal displacement is being calculated because it can be assumed that the series of pictures are panned horizontally and fixed vertically.
         
         Therefore, an assumption is being made that a match may occur between the right horizontal half of the left picture and the left horizontal half of the right picture.
         
         Let's create an arbitrary ROI and find its match
         Let's choose a point (feature) in the center of the second half of the left picture and create a 3x3 roi around it
        */
        
        //Left image dimensions needed to create arbitrary ROI
        int leftImageHeight = leftImage.getHeight();
        int leftImageWidth = leftImage.getWidth();
        
        //Arbitrary ROI's starting coordinates to get it in the center of the second half of the left image
        int startX = (int) Math.floor(0.75 * leftImageWidth) - 1;
        int startY = (int) Math.floor(0.5 * leftImageHeight) - 1;
        
        //Create the actual arbitrary ROI("feature") from the left image and obtain its pixel information
        ImagePlus leftImageImp = new ImagePlus("leftImageImp", leftImage);
        Roi feature = new Roi(startX, startY, 3, 3, leftImageImp);
        imp.setRoi(feature);
        ImagePlus featureImp = imp.duplicate();
        ImageProcessor featureProcesor = featureImp.getProcessor();
        float[][] featurePixels = featureProcesor.getFloatArray();
        
        //Compare against first half of the 2nd pic to find match all along the same line as the feature
        int rightImageHeight = rightImage.getHeight();
        int rightImageWidth = rightImage.getWidth();
        int rightHalfX = (int) Math.ceil(rightImageWidth/2);
        int rightHalfY = (int) Math.ceil(rightImageHeight/2);
        
        //Will be used to detect best match
        ImagePlus rightImageImp = new ImagePlus("RightImageImp", rightImage);
        int minSsd = 0;
        int xValueOfMatch = 0;

        //Only check the first half of the second image
        for (int s=0; s<rightHalfX; s++) {
            
            //Construct ROIs from the right image that are along the same horizontal line as the selected feature from the left image
            Roi rightRoi = new Roi(s, startY, 3, 3, rightImageImp);
            imp.setRoi(rightRoi);
            ImagePlus rightRoiImp = imp.duplicate();
            ImageProcessor rightRoiProcessor = rightRoiImp.getProcessor();
            float[][] rightRoiPixels = rightRoiProcessor.getFloatArray();
            
            //reset this sum to 0
            int ssd = 0;
            
            //Ensure left and right regions are of same size so we don't get an out of bounds exception
            if ((featurePixels.length == rightRoiPixels.length) && (featurePixels[0].length == rightRoiPixels[0].length)) {
                
                for (int i=0; i<featurePixels.length; i++) {
                    
                    for (int j=0; j<featurePixels[i].length; j++) {
                        
                        //Compute ssd
                        ssd += (featurePixels[i][j] - rightRoiPixels[i][j]) * (featurePixels[i][j] - rightRoiPixels[i][j]);
                    }
                    
                }
           
            }
            
            //A match is the lowest SSD value
            //Keep updating the match and get the x coordinate of it
            if ((minSsd>ssd || s==1) && ssd != 0) {
                minSsd = ssd;
                xValueOfMatch = s;
            }
            
        }

        //Horizontal displacement is the match on the right image's X location subtracted from the feature's on the left
        int horizontalDisplacement = startX - xValueOfMatch;
        
        float[][] rightImagePixels = rightImage.getFloatArray();
        float[][] leftImagePixels = leftImage.getFloatArray();
        
        int shiftedRightImageColumnLength = rightImageWidth - horizontalDisplacement;
        
        //Perform the stitching into a new array that has a width of the original image + the length of the shifted image
        int mosiacColumnLength = leftImagePixels.length + shiftedRightImageColumnLength;
        float[][] mosiacPixels = new float[mosiacColumnLength][rightImageHeight];
        
        //Populate mosiac array
        for (int i=0; i<mosiacPixels.length; i++) {
            
            for (int j=0; j<mosiacPixels[i].length; j++) {
                
                float output = 0.0f;
                
                //Populate from the left image until we've exceeded its width and are on the new shifted image
                if (i<leftImagePixels.length) {
                    output = leftImagePixels[i][j];
                } else {
                    output = rightImagePixels[i+horizontalDisplacement-leftImagePixels.length][j];
                }
                
                mosiacPixels[i][j] = output;
                
            }
            
        }
    
        //Create and display the new Mosiac picture
        FloatProcessor mosiacProcessor = new FloatProcessor(mosiacPixels);
        ImagePlus mosiacOutput = new ImagePlus("mosiacOutput", mosiacProcessor);
        mosiacOutput.show();
        
    }

}
