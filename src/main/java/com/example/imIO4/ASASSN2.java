package com.example.imIO4;/*
 * imIO5.1
 * Created by Aditya Gholba on 26/4/17.
 */

import com.datatorrent.api.DefaultOutputPort;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import static org.opencv.imgcodecs.Imgcodecs.imread;

public class ASASSN2 extends ToolKit
{
    public String getSoPath()
    {
        return soPath;
    }

    public void setSoPath(String soPath)
    {
        this.soPath = soPath;
    }
    protected static  String soPath ="/home/aditya/opencv-3.2.0/build/lib/libopencv_java320.so";
    protected static final Logger LOG = LoggerFactory.getLogger(ASASSN.class);
    protected  int matches=0;
    protected transient int notMatch=0;
    protected  int dense=0;
    //static {System.load(soPath);}
    protected ArrayList<Mat> referenceList = new ArrayList<>();
    protected  ArrayList<Mat> templateList = new ArrayList<>();
    protected String refPath;
    public final transient DefaultOutputPort<Data> outputScore = new DefaultOutputPort<>();
    public String getRefPath()
    {
        return refPath;
    }

    public void setRefPath(String refPath)
    {
        this.refPath = refPath;
    }


    protected  BufferedImage convertToRGB(BufferedImage image)
    {
        BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = newImage.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return newImage;
    }

    protected  void compute(Data data)
    {
        System.load(soPath);
        byte[] bytesImage = data.bytesImage;
        Mat source = readMat(bytesImage);
        BufferedImage  bufferedImage= null;
        dense=0;
        float blackPixels = 0;
        float whitePixels = 0;
        float whitePixelsInGrid=0;
        float blackPixelsInGrid=0;
        referenceList.clear();
        ArrayList<String> whiteRange = new ArrayList<>();
        InputStream src = new ByteArrayInputStream(bytesImage);
        try
        {
            bufferedImage = ImageIO.read(src);
        }
        catch (Exception e){System.out.print(e.getMessage());}
        BufferedImage bufferedImage1 = convertToRGB(bufferedImage);
        for (int y = 0; y < 2048; y = y + 64)
        {
            for (int x = 0; x < 2048; x = x + 64)
            {
                for (int i = x; i < x + 64; i++)
                {
                    for (int j = y; j < y + 64; j++)
                    {
                        Color c = new Color(bufferedImage1.getRGB(i, j));
                        String hex = "#" + Integer.toHexString(c.getRGB()).substring(2);
                        //System.out.println("Pixel at "+i+","+j+" "+hex);
                        if(c.getRed()<50)
                        {
                            blackPixels++;
                            blackPixelsInGrid++;
                        } else
                        {
                            whitePixels++;
                            whitePixelsInGrid++;
                            if (!whiteRange.contains(hex))
                            {
                                whiteRange.add(hex);
                            }
                        }

                    }
                }
                int totalPixelsInGrid = (int)whitePixelsInGrid + (int)blackPixelsInGrid;
                float whitePixelDensity = whitePixelsInGrid/totalPixelsInGrid;
                if (((whitePixelDensity*100)>89 && (whitePixelDensity*100)<99) || ((whitePixelDensity*100)>60 && (whitePixelDensity*100)<70))
                {//System.out.println(x+","+y);
                    for (int p1=x;p1<x+64;p1++)
                    {
                        for (int p2=y;p2<y+64;p2++)
                        {
                            Color pink = new Color	(255,104,150);
                            int rgb = pink.getRGB();
                            bufferedImage1.setRGB(p1,p2,rgb);
                        }
                    }
                    dense++;

                    Mat sub = source.submat(y, y + 64, x, x + 64);
                    //writeMat(sub,destination);
                    templateList.add(sub);

                    //System.out.println(x+" "+y);
                    //referenceList.add(sub);

                }
                whitePixelsInGrid=0;
                blackPixelsInGrid=0;

            }
        }

        System.out.println("Black pixels:" + blackPixels);
        System.out.println("White pixels:" + whitePixels);/*
        System.out.println("White area:" + (whitePixels * 100 / (whitePixels + blackPixels)));
        System.out.println("Black area:" + (blackPixels * 100 / (whitePixels + blackPixels)));
        System.out.println("Total:" + (whitePixels + blackPixels) + " should be :" + (2048 * 2048));
        System.out.println("White hex range size:" + whiteRange.size());
        System.out.println("White hex range:" + whiteRange);
        */
        //System.out.println("Dense blocks:"+dense);
        LOG.info("Dense blocks:"+dense+" temList:"+templateList.size());
        //LOG.info("matchCalled");
        //firstConcat(data);
        //ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        //try{ImageIO.write(bufferedImage1,"png",byteArrayOutputStream);}catch(Exception e){}
        //data.bytesImage=byteArrayOutputStream.toByteArray();
        if(dense<512)
        {
            match(data);
        }

        if(dense>512)
        {
            output.emit(data);
        }

    }

