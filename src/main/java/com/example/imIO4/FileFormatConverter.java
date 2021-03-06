package com.example.imIO4;/*
 * imIO5.1
 * Created by Aditya Gholba on 4/4/17.
 * Jpeg to PNG and back
 * fits to jpeg/png.
 *
 */
import com.datatorrent.api.DefaultOutputPort;
import ij.IJ;

import ij.ImagePlus;
import ij.io.Opener;
import ij.process.ImageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.imageio.ImageIO;
import javax.validation.constraints.NotNull;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class FileFormatConverter extends ToolKit
{
    private static final Logger LOG = LoggerFactory.getLogger(FileFormatConverter.class);

    @NotNull
    protected String toFileType;
    public String getToFileType()
    {
        return toFileType;
    }
    public void setToFileType(String toFileType)
    {
        this.toFileType = toFileType;
    }
   // public final transient DefaultOutputPort<Data> output1 = new DefaultOutputPort<>();
   // public final transient DefaultOutputPort<Data> output2 = new DefaultOutputPort<>();

    protected void converter(Data data)
    {
        String fromFileType = ToolKit.fileType;
        if(!fromFileType.contains("fit"))
        {
            LOG.info("toFileType: " + toFileType + " fromFileType " + fromFileType);
            byte[] bytesImage = data.bytesImage;
            try
            {
                bufferedImage = ImageIO.read(new ByteArrayInputStream(bytesImage));
            } catch (Exception e)
            {
                LOG.info("ERR " + e.getMessage());
            }
            ImagePlus imgPlus = new ImagePlus("source", bufferedImage);
            try
            {
                IJ.saveAs(imgPlus, toFileType, "");
            } catch (HeadlessException h)
            {
                LOG.info(h.getMessage() + "/n");
            }
            ImageProcessor imageProcessor = imgPlus.getProcessor();
            BufferedImage bufferedImage1;
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try
            {
                bufferedImage1 = (BufferedImage) imageProcessor.createImage();
                ImageIO.write(bufferedImage1, toFileType, byteArrayOutputStream);
            } catch (Exception e)
            {
                LOG.info("ERR " + e.getMessage());
            }
            data.bytesImage = byteArrayOutputStream.toByteArray();
            data.fileName = data.fileName.replace(fromFileType, toFileType);
            LOG.info("fileName " + data.fileName);
            try
            {
                byteArrayOutputStream.flush();
                byteArrayOutputStream.reset();
                byteArrayOutputStream.close();
                imageProcessor.reset();
            }catch (Exception e){}
            output.emit(data);
        }
        else
        {
            LOG.info("itsFits");
            ImagePlus imgPlus = new Opener().deserialize(data.bytesImage);
            try
            {
                IJ.saveAs(imgPlus, toFileType, "");
            }
            catch (Exception e)
            {
                LOG.info("ERR "+e.getMessage());
            }
            ImageProcessor imageProcessor = imgPlus.getProcessor();
            BufferedImage bufferedImage1;
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try
            {
                bufferedImage1 = (BufferedImage) imageProcessor.createImage();
                ImageIO.write(bufferedImage1, toFileType, byteArrayOutputStream);
            }
            catch (Exception e)
            {
                LOG.info("ERR " + e.getMessage());
            }
            data.bytesImage = byteArrayOutputStream.toByteArray();
            data.fileName = data.fileName.replace(fromFileType, toFileType);
            LOG.info("fileName " + data.fileName);
            try
            {
                byteArrayOutputStream.flush();
                byteArrayOutputStream.reset();
                byteArrayOutputStream.close();
            }catch (Exception e){}
            output.emit(data);
        }
    }

    @Override
    void processTuple(Data data)
    {
        converter(data);
    }
}
