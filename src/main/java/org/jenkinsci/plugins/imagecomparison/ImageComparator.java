/*
 * Copyright (c) 2015 Dimitri Tenenbaum All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package org.jenkinsci.plugins.imagecomparison;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import javax.imageio.ImageIO;

import org.kohsuke.stapler.DataBoundConstructor;


public class ImageComparator extends Recorder implements Serializable {

	private String image1;
	private String image2;
	private float difThreshold;
	
	public String getImage1() {
		return image1;
	}

	public void setImage1(String image1) {
		this.image1 = image1;
	}

	public String getImage2() {
		return image2;
	}

	public void setImage2(String image2) {
		this.image2 = image2;
	}

	public float getDifThreshold() {
		return difThreshold;
	}

	public void setDifThreshold(int difThreshold) {
		this.difThreshold = difThreshold;
	}


	@DataBoundConstructor
	public ImageComparator(String image1, String image2, float difThreshold) 
	{
		this.image1 = image1;
		this.image2 = image2;
		this.difThreshold = difThreshold;
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
		File f1 = new File(image1);
		File f2 = new File(image2);
		if(!f1.isFile() || !f1.canRead())
		{
			listener.error("Can't access " + f1.getAbsolutePath() + "!\n");
			return false;
		}
		if(!f2.isFile() || !f2.canRead())
		{
			listener.error("Can't access " + f2.getAbsolutePath() + "!\n");
			return false;
		}
		
		float dif = compareImages(f1, f2);
		
		if (dif < difThreshold)
		{
			listener.error("Image similarity " + dif + "% is under configured required similarity " + difThreshold + "% !\n");
			return false;
		}
		listener.getLogger().print("Image similarity " + dif + "% is over configured required similarity " + difThreshold + "% ==> OK!\n");
		return true;
	}


	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
		public String getDisplayName() {
			return "Compare images (png or jpeg)";
		}

		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}
	}

	public static float compareImages(File f1, File f2) throws IOException
	{
		float ret = -1;
		int q = 0;	
		BufferedImage image = ImageIO.read(f1);
		int widthOfImage1 = image.getWidth();
		int heightOfImage1 = image.getHeight();
		int[][] bufOfImage1 =  new int[widthOfImage1][heightOfImage1]; 

		BufferedImage images = ImageIO.read(f2);
		int widthOfImage2 = images.getWidth();
		int heightOfImage2 = images.getHeight();
		int[][] bufOfImage2 =  new int[widthOfImage2][heightOfImage2]; 

		int smallestWidth = Math.min(widthOfImage1, widthOfImage2) ;
		int smallestHeight= Math.min(heightOfImage1, heightOfImage2) ;
		int p = 0;
		//calculation of pixel similarity 
		for(int a=0;a<smallestWidth;a++)
		{
			for(int b=0;b<smallestHeight;b++)
			{
				bufOfImage2[a][b]=images.getRGB(a,b);
				bufOfImage1[a][b]=image.getRGB(a,b);
				if(bufOfImage1[a][b]==bufOfImage2[a][b]) 
				{
					p = p + 1;
				}
				else
					q = q + 1;
			}
		}
		//percentage calculation
		ret = (100*p)/(smallestWidth*smallestHeight);
		return ret;
	} 
}

