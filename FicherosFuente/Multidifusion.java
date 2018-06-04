package services;

import java.util.concurrent.Semaphore;

import javax.ws.rs.client.WebTarget;

public class Multidifusion extends Thread{
	
	//ATRIBUTOS
	Proceso p;
	WebTarget[] service;
	
	//CONSTRUCTOR
	public Multidifusion(Proceso p_, WebTarget[] service_ ){
		p = p_;
		service = service_;
	}
	
	//METODO RUN
	public void run() {
		
		String contenido, id_mensaje, cadenaMensaje, emisor;
		Semaphore semAviso = new Semaphore(0,true);
		
		//Envío de 100 mensajes al grupo multicast
		for(int i=0; i<100; i++) { 
			
			semAviso = new Semaphore(0,true);
			
			//Construcción del mensaje
			contenido = "P"+p.infoProcesos[0][0]+" "+i;
			id_mensaje = i+"-"+p.infoProcesos[0][0];
			emisor = p.infoProcesos[0][0];
			
			cadenaMensaje = contenido+","+id_mensaje+","+"PROVISIONAL"+","+emisor;
			
			//Multidifusión del mensaje 
			MensajeMultidifusion[] hilosMensajeMultidifusion = new MensajeMultidifusion[p.infoProcesos.length];
			for(int j=0; j<p.infoProcesos.length; j++) {
				hilosMensajeMultidifusion[j] = new MensajeMultidifusion(cadenaMensaje,p.infoProcesos[j][0],service[j], p, semAviso);
				hilosMensajeMultidifusion[j].start();
			}
			
			semAviso.release(p.infoProcesos.length);
			
			//Duerme un tiempo aleatorio entre 1 y 1.5 s
			try { Thread.sleep(1000+(int)(Math.random()*500)); } catch (InterruptedException e) {e.printStackTrace();}
			
		}
		
		System.out.println("Hilo multidifusion del proceso "+p.infoProcesos[0][0]+" ha finalizado.");
		
	}

}