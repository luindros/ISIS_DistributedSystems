package services;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.Semaphore;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

public class Proceso extends Thread {
	
	//ATRIBUTOS
	
	String[][] infoProcesos;
	String dispatcherArbitro;
	float orden; // tiempo lógico según Lamport
	ArrayList<Mensaje> cola;
	File fichero_log;
	PrintWriter pw;
	Comparator<Mensaje> comparador;
	Object testigo;
	int numMensajesEntregados = 0;
	
	String ordenacionTotal;
	Multidifusion hiloMultidifusion;
	
	WebTarget[] service;
	
	//CONSTRUCTOR
	public Proceso(String dispatcherArbitro_, String id1, String ip1, String id2, String ip2,String id3, String ip3, String id4, String ip4, String id5, String ip5, String id6,String ip6, String rutaFichero, String ordenacionTotal_) {
		
		infoProcesos = new String[6][2]; 
		
		//Inicialización en el proceso
		
		infoProcesos[0][0] = id1;
		infoProcesos[0][1] = ip1;
		infoProcesos[1][0] = id2;
		infoProcesos[1][1] = ip2;
		infoProcesos[2][0] = id3;
		infoProcesos[2][1] = ip3;
		infoProcesos[3][0] = id4;
		infoProcesos[3][1] = ip4;
		infoProcesos[4][0] = id5;
		infoProcesos[4][1] = ip5;
		infoProcesos[5][0] = id6;
		infoProcesos[5][1] = ip6;
		
		dispatcherArbitro = dispatcherArbitro_;
		orden = 0+Float.parseFloat(id1)/10;
		cola = new ArrayList<Mensaje>();
		testigo = new Object();
		
		String ruta = rutaFichero+"fichero"+infoProcesos[0][0]+".txt";
		fichero_log = new File(ruta); 
		try {
			pw = new PrintWriter(fichero_log);
		} catch (FileNotFoundException e) {e.printStackTrace();}
		
		comparador = new CompararPorOrden();
		
		Client[] client = new Client[infoProcesos.length];
		URI[] uri = new URI[infoProcesos.length];
		service = new WebTarget[infoProcesos.length];
		
		for(int i=0; i<infoProcesos.length; i++) {
			client[i] = ClientBuilder.newClient();;
			uri[i] =UriBuilder.fromUri("http://"+infoProcesos[i][1]+":8080/algoritmoISIS").build();
		    service[i] = client[i].target(uri[i]);
		}
		
		//Variable para saber si se ejecutará sin o con el protocolo de multidifusión ordenada
		ordenacionTotal = ordenacionTotal_;
		
		//Creación del hilo multidifusión
		hiloMultidifusion = new Multidifusion(this, service);
		
	}
	
	//RECEPCION MENSAJE
	public void mensaje(String cadena_mensaje, String procesoEmisorId) {
		String idMensaje;
		Mensaje mensajeRecibido;
		
		if(ordenacionTotal.equalsIgnoreCase("false")) { //Se ejecuta SIN el protocolo de multidifusión ordenada
			String[] camposMensaje = cadena_mensaje.split(",");
			String contenido = camposMensaje[0];
			
			//Escritura en el fichero log del mensaje Pxx nnn 
			pw.println(contenido);
			System.out.println(contenido);
			numMensajesEntregados++;
			
			//Comprobación de si se han recibido todos los mensajes del grupo para así cerrar el fichero
			if(numMensajesEntregados == (infoProcesos.length*100)) {
				pw.close();
				//Aviso al dispatcherArbitro
				int flag = 0;
				for(int j=0; j<infoProcesos.length && flag==0; j++) {
					if(infoProcesos[j][1].equals(dispatcherArbitro)) {
						System.out.println(service[j].path("rest/dispatcher/finalizacion").request(MediaType.TEXT_PLAIN).get(String.class));
						flag=1;
					}
				}
				System.out.println("Proceso "+infoProcesos[0][0]+" ha finalizado.");
			}
			
		}
	    
		else if(ordenacionTotal.equalsIgnoreCase("true")) { //Se ejecuta CON el protocolo de multidifusión ordenada
			synchronized(testigo) {
					//Incremento del orden (LC1)
					orden = orden + 1;
					
					//Introducción del mensaje en la cola y reordenación de la cola
					String[] camposMensaje = cadena_mensaje.split(",");
					String contenido = camposMensaje[0];
					idMensaje = camposMensaje[1];
					String estado = camposMensaje[2];
					String emisor = camposMensaje[3];
					mensajeRecibido = new Mensaje(contenido,idMensaje,estado,emisor);
					mensajeRecibido.orden = orden;
					
					cola.add(mensajeRecibido);
					Collections.sort(cola,comparador);
					
			
			}//fin synchronized testigo
			
			int flag=0;
			//Envío de la propuesta al proceso que envió el mensaje
			for(int j=0; j<infoProcesos.length && flag==0; j++) {
				if(infoProcesos[j][0].equals(procesoEmisorId)) {
					MensajeMultidifusion hiloMensajeMultidifusion = new MensajeMultidifusion(mensajeRecibido.orden,idMensaje,infoProcesos[j][0],service[j],this);
					hiloMensajeMultidifusion.start();
					flag=1;
				}
			}
		}
	}
		
