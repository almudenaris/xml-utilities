package model.manager.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import jcolibri.util.FileIO;

import model.datatransferobject.Lesson;
import model.datatransferobject.TransferUser;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


public class CallbackSAXParser extends DefaultHandler {
	
	/**
	 * Interfaz para implementar una pila de callbacks en SAX
	 * Extraido de http://www.gamasutra.com/view/feature/2678/efficient_xml_file_reading_for_.php
	 * @author almudena ruiz-iniesta (almudenaris@gmail.com)
	 *
	 */	
	
	
	private abstract class Receiver {		
		
		abstract Receiver processData(String name, Attributes attrs);

		abstract void finishProcessData(String name);
	}
	
	// Receiver del principio del documento
	private class StartReceiver extends Receiver{

		public void finishProcessData(String name) {
			
		}
	
		public Receiver processData(String name, Attributes attrs) {			
			// creamos la lista de usuarios
			listaUsuarios = new ArrayList<TransferUser>();			
			return new UserReceiver();
		}		
	}
	
	private class UserReceiver extends Receiver{

		
		public void finishProcessData(String name) {
			// ya tenemos al usuario con su perfil entero generado
			// lo añadimos a la lista de usuarios
			listaUsuarios.add(user);
		}
		
		public Receiver processData(String name, Attributes attrs) {
			// estamos procesando un nuevo usuario
			user = new TransferUser();
			user.setId(attrs.getValue(0));			
	
			return new LessonReceiver();
		}		
	}
	
	private class LessonReceiver extends Receiver{		
		
		public void finishProcessData(String name) {
			
		}

		public Receiver processData(String name, Attributes attrs) {			
			// tengo que coger el nombre de la lección para añadirlo al perfil
			String lessonName = attrs.getValue(0);			
			lesson = new Lesson();
			lesson.setLesson(lessonName);
			user.addLesson(lesson);
			// devolvemos un puntero a lo siguiente que vendrá que es un concepto
			return new ConceptReceiver();
		}		
	}
	
	private class ConceptReceiver extends Receiver{

		void finishProcessData(String name) {			
			
		}

		Receiver processData(String name, Attributes attrs) {
			String nombreConcepto = attrs.getValue(0);
			lesson.addConcepto(nombreConcepto);
			String nota = attrs.getValue(1);
			if (nota.isEmpty()) 
				nota = "-1.0";			
			lesson.addNota(nota);
			return null;
		}		
	}
	
	/**
	* Atributos
	*/
	private javax.xml.parsers.SAXParser saxParser;
	private String xmlFilename;	
	private Stack<Receiver> parserStack;
	
	//atributo para guardar a todos los usuarios del sistema
	private ArrayList<TransferUser> listaUsuarios;
	
	//atributo para guardar un usuario
	private TransferUser user;
	//atributo para guardar una lección con conceptos
	private Lesson lesson;
	
	
	public CallbackSAXParser() {
		  parserStack = new Stack<Receiver>();
	}
	
	public void startElement(String uri, String local, String name, Attributes attrs)  throws SAXException {
    	// el elemento que está en la cima de la pila
		Receiver candidate = parserStack.peek();
		Receiver followUp;	
		
		if (name.equals("user"))
			followUp = ((UserReceiver)candidate).processData(name, attrs);
		else if (name.equals("lesson"))
			followUp = ((LessonReceiver)candidate).processData(name, attrs);
		else if (name.equals("concept"))
			followUp = ((ConceptReceiver)candidate).processData(name, attrs);
		else
			followUp = candidate.processData(name, attrs);
		
		// el elemento siguiente		
    	if (followUp!=null) {
    		parserStack.push(followUp);
    	}
    	else
    		parserStack.push(candidate);    	
    }
    
    public void endElement(String uri, String local, String name) throws SAXException {
    	Receiver top = parserStack.pop();
    	top.finishProcessData(name);
    } 
    
    /* 
	 * Esta funcion es llamada cuando ve el contenido de una etiqueta      
	 */
    public void characters(char buf[], int offset, int len) throws SAXException{
    	String contenido = new String(buf, offset, len);
    	System.out.println(contenido);
    }
    
	public boolean init(String xmlFile){
		boolean _ok = false;
		// Crear la fabrica utilizar para SAX 
		SAXParserFactory factory  = SAXParserFactory.newInstance();
		
		try { 
		    saxParser = factory.newSAXParser();
		    xmlFilename = xmlFile;
		    parserStack.clear();
		    // Todas las inicializaciones adicionales que hace nuestro parser
		    _ok = true;

		} catch (ParserConfigurationException pexc) { 
		    pexc.printStackTrace();
		} catch (SAXException saxex) { 
		    saxex.printStackTrace();
		} 
		return _ok;
	}
	
	public void parse() {
		try {
			// StartReceiver implementa Receiver y es el responsable de 
			// la primera etiqueta que envuelve a todo el XML
			parserStack.push(new StartReceiver());
			InputStream ficEntrada=null;
			if ((ficEntrada=FileIO.openFile(xmlFilename)) != null)
				saxParser.parse( ficEntrada, this );
			
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void end() {
		// Si hay que hacer algo al finalizar
	}
	
	public ArrayList<TransferUser> getListaUsuarios(){
		return listaUsuarios;
	}
	
	// EJEMPLO DE USO
	public static void main(String[] args) {
		CallbackSAXParser p = new CallbackSAXParser();
		if (p.init("domains/user/profile.xml")){
			p.parse();
		}
		p.end();
	}

}
