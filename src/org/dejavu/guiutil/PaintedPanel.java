package org.dejavu.guiutil;

import org.dejavu.util.DjvLogMsg.Category;
import org.dejavu.util.DjvSystem;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.ImageObserver;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.JPanel;


/**
 * Derived from JPanel and have a background image.
 * The background image is loaded from a file whose URL is specified in the
 * constructor.
 */
public class PaintedPanel extends JPanel implements ImageObserver
{
	private static final long serialVersionUID = 1L;

	private Image m_Image;
	private Image m_cornerIcon;
	
	private int m_imgHeight = -1;
	private int m_imgWidth = -1;
	private int m_iconHeight = -1;
	private int m_iconWidth = -1;
	private boolean m_tiled = false;
	
	public PaintedPanel(java.lang.String imageName, java.lang.String cornerIcon, boolean tiled)
	{
		URL imgUrl = getClass().getClassLoader().getResource(imageName);
		if(null != imgUrl)
		{
			try
			{
				m_Image = ImageIO.read(imgUrl);
				if(null != m_Image)
				{
					int height = m_Image.getHeight(this);
					int width = m_Image.getWidth(this);
					if(-1 != height)
						m_imgHeight = height;
					if(-1 != width)
						m_imgWidth = width;
				}
				else
				{
					DjvSystem.logError(Category.DESIGN, "Failed to get image for " + imgUrl);
				}
			}
			catch(IOException ex)
			{
				DjvSystem.logError(Category.DESIGN, "Encountered " + ex
					+ " while loading the image " + imageName);
			}
		}
		else
		{
			DjvSystem.logError(Category.DESIGN, "Failed to get URL for " + imageName);
		}
		
		if(null != cornerIcon)
		{
			URL iconUrl = getClass().getClassLoader().getResource(cornerIcon);
			if(null != iconUrl)
			{
				try
				{
					m_cornerIcon = ImageIO.read(iconUrl);
					if(null != m_cornerIcon)
					{
						int height = m_cornerIcon.getHeight(this);
						int width = m_cornerIcon.getWidth(this);
						if(-1 != height)
							m_iconHeight = height;
						if(-1 != width)
							m_iconWidth = width;
					}
					else
					{
						DjvSystem.logError(Category.DESIGN, "Failed to get image for " + iconUrl);
					}
				}
				catch(IOException ex)
				{
					DjvSystem.logError(Category.DESIGN, "Encountered " + ex
						+ " while attempting to load icon " + cornerIcon);
				}
			}
			else
			{
				DjvSystem.logError(Category.DESIGN, "Failed to get URL for " + cornerIcon);
			}
		}
		m_tiled = tiled;
	}
	
	@Override
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D)g;
		if((null != m_Image)&&(-1 != m_imgWidth)&&(-1 != m_imgHeight))
		{
			if(m_tiled)
			{
				int x = 0;
				int y = 0;
				int safetyBreak = 1000;
				while(true)
				{
					if(--safetyBreak < 0)
						break;
					
					g2d.drawImage(m_Image, x, y, null);
					x += m_imgWidth;
					if(x > getWidth())
					{
						x = 0;

						y += m_imgHeight;
						if( y > getHeight())
						{
							break;
						}
					}
				}
			}
			else
			{
				AffineTransform savedAt = g2d.getTransform();
				double sx = (double)getWidth() / (double)m_imgWidth;
				double sy = (double)getHeight() / (double)m_imgHeight;
				AffineTransform at = new AffineTransform();
				at.setToScale(sx, sy);
				g2d.setTransform(at);
				g2d.drawImage(m_Image, 0, 0, null);
				g2d.setTransform(savedAt);
			}
		}
		
		if((null != m_cornerIcon)&&(m_iconWidth != -1)&&(m_iconHeight != -1))
		{
			g2d.drawImage(m_cornerIcon, 4, 4, this);
			g2d.drawImage(m_cornerIcon, getWidth() - m_iconWidth - 4, 4, this);
			g2d.drawImage(m_cornerIcon, 4, getHeight() - m_iconHeight - 4, this);
			g2d.drawImage(m_cornerIcon, getWidth() - m_iconWidth - 4, getHeight() - m_iconHeight - 4, this);
		}
	}
	
	@Override
	public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height)
	{
		if(img == m_Image)
		{
			if(0 != (infoflags & ImageObserver.WIDTH))
			{
				m_imgWidth = width;
			}

			if(0 != (infoflags & ImageObserver.HEIGHT))
			{
				m_imgHeight = height;
			}
		}
		else if(img == m_cornerIcon)
		{
			if(0 != (infoflags & ImageObserver.WIDTH))
			{
				m_iconWidth = width;
			}

			if(0 != (infoflags & ImageObserver.HEIGHT))
			{
				m_iconHeight = height;
			}
		}
		
		
		if((infoflags & ImageObserver.ALLBITS) != 0)
		{
			repaint();
			return false;
		}
		else if((infoflags & ImageObserver.ERROR) != 0)
		{
			System.out.println("ERROR: " + getClass().getName() + ".imageUpdate() - Failed to load image " + img);
		}
		else if((infoflags & ImageObserver.ABORT) != 0)
		{
			System.out.println("ERROR: " + getClass().getName() + ".imageUpdate() - image loading aborted for " + img);
		}
		
		return true;
	}
	
}
