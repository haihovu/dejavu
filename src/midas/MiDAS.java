import com.mitel.guiutil.AboutInformation;
import com.mitel.guiutil.AboutPanel;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JApplet;

/**
 * The MiDAS marquee applet
 * @author  haiv
 */
public class MiDAS extends JApplet
{
	/**
	 * Slogans that can be displayed randomly
	 */
	private static final String s_InspirationalSlogans[] = 
	{
		"Do More", "Go Farther", "Reach Higher", "Innovate"
	};
	private AboutPanel m_aboutPanel;
	
	/** Creates a new instance of MiDAS */
	public MiDAS()
	{
	}
	
	@Override
	public void paint(Graphics g)
	{
		m_aboutPanel.paint(g);
	}
	
	@Override
	public void start()
	{
		m_aboutPanel.start();
	}

	@Override
	public void init()
	{
		super.init();
		List<String> otherStrings = new ArrayList<String>();
		int index = (int)(Math.random() * s_InspirationalSlogans.length);
		if(index == s_InspirationalSlogans.length)
		{
			--index;
		}
		otherStrings.add(s_InspirationalSlogans[index]);
		otherStrings.add("Outside The Box Software");
		otherStrings.add("Brought to you by Hai Vu");
		otherStrings.add("Powered by Java");
		otherStrings.add("Developed with NetBeans (www.netbeans.org)");
		otherStrings.add("Made in Canada");
		AboutInformation info = new AboutInformation("DejaVu - MiDAS", 
			"images/midas.gif", getSize(), otherStrings);
		m_aboutPanel = new AboutPanel(info);
		this.getContentPane().add(m_aboutPanel);
	}
	
}
