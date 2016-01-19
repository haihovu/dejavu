/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import com.mitel.miutil.MiException;
import com.mitel.miutil.MiExceptionUtil;
import com.mitel.xmlutil.DomUtil;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import junit.framework.TestCase;
import org.w3c.dom.DOMStringList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

/**
 *
 * @author haiv
 */
public class DomUtilTest extends TestCase
{
	private static final Logger gLogger = Logger.getLogger(IcpDescriptorTest.class.getName());

	public DomUtilTest(String testName)
	{
		super(testName);
	}
	
	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
	}
	
	@Override
	protected void tearDown() throws Exception
	{
		super.tearDown();
	}
	
	private static Element buildDummyDocument(Document doc, int depth)
	{
		Element ret = doc.createElement("ElementLevel" + depth);
		ret.setAttribute("att1", "One" + depth);
		ret.setAttribute("att2", "Two" + depth);
		int nextLevel = depth - 1;
		if(nextLevel > 0)
		{
			ret.appendChild(buildDummyDocument(doc, nextLevel));
		}
		return ret;
	}
	
	private static void dumpTransformerProperties(Transformer t)
	{
		Properties p = t.getOutputProperties();
		Enumeration<?> names = p.propertyNames();
		while(names.hasMoreElements())
		{
			String name = names.nextElement().toString();
			System.out.println("Property " + name + "->" + p.getProperty(name));
		}
	}
	// TODO add test methods here. The name must begin with 'test'. For example:
	// public void testHello() {}
	public void testSerialization()
	{
		Document doc = DomUtil.createDocument("Test");
		assertNotNull("Failed to create document", doc);
		Element top = doc.getDocumentElement();
		top.appendChild(buildDummyDocument(doc, 3));
		System.out.println("Default result: " + DomUtil.serializeDom(top));
		try
		{
			LSSerializer serializer = DomUtil.createSerializer();
			DOMStringList params = serializer.getDomConfig().getParameterNames();
			for(int i = 0; i < params.getLength(); ++i)
			{
				System.out.println("Serializer param: " + params.item(i) + " -> " + serializer.getDomConfig().getParameter(params.item(i)));
			}
			serializer.getDomConfig().setParameter(DomUtil.DomConfigParam.FORMAT_PRETTY_PRINT.value, true);
			System.out.println("Pretty result: " + serializer.writeToString(top));
			serializer = DomUtil.createSerializer();
			serializer.getDomConfig().setParameter(DomUtil.DomConfigParam.XML_DECLARATION.value, false);
			System.out.println("No declaration result: " + serializer.writeToString(top));
			serializer.getDomConfig().setParameter(DomUtil.DomConfigParam.FORMAT_PRETTY_PRINT.value, true);
			System.out.println("Pretty and no declaration result: " + serializer.writeToString(top));
			File xmlTestFile = File.createTempFile("serializationTest", ".xml");
			xmlTestFile.createNewFile();
			//out.deleteOnExit();
			DomUtil.serializeDom(top, xmlTestFile, Charset.forName("UTF-8"));
			try
			{
				// Use the wrong charset, this should fail
				DomUtil.deserializeDom(xmlTestFile, Charset.forName("UTF-16"));
				fail("Was able to deserialize a UTF-8 file in UTF-16 format");
			}
			catch(FileNotFoundException ex)
			{
				fail(ex.getMessage());
			}
			catch(SAXException ex)
			{
				Logger.getLogger(DomUtilTest.class.getName()).log(Level.INFO, null, ex);
			}
			
			try
			{
				DomUtil.deserializeDom(xmlTestFile, Charset.forName("UTF-8"));
			}
			catch(FileNotFoundException ex)
			{
				fail(ex.getMessage());
			}
			catch(SAXException ex)
			{
				fail(ex.getMessage());
			}
			
			// Another way to serialize XML data is via the Transformer framework
			Transformer t = TransformerFactory.newInstance().newTransformer();
			t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			StringWriter w = new StringWriter();
			t.transform(new DOMSource(top), new StreamResult(w));
			System.out.println(w.toString());
			
			w = new StringWriter();
			t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			t.setOutputProperty(OutputKeys.INDENT, "yes");
			t.transform(new DOMSource(top), new StreamResult(w));
			System.out.println(w.toString());
			
			w = new StringWriter();
			t.setOutputProperty(OutputKeys.ENCODING, "UTF-16");
			t.transform(new DOMSource(top), new StreamResult(w));
			System.out.println(w.toString());
		}
		catch(TransformerException ex)
		{
			fail(MiExceptionUtil.simpleTrace(ex));
		}
		catch(IOException ex)
		{
			fail(MiExceptionUtil.simpleTrace(ex));
		}
		catch(MiException ex)
		{
			fail(MiExceptionUtil.simpleTrace(ex));
		}
	}
}
