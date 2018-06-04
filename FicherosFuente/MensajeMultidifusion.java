package services;

import java.util.concurrent.Semaphore;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

public class MensajeMultidifusion extends Thread{
	
	//ATRIBUTOS
	int tipoMensaje;
	String cadenaMensaje;
	String procesoDestino;
	float orden;
	String idMensaje;
	WebTarget service;
	Proceso p;
	Semaphore semAviso;
		
	//CONSTRUCTOR
	//Constructor para el mensaje
	public MensajeMultidifusion(String cadenaMensaje_, String procesoDestino_, WebTarget service_, Proceso p_, Semaphore semAviso_){
		tipoMensaje = 1;
		cadenaMensaje = cadenaMensaje_;
		procesoDestino = procesoDestino_;
		service = service_;
		p = p_;
		semAviso = semAviso_;
	}
	
	//Constructor para la propuesta
	public MensajeMultidifusion(float orden_, String idMensaje_, String procesoDestino_, WebTarget service_, Proceso p_){
		tipoMensaje = 2;
		orden = orden_;
		idMensaje = idMensaje_;
		procesoDestino = procesoDestino_;
		service = service_;
		p = p_;
	}
	
	//Constructor para el acuerdo
	public MensajeMultidifusion(float orden_, String idMensaje_, String procesoDestino_, WebTarget service_, Proceso p_, Semaphore semAviso_){
		tipoMensaje = 3;
		orden = orden_;
		idMensaje = idMensaje_;
		procesoDestino = procesoDestino_;
		service = service_;
		p = p_;
		semAviso = semAviso_;
	}
	
	//METODO RUN
	public void run() {
			
		//Envío de un mensaje
		if(tipoMensaje == 1) { 
			//Espero
			try {semAviso.acquire();} catch (InterruptedException e1) {e1.printStackTrace();}
			
			if(p.infoProcesos[0][0].equals(procesoDestino)) { //Envío a nosotros mismos
				p.mensaje(cadenaMensaje, procesoDestino);
			}
			else {
				//Duerme un tiempo aleatorio entre 0.2 y 0.5 s
				try { Thread.sleep(200+(int)(Math.random()*300)); } catch (InterruptedException e) {e.printStackTrace();}
				
				System.out.println(service.path("rest/dispatcher/mensaje").queryParam("cadenaMensaje", cadenaMensaje).queryParam("procesoDestino", procesoDestino).request(MediaType.TEXT_PLAIN).get(String.class));
			}
		}
			
		//Envío de una propuesta
		else if(tipoMensaje == 2) { 
			if(p.infoProcesos[0][0].equals(procesoDestino)) { //Envío a nosotros mismos
				p.propuesta(""+orden, idMensaje);
			}
			else {
				//Duerme un tiempo aleatorio entre 0.2 y 0.5 s
				try { Thread.sleep(200+(int)(Math.random()*300)); } catch (InterruptedException e) {e.printStackTrace();}
					
				System.out.println(service.path("rest/dispatcher/propuesta").queryParam("ordenPropuesto", ""+orden).queryParam("idMensaje", idMensaje).queryParam("procesoDestino", procesoDestino).request(MediaType.TEXT_PLAIN).get(String.class));
			}
		}
			
		//Envío de un acuerdo
		else if(tipoMensaje == 3) { 
			//Espero
			try {semAviso.acquire();} catch (InterruptedException e1) {e1.printStackTrace();}
			
			if(p.infoProcesos[0][0].equals(procesoDestino)) { //Envío a nosotros mismos
				p.acuerdo(""+orden, idMensaje);
			}
			else {
				//Duerme un tiempo aleatorio entre 0.2 y 0.5 s
				try { Thread.sleep(200+(int)(Math.random()*300)); } catch (InterruptedException e) {e.printStackTrace();}
					
				System.out.println(service.path("rest/dispatcher/acuerdo").queryParam("ordenAcordado", ""+orden).queryParam("idMensaje", idMensaje).queryParam("procesoDestino", procesoDestino).request(MediaType.TEXT_PLAIN).get(String.class));
			}
		}
	}
}