    protected void match(Data data)
    {
        System.load(soPath);
        int i=0;
        File[] files = new File(refPath).listFiles();
        double[] mval = new double[templateList.size()*files.length];
        int matches2=0;
        for(File file: files)
        {
            Mat source = imread(file.getAbsoluteFile().toString());
            if(matches<dense)
            {
                for (Mat template:templateList)
                {
                    String refImagePath = file.getAbsolutePath();
                    Mat result = new Mat();
                    double threshold=0.44;
                    if(matches>5)
                    {
                        threshold=0.43;
                    }
                    else if(matches>10)
                    {
                        threshold=0.42;
                    }
                    else if(matches>15)
                    {
                        threshold=0.41;
                    }
                    else if(matches>20)
                    {
                        threshold=0.40;
                    }
                    //Mat template = templateList.get(i);
                    //System.out.println(templateList.indexOf(template));
                    if(matches<=25)
                    {
                        Imgproc.matchTemplate(source, template, result, Imgproc.TM_CCOEFF_NORMED);
                        //Core.normalize(result, result, 0, 1, Core.NORM_MINMAX, -1, new Mat());
                        Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
                        //System.out.println(mmr.maxVal );

                        {
                            mval[i] = mmr.maxVal;
                        }
                        i++;
                        if (mmr.maxVal >= threshold)
                        {
                            matches++;
                            //System.out.println(mmr.maxVal );
                            //System.out.println(refImagePath);
                            //System.out.println(templateList.indexOf(template));
                        }
                        if (mmr.maxVal < threshold)
                        {
                            notMatch++;
                            if (refImagePath.contains(data.fileName))
                            {
                                // System.out.println("Should Match but did NOT! "+refImagePath);
                            }
                        }
                    /*
                    if (mmr.maxVal >= 0.95 && mmr.maxVal < 0.97)
                    {
                        referenceList.add(template);
                    }*/
                        // Point matchLoc;
                        //matchLoc = mmr.maxLoc;
                        //rectangle(source, matchLoc, new Point(matchLoc.x + template.cols(), matchLoc.y + template.rows()), new Scalar(0, 255, 0));
                    }
                    //writeMat(source,"/home/aditya/Desktop/xyzabc"+index+".jpg");
                }
            }

            //System.out.println(fileName);
            //System.out.println("Matches:"+matches);
            //System.out.println("Not matches:"+notMatch);

        }
        templateList.clear();
        Arrays.sort(mval);
        if (matches >= 20)
        {
            // data2ArrayList.remove(partData);

            String mvalsToString="";
            if (mval.length>10)
            {
                for (int l = mval.length - 1; l > mval.length-11; l--)
                {
                    mvalsToString = mvalsToString + mval[l]+" ";
                }

            }
            else
            {
                for (int l = mval.length - 1; l > 0; l--)
                {
                    mvalsToString = mvalsToString + mval[l]+" ";
                }
            }
            LOG.info("Matches C "+data.fileName+" matches "+matches+" dense "+dense+" mvals "+mvalsToString);
            outputScore.emit(data);
        }
        else
        {
            //data2ArrayList.remove(partData);
            LOG.info("Matches O "+data.fileName+" matches "+matches+" dense "+dense);
            output.emit(data);
        }
        matches=0;


    }


    protected  void firstConcat(Data data)
    {
        Mat r2 = new Mat();
        System.out.println("ref list size "+referenceList.size());
        if(referenceList.size()<1024)
        {
            Core.hconcat(referenceList,r2);
        }
        else
        {
            ArrayList <Mat> subReferenceList = new ArrayList <> (referenceList.subList(0,1023));
            Core.hconcat(subReferenceList,r2);
        }
        writeMat(r2);
    }

    @Override
    void processTuple(Data data)
    {
        compute(data);
    }


}