	//RECEPCION PROPUESTA
	public void propuesta(String ordenPropuesto, String idMensaje) {
		int propuestasRecibidas = 0;
		float ordenAcordado = 0;
		
		synchronized(testigo) {
				// Actualización del orden (LC2)
				if(Float.parseFloat(ordenPropuesto) > orden) {
					orden = (int) Float.parseFloat(ordenPropuesto) + Float.parseFloat(infoProcesos[0][0])/10 + 1;
				}
				else {
					orden = orden + 1;
				}
			
				//Obtención del mensaje correspondiente al idMensaje
				int flag = 0;
				for(int i=0; i<cola.size() && flag==0; i++) {
					if(cola.get(i).id_mensaje.equals(idMensaje)) {
						//Elección del mayor orden
						cola.get(i).orden = Math.max(cola.get(i).orden, Float.parseFloat(ordenPropuesto));
						ordenAcordado = cola.get(i).orden;
						
						//Incremento de las propuestas recibidas
						cola.get(i).numPropuestasRecibidas++;
						propuestasRecibidas = cola.get(i).numPropuestasRecibidas;
							
						flag=1;
					}
				}
		} //fin synchronized testigo	
		
		//Comprobación de si se han recibido todas las propuestas
		if(propuestasRecibidas == infoProcesos.length) {
			Semaphore semAviso = new Semaphore(0,true);
			MensajeMultidifusion[] hilosMensajeMultidifusion = new MensajeMultidifusion[infoProcesos.length];
			
			//Multidifusión del acuerdo
			for(int j=0; j<infoProcesos.length; j++) {
				hilosMensajeMultidifusion[j] = new MensajeMultidifusion(ordenAcordado,idMensaje,infoProcesos[j][0],service[j],this,semAviso);
				hilosMensajeMultidifusion[j].start();
			}
			
			semAviso.release(infoProcesos.length);
		}
		
	}
		
	//RECEPCION ACUERDO
	public void acuerdo(String ordenAcordado, String idMensaje) {
			
		synchronized(testigo) {
				// Actualización del orden (LC2)
				if(Float.parseFloat(ordenAcordado) > orden) {
					orden = (int) Float.parseFloat(ordenAcordado) + Float.parseFloat(infoProcesos[0][0])/10 + 1;
				}
				else {
					orden = orden + 1;
				}
				
				//Obtención del mensaje correspondiente al idMensaje
				int flag=0;
				for(int i=0; i<cola.size() && flag==0; i++) {
					if(cola.get(i).id_mensaje.equals(idMensaje)) {
						cola.get(i).orden = Float.parseFloat(ordenAcordado);
						cola.get(i).estado = "DEFINITIVO";
						flag=1;
					}
				}
				
				//Reordenación de la cola
				Collections.sort(cola,comparador);
				
				flag = 0;
				//Si hay mensajes en estado DEFINITIVO en la cola, se realiza la Entrega
				while(!cola.isEmpty() && flag==0) {
					if(cola.get(0).estado.equals("DEFINITIVO")) {
						//Escritura en el fichero log del mensaje Pxx nnn 
						pw.println(cola.get(0).contenido+" "+cola.get(0).orden);
						System.out.println(cola.get(0).contenido+" "+cola.get(0).orden);
						numMensajesEntregados++;
						//Eliminación del mensaje entregado de la cola
				        cola.remove(0);
					}
					else {
						flag=1;
					}
				}
				
				//Comprobación de si se han recibido todos los mensajes del grupo para así cerrar el fichero
				if(numMensajesEntregados == (infoProcesos.length*100)) {
					pw.close();
					//Aviso al dispatcherArbitro
					flag = 0;
					for(int j=0; j<infoProcesos.length && flag==0; j++) {
						if(infoProcesos[j][1].equals(dispatcherArbitro)) {
							System.out.println(service[j].path("rest/dispatcher/finalizacion").request(MediaType.TEXT_PLAIN).get(String.class));
							flag=1;
						}
					}
					System.out.println("Proceso "+infoProcesos[0][0]+" ha finalizado.");
				}
		}//fin synchronized testigo
	}
	
	//METODO RUN
	public void run() {
		
		// Llamada al servicio de sincronizacion del dispatcher arbitro  
		// para que cuando ya todos esten listos comiencen a multidifundir
		int flag = 0;
		for(int j=0; j<infoProcesos.length && flag==0; j++) {
			if(infoProcesos[j][1].equals(dispatcherArbitro)) {
				System.out.println(service[j].path("rest/dispatcher/sincronizacion").request(MediaType.TEXT_PLAIN).get(String.class));
				hiloMultidifusion.start();
				flag=1;
			}
		}
	}
	
	
}

